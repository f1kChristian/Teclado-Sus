
package com.tecladop2pv2

import com.tecladop2pv2.signaling.FirebaseSignalingClient
import com.tecladop2pv2.feature.pairing.PairingController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object PairingManager {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main
    )

    private val signalingClient by lazy {
        FirebaseSignalingClient()
    }

    val controller by lazy {
        PairingController(
            signalingClient = signalingClient,
            scope = scope
        )
    }
}
