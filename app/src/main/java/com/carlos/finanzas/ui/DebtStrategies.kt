package com.carlos.finanzas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.carlos.finanzas.data.DebtEntity
import com.carlos.finanzas.debt.DebtCalculator
import com.carlos.finanzas.debt.DebtCalculator.Strategy
import java.time.LocalDate

/** Compara pagar solo mínimos, avalancha (mayor tasa primero) y bola de nieve (menor saldo primero). */
@Composable
fun DebtStrategiesCard(debts: List<DebtEntity>) {
    val owed = debts.filter { it.direction == "OWE" && it.balanceCents > 0 }
    if (owed.size < 2) return

    val minTotal = owed.sumOf { it.monthlyPaymentCents }
    var budgetText by remember { mutableStateOf("") }
    val budget = pesosToCents(budgetText).takeIf { it > 0 } ?: minTotal
    val inputs = owed.map {
        DebtCalculator.DebtInput(it.id, it.name, it.balanceCents, it.annualRatePct, it.monthlyPaymentCents)
    }
    val today = LocalDate.now()

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("¿Cómo pagar más rápido?", style = MaterialTheme.typography.titleMedium)
            Text(
                "Suma de tus cuotas mínimas: ${formatMoney(minTotal)}. Escribe cuánto puedes destinar " +
                    "en total cada mes a tus deudas.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = budgetText,
                onValueChange = { budgetText = it.filter(Char::isDigit) },
                label = { Text("Presupuesto mensual total (COP)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            if (budget < minTotal) {
                Text("Ese monto es menor que la suma de mínimos; se usan los mínimos.", style = MaterialTheme.typography.bodySmall)
            }
            val labels = mapOf(
                Strategy.MINIMUMS_ONLY to "Solo mínimos",
                Strategy.AVALANCHE to "Avalancha (mayor tasa primero)",
                Strategy.SNOWBALL to "Bola de nieve (menor saldo primero)"
            )
            Strategy.values().forEach { s ->
                val r = DebtCalculator.simulate(inputs, maxOf(budget, minTotal), s)
                val months = r.months
                Column {
                    Text(labels.getValue(s), style = MaterialTheme.typography.titleSmall)
                    if (r.neverPaysOff || months == null) {
                        Text("No se llega a saldar con este presupuesto.")
                    } else {
                        Text(
                            "Libre de deudas en $months meses (${today.plusMonths(months.toLong()).format(monthYearFormat)}), " +
                                "interés total ${formatMoney(r.totalInterestCents)}."
                        )
                        if (s != Strategy.MINIMUMS_ONLY && r.payoffOrder.isNotEmpty()) {
                            Text("Orden: ${r.payoffOrder.joinToString(" → ")}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
