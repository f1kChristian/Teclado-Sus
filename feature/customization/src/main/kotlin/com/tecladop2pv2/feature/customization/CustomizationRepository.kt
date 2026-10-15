
package com.tecladop2pv2.feature.customization

import com.tecladop2pv2.core.model.KeyboardCustomization
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CustomizationRepository {

    private val _customization =
        MutableStateFlow(KeyboardCustomization())

    val customization: StateFlow<KeyboardCustomization> =
        _customization.asStateFlow()

    fun getCurrent(): KeyboardCustomization {
        return _customization.value
    }

    fun update(customization: KeyboardCustomization) {
        _customization.value = customization
    }
}
