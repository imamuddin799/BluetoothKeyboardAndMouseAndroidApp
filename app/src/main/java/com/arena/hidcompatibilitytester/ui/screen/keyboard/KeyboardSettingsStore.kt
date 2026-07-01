// ui/screen/keyboard/KeyboardSettingsStore.kt
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
            putString("keyHeight", s.keyHeight.name)
            putBoolean("showFunctionRow", s.showFunctionRow)
            putBoolean("showStatusBar", s.showStatusBar)
            putBoolean("compactModifiers", s.compactModifiers)
            putBoolean("stickyModifiers", s.stickyModifiers)
            putBoolean("autoReleaseModsAfterKey", s.autoReleaseModsAfterKey)
            putBoolean("capsLockWarning", s.capsLockWarning)
            putBoolean("soundOnPress", s.soundOnPress)
            putString("keyFontSize", s.keyFontSize.name)
            putBoolean("showKeyHints", s.showKeyHints)
            putBoolean("highContrastMode", s.highContrastMode)
            putBoolean("numpadStartsLocked", s.numpadStartsLocked)
            putBoolean("numpadShowHints", s.numpadShowHints)
            putString("mediaKeySize", s.mediaKeySize.name)
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
            keyHeight = runCatching {
                KeyHeight.valueOf(p.getString("keyHeight", "MEDIUM")!!)
            }.getOrDefault(KeyHeight.MEDIUM),
            showFunctionRow = p.getBoolean("showFunctionRow", true),
            showStatusBar = p.getBoolean("showStatusBar", true),
            compactModifiers = p.getBoolean("compactModifiers", false),
            stickyModifiers = p.getBoolean("stickyModifiers", false),
            autoReleaseModsAfterKey = p.getBoolean("autoReleaseModsAfterKey", true),
            capsLockWarning = p.getBoolean("capsLockWarning", true),
            soundOnPress = p.getBoolean("soundOnPress", false),
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
            defaultTab = p.getInt("defaultTab", 0),
        )
    }
}