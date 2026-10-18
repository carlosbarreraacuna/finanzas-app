package com.carlos.finanzas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carlos.finanzas.FinanzasApp
import com.carlos.finanzas.capture.CaptureProcessor
import com.carlos.finanzas.capture.MerchantNormalizer
import com.carlos.finanzas.capture.MessageParser
import com.carlos.finanzas.capture.TemplateBuilder
import com.carlos.finanzas.data.CategoryRuleEntity
import com.carlos.finanzas.data.TemplateEntity
import com.carlos.finanzas.data.TransactionEntity
import com.carlos.finanzas.data.UnrecognizedMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionsViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as FinanzasApp).db

    val transactions: StateFlow<List<TransactionEntity>> =
        db.transactions().recent(200)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Asigna la categoría a una transacción. Con [applyToMerchant] además crea (o actualiza) la regla
     * del comercio y recategoriza todas las transacciones existentes del mismo comercio.
     */
    fun setCategory(tx: TransactionEntity, category: String, applyToMerchant: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val key = MerchantNormalizer.key(tx.merchant)
            val learn = applyToMerchant && MerchantNormalizer.isLearnable(key)
            val ids = if (learn) {
                db.transactions().all().filter { MerchantNormalizer.key(it.merchant) == key }.map { it.id }
            } else {
                listOf(tx.id)
            }
            // SQLite limita las variables por consulta: se actualiza por lotes.
            ids.chunked(500).forEach { db.transactions().setCategoryForIds(it, category) }
            if (learn) {
                db.categoryRules().upsert(
                    CategoryRuleEntity(merchantKey = key, category = category, createdAt = System.currentTimeMillis())
                )
            }
        }
    }
}

class InboxViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as FinanzasApp).db

    val pending: StateFlow<List<UnrecognizedMessageEntity>> =
        db.unrecognized().pending()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Mensaje informativo para mostrar tras guardar una plantilla (ej. "3 mensajes reprocesados"). */
    val info = MutableStateFlow<String?>(null)

    fun clearInfo() { info.value = null }

    fun discard(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { db.unrecognized().setStatus(id, "DISCARDED") }
    }

    /** Vista previa: qué transacción saldría de aplicar [pattern] al [text]. */
    fun preview(pattern: String, text: String, isIncome: Boolean) =
        TemplateBuilder.compile(pattern)?.let {
            MessageParser(listOf(MessageParser.Template("preview", it, isIncome))).parse(text)
        }

    /**
     * Guarda la plantilla y reprocesa los pendientes del mismo remitente.
     * El mensaje que originó la plantilla también se reprocesa (está pendiente del mismo remitente).
     */
    fun saveTemplate(message: UnrecognizedMessageEntity, pattern: String, isIncome: Boolean, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val regex = TemplateBuilder.compile(pattern)
            if (regex == null) {
                info.value = "La plantilla no es válida"
                return@launch
            }
            db.templates().insert(
                TemplateEntity(
                    name = name.ifBlank { "plantilla_${message.sender}" },
                    pattern = pattern,
                    isIncome = isIncome,
                    sender = message.sender,
                    createdAt = System.currentTimeMillis()
                )
            )
            val parser = MessageParser(listOf(MessageParser.Template("nueva", regex, isIncome)))
            var resolved = 0
            for (m in db.unrecognized().pendingBySender(message.sender)) {
                val parsed = parser.parse(m.text) ?: continue
                CaptureProcessor.saveTransaction(db, parsed, m.source, m.sender, m.text, m.timestamp)
                db.unrecognized().setStatus(m.id, "RESOLVED")
                resolved++
            }
            info.value = "Plantilla guardada. Mensajes reprocesados: $resolved"
        }
    }
}
