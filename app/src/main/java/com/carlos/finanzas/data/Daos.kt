package com.carlos.finanzas.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tx: TransactionEntity): Long

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun recent(limit: Int = 100): Flow<List<TransactionEntity>>
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
