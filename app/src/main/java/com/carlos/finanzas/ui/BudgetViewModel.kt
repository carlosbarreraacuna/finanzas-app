package com.carlos.finanzas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carlos.finanzas.FinanzasApp
import com.carlos.finanzas.budget.BudgetCalculator
import com.carlos.finanzas.budget.BudgetPrefs
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class BudgetUiState(
    val limitCents: Long = 0,
    val payday: Int = 1,
    val summary: BudgetCalculator.Summary? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = BudgetPrefs(app)
    private val transactions = (app as FinanzasApp).db.transactions()

    private data class Config(val limitCents: Long, val payday: Int)

    private val config = MutableStateFlow(Config(prefs.limitCents, prefs.payday))

    val state: StateFlow<BudgetUiState> = config.flatMapLatest { cfg ->
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val period = BudgetCalculator.periodFor(today, cfg.payday)
        val from = period.start.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = period.endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
        transactions.between(from, to).map { list ->
            val spends = list.map { BudgetCalculator.Spend(it.timestamp, it.amountCents, it.category) }
            BudgetUiState(
                limitCents = cfg.limitCents,
                payday = cfg.payday,
                summary = BudgetCalculator.summarize(cfg.limitCents, spends, today, cfg.payday, zone)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState())

    /** @param limitPesos presupuesto en pesos (sin decimales). */
    fun save(limitPesos: Long, payday: Int) {
        val limit = (limitPesos * 100).coerceAtLeast(0)
        val day = payday.coerceIn(1, 31)
        prefs.limitCents = limit
        prefs.payday = day
        config.value = Config(limit, day)
    }
}
