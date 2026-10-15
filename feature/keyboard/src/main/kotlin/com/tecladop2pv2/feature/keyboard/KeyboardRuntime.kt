package com.tecladop2pv2.feature.keyboard

import com.tecladop2pv2.core.database.HistoryRepository

/** Application-owned dependencies exposed to the input method; no DAO is exposed to the service. */
object KeyboardRuntime {
    @Volatile var historyRepository: HistoryRepository? = null
}
