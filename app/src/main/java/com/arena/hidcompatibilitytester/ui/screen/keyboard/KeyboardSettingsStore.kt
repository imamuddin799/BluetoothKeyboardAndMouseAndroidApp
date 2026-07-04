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

            // Keys tab sections
            putBoolean("keysTabShowNavigation", s.keysTabShowNavigation)
            putBoolean("keysTabShowArrowKeys", s.keysTabShowArrowKeys)
            putBoolean("keysTabShowSystemKeys", s.keysTabShowSystemKeys)
            putBoolean("keysTabShowQuickMods", s.keysTabShowQuickMods)
            putString("keysTabSectionStyle", s.keysTabSectionStyle.name)

            putBoolean("numpadStartsLocked", s.numpadStartsLocked)
            putBoolean("numpadShowHints", s.numpadShowHints)
            putString("mediaKeySize", s.mediaKeySize.name)

            // Media tab sections
            putBoolean("mediaTabShowNavigation", s.mediaTabShowNavigation)
            putBoolean("mediaTabShowArrowKeys", s.mediaTabShowArrowKeys)
            putBoolean("mediaTabShowSystemKeys", s.mediaTabShowSystemKeys)
            putBoolean("mediaTabShowQuickMods", s.mediaTabShowQuickMods)
            putString("mediaTabSectionStyle", s.mediaTabSectionStyle.name)

            putBoolean("showMediaRowInKeyboard", s.showMediaRowInKeyboard)
            putBoolean("showMediaRowInTrackpad", s.showMediaRowInTrackpad)
            putBoolean("mediaRowShowTransport", s.mediaRowShowTransport)
            putBoolean("mediaRowShowVolume", s.mediaRowShowVolume)
            putBoolean("mediaRowShowBrightness", s.mediaRowShowBrightness)
            putBoolean("mediaRowRepeatVolume", s.mediaRowRepeatVolume)
            putBoolean("mediaRowRepeatBrightness", s.mediaRowRepeatBrightness)
            putString("mediaRowGroupOrder", s.mediaRowGroupOrder.joinToString(",") { it.name })

            putBoolean("showNavRowInKeyboard", s.showNavRowInKeyboard)
            putBoolean("showNavRowInTrackpad", s.showNavRowInTrackpad)

            putBoolean("navTabShowNavigation", s.navTabShowNavigation)
            putBoolean("navTabShowArrowKeys", s.navTabShowArrowKeys)
            putBoolean("navTabShowInsertToggle", s.navTabShowInsertToggle)
            putBoolean("navTabShowTypeText", s.navTabShowTypeText)
            putBoolean("navTabShowSystemKeys", s.navTabShowSystemKeys)
            putBoolean("navTabShowQuickMods", s.navTabShowQuickMods)

            putBoolean("mergeSystemAndModsGlobal", s.mergeSystemAndModsGlobal)
            putBoolean("keysTabMergeSystemAndMods", s.keysTabMergeSystemAndMods)
            putBoolean("mediaTabMergeSystemAndMods", s.mediaTabMergeSystemAndMods)
            putBoolean("navTabMergeSystemAndMods", s.navTabMergeSystemAndMods)

            putString("keysTabSectionOrder", s.keysTabSectionOrder.joinToString(",") { it.name })
            putBoolean("keysTabNavArrowsSwapped", s.keysTabNavArrowsSwapped)
            putString("navTabSectionOrder", s.navTabSectionOrder.joinToString(",") { it.name })
            putBoolean("navTabNavArrowsSwapped", s.navTabNavArrowsSwapped)
            putString("mediaTabSectionOrder", s.mediaTabSectionOrder.joinToString(",") { it.name })
            putBoolean("inPlaceReorderGlobal", s.inPlaceReorderGlobal)
            putBoolean("keysTabInPlaceReorder", s.keysTabInPlaceReorder)
            putBoolean("mediaTabInPlaceReorder", s.mediaTabInPlaceReorder)
            putBoolean("navTabInPlaceReorder", s.navTabInPlaceReorder)

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

            keysTabShowNavigation = p.getBoolean("keysTabShowNavigation", true),
            keysTabShowArrowKeys = p.getBoolean("keysTabShowArrowKeys", true),
            keysTabShowSystemKeys = p.getBoolean("keysTabShowSystemKeys", true),
            keysTabShowQuickMods = p.getBoolean("keysTabShowQuickMods", true),
            keysTabSectionStyle = runCatching {
                SectionStyle.valueOf(p.getString("keysTabSectionStyle", "COMPACT")!!)
            }.getOrDefault(SectionStyle.COMPACT),

            numpadStartsLocked = p.getBoolean("numpadStartsLocked", true),
            numpadShowHints = p.getBoolean("numpadShowHints", true),
            mediaKeySize = runCatching {
                MediaKeySize.valueOf(p.getString("mediaKeySize", "MEDIUM")!!)
            }.getOrDefault(MediaKeySize.MEDIUM),

            mediaTabShowNavigation = p.getBoolean("mediaTabShowNavigation", false),
            mediaTabShowArrowKeys = p.getBoolean("mediaTabShowArrowKeys", true),
            mediaTabShowSystemKeys = p.getBoolean("mediaTabShowSystemKeys", true),
            mediaTabShowQuickMods = p.getBoolean("mediaTabShowQuickMods", true),
            mediaTabSectionStyle = runCatching {
                SectionStyle.valueOf(p.getString("mediaTabSectionStyle", "MEDIA")!!)
            }.getOrDefault(SectionStyle.MEDIA),

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

            showNavRowInKeyboard = p.getBoolean("showNavRowInKeyboard", false),
            showNavRowInTrackpad = p.getBoolean("showNavRowInTrackpad", false),

            navTabShowNavigation = p.getBoolean("navTabShowNavigation", true),
            navTabShowArrowKeys = p.getBoolean("navTabShowArrowKeys", true),
            navTabShowInsertToggle = p.getBoolean("navTabShowInsertToggle", true),
            navTabShowTypeText = p.getBoolean("navTabShowTypeText", true),
            navTabShowSystemKeys = p.getBoolean("navTabShowSystemKeys", true),
            navTabShowQuickMods = p.getBoolean("navTabShowQuickMods", true),

            mergeSystemAndModsGlobal = p.getBoolean("mergeSystemAndModsGlobal", false),
            keysTabMergeSystemAndMods = p.getBoolean("keysTabMergeSystemAndMods", false),
            mediaTabMergeSystemAndMods = p.getBoolean("mediaTabMergeSystemAndMods", false),
            navTabMergeSystemAndMods = p.getBoolean("navTabMergeSystemAndMods", false),

            keysTabSectionOrder = runCatching {
                p.getString("keysTabSectionOrder", null)
                    ?.split(",")
                    ?.map { KeysTabSection.valueOf(it.trim()) }
                    ?: listOf(KeysTabSection.NAV_ARROWS, KeysTabSection.SYSTEM_KEYS, KeysTabSection.QUICK_MODS)
            }.getOrDefault(listOf(KeysTabSection.NAV_ARROWS, KeysTabSection.SYSTEM_KEYS, KeysTabSection.QUICK_MODS)),
            keysTabNavArrowsSwapped = p.getBoolean("keysTabNavArrowsSwapped", false),

            navTabSectionOrder = runCatching {
                p.getString("navTabSectionOrder", null)
                    ?.split(",")
                    ?.map { NavTabSection.valueOf(it.trim()) }
                    ?: listOf(NavTabSection.NAV_ARROWS, NavTabSection.INSERT_TOGGLE, NavTabSection.SYSTEM_KEYS, NavTabSection.QUICK_MODS, NavTabSection.TYPE_TEXT)
            }.getOrDefault(listOf(NavTabSection.NAV_ARROWS, NavTabSection.INSERT_TOGGLE, NavTabSection.SYSTEM_KEYS, NavTabSection.QUICK_MODS, NavTabSection.TYPE_TEXT)),
            navTabNavArrowsSwapped = p.getBoolean("navTabNavArrowsSwapped", false),

            mediaTabSectionOrder = runCatching {
                p.getString("mediaTabSectionOrder", null)
                    ?.split(",")
                    ?.map { MediaTabSection.valueOf(it.trim()) }
                    ?: listOf(MediaTabSection.TRANSPORT, MediaTabSection.VOLUME_BRIGHTNESS, MediaTabSection.NAVIGATION, MediaTabSection.ARROW_KEYS, MediaTabSection.SYSTEM_KEYS, MediaTabSection.QUICK_MODS)
            }.getOrDefault(listOf(MediaTabSection.TRANSPORT, MediaTabSection.VOLUME_BRIGHTNESS, MediaTabSection.NAVIGATION, MediaTabSection.ARROW_KEYS, MediaTabSection.SYSTEM_KEYS, MediaTabSection.QUICK_MODS)),

            inPlaceReorderGlobal = p.getBoolean("inPlaceReorderGlobal", false),
            keysTabInPlaceReorder = p.getBoolean("keysTabInPlaceReorder", false),
            mediaTabInPlaceReorder = p.getBoolean("mediaTabInPlaceReorder", false),
            navTabInPlaceReorder = p.getBoolean("navTabInPlaceReorder", false),

            defaultTab = p.getInt("defaultTab", 0),
        )
    }
}