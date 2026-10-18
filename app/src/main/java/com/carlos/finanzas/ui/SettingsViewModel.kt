package com.carlos.finanzas.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carlos.finanzas.FinanzasApp
import com.carlos.finanzas.backup.BackupManager
import com.carlos.finanzas.backup.BackupPrefs
import com.carlos.finanzas.backup.BackupWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val enabled: Boolean = false,
    val folder: String? = null,
    val hasPassword: Boolean = false,
    val lastBackup: Long? = null,
    val message: String? = null,
    val busy: Boolean = false
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = BackupPrefs(app)
    private val health = (app as FinanzasApp).db.health()

    private data class Local(
        val enabled: Boolean,
        val folder: String?,
        val hasPassword: Boolean,
        val message: String? = null,
        val busy: Boolean = false
    )

    private val local = MutableStateFlow(readLocal())

    val state: StateFlow<SettingsUiState> =
        combine(local, health.lastOf("BACKUP_OK")) { l, last ->
            SettingsUiState(l.enabled, l.folder, l.hasPassword, last, l.message, l.busy)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private fun readLocal(message: String? = null, busy: Boolean = false) = Local(
        enabled = prefs.enabled,
        folder = prefs.folderUri?.let { Uri.decode(it.lastPathSegment ?: it.toString()) },
        hasPassword = prefs.hasPassword,
        message = message,
        busy = busy
    )

    private fun post(message: String?, busy: Boolean = false) {
        local.value = readLocal(message, busy)
    }

    fun setFolder(uri: Uri) {
        prefs.folderUri = uri
        post("Carpeta guardada")
    }

    fun savePassword(password: String) {
        if (password.length < 8) {
            post("La contraseña debe tener al menos 8 caracteres")
            return
        }
        prefs.savePassword(password)
        post("Contraseña guardada (cifrada con el Keystore del teléfono). No la olvides: sin ella no se puede restaurar.")
    }

    fun setEnabled(enabled: Boolean) {
        if (enabled && (prefs.folderUri == null || !prefs.hasPassword)) {
            post("Antes de activar, elige la carpeta y define la contraseña")
            return
        }
        prefs.enabled = enabled
        if (enabled) BackupWorker.schedule(getApplication()) else BackupWorker.cancel(getApplication())
        post(if (enabled) "Backup diario activado" else "Backup diario desactivado")
    }

    fun backupNow() {
        local.update { it.copy(busy = true, message = "Creando backup…") }
        viewModelScope.launch {
            val result = BackupManager.backupNow(getApplication())
            post(result.message)
        }
    }

    fun restore(source: Uri, typedPassword: String) {
        val password = typedPassword.ifBlank { prefs.loadPassword().orEmpty() }
        if (password.isBlank()) {
            post("Escribe la contraseña del backup para restaurar")
            return
        }
        local.update { it.copy(busy = true, message = "Restaurando…") }
        viewModelScope.launch {
            val result = BackupManager.restore(getApplication(), source, password)
            post(result.message)
        }
    }
}
