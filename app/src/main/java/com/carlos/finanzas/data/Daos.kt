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
