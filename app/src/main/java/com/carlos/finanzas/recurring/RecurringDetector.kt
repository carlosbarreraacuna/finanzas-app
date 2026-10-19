package com.carlos.finanzas.recurring

import kotlin.math.abs

/** Detecta cargos que se repiten (suscripciones, cuotas, servicios) a partir del historial de gastos. */
object RecurringDetector {
    private const val DAY_MS = 86_400_000L

    /** Un gasto; [amountCents] siempre positivo. */
    data class Charge(val merchantKey: String, val merchant: String, val timestamp: Long, val amountCents: Long)

    data class Recurring(
        val merchantKey: String,
        val merchant: String,
        /** Días típicos entre cobros (mediana). */
        val periodDays: Int,
        val typicalCents: Long,
        val lastCents: Long,
        val lastTimestamp: Long,
        val nextTimestamp: Long,
        val count: Int,
        /** El último cobro difiere más de 5 % de lo habitual. */
        val priceChanged: Boolean,
        val previousCents: Long,
        /** Costo aproximado por mes. */
        val monthlyCents: Long
    )

    private fun <T : Comparable<T>> List<T>.median(): T = sorted()[size / 2]

    fun detect(charges: List<Charge>, now: Long): List<Recurring> =
        charges.groupBy { it.merchantKey }.mapNotNull { (key, list) -> analyze(key, list, now) }
            .sortedByDescending { it.monthlyCents }

    private fun analyze(key: String, list: List<Charge>, now: Long): Recurring? {
        // Un cobro por día: SMS y notificación duplicados no cuentan dos veces.
        val byDay = list.sortedBy { it.timestamp }.distinctBy { it.timestamp / DAY_MS }
        if (byDay.size < 3) return null

        val gaps = byDay.zipWithNext { a, b -> (b.timestamp / DAY_MS) - (a.timestamp / DAY_MS) }
        val median = gaps.median()
        if (median !in 6L..8L && median !in 13L..16L && median !in 26L..35L) return null

        val tolerance = maxOf(2.0, median * 0.2)
        val regular = gaps.count { abs(it - median) <= tolerance }
        if (regular * 10 < gaps.size * 7) return null

        val last = byDay.last()
        if (now - last.timestamp > 2 * median * DAY_MS) return null // ya no se repite

        val amounts = byDay.map { it.amountCents }
        val typical = amounts.median()
        val previous = amounts.dropLast(1).median()
        val changed = abs(last.amountCents - previous) * 100 > previous * 5

        return Recurring(
            merchantKey = key,
            merchant = last.merchant,
            periodDays = median.toInt(),
            typicalCents = typical,
            lastCents = last.amountCents,
            lastTimestamp = last.timestamp,
            nextTimestamp = last.timestamp + median * DAY_MS,
            count = byDay.size,
            priceChanged = changed,
            previousCents = previous,
            monthlyCents = typical * 30 / median
        )
    }
}
