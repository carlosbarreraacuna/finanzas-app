package com.carlos.finanzas.capture

/**
 * Genera una plantilla (regex con grupos `amount` y `merchant`) a partir de un mensaje
 * y de los rangos que el usuario marcó en él. No depende de Android.
 */
object TemplateBuilder {

    /** Rango [start, end) dentro del texto. */
    data class Range(val start: Int, val end: Int)

    private const val PREFIX_CONTEXT = 24
    private const val AMOUNT_GROUP = """(?<amount>[\d.,]+)"""
    private const val MERCHANT_GROUP = """(?<merchant>[^\n]+?)"""

    /**
     * @return el patrón, o null si los rangos son inválidos o el patrón generado
     *         no reproduce el mensaje original.
     */
    fun build(text: String, amount: Range, merchant: Range?): String? {
        val amountRange = trimToDigits(text, amount) ?: return null
        if (merchant != null && !isValid(text, merchant)) return null
        if (merchant != null && overlaps(amountRange, merchant)) return null

        val segments = buildList {
            add(amountRange to AMOUNT_GROUP)
            if (merchant != null) add(trimWhitespace(text, merchant) to MERCHANT_GROUP)
        }.sortedBy { it.first.start }

        val sb = StringBuilder()
        val first = segments.first().first
        sb.append(literal(text.substring(maxOf(0, first.start - PREFIX_CONTEXT), first.start)))

        segments.forEachIndexed { i, (range, group) ->
            sb.append(group)
            val next = segments.getOrNull(i + 1)?.first
            if (next != null) {
                sb.append(literal(text.substring(range.end, next.start)))
            } else if (group == MERCHANT_GROUP) {
                // El comercio es lazy: necesita un terminador. Se usa la siguiente "palabra"
                // (con los dígitos generalizados), no un solo carácter, porque un comercio
                // como "TIENDA D1" contiene espacios.
                val terminator = Regex("""^\s*\S+""").find(text.substring(range.end))?.value.orEmpty()
                sb.append(if (terminator.isEmpty()) """\s*$""" else looseLiteral(terminator))
            }
        }

        val pattern = sb.toString()
        return if (reproduces(pattern, text, amountRange)) pattern else null
    }

    /** Compila el patrón con las mismas opciones que usa el parser. */
    fun compile(pattern: String): Regex? =
        runCatching { Regex(pattern, RegexOption.IGNORE_CASE) }.getOrNull()

    private fun reproduces(pattern: String, text: String, amount: Range): Boolean {
        val regex = compile(pattern) ?: return false
        val m = regex.find(text) ?: return false
        return m.groups["amount"]?.value == text.substring(amount.start, amount.end)
    }

    private fun isValid(text: String, r: Range) = r.start >= 0 && r.start < r.end && r.end <= text.length

    private fun overlaps(a: Range, b: Range) = a.start < b.end && b.start < a.end

    /** Quita de los extremos del monto todo lo que no sea dígito ("$", espacios, puntos finales). */
    private fun trimToDigits(text: String, r: Range): Range? {
        if (!isValid(text, r)) return null
        var s = r.start
        var e = r.end
        while (s < e && !text[s].isDigit()) s++
        while (e > s && !text[e - 1].isDigit()) e--
        return if (s < e) Range(s, e) else null
    }

    private fun trimWhitespace(text: String, r: Range): Range {
        var s = r.start
        var e = r.end
        while (s < e && text[s].isWhitespace()) s++
        while (e > s && text[e - 1].isWhitespace()) e--
        return Range(s, e)
    }

    /** Como [literal], pero además cualquier racha de dígitos pasa a `\d+` (fechas, horas, etc.). */
    private fun looseLiteral(s: String): String {
        val out = StringBuilder()
        var last = 0
        for (m in Regex("""\d+""").findAll(s)) {
            if (m.range.first > last) out.append(literal(s.substring(last, m.range.first)))
            out.append("""\d+""")
            last = m.range.last + 1
        }
        if (last < s.length) out.append(literal(s.substring(last)))
        return out.toString()
    }

    /** Texto literal escapado; cualquier racha de espacios pasa a `\s+`. */
    private fun literal(s: String): String {
        val out = StringBuilder()
        var last = 0
        for (m in Regex("""\s+""").findAll(s)) {
            if (m.range.first > last) out.append(Regex.escape(s.substring(last, m.range.first)))
            out.append("""\s+""")
            last = m.range.last + 1
        }
        if (last < s.length) out.append(Regex.escape(s.substring(last)))
        return out.toString()
    }
}
