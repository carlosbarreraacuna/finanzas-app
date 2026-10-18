package com.carlos.finanzas.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.carlos.finanzas.capture.TemplateBuilder
import com.carlos.finanzas.data.UnrecognizedMessageEntity
import java.text.DateFormat
import java.util.Date

@Composable
fun InboxScreen(viewModel: InboxViewModel) {
    val pending by viewModel.pending.collectAsState()
    val info by viewModel.info.collectAsState()
    var selected by remember { mutableStateOf<UnrecognizedMessageEntity?>(null) }

    val current = selected
    if (current != null) {
        TemplateWizard(
            message = current,
            viewModel = viewModel,
            onClose = { selected = null }
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Text("Sin reconocer", style = MaterialTheme.typography.headlineSmall) }

        if (info != null) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(info.orEmpty(), Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearInfo() }) { Text("OK") }
                    }
                }
            }
        }

        if (pending.isEmpty()) {
            item { Text("No hay mensajes pendientes.") }
        }

        items(pending, key = { it.id }) { msg ->
            Card(Modifier.fillMaxWidth().clickable { selected = msg }) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${msg.sender} · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(msg.timestamp))}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(msg.text, maxLines = 3)
                    Row {
                        TextButton(onClick = { selected = msg }) { Text("Crear plantilla") }
                        TextButton(onClick = { viewModel.discard(msg.id) }) { Text("Descartar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateWizard(
    message: UnrecognizedMessageEntity,
    viewModel: InboxViewModel,
    onClose: () -> Unit
) {
    var tfv by remember(message.id) { mutableStateOf(TextFieldValue(message.text)) }
    var amount by remember(message.id) { mutableStateOf<TemplateBuilder.Range?>(null) }
    var merchant by remember(message.id) { mutableStateOf<TemplateBuilder.Range?>(null) }
    var isIncome by remember(message.id) { mutableStateOf(false) }
    var name by remember(message.id) { mutableStateOf("") }

    val selection = tfv.selection
    val hasSelection = !selection.collapsed

    val amountRange = amount
    val pattern = amountRange?.let { TemplateBuilder.build(message.text, it, merchant) }
    val preview = pattern?.let { viewModel.preview(it, message.text, isIncome) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Crear plantilla", style = MaterialTheme.typography.headlineSmall)
        Text("Remitente: ${message.sender}", style = MaterialTheme.typography.labelMedium)
        Text("Mantén presionado en el mensaje para seleccionar el monto, toca \"Marcar monto\"; " +
            "luego haz lo mismo con el comercio (opcional).")

        OutlinedTextField(
            value = tfv,
            onValueChange = { tfv = it.copy(text = message.text) }, // solo lectura: se permite mover la selección
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = hasSelection,
                onClick = { amount = TemplateBuilder.Range(selection.min, selection.max) }
            ) { Text("Marcar monto") }
            OutlinedButton(
                enabled = hasSelection,
                onClick = { merchant = TemplateBuilder.Range(selection.min, selection.max) }
            ) { Text("Marcar comercio") }
        }

        Text("Monto: " + (amountRange?.let { message.text.substring(it.start, it.end) } ?: "sin marcar"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Comercio: " + (merchant?.let { message.text.substring(it.start, it.end) } ?: "sin marcar"))
            if (merchant != null) TextButton(onClick = { merchant = null }) { Text("Quitar") }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isIncome, onCheckedChange = { isIncome = it })
            Text("Es un ingreso")
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre de la plantilla (opcional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Vista previa", style = MaterialTheme.typography.titleSmall)
                when {
                    amountRange == null -> Text("Marca el monto para ver la vista previa.")
                    pattern == null -> Text("No se pudo generar una plantilla con esa selección.")
                    preview == null -> Text("La plantilla no reproduce el mensaje.")
                    else -> {
                        Text("Monto: ${formatMoney(preview.amountCents)}")
                        Text("Comercio: ${preview.merchant}")
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = pattern != null && preview != null,
                onClick = {
                    viewModel.saveTemplate(message, pattern.orEmpty(), isIncome, name)
                    onClose()
                }
            ) { Text("Guardar plantilla") }
            OutlinedButton(onClick = {
                viewModel.discard(message.id)
                onClose()
            }) { Text("No es financiero") }
            TextButton(onClick = onClose) { Text("Volver") }
        }
    }
}
