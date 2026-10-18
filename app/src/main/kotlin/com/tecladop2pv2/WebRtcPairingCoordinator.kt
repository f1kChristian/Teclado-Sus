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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import java.util.UUID

/**
 * Coordinates Firebase signaling and a WebRTC data channel for an explicitly initiated pairing.
 * History transfer is automatic.
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
    private var currentAttemptId: String? = null
    private var retryJob: Job? = null
    private var retryAttempt = 0
    private val pendingCandidates = mutableListOf<IceCandidate>()

    private val _status = MutableStateFlow("Aguardando início do pareamento.")
    val status: StateFlow<String> = _status.asStateFlow()
    private val _connectionState = MutableStateFlow(WebRtcConnectionState.IDLE)
    val connectionState: StateFlow<WebRtcConnectionState> = _connectionState.asStateFlow()
    @Volatile var onHistoryBatchReceived: ((String) -> Unit)? = null

    fun start(pairingSession: PairingSession, isOfferer: Boolean) {
        check(!closed.get()) { "COORDINATOR_CLOSED" }
        if (session != null) return
        session = pairingSession
        offerer = isOfferer

        listener = signaling.observeMessages(
            sessionId = pairingSession.sessionId,
            onMessage = { raw -> handleSignal(raw) },
            onError = { error ->
                _status.value = "Erro na sinalização: ${error.message ?: "desconhecido"}"
                scheduleReconnect()
            }
        )
        _status.value = "Negociando conexão segura entre os dispositivos…"

        scope.launch {
            manager.state.collect { state ->
                _connectionState.value = state
                when (state) {
                    WebRtcConnectionState.CONNECTED -> {
                        retryAttempt = 0
                        retryJob?.cancel()
                        retryJob = null
                    }
                    WebRtcConnectionState.DISCONNECTED,
                    WebRtcConnectionState.FAILED -> scheduleReconnect()
                    else -> Unit
                }
            }
        }

        if (offerer) beginOfferAttempt() else createPeerConnection()
    }

    private fun createPeerConnection(): Boolean {
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
            return false
        }
        return true
    }

    /** The offerer owns negotiation IDs; each retry creates a fresh WebRTC negotiation. */
    private fun beginOfferAttempt() {
        if (closed.get() || !offerer) return
        val current = session ?: return
        if (System.currentTimeMillis() >= current.expiresAtMillis) {
            _status.value = "A sessão de pareamento expirou. Gere um novo QR Code para reconectar."
            return
        }

        retryJob?.cancel()
        retryJob = null
        currentAttemptId = UUID.randomUUID().toString()
        remoteDescriptionSet = false
        synchronized(pendingCandidates) { pendingCandidates.clear() }
        if (!createPeerConnection()) {
            scheduleReconnect()
            return
        }
        manager.createDataChannel()
        _status.value = "Tentando reconectar automaticamente…"
        manager.createOffer(
            onCreated = { description ->
                sendSignal(
                    JSONObject().put("type", "offer").put("sdp", description.description)
                )
            },
            onError = { error ->
                _status.value = "Falha ao criar oferta: $error"
                scheduleReconnect()
            }
        )
    }

    private fun scheduleReconnect() {
        if (closed.get() || retryJob?.isActive == true) return
        retryJob = scope.launch {
            val waitMs = (2_000L * (1L shl retryAttempt.coerceAtMost(4))).coerceAtMost(30_000L)
            delay(waitMs)
            if (closed.get()) return@launch
            retryJob = null
            if (_connectionState.value == WebRtcConnectionState.CONNECTED) {
                retryAttempt = 0
                return@launch
            }
            val current = session ?: return@launch
            if (System.currentTimeMillis() >= current.expiresAtMillis) {
                _status.value = "A sessão de pareamento expirou. Gere um novo QR Code para reconectar."
                return@launch
            }
            retryAttempt = (retryAttempt + 1).coerceAtMost(5)
            if (offerer) {
                beginOfferAttempt()
            } else {
                currentAttemptId = null
                remoteDescriptionSet = false
                synchronized(pendingCandidates) { pendingCandidates.clear() }
                if (createPeerConnection()) {
                    _status.value = "Conexão perdida; aguardando nova negociação do dispositivo pareado…"
                    sendSignal(JSONObject().put("type", "reconnect_request"))
                } else {
                    scheduleReconnect()
                }
            }
        }
    }

    private fun handleSignal(raw: String) {
        val json = runCatching { JSONObject(raw) }.getOrElse {
            _status.value = "Mensagem de sinalização inválida."
            return
        }
        when (json.optString("type")) {
            "reconnect_request" -> {
                if (offerer) beginOfferAttempt()
            }
            "offer" -> {
                if (offerer) return
                val sdp = json.optString("sdp").takeIf { it.isNotBlank() } ?: return
                val incomingAttempt = json.optString("attemptId").takeIf { it.isNotBlank() } ?: return
                if (incomingAttempt != currentAttemptId) {
                    val hadAttempt = currentAttemptId != null && remoteDescriptionSet
                    currentAttemptId = incomingAttempt
                    remoteDescriptionSet = false
                    synchronized(pendingCandidates) { pendingCandidates.clear() }
                    if (hadAttempt && !createPeerConnection()) return
                }
                setRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, sdp)) {
                    manager.createAnswer(
                        onCreated = { answer ->
                            sendSignal(JSONObject().put("type", "answer").put("sdp", answer.description))
                        },
                        onError = { error ->
                            _status.value = "Falha ao criar resposta: $error"
                            scheduleReconnect()
                        }
                    )
                }
            }
            "answer" -> {
                if (!offerer) return
                val incomingAttempt = json.optString("attemptId")
                if (incomingAttempt.isBlank() || incomingAttempt != currentAttemptId) return
                val sdp = json.optString("sdp").takeIf { it.isNotBlank() } ?: return
                setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, sdp)) {
                    _status.value = "Resposta recebida; estabelecendo conexão…"
                }
            }
            "ice" -> {
                val incomingAttempt = json.optString("attemptId").takeIf { it.isNotBlank() } ?: return
                if (offerer && incomingAttempt != currentAttemptId) return
                if (!offerer && currentAttemptId == null) currentAttemptId = incomingAttempt
                if (incomingAttempt != currentAttemptId) return
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
        currentAttemptId?.let { json.put("attemptId", it) }
        val payload = json.toString()
        scope.launch {
            runCatching { signaling.send(current.sessionId, payload) }
                .onFailure { _status.value = "Falha ao enviar sinalização: ${it.message ?: "erro"}" }
        }
    }

    /** Sends one bounded batch of message records over the data channel. */
    fun sendHistoryBatch(recordsJson: String): Boolean {
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
            "history_batch" -> {
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
        retryJob?.cancel()
        retryJob = null
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
