package com.arena.hidcompatibilitytester.settings

/**
 * Syncs common fields between Portrait and Landscape when sync flags are ON.
 * Only common interface fields are mirrored — orientation-specific fields stay independent.
 */
object SettingsSyncManager {

    // ═════════════════════════════════════════════════════════════════════
    // KEYBOARD SYNC
    // ═════════════════════════════════════════════════════════════════════

    /**
     * When Portrait keyboard changes AND sync is ON, mirror common fields to Landscape.
     */
    fun applyKeyboardSyncFromPortrait(settings: AppSettings): AppSettings {
        if (!settings.syncFlags.syncKeyboardPortraitLandscape) return settings

        val src = settings.portraitKeyboard
        val target = settings.landscapeKeyboard.copy(
            repeatEnabled = src.repeatEnabled,
            repeatInitialDelayMs = src.repeatInitialDelayMs,
            repeatIntervalMs = src.repeatIntervalMs,
            hapticEnabled = src.hapticEnabled,
            hapticIntensity = src.hapticIntensity,
            soundOnPress = src.soundOnPress,
            stickyModifiers = src.stickyModifiers,
            keepModsAfterTab = src.keepModsAfterTab,
            showKeyHints = src.showKeyHints,
            highContrastMode = src.highContrastMode,
            compactModifiers = src.compactModifiers,
            numpadStartsLocked = src.numpadStartsLocked,
            numpadShowHints = src.numpadShowHints,
            showStatusBar = src.showStatusBar,
            showComboPreview = src.showComboPreview,
        )
        return settings.copy(landscapeKeyboard = target)
    }

    /**
     * When Landscape keyboard changes AND sync is ON, mirror common fields to Portrait.
     */
    fun applyKeyboardSyncFromLandscape(settings: AppSettings): AppSettings {
        if (!settings.syncFlags.syncKeyboardPortraitLandscape) return settings

        val src = settings.landscapeKeyboard
        val target = settings.portraitKeyboard.copy(
            repeatEnabled = src.repeatEnabled,
            repeatInitialDelayMs = src.repeatInitialDelayMs,
            repeatIntervalMs = src.repeatIntervalMs,
            hapticEnabled = src.hapticEnabled,
            hapticIntensity = src.hapticIntensity,
            soundOnPress = src.soundOnPress,
            stickyModifiers = src.stickyModifiers,
            keepModsAfterTab = src.keepModsAfterTab,
            showKeyHints = src.showKeyHints,
            highContrastMode = src.highContrastMode,
            compactModifiers = src.compactModifiers,
            numpadStartsLocked = src.numpadStartsLocked,
            numpadShowHints = src.numpadShowHints,
            showStatusBar = src.showStatusBar,
            showComboPreview = src.showComboPreview,
        )
        return settings.copy(portraitKeyboard = target)
    }

    // ═════════════════════════════════════════════════════════════════════
    // TRACKPAD SYNC
    // ═════════════════════════════════════════════════════════════════════

    fun applyTrackpadSyncFromPortrait(settings: AppSettings): AppSettings {
        if (!settings.syncFlags.syncTrackpadPortraitLandscape) return settings

        val src = settings.portraitTrackpad
        val target = settings.landscapeTrackpad.copy(
            pointerSpeed = src.pointerSpeed,
            scrollSpeed = src.scrollSpeed,
            invertScroll = src.invertScroll,
            tapToClick = src.tapToClick,
            twoFingerRightClick = src.twoFingerRightClick,
            accelerationEnabled = src.accelerationEnabled,
            dragLockMode = src.dragLockMode,
            clickPressure = src.clickPressure,
        )
        return settings.copy(landscapeTrackpad = target)
    }

    fun applyTrackpadSyncFromLandscape(settings: AppSettings): AppSettings {
        if (!settings.syncFlags.syncTrackpadPortraitLandscape) return settings

        val src = settings.landscapeTrackpad
        val target = settings.portraitTrackpad.copy(
            pointerSpeed = src.pointerSpeed,
            scrollSpeed = src.scrollSpeed,
            invertScroll = src.invertScroll,
            tapToClick = src.tapToClick,
            twoFingerRightClick = src.twoFingerRightClick,
            accelerationEnabled = src.accelerationEnabled,
            dragLockMode = src.dragLockMode,
            clickPressure = src.clickPressure,
        )
        return settings.copy(portraitTrackpad = target)
    }

    // ═════════════════════════════════════════════════════════════════════
    // FULL SYNC — call after loading settings, or when a sync flag is enabled
    // ═════════════════════════════════════════════════════════════════════

    /**
     * When user enables Keyboard sync, mirror Portrait → Landscape immediately.
     */
    fun enableKeyboardSync(settings: AppSettings): AppSettings {
        val withFlag = settings.copy(
            syncFlags = settings.syncFlags.copy(syncKeyboardPortraitLandscape = true)
        )
        return applyKeyboardSyncFromPortrait(withFlag)
    }

    fun disableKeyboardSync(settings: AppSettings): AppSettings {
        return settings.copy(
            syncFlags = settings.syncFlags.copy(syncKeyboardPortraitLandscape = false)
        )
    }

    fun enableTrackpadSync(settings: AppSettings): AppSettings {
        val withFlag = settings.copy(
            syncFlags = settings.syncFlags.copy(syncTrackpadPortraitLandscape = true)
        )
        return applyTrackpadSyncFromPortrait(withFlag)
    }

    fun disableTrackpadSync(settings: AppSettings): AppSettings {
        return settings.copy(
            syncFlags = settings.syncFlags.copy(syncTrackpadPortraitLandscape = false)
        )
    }
}