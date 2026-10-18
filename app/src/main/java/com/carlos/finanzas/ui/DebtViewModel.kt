package com.carlos.finanzas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.carlos.finanzas.FinanzasApp
import com.carlos.finanzas.data.DebtEntity
import com.carlos.finanzas.data.DebtPaymentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DebtViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as FinanzasApp).db

    val debts: StateFlow<List<DebtEntity>> =
        db.debts().observeActive()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val selected = MutableStateFlow<Long?>(null)

    /** Pagos de la deuda abierta en el detalle. */
    val payments: StateFlow<List<DebtPaymentEntity>> =
        selected.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else db.debtPayments().observeFor(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun select(id: Long?) {
        selected.value = id
    }

    fun save(debt: DebtEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (debt.id == 0L) db.debts().insert(debt) else db.debts().update(debt)
        }
    }

    /** Archiva la deuda: deja de mostrarse, pero sus pagos se conservan. */
    fun archive(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { db.debts().archive(id) }
    }

    fun recordPayment(debtId: Long, amountCents: Long, note: String) {
        if (amountCents <= 0) return
        viewModelScope.launch(Dispatchers.IO) {
            db.withTransaction {
                db.debtPayments().insert(
                    DebtPaymentEntity(
                        debtId = debtId,
                        timestamp = System.currentTimeMillis(),
                        amountCents = amountCents,
                        source = "MANUAL",
                        note = note
                    )
                )
                db.debts().applyPayment(debtId, amountCents)
            }
        }
    }

    /** Borra un pago y devuelve su monto al saldo. */
    fun deletePayment(payment: DebtPaymentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.withTransaction {
                db.debtPayments().delete(payment.id)
                db.debts().revertPayment(payment.debtId, payment.amountCents)
            }
        }
    }
}
