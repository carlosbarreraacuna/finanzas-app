package com.carlos.finanzas.debt

import com.carlos.finanzas.debt.DebtMatcher.Candidate
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class DebtMatcherTest {
    private val card = Candidate(1, "bancolombia tarjeta", owedByMe = true)

    @Test
    fun `coincide cuando todas las palabras estan en el texto`() {
        assertEquals(1L, DebtMatcher.find(listOf(card), "", "Pago TARJETA Bancolombia exitoso", -5_000_000)!!)
    }

    @Test
    fun `no coincide si falta una palabra`() {
        assertNull(DebtMatcher.find(listOf(card), "", "Pago Bancolombia exitoso", -5_000_000))
    }

    @Test
    fun `deuda propia exige monto negativo`() {
        assertNull(DebtMatcher.find(listOf(card), "", "tarjeta bancolombia", 5_000_000))
    }

    @Test
    fun `cobro exige monto positivo`() {
        val owed = Candidate(2, "juan", owedByMe = false)
        assertEquals(2L, DebtMatcher.find(listOf(owed), "Juan Perez", "", 5_000_000)!!)
        assertNull(DebtMatcher.find(listOf(owed), "Juan Perez", "", -5_000_000))
    }
}
