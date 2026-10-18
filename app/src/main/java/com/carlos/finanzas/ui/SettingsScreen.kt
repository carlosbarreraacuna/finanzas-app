package com.carlos.finanzas.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val ctx = LocalContext.current
    val ui by viewModel.state.collectAsState()
    var password by remember { mutableStateOf("") }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            ctx.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.setFolder(uri)
        }
    }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.restore(uri, password)
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Backup cifrado", style = MaterialTheme.typography.titleMedium)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Backup diario automático")
                    Switch(checked = ui.enabled, onCheckedChange = viewModel::setEnabled)
                }
                Text("Carpeta: ${ui.folder ?: "sin elegir"}")
                OutlinedButton(onClick = { folderPicker.launch(null) }) { Text("Elegir carpeta") }
                Text(
                    "Último backup: " + (ui.lastBackup?.let {
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))
                    } ?: "nunca")
                )
                Text("Se conservan los últimos 7 backups.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Contraseña", style = MaterialTheme.typography.titleMedium)
                Text(if (ui.hasPassword) "Contraseña definida" else "Aún no definida")
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña del backup (mín. 8)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    enabled = password.isNotBlank(),
                    onClick = { viewModel.savePassword(password) }
                ) { Text("Guardar contraseña") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Acciones", style = MaterialTheme.typography.titleMedium)
                Button(enabled = !ui.busy, onClick = { viewModel.backupNow() }) { Text("Hacer backup ahora") }
                OutlinedButton(
                    enabled = !ui.busy,
                    onClick = { restorePicker.launch(arrayOf("*/*")) }
                ) { Text("Restaurar desde un archivo…") }
                Text(
                    "Para restaurar usa la contraseña escrita arriba (o la guardada si el campo está vacío). " +
                        "La restauración reemplaza todos los datos actuales.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        ui.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}
