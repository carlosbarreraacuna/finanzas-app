package com.carlos.finanzas.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carlos.finanzas.FinanzasApp
import com.carlos.finanzas.capture.Categories
import com.carlos.finanzas.capture.MerchantNormalizer
import com.carlos.finanzas.recurring.RecurringDetector
import com.carlos.finanzas.ui.theme.Fin
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import kotlin.math.abs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Subpantallas que se abren encima de las pestañas. */
enum class SubScreen { PENDING, RECURRING, CAPTURE, SETTINGS }

// ---------- Más ----------

@Composable
fun MoreScreen(
    health: HealthViewModel,
    settings: SettingsViewModel,
    subscriptions: SubscriptionsViewModel,
    inbox: InboxViewModel,
    onOpen: (SubScreen) -> Unit
) {
    val ctx = LocalContext.current
    val h by health.state.collectAsState()
    val s by settings.state.collectAsState()
    val recurring by subscriptions.recurring.collectAsState()
    val pending by inbox.pending.collectAsState()
    val p = h.permissions

    val (captureText, captureColor) = when {
        !p.notificationAccess -> "Falta el acceso a notificaciones" to Fin.colors.brick
        !p.listenerConnected -> "Desconectada" to Fin.colors.brick
        else -> "Funcionando" + (h.lastNotif?.let { " · última captura ${shortTime(it)}" } ?: "") to Fin.colors.accent
    }
    val version = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        ScreenHeader("Más")
        Sheet(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)) {
            NavRow("Captura de mensajes", captureText, { onOpen(SubScreen.CAPTURE) }, subtitleColor = captureColor)
            NavRow(
                "Cargos que se repiten",
                if (recurring.isEmpty()) "Aún no hay cargos repetidos"
                else "${formatMoney(recurring.sumOf { it.monthlyCents })} al mes en ${recurring.size} ${if (recurring.size == 1) "cargo" else "cargos"}",
                { onOpen(SubScreen.RECURRING) }
            )
            NavRow(
                "Mensajes por revisar",
                if (pending.isEmpty()) "Nada pendiente" else "${pending.size} pendientes",
                { onOpen(SubScreen.PENDING) },
                subtitleColor = if (pending.isEmpty()) Fin.colors.inkSoft else Fin.colors.amber
            )
        }
        Sheet(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            NavRow(
                "Respaldo cifrado",
                if (s.lastBackup != null) "Último: ${shortTime(s.lastBackup!!)}" else if (s.enabled) "Aún no se ha hecho" else "Apagado",
                { onOpen(SubScreen.SETTINGS) },
                subtitleColor = if (s.lastBackup == null) Fin.colors.amber else Fin.colors.inkSoft
            )
        }
        Text(
            "Finanzas $version. Tus datos viven solo en este teléfono y en tus respaldos cifrados.",
            style = MaterialTheme.typography.bodySmall,
            color = Fin.colors.inkSoft,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp)
        )
    }
}

private fun shortTime(ts: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Es).format(Date(ts))

// ---------- Captura ----------

@Composable
fun CaptureScreen(viewModel: HealthViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val ui by viewModel.state.collectAsState()
    val p = ui.permissions
    val down = !p.notificationAccess || !p.listenerConnected

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        BackBar("Más", onBack)

        Sheet(
            Modifier.padding(horizontal = 16.dp),
            color = if (down) Fin.colors.brickTint else Fin.colors.mint,
            padding = PaddingValues(20.dp)
        ) {
            val fg = if (down) Fin.colors.onBrickTint else Fin.colors.onMint
            Text(
                when {
                    !p.notificationAccess -> "La captura no tiene permiso"
                    !p.listenerConnected -> "Dejé de capturar"
                    else -> "Capturando"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = fg
            )
            Text(
                when {
                    !p.notificationAccess -> "Activa el acceso a notificaciones para Finanzas. Mientras tanto no se registra nada."
                    !p.listenerConnected -> "El sistema cerró el servicio en segundo plano. Mientras esté así no se registran tus compras."
                    else -> "Recibo tus notificaciones y SMS. Te aviso si se corta."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = fg,
                modifier = Modifier.padding(top = 10.dp)
            )
            if (!p.notificationAccess) {
                PillButton("Activar acceso", onClick = { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).newTask()) },
                    container = Fin.colors.brick, content = Fin.colors.card, modifier = Modifier.padding(top = 16.dp))
            } else if (!p.listenerConnected) {
                PillButton("Reconectar ahora", onClick = { viewModel.requestRebind() },
                    container = Fin.colors.brick, content = Fin.colors.card, modifier = Modifier.padding(top = 16.dp))
            }
        }

        Sheet(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            StatusLine("Servicio de captura", if (p.listenerConnected) "Conectado" else "Desconectado", p.listenerConnected)
            StatusLine("Acceso a notificaciones", if (p.notificationAccess) "Activo" else "Inactivo", p.notificationAccess,
                action = if (p.notificationAccess) null else ({ ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).newTask()) }))
            StatusLine("Permiso de SMS", if (p.sms) "Activo" else "Inactivo", p.sms,
                action = if (p.sms) null else ({ ctx.startActivity(appDetailsIntent(ctx)) }))
            StatusLine("Batería sin restricciones", if (p.batteryExempt) "Activo" else "Restringida", p.batteryExempt,
                action = if (p.batteryExempt) null else ({ ctx.startActivity(batteryIntent(ctx)) }))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Inicio automático", style = MaterialTheme.typography.bodyLarge, color = Fin.colors.ink)
                    Text("Ajuste de Xiaomi que evita los cortes", style = MaterialTheme.typography.bodySmall, color = Fin.colors.inkSoft)
                }
                PillButton("Abrir ajuste", onClick = { ctx.startActivity(appDetailsIntent(ctx)) },
                    container = Fin.colors.amberTint, content = Fin.colors.onAmberTint, height = 40.dp)
            }
        }

        Text("Últimas 24 horas", style = MaterialTheme.typography.titleMedium, color = Fin.colors.ink,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 6.dp))
        Text(
            "${ui.capturesLast24h} mensajes capturados, ${ui.parseFailuresLast24h} sin leer." +
                (ui.lastBackup?.let { "\nÚltimo respaldo: ${shortTime(it)}." } ?: "\nTodavía no hay respaldos."),
            style = MaterialTheme.typography.bodyMedium,
            color = Fin.colors.inkSoft,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Text("Registro técnico", style = MaterialTheme.typography.titleMedium, color = Fin.colors.ink,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 6.dp))
        if (ui.recentEvents.isEmpty()) {
            Text("Sin eventos todavía.", style = MaterialTheme.typography.bodySmall, color = Fin.colors.inkSoft, modifier = Modifier.padding(horizontal = 24.dp))
        }
        ui.recentEvents.take(10).forEach { e ->
            Text(
                "${shortTime(e.timestamp)} · ${e.type}" + (e.detail?.let { " ($it)" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = Fin.colors.inkSoft,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String, ok: Boolean, action: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Fin.colors.ink, modifier = Modifier.weight(1f))
        if (action != null) {
            QuietButton(value + " · Abrir", onClick = action, color = Fin.colors.brick)
        } else {
            Text(value, style = MaterialTheme.typography.labelLarge, color = if (ok) Fin.colors.accent else Fin.colors.brick)
        }
    }
}

private fun Intent.newTask() = apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

private fun batteryIntent(ctx: Context) =
    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}")).newTask()

private fun appDetailsIntent(ctx: Context) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")).newTask()

// ---------- Cargos recurrentes ----------

class SubscriptionsViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as FinanzasApp).db

    val recurring: StateFlow<List<RecurringDetector.Recurring>> =
        db.transactions().recent(3000).map { txs ->
            val charges = txs.filter {
                it.amountCents < 0 && it.category != Categories.DEBTS &&
                    MerchantNormalizer.isLearnable(MerchantNormalizer.key(it.merchant))
            }.map {
                RecurringDetector.Charge(MerchantNormalizer.key(it.merchant), it.merchant, it.timestamp, abs(it.amountCents))
            }
            RecurringDetector.detect(charges, System.currentTimeMillis())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

private fun periodLabel(days: Int) = when (days) {
    in 6..8 -> "Cada semana"
    in 13..16 -> "Cada 2 semanas"
    else -> "Cada mes"
}

@Composable
fun RecurringScreen(viewModel: SubscriptionsViewModel, onBack: () -> Unit) {
    val items by viewModel.recurring.collectAsState()
    val total = items.sumOf { it.monthlyCents }
    val zone = ZoneId.systemDefault()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        BackBar("Más", onBack)
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text("Cargos que se repiten", style = MaterialTheme.typography.headlineMedium, color = Fin.colors.ink)
            if (items.isEmpty()) {
                Text(
                    "Todavía no hay cargos repetidos. Aparecen cuando un mismo comercio te cobra al menos 3 veces " +
                        "con un ritmo parecido: semanal, quincenal o mensual.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Fin.colors.inkSoft,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else {
                Text("Pagas cada mes, más o menos", style = MaterialTheme.typography.bodyMedium, color = Fin.colors.inkSoft, modifier = Modifier.padding(top = 14.dp))
                Text(formatMoney(total), style = MaterialTheme.typography.displayMedium, color = Fin.colors.ink)
                Text(
                    "Son ${formatMoney(total * 12)} al año en ${items.size} ${if (items.size == 1) "cargo" else "cargos"}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Fin.colors.inkSoft,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        if (items.isNotEmpty()) {
            Sheet(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)) {
                items.forEach { r ->
                    val next = Instant.ofEpochMilli(r.nextTimestamp).atZone(zone).toLocalDate()
                    Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(r.merchant, style = MaterialTheme.typography.titleMedium, color = Fin.colors.ink, modifier = Modifier.weight(1f))
                            Text(formatMoney(r.lastCents), style = MaterialTheme.typography.titleMedium, color = Fin.colors.ink)
                        }
                        Text(
                            "${periodLabel(r.periodDays)} · próximo hacia el ${next.format(dayMonth)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Fin.colors.inkSoft,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                        if (r.priceChanged) {
                            Sheet(Modifier.padding(top = 8.dp), radius = 12.dp, color = Fin.colors.brickTint, padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    (if (r.lastCents > r.previousCents) "Subió" else "Cambió") + " de ${formatMoney(r.previousCents)} a ${formatMoney(r.lastCents)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Fin.colors.onBrickTint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- Respaldo y ajustes ----------

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        BackBar("Más", onBack)
        Text("Respaldo cifrado", style = MaterialTheme.typography.headlineMedium, color = Fin.colors.ink, modifier = Modifier.padding(horizontal = 24.dp))
        Text(
            "Una copia diaria cifrada con tu contraseña en la carpeta que elijas. Se guardan las últimas 7.",
            style = MaterialTheme.typography.bodyMedium,
            color = Fin.colors.inkSoft,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp)
        )

        Sheet(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Respaldo diario", style = MaterialTheme.typography.titleMedium, color = Fin.colors.ink)
                    Text(
                        "Último: " + (ui.lastBackup?.let { shortTime(it) } ?: "nunca"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Fin.colors.inkSoft
                    )
                }
                Switch(
                    checked = ui.enabled,
                    onCheckedChange = viewModel::setEnabled,
                    colors = SwitchDefaults.colors(checkedTrackColor = Fin.colors.accent, checkedThumbColor = Color.White)
                )
            }
            NavRow("Carpeta", ui.folder ?: "Sin elegir", { folderPicker.launch(null) },
                subtitleColor = if (ui.folder == null) Fin.colors.amber else Fin.colors.inkSoft)
        }

        Sheet(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), padding = PaddingValues(18.dp)) {
            Text("Contraseña", style = MaterialTheme.typography.titleMedium, color = Fin.colors.ink)
            Text(
                if (ui.hasPassword) "Ya tienes una contraseña guardada. Escribe otra para cambiarla." else "Aún no hay contraseña. Mínimo 8 caracteres.",
                style = MaterialTheme.typography.bodySmall,
                color = if (ui.hasPassword) Fin.colors.inkSoft else Fin.colors.amber,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            androidx.compose.material3.TextField(
                value = password,
                onValueChange = { password = it },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                placeholder = { Text("Contraseña del respaldo") },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = Fin.colors.ground,
                    unfocusedContainerColor = Fin.colors.ground,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            PillButton("Guardar contraseña", onClick = { viewModel.savePassword(password) }, enabled = password.isNotBlank(),
                modifier = Modifier.padding(top = 12.dp))
        }

        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PillButton("Respaldar ahora", onClick = { viewModel.backupNow() }, enabled = !ui.busy)
            PillButton("Restaurar…", onClick = { restorePicker.launch(arrayOf("*/*")) }, enabled = !ui.busy,
                container = Fin.colors.mint, content = Fin.colors.onMint)
        }
        Text(
            "Para restaurar se usa la contraseña escrita arriba, o la guardada si el campo está vacío. Restaurar reemplaza todos los datos actuales.",
            style = MaterialTheme.typography.bodySmall,
            color = Fin.colors.inkSoft,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp)
        )
        ui.message?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Fin.colors.accent,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp))
        }
    }
}
