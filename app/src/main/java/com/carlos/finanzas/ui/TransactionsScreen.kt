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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carlos.finanzas.capture.Categories
import com.carlos.finanzas.data.TransactionEntity
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
    var editing by remember { mutableStateOf<TransactionEntity?>(null) }

    editing?.let { tx ->
        CategoryDialog(
            tx = tx,
            onPick = { category, applyToMerchant ->
                viewModel.setCategory(tx, category, applyToMerchant)
                editing = null
            },
            onDismiss = { editing = null }
        )
    }

    if (items.isEmpty()) {
        Column(Modifier.padding(16.dp)) {
            Text("Movimientos", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Aún no hay movimientos capturados. Cuando llegue un SMS o una notificación " +
                    "bancaria reconocida aparecerá aquí.",
                Modifier.padding(top = 8.dp)
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Movimientos", style = MaterialTheme.typography.headlineSmall)
            Text("Toca un movimiento para cambiar su categoría.", style = MaterialTheme.typography.bodySmall)
        }
        items(items, key = { it.id }) { tx ->
            Card(Modifier.fillMaxWidth().clickable { editing = tx }) {
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
                        Text(
                            tx.category ?: Categories.UNCATEGORIZED,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
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

@Composable
private fun CategoryDialog(
    tx: TransactionEntity,
    onPick: (category: String, applyToMerchant: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var applyToMerchant by remember(tx.id) { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Categoría de ${tx.merchant}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = applyToMerchant, onCheckedChange = { applyToMerchant = it })
                    Text("Recordar para este comercio y aplicar a los anteriores")
                }
                Categories.ALL.forEach { category ->
                    TextButton(onClick = { onPick(category, applyToMerchant) }) {
                        Text(if (category == tx.category) "$category (actual)" else category)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
