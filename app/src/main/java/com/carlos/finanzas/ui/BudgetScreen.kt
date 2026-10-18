package com.carlos.finanzas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFormat = DateTimeFormatter.ofPattern("d MMM", Locale("es", "CO"))

@Composable
fun BudgetScreen(viewModel: BudgetViewModel) {
    val ui by viewModel.state.collectAsState()
    val summary = ui.summary

    var limitText by remember(ui.limitCents) {
        mutableStateOf(if (ui.limitCents > 0) (ui.limitCents / 100).toString() else "")
    }
    var paydayText by remember(ui.payday) { mutableStateOf(ui.payday.toString()) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Presupuesto", style = MaterialTheme.typography.headlineSmall)

        if (summary != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Periodo: ${summary.period.start.format(dayFormat)} – ${summary.period.endExclusive.format(dayFormat)}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (ui.limitCents <= 0) {
                        Text("Define tu presupuesto abajo para calcular cuánto puedes gastar por día.")
                        Text("Gastado en el periodo: ${formatMoney(summary.spentCents)}")
                    } else {
                        Text("Puedes gastar por día", style = MaterialTheme.typography.titleSmall)
                        Text(
                            formatMoney(summary.perDayCents),
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (summary.overBudget) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                        LinearProgressIndicator(
                            progress = { (summary.spentCents.toFloat() / ui.limitCents).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Gastado: ${formatMoney(summary.spentCents)} de ${formatMoney(ui.limitCents)}")
                        Text(
                            if (summary.overBudget) "Te pasaste por ${formatMoney(-summary.remainingCents)}"
                            else "Disponible: ${formatMoney(summary.remainingCents)}"
                        )
                        Text("Días hasta el próximo pago: ${summary.daysLeft}")
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Gasto por categoría", style = MaterialTheme.typography.titleMedium)
                    if (summary.byCategory.isEmpty()) Text("Sin gastos en este periodo")
                    summary.byCategory.forEach { (category, cents) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(category)
                            Text(formatMoney(cents))
                        }
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Configuración", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it.filter(Char::isDigit) },
                    label = { Text("Presupuesto de gasto por periodo (COP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = paydayText,
                    onValueChange = { paydayText = it.filter(Char::isDigit).take(2) },
                    label = { Text("Día del mes en que recibes tu pago (1-31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    enabled = paydayText.toIntOrNull() in 1..31,
                    onClick = { viewModel.save(limitText.toLongOrNull() ?: 0L, paydayText.toInt()) }
                ) { Text("Guardar") }
                Text(
                    "Se cuentan todos los gastos del periodo, incluidos los pagos a deudas.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
