package com.tecladop2pv2.feature.pairing

enum class ConnectionState { IDLE, PAIRING, SIGNALING, CONNECTING_ICE, CONNECTED, DISCONNECTED, REVOKED }

data class PairingUiState(val state: ConnectionState = ConnectionState.DISCONNECTED, val sessionExpiresAt: Long? = null) {
    val accessibleStatus: String get() = if (state == ConnectionState.CONNECTED) "Conectado" else "Desconectado"
}
