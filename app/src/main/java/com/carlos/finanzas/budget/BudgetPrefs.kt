package com.carlos.finanzas.budget

import android.content.Context

/** Presupuesto del periodo y día de pago. Se guardan aparte de la base de datos. */
class BudgetPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)

    /** Presupuesto de gasto por periodo, en centavos. 0 = sin definir. */
    var limitCents: Long
        get() = prefs.getLong(KEY_LIMIT, 0L)
        set(v) = prefs.edit().putLong(KEY_LIMIT, v).apply()

    /** Día del mes en que recibes tu pago (1-31). */
    var payday: Int
        get() = prefs.getInt(KEY_PAYDAY, 1)
        set(v) = prefs.edit().putInt(KEY_PAYDAY, v.coerceIn(1, 31)).apply()

    private companion object {
        const val KEY_LIMIT = "limit_cents"
        const val KEY_PAYDAY = "payday"
    }
}
