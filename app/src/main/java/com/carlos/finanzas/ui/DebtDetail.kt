package com.carlos.finanzas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.carlos.finanzas.data.DebtEntity
import com.carlos.finanzas.debt.DebtCalculator
import com.carlos.finanzas.debt.DebtDates
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date

private fun monthsLabel(months: Int): String = if (months == 1) "1 mes" else "$months meses"

private fun projectionText(p: DebtCalculator.Projection, today: LocalDate): String {
    val months = p.months
    return when {
        months == 0 -> "Saldada."
        months == null -> "Con esta cuota no se llega a saldar: no alcanza a cubrir el interés " +
            "(o tardaría más de 50 años). Sube la cuota en Editar o haz abonos extra."
        else -> "Terminas en ${monthsLabel(months)} (${today.plusMonths(months.toLong()).format(monthYearFormat)}) " +
            "y pagas ${formatMoney(p.totalInterestCents)} de interés."
    }
}

@Composable
fun DebtDetail(debt: DebtEntity, viewModel: DebtViewModel, onBack: () -> Unit, onEdit: () -> Unit) {
    val payments by viewModel.payments.collectAsState()
    var extraText by remember(debt.id) { mutableStateOf("") }
    var oneTimeText by remember(debt.id) { mutableStateOf("") }
    var showPayment by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val owe = debt.direction == "OWE"
    val paid = if (debt.originalCents > 0) {
        (1f - debt.balanceCents.toFloat() / debt.originalCents).coerceIn(0f, 1f)
    } else 0f
    val nextDue = DebtDates.nextDueDate(today, debt.dueDay)

    val base = remember(debt) {
        DebtCalculator.project(debt.balanceCents, debt.annualRatePct, debt.monthlyPaymentCents)
    }
    val extra = pesosToCents(extraText)
    val oneTime = pesosToCents(oneTimeText)
    val withExtra = if (extra > 0 || oneTime > 0) {
        DebtCalculator.project(debt.balanceCents, debt.annualRatePct, debt.monthlyPaymentCents, extra, oneTime)
    } else null

    if (showPayment) {
        PaymentDialog(
            owe = owe,
            onConfirm = { cents, note ->
                viewModel.recordPayment(debt.id, cents, note)
                showPayment = false
            },
            onDismiss = { showPayment = false }
        )
    }
    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text("Archivar deuda") },
            text = { Text("Dejará de mostrarse en la lista. Sus pagos se conservan. ¿Archivar \"${debt.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmArchive = false
                    viewModel.archive(debt.id)
                    onBack()
                }) { Text("Archivar") }
            },
            dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancelar") } }
        )
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("← Volver") }
            TextButton(onClick = onEdit) { Text("Editar") }
        }
        Text(debt.name, style = MaterialTheme.typography.headlineSmall)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    (if (owe) "Saldo por pagar: " else "Te deben: ") + formatMoney(debt.balanceCents),
                    style = MaterialTheme.typography.titleMedium
                )
                LinearProgressIndicator(progress = { paid }, modifier = Modifier.fillMaxWidth())
                Text("Monto original: ${formatMoney(debt.originalCents)}")
                if (debt.annualRatePct > 0) Text("Tasa: ${"%.2f".format(debt.annualRatePct)} % EA")
                if (debt.monthlyPaymentCents > 0) {
                    Text(
                        (if (debt.kind == "CARD") "Pago mínimo: " else "Cuota mensual: ") +
                            formatMoney(debt.monthlyPaymentCents)
                    )
                }
                if (nextDue != null) Text("Próxima fecha: ${nextDue.format(dayMonthFormat)}")
                if (debt.kind == "CARD" && debt.creditLimitCents > 0) {
                    Text(
                        "Cupo: ${formatMoney(debt.creditLimitCents)} " +
                            "(usado ${debt.balanceCents * 100 / debt.creditLimitCents} %)"
                    )
                }
                if (debt.matchKeyword.isNotBlank()) {
                    Text("Detección automática: \"${debt.matchKeyword}\"", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (owe) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (debt.kind == "CARD") "Si solo pagas el mínimo" else "Proyección con tu cuota",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(projectionText(base, today))
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Simulador de abonos", style = MaterialTheme.typography.titleMedium)
                    MoneyField("Abono extra cada mes (COP)", extraText) { extraText = it }
                    MoneyField("Abono único hoy (COP)", oneTimeText) { oneTimeText = it }
                    if (withExtra != null) {
                        val baseMonths = base.months
                        val newMonths = withExtra.months
                        Text(
                            when {
                                withExtra.neverPaysOff -> "Aun con ese abono no se salda."
                                baseMonths == null || newMonths == null ->
                                    "Con ese abono sí se salda: ${monthsLabel(newMonths ?: 0)} y " +
                                        "${formatMoney(withExtra.totalInterestCents)} de interés."
                                else -> "Terminas en ${monthsLabel(newMonths)} (ahorras ${monthsLabel(baseMonths - newMonths)}) " +
                                    "y pagas ${formatMoney(withExtra.totalInterestCents)} de interés " +
                                    "(ahorras ${formatMoney(base.totalInterestCents - withExtra.totalInterestCents)})."
                            }
                        )
                    }
                }
            }
        }

        Button(onClick = { showPayment = true }) { Text(if (owe) "Registrar pago" else "Registrar cobro") }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Pagos registrados", style = MaterialTheme.typography.titleMedium)
                if (payments.isEmpty()) Text("Sin pagos todavía.")
                payments.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(DateFormat.getDateInstance(DateFormat.SHORT).format(Date(p.timestamp)))
                            Text(
                                (if (p.source == "AUTO") "Detectado" else "Manual") +
                                    (if (p.note.isNotBlank()) " · ${p.note}" else ""),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(formatMoney(p.amountCents))
                        TextButton(onClick = { viewModel.deletePayment(p) }) { Text("Borrar") }
                    }
                }
            }
        }

        TextButton(onClick = { confirmArchive = true }) { Text("Archivar deuda") }
    }
}

@Composable
private fun MoneyField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PaymentDialog(owe: Boolean, onConfirm: (Long, String) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (owe) "Registrar pago" else "Registrar cobro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MoneyField("Monto (COP)", amount) { amount = it }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("El saldo baja por el monto registrado. Si tu extracto muestra otro saldo, ajústalo en Editar.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                enabled = pesosToCents(amount) > 0,
                onClick = { onConfirm(pesosToCents(amount), note.trim()) }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
