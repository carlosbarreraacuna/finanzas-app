package com.carlos.finanzas

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.carlos.finanzas.capture.ListenerKeepAlive
import com.carlos.finanzas.ui.BudgetScreen
import com.carlos.finanzas.ui.BudgetViewModel
import com.carlos.finanzas.ui.CaptureScreen
import com.carlos.finanzas.ui.DebtViewModel
import com.carlos.finanzas.ui.DebtsScreen
import com.carlos.finanzas.ui.FinIcons
import com.carlos.finanzas.ui.HealthViewModel
import com.carlos.finanzas.ui.HomeScreen
import com.carlos.finanzas.ui.InboxViewModel
import com.carlos.finanzas.ui.MoreScreen
import com.carlos.finanzas.ui.MovementsScreen
import com.carlos.finanzas.ui.PendingScreen
import com.carlos.finanzas.ui.RecurringScreen
import com.carlos.finanzas.ui.SettingsScreen
import com.carlos.finanzas.ui.SettingsViewModel
import com.carlos.finanzas.ui.SubScreen
import com.carlos.finanzas.ui.SubscriptionsViewModel
import com.carlos.finanzas.ui.TransactionsViewModel
import com.carlos.finanzas.ui.theme.Fin
import com.carlos.finanzas.ui.theme.FinanzasTheme

class MainActivity : ComponentActivity() {
    private val healthViewModel: HealthViewModel by viewModels()
    private val transactionsViewModel: TransactionsViewModel by viewModels()
    private val inboxViewModel: InboxViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val budgetViewModel: BudgetViewModel by viewModels()
    private val debtViewModel: DebtViewModel by viewModels()
    private val subscriptionsViewModel: SubscriptionsViewModel by viewModels()

    private data class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

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

            val tabs = listOf(
                Tab("Inicio", FinIcons.Home),
                Tab("Movimientos", FinIcons.Movements),
                Tab("Presupuesto", FinIcons.Budget),
                Tab("Deudas", FinIcons.Debts),
                Tab("Más", FinIcons.More)
            )
            var tab by rememberSaveable { mutableIntStateOf(0) }
            var subName by rememberSaveable { mutableStateOf<String?>(null) }
            val sub = subName?.let { SubScreen.valueOf(it) }
            fun open(s: SubScreen) { subName = s.name }
            fun close() { subName = null }

            FinanzasTheme {
                BackHandler(enabled = sub != null) { close() }
                BackHandler(enabled = sub == null && tab != 0) { tab = 0 }

                Scaffold(
                    containerColor = Fin.colors.ground,
                    bottomBar = {
                        if (sub == null) {
                            NavigationBar(containerColor = Fin.colors.card, tonalElevation = androidx.compose.ui.unit.Dp(0f)) {
                                tabs.forEachIndexed { i, t ->
                                    NavigationBarItem(
                                        selected = tab == i,
                                        onClick = { tab = i },
                                        icon = { Icon(t.icon, contentDescription = null) },
                                        label = {
                                            Text(
                                                t.label,
                                                maxLines = 1,
                                                softWrap = false,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Fin.colors.ink,
                                            selectedTextColor = Fin.colors.ink,
                                            indicatorColor = Fin.colors.mint,
                                            unselectedIconColor = Fin.colors.inkSoft,
                                            unselectedTextColor = Fin.colors.inkSoft
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        when (sub) {
                            SubScreen.PENDING -> PendingScreen(inboxViewModel, transactionsViewModel, onBack = { close() })
                            SubScreen.RECURRING -> RecurringScreen(subscriptionsViewModel, onBack = { close() })
                            SubScreen.CAPTURE -> CaptureScreen(healthViewModel, onBack = { close() })
                            SubScreen.SETTINGS -> SettingsScreen(settingsViewModel, onBack = { close() })
                            null -> when (tab) {
                                0 -> HomeScreen(
                                    debtViewModel, budgetViewModel, transactionsViewModel, inboxViewModel, healthViewModel,
                                    onDebts = { tab = 3 },
                                    onBudget = { tab = 2 },
                                    onMovements = { tab = 1 },
                                    onPending = { open(SubScreen.PENDING) },
                                    onCapture = { open(SubScreen.CAPTURE) }
                                )
                                1 -> MovementsScreen(transactionsViewModel)
                                2 -> BudgetScreen(budgetViewModel)
                                3 -> DebtsScreen(debtViewModel)
                                else -> MoreScreen(
                                    healthViewModel, settingsViewModel, subscriptionsViewModel, inboxViewModel,
                                    onOpen = { open(it) }
                                )
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
