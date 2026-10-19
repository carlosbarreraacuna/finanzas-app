package com.carlos.finanzas.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carlos.finanzas.capture.Categories
import com.carlos.finanzas.data.TransactionEntity
import com.carlos.finanzas.ui.theme.Fin
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

private fun TransactionEntity.date(zone: ZoneId): LocalDate = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()

private fun TransactionEntity.timeText(zone: ZoneId): String =
    Instant.ofEpochMilli(timestamp).atZone(zone).toLocalTime().format(timeShort)

/** "Hoy, 18 de octubre", "Ayer, 17 de octubre" o "Lunes 13 de octubre". */
fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Hoy, " + date.format(dayMonthLong)
    today.minusDays(1) -> "Ayer, " + date.format(dayMonthLong)
    else -> date.format(weekdayDayMonth).capitalized()
}

/** Fila de un movimiento: inicial, comercio, categoría y hora, monto. */
@Composable
fun TxRow(tx: TransactionEntity, subtitlePrefix: String? = null, onClick: (() -> Unit)? = null) {
    val zone = ZoneId.systemDefault()
    val income = tx.amountCents > 0
    val uncategorized = tx.category == null || tx.category == Categories.UNCATEGORIZED
    val base = Modifier
        .fillMaxWidth()
        .heightIn(min = 66.dp)
    Row(
        if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when {
            uncategorized && !income -> Monogram("?", bg = Fin.colors.amberTint, fg = Fin.colors.amber)
            income -> Monogram(tx.merchant, bg = Fin.colors.mint, fg = Fin.colors.onMint)
            else -> Monogram(tx.merchant)
        }
        Column(Modifier.weight(1f)) {
            Text(tx.merchant, style = MaterialTheme.typography.titleSmall, color = Fin.colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val time = (subtitlePrefix?.let { "$it " } ?: "") + tx.timeText(zone)
            if (uncategorized && !income) {
                Text("Sin categoría · tócalo para asignar", style = MaterialTheme.typography.bodySmall, color = Fin.colors.amber)
            } else {
                Text("${tx.category ?: Categories.UNCATEGORIZED} · $time", style = MaterialTheme.typography.bodySmall, color = Fin.colors.inkSoft)
            }
        }
        Text(
            formatSigned(tx.amountCents),
            style = MaterialTheme.typography.titleSmall,
            color = if (income) Fin.colors.accent else Fin.colors.ink
        )
    }
}

private sealed interface EditTarget {
    data object New : EditTarget
    data class Existing(val tx: TransactionEntity) : EditTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementsScreen(viewModel: TransactionsViewModel) {
    val items by viewModel.transactions.collectAsState()
    var filter by remember { mutableIntStateOf(0) }
    var target by remember { mutableStateOf<EditTarget?>(null) }
    var deleting by remember { mutableStateOf<TransactionEntity?>(null) }

    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val thisMonth = items.filter { val d = it.date(zone); d.year == today.year && d.month == today.month }
    val spent = thisMonth.filter { it.amountCents < 0 }.sumOf { -it.amountCents }
    val income = thisMonth.filter { it.amountCents > 0 }.sumOf { it.amountCents }
    val shown = when (filter) {
        1 -> items.filter { it.amountCents < 0 }
        2 -> items.filter { it.amountCents > 0 }
        else -> items
    }
    val groups = shown.groupBy { it.date(zone) }.toList().sortedByDescending { it.first }

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Column {
            ScreenHeader("Movimientos") {
                PillButton("Agregar", onClick = { target = EditTarget.New }, icon = FinIcons.Plus, height = 44.dp)
            }
            Text(
                "${today.month.getDisplayName(java.time.format.TextStyle.FULL, Es).capitalized()}: gastaste ${formatMoney(spent)} y entraron ${formatMoney(income)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Fin.colors.inkSoft,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp)
            )
            Segmented(
                listOf("Todos", "Gastos", "Ingresos"), filter, { filter = it },
                Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
            )
        } }
        if (groups.isEmpty()) {
            item {
                Text(
                    "Aún no hay movimientos. Cuando llegue un SMS o una notificación de tu banco aparecerá aquí; " +
                        "también puedes agregarlos a mano.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Fin.colors.inkSoft,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
        items(groups, key = { it.first.toString() }) { (date, txs) ->
            Column {
                GroupLabel(dayLabel(date, today))
                Sheet(Modifier.padding(horizontal = 16.dp), radius = 20.dp, padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    txs.forEach { tx -> TxRow(tx) { target = EditTarget.Existing(tx) } }
                }
            }
        }
    }

    target?.let { t ->
        ModalBottomSheet(
            onDismissRequest = { target = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Fin.colors.card
        ) {
            TxEditor(
                initial = (t as? EditTarget.Existing)?.tx,
                onSave = { cents, merchant, isIncome, category, applyRule ->
                    val tx = (t as? EditTarget.Existing)?.tx
                    if (tx == null) viewModel.addManual(cents, merchant, isIncome, category)
                    else viewModel.saveEdit(tx, cents, merchant, isIncome, category, applyRule)
                    target = null
                },
                onDelete = { tx -> target = null; deleting = tx },
                onClose = { target = null }
            )
        }
    }

    deleting?.let { tx ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Eliminar movimiento") },
            text = { Text("Se borrará \"${tx.merchant}\" por ${formatMoney(tx.amountCents)}. Si había descontado una deuda, ese monto vuelve a su saldo.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(tx); deleting = null }) { Text("Eliminar", color = Fin.colors.brick) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun TxEditor(
    initial: TransactionEntity?,
    onSave: (cents: Long, merchant: String, isIncome: Boolean, category: String, applyRule: Boolean) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
    onClose: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    var isIncome by remember { mutableStateOf((initial?.amountCents ?: -1L) > 0) }
    var amount by remember { mutableStateOf(initial?.let { (abs(it.amountCents) / 100).toString() } ?: "") }
    var merchant by remember { mutableStateOf(initial?.merchant ?: "") }
    var category by remember {
        mutableStateOf(initial?.category?.takeIf { it != Categories.UNCATEGORIZED } ?: if (isIncome) Categories.INCOME else "Otros")
    }
    var applyRule by remember { mutableStateOf(true) }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(initial?.merchant ?: "Agregar movimiento", style = MaterialTheme.typography.headlineSmall, color = Fin.colors.ink)
                if (initial != null) {
                    val how = when (initial.source) {
                        "SMS" -> "capturado de un SMS"
                        "NOTIF" -> "capturado de una notificación"
                        else -> "agregado a mano"
                    }
                    Text(
                        dayLabel(initial.date(zone), LocalDate.now(zone)) + ", " + initial.timeText(zone) + " · " + how,
                        style = MaterialTheme.typography.bodySmall,
                        color = Fin.colors.inkSoft
                    )
                }
            }
            QuietButton("Cerrar", onClick = onClose)
        }

        Segmented(listOf("Gasto", "Ingreso"), if (isIncome) 1 else 0, {
            isIncome = it == 1
            if (isIncome && category == "Otros") category = Categories.INCOME
            if (!isIncome && category == Categories.INCOME) category = "Otros"
        })

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FinField("Monto (COP)", amount, { amount = it.filter(Char::isDigit) }, Modifier.weight(1f), keyboard = KeyboardType.Number, big = true)
            FinField("Detalle", merchant, { merchant = it }, Modifier.weight(1f))
        }

        Text("Categoría", style = MaterialTheme.typography.titleSmall, color = Fin.colors.ink)
        ChoiceGrid(Categories.ALL, category, { category = it })

        if (initial != null && merchant.isNotBlank()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clickable(role = Role.Checkbox) { applyRule = !applyRule },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = applyRule,
                    onCheckedChange = { applyRule = it },
                    colors = CheckboxDefaults.colors(checkedColor = Fin.colors.accent)
                )
                Text("Usar siempre esta categoría para ${merchant.trim()}", style = MaterialTheme.typography.bodyMedium, color = Fin.colors.ink)
            }
        }

        Spacer(Modifier.height(4.dp))
        PillButton(
            if (initial == null) "Guardar movimiento" else "Guardar cambios",
            onClick = { onSave(pesosToCents(amount), merchant, isIncome, category, applyRule && initial != null) },
            enabled = pesosToCents(amount) > 0,
            modifier = Modifier.fillMaxWidth(),
            height = 52.dp
        )
        if (initial != null) {
            QuietButton("Eliminar movimiento", onClick = { onDelete(initial) }, color = Fin.colors.brick, modifier = Modifier.fillMaxWidth())
        }
    }
}
