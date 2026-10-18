package com.carlos.finanzas.capture

data class ParsedTransaction(
    val amountCents: Long,
    val merchant: String
)

/**
 * Parser por plantillas. Cada plantilla es una regex con grupos con nombre:
 *  - amount   (obligatorio)
 *  - merchant (opcional)
 * Más adelante estas plantillas vivirán en la base de datos y se podrán
 * crear desde la bandeja "Sin reconocer".
 */
class MessageParser(private val templates: List<Template> = DEFAULT_TEMPLATES) {

    data class Template(val name: String, val regex: Regex, val isIncome: Boolean = false)

    fun parse(text: String): ParsedTransaction? {
        for (t in templates) {
            val m = t.regex.find(text) ?: continue
            val rawAmount = m.groups["amount"]?.value ?: continue
            val cents = parseAmountToCents(rawAmount) ?: continue
            val merchant = runCatching { m.groups["merchant"]?.value }.getOrNull()
                ?.trim()?.ifBlank { null } ?: "Desconocido"
            return ParsedTransaction(if (t.isIncome) cents else -cents, merchant)
        }
        return null
    }

    /** "1.234.567,89" o "1,234,567.89" o "25000" -> centavos. */
    internal fun parseAmountToCents(raw: String): Long? {
        val s = raw.trim().replace("$", "").replace(" ", "")
        if (s.isEmpty()) return null
        val lastDot = s.lastIndexOf('.')
        val lastComma = s.lastIndexOf(',')
        val decimalSep = when {
            lastDot >= 0 && lastComma >= 0 -> if (lastDot > lastComma) '.' else ','
            lastComma >= 0 && s.length - lastComma - 1 in 1..2 -> ','
            lastDot >= 0 && s.length - lastDot - 1 in 1..2 -> '.'
            else -> null
        }
        val normalized = if (decimalSep == null) {
            s.replace(".", "").replace(",", "")
        } else {
            val intPart = s.substring(0, s.lastIndexOf(decimalSep)).replace(".", "").replace(",", "")
            val decPart = s.substring(s.lastIndexOf(decimalSep) + 1)
            "$intPart.$decPart"
        }
        return normalized.toBigDecimalOrNull()?.movePointRight(2)?.toLong()
    }

    companion object {
        // Plantillas de ejemplo. Ajústalas a los mensajes reales de tus bancos.
        val DEFAULT_TEMPLATES = listOf(
            Template(
                "compra_generica",
                Regex("""compra\s+por\s+\$?\s*(?<amount>[\d.,]+)\s+en\s+(?<merchant>[^.\n]+)""", RegexOption.IGNORE_CASE)
            ),
            Template(
                "transferencia_recibida",
                Regex("""recibiste\s+\$?\s*(?<amount>[\d.,]+)""", RegexOption.IGNORE_CASE),
                isIncome = true
            ),
            Template(
                "pago_enviado",
                Regex("""(?:pagaste|enviaste)\s+\$?\s*(?<amount>[\d.,]+)(?:\s+a\s+(?<merchant>[^.\n]+))?""", RegexOption.IGNORE_CASE)
            )
        )
    }
}
