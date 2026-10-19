package com.carlos.finanzas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.carlos.finanzas.capture.Categories
import com.carlos.finanzas.ui.theme.Fin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: BudgetViewModel) {
    val ui by viewModel.state.collectAsState()
    val summary = ui.summary
    var editing by remember { mutableStateOf(false) }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        ScreenHeader("Presupuesto")
        if (summary != null) {
            val end = summary.period.endExclusive.minusDays(1)
            Text(
                "Del ${summary.period.start.format(dayMonthLong)} al ${end.format(dayMonthLong)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Fin.colors.inkSoft,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp)
            )
        }

        if (summary == null || ui.limitCents <= 0) {
            Sheet(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp), padding = PaddingValues(20.dp)) {
                Text("Define cuánto quieres gastar", style = MaterialTheme.typography.titleLarge, color = Fin.colors.ink)
                Text(
                    "Con un presupuesto por periodo de pago te digo cuánto te queda y cuánto puedes gastar por día." +
                        (summary?.let { " Este periodo llevas ${formatMoney(it.spentCents)} en gastos." } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Fin.colors.inkSoft,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
                PillButton("Definir presupuesto", onClick = { editing = true })
            }
        } else {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp)) {
                Text(if (summary.overBudget) "Te pasaste" else "Te quedan", style = MaterialTheme.typography.bodyMedium, color = Fin.colors.inkSoft)
                Text(
                    formatMoney(kotlin.math.abs(summary.remainingCents)),
                    style = MaterialTheme.typography.displayMedium,
                    color = if (summary.overBudget) Fin.colors.brick else Fin.colors.ink
                )
                Text(
                    if (summary.overBudget) "Gastaste ${formatMoney(summary.spentCents)} de ${formatMoney(summary.limitCents)}. Faltan ${summary.daysLeft} días para el pago."
                    else "Unos ${formatMoney(summary.perDayCents)} por día durante ${summary.daysLeft} días, hasta el pago.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Fin.colors.inkSoft,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val spentBy = summary.byCategory.toMap()
            val limited = ui.categoryLimits.keys
                .map { cat -> Triple(cat, spentBy[cat] ?: 0L, ui.categoryLimits.getValue(cat)) }
                .sortedByDescending { (_, spent, limit) -> spent.toDouble() / limit }
            val unlimited = summary.byCategory.filter { it.first !in ui.categoryLimits }

            Sheet(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp), padding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)) {
                if (limited.isEmpty() && unlimited.isEmpty()) {
                    Text("Sin gastos en este periodo.", style = MaterialTheme.typography.bodyMedium, color = Fin.colors.inkSoft, modifier = Modifier.padding(vertical = 14.dp))
                }
                limited.forEach { (cat, spent, limit) -> CategoryLimitRow(cat, spent, limit) }
                if (limited.isNotEmpty() && unlimited.isNotEmpty()) {
                    Text(
                        "Sin límite",
                        style = MaterialTheme.typography.labelMedium,
                        color = Fin.colors.inkSoft,
                        modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)
                    )
                }
                unlimited.forEach { (cat, spent) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cat, style = MaterialTheme.typography.titleSmall, color = Fin.colors.ink)
                        Text(formatMoney(spent), style = MaterialTheme.typography.bodyMedium, color = Fin.colors.inkSoft)
                    }
                }
            }

            PillButton(
                "Ajustar límites",
                onClick = { editing = true },
                container = Fin.colors.mint,
                content = Fin.colors.onMint,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp)
            )
        }
    }

    if (editing) {
        ModalBottomSheet(
            onDismissRequest = { editing = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Fin.colors.card
        ) {
            BudgetEditor(ui, onSave = { limit, day, cats -> viewModel.save(limit, day, cats); editing = false })
        }
    }
}

@Composable
private fun CategoryLimitRow(category: String, spent: Long, limit: Long) {
    val ratio = spent.toFloat() / limit
    val over = spent > limit
    val warn = !over && ratio >= 0.85f
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(category, style = MaterialTheme.typography.titleSmall, color = Fin.colors.ink)
            Text(
                if (over) "Te pasaste ${formatMoney(spent - limit)}" else "Quedan ${formatMoney(limit - spent)}",
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    over -> Fin.colors.brick
                    warn -> Fin.colors.amber
                    else -> Fin.colors.inkSoft
                }
            )
        }
        Bar(
            ratio,
            color = when {
                over -> Fin.colors.brick
                warn -> Fin.colors.amberBar
                else -> Fin.colors.accent
            },
            track = if (over) Fin.colors.brickTint else Fin.colors.track,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            "${formatMoney(spent)} de ${formatMoney(limit)}",
            style = MaterialTheme.typography.bodySmall,
            color = Fin.colors.inkSoft,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun BudgetEditor(ui: BudgetUiState, onSave: (limitPesos: Long, payday: Int, categories: Map<String, Long>) -> Unit) {
    var limit by remember { mutableStateOf(centsToPesosText(ui.limitCents)) }
    var payday by remember { mutableStateOf(ui.payday.toString()) }
    val cats = remember {
        mutableStateMapOf<String, String>().apply {
            Categories.ALL.filter { it != Categories.INCOME }.forEach { put(it, centsToPesosText(ui.categoryLimits[it] ?: 0L)) }
        }
    }
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Presupuesto", style = MaterialTheme.typography.headlineSmall, color = Fin.colors.ink)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FinField("Gasto total por periodo", limit, { limit = it.filter(Char::isDigit) }, Modifier.weight(1.5f), keyboard = KeyboardType.Number, big = true)
            FinField("Día de pago", payday, { payday = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f), keyboard = KeyboardType.Number, big = true)
        }
        Text("Límite por categoría", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Fin.colors.ink)
        Text("Déjalo vacío si esa categoría no tiene límite.", style = MaterialTheme.typography.bodySmall, color = Fin.colors.inkSoft)
        Categories.ALL.filter { it != Categories.INCOME }.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { cat ->
                    FinField(cat, cats[cat].orEmpty(), { cats[cat] = it.filter(Char::isDigit) }, Modifier.weight(1f), keyboard = KeyboardType.Number)
                }
                if (pair.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            }
        }
        PillButton(
            "Guardar",
            onClick = {
                onSave(
                    limit.toLongOrNull() ?: 0L,
                    payday.toIntOrNull() ?: 1,
                    cats.mapValues { pesosToCents(it.value) }.filter { it.value > 0 }
                )
            },
            enabled = payday.toIntOrNull() in 1..31,
            modifier = Modifier.fillMaxWidth(),
            height = 52.dp
        )
        Text("Se cuentan todos los gastos del periodo, incluidos los pagos a deudas.", style = MaterialTheme.typography.bodySmall, color = Fin.colors.inkSoft)
    }
}
