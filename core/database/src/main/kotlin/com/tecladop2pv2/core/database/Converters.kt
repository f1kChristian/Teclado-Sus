package com.tecladop2pv2.core.database

import androidx.room.TypeConverter
import com.tecladop2pv2.core.model.*

class Converters {
    @TypeConverter fun syncStateFrom(value: String) = SyncState.valueOf(value)
    @TypeConverter fun syncStateTo(value: SyncState) = value.name
    @TypeConverter fun peerStatusFrom(value: String) = PeerStatus.valueOf(value)
    @TypeConverter fun peerStatusTo(value: PeerStatus) = value.name
    @TypeConverter fun operationTypeFrom(value: String) = SyncOperationType.valueOf(value)
    @TypeConverter fun operationTypeTo(value: SyncOperationType) = value.name
    @TypeConverter fun operationStateFrom(value: String) = OperationState.valueOf(value)
    @TypeConverter fun operationStateTo(value: OperationState) = value.name
    @TypeConverter fun signalingRoleFrom(value: String) = SignalingRole.valueOf(value)
    @TypeConverter fun signalingRoleTo(value: SignalingRole) = value.name
    @TypeConverter fun signalingStateFrom(value: String) = SignalingState.valueOf(value)
    @TypeConverter fun signalingStateTo(value: SignalingState) = value.name
}
