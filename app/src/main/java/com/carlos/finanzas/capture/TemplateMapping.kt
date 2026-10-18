package com.carlos.finanzas.capture

import com.carlos.finanzas.data.TemplateEntity

/** Convierte una plantilla guardada en una plantilla del parser; null si su regex es inválida. */
fun TemplateEntity.toParserTemplate(): MessageParser.Template? {
    val regex = TemplateBuilder.compile(pattern) ?: return null
    return MessageParser.Template(name, regex, isIncome)
}
