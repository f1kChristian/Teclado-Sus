package com.tecladop2pv2.core.model

enum class SyncState { LOCAL_ONLY, QUEUED, SENDING, AWAITING_ACK, SYNCED, RETRY_WAIT, FAILED_PERMANENT, DELETED }
enum class PeerStatus { IDLE, PAIRING, SIGNALING, CONNECTING_ICE, CONNECTED, DEGRADED, DISCONNECTED, REVOKED }
enum class SyncOperationType { RECORD }
enum class OperationState { QUEUED, SENDING, AWAITING_ACK, RETRY_WAIT, COMPLETE, FAILED }
enum class SignalingRole { INITIATOR, JOINER }
enum class SignalingState { CREATED, ACTIVE, EXPIRED, CANCELED }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class BackgroundType { SOLID, GRADIENT, IMAGE, PRESET }

data class HistoryRecord(
    val recordId: String,
    val rawText: String,
    val filteredText: String?,
    val createdAt: Long,
    val completedAt: Long?,
    val sourceDeviceId: String,
    val sourceAppName: String,
    val sourceAppPackage: String?,
    val version: Long,
    val syncState: SyncState,
    val attemptCount: Int = 0
)
