package com.tecladop2pv2.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tecladop2pv2.core.model.*

@Entity(tableName = "history_records", indices = [Index("createdAt"), Index("syncState"), Index("recordId", unique = true)])
data class HistoryRecordEntity(
    @PrimaryKey val recordId: String,
    val rawTextCiphertext: ByteArray,
    val filteredTextCiphertext: ByteArray?,
    val createdAt: Long,
    val completedAt: Long?,
    val sourceDeviceId: String,
    val sourceAppName: String,
    val sourceAppPackage: String?,
    val version: Long,
    val syncState: SyncState,
    val lastAttemptAt: Long? = null,
    val attemptCount: Int = 0,
    val lastErrorCode: String? = null,
    val deletedAt: Long? = null
)

@Entity(tableName = "received_records", indices = [Index("createdAt"), Index("recordId", unique = true)])
data class ReceivedRecordEntity(
    @PrimaryKey val recordId: String,
    val rawTextCiphertext: ByteArray,
    val filteredTextCiphertext: ByteArray?,
    val createdAt: Long,
    val completedAt: Long?,
    val sourceDeviceId: String,
    val sourceAppName: String,
    val sourceAppPackage: String?,
    val version: Long,
    val receivedAt: Long,
    val originSignature: ByteArray?
)

@Entity(tableName = "peer_devices", indices = [Index("status")])
data class PeerDeviceEntity(
    @PrimaryKey val deviceId: String,
    val displayName: String,
    val publicKey: ByteArray,
    val status: PeerStatus,
    val pairedAt: Long,
    val lastSeenAt: Long?,
    val revokedAt: Long?,
    val protocolVersion: Int
)

@Entity(tableName = "sync_operations", indices = [Index("peerDeviceId"), Index("nextAttemptAt"), Index("recordId")])
data class SyncOperationEntity(
    @PrimaryKey val operationId: String,
    val recordId: String,
    val peerDeviceId: String,
    val operationType: SyncOperationType,
    val state: OperationState,
    val attemptCount: Int,
    val nextAttemptAt: Long,
    val createdAt: Long,
    val lastErrorCode: String?
)

@Entity(tableName = "signaling_sessions")
data class SignalingSessionEntity(
    @PrimaryKey val sessionId: String,
    val peerDeviceId: String?,
    val role: SignalingRole,
    val state: SignalingState,
    val expiresAt: Long,
    val lastMessageId: String?
)
