package com.tecladop2pv2.sync

import org.json.JSONObject
import java.util.UUID

/** Versioned envelope helper. Signature verification must be supplied by the identity layer before dispatch. */
data class ProtocolEnvelope(
    val protocol: String,
    val version: Int,
    val messageId: String,
    val sessionId: String,
    val senderDeviceId: String,
    val recipientDeviceId: String,
    val timestamp: Long,
    val type: String,
    val nonce: String,
    val payload: JSONObject,
    val signature: String?
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("protocol", protocol); put("version", version); put("messageId", messageId)
        put("sessionId", sessionId); put("senderDeviceId", senderDeviceId); put("recipientDeviceId", recipientDeviceId)
        put("timestamp", timestamp); put("type", type); put("nonce", nonce); put("payload", payload)
        signature?.let { put("signature", it) }
    }

    companion object {
        const val PROTOCOL = "text-backup"
        const val MAX_ENVELOPE_BYTES = 256 * 1024
        fun parse(json: String, expectedRecipient: String, now: Long = System.currentTimeMillis()): ProtocolEnvelope {
            require(json.toByteArray(Charsets.UTF_8).size <= MAX_ENVELOPE_BYTES) { "MESSAGE_TOO_LARGE" }
            val obj = JSONObject(json)
            require(obj.getString("protocol") == PROTOCOL) { "INVALID_PROTOCOL" }
            require(obj.getInt("version") == 1) { "UNSUPPORTED_VERSION" }
            require(obj.getString("recipientDeviceId") == expectedRecipient) { "WRONG_RECIPIENT" }
            val time = obj.getLong("timestamp")
            require(kotlin.math.abs(now - time) <= 5 * 60 * 1000L) { "STALE_MESSAGE" }
            val id = obj.getString("messageId")
            require(runCatching { UUID.fromString(id) }.isSuccess) { "INVALID_MESSAGE_ID" }
            return ProtocolEnvelope(
                obj.getString("protocol"), obj.getInt("version"), id,
                obj.getString("sessionId"), obj.getString("senderDeviceId"), expectedRecipient,
                time, obj.getString("type"), obj.getString("nonce"), obj.optJSONObject("payload") ?: JSONObject(),
                obj.optString("signature").takeIf { it.isNotEmpty() }
            )
        }
    }
}
