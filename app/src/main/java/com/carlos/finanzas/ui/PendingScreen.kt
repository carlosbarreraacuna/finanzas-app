package com.carlos.finanzas.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.carlos.finanzas.capture.TemplateBuilder
import com.carlos.finanzas.data.UnrecognizedMessageEntity
import com.carlos.finanzas.ui.theme.Fin
import java.text.DateFormat
import java.util.Date

private fun sourceLabel(m: UnrecognizedMessageEntity): String {
    val app = when {
        m.source == "SMS" -> "SMS de ${m.sender}"
        m.sender.contains("gm", ignoreCase = true) -> "Gmail"
        else -> m.sender.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }
    return app + " · " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Es).format(Date(m.timestamp))
}

@Composable
fun PendingScreen(viewModel: InboxViewModel, transactions: TransactionsViewModel, onBack: () -> Unit) {
    val pending by viewModel.pending.collectAsState()
    val info by viewModel.info.collectAsState()
    var teaching by remember { mutableStateOf<UnrecognizedMessageEntity?>(null) }
    var adding by remember { mutableStateOf<UnrecognizedMessageEntity?>(null) }

    val current = teaching
    if (current != null) {
        BackHandler { teaching = null }
        TemplateWizard(current, viewModel, onClose = { teaching = null })
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        BackBar("Volver", onBack)
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text("Por revisar", style = MaterialTheme.typography.headlineMedium, color = Fin.colors.ink)
            Text(
                "Mensajes que parecen de dinero pero no supe leer. Enséñame una vez y los siguientes de ese remitente se registran solos.",
                style = MaterialTheme.typography.bodyMedium,
                color = Fin.colors.inkSoft,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        info?.let {
            Sheet(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), color = Fin.colors.mint, padding = PaddingValues(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(it, style = MaterialTheme.typography.labelLarge, color = Fin.colors.onMint, modifier = Modifier.weight(1f))
                    QuietButton("Listo", onClick = { viewModel.clearInfo() }, color = Fin.colors.onMint)
                }
            }
        }

        if (pending.isEmpty()) {
            Text("No hay nada por revisar.", style = MaterialTheme.typography.bodyLarge, color = Fin.colors.inkSoft, modifier = Modifier.padding(24.dp))
        }

        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            pending.forEach { msg ->
                val hasNumber = msg.text.any(Char::isDigit) && msg.text.contains('$') || Regex("""\d[\d.,]{3,}""").containsMatchIn(msg.text)
                Sheet(radius = 20.dp, padding = PaddingValues(16.dp)) {
                    Text(sourceLabel(msg), style = MaterialTheme.typography.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = Fin.colors.inkSoft)
                    Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = Fin.colors.ink, maxLines = 5, modifier = Modifier.padding(top = 8.dp))
                    if (!hasNumber) {
                        Text("No trae monto; quizá venga en un adjunto.", style = MaterialTheme.typography.bodySmall, color = Fin.colors.amber, modifier = Modifier.padding(top = 6.dp))
                    }
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (hasNumber) {
                            PillButton("Enseñar a leerlo", onClick = { teaching = msg }, height = 44.dp)
                        } else {
                            PillButton("Agregar a mano", onClick = { adding = msg }, container = Fin.colors.mint, content = Fin.colors.onMint, height = 44.dp)
                        }
                        QuietButton("Descartar", onClick = { viewModel.discard(msg.id) }, color = Fin.colors.inkSoft)
                    }
                }
            }
        }
    }

    adding?.let { msg ->
        var amount by remember(msg.id) { mutableStateOf("") }
        var detail by remember(msg.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { adding = null },
            containerColor = Fin.colors.card,
            title = { Text("Agregar a mano") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FinField("Monto (COP)", amount, { amount = it.filter(Char::isDigit) }, keyboard = KeyboardType.Number, big = true)
                    FinField("Detalle", detail, { detail = it })
                }
            },
            confirmButton = {
                PillButton("Guardar", enabled = pesosToCents(amount) > 0, onClick = {
                    transactions.addManual(pesosToCents(amount), detail, isIncome = false)
                    viewModel.resolve(msg.id)
                    adding = null
                })
            },
            dismissButton = { QuietButton("Cancelar", onClick = { adding = null }, color = Fin.colors.inkSoft) }
        )
    }
}

@Composable
private fun TemplateWizard(message: UnrecognizedMessageEntity, viewModel: InboxViewModel, onClose: () -> Unit) {
    var tfv by remember(message.id) { mutableStateOf(TextFieldValue(message.text)) }
    var amount by remember(message.id) { mutableStateOf<TemplateBuilder.Range?>(null) }
    var merchant by remember(message.id) { mutableStateOf<TemplateBuilder.Range?>(null) }
    var isIncome by remember(message.id) { mutableStateOf(false) }

    val selection = tfv.selection
    val hasSelection = !selection.collapsed
    val amountRange = amount
    val pattern = amountRange?.let { TemplateBuilder.build(message.text, it, merchant) }
    val preview = pattern?.let { viewModel.preview(it, message.text, isIncome) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        BackBar("Por revisar", onClose)
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Enseñar a leer", style = MaterialTheme.typography.headlineMedium, color = Fin.colors.ink)
            Text(
                "Mantén presionado sobre el mensaje para seleccionar el monto y toca Es el monto. Luego, si quieres, haz lo mismo con el comercio.",
                style = MaterialTheme.typography.bodyMedium,
                color = Fin.colors.inkSoft
            )
            TextField(
                value = tfv,
                onValueChange = { tfv = it.copy(text = message.text) },
                readOnly = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Fin.colors.card,
                    unfocusedContainerColor = Fin.colors.card,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Fin.colors.ink,
                    unfocusedTextColor = Fin.colors.ink
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Es el monto", onClick = { amount = TemplateBuilder.Range(selection.min, selection.max) }, enabled = hasSelection, height = 44.dp)
                PillButton("Es el comercio", onClick = { merchant = TemplateBuilder.Range(selection.min, selection.max) }, enabled = hasSelection,
                    container = Fin.colors.mint, content = Fin.colors.onMint, height = 44.dp)
            }
            Sheet(padding = PaddingValues(16.dp)) {
                Text("Monto: " + (amountRange?.let { message.text.substring(it.start, it.end) } ?: "sin marcar"), style = MaterialTheme.typography.bodyMedium, color = Fin.colors.ink)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Comercio: " + (merchant?.let { message.text.substring(it.start, it.end) } ?: "sin marcar"), style = MaterialTheme.typography.bodyMedium, color = Fin.colors.ink, modifier = Modifier.weight(1f))
                    if (merchant != null) QuietButton("Quitar", onClick = { merchant = null }, color = Fin.colors.inkSoft)
                }
                Row(Modifier.heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isIncome, onCheckedChange = { isIncome = it }, colors = CheckboxDefaults.colors(checkedColor = Fin.colors.accent))
                    Text("Es un ingreso", style = MaterialTheme.typography.bodyMedium, color = Fin.colors.ink)
                }
                Text(
                    when {
                        amountRange == null -> "Marca el monto para ver cómo quedaría."
                        pattern == null || preview == null -> "Con esa selección no logro leer el mensaje. Prueba seleccionando solo el número."
                        else -> "Quedaría: ${formatMoney(if (isIncome) preview.amountCents else -kotlin.math.abs(preview.amountCents))} en ${preview.merchant}"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (preview != null) Fin.colors.accent else Fin.colors.inkSoft,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            PillButton(
                "Guardar y aplicar",
                onClick = { viewModel.saveTemplate(message, pattern.orEmpty(), isIncome, ""); onClose() },
                enabled = pattern != null && preview != null,
                modifier = Modifier.fillMaxWidth(),
                height = 52.dp
            )
            QuietButton("No es de dinero", onClick = { viewModel.discard(message.id); onClose() }, color = Fin.colors.brick)
        }
    }
}
