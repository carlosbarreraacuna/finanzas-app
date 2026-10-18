package com.carlos.finanzas.debt

import java.time.LocalDate
import java.time.YearMonth

/** Proyecciones de pago de deudas. Todo en centavos. No depende de Android. */
object DebtCalculator {

    /** Máximo de meses que se simulan (50 años); más allá se considera que no se paga. */
    const val MAX_MONTHS = 600

    data class Row(val month: Int, val interestCents: Long, val paymentCents: Long, val balanceCents: Long)

    /**
     * @property months meses hasta saldar; null si no se salda (la cuota no cubre el interés o pasa de [MAX_MONTHS]).
     */
    data class Projection(
        val months: Int?,
        val totalInterestCents: Long,
        val totalPaidCents: Long,
        val neverPaysOff: Boolean,
        val schedule: List<Row>
    )

    data class DebtInput(
        val id: Long,
        val name: String,
        val balanceCents: Long,
        val annualRatePct: Double,
        val minPaymentCents: Long
    )

    enum class Strategy { MINIMUMS_ONLY, AVALANCHE, SNOWBALL }

    data class StrategyResult(
        val strategy: Strategy,
        val months: Int?,
        val totalInterestCents: Long,
        val payoffOrder: List<String>,
        val neverPaysOff: Boolean
    )

    /** Tasa mensual equivalente a una tasa efectiva anual (EA) en %. */
    fun monthlyRate(annualPct: Double): Double =
        if (annualPct <= 0.0) 0.0 else Math.pow(1.0 + annualPct / 100.0, 1.0 / 12.0) - 1.0

    /**
     * Proyecta el pago de una deuda con cuota fija.
     * @param extraMonthlyCents abono extra que se suma a la cuota cada mes.
     * @param oneTimeExtraCents abono único que se aplica hoy al saldo.
     */
    fun project(
        balanceCents: Long,
        annualRatePct: Double,
        monthlyPaymentCents: Long,
        extraMonthlyCents: Long = 0,
        oneTimeExtraCents: Long = 0
    ): Projection {
        var balance = (balanceCents - oneTimeExtraCents).coerceAtLeast(0)
        if (balance == 0L) return Projection(0, 0, 0, false, emptyList())

        val rate = monthlyRate(annualRatePct)
        val payment = monthlyPaymentCents + extraMonthlyCents
        val rows = ArrayList<Row>()
        var totalInterest = 0L
        var totalPaid = 0L
        var month = 0

        while (balance > 0 && month < MAX_MONTHS) {
            month++
            val interest = Math.round(balance * rate)
            // Si la cuota no cubre el interés, el saldo nunca baja.
            if (payment <= interest) return Projection(null, totalInterest, totalPaid, true, rows)
            val pay = minOf(payment, balance + interest)
            balance = balance + interest - pay
            totalInterest += interest
            totalPaid += pay
            rows += Row(month, interest, pay, balance)
        }
        return if (balance > 0) {
            Projection(null, totalInterest, totalPaid, true, rows)
        } else {
            Projection(month, totalInterest, totalPaid, false, rows)
        }
    }

    /**
     * Simula pagar varias deudas a la vez.
     * - MINIMUMS_ONLY: cada deuda recibe solo su cuota mínima y los pagos liberados no se reasignan.
     * - AVALANCHE: el dinero sobrante va a la deuda de mayor tasa.
     * - SNOWBALL: el dinero sobrante va a la deuda de menor saldo.
     * En ambas, las cuotas de las deudas saldadas pasan a la siguiente. El presupuesto nunca baja
     * de la suma de cuotas mínimas.
     */
    fun simulate(debts: List<DebtInput>, monthlyBudgetCents: Long, strategy: Strategy): StrategyResult {
        class State(val input: DebtInput) {
            var balance: Long = input.balanceCents
            val rate: Double = monthlyRate(input.annualRatePct)
        }

        val states = debts.filter { it.balanceCents > 0 }.map { State(it) }
        if (states.isEmpty()) return StrategyResult(strategy, 0, 0, emptyList(), false)

        val sumMins = states.sumOf { it.input.minPaymentCents }
        val budget = if (strategy == Strategy.MINIMUMS_ONLY) sumMins else maxOf(monthlyBudgetCents, sumMins)

        var totalInterest = 0L
        val order = ArrayList<String>()
        var month = 0

        while (states.any { it.balance > 0 } && month < MAX_MONTHS) {
            month++

            for (s in states) {
                if (s.balance > 0) {
                    val interest = Math.round(s.balance * s.rate)
                    s.balance += interest
                    totalInterest += interest
                }
            }

            var spent = 0L
            for (s in states) {
                if (s.balance > 0) {
                    val pay = minOf(s.input.minPaymentCents, s.balance)
                    s.balance -= pay
                    spent += pay
                    if (s.balance == 0L) order += s.input.name
                }
            }

            if (strategy != Strategy.MINIMUMS_ONLY) {
                var extra = budget - spent
                val alive = states.filter { it.balance > 0 }
                val priority = when (strategy) {
                    Strategy.AVALANCHE ->
                        alive.sortedWith(compareByDescending<State> { it.input.annualRatePct }.thenBy { it.balance })
                    Strategy.SNOWBALL -> alive.sortedBy { it.balance }
                    Strategy.MINIMUMS_ONLY -> alive
                }
                for (s in priority) {
                    if (extra <= 0) break
                    val pay = minOf(extra, s.balance)
                    s.balance -= pay
                    extra -= pay
                    if (s.balance == 0L) order += s.input.name
                }
            }
        }

        val stuck = states.any { it.balance > 0 }
        return StrategyResult(strategy, if (stuck) null else month, totalInterest, order, stuck)
    }
}

object DebtDates {
    /**
     * Próxima fecha de pago (hoy cuenta). Si el mes no tiene ese día (ej. 31 en abril) se usa el último día.
     * @return null si [dueDay] no está entre 1 y 31.
     */
    fun nextDueDate(today: LocalDate, dueDay: Int): LocalDate? {
        if (dueDay !in 1..31) return null
        fun at(ym: YearMonth) = ym.atDay(minOf(dueDay, ym.lengthOfMonth()))
        val thisMonth = at(YearMonth.from(today))
        return if (!thisMonth.isBefore(today)) thisMonth else at(YearMonth.from(today).plusMonths(1))
    }
}
