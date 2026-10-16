package com.tecladop2pv2

import android.app.Application
import androidx.room.Room
import com.tecladop2pv2.core.database.LocalDatabase
import com.tecladop2pv2.core.database.RoomHistoryRepository
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

        // Inicializa o banco de dados local.
        database = Room.databaseBuilder(
            applicationContext,
            LocalDatabase::class.java,
            "teclado-local.db"
        ).build()

        // Obtém ou cria um identificador persistente para este dispositivo.
        val preferences = getSharedPreferences(
            "device",
            MODE_PRIVATE
        )

        val deviceId = preferences.getString("id", null)
            ?: UUID.randomUUID().toString().also { newId ->
                preferences.edit()
                    .putString("id", newId)
                    .apply()
            }

        // Inicializa o histórico sem criptografia local.
        history = RoomHistoryRepository(
            database.historyDao(),
            deviceId
        )

        // Inicializa as configurações de personalização do teclado.
        customization = CustomizationRepository()

        // Disponibiliza o repositório para o serviço do teclado.
        KeyboardRuntime.historyRepository = history
    }
}
