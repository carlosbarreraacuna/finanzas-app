package com.carlos.finanzas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

private val copFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
    maximumFractionDigits = 0
}

fun formatMoney(cents: Long): String = copFormat.format(cents / 100.0)

@Composable
fun TransactionsScreen(viewModel: TransactionsViewModel) {
    val items by viewModel.transactions.collectAsState()

    if (items.isEmpty()) {
        Column(Modifier.padding(16.dp)) {
            Text("Transacciones", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Aún no hay transacciones capturadas. Cuando llegue un SMS o una notificación " +
                    "bancaria reconocida aparecerá aquí.",
                Modifier.padding(top = 8.dp)
            )
        }
        return
    }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Text("Transacciones", style = MaterialTheme.typography.headlineSmall) }
        items(items, key = { it.id }) { tx ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(tx.merchant, style = MaterialTheme.typography.titleSmall)
                        Text(
                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(tx.timestamp)) +
                                " · " + tx.source,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        formatMoney(tx.amountCents),
                        color = if (tx.amountCents < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
