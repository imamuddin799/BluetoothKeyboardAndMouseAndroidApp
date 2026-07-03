package com.arena.hidcompatibilitytester.ui.screen.keyboard

import android.content.Context

object KeyboardSettingsStore {

    private const val PREFS = "keyboard_settings"

    fun save(context: Context, s: KeyboardSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            putBoolean("repeatEnabled", s.repeatEnabled)
            putLong("repeatInitialDelayMs", s.repeatInitialDelayMs)
            putLong("repeatIntervalMs", s.repeatIntervalMs)
            putBoolean("hapticEnabled", s.hapticEnabled)
            putString("hapticIntensity", s.hapticIntensity.name)
            putBoolean("soundOnPress", s.soundOnPress)
            putString("keyHeight", s.keyHeight.name)
            putBoolean("showStatusBar", s.showStatusBar)
            putBoolean("showComboPreview", s.showComboPreview)
            putBoolean("compactModifiers", s.compactModifiers)
            putBoolean("stickyModifiers", s.stickyModifiers)
            putBoolean("keepModsAfterTab", s.keepModsAfterTab)
            putString("keyFontSize", s.keyFontSize.name)
            putBoolean("showKeyHints", s.showKeyHints)
            putBoolean("highContrastMode", s.highContrastMode)
            putBoolean("numpadStartsLocked", s.numpadStartsLocked)
            putBoolean("numpadShowHints", s.numpadShowHints)
            putString("mediaKeySize", s.mediaKeySize.name)
            putBoolean("showMediaRowInKeyboard", s.showMediaRowInKeyboard)
            putBoolean("showMediaRowInTrackpad", s.showMediaRowInTrackpad)
            putBoolean("mediaRowShowTransport", s.mediaRowShowTransport)
            putBoolean("mediaRowShowVolume", s.mediaRowShowVolume)
            putBoolean("mediaRowShowBrightness", s.mediaRowShowBrightness)
            putBoolean("mediaRowRepeatVolume", s.mediaRowRepeatVolume)
            putBoolean("mediaRowRepeatBrightness", s.mediaRowRepeatBrightness)
            putString("mediaRowGroupOrder", s.mediaRowGroupOrder.joinToString(",") { it.name })
            putInt("defaultTab", s.defaultTab)
            apply()
        }
    }

    fun load(context: Context): KeyboardSettings {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return KeyboardSettings(
            repeatEnabled = p.getBoolean("repeatEnabled", true),
            repeatInitialDelayMs = p.getLong("repeatInitialDelayMs", 400L),
            repeatIntervalMs = p.getLong("repeatIntervalMs", 50L),
            hapticEnabled = p.getBoolean("hapticEnabled", true),
            hapticIntensity = runCatching {
                HapticIntensity.valueOf(p.getString("hapticIntensity", "MEDIUM")!!)
            }.getOrDefault(HapticIntensity.MEDIUM),
            soundOnPress = p.getBoolean("soundOnPress", false),
            keyHeight = runCatching {
                KeyHeight.valueOf(p.getString("keyHeight", "MEDIUM")!!)
            }.getOrDefault(KeyHeight.MEDIUM),
            showStatusBar = p.getBoolean("showStatusBar", true),
            showComboPreview = p.getBoolean("showComboPreview", true),
            compactModifiers = p.getBoolean("compactModifiers", false),
            stickyModifiers = p.getBoolean("stickyModifiers", false),
            keepModsAfterTab = p.getBoolean("keepModsAfterTab", true),
            keyFontSize = runCatching {
                KeyFontSize.valueOf(p.getString("keyFontSize", "MEDIUM")!!)
            }.getOrDefault(KeyFontSize.MEDIUM),
            showKeyHints = p.getBoolean("showKeyHints", true),
            highContrastMode = p.getBoolean("highContrastMode", false),
            numpadStartsLocked = p.getBoolean("numpadStartsLocked", true),
            numpadShowHints = p.getBoolean("numpadShowHints", true),
            mediaKeySize = runCatching {
                MediaKeySize.valueOf(p.getString("mediaKeySize", "MEDIUM")!!)
            }.getOrDefault(MediaKeySize.MEDIUM),
            showMediaRowInKeyboard = p.getBoolean("showMediaRowInKeyboard", false),
            showMediaRowInTrackpad = p.getBoolean("showMediaRowInTrackpad", false),
            mediaRowShowTransport = p.getBoolean("mediaRowShowTransport", true),
            mediaRowShowVolume = p.getBoolean("mediaRowShowVolume", true),
            mediaRowShowBrightness = p.getBoolean("mediaRowShowBrightness", true),
            mediaRowRepeatVolume = p.getBoolean("mediaRowRepeatVolume", true),
            mediaRowRepeatBrightness = p.getBoolean("mediaRowRepeatBrightness", true),
            mediaRowGroupOrder = runCatching {
                p.getString("mediaRowGroupOrder", null)
                    ?.split(",")
                    ?.map { MediaRowGroup.valueOf(it.trim()) }
                    ?: listOf(
                        MediaRowGroup.TRANSPORT,
                        MediaRowGroup.VOLUME,
                        MediaRowGroup.BRIGHTNESS
                    )
            }.getOrDefault(
                listOf(
                    MediaRowGroup.TRANSPORT,
                    MediaRowGroup.VOLUME,
                    MediaRowGroup.BRIGHTNESS
                )
            ),
            defaultTab = p.getInt("defaultTab", 0),
        )
    }
}