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

    /** Límite por categoría en centavos. Se guarda como "Categoría=centavos|…". */
    var categoryLimits: Map<String, Long>
        get() = prefs.getString(KEY_CATEGORY_LIMITS, "").orEmpty()
            .split("|")
            .mapNotNull { entry ->
                val i = entry.lastIndexOf('=')
                if (i <= 0) null else entry.substring(0, i) to (entry.substring(i + 1).toLongOrNull() ?: return@mapNotNull null)
            }
            .filter { it.second > 0 }
            .toMap()
        set(v) = prefs.edit().putString(
            KEY_CATEGORY_LIMITS,
            v.filter { it.value > 0 }.entries.joinToString("|") { "${it.key.replace("|", " ").replace("=", " ")}=${it.value}" }
        ).apply()

    private companion object {
        const val KEY_LIMIT = "limit_cents"
        const val KEY_PAYDAY = "payday"
        const val KEY_CATEGORY_LIMITS = "category_limits"
    }
}
