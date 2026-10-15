package com.tecladop2pv2.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [HistoryRecordEntity::class, ReceivedRecordEntity::class, PeerDeviceEntity::class, SyncOperationEntity::class, SignalingSessionEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}
