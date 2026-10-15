package com.tecladop2pv2.webrtc

import org.webrtc.PeerConnection

/** TURN credentials must be short-lived and delivered by a configured signaling backend. */
class IceServerProvider(private val turnServers: List<TurnCredential> = emptyList()) {
    fun servers(): List<PeerConnection.IceServer> = buildList {
        add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        turnServers.forEach { turn ->
            add(PeerConnection.IceServer.builder(turn.urls).setUsername(turn.username).setPassword(turn.password).createIceServer())
        }
    }
}

data class TurnCredential(val urls: List<String>, val username: String, val password: String)
