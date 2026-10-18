package com.carlos.finanzas.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index("timestamp"), Index("fingerprint", unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    /** Monto en centavos negativos = gasto, positivos = ingreso. */
    val amountCents: Long,
    val merchant: String,
    val category: String? = null,
    val source: String,          // "SMS" o "NOTIF"
    val sender: String,          // remitente SMS o paquete de la app
    val rawText: String,
    /** Hash para evitar duplicados (misma transacción por SMS + notificación). */
    val fingerprint: String
)

@Entity(tableName = "unrecognized_messages", indices = [Index("timestamp")])
data class UnrecognizedMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val source: String,
    val sender: String,
    val text: String,
    val status: String = "PENDING" // PENDING, DISCARDED, RESOLVED
)

/** Plantilla creada por el usuario desde la bandeja "Sin reconocer". */
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Regex con grupos con nombre: amount (obligatorio) y merchant (opcional). */
    val pattern: String,
    val isIncome: Boolean,
    val sender: String?,
    val createdAt: Long
)

/** Regla aprendida: comercio normalizado -> categoría (se crea al corregir una categoría a mano). */
@Entity(
    tableName = "category_rules",
    indices = [Index(value = ["merchantKey"], unique = true)]
)
data class CategoryRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantKey: String,
    val category: String,
    val createdAt: Long
)

/**
 * Deuda propia ([direction] = OWE) o dinero que otros me deben ([direction] = OWED).
 * El saldo baja con cada pago registrado; se puede ajustar editando la deuda.
 */
@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** CARD, LOAN o INFORMAL. */
    val kind: String,
    /** OWE (yo debo) u OWED (me deben). */
    val direction: String,
    val originalCents: Long,
    val balanceCents: Long,
    /** Tasa efectiva anual en %, ej. 28.5. */
    val annualRatePct: Double,
    /** Cuota mensual, o pago mínimo en tarjetas. 0 si no aplica. */
    val monthlyPaymentCents: Long,
    /** Día del mes del pago/vencimiento (1-31); 0 = sin fecha. */
    val dueDay: Int,
    val creditLimitCents: Long = 0,
    /** Palabras que identifican los pagos de esta deuda en los mensajes capturados. */
    val matchKeyword: String = "",
    val notes: String = "",
    val active: Boolean = true,
    val createdAt: Long
)

@Entity(tableName = "debt_payments", indices = [Index("debtId")])
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val timestamp: Long,
    val amountCents: Long,
    /** MANUAL o AUTO (detectado en una transacción capturada). */
    val source: String,
    val transactionId: Long? = null,
    val note: String = ""
)

@Entity(tableName = "health_events", indices = [Index("timestamp")])
data class HealthEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    /** LISTENER_CONNECTED, LISTENER_DISCONNECTED, REBIND_REQUESTED, SMS_CAPTURED, NOTIF_CAPTURED, PARSE_FAILED */
    val type: String,
    val detail: String? = null
)
