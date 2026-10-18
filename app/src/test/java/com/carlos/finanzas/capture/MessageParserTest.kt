package com.carlos.finanzas.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MessageParserTest {
    private val parser = MessageParser()

    @Test
    fun `parsea montos en distintos formatos`() {
        assertEquals(123_456_789L, parser.parseAmountToCents("1.234.567,89"))
        assertEquals(123_450L, parser.parseAmountToCents("1,234.50"))
        assertEquals(2_500_000L, parser.parseAmountToCents("25000"))
        assertEquals(2_500_000L, parser.parseAmountToCents("25.000"))
        assertEquals(1_250L, parser.parseAmountToCents("12.5"))
        assertEquals(2_500_000L, parser.parseAmountToCents("25.000.")) // punto final de la oración
        assertNull(parser.parseAmountToCents("abc"))
    }

    @Test
    fun `compra genera gasto negativo con comercio`() {
        val p = parser.parse("Compra por \$25.000 en Rappi.")!!
        assertEquals(-2_500_000L, p.amountCents)
        assertEquals("Rappi", p.merchant)
    }

    @Test
    fun `transferencia recibida es ingreso`() {
        val p = parser.parse("Recibiste \$1.500.000 de tu empresa")!!
        assertEquals(150_000_000L, p.amountCents)
    }

    @Test
    fun `mensaje no reconocido devuelve null`() {
        assertNull(parser.parse("Tu código de verificación es 123456"))
    }
}
