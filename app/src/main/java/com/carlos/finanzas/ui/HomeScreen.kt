package com.carlos.finanzas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carlos.finanzas.ui.theme.Fin
import java.time.LocalDate

@Composable
fun HomeScreen(
    debtViewModel: DebtViewModel,
    budgetViewModel: BudgetViewModel,
    transactionsViewModel: TransactionsViewModel,
    inboxViewModel: InboxViewModel,
    healthViewModel: HealthViewModel,
    onDebts: () -> Unit,
    onBudget: () -> Unit,
    onMovements: () -> Unit,
    onPending: () -> Unit,
    onCapture: () -> Unit
) {
    val debts by debtViewModel.debts.collectAsState()
    val budget by budgetViewModel.state.collectAsState()
    val txs by transactionsViewModel.transactions.collectAsState()
    val pending by inboxViewModel.pending.collectAsState()
    val health by healthViewModel.state.collectAsState()
    val plan = remember(debts) { planFor(debts) }
    val today = LocalDate.now()

    Column(Modifier.verticalScroll(rememberScrollState())) {
        // Encabezado: la fecha en que quedas libre de deudas.
        Column(
            Modifier
                .fillMaxWidth()
                .background(Fin.colors.field)
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 56.dp)
        ) {
            Text(
                today.format(weekdayDayMonth).capitalized(),
                style = MaterialTheme.typography.bodyMedium,
                color = Fin.colors.onFieldSoft,
                modifier = Modifier.padding(bottom = 18.dp)
            )
            val months = plan.months
            when {
                plan.oweCount == 0 -> {
                    Text("Sin deudas", style = MaterialTheme.typography.displayLarge, color = Fin.colors.onField)
                    Text(
                        "Registra tus tarjetas y préstamos para saber cuándo quedas libre y cuánto interés pagas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Fin.colors.onField.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 14.dp).widthIn(max = 320.dp)
                    )
                }
                plan.neverPaysOff || months == null -> {
                    Text("Con tus cuotas actuales", style = MaterialTheme.typography.bodyLarge, color = Fin.colors.onFieldSoft)
                    Text("no terminas de pagar", style = MaterialTheme.typography.headlineLarge, color = Fin.colors.onField)
                    Text(
                        "Las cuotas no alcanzan a cubrir el interés. Debes ${formatMoney(plan.oweCents)}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Fin.colors.onField.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 14.dp).widthIn(max = 320.dp)
                    )
                }
                else -> {
                    Text("Libre de deudas en", style = MaterialTheme.typography.bodyLarge, color = Fin.colors.onFieldSoft)
                    Text(monthYearText(today.plusMonths(months.toLong())), style = MaterialTheme.typography.displayLarge, color = Fin.colors.onField)
                    Text(
                        "Pagando ${formatMoney(plan.monthlyCents)} al mes y abonando primero a la tasa más alta. Debes ${formatMoney(plan.oweCents)}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Fin.colors.onField.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 14.dp).widthIn(max = 320.dp)
                    )
                }
            }
            PillButton(
                if (plan.oweCount == 0) "Registrar una deuda" else "Ver plan de deudas",
                onClick = onDebts,
                container = Fin.colors.onField,
                content = Fin.colors.field,
                height = 44.dp,
                modifier = Modifier.padding(top = 18.dp)
            )
        }

        Column(Modifier.offset(y = (-28).dp)) {
            // Presupuesto del periodo.
            val summary = budget.summary
            Sheet(
                Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button, onClick = onBudget),
                padding = PaddingValues(20.dp)
            ) {
                if (summary != null && budget.limitCents > 0) {
                    Text(
                        if (summary.overBudget) "Te pasaste este periodo" else "Te quedan este periodo",
                        style = MaterialTheme.typography.bodySmall,
                        color = Fin.colors.inkSoft
                    )
                    Text(
                        formatMoney(kotlin.math.abs(summary.remainingCents)),
                        style = MaterialTheme.typography.headlineLarge,
                        color = if (summary.overBudget) Fin.colors.brick else Fin.colors.ink
                    )
                    Bar(
                        summary.spentCents.toFloat() / budget.limitCents,
                        if (summary.overBudget) Fin.colors.brick else Fin.colors.accent,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                    Text(
                        "Gastaste ${formatMoney(summary.spentCents)} de ${formatMoney(budget.limitCents)}. " +
                            if (summary.daysLeft == 1) "El pago llega mañana." else "El pago llega en ${summary.daysLeft} días.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Fin.colors.inkSoft
                    )
                } else {
                    Text("Gastaste este periodo", style = MaterialTheme.typography.bodySmall, color = Fin.colors.inkSoft)
                    Text(formatMoney(summary?.spentCents ?: 0L), style = MaterialTheme.typography.headlineLarge, color = Fin.colors.ink)
                    Text("Define un presupuesto para saber cuánto te queda.", style = MaterialTheme.typography.bodySmall, color = Fin.colors.accent, modifier = Modifier.padding(top = 6.dp))
                }
            }

            val p = health.permissions
            if (!p.notificationAccess || !p.listenerConnected) {
                Banner(
                    text = if (!p.notificationAccess) "La captura no tiene permiso" else "La captura está desconectada",
                    action = "Revisar",
                    bg = Fin.colors.brickTint,
                    fg = Fin.colors.onBrickTint,
                    onClick = onCapture
                )
            }
            if (pending.isNotEmpty()) {
                Banner(
                    text = if (pending.size == 1) "1 mensaje por revisar" else "${pending.size} mensajes por revisar",
                    action = "Revisar",
                    bg = Fin.colors.amberTint,
                    fg = Fin.colors.onAmberTint,
                    onClick = onPending
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Últimos movimientos", style = MaterialTheme.typography.titleLarge, color = Fin.colors.ink)
                QuietButton("Ver todos", onClick = onMovements)
            }
            Sheet(Modifier.padding(horizontal = 16.dp), radius = 20.dp, padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                if (txs.isEmpty()) {
                    Text("Aún no hay movimientos.", style = MaterialTheme.typography.bodyMedium, color = Fin.colors.inkSoft, modifier = Modifier.padding(vertical = 16.dp))
                }
                txs.take(3).forEach { TxRow(it, onClick = onMovements) }
            }
        }
    }
}

@Composable
private fun Banner(text: String, action: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg, modifier = Modifier.weight(1f))
        Text(action, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = fg)
    }
}
