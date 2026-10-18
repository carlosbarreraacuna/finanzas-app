package com.carlos.finanzas.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

@Composable
fun HealthScreen(viewModel: HealthViewModel) {
    val ctx = LocalContext.current
    val ui by viewModel.state.collectAsState()
    val p = ui.permissions

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Salud del servicio", style = MaterialTheme.typography.headlineSmall)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Permisos y servicio", style = MaterialTheme.typography.titleMedium)
                StatusRow("Acceso a notificaciones", p.notificationAccess)
                StatusRow("Listener conectado", p.listenerConnected)
                StatusRow("Permiso de SMS", p.sms)
                StatusRow("Sin restricción de batería", p.batteryExempt)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Capturas", style = MaterialTheme.typography.titleMedium)
                Text("Última notificación: ${formatTime(ui.lastNotif)}")
                Text("Último SMS: ${formatTime(ui.lastSms)}")
                Text("Capturas últimas 24 h: ${ui.capturesLast24h}")
                Text("Fallos de parseo últimas 24 h: ${ui.parseFailuresLast24h}")
                Text("Último backup: ${formatTime(ui.lastBackup)}")
            }
        }

        if (p.notificationAccess && !p.listenerConnected) {
            Button(onClick = { viewModel.requestRebind() }) { Text("Reintentar enlace del listener") }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Eventos recientes", style = MaterialTheme.typography.titleMedium)
                if (ui.recentEvents.isEmpty()) Text("Sin eventos todavía")
                ui.recentEvents.forEach { e ->
                    Text(
                        "${formatTime(e.timestamp)} · ${e.type}" + (e.detail?.let { " ($it)" } ?: ""),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (!p.notificationAccess) {
            Button(onClick = { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).newTask()) }) {
                Text("Activar acceso a notificaciones")
            }
        }
        if (!p.batteryExempt) {
            Button(onClick = { ctx.startActivity(batteryIntent(ctx)) }) {
                Text("Quitar restricción de batería")
            }
        }
        if (!p.sms) {
            Button(onClick = { ctx.startActivity(appDetailsIntent(ctx)) }) {
                Text("Abrir ajustes de la app (SMS)")
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Text(
            if (ok) "OK" else "Falta",
            color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

private fun formatTime(ts: Long?): String =
    if (ts == null) "sin registros"
    else DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(ts))

private fun Intent.newTask() = apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

private fun batteryIntent(ctx: Context) =
    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}")).newTask()

private fun appDetailsIntent(ctx: Context) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")).newTask()
