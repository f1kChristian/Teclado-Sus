package com.tecladop2pv2.sync

import com.tecladop2pv2.core.model.SyncState

/** Centralizes legal persistent delivery-state transitions. */
object SyncStateMachine {
    private val allowed = mapOf(
        SyncState.LOCAL_ONLY to setOf(SyncState.QUEUED, SyncState.DELETED),
        SyncState.QUEUED to setOf(SyncState.SENDING, SyncState.DELETED, SyncState.RETRY_WAIT),
        SyncState.SENDING to setOf(SyncState.AWAITING_ACK, SyncState.RETRY_WAIT, SyncState.FAILED_PERMANENT, SyncState.DELETED),
        SyncState.AWAITING_ACK to setOf(SyncState.SYNCED, SyncState.RETRY_WAIT, SyncState.FAILED_PERMANENT, SyncState.DELETED),
        SyncState.RETRY_WAIT to setOf(SyncState.SENDING, SyncState.FAILED_PERMANENT, SyncState.DELETED),
        SyncState.SYNCED to setOf(SyncState.DELETED),
        SyncState.FAILED_PERMANENT to setOf(SyncState.QUEUED, SyncState.DELETED),
        SyncState.DELETED to emptySet()
    )

    fun transition(current: SyncState, next: SyncState): SyncState {
        require(next == current || allowed[current].orEmpty().contains(next)) { "Invalid sync transition: $current -> $next" }
        return next
    }

    fun retryDelayMillis(attempt: Int, randomUnit: Double = Math.random()): Long {
        val capped = (1L shl attempt.coerceIn(0, 10)).coerceAtMost(3600L)
        val jitter = 0.8 + randomUnit.coerceIn(0.0, 1.0) * 0.4
        return (capped * 1_000 * jitter).toLong()
    }
}
