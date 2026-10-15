package com.tecladop2pv2.core.diagnostics

/** Allow-listed technical diagnostics; never include text, image URIs, tokens, or SDP. */
data class DiagnosticEvent(val code: Code, val occurredAt: Long = System.currentTimeMillis()) {
    enum class Code { SIGNALING_UNAVAILABLE, ICE_FAILED, DATA_CHANNEL_CLOSED, ACK_TIMEOUT, INVALID_MESSAGE, AUTHORIZATION_FAILED }
}
