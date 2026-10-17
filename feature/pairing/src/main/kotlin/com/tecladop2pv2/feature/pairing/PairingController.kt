
package com.tecladop2pv2.feature.pairing

import com.tecladop2pv2.signaling.PairingSession
import com.tecladop2pv2.signaling.SignalingClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PairingController(
    private val signalingClient: SignalingClient,
    private val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main
    )
) {
    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    fun createSession() {
        scope.launch {
            _uiState.value = PairingUiState(
                state = ConnectionState.PAIRING
            )

            try {
                val session = withContext(Dispatchers.IO) {
                    signalingClient.createSession()
                }

                _uiState.value = PairingUiState(
                    state = ConnectionState.SIGNALING,
                    sessionId = session.sessionId,
                    sessionCode = session.oneTimeCode,
                    sessionExpiresAt = session.expiresAtMillis
                )
            } catch (exception: Exception) {
                _uiState.value = PairingUiState(
                    state = ConnectionState.ERROR,
                    errorMessage = exception.message
                        ?: "Não foi possível criar a sessão."
                )
            }
        }
    }

    fun joinSession(code: String) {
        scope.launch {
            _uiState.value = PairingUiState(
                state = ConnectionState.PAIRING
            )

            try {
                val session = withContext(Dispatchers.IO) {
                    signalingClient.joinSession(code.trim())
                }

                _uiState.value = PairingUiState(
                    state = ConnectionState.SIGNALING,
                    sessionId = session.sessionId,
                    sessionCode = session.oneTimeCode,
                    sessionExpiresAt = session.expiresAtMillis
                )
            } catch (exception: Exception) {
                _uiState.value = PairingUiState(
                    state = ConnectionState.ERROR,
                    errorMessage = when (exception.message) {
                        "INVALID_PAIRING_CODE" ->
                            "O código deve conter seis números."
                        "SESSION_NOT_FOUND" ->
                            "Sessão não encontrada."
                        "SESSION_EXPIRED" ->
                            "O código de pareamento expirou."
                        else ->
                            exception.message
                                ?: "Não foi possível entrar na sessão."
                    }
                )
            }
        }
    }

    fun cancelSession() {
        val sessionId = _uiState.value.sessionId

        scope.launch {
            if (sessionId != null) {
                runCatching {
                    withContext(Dispatchers.IO) {
                        signalingClient.cancel(sessionId)
                    }
                }
            }

            _uiState.value = PairingUiState(
                state = ConnectionState.DISCONNECTED
            )
        }
    }

    fun dispose() {
        scope.cancel()
    }
}
