package com.carlos.finanzas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Pestaña "Más": cargos recurrentes, salud del servicio y ajustes. */
@Composable
fun MoreScreen(health: HealthViewModel, settings: SettingsViewModel) {
    var section by rememberSaveable { mutableIntStateOf(0) }
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Recurrentes", "Salud", "Ajustes").forEachIndexed { i, label ->
                val modifier = Modifier.weight(1f)
                if (section == i) {
                    Button(onClick = { section = i }, modifier = modifier) { Text(label, maxLines = 1) }
                } else {
                    OutlinedButton(onClick = { section = i }, modifier = modifier) { Text(label, maxLines = 1) }
                }
            }
        }
        when (section) {
            0 -> SubscriptionsScreen()
            1 -> HealthScreen(health)
            else -> SettingsScreen(settings)
        }
    }
}
