package com.tecladop2pv2.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: HistoryRecordEntity)

    @Query("SELECT * FROM history_records WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HistoryRecordEntity>>

    @Query("SELECT * FROM history_records WHERE deletedAt IS NULL AND filteredTextCiphertext IS NOT NULL ORDER BY createdAt DESC")
    fun observeFiltered(): Flow<List<HistoryRecordEntity>>

    @Query("UPDATE history_records SET deletedAt = :time, syncState = 'DELETED' WHERE recordId = :id")
    suspend fun markDeleted(id: String, time: Long)

    @Query("UPDATE history_records SET deletedAt = :time, syncState = 'DELETED' WHERE deletedAt IS NULL")
    suspend fun markAllDeleted(time: Long)

    @Query("DELETE FROM history_records WHERE createdAt < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("SELECT COUNT(*) FROM history_records WHERE syncState IN ('LOCAL_ONLY','QUEUED','SENDING','AWAITING_ACK','RETRY_WAIT') AND deletedAt IS NULL")
    fun observePendingCount(): Flow<Int>

    @Transaction
    suspend fun deleteAll(time: Long) = markAllDeleted(time)
}
