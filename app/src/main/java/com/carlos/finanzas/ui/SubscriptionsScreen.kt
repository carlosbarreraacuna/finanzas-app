package com.carlos.finanzas.ui

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.carlos.finanzas.FinanzasApp
import com.carlos.finanzas.capture.Categories
import com.carlos.finanzas.capture.MerchantNormalizer
import com.carlos.finanzas.recurring.RecurringDetector
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SubscriptionsViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as FinanzasApp).db

    val recurring: StateFlow<List<RecurringDetector.Recurring>> =
        db.transactions().recent(3000).map { txs ->
            val charges = txs.filter {
                it.amountCents < 0 && it.category != Categories.DEBTS &&
                    MerchantNormalizer.isLearnable(MerchantNormalizer.key(it.merchant))
            }.map {
                RecurringDetector.Charge(MerchantNormalizer.key(it.merchant), it.merchant, it.timestamp, abs(it.amountCents))
            }
            RecurringDetector.detect(charges, System.currentTimeMillis())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

private fun periodLabel(days: Int) = when (days) {
    in 6..8 -> "cada semana"
    in 13..16 -> "cada 2 semanas"
    else -> "cada mes"
}

@Composable
fun SubscriptionsScreen(viewModel: SubscriptionsViewModel = viewModel()) {
    val items by viewModel.recurring.collectAsState()
    val total = items.sumOf { it.monthlyCents }
    val zone = ZoneId.systemDefault()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Cargos recurrentes", style = MaterialTheme.typography.headlineSmall) }
        if (items.isEmpty()) {
            item {
                Text(
                    "Todavía no hay cargos que se repitan. Aparecen cuando un mismo comercio te cobra al menos " +
                        "3 veces con un ritmo parecido (semanal, quincenal o mensual)."
                )
            }
            return@LazyColumn
        }
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Pagas cada mes, aproximadamente", style = MaterialTheme.typography.bodyMedium)
                    Text(formatMoney(total), style = MaterialTheme.typography.displaySmall)
                    Text("en ${items.size} cargos que se repiten", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        items(items, key = { it.merchantKey }) { r ->
            val next = Instant.ofEpochMilli(r.nextTimestamp).atZone(zone).toLocalDate()
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(r.merchant, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(formatMoney(r.lastCents))
                    }
                    Text(
                        "${periodLabel(r.periodDays)} · ${r.count} cobros · próximo hacia el ${next.format(dayMonthFormat)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (r.priceChanged) {
                        Text(
                            "Cambió de precio: antes ${formatMoney(r.previousCents)}, ahora ${formatMoney(r.lastCents)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
