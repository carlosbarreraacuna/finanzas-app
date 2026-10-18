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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.carlos.finanzas.data.DebtEntity
import com.carlos.finanzas.debt.DebtDates
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val monthYearFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale("es", "CO"))
internal val dayMonthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale("es", "CO"))

/** "150000" -> 15.000.000 centavos. Ignora cualquier carácter que no sea dígito. */
internal fun pesosToCents(text: String): Long = (text.filter(Char::isDigit).toLongOrNull() ?: 0L) * 100

internal fun centsToPesosText(cents: Long): String = if (cents > 0) (cents / 100).toString() else ""

internal fun kindLabel(kind: String): String = when (kind) {
    "CARD" -> "Tarjeta"
    "LOAN" -> "Préstamo"
    else -> "Informal"
}

internal fun newDebt() = DebtEntity(
    name = "",
    kind = "CARD",
    direction = "OWE",
    originalCents = 0,
    balanceCents = 0,
    annualRatePct = 0.0,
    monthlyPaymentCents = 0,
    dueDay = 0,
    createdAt = System.currentTimeMillis()
)

@Composable
fun DebtScreen(viewModel: DebtViewModel) {
    val debts by viewModel.debts.collectAsState()
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var form by remember { mutableStateOf<DebtEntity?>(null) }
    val selected = debts.firstOrNull { it.id == selectedId }

    form?.let { current ->
        DebtFormDialog(
            initial = current,
            onSave = { saved ->
                viewModel.save(saved)
                form = null
            },
            onDismiss = { form = null }
        )
    }

    if (selected != null) {
        DebtDetail(
            debt = selected,
            viewModel = viewModel,
            onBack = {
                selectedId = null
                viewModel.select(null)
            },
            onEdit = { form = selected }
        )
    } else {
        DebtList(
            debts = debts,
            onOpen = { debt ->
                selectedId = debt.id
                viewModel.select(debt.id)
            },
            onAdd = { form = newDebt() }
        )
    }
}

@Composable
private fun DebtList(debts: List<DebtEntity>, onOpen: (DebtEntity) -> Unit, onAdd: () -> Unit) {
    val owe = debts.filter { it.direction == "OWE" }.sumOf { it.balanceCents }
    val owed = debts.filter { it.direction == "OWED" }.sumOf { it.balanceCents }
    val today = LocalDate.now()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Text("Deudas", style = MaterialTheme.typography.headlineSmall) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Debo en total: ${formatMoney(owe)}", style = MaterialTheme.typography.titleMedium)
                    Text("Me deben: ${formatMoney(owed)}")
                }
            }
        }
        item { Button(onClick = onAdd) { Text("Agregar deuda") } }

        if (debts.isEmpty()) {
            item {
                Text(
                    "Aún no registras deudas. Agrega tus tarjetas, préstamos y deudas con personas " +
                        "para ver cuándo terminas de pagarlas."
                )
            }
        }

        items(debts, key = { it.id }) { debt -> DebtCard(debt, today) { onOpen(debt) } }

        item { DebtStrategiesCard(debts) }
    }
}

@Composable
private fun DebtCard(d: DebtEntity, today: LocalDate, onClick: () -> Unit) {
    val owe = d.direction == "OWE"
    val paid = if (d.originalCents > 0) (1f - d.balanceCents.toFloat() / d.originalCents).coerceIn(0f, 1f) else 0f
    val next = DebtDates.nextDueDate(today, d.dueDay)

    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(d.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    formatMoney(d.balanceCents),
                    color = if (owe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            Text(
                kindLabel(d.kind) + " · " + (if (owe) "Yo debo" else "Me deben") +
                    (if (d.annualRatePct > 0) " · ${"%.1f".format(d.annualRatePct)} % EA" else ""),
                style = MaterialTheme.typography.bodySmall
            )
            LinearProgressIndicator(progress = { paid }, modifier = Modifier.fillMaxWidth())
            if (next != null && d.balanceCents > 0) {
                Text(
                    (if (owe) "Próximo pago: " else "Próximo cobro: ") + next.format(dayMonthFormat),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (d.kind == "CARD" && d.creditLimitCents > 0) {
                Text(
                    "Cupo usado: ${d.balanceCents * 100 / d.creditLimitCents} %",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, decimal: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = { text ->
            onChange(if (decimal) text.filter { it.isDigit() || it == '.' || it == ',' } else text.filter(Char::isDigit))
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ChoiceRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (value, label) ->
            val modifier = Modifier.weight(1f)
            val padding = PaddingValues(horizontal = 4.dp)
            if (value == selected) {
                Button(onClick = { onSelect(value) }, modifier = modifier, contentPadding = padding) {
                    Text(label, maxLines = 1)
                }
            } else {
                OutlinedButton(onClick = { onSelect(value) }, modifier = modifier, contentPadding = padding) {
                    Text(label, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun DebtFormDialog(initial: DebtEntity, onSave: (DebtEntity) -> Unit, onDismiss: () -> Unit) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var kind by remember(initial.id) { mutableStateOf(initial.kind) }
    var direction by remember(initial.id) { mutableStateOf(initial.direction) }
    var balance by remember(initial.id) { mutableStateOf(centsToPesosText(initial.balanceCents)) }
    var original by remember(initial.id) { mutableStateOf(centsToPesosText(initial.originalCents)) }
    var rate by remember(initial.id) {
        mutableStateOf(if (initial.annualRatePct > 0) initial.annualRatePct.toString() else "")
    }
    var payment by remember(initial.id) { mutableStateOf(centsToPesosText(initial.monthlyPaymentCents)) }
    var dueDay by remember(initial.id) { mutableStateOf(if (initial.dueDay > 0) initial.dueDay.toString() else "") }
    var limit by remember(initial.id) { mutableStateOf(centsToPesosText(initial.creditLimitCents)) }
    var keyword by remember(initial.id) { mutableStateOf(initial.matchKeyword) }

    val owe = direction == "OWE"
    val isCard = kind == "CARD" && owe

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Nueva deuda" else "Editar deuda") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre (ej. Tarjeta Bancolombia)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ChoiceRow(listOf("OWE" to "Yo debo", "OWED" to "Me deben"), direction) { direction = it }
                ChoiceRow(listOf("CARD" to "Tarjeta", "LOAN" to "Préstamo", "INFORMAL" to "Informal"), kind) { kind = it }
                NumberField("Saldo actual (COP)", balance, { balance = it })
                NumberField("Monto original (COP, opcional)", original, { original = it })
                NumberField("Tasa efectiva anual % (0 si no tiene)", rate, { rate = it }, decimal = true)
                NumberField(
                    if (isCard) "Pago mínimo mensual (COP)" else "Cuota mensual (COP)",
                    payment, { payment = it }
                )
                NumberField("Día del mes del pago (1-31, opcional)", dueDay, { dueDay = it.take(2) })
                if (isCard) NumberField("Cupo de la tarjeta (COP, opcional)", limit, { limit = it })
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("Palabra clave para detectar pagos (opcional)") },
                    supportingText = { Text("Ej. \"bancolombia tarjeta\". Si un mensaje la contiene, el pago se descuenta solo.") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    val balanceCents = pesosToCents(balance)
                    onSave(
                        initial.copy(
                            name = name.trim(),
                            kind = kind,
                            direction = direction,
                            originalCents = pesosToCents(original).takeIf { it > 0 } ?: balanceCents,
                            balanceCents = balanceCents,
                            annualRatePct = rate.replace(',', '.').toDoubleOrNull()?.coerceIn(0.0, 300.0) ?: 0.0,
                            monthlyPaymentCents = pesosToCents(payment),
                            dueDay = dueDay.toIntOrNull()?.coerceIn(0, 31) ?: 0,
                            creditLimitCents = if (isCard) pesosToCents(limit) else 0,
                            matchKeyword = keyword.trim()
                        )
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
