package com.tecladop2pv2.feature.customization

import com.tecladop2pv2.core.model.KeyboardCustomization
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CustomizationRepository {

    private val _customization =
        MutableStateFlow(KeyboardCustomization())

    val customization: StateFlow<KeyboardCustomization> =
        _customization.asStateFlow()

    private val mutex = Mutex()

    fun getCurrent(): KeyboardCustomization {
        return _customization.value
    }

    suspend fun update(
        transform: (KeyboardCustomization) -> KeyboardCustomization
    ) {
        mutex.withLock {
            _customization.value = transform(_customization.value)
        }
    }

    suspend fun reset() {
        mutex.withLock {
            _customization.value = KeyboardCustomization()
        }
    }
}
