package com.carlos.finanzas.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TemplateBuilderTest {

    private fun rangeOf(text: String, part: String): TemplateBuilder.Range {
        val i = text.indexOf(part)
        require(i >= 0) { "'$part' no está en el texto" }
        return TemplateBuilder.Range(i, i + part.length)
    }

    private fun parserWith(pattern: String, isIncome: Boolean = false) =
        MessageParser(listOf(MessageParser.Template("t", TemplateBuilder.compile(pattern)!!, isIncome)))

    @Test
    fun `genera plantilla con monto y comercio y la reutiliza en otros mensajes`() {
        val text = "Bancolombia: Compra por \$25.000 en TIENDA D1 el 18/10."
        val pattern = TemplateBuilder.build(text, rangeOf(text, "25.000"), rangeOf(text, "TIENDA D1"))
        assertNotNull(pattern)

        val parser = parserWith(pattern!!)
        val first = parser.parse(text)!!
        assertEquals(-2_500_000L, first.amountCents)
        assertEquals("TIENDA D1", first.merchant)

        val other = parser.parse("Bancolombia: Compra por \$1.200.500 en EXITO CENTRO el 19/10.")!!
        assertEquals(-120_050_000L, other.amountCents)
        assertEquals("EXITO CENTRO", other.merchant)
    }

    @Test
    fun `solo monto`() {
        val text = "Nequi: recibiste \$50.000 de Juan"
        val pattern = TemplateBuilder.build(text, rangeOf(text, "50.000"), null)!!
        val parsed = parserWith(pattern, isIncome = true).parse(text)!!
        assertEquals(5_000_000L, parsed.amountCents)
        assertEquals("Desconocido", parsed.merchant)
    }

    @Test
    fun `el simbolo de pesos dentro de la seleccion se trata como literal`() {
        val text = "Pago de \$80.000 realizado"
        val pattern = TemplateBuilder.build(text, rangeOf(text, "\$80.000"), null)
        assertNotNull(pattern)
        assertEquals(-8_000_000L, parserWith(pattern!!).parse(text)!!.amountCents)
    }

    @Test
    fun `comercio al final del mensaje`() {
        val text = "Compra aprobada por \$9.900 en CAFE ALBA"
        val pattern = TemplateBuilder.build(text, rangeOf(text, "9.900"), rangeOf(text, "CAFE ALBA"))!!
        val parsed = parserWith(pattern).parse("Compra aprobada por \$12.300 en PANADERIA LA 80")!!
        assertEquals("PANADERIA LA 80", parsed.merchant)
        assertEquals(-1_230_000L, parsed.amountCents)
    }

    @Test
    fun `rangos invalidos devuelven null`() {
        val text = "Compra por \$1.000"
        assertNull(TemplateBuilder.build(text, TemplateBuilder.Range(5, 5), null))
        assertNull(TemplateBuilder.build(text, TemplateBuilder.Range(0, 99), null))
        assertNull(TemplateBuilder.build(text, rangeOf(text, "Compra"), null)) // sin dígitos
    }

    @Test
    fun `monto y comercio solapados devuelven null`() {
        val text = "Compra por \$1.000 en TIENDA"
        val amount = rangeOf(text, "1.000")
        assertNull(TemplateBuilder.build(text, amount, TemplateBuilder.Range(amount.start, amount.end + 3)))
    }
}
