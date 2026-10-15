package com.tecladop2pv2.feature.customization

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

object Contrast {
    fun ratio(foreground: Color, background: Color): Float {
        val a = foreground.luminance()
        val b = background.luminance()
        return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
    }

    fun isReadable(foreground: Color, background: Color): Boolean = ratio(foreground, background) >= 4.5f
}
