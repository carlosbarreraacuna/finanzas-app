package com.carlos.finanzas.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategorizerTest {

    @Test
    fun `normaliza comercios quitando acentos, simbolos y numeros de local`() {
        assertEquals("rappi col", MerchantNormalizer.key("RAPPI*COL 123"))
        assertEquals("exito poblado", MerchantNormalizer.key("Éxito Poblado"))
        assertEquals("tienda d1", MerchantNormalizer.key("TIENDA D1"))
        assertEquals("", MerchantNormalizer.key("   "))
    }

    @Test
    fun `comercio desconocido o vacio no es aprendible`() {
        assertFalse(MerchantNormalizer.isLearnable(MerchantNormalizer.key("Desconocido")))
        assertFalse(MerchantNormalizer.isLearnable(""))
        assertTrue(MerchantNormalizer.isLearnable("rappi col"))
    }

    @Test
    fun `la regla del usuario tiene prioridad sobre todo`() {
        assertEquals("Ocio", Categorizer.suggest("rappi col", -100, "Ocio"))
        assertEquals("Ocio", Categorizer.suggest("rappi col", 100, "Ocio"))
    }

    @Test
    fun `monto positivo es ingreso`() {
        assertEquals(Categories.INCOME, Categorizer.suggest("empresa sa", 5_000, null))
    }

    @Test
    fun `palabras clave por defecto`() {
        assertEquals("Alimentación", Categorizer.suggest("rappi col", -100, null))
        assertEquals("Alimentación", Categorizer.suggest("tienda d1", -100, null))
        assertEquals("Transporte", Categorizer.suggest("uber trip", -100, null))
        assertEquals("Suscripciones", Categorizer.suggest("netflix com", -100, null))
    }

    @Test
    fun `las palabras clave coinciden por palabra completa`() {
        // "ara" no debe coincidir dentro de "para" ni "arabica"
        assertNull(Categorizer.suggest("pago para tienda", -100, null))
        assertNull(Categorizer.suggest("arabica gourmet", -100, null))
    }

    @Test
    fun `sin coincidencia devuelve null`() {
        assertNull(Categorizer.suggest("tienda xyz", -100, null))
        assertNull(Categorizer.suggest("", -100, null))
    }
}
