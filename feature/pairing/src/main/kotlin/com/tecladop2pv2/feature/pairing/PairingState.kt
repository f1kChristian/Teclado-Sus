
package com.tecladop2pv2.feature.pairing

enum class ConnectionState {
    IDLE,
    PAIRING,
    SIGNALING,
    CONNECTING_ICE,
    CONNECTED,
    DISCONNECTED,
    REVOKED,
    ERROR
}

data class PairingUiState(
    val state: ConnectionState = ConnectionState.DISCONNECTED,
    val sessionId: String? = null,
    val sessionCode: String? = null,
    val sessionExpiresAt: Long? = null,
    val errorMessage: String? = null
) {
    val accessibleStatus: String
        get() = when (state) {
            ConnectionState.IDLE -> "Pronto para conectar"
            ConnectionState.PAIRING -> "Criando pareamento"
            ConnectionState.SIGNALING -> "Aguardando outro dispositivo"
            ConnectionState.CONNECTING_ICE -> "Estabelecendo conexão"
            ConnectionState.CONNECTED -> "Conectado"
            ConnectionState.DISCONNECTED -> "Desconectado"
            ConnectionState.REVOKED -> "Conexão revogada"
            ConnectionState.ERROR -> errorMessage ?: "Erro de conexão"
        }
}
