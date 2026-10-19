package com.carlos.finanzas

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.carlos.finanzas.capture.ListenerKeepAlive
import com.carlos.finanzas.ui.BudgetScreen
import com.carlos.finanzas.ui.BudgetViewModel
import com.carlos.finanzas.ui.DebtScreen
import com.carlos.finanzas.ui.DebtViewModel
import com.carlos.finanzas.ui.HealthViewModel
import com.carlos.finanzas.ui.InboxScreen
import com.carlos.finanzas.ui.InboxViewModel
import com.carlos.finanzas.ui.MoreScreen
import com.carlos.finanzas.ui.SettingsViewModel
import com.carlos.finanzas.ui.TransactionsScreen
import com.carlos.finanzas.ui.TransactionsViewModel
import com.carlos.finanzas.ui.theme.FinanzasTheme

class MainActivity : ComponentActivity() {
    private val healthViewModel: HealthViewModel by viewModels()
    private val transactionsViewModel: TransactionsViewModel by viewModels()
    private val inboxViewModel: InboxViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val budgetViewModel: BudgetViewModel by viewModels()
    private val debtViewModel: DebtViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { healthViewModel.refreshPermissions() }

            LaunchedEffect(Unit) {
                val perms = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
                launcher.launch(perms.toTypedArray())
            }

            val tabs = listOf("Movimientos", "Presupuesto", "Deudas", "Sin reconocer", "Más")
            var tab by remember { mutableIntStateOf(0) }

            FinanzasTheme {
                Surface(Modifier.fillMaxSize()) {
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                tabs.forEachIndexed { i, label ->
                                    NavigationBarItem(
                                        selected = tab == i,
                                        onClick = { tab = i },
                                        icon = {},
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    ) { padding ->
                        Column(Modifier.padding(padding)) {
                            when (tab) {
                                0 -> TransactionsScreen(transactionsViewModel)
                                1 -> BudgetScreen(budgetViewModel)
                                2 -> DebtScreen(debtViewModel)
                                3 -> InboxScreen(inboxViewModel)
                                else -> MoreScreen(healthViewModel, settingsViewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ListenerKeepAlive.ensureBound(this, "resume")
        healthViewModel.refreshPermissions()
    }
}
