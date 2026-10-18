package com.carlos.finanzas.debt

import com.carlos.finanzas.debt.DebtCalculator.DebtInput
import com.carlos.finanzas.debt.DebtCalculator.Strategy
import java.time.LocalDate
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

class DebtCalculatorTest {
    @Test
    fun `tasa cero da interes cero`() {
        assertEquals(0.0, DebtCalculator.monthlyRate(0.0), 1e-12)
    }

    @Test
    fun `tasa efectiva anual se convierte a mensual`() {
        assertEquals(0.01, DebtCalculator.monthlyRate(12.682503), 1e-6)
    }

    @Test
    fun `sin interes se paga en cuotas exactas`() {
        val p = DebtCalculator.project(40_000_00, 0.0, 10_000_00)
        assertEquals(4, p.months!!)
        assertEquals(0L, p.totalInterestCents)
    }

    @Test
    fun `prestamo con interes termina y paga interes`() {
        val p = DebtCalculator.project(120_000_000, 12.682503, 20_000_000)
        assertEquals(7, p.months!!)
        assertTrue(p.totalInterestCents in 4_000_000..4_800_000)
    }

    @Test
    fun `cuota que no cubre el interes nunca salda`() {
        val p = DebtCalculator.project(100_000_000, 36.0, 100_000)
        assertTrue(p.neverPaysOff)
        assertNull(p.months)
    }

    @Test
    fun `abono extra reduce meses e interes`() {
        val base = DebtCalculator.project(120_000_000, 20.0, 15_000_000)
        val extra = DebtCalculator.project(120_000_000, 20.0, 15_000_000, extraMonthlyCents = 5_000_000)
        assertTrue(extra.months!! < base.months!!)
        assertTrue(extra.totalInterestCents < base.totalInterestCents)
    }

    @Test
    fun `avalancha no paga mas interes que bola de nieve ni que solo minimos`() {
        val debts = listOf(
            DebtInput(1, "A", 100_000_000, 30.0, 3_000_000),
            DebtInput(2, "B", 20_000_000, 10.0, 1_000_000)
        )
        val min = DebtCalculator.simulate(debts, 10_000_000, Strategy.MINIMUMS_ONLY)
        val snow = DebtCalculator.simulate(debts, 10_000_000, Strategy.SNOWBALL)
        val aval = DebtCalculator.simulate(debts, 10_000_000, Strategy.AVALANCHE)
        assertTrue(aval.totalInterestCents <= snow.totalInterestCents)
        assertTrue(snow.totalInterestCents <= min.totalInterestCents)
    }

    @Test
    fun `bola de nieve paga primero la deuda mas pequena`() {
        val debts = listOf(
            DebtInput(1, "A", 100_000_000, 30.0, 3_000_000),
            DebtInput(2, "B", 20_000_000, 10.0, 1_000_000)
        )
        val r = DebtCalculator.simulate(debts, 10_000_000, Strategy.SNOWBALL)
        assertEquals("B", r.payoffOrder.first())
    }

    @Test
    fun `proxima fecha de pago`() {
        assertEquals(LocalDate.of(2025, 10, 20), DebtDates.nextDueDate(LocalDate.of(2025, 10, 18), 20))
        assertEquals(LocalDate.of(2025, 11, 10), DebtDates.nextDueDate(LocalDate.of(2025, 10, 18), 10))
        assertEquals(LocalDate.of(2025, 11, 30), DebtDates.nextDueDate(LocalDate.of(2025, 11, 5), 31))
        assertNull(DebtDates.nextDueDate(LocalDate.of(2025, 10, 18), 0))
        assertEquals(LocalDate.of(2025, 10, 18), DebtDates.nextDueDate(LocalDate.of(2025, 10, 18), 18))
    }
}
