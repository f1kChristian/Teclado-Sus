package com.tecladop2pv2.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tecladop2pv2.core.model.OperationState
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOperationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(operation: SyncOperationEntity)

    @Update
    suspend fun update(operation: SyncOperationEntity)

    @Query("""
        SELECT * FROM sync_operations
        WHERE state IN ('QUEUED', 'RETRY_WAIT')
          AND nextAttemptAt <= :now
        ORDER BY createdAt ASC
    """)
    suspend fun getReadyOperations(now: Long): List<SyncOperationEntity>

    @Query("""
        SELECT COUNT(*) FROM sync_operations
        WHERE state IN ('QUEUED', 'SENDING', 'AWAITING_ACK', 'RETRY_WAIT')
    """)
    fun observePendingCount(): Flow<Int>

    @Query("""
        SELECT * FROM sync_operations
        WHERE operationId = :operationId
        LIMIT 1
    """)
    suspend fun getById(operationId: String): SyncOperationEntity?

    @Query("""
        UPDATE sync_operations
        SET state = :state,
            attemptCount = :attemptCount,
            nextAttemptAt = :nextAttemptAt,
            lastErrorCode = :errorCode
        WHERE operationId = :operationId
    """)
    suspend fun updateState(
        operationId: String,
        state: OperationState,
        attemptCount: Int,
        nextAttemptAt: Long,
        errorCode: String?
    )

    @Query("""
        DELETE FROM sync_operations
        WHERE operationId = :operationId
    """)
    suspend fun delete(operationId: String)
}
