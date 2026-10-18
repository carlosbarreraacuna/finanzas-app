package com.carlos.finanzas.backup

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Ajustes del backup. La contraseña nunca se guarda en texto plano: se cifra con una clave
 * no exportable del Android Keystore para que la tarea en segundo plano pueda usarla.
 */
class BackupPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(v) = prefs.edit().putBoolean(KEY_ENABLED, v).apply()

    var folderUri: Uri?
        get() = prefs.getString(KEY_FOLDER, null)?.let(Uri::parse)
        set(v) = prefs.edit().putString(KEY_FOLDER, v?.toString()).apply()

    val hasPassword: Boolean get() = prefs.contains(KEY_PASSWORD)

    fun savePassword(password: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val ct = cipher.doFinal(password.toByteArray(Charsets.UTF_8))
        val packed = Base64.encodeToString(cipher.iv + ct, Base64.NO_WRAP)
        prefs.edit().putString(KEY_PASSWORD, packed).apply()
    }

    fun loadPassword(): String? {
        val packed = prefs.getString(KEY_PASSWORD, null) ?: return null
        return runCatching {
            val raw = Base64.decode(packed, Base64.NO_WRAP)
            val iv = raw.copyOfRange(0, IV_LEN)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(raw, IV_LEN, raw.size - IV_LEN), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun keystoreKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_FOLDER = "folder_uri"
        const val KEY_PASSWORD = "password_wrapped"
        const val ALIAS = "finanzas_backup_pw"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LEN = 12
    }
}
