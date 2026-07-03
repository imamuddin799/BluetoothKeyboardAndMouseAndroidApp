package com.arena.hidcompatibilitytester.ui.screen.keyboard

data class KeyboardSettings(
    // Key repeat
    val repeatEnabled: Boolean = true,
    val repeatInitialDelayMs: Long = 400L,
    val repeatIntervalMs: Long = 50L,

    // Haptics
    val hapticEnabled: Boolean = true,
    val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,

    // Sound
    val soundOnPress: Boolean = false,

    // Layout
    val keyHeight: KeyHeight = KeyHeight.MEDIUM,
    val showStatusBar: Boolean = true,
    val showComboPreview: Boolean = true, // <- add this
    val compactModifiers: Boolean = false,

    // Behavior
    val stickyModifiers: Boolean = false,
    val keepModsAfterTab: Boolean = true,

    // Appearance
    val keyFontSize: KeyFontSize = KeyFontSize.MEDIUM,
    val showKeyHints: Boolean = true,
    val highContrastMode: Boolean = false,

    // Numpad
    val numpadStartsLocked: Boolean = true,
    val numpadShowHints: Boolean = true,

    // Media
    val mediaKeySize: MediaKeySize = MediaKeySize.MEDIUM,

    // Default tab
    val defaultTab: Int = 0,
)

enum class HapticIntensity(val label: String) {
    LIGHT("Light"),
    MEDIUM("Medium"),
    STRONG("Strong"),
}

enum class KeyHeight(val label: String, val mainDp: Int, val fnDp: Int, val navDp: Int) {
    SMALL("Small", 34, 32, 38),
    MEDIUM("Medium", 40, 38, 46),
    LARGE("Large", 48, 44, 52),
}

enum class KeyFontSize(val label: String, val baseSp: Int) {
    SMALL("Small", 10),
    MEDIUM("Medium", 12),
    LARGE("Large", 14),
}

enum class MediaKeySize(val label: String, val heightDp: Int) {
    SMALL("Small", 48),
    MEDIUM("Medium", 64),
    LARGE("Large", 80),
}