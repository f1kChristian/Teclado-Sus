package com.tecladop2pv2.core.model

data class KeyboardCustomization(
    val profileId: String = "default",
    val name: String = "Padrão",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val backgroundType: BackgroundType = BackgroundType.SOLID,
    val backgroundUri: String? = null,
    val backgroundColor: Long = 0xFFE8EBF0,
    val secondaryBackgroundColor: Long = 0xFFDDE2E9,
    val keyColor: Long = 0xFFFFFFFF,
    val specialKeyColor: Long = 0xFFD4DCE7,
    val pressedKeyColor: Long = 0xFFB8C5D8,
    val textColor: Long = 0xFF18202A,
    val keyOpacity: Float = 1f,
    val cornerRadius: Float = 8f,
    val keySpacing: Float = 4f,
    val keyboardHeightScale: Float = 1f,
    val soundEnabled: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val showNumberRow: Boolean = false,
    val showEmoji: Boolean = true,
    val showLanguage: Boolean = true,
    val showHistory: Boolean = true,
    val showSyncIndicator: Boolean = true,
    val showKeyPreview: Boolean = true,
    val schemaVersion: Int = 1,
    val updatedAt: Long = System.currentTimeMillis()
)
