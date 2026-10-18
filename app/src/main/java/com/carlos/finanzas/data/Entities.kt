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

@Entity(tableName = "health_events", indices = [Index("timestamp")])
data class HealthEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    /** LISTENER_CONNECTED, LISTENER_DISCONNECTED, REBIND_REQUESTED, SMS_CAPTURED, NOTIF_CAPTURED, PARSE_FAILED */
    val type: String,
    val detail: String? = null
)
