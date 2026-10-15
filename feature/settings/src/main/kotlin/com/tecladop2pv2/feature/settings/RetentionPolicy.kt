package com.tecladop2pv2.feature.settings

/** Local retention bounds; callers should execute prune operations transactionally. */
data class RetentionPolicy(val maxAgeDays: Int = 90, val maxRecords: Int = 5_000) {
    init { require(maxAgeDays in 1..3_650); require(maxRecords in 100..100_000) }
}
