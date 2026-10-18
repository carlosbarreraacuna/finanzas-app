package com.carlos.finanzas.capture

import java.text.Normalizer

object Categories {
    const val INCOME = "Ingresos"
    const val UNCATEGORIZED = "Sin categoría"

    /** Categorías que se pueden asignar a mano. */
    val ALL = listOf(
        "Alimentación", "Transporte", "Hogar", "Servicios", "Suscripciones",
        "Salud", "Ocio", "Educación", "Deudas", INCOME, "Otros"
    )
}

/** Convierte el nombre de un comercio en una clave estable para reglas ("RAPPI*COL 123" -> "rappi col"). */
object MerchantNormalizer {
    private val accents = Regex("""\p{Mn}+""")
    private val nonAlnum = Regex("""[^a-z0-9]+""")
    private const val UNKNOWN = "desconocido"

    fun key(merchant: String): String {
        val plain = Normalizer.normalize(merchant.lowercase(), Normalizer.Form.NFD).replace(accents, "")
        return plain.replace(nonAlnum, " ")
            .split(" ")
            .filter { it.isNotEmpty() && !(it.length >= 3 && it.all(Char::isDigit)) } // números de local/terminal
            .joinToString(" ")
    }

    /** Un comercio desconocido o vacío no sirve para aprender reglas. */
    fun isLearnable(key: String) = key.isNotEmpty() && key != UNKNOWN
}

/**
 * Orden de decisión: 1) regla aprendida del usuario, 2) ingreso si el monto es positivo,
 * 3) palabras clave por defecto, 4) sin categoría (null).
 */
object Categorizer {
    private val DEFAULT_KEYWORDS: Map<String, List<String>> = mapOf(
        "Alimentación" to listOf(
            "rappi", "exito", "jumbo", "carulla", "olimpica", "ara", "d1", "pricesmart",
            "mcdonalds", "kfc", "frisby", "crepes", "restaurante", "panaderia", "cafe"
        ),
        "Transporte" to listOf(
            "uber", "didi", "cabify", "indriver", "beat", "transmilenio", "sitp",
            "terpel", "primax", "texaco", "gasolina", "parqueadero", "peaje"
        ),
        "Suscripciones" to listOf(
            "netflix", "spotify", "disney", "hbo", "max", "youtube", "prime video",
            "apple com bill", "google one", "icloud"
        ),
        "Servicios" to listOf(
            "claro", "movistar", "tigo", "etb", "wom", "enel", "vanti", "acueducto",
            "codensa", "epm", "internet", "energia"
        ),
        "Salud" to listOf(
            "farmacia", "drogueria", "cruz verde", "farmatodo", "colsanitas", "sura eps",
            "eps", "clinica", "medico"
        )
    )

    fun suggest(key: String, amountCents: Long, userCategory: String?): String? {
        if (userCategory != null) return userCategory
        if (amountCents > 0) return Categories.INCOME
        if (key.isEmpty()) return null
        val padded = " $key "
        for ((category, words) in DEFAULT_KEYWORDS) {
            if (words.any { padded.contains(" $it ") }) return category
        }
        return null
    }
}
