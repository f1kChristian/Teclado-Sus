package com.tecladop2pv2.signaling

/** Transport contract for a deployment-provided HTTPS + WSS signaling service. */
interface SignalingClient {
    suspend fun createSession(): PairingSession
    suspend fun joinSession(sessionCode: String): PairingSession
    suspend fun send(sessionId: String, message: String)
    suspend fun messages(sessionId: String, onMessage: (String) -> Unit)
    suspend fun cancel(sessionId: String)
}

data class PairingSession(val sessionId: String, val oneTimeCode: String, val expiresAtMillis: Long)

/** Fails closed until a real authenticated backend is supplied by the deployment. */
class UnconfiguredSignalingClient : SignalingClient {
    private fun unavailable(): Nothing = error("SIGNALING_NOT_CONFIGURED")
    override suspend fun createSession(): PairingSession = unavailable()
    override suspend fun joinSession(sessionCode: String): PairingSession = unavailable()
    override suspend fun send(sessionId: String, message: String): Unit = unavailable()
    override suspend fun messages(sessionId: String, onMessage: (String) -> Unit): Unit = unavailable()
    override suspend fun cancel(sessionId: String): Unit = unavailable()
}
