package com.tecladop2pv2

import android.app.Application
import androidx.room.Room
import com.tecladop2pv2.core.database.LocalDatabase
import com.tecladop2pv2.core.database.RoomHistoryRepository
import com.tecladop2pv2.core.security.KeystoreTextCipher
import com.tecladop2pv2.feature.customization.CustomizationRepository
import com.tecladop2pv2.feature.keyboard.KeyboardRuntime
import java.util.UUID

class TecladoApplication : Application() {
    lateinit var database: LocalDatabase
        private set
    lateinit var history: RoomHistoryRepository
        private set
    lateinit var customization: CustomizationRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, LocalDatabase::class.java, "teclado-local.db").build()
        val deviceId = getSharedPreferences("device", MODE_PRIVATE).let { prefs ->
            prefs.getString("id", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("id", it).apply() }
        }
        history = RoomHistoryRepository(database.historyDao(), KeystoreTextCipher(), deviceId)
        customization = CustomizationRepository(this)
        KeyboardRuntime.historyRepository = history
    }
}
