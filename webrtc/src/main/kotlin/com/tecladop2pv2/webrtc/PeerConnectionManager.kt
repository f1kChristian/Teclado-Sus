package com.tecladop2pv2.webrtc

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription

enum class WebRtcConnectionState {
    IDLE,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    FAILED,
    CLOSED
}

class PeerConnectionManager(
    private val factory: PeerConnectionFactory,
    private val iceServerProvider: IceServerProvider = IceServerProvider()
) {

    private val _state = MutableStateFlow(WebRtcConnectionState.IDLE)
    val state: StateFlow<WebRtcConnectionState> = _state.asStateFlow()

    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null

    private var onLocalIceCandidate: ((IceCandidate) -> Unit)? = null
    private var onMessageReceived: ((String) -> Unit)? = null

    fun createConnection(
        onIceCandidate: (IceCandidate) -> Unit,
        onMessage: (String) -> Unit
    ): Boolean {
        close()

        onLocalIceCandidate = onIceCandidate
        onMessageReceived = onMessage

        val configuration = PeerConnection.RTCConfiguration(
            iceServerProvider.servers()
        ).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        val observer = object : PeerConnection.Observer {

            override fun onSignalingChange(
                state: PeerConnection.SignalingState
            ) = Unit

            override fun onIceConnectionChange(
                state: PeerConnection.IceConnectionState
            ) {
                _state.value = when (state) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED ->
                        WebRtcConnectionState.CONNECTED

                    PeerConnection.IceConnectionState.CHECKING ->
                        WebRtcConnectionState.CONNECTING

                    PeerConnection.IceConnectionState.DISCONNECTED ->
                        WebRtcConnectionState.DISCONNECTED

                    PeerConnection.IceConnectionState.FAILED ->
                        WebRtcConnectionState.FAILED

                    PeerConnection.IceConnectionState.CLOSED ->
                        WebRtcConnectionState.CLOSED

                    else -> _state.value
                }
            }

            override fun onIceConnectionReceivingChange(
                receiving: Boolean
            ) = Unit

            override fun onIceGatheringChange(
                state: PeerConnection.IceGatheringState
            ) = Unit

            override fun onIceCandidate(candidate: IceCandidate) {
                onLocalIceCandidate?.invoke(candidate)
            }

            override fun onIceCandidatesRemoved(
                candidates: Array<out IceCandidate>
            ) = Unit

            override fun onAddStream(stream: org.webrtc.MediaStream) = Unit

            override fun onRemoveStream(stream: org.webrtc.MediaStream) = Unit

            override fun onDataChannel(channel: DataChannel) {
                attachDataChannel(channel)
            }

            override fun onRenegotiationNeeded() = Unit

            override fun onAddTrack(
                receiver: org.webrtc.RtpReceiver,
                streams: Array<out org.webrtc.MediaStream>
            ) = Unit

            override fun onConnectionChange(
                state: PeerConnection.PeerConnectionState
            ) {
                if (state == PeerConnection.PeerConnectionState.FAILED) {
                    _state.value = WebRtcConnectionState.FAILED
                }
            }
        }

        val connection = factory.createPeerConnection(
            configuration,
            observer
        ) ?: run {
            _state.value = WebRtcConnectionState.FAILED
            return false
        }

        peerConnection = connection
        _state.value = WebRtcConnectionState.CONNECTING
        return true
    }

    fun createDataChannel(label: String = "teclado-history"): Boolean {
        val connection = peerConnection ?: return false

        val channel = connection.createDataChannel(
            label,
            DataChannel.Init()
        ) ?: return false

        attachDataChannel(channel)
        return true
    }

    private fun attachDataChannel(channel: DataChannel) {
        dataChannel?.unregisterObserver()
        dataChannel = channel

        channel.registerObserver(object : DataChannel.Observer {

            override fun onBufferedAmountChange(previousAmount: Long) = Unit

            override fun onStateChange() {
                if (channel.state() == DataChannel.State.OPEN) {
                    _state.value = WebRtcConnectionState.CONNECTED
                }
            }

            override fun onMessage(buffer: DataChannel.Buffer) {
                if (buffer.binary) return

                val bytes = ByteArray(buffer.data.remaining())
                buffer.data.get(bytes)

                onMessageReceived?.invoke(
                    bytes.toString(Charsets.UTF_8)
                )
            }
        })
    }

    fun createOffer(
        onCreated: (SessionDescription) -> Unit,
        onError: (String) -> Unit
    ) {
        val connection = peerConnection ?: run {
            onError("CONNECTION_NOT_CREATED")
            return
        }

        connection.createOffer(
            object : SdpObserverAdapter() {
                override fun onCreateSuccess(description: SessionDescription) {
                    connection.setLocalDescription(
                        object : SdpObserverAdapter() {
                            override fun onSetSuccess() {
                                onCreated(description)
                            }

                            override fun onCreateFailure(error: String) {
                                onError(error)
                            }
                        },
                        description
                    )
                }

                override fun onCreateFailure(error: String) {
                    onError(error)
                }
            },
            MediaConstraints()
        )
    }

    fun createAnswer(
        onCreated: (SessionDescription) -> Unit,
        onError: (String) -> Unit
    ) {
        val connection = peerConnection ?: run {
            onError("CONNECTION_NOT_CREATED")
            return
        }

        connection.createAnswer(
            object : SdpObserverAdapter() {
                override fun onCreateSuccess(description: SessionDescription) {
                    connection.setLocalDescription(
                        object : SdpObserverAdapter() {
                            override fun onSetSuccess() {
                                onCreated(description)
                            }

                            override fun onCreateFailure(error: String) {
                                onError(error)
                            }
                        },
                        description
                    )
                }

                override fun onCreateFailure(error: String) {
                    onError(error)
                }
            },
            MediaConstraints()
        )
    }

    fun setRemoteDescription(
        description: SessionDescription,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val connection = peerConnection ?: run {
            onError("CONNECTION_NOT_CREATED")
            return
        }

        connection.setRemoteDescription(
            object : SdpObserverAdapter() {
                override fun onSetSuccess() {
                    onSuccess()
                }

                override fun onCreateFailure(error: String) {
                    onError(error)
                }
            },
            description
        )
    }

    fun addIceCandidate(candidate: IceCandidate): Boolean {
        return peerConnection?.addIceCandidate(candidate) ?: false
    }

    fun sendMessage(message: String): Boolean {
        val channel = dataChannel ?: return false

        if (channel.state() != DataChannel.State.OPEN) {
            return false
        }

        val bytes = java.nio.ByteBuffer.wrap(
            message.toByteArray(Charsets.UTF_8)
        )

        return channel.send(
            DataChannel.Buffer(bytes, false)
        )
    }

    fun close() {
        dataChannel?.unregisterObserver()
        dataChannel?.close()
        dataChannel = null

        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null

        onLocalIceCandidate = null
        onMessageReceived = null

        _state.value = WebRtcConnectionState.CLOSED
    }
}

open class SdpObserverAdapter : org.webrtc.SdpObserver {

    override fun onCreateSuccess(description: SessionDescription) = Unit

    override fun onSetSuccess() = Unit

    override fun onCreateFailure(error: String) = Unit

    override fun onSetFailure(error: String) {
        onCreateFailure(error)
    }
}
