package com.carlos.finanzas.recurring

import com.carlos.finanzas.recurring.RecurringDetector.Charge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringDetectorTest {
    private val day = 86_400_000L
    private val t0 = 1_700_000_000_000L

    private fun charge(key: String, dayOffset: Long, cents: Long) =
        Charge(key, key.uppercase(), t0 + dayOffset * day, cents)

    @Test
    fun `cuatro cobros mensuales se detectan`() {
        val list = listOf(0L, 30, 60, 90).map { charge("netflix", it, 3_000_000) }
        val r = RecurringDetector.detect(list, t0 + 100 * day)
        assertEquals(1, r.size)
        assertEquals(30, r[0].periodDays)
        assertEquals(3_000_000L, r[0].monthlyCents)
        assertFalse(r[0].priceChanged)
        assertEquals(t0 + 120 * day, r[0].nextTimestamp)
    }

    @Test
    fun `cobros semanales se detectan`() {
        val list = listOf(0L, 7, 14, 21).map { charge("gym", it, 1_000_000) }
        val r = RecurringDetector.detect(list, t0 + 25 * day)
        assertEquals(1, r.size)
        assertEquals(7, r[0].periodDays)
    }

    @Test
    fun `un aumento de precio se marca`() {
        val list = listOf(
            charge("spotify", 0, 2_000_000), charge("spotify", 30, 2_000_000),
            charge("spotify", 60, 2_000_000), charge("spotify", 90, 2_500_000)
        )
        val r = RecurringDetector.detect(list, t0 + 95 * day)
        assertTrue(r[0].priceChanged)
        assertEquals(2_000_000L, r[0].previousCents)
        assertEquals(2_500_000L, r[0].lastCents)
    }

    @Test
    fun `compras irregulares no son recurrentes`() {
        val list = listOf(0L, 3, 43, 52).map { charge("tienda", it, 500_000) }
        assertTrue(RecurringDetector.detect(list, t0 + 55 * day).isEmpty())
    }

    @Test
    fun `menos de tres cobros no alcanza`() {
        val list = listOf(0L, 30).map { charge("netflix", it, 3_000_000) }
        assertTrue(RecurringDetector.detect(list, t0 + 35 * day).isEmpty())
    }

    @Test
    fun `suscripcion que dejo de cobrarse se descarta`() {
        val list = listOf(0L, 30, 60, 90).map { charge("netflix", it, 3_000_000) }
        assertTrue(RecurringDetector.detect(list, t0 + 400 * day).isEmpty())
    }

    @Test
    fun `dos cobros el mismo dia cuentan uno`() {
        val list = listOf(
            charge("netflix", 0, 3_000_000), charge("netflix", 0, 3_000_000),
            charge("netflix", 30, 3_000_000)
        )
        assertTrue(RecurringDetector.detect(list, t0 + 35 * day).isEmpty())
    }
}
