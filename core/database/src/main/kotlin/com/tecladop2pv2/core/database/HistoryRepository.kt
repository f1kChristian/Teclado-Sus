package com.tecladop2pv2.core.database

import com.tecladop2pv2.core.model.HistoryRecord
import com.tecladop2pv2.core.model.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

interface HistoryRepository {
    fun observeAll(): Flow<List<HistoryRecord>>
    fun observeFiltered(): Flow<List<HistoryRecord>>
    fun observePendingCount(): Flow<Int>

    suspend fun saveAuthorized(
        rawText: String,
        appName: String,
        appPackage: String?
    ): String

    suspend fun delete(recordId: String)
    suspend fun deleteAll()
}

class RoomHistoryRepository(
    private val dao: HistoryDao,
    private val deviceId: String
) : HistoryRepository {

    override fun observeAll() =
        dao.observeAll().map { rows ->
            rows.map(::decode)
        }

    override fun observeFiltered() =
        dao.observeFiltered().map { rows ->
            rows.map(::decode)
        }

    override fun observePendingCount() =
        dao.observePendingCount()

    override suspend fun saveAuthorized(
        rawText: String,
        appName: String,
        appPackage: String?
    ): String {
        require(rawText.isNotBlank())

        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        dao.insert(
            HistoryRecordEntity(
                recordId = id,
                rawTextCiphertext = rawText.toByteArray(Charsets.UTF_8),
                filteredTextCiphertext = null,
                createdAt = now,
                completedAt = now,
                sourceDeviceId = deviceId,
                sourceAppName = appName,
                sourceAppPackage = appPackage,
                version = 1,
                syncState = SyncState.LOCAL_ONLY
            )
        )

        return id
    }

    override suspend fun delete(recordId: String) {
        dao.markDeleted(recordId, System.currentTimeMillis())
    }

    override suspend fun deleteAll() {
        dao.deleteAll(System.currentTimeMillis())
    }

    private fun decode(row: HistoryRecordEntity): HistoryRecord {
        return HistoryRecord(
            recordId = row.recordId,
            rawText = row.rawTextCiphertext.toString(Charsets.UTF_8),
            filteredText = row.filteredTextCiphertext?.toString(Charsets.UTF_8),
            createdAt = row.createdAt,
            completedAt = row.completedAt,
            sourceDeviceId = row.sourceDeviceId,
            sourceAppName = row.sourceAppName,
            sourceAppPackage = row.sourceAppPackage,
            version = row.version,
            syncState = row.syncState,
            attemptCount = row.attemptCount
        )
    }
}
