package com.carlos.finanzas.capture

import android.content.Context
import com.carlos.finanzas.FinanzasApp
import com.carlos.finanzas.data.HealthEventEntity
import com.carlos.finanzas.data.TransactionEntity
import com.carlos.finanzas.data.UnrecognizedMessageEntity
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Punto único por el que pasan SMS y notificaciones capturados. */
object CaptureProcessor {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val parser = MessageParser()

    /** Solo se procesan mensajes que parezcan financieros. */
    private val financialHint = Regex(
        """\$|compra|pago|pagaste|recibiste|transferencia|retiro|cuota|saldo""",
        RegexOption.IGNORE_CASE
    )

    fun process(context: Context, source: String, sender: String, text: String, timestamp: Long) {
        if (!financialHint.containsMatchIn(text)) return
        val db = (context.applicationContext as FinanzasApp).db
        scope.launch {
            val captureType = if (source == "SMS") "SMS_CAPTURED" else "NOTIF_CAPTURED"
            db.health().insert(HealthEventEntity(timestamp = timestamp, type = captureType, detail = sender))

            val parsed = parser.parse(text)
            if (parsed == null) {
                db.unrecognized().insert(
                    UnrecognizedMessageEntity(timestamp = timestamp, source = source, sender = sender, text = text)
                )
                db.health().insert(HealthEventEntity(timestamp = timestamp, type = "PARSE_FAILED", detail = sender))
                return@launch
            }
            // Ventana de 5 minutos para deduplicar SMS + notificación del mismo movimiento.
            val bucket = timestamp / (5 * 60 * 1000)
            val fingerprint = sha256("${parsed.amountCents}|${parsed.merchant.lowercase()}|$bucket")
            db.transactions().insert(
                TransactionEntity(
                    timestamp = timestamp,
                    amountCents = parsed.amountCents,
                    merchant = parsed.merchant,
                    source = source,
                    sender = sender,
                    rawText = text,
                    fingerprint = fingerprint
                )
            )
        }
    }

    fun logHealth(context: Context, type: String, detail: String? = null) {
        val db = (context.applicationContext as FinanzasApp).db
        scope.launch {
            db.health().insert(HealthEventEntity(timestamp = System.currentTimeMillis(), type = type, detail = detail))
        }
    }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
