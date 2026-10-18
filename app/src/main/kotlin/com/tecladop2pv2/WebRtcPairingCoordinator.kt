package com.tecladop2pv2

import android.content.Context
import com.tecladop2pv2.signaling.FirebaseSignalingClient
import com.tecladop2pv2.signaling.PairingSession
import com.tecladop2pv2.signaling.SignalingClient
import com.tecladop2pv2.webrtc.IceServerProvider
import com.tecladop2pv2.webrtc.PeerConnectionManager
import com.tecladop2pv2.webrtc.WebRtcConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Coordinates Firebase signaling and a WebRTC data channel for an explicitly initiated pairing.
 * History transfer is manual and requires consent on both paired devices.
 */
class WebRtcPairingCoordinator(
    context: Context,
    private val signaling: SignalingClient = FirebaseSignalingClient()
) : Closeable {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val manager = PeerConnectionManager(factory(appContext), IceServerProvider())
    private val closed = AtomicBoolean(false)
    private var listener: Closeable? = null
    private var session: PairingSession? = null
    private var offerer = false
    private var remoteDescriptionSet = false
    private val pendingCandidates = mutableListOf<IceCandidate>()

    private val _status = MutableStateFlow("Aguardando início do pareamento.")
    val status: StateFlow<String> = _status.asStateFlow()
    private val _connectionState = MutableStateFlow(WebRtcConnectionState.IDLE)
    val connectionState: StateFlow<WebRtcConnectionState> = _connectionState.asStateFlow()
    private val _remoteHistoryConsent = MutableStateFlow(false)
    val remoteHistoryConsent: StateFlow<Boolean> = _remoteHistoryConsent.asStateFlow()
    private var localHistoryConsent = false
    @Volatile var onHistoryBatchReceived: ((String) -> Unit)? = null

    fun start(pairingSession: PairingSession, isOfferer: Boolean) {
        check(!closed.get()) { "COORDINATOR_CLOSED" }
        if (session != null) return
        session = pairingSession
        offerer = isOfferer

        val created = manager.createConnection(
            onIceCandidate = { candidate ->
                sendSignal(
                    JSONObject()
                        .put("type", "ice")
                        .put("sdpMid", candidate.sdpMid)
                        .put("sdpMLineIndex", candidate.sdpMLineIndex)
                        .put("candidate", candidate.sdp)
                )
            },
            onMessage = { message -> handleDataMessage(message) }
        )
        if (!created) {
            _status.value = "Não foi possível criar a conexão WebRTC."
            return
        }

        if (offerer) manager.createDataChannel()
        listener = signaling.observeMessages(
            sessionId = pairingSession.sessionId,
            onMessage = { raw -> handleSignal(raw) },
            onError = { error -> _status.value = "Erro na sinalização: ${error.message ?: "desconhecido"}" }
        )
        _status.value = "Negociando conexão segura entre os dispositivos…"

        if (offerer) {
            manager.createOffer(
                onCreated = { description ->
                    sendSignal(
                        JSONObject().put("type", "offer").put("sdp", description.description)
                    )
                },
                onError = { error -> _status.value = "Falha ao criar oferta: $error" }
            )
        }
        scope.launch {
            manager.state.collect { state ->
                _connectionState.value = state
                if (state == WebRtcConnectionState.CONNECTED) publishHistoryConsent()
            }
        }
    }

    private fun handleSignal(raw: String) {
        val json = runCatching { JSONObject(raw) }.getOrElse {
            _status.value = "Mensagem de sinalização inválida."
            return
        }
        when (json.optString("type")) {
            "offer" -> {
                if (offerer) return
                val sdp = json.optString("sdp").takeIf { it.isNotBlank() } ?: return
                setRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, sdp)) {
                    manager.createAnswer(
                        onCreated = { answer ->
                            sendSignal(JSONObject().put("type", "answer").put("sdp", answer.description))
                        },
                        onError = { error -> _status.value = "Falha ao criar resposta: $error" }
                    )
                }
            }
            "answer" -> {
                if (!offerer) return
                val sdp = json.optString("sdp").takeIf { it.isNotBlank() } ?: return
                setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, sdp)) {
                    _status.value = "Resposta recebida; estabelecendo conexão…"
                }
            }
            "ice" -> {
                val candidateText = json.optString("candidate").takeIf { it.isNotBlank() } ?: return
                val candidate = IceCandidate(
                    json.optString("sdpMid").takeIf { it.isNotBlank() },
                    json.optInt("sdpMLineIndex"),
                    candidateText
                )
                synchronized(pendingCandidates) {
                    if (!remoteDescriptionSet) pendingCandidates += candidate
                    else if (!manager.addIceCandidate(candidate)) {
                        _status.value = "Não foi possível adicionar um candidato ICE."
                    }
                }
            }
        }
    }

    private fun setRemoteDescription(description: SessionDescription, onSuccess: () -> Unit) {
        manager.setRemoteDescription(
            description,
            onSuccess = {
                val queued = synchronized(pendingCandidates) {
                    remoteDescriptionSet = true
                    pendingCandidates.toList().also { pendingCandidates.clear() }
                }
                queued.forEach { candidate ->
                    if (!manager.addIceCandidate(candidate)) {
                        _status.value = "Não foi possível adicionar um candidato ICE recebido."
                    }
                }
                onSuccess()
            },
            onError = { error -> _status.value = "Falha ao definir descrição remota: $error" }
        )
    }

    private fun sendSignal(json: JSONObject) {
        val current = session ?: return
        scope.launch {
            runCatching { signaling.send(current.sessionId, json.toString()) }
                .onFailure { _status.value = "Falha ao enviar sinalização: ${it.message ?: "erro"}" }
        }
    }


    /**
     * History sharing is disabled by default and must be enabled separately on both devices.
     * Consent is re-announced when the data channel opens after a reconnect.
     */
    fun setHistoryConsent(enabled: Boolean) {
        localHistoryConsent = enabled
        if (!enabled) _status.value = "Compartilhamento de histórico desativado neste aparelho."
        publishHistoryConsent()
    }

    private fun publishHistoryConsent() {
        if (_connectionState.value != WebRtcConnectionState.CONNECTED) return
        val sent = manager.sendMessage(
            JSONObject()
                .put("type", "history_consent")
                .put("enabled", localHistoryConsent)
                .toString()
        )
        if (sent && localHistoryConsent) {
            _status.value = if (_remoteHistoryConsent.value) {
                "Os dois aparelhos autorizaram o compartilhamento do histórico."
            } else {
                "Autorização local ativa; aguardando autorização do outro aparelho."
            }
        }
    }

    /** Sends one bounded batch of message records after explicit consent from both devices. */
    fun sendHistoryBatch(recordsJson: String): Boolean {
        if (!localHistoryConsent || !_remoteHistoryConsent.value) {
            _status.value = "Ative o compartilhamento nos dois aparelhos antes de enviar."
            return false
        }
        val records = runCatching { JSONArray(recordsJson) }.getOrElse {
            _status.value = "Lote de histórico inválido."
            return false
        }
        if (records.length() == 0) return true
        val message = JSONObject()
            .put("type", "history_batch")
            .put("records", records)
            .toString()
        if (message.toByteArray(Charsets.UTF_8).size > MAX_HISTORY_BATCH_BYTES) {
            _status.value = "Lote muito grande; reduza a quantidade de registros."
            return false
        }
        val sent = manager.sendMessage(message)
        _status.value = if (sent) "Lote de histórico enviado." else "Canal de dados ainda não está aberto."
        return sent
    }

    private fun handleDataMessage(message: String) {
        if (message == "p2p:ping") {
            manager.sendMessage("p2p:pong")
            return
        }
        if (message == "p2p:pong") {
            _status.value = "Canal de dados WebRTC validado."
            return
        }
        if (message.toByteArray(Charsets.UTF_8).size > MAX_HISTORY_BATCH_BYTES) {
            _status.value = "Mensagem recebida excede o limite permitido."
            return
        }
        val json = runCatching { JSONObject(message) }.getOrElse { return }
        when (json.optString("type")) {
            "history_consent" -> {
                _remoteHistoryConsent.value = json.optBoolean("enabled", false)
                _status.value = when {
                    !_remoteHistoryConsent.value -> "O outro aparelho não autorizou o compartilhamento."
                    localHistoryConsent -> "Os dois aparelhos autorizaram o compartilhamento do histórico."
                    else -> "O outro aparelho autorizou; ative a opção neste aparelho também."
                }
            }
            "history_batch" -> {
                if (!localHistoryConsent || !_remoteHistoryConsent.value) {
                    _status.value = "Lote ignorado: o compartilhamento não está autorizado nos dois aparelhos."
                    return
                }
                val records = json.optJSONArray("records") ?: return
                if (records.length() > MAX_RECORDS_PER_BATCH) {
                    _status.value = "Lote rejeitado: quantidade de registros acima do limite."
                    return
                }
                onHistoryBatchReceived?.invoke(records.toString())
                _status.value = "Recebidos ${records.length()} registros de histórico."
            }
        }
    }

    /** Sends a harmless connectivity probe. */
    
    fun testDataChannel(): Boolean {
        val sent = manager.sendMessage("p2p:ping")
        if (sent) _status.value = "Teste de canal enviado; aguardando resposta."
        else _status.value = "Canal de dados ainda não está aberto."
        return sent
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        runCatching { listener?.close() }
        listener = null
        manager.close()
        scope.cancel()
        synchronized(pendingCandidates) { pendingCandidates.clear() }
        _status.value = "Conexão encerrada."
    }

    companion object {
        private const val MAX_HISTORY_BATCH_BYTES = 48 * 1024
        private const val MAX_RECORDS_PER_BATCH = 10
        @Volatile private var sharedFactory: PeerConnectionFactory? = null

        private fun factory(context: Context): PeerConnectionFactory {
            initialize(context)
            return checkNotNull(sharedFactory) { "WEBRTC_FACTORY_INITIALIZATION_FAILED" }
        }

        @Synchronized
        fun initialize(context: Context) {
            if (sharedFactory != null) return
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                    .createInitializationOptions()
            )
            sharedFactory = PeerConnectionFactory.builder().createPeerConnectionFactory()
        }
    }
}
