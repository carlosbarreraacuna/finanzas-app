package com.carlos.finanzas.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.carlos.finanzas.FinanzasApp
import com.carlos.finanzas.data.AppDatabase
import com.carlos.finanzas.data.HealthEventEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BackupManager {
    const val PREFIX = "finanzas-backup-"
    const val EXTENSION = ".fnzbk"
    const val KEEP_LAST = 7

    /** Versión máxima de esquema que esta app sabe abrir (la de la base actual). */
    private const val MAX_SCHEMA_VERSION = AppDatabase.SCHEMA_VERSION

    data class Outcome(val ok: Boolean, val message: String)

    /** Crea un backup cifrado en la carpeta elegida y verifica que se pueda descifrar. */
    suspend fun backupNow(context: Context, passwordOverride: String? = null): Outcome =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext as FinanzasApp
            val outcome = try {
                doBackup(app, passwordOverride)
            } catch (e: Exception) {
                Outcome(false, "Backup fallido: ${e.message ?: e.javaClass.simpleName}")
            }
            app.db.health().insert(
                HealthEventEntity(
                    timestamp = System.currentTimeMillis(),
                    type = if (outcome.ok) "BACKUP_OK" else "BACKUP_FAILED",
                    detail = outcome.message
                )
            )
            outcome
        }

    private fun doBackup(app: FinanzasApp, passwordOverride: String?): Outcome {
        val prefs = BackupPrefs(app)
        val folder = prefs.folderUri ?: return Outcome(false, "Elige primero una carpeta de destino")
        val password = passwordOverride ?: prefs.loadPassword()
            ?: return Outcome(false, "Define primero la contraseña del backup")

        // Vuelca el WAL al archivo principal para que la copia esté completa.
        app.db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        val dbBytes = app.getDatabasePath(AppDatabase.FILE_NAME).readBytes()
        val encrypted = BackupCrypto.encrypt(dbBytes, password.toCharArray())

        val dir = DocumentFile.fromTreeUri(app, folder)
            ?: return Outcome(false, "No se puede acceder a la carpeta elegida")
        if (!dir.canWrite()) return Outcome(false, "Sin permiso de escritura en la carpeta; vuelve a elegirla")

        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val fileName = "$PREFIX$stamp$EXTENSION"
        val file = dir.createFile("application/octet-stream", fileName)
            ?: return Outcome(false, "No se pudo crear el archivo de backup")
        app.contentResolver.openOutputStream(file.uri, "w")?.use { it.write(encrypted) }
            ?: return Outcome(false, "No se pudo escribir el archivo de backup")

        // Verificación: releer y descifrar lo escrito.
        val readBack = app.contentResolver.openInputStream(file.uri)?.use { it.readBytes() }
        val verified = readBack != null && runCatching {
            BackupCrypto.decrypt(readBack, password.toCharArray()).size == dbBytes.size
        }.getOrDefault(false)
        if (!verified) {
            file.delete()
            return Outcome(false, "La verificación del backup falló; se descartó el archivo")
        }

        rotate(dir)
        return Outcome(true, "Backup creado: ${file.name}")
    }

    /** Conserva solo los últimos [KEEP_LAST] backups (el nombre incluye la fecha, así que ordena bien). */
    private fun rotate(dir: DocumentFile) {
        dir.listFiles()
            .filter { it.name?.startsWith(PREFIX) == true && it.name?.endsWith(EXTENSION) == true }
            .sortedBy { it.name }
            .dropLast(KEEP_LAST)
            .forEach { it.delete() }
    }

    /**
     * Restaura un backup: descifra (comprueba integridad GCM), valida que sea una base SQLite sana
     * con las tablas esperadas y solo entonces reemplaza la base actual.
     */
    suspend fun restore(context: Context, source: Uri, password: String): Outcome =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext as FinanzasApp
            val temp = File(app.cacheDir, "restore-check.db")
            try {
                val data = app.contentResolver.openInputStream(source)?.use { it.readBytes() }
                    ?: return@withContext Outcome(false, "No se pudo leer el archivo")
                val plain = BackupCrypto.decrypt(data, password.toCharArray())
                temp.writeBytes(plain)

                val problem = validate(temp)
                if (problem != null) return@withContext Outcome(false, problem)

                AppDatabase.closeAndReset()
                val target = app.getDatabasePath(AppDatabase.FILE_NAME)
                File(target.path + "-wal").delete()
                File(target.path + "-shm").delete()
                temp.copyTo(target, overwrite = true)
                Outcome(true, "Restauración completa. Cierra y vuelve a abrir la app.")
            } catch (e: BackupCrypto.InvalidBackupException) {
                Outcome(false, e.message ?: "Backup inválido")
            } catch (e: Exception) {
                Outcome(false, "Restauración fallida: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                temp.delete()
            }
        }

    /** @return null si la base es válida, o el motivo del rechazo. */
    private fun validate(file: File): String? {
        val db = try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: Exception) {
            return "El backup no contiene una base de datos válida"
        }
        db.use {
            val integrity = it.rawQuery("PRAGMA integrity_check", null).use { c ->
                if (c.moveToFirst()) c.getString(0) else "sin respuesta"
            }
            if (integrity != "ok") return "La base del backup está dañada ($integrity)"

            val version = it.rawQuery("PRAGMA user_version", null).use { c ->
                if (c.moveToFirst()) c.getInt(0) else 0
            }
            if (version > MAX_SCHEMA_VERSION) return "El backup es de una versión más nueva de la app"

            val hasTransactions = it.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='transactions'", null
            ).use { c -> c.moveToFirst() }
            if (!hasTransactions) return "El backup no tiene la tabla de transacciones"
        }
        return null
    }
}
