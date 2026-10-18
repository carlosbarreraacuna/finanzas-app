package com.carlos.finanzas.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tx: TransactionEntity): Long

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun recent(limit: Int = 100): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions")
    suspend fun all(): List<TransactionEntity>

    /** Transacciones con [from] <= timestamp < [to]. */
    @Query("SELECT * FROM transactions WHERE timestamp >= :fromMs AND timestamp < :toMs")
    fun between(fromMs: Long, toMs: Long): Flow<List<TransactionEntity>>

    @Query("UPDATE transactions SET category = :category WHERE id IN (:ids)")
    suspend fun setCategoryForIds(ids: List<Long>, category: String)
}

@Dao
interface DebtDao {
    @Insert
    suspend fun insert(debt: DebtEntity): Long

    @Update
    suspend fun update(debt: DebtEntity)

    @Query("SELECT * FROM debts WHERE active = 1 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE active = 1")
    suspend fun activeNow(): List<DebtEntity>

    @Query("UPDATE debts SET active = 0 WHERE id = :id")
    suspend fun archive(id: Long)

    /** Resta el pago al saldo sin dejarlo bajo cero. */
    @Query("UPDATE debts SET balanceCents = MAX(0, balanceCents - :amountCents) WHERE id = :id")
    suspend fun applyPayment(id: Long, amountCents: Long)

    /** Devuelve al saldo un pago borrado. */
    @Query("UPDATE debts SET balanceCents = balanceCents + :amountCents WHERE id = :id")
    suspend fun revertPayment(id: Long, amountCents: Long)
}

@Dao
interface DebtPaymentDao {
    @Insert
    suspend fun insert(payment: DebtPaymentEntity): Long

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY timestamp DESC LIMIT 50")
    fun observeFor(debtId: Long): Flow<List<DebtPaymentEntity>>

    @Query("DELETE FROM debt_payments WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface CategoryRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: CategoryRuleEntity)

    @Query("SELECT * FROM category_rules WHERE merchantKey = :key LIMIT 1")
    suspend fun findByKey(key: String): CategoryRuleEntity?
}

@Dao
interface UnrecognizedDao {
    @Insert
    suspend fun insert(msg: UnrecognizedMessageEntity): Long

    @Query("SELECT * FROM unrecognized_messages WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun pending(): Flow<List<UnrecognizedMessageEntity>>

    @Query("SELECT * FROM unrecognized_messages WHERE status = 'PENDING' AND sender = :sender")
    suspend fun pendingBySender(sender: String): List<UnrecognizedMessageEntity>

    @Query("UPDATE unrecognized_messages SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)
}

@Dao
interface TemplateDao {
    @Insert
    suspend fun insert(template: TemplateEntity): Long

    @Query("SELECT * FROM templates ORDER BY id DESC")
    suspend fun all(): List<TemplateEntity>
}

@Dao
interface HealthDao {
    @Insert
    suspend fun insert(event: HealthEventEntity)

    @Query("SELECT COUNT(*) FROM health_events WHERE type = :type AND timestamp >= :since")
    fun countSince(type: String, since: Long): Flow<Int>

    @Query("SELECT MAX(timestamp) FROM health_events WHERE type = :type")
    fun lastOf(type: String): Flow<Long?>

    @Query("SELECT * FROM health_events ORDER BY timestamp DESC LIMIT :limit")
    fun recent(limit: Int = 30): Flow<List<HealthEventEntity>>
}
