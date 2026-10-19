package com.carlos.finanzas.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.carlos.finanzas.data.DebtEntity
import com.carlos.finanzas.debt.DebtCalculator
import com.carlos.finanzas.debt.DebtCalculator.Strategy
import com.carlos.finanzas.debt.DebtDates
import com.carlos.finanzas.ui.theme.Fin
import java.text.DateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.math.roundToLong

// ---------- Plan global ----------

data class DebtPlan(
    val oweCents: Long,
    val owedCents: Long,
    val monthlyCents: Long,
    val oweCount: Int,
    /** Meses para quedar libre con avalancha usando la suma de cuotas; null si no se salda. */
    val months: Int?,
    val neverPaysOff: Boolean,
    /** Deuda a la que conviene abonar primero (mayor tasa). */
    val firstTarget: String?,
    /** Interés que se ahorra con avalancha frente a pagar solo mínimos. */
    val savingsCents: Long
)

fun planFor(debts: List<DebtEntity>): DebtPlan {
    val owe = debts.filter { it.direction == "OWE" && it.balanceCents > 0 }
    val owed = debts.filter { it.direction == "OWED" }.sumOf { it.balanceCents }
    val monthly = owe.sumOf { it.monthlyPaymentCents }
    if (owe.isEmpty()) return DebtPlan(0, owed, 0, 0, 0, false, null, 0)
    val inputs = owe.map { DebtCalculator.DebtInput(it.id, it.name, it.balanceCents, it.annualRatePct, it.monthlyPaymentCents) }
    val aval = DebtCalculator.simulate(inputs, monthly, Strategy.AVALANCHE)
    val mins = DebtCalculator.simulate(inputs, monthly, Strategy.MINIMUMS_ONLY)
    val savings = if (!aval.neverPaysOff && !mins.neverPaysOff) mins.totalInterestCents - aval.totalInterestCents else 0L
    return DebtPlan(
        oweCents = owe.sumOf { it.balanceCents },
        owedCents = owed,
        monthlyCents = monthly,
        oweCount = owe.size,
        months = if (aval.neverPaysOff) null else aval.months,
        neverPaysOff = aval.neverPaysOff,
        firstTarget = owe.maxByOrNull { it.annualRatePct }?.name,
        savingsCents = savings.coerceAtLeast(0)
    )
}

private fun kindLabel(kind: String) = when (kind) {
    "CARD" -> "Tarjeta"
    "LOAN" -> "Préstamo"
    else -> "Informal"
}

private fun rateText(pct: Double) = "%.1f".format(Es, pct).removeSuffix(",0") + " % EA"

private fun newDebt() = DebtEntity(
    name = "", kind = "CARD", direction = "OWE", originalCents = 0, balanceCents = 0,
    annualRatePct = 0.0, monthlyPaymentCents = 0, dueDay = 0, createdAt = System.currentTimeMillis()
)

private fun monthsLabel(m: Int) = if (m == 1) "1 mes" else "$m meses"

// ---------- Pantalla ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(viewModel: DebtViewModel) {
    val debts by viewModel.debts.collectAsState()
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var form by remember { mutableStateOf<DebtEntity?>(null) }
    var strategies by remember { mutableStateOf(false) }
    val selected = debts.firstOrNull { it.id == selectedId }

    val editing = form
    if (editing != null) {
        BackHandler { form = null }
        DebtForm(editing, onSave = { viewModel.save(it); form = null }, onCancel = { form = null })
        return
    }
    if (selected != null) {
        BackHandler { selectedId = null; viewModel.select(null) }
        DebtDetail(
            debt = selected,
            viewModel = viewModel,
            onBack = { selectedId = null; viewModel.select(null) },
            onEdit = { form = selected }
        )
        return
    }

    DebtList(
        debts = debts,
        onOpen = { selectedId = it.id; viewModel.select(it.id) },
        onAdd = { form = newDebt() },
        onStrategies = { strategies = true }
    )

    if (strategies) {
        ModalBottomSheet(
            onDismissRequest = { strategies = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Fin.colors.card
        ) { StrategySheet(debts) }
    }
}

@Composable
private fun DebtList(debts: List<DebtEntity>, onOpen: (DebtEntity) -> Unit, onAdd: () -> Unit, onStrategies: () -> Unit) {
    val plan = remember(debts) { planFor(debts) }
    val today = LocalDate.now()

    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        ScreenHeader("Deudas") { PillButton("Nueva", onClick = onAdd, icon = FinIcons.Plus, height = 44.dp) }

        Sheet(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            color = Fin.colors.field,
            padding = PaddingValues(20.dp)
        ) {
            Text("Debes en total", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = Fin.colors.onFieldSoft)
            Text(formatMoney(plan.oweCents), style = MaterialTheme.typography.displaySmall, color = Fin.colors.onField)
            val months = plan.months
            val line = when {
                plan.oweCount == 0 -> "No tienes deudas registradas."
                plan.neverPaysOff -> "Con tus cuotas actuales no terminas de pagar: no alcanzan a cubrir el interés."
                months != null -> "Quedas libre en ${monthYearText(today.plusMonths(months.toLong()))}, en ${monthsLabel(months)}."
                else -> ""
            } + if (plan.owedCents > 0) " Te deben ${formatMoney(plan.owedCents)}." else ""
            Text(line.trim(), style = MaterialTheme.typography.bodyMedium, color = Fin.colors.onField, modifier = Modifier.padding(top = 10.dp))
        }

        if (debts.isEmpty()) {
            Text(
                "Agrega tus tarjetas, préstamos y deudas con personas para ver cuándo terminas de pagarlas y cuánto interés te cuestan.",
                style = MaterialTheme.typography.bodyLarge,
                color = Fin.colors.inkSoft,
                modifier = Modifier.padding(24.dp)
            )
        } else {
            Sheet(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                debts.forEach { DebtRow(it, today) { onOpen(it) } }
            }
        }

        if (plan.oweCount >= 2 && plan.firstTarget != null) {
            Row(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Fin.colors.mint)
                    .clickable(role = Role.Button, onClick = onStrategies)
                    .heightIn(min = 60.dp)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Abonar primero a ${plan.firstTarget}", style = MaterialTheme.typography.labelLarge, color = Fin.colors.onMint)
                    Text(
                        if (plan.savingsCents > 0) "Ahorras ${formatMoney(plan.savingsCents)} frente a pagar solo mínimos" else "Compara las estrategias de pago",
                        style = MaterialTheme.typography.bodySmall,
                        color = Fin.colors.onMint
                    )
                }
                Icon(FinIcons.Chevron, contentDescription = null, tint = Fin.colors.onMint, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun DebtRow(d: DebtEntity, today: LocalDate, onClick: () -> Unit) {
    val owe = d.direction == "OWE"
    val paid = if (d.originalCents > 0) (1f - d.balanceCents.toFloat() / d.originalCents).coerceIn(0f, 1f) else 0f
    val next = DebtDates.nextDueDate(today, d.dueDay)
    val days = next?.let { ChronoUnit.DAYS.between(today, it) }
    val soon = owe && days != null && days <= 3 && d.balanceCents > 0

    val meta = buildList {
        add(if (owe) kindLabel(d.kind) else "Te debe")
        if (d.annualRatePct > 0) add(rateText(d.annualRatePct)) else if (!owe) add("sin interés")
        if (d.kind == "CARD" && owe && d.creditLimitCents > 0) add("cupo usado ${d.balanceCents * 100 / d.creditLimitCents} %")
    }.joinToString(" · ")

    val due = when {
        d.balanceCents <= 0 -> "Saldada"
        next == null -> if (d.monthlyPaymentCents > 0) "Cuota ${formatMoney(d.monthlyPaymentCents)}" else null
        !owe -> "Quedó de pagarte el ${next.format(dayMonth)}"
        else -> {
            val label = if (d.kind == "CARD") "Mínimo" else "Cuota"
            val whenText = when (days) {
                0L -> "vence hoy"
                1L -> "vence mañana"
                in 2L..3L -> "vence el ${next.format(dayMonth)}, en $days días"
                else -> "vence el ${next.format(dayMonth)}"
            }
            "$label ${formatMoney(d.monthlyPaymentCents)} · $whenText"
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(d.name, style = MaterialTheme.typography.titleMedium, color = Fin.colors.ink, modifier = Modifier.weight(1f))
            Text(
                formatMoney(d.balanceCents),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (owe) Fin.colors.brick else Fin.colors.accent
            )
        }
        Text(meta, style = MaterialTheme.typography.bodySmall, color = Fin.colors.inkSoft, modifier = Modifier.padding(top = 3.dp))
        if (owe && d.originalCents > 0) Bar(paid, Fin.colors.accent, height = 6.dp, modifier = Modifier.padding(top = 10.dp))
        if (due != null) {
            Text(
                due,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (soon) FontWeight.SemiBold else FontWeight.Normal),
                color = if (soon) Fin.colors.amber else Fin.colors.inkSoft,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// ---------- Detalle con simulador ----------

@Composable
private fun DebtDetail(debt: DebtEntity, viewModel: DebtViewModel, onBack: () -> Unit, onEdit: () -> Unit) {
    val payments by viewModel.payments.collectAsState()
    var extra by remember(debt.id) { mutableFloatStateOf(0f) }
    var paying by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val owe = debt.direction == "OWE"
    val paid = if (debt.originalCents > 0) (1f - debt.balanceCents.toFloat() / debt.originalCents).coerceIn(0f, 1f) else 0f
    val extraCents = (extra.roundToLong() / 50_000L) * 50_000L * 100
    val base = remember(debt) { DebtCalculator.project(debt.balanceCents, debt.annualRatePct, debt.monthlyPaymentCents) }
    val simulated = DebtCalculator.project(debt.balanceCents, debt.annualRatePct, debt.monthlyPaymentCents, extraCents)

    if (paying) {
        PaymentDialog(owe, onConfirm = { cents, note -> viewModel.recordPayment(debt.id, cents, note); paying = false }, onDismiss = { paying = false })
    }
    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text("Archivar deuda") },
            text = { Text("\"${debt.name}\" dejará de mostrarse. Sus pagos se conservan.") },
            confirmButton = { TextButton(onClick = { confirmArchive = false; viewModel.archive(debt.id); onBack() }) { Text("Archivar") } },
            dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Cancelar") } }
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        BackBar("Deudas", onBack) { QuietButton("Editar", onClick = onEdit) }

        Column(Modifier.padding(horizontal = 24.dp)) {
            Text(debt.name, style = MaterialTheme.typography.headlineSmall, color = Fin.colors.ink)
            val meta = buildList {
                add(kindLabel(debt.kind))
                if (debt.annualRatePct > 0) add(rateText(debt.annualRatePct))
                if (debt.kind == "CARD" && debt.creditLimitCents > 0) add("cupo ${formatMoney(debt.creditLimitCents)}")
                if (debt.dueDay > 0) add("vence el ${debt.dueDay} de cada mes")
            }.joinToString(" · ")
            Text(meta, style = MaterialTheme.typography.bodySmall, color = Fin.colors.inkSoft, modifier = Modifier.padding(top = 4.dp))
            Text(
                if (owe) "Saldo por pagar" else "Te debe",
                style = MaterialTheme.typography.bodySmall,
                color = Fin.colors.inkSoft,
                modifier = Modifier.padding(top = 18.dp)
            )
            Text(formatMoney(debt.balanceCents), style = MaterialTheme.typography.displaySmall, color = if (owe) Fin.colors.brick else Fin.colors.accent)
            if (debt.originalCents > 0) {
                Bar(paid, Fin.colors.accent, modifier = Modifier.padding(top = 10.dp))
                Text(
                    "Has ${if (owe) "pagado" else "recibido"} el ${(paid * 100).toInt()} % de ${formatMoney(debt.originalCents)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Fin.colors.inkSoft,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        if (owe && debt.balanceCents > 0) {
            Sheet(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp), padding = PaddingValues(18.dp)) {
                val baseMonths = base.months
                Text(
                    when {
                        debt.monthlyPaymentCents <= 0 -> "Escribe la cuota o el pago mínimo en Editar para proyectar cuándo terminas."
                        baseMonths == null -> "Con esta cuota no se llega a saldar: no alcanza a cubrir el interés."
                        else -> "Pagando ${if (debt.kind == "CARD") "solo el mínimo" else "la cuota"} terminas en ${monthsLabel(baseMonths)} " +
                            "(${monthYearText(today.plusMonths(baseMonths.toLong()))}) y pagas ${formatMoney(base.totalInterestCents)} de interés."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Fin.colors.inkSoft
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Abono extra cada mes", style = MaterialTheme.typography.titleSmall, color = Fin.colors.ink)
                    Text(formatMoney(extraCents), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Fin.colors.ink)
                }
                Slider(
                    value = extra,
                    onValueChange = { extra = it },
                    valueRange = 0f..1_000_000f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = Fin.colors.accent,
                        activeTrackColor = Fin.colors.accent,
                        inactiveTrackColor = Fin.colors.track,
                        activeTickColor = Fin.colors.accent,
                        inactiveTickColor = Fin.colors.track
                    )
                )
                val newMonths = simulated.months
                Text(
                    when {
                        extraCents == 0L -> "Mueve la barra para ver cuánto te ahorras."
                        newMonths == null -> "Aun con ese abono no se salda."
                        baseMonths == null -> "Con ese abono sí terminas: ${monthsLabel(newMonths)}, en ${monthYearText(today.plusMonths(newMonths.toLong()))}."
                        else -> "Terminas en ${monthsLabel(newMonths)}, en ${monthYearText(today.plusMonths(newMonths.toLong()))}: " +
                            "${monthsLabel(baseMonths - newMonths)} antes y ${formatMoney(base.totalInterestCents - simulated.totalInterestCents)} menos de interés."
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Fin.colors.accent
                )
            }
        }

        PillButton(
            if (owe) "Registrar pago" else "Registrar cobro",
            onClick = { paying = true },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).fillMaxWidth(),
            height = 52.dp
        )

        Text("Pagos", style = MaterialTheme.typography.titleMedium, color = Fin.colors.ink, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 4.dp))
        if (payments.isEmpty()) {
            Text("Todavía no hay pagos registrados.", style = MaterialTheme.typography.bodyMedium, color = Fin.colors.inkSoft, modifier = Modifier.padding(horizontal = 24.dp))
        }
        payments.forEach { p ->
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp).heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(DateFormat.getDateInstance(DateFormat.MEDIUM, Es).format(Date(p.timestamp)), style = MaterialTheme.typography.bodyMedium, color = Fin.colors.ink)
                    Text(
                        (if (p.source == "AUTO") "Detectado en un mensaje" else "A mano") + (if (p.note.isNotBlank()) " · ${p.note}" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = Fin.colors.inkSoft
                    )
                }
                Text(formatMoney(p.amountCents), style = MaterialTheme.typography.titleSmall, color = Fin.colors.ink)
                QuietButton("Borrar", onClick = { viewModel.deletePayment(p) }, color = Fin.colors.inkSoft)
            }
        }

        QuietButton("Archivar deuda", onClick = { confirmArchive = true }, color = Fin.colors.brick, modifier = Modifier.padding(start = 12.dp, top = 12.dp))
    }
}

@Composable
private fun PaymentDialog(owe: Boolean, onConfirm: (Long, String) -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fin.colors.card,
        title = { Text(if (owe) "Registrar pago" else "Registrar cobro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FinField("Monto (COP)", amount, { amount = it.filter(Char::isDigit) }, keyboard = KeyboardType.Number, big = true)
                FinField("Nota (opcional)", note, { note = it })
                Text("El saldo baja por este monto. Si tu extracto dice otra cosa, corrígelo en Editar.", style = MaterialTheme.typography.bodySmall, color = Fin.colors.inkSoft)
            }
        },
        confirmButton = { PillButton("Guardar", onClick = { onConfirm(pesosToCents(amount), note.trim()) }, enabled = pesosToCents(amount) > 0) },
        dismissButton = { QuietButton("Cancelar", onClick = onDismiss, color = Fin.colors.inkSoft) }
    )
}

// ---------- Formulario ----------

@Composable
private fun DebtForm(initial: DebtEntity, onSave: (DebtEntity) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var direction by remember { mutableStateOf(initial.direction) }
    var kind by remember { mutableStateOf(kindLabel(initial.kind)) }
    var balance by remember { mutableStateOf(centsToPesosText(initial.balanceCents)) }
    var original by remember { mutableStateOf(centsToPesosText(initial.originalCents)) }
    var rate by remember { mutableStateOf(if (initial.annualRatePct > 0) "%.2f".format(Es, initial.annualRatePct).trimEnd('0').trimEnd(',') else "") }
    var payment by remember { mutableStateOf(centsToPesosText(initial.monthlyPaymentCents)) }
    var dueDay by remember { mutableStateOf(if (initial.dueDay > 0) initial.dueDay.toString() else "") }
    var limit by remember { mutableStateOf(centsToPesosText(initial.creditLimitCents)) }
    var keyword by remember { mutableStateOf(initial.matchKeyword) }

    val owe = direction == "OWE"
    val kindCode = when (kind) { "Tarjeta" -> "CARD"; "Préstamo" -> "LOAN"; else -> "INFORMAL" }
    val isCard = kindCode == "CARD" && owe

    fun save() {
        val balanceCents = pesosToCents(balance)
        onSave(
            initial.copy(
                name = name.trim(),
                kind = kindCode,
                direction = direction,
                originalCents = pesosToCents(original).takeIf { it > 0 } ?: maxOf(balanceCents, initial.originalCents),
                balanceCents = balanceCents,
                annualRatePct = rate.replace(',', '.').filter { it.isDigit() || it == '.' }.toDoubleOrNull()?.coerceIn(0.0, 300.0) ?: 0.0,
                monthlyPaymentCents = pesosToCents(payment),
                dueDay = dueDay.toIntOrNull()?.coerceIn(0, 31) ?: 0,
                creditLimitCents = if (isCard) pesosToCents(limit) else 0,
                matchKeyword = keyword.trim()
            )
        )
    }

    Column(Modifier.fillMaxSize().background(Fin.colors.card)) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuietButton("Cancelar", onClick = onCancel, color = Fin.colors.inkSoft, modifier = Modifier.weight(1f))
            Text(
                if (initial.id == 0L) "Nueva deuda" else "Editar deuda",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Fin.colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1.4f)
            )
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { save() }, enabled = name.isNotBlank(), modifier = Modifier.heightIn(min = 44.dp)) {
                    Text("Guardar", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Segmented(listOf("Yo debo", "Me deben"), if (owe) 0 else 1, { direction = if (it == 0) "OWE" else "OWED" })
            ChoiceGrid(listOf("Tarjeta", "Préstamo", "Informal"), kind, { kind = it })
            FinField("Nombre", name, { name = it }, helper = if (name.isBlank()) "Ej. Tarjeta Bancolombia o Juan Esteban" else null)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FinField("Saldo actual", balance, { balance = it.filter(Char::isDigit) }, Modifier.weight(1f), keyboard = KeyboardType.Number)
                FinField("Tasa EA %", rate, { rate = it.filter { c -> c.isDigit() || c == ',' || c == '.' } }, Modifier.weight(1f), keyboard = KeyboardType.Decimal)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FinField(if (isCard) "Pago mínimo" else "Cuota mensual", payment, { payment = it.filter(Char::isDigit) }, Modifier.weight(1f), keyboard = KeyboardType.Number)
                FinField("Día de pago", dueDay, { dueDay = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f), keyboard = KeyboardType.Number)
            }
            if (isCard) FinField("Cupo de la tarjeta", limit, { limit = it.filter(Char::isDigit) }, keyboard = KeyboardType.Number)
            FinField("Monto original (opcional)", original, { original = it.filter(Char::isDigit) }, keyboard = KeyboardType.Number,
                helper = "Para medir cuánto llevas pagado. Si lo dejas vacío se usa el saldo actual.")
            FinField(
                "Palabras para detectar pagos", keyword, { keyword = it },
                helper = "Si un SMS o notificación las contiene, el pago se descuenta solo del saldo."
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ---------- Estrategias ----------

@Composable
private fun StrategySheet(debts: List<DebtEntity>) {
    val owe = debts.filter { it.direction == "OWE" && it.balanceCents > 0 }
    val minTotal = owe.sumOf { it.monthlyPaymentCents }
    var budgetText by remember { mutableStateOf(centsToPesosText(minTotal)) }
    val budget = maxOf(pesosToCents(budgetText), minTotal)
    val inputs = owe.map { DebtCalculator.DebtInput(it.id, it.name, it.balanceCents, it.annualRatePct, it.monthlyPaymentCents) }
    val today = LocalDate.now()

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Cómo salir más rápido", style = MaterialTheme.typography.headlineSmall, color = Fin.colors.ink)
        Text(
            "Tus cuotas suman ${formatMoney(minTotal)} al mes. Escribe cuánto puedes pagar en total cada mes.",
            style = MaterialTheme.typography.bodyMedium,
            color = Fin.colors.inkSoft
        )
        FinField("Pago total mensual (COP)", budgetText, { budgetText = it.filter(Char::isDigit) }, keyboard = KeyboardType.Number, big = true)
        listOf(
            Strategy.AVALANCHE to "Primero la tasa más alta",
            Strategy.SNOWBALL to "Primero el saldo más pequeño",
            Strategy.MINIMUMS_ONLY to "Solo los mínimos"
        ).forEach { (s, label) ->
            val r = DebtCalculator.simulate(inputs, budget, s)
            val months = r.months
            Sheet(color = Fin.colors.ground, radius = 20.dp, padding = PaddingValues(16.dp)) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = Fin.colors.ink)
                Text(
                    if (r.neverPaysOff || months == null) "No se llega a saldar con este pago."
                    else "Libre en ${monthYearText(today.plusMonths(months.toLong()))} (${monthsLabel(months)}) · interés ${formatMoney(r.totalInterestCents)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Fin.colors.inkSoft,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (s != Strategy.MINIMUMS_ONLY && r.payoffOrder.isNotEmpty()) {
                    Text("Orden: ${r.payoffOrder.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = Fin.colors.inkSoft, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
