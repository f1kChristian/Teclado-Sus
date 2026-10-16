
package com.tecladop2pv2.signaling

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.database
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class PairingSession(
    val sessionId: String,
    val oneTimeCode: String,
    val expiresAtMillis: Long
)

interface SignalingClient {
    suspend fun createSession(): PairingSession
    suspend fun joinSession(sessionCode: String): PairingSession
    suspend fun send(sessionId: String, message: String)
    suspend fun messages(sessionId: String, onMessage: (String) -> Unit)
    suspend fun cancel(sessionId: String)
}

class FirebaseSignalingClient : SignalingClient {

    private val database = Firebase.database(
        "https://tecladop2p-default-rtdb.firebaseio.com/"
    )

    private val sessions = database.getReference("pairingSessions")

    private suspend fun ensureAuthenticated() {
        if (Firebase.auth.currentUser == null) {
            Firebase.auth.signInAnonymously().await()
        }
    }

    override suspend fun createSession(): PairingSession {
        ensureAuthenticated()

        val sessionId = UUID.randomUUID().toString()
        val code = (100000..999999).random().toString()
        val expiresAt = System.currentTimeMillis() + SESSION_DURATION_MS
        val uid = Firebase.auth.currentUser?.uid
            ?: error("FIREBASE_AUTH_REQUIRED")

        val data = mapOf(
            "sessionId" to sessionId,
            "code" to code,
            "ownerUid" to uid,
            "createdAt" to ServerValue.TIMESTAMP,
            "expiresAt" to expiresAt,
            "status" to "waiting"
        )

        sessions.child(sessionId).setValue(data).await()

        return PairingSession(
            sessionId = sessionId,
            oneTimeCode = code,
            expiresAtMillis = expiresAt
        )
    }

    override suspend fun joinSession(sessionCode: String): PairingSession {
        ensureAuthenticated()

        require(sessionCode.matches(Regex("\\d{6}"))) {
            "INVALID_PAIRING_CODE"
        }

        val snapshot = sessions.get().await()
        val match = snapshot.children.firstOrNull {
            it.child("code").getValue(String::class.java) == sessionCode
        } ?: error("SESSION_NOT_FOUND")

        val expiresAt = match.child("expiresAt").getValue(Long::class.java)
            ?: error("INVALID_SESSION")

        require(expiresAt > System.currentTimeMillis()) {
            "SESSION_EXPIRED"
        }

        val sessionId = match.child("sessionId").getValue(String::class.java)
            ?: match.key
            ?: error("INVALID_SESSION")

        sessions.child(sessionId).child("status").setValue("joined").await()

        return PairingSession(
            sessionId = sessionId,
            oneTimeCode = sessionCode,
            expiresAtMillis = expiresAt
        )
    }

    override suspend fun send(sessionId: String, message: String) {
        ensureAuthenticated()

        require(sessionId.matches(Regex("[a-fA-F0-9-]{36}"))) {
            "INVALID_SESSION_ID"
        }

        val ref = sessions.child(sessionId).child("messages").push()
        ref.setValue(
            mapOf(
                "senderUid" to Firebase.auth.currentUser?.uid,
                "payload" to message,
                "createdAt" to ServerValue.TIMESTAMP
            )
        ).await()
    }

    override suspend fun messages(
        sessionId: String,
        onMessage: (String) -> Unit
    ) {
        ensureAuthenticated()

        val ref = sessions.child(sessionId).child("messages")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { child ->
                    child.child("payload")
                        .getValue(String::class.java)
                        ?.let(onMessage)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Acesso negado ou falha de rede.
            }
        }

        ref.addValueEventListener(listener)
    }

    override suspend fun cancel(sessionId: String) {
        ensureAuthenticated()
        sessions.child(sessionId).removeValue().await()
    }

    companion object {
        private const val SESSION_DURATION_MS = 5 * 60 * 1000L
    }
}
