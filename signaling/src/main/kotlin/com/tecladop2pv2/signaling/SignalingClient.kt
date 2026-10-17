package com.tecladop2pv2.signaling

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.io.Closeable
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
    fun observeMessages(
        sessionId: String,
        onMessage: (String) -> Unit,
        onError: (Throwable) -> Unit = {}
    ): Closeable
    suspend fun cancel(sessionId: String)
}

class FirebaseSignalingClient : SignalingClient {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(
        "https://tecladop2p-default-rtdb.firebaseio.com/"
    )
    private val sessions = database.getReference("pairingSessions")

    private suspend fun ensureAuthenticated() {
        if (auth.currentUser == null) auth.signInAnonymously().await()
        check(auth.currentUser != null) { "FIREBASE_AUTH_REQUIRED" }
    }

    override suspend fun createSession(): PairingSession {
        ensureAuthenticated()
        val sessionId = UUID.randomUUID().toString()
        val code = (100000..999999).random().toString()
        val expiresAt = System.currentTimeMillis() + SESSION_DURATION_MS
        val uid = auth.currentUser?.uid ?: error("FIREBASE_AUTH_REQUIRED")
        sessions.child(sessionId).setValue(
            mapOf(
                "sessionId" to sessionId,
                "code" to code,
                "ownerUid" to uid,
                "createdAt" to ServerValue.TIMESTAMP,
                "expiresAt" to expiresAt,
                "status" to "waiting"
            )
        ).await()
        return PairingSession(sessionId, code, expiresAt)
    }

    override suspend fun joinSession(sessionCode: String): PairingSession {
        ensureAuthenticated()
        require(sessionCode.matches(Regex("^\\d{6}$"))) { "INVALID_PAIRING_CODE" }

        val matches = sessions.orderByChild("code").equalTo(sessionCode).get().await()
        val match = matches.children.firstOrNull() ?: error("SESSION_NOT_FOUND")
        val sessionId = match.child("sessionId").getValue(String::class.java)
            ?: match.key ?: error("INVALID_SESSION")
        val expiresAt = match.child("expiresAt").getValue(Long::class.java)
            ?: error("INVALID_SESSION")
        if (expiresAt <= System.currentTimeMillis()) error("SESSION_EXPIRED")

        val uid = auth.currentUser?.uid ?: error("FIREBASE_AUTH_REQUIRED")
        claimSession(sessionId, sessionCode, uid)
        return PairingSession(sessionId, sessionCode, expiresAt)
    }

    private suspend fun claimSession(sessionId: String, code: String, uid: String) =
        suspendCancellableCoroutine<Unit> { continuation ->
            sessions.child(sessionId).runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    if (!currentData.exists()) return Transaction.abort()
                    if (currentData.child("code").getValue(String::class.java) != code) {
                        return Transaction.abort()
                    }
                    val expiry = currentData.child("expiresAt").getValue(Long::class.java)
                        ?: return Transaction.abort()
                    if (expiry <= System.currentTimeMillis()) return Transaction.abort()
                    if (currentData.child("status").getValue(String::class.java) != "waiting") {
                        return Transaction.abort()
                    }
                    currentData.child("status").value = "joined"
                    currentData.child("joinedUid").value = uid
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (!continuation.isActive) return
                    when {
                        error != null -> continuation.resumeWithException(error.toException())
                        committed -> continuation.resume(Unit)
                        currentData?.child("expiresAt")?.getValue(Long::class.java)
                            ?.let { it <= System.currentTimeMillis() } == true ->
                            continuation.resumeWithException(IllegalStateException("SESSION_EXPIRED"))
                        currentData?.child("status")?.getValue(String::class.java) == "joined" ->
                            continuation.resumeWithException(IllegalStateException("SESSION_ALREADY_JOINED"))
                        else -> continuation.resumeWithException(IllegalStateException("SESSION_NOT_FOUND"))
                    }
                }
            }, false)
        }

    override suspend fun send(sessionId: String, message: String) {
        ensureAuthenticated()
        require(runCatching { UUID.fromString(sessionId) }.isSuccess) { "INVALID_SESSION_ID" }
        require(message.toByteArray(Charsets.UTF_8).size <= MAX_SIGNAL_MESSAGE_BYTES) {
            "SIGNAL_MESSAGE_TOO_LARGE"
        }
        val uid = auth.currentUser?.uid ?: error("FIREBASE_AUTH_REQUIRED")
        val session = sessions.child(sessionId).get().await()
        val ownerUid = session.child("ownerUid").getValue(String::class.java)
        val joinedUid = session.child("joinedUid").getValue(String::class.java)
        val expiry = session.child("expiresAt").getValue(Long::class.java) ?: 0L
        require(expiry > System.currentTimeMillis()) { "SESSION_EXPIRED" }
        require(uid == ownerUid || uid == joinedUid) { "SESSION_ACCESS_DENIED" }

        sessions.child(sessionId).child("messages").push().setValue(
            mapOf(
                "senderUid" to uid,
                "payload" to message,
                "createdAt" to ServerValue.TIMESTAMP
            )
        ).await()
    }

    override suspend fun messages(sessionId: String, onMessage: (String) -> Unit) {
        ensureAuthenticated()
        observeMessages(sessionId, onMessage)
    }

    override fun observeMessages(
        sessionId: String,
        onMessage: (String) -> Unit,
        onError: (Throwable) -> Unit
    ): Closeable {
        require(runCatching { UUID.fromString(sessionId) }.isSuccess) { "INVALID_SESSION_ID" }
        val uid = auth.currentUser?.uid ?: error("FIREBASE_AUTH_REQUIRED")
        val ref = sessions.child(sessionId).child("messages")
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val senderUid = snapshot.child("senderUid").getValue(String::class.java)
                if (senderUid == uid) return
                val payload = snapshot.child("payload").getValue(String::class.java) ?: return
                if (payload.toByteArray(Charsets.UTF_8).size > MAX_SIGNAL_MESSAGE_BYTES) {
                    onError(IllegalStateException("SIGNAL_MESSAGE_TOO_LARGE"))
                    return
                }
                onMessage(payload)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) = Unit
            override fun onChildRemoved(snapshot: DataSnapshot) = Unit
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit
            override fun onCancelled(error: DatabaseError) = onError(error.toException())
        }
        ref.addChildEventListener(listener)
        return Closeable { ref.removeEventListener(listener) }
    }

    override suspend fun cancel(sessionId: String) {
        ensureAuthenticated()
        require(runCatching { UUID.fromString(sessionId) }.isSuccess) { "INVALID_SESSION_ID" }
        val uid = auth.currentUser?.uid ?: error("FIREBASE_AUTH_REQUIRED")
        val session = sessions.child(sessionId).get().await()
        require(session.child("ownerUid").getValue(String::class.java) == uid) {
            "SESSION_CANCEL_NOT_OWNER"
        }
        sessions.child(sessionId).removeValue().await()
    }

    companion object {
        private const val SESSION_DURATION_MS = 5 * 60 * 1000L
        private const val MAX_SIGNAL_MESSAGE_BYTES = 256 * 1024
    }
}
