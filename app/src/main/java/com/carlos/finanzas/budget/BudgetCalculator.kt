package com.carlos.finanzas.budget

import com.carlos.finanzas.capture.Categories
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Cálculo del presupuesto por periodo de pago. No depende de Android. */
object BudgetCalculator {

    /** Periodo [start, endExclusive): desde el último día de pago hasta el siguiente. */
    data class Period(val start: LocalDate, val endExclusive: LocalDate)

    data class Spend(val timestamp: Long, val amountCents: Long, val category: String?)

    data class Summary(
        val period: Period,
        val limitCents: Long,
        val spentCents: Long,
        val remainingCents: Long,
        /** Días que faltan para el próximo pago, contando hoy (mínimo 1). */
        val daysLeft: Int,
        /** Lo que se puede gastar por día sin pasarse; 0 si ya se superó el presupuesto. */
        val perDayCents: Long,
        val overBudget: Boolean,
        /** Gasto del periodo por categoría, de mayor a menor. */
        val byCategory: List<Pair<String, Long>>
    )

    /** Si el mes no tiene ese día (ej. 31 en abril) se usa el último día del mes. */
    fun periodFor(today: LocalDate, payday: Int): Period {
        val day = payday.coerceIn(1, 31)
        fun payDate(ym: YearMonth) = ym.atDay(minOf(day, ym.lengthOfMonth()))

        val thisMonth = YearMonth.from(today)
        val thisMonthPay = payDate(thisMonth)
        val start = if (!today.isBefore(thisMonthPay)) thisMonthPay else payDate(thisMonth.minusMonths(1))
        val end = payDate(YearMonth.from(start).plusMonths(1))
        return Period(start, end)
    }

    fun summarize(
        limitCents: Long,
        spends: List<Spend>,
        today: LocalDate,
        payday: Int,
        zone: ZoneId = ZoneId.systemDefault()
    ): Summary {
        val period = periodFor(today, payday)
        val inPeriod = spends.filter {
            val date = Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
            it.amountCents < 0 && !date.isBefore(period.start) && date.isBefore(period.endExclusive)
        }
        val spent = inPeriod.sumOf { -it.amountCents }
        val remaining = limitCents - spent
        val daysLeft = maxOf(1L, ChronoUnit.DAYS.between(today, period.endExclusive)).toInt()
        val byCategory = inPeriod
            .groupBy { it.category ?: Categories.UNCATEGORIZED }
            .map { (category, items) -> category to items.sumOf { -it.amountCents } }
            .sortedByDescending { it.second }
        return Summary(
            period = period,
            limitCents = limitCents,
            spentCents = spent,
            remainingCents = remaining,
            daysLeft = daysLeft,
            perDayCents = if (remaining > 0) remaining / daysLeft else 0L,
            overBudget = limitCents > 0 && remaining < 0,
            byCategory = byCategory
        )
    }
}
