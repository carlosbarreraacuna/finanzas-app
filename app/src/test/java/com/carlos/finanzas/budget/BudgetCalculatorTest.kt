package com.carlos.finanzas.budget

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetCalculatorTest {

    private fun ts(y: Int, m: Int, d: Int) =
        LocalDate.of(y, m, d).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun `hoy antes del dia de pago el periodo empieza en el pago del mes anterior`() {
        val p = BudgetCalculator.periodFor(LocalDate.of(2026, 10, 18), 30)
        assertEquals(LocalDate.of(2026, 9, 30), p.start)
        assertEquals(LocalDate.of(2026, 10, 30), p.endExclusive)
    }

    @Test
    fun `el mismo dia de pago empieza un periodo nuevo`() {
        val p = BudgetCalculator.periodFor(LocalDate.of(2026, 10, 30), 30)
        assertEquals(LocalDate.of(2026, 10, 30), p.start)
        assertEquals(LocalDate.of(2026, 11, 30), p.endExclusive)
    }

    @Test
    fun `dia de pago 31 se ajusta al ultimo dia de meses cortos`() {
        val feb = BudgetCalculator.periodFor(LocalDate.of(2026, 3, 5), 31)
        assertEquals(LocalDate.of(2026, 2, 28), feb.start)
        assertEquals(LocalDate.of(2026, 3, 31), feb.endExclusive)

        val apr = BudgetCalculator.periodFor(LocalDate.of(2026, 3, 31), 31)
        assertEquals(LocalDate.of(2026, 3, 31), apr.start)
        assertEquals(LocalDate.of(2026, 4, 30), apr.endExclusive)
    }

    @Test
    fun `resumen cuenta solo gastos del periodo y calcula el gasto diario`() {
        val spends = listOf(
            BudgetCalculator.Spend(ts(2026, 10, 10), -2_000_000, "Alimentación"),
            BudgetCalculator.Spend(ts(2026, 10, 17), -1_000_000, null),
            BudgetCalculator.Spend(ts(2026, 10, 12), 50_000_000, "Ingresos"), // ingreso: se ignora
            BudgetCalculator.Spend(ts(2026, 9, 29), -999, "Otros")            // fuera del periodo
        )
        val s = BudgetCalculator.summarize(
            limitCents = 10_000_000,
            spends = spends,
            today = LocalDate.of(2026, 10, 18),
            payday = 30,
            zone = ZoneOffset.UTC
        )
        assertEquals(3_000_000L, s.spentCents)
        assertEquals(7_000_000L, s.remainingCents)
        assertEquals(12, s.daysLeft)
        assertEquals(583_333L, s.perDayCents)
        assertFalse(s.overBudget)
        assertEquals(listOf("Alimentación" to 2_000_000L, "Sin categoría" to 1_000_000L), s.byCategory)
    }

    @Test
    fun `si se pasa del presupuesto el gasto diario es cero`() {
        val s = BudgetCalculator.summarize(
            limitCents = 1_000_000,
            spends = listOf(BudgetCalculator.Spend(ts(2026, 10, 10), -1_500_000, "Ocio")),
            today = LocalDate.of(2026, 10, 18),
            payday = 30,
            zone = ZoneOffset.UTC
        )
        assertTrue(s.overBudget)
        assertEquals(-500_000L, s.remainingCents)
        assertEquals(0L, s.perDayCents)
    }

    @Test
    fun `el ultimo dia antes del pago queda al menos un dia`() {
        val s = BudgetCalculator.summarize(
            limitCents = 1_000_000,
            spends = emptyList(),
            today = LocalDate.of(2026, 10, 29),
            payday = 30,
            zone = ZoneOffset.UTC
        )
        assertEquals(1, s.daysLeft)
        assertEquals(1_000_000L, s.perDayCents)
    }
}
