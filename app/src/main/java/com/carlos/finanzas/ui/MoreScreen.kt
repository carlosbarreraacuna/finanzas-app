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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Pestaña "Más": agrupa Salud y Ajustes. */
@Composable
fun MoreScreen(health: HealthViewModel, settings: SettingsViewModel) {
    var section by rememberSaveable { mutableIntStateOf(0) }
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Salud", "Ajustes").forEachIndexed { i, label ->
                if (section == i) {
                    Button(onClick = { section = i }, modifier = Modifier.weight(1f)) { Text(label) }
                } else {
                    OutlinedButton(onClick = { section = i }, modifier = Modifier.weight(1f)) { Text(label) }
                }
            }
        }
        if (section == 0) HealthScreen(health) else SettingsScreen(settings)
    }
}
