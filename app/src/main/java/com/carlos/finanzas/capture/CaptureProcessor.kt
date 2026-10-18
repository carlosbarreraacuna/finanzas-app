package com.carlos.finanzas.capture

import android.content.Context
import androidx.room.withTransaction
import com.carlos.finanzas.FinanzasApp
import com.carlos.finanzas.data.AppDatabase
import com.carlos.finanzas.data.DebtPaymentEntity
import com.carlos.finanzas.data.HealthEventEntity
import com.carlos.finanzas.data.TransactionEntity
import com.carlos.finanzas.data.UnrecognizedMessageEntity
import com.carlos.finanzas.debt.DebtMatcher
import java.security.MessageDigest
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Punto único por el que pasan SMS y notificaciones capturados. */
object CaptureProcessor {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

            val parsed = parserFor(db).parse(text)
            if (parsed == null) {
                db.unrecognized().insert(
                    UnrecognizedMessageEntity(timestamp = timestamp, source = source, sender = sender, text = text)
                )
                db.health().insert(HealthEventEntity(timestamp = timestamp, type = "PARSE_FAILED", detail = sender))
                return@launch
            }
            saveTransaction(db, parsed, source, sender, text, timestamp)
        }
    }

    /** Plantillas del usuario primero (más recientes antes), luego las de ejemplo. */
    suspend fun parserFor(db: AppDatabase): MessageParser {
        val userTemplates = db.templates().all().mapNotNull { it.toParserTemplate() }
        return MessageParser(userTemplates + MessageParser.DEFAULT_TEMPLATES)
    }

    /**
     * Guarda la transacción (con categoría) y, si es el pago de una deuda registrada, descuenta el saldo.
     * @return true si se insertó; false si era duplicado.
     */
    suspend fun saveTransaction(
        db: AppDatabase,
        parsed: ParsedTransaction,
        source: String,
        sender: String,
        text: String,
        timestamp: Long
    ): Boolean {
        // Ventana de 5 minutos para deduplicar SMS + notificación del mismo movimiento.
        val bucket = timestamp / (5 * 60 * 1000)
        val fingerprint = sha256("${parsed.amountCents}|${parsed.merchant.lowercase()}|$bucket")
        val key = MerchantNormalizer.key(parsed.merchant)
        val learned = if (MerchantNormalizer.isLearnable(key)) db.categoryRules().findByKey(key)?.category else null

        val debts = db.debts().activeNow()
        val debtId = DebtMatcher.find(
            debts.map { DebtMatcher.Candidate(it.id, it.matchKeyword, owedByMe = it.direction == "OWE") },
            parsed.merchant, text, parsed.amountCents
        )
        val debt = debts.firstOrNull { it.id == debtId }
        val category = if (learned == null && debt?.direction == "OWE") {
            Categories.DEBTS
        } else {
            Categorizer.suggest(key, parsed.amountCents, learned)
        }

        val id = db.transactions().insert(
            TransactionEntity(
                timestamp = timestamp,
                amountCents = parsed.amountCents,
                merchant = parsed.merchant,
                category = category,
                source = source,
                sender = sender,
                rawText = text,
                fingerprint = fingerprint
            )
        )
        if (id == -1L) return false

        if (debt != null) {
            val amount = abs(parsed.amountCents)
            db.withTransaction {
                db.debtPayments().insert(
                    DebtPaymentEntity(
                        debtId = debt.id,
                        timestamp = timestamp,
                        amountCents = amount,
                        source = "AUTO",
                        transactionId = id,
                        note = parsed.merchant
                    )
                )
                db.debts().applyPayment(debt.id, amount)
            }
        }
        return true
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
