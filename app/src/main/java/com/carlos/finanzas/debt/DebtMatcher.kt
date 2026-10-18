package com.carlos.finanzas.debt

import com.carlos.finanzas.capture.MerchantNormalizer

/** Decide si una transacción capturada es un pago de alguna de las deudas registradas. */
object DebtMatcher {

    /**
     * @property owedByMe true si la deuda es mía (el pago es un gasto); false si me deben (el pago es un ingreso).
     */
    data class Candidate(val id: Long, val keyword: String, val owedByMe: Boolean)

    /**
     * Una deuda coincide si todas las palabras de su palabra clave (normalizadas) aparecen en el
     * comercio o en el texto del mensaje, y el signo del monto corresponde al tipo de deuda.
     * @return el id de la primera deuda que coincide, o null.
     */
    fun find(candidates: List<Candidate>, merchant: String, text: String, amountCents: Long): Long? {
        val haystack = MerchantNormalizer.key("$merchant $text").split(" ").toSet()
        return candidates.firstOrNull { c ->
            val words = MerchantNormalizer.key(c.keyword).split(" ").filter { it.isNotEmpty() }
            val signMatches = if (c.owedByMe) amountCents < 0 else amountCents > 0
            words.isNotEmpty() && signMatches && haystack.containsAll(words)
        }?.id
    }
}
