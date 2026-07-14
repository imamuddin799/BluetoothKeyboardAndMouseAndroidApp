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

            putBoolean("keysTabShowNavigation", s.keysTabShowNavigation)
            putBoolean("keysTabShowArrowKeys", s.keysTabShowArrowKeys)
            putBoolean("keysTabShowSystemKeys", s.keysTabShowSystemKeys)
            putBoolean("keysTabShowQuickMods", s.keysTabShowQuickMods)
            putString("keysTabSectionStyle", s.keysTabSectionStyle.name)

            putBoolean("numpadStartsLocked", s.numpadStartsLocked)
            putBoolean("numpadShowHints", s.numpadShowHints)
            putString("mediaKeySize", s.mediaKeySize.name)

            putBoolean("mediaTabShowNavigation", s.mediaTabShowNavigation)
            putBoolean("mediaTabShowArrowKeys", s.mediaTabShowArrowKeys)
            putBoolean("mediaTabShowSystemKeys", s.mediaTabShowSystemKeys)
            putBoolean("mediaTabShowQuickMods", s.mediaTabShowQuickMods)
            putString("mediaTabSectionStyle", s.mediaTabSectionStyle.name)

            putBoolean("navTabShowNavigation", s.navTabShowNavigation)
            putBoolean("navTabShowArrowKeys", s.navTabShowArrowKeys)
            putBoolean("navTabShowInsertToggle", s.navTabShowInsertToggle)
            putBoolean("navTabShowTypeText", s.navTabShowTypeText)
            putBoolean("navTabShowSystemKeys", s.navTabShowSystemKeys)
            putBoolean("navTabShowQuickMods", s.navTabShowQuickMods)

            // Optional row visibility
            putBoolean("globalOptionalRowVisibility", s.globalOptionalRowVisibility)
            putBoolean("globalShowMediaRow", s.globalShowMediaRow)
            putBoolean("globalShowNavRow", s.globalShowNavRow)
            putBoolean("keysTabShowMediaRow", s.keysTabShowMediaRow)
            putBoolean("keysTabShowNavRow", s.keysTabShowNavRow)
            putBoolean("trackpadShowMediaRow", s.trackpadShowMediaRow)
            putBoolean("trackpadShowNavRow", s.trackpadShowNavRow)

            // Media row content config
            putBoolean("mediaRowShowTransport", s.mediaRowShowTransport)
            putBoolean("mediaRowShowVolume", s.mediaRowShowVolume)
            putBoolean("mediaRowShowBrightness", s.mediaRowShowBrightness)
            putBoolean("mediaRowRepeatVolume", s.mediaRowRepeatVolume)
            putBoolean("mediaRowRepeatBrightness", s.mediaRowRepeatBrightness)
            putString("mediaRowGroupOrder", s.mediaRowGroupOrder.joinToString(",") { it.name })

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

            putBoolean("globalOptionalRowOrder", s.globalOptionalRowOrder)
            putString("keyboardOptionalRowOrder", s.keyboardOptionalRowOrder.joinToString(",") { it.name })
            putString("keysTabOptionalRowOrder", s.keysTabOptionalRowOrder.joinToString(",") { it.name })
            putString("trackpadOptionalRowOrder", s.trackpadOptionalRowOrder.joinToString(",") { it.name })

            putInt("defaultTab", s.defaultTab)
            apply()
        }
    }

    fun load(context: Context): KeyboardSettings {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // Migration: read old field names and map to new ones
        val oldMediaInKb = p.getBoolean("showMediaRowInKeyboard", false)
        val oldMediaInTp = p.getBoolean("showMediaRowInTrackpad", true)
        val oldNavInKb = p.getBoolean("showNavRowInKeyboard", false)
        val oldNavInTp = p.getBoolean("showNavRowInTrackpad", false)

        return KeyboardSettings(
            repeatEnabled = p.getBoolean("repeatEnabled", true),
            repeatInitialDelayMs = p.getLong("repeatInitialDelayMs", 350L),
            repeatIntervalMs = p.getLong("repeatIntervalMs", 30L),
            hapticEnabled = p.getBoolean("hapticEnabled", true),
            hapticIntensity = runCatching {
                HapticIntensity.valueOf(p.getString("hapticIntensity", "MEDIUM")!!)
            }.getOrDefault(HapticIntensity.MEDIUM),
            soundOnPress = p.getBoolean("soundOnPress", true),
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
            keysTabShowQuickMods = p.getBoolean("keysTabShowQuickMods", false),
            keysTabSectionStyle = runCatching {
                SectionStyle.valueOf(p.getString("keysTabSectionStyle", "MEDIA")!!)
            }.getOrDefault(SectionStyle.MEDIA),

            numpadStartsLocked = p.getBoolean("numpadStartsLocked", true),
            numpadShowHints = p.getBoolean("numpadShowHints", true),
            mediaKeySize = runCatching {
                MediaKeySize.valueOf(p.getString("mediaKeySize", "MEDIUM")!!)
            }.getOrDefault(MediaKeySize.MEDIUM),

            mediaTabShowNavigation = p.getBoolean("mediaTabShowNavigation", false),
            mediaTabShowArrowKeys = p.getBoolean("mediaTabShowArrowKeys", true),
            mediaTabShowSystemKeys = p.getBoolean("mediaTabShowSystemKeys", true),
            mediaTabShowQuickMods = p.getBoolean("mediaTabShowQuickMods", false),
            mediaTabSectionStyle = runCatching {
                SectionStyle.valueOf(p.getString("mediaTabSectionStyle", "MEDIA")!!)
            }.getOrDefault(SectionStyle.MEDIA),

            navTabShowNavigation = p.getBoolean("navTabShowNavigation", false),
            navTabShowArrowKeys = p.getBoolean("navTabShowArrowKeys", false),
            navTabShowInsertToggle = p.getBoolean("navTabShowInsertToggle", false),
            navTabShowTypeText = p.getBoolean("navTabShowTypeText", true),
            navTabShowSystemKeys = p.getBoolean("navTabShowSystemKeys", true),
            navTabShowQuickMods = p.getBoolean("navTabShowQuickMods", false),

            // Optional row visibility — migrate from old keys
            globalOptionalRowVisibility = p.getBoolean("globalOptionalRowVisibility", false),
            globalShowMediaRow = p.getBoolean("globalShowMediaRow", false),
            globalShowNavRow = p.getBoolean("globalShowNavRow", false),
            keysTabShowMediaRow = p.getBoolean("keysTabShowMediaRow", oldMediaInKb),
            keysTabShowNavRow = p.getBoolean("keysTabShowNavRow", oldNavInKb),
            trackpadShowMediaRow = p.getBoolean("trackpadShowMediaRow", oldMediaInTp),
            trackpadShowNavRow = p.getBoolean("trackpadShowNavRow", oldNavInTp),

            mediaRowShowTransport = p.getBoolean("mediaRowShowTransport", true),
            mediaRowShowVolume = p.getBoolean("mediaRowShowVolume", true),
            mediaRowShowBrightness = p.getBoolean("mediaRowShowBrightness", true),
            mediaRowRepeatVolume = p.getBoolean("mediaRowRepeatVolume", true),
            mediaRowRepeatBrightness = p.getBoolean("mediaRowRepeatBrightness", true),
            mediaRowGroupOrder = runCatching {
                p.getString("mediaRowGroupOrder", null)
                    ?.split(",")
                    ?.map { MediaRowGroup.valueOf(it.trim()) }
                    ?: listOf(MediaRowGroup.VOLUME, MediaRowGroup.BRIGHTNESS, MediaRowGroup.TRANSPORT)
            }.getOrDefault(listOf(MediaRowGroup.VOLUME, MediaRowGroup.BRIGHTNESS, MediaRowGroup.TRANSPORT)),

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

            globalOptionalRowOrder = p.getBoolean("globalOptionalRowOrder", false),
            keyboardOptionalRowOrder = runCatching {
                p.getString("keyboardOptionalRowOrder", null)
                    ?.split(",")
                    ?.map { KeyboardOptionalRow.valueOf(it.trim()) }
                    ?: listOf(KeyboardOptionalRow.MEDIA_ROW, KeyboardOptionalRow.NAV_ROW)
            }.getOrDefault(listOf(KeyboardOptionalRow.MEDIA_ROW, KeyboardOptionalRow.NAV_ROW)),
            keysTabOptionalRowOrder = runCatching {
                p.getString("keysTabOptionalRowOrder", null)
                    ?.split(",")
                    ?.map { KeyboardOptionalRow.valueOf(it.trim()) }
                    ?: listOf(KeyboardOptionalRow.MEDIA_ROW, KeyboardOptionalRow.NAV_ROW)
            }.getOrDefault(listOf(KeyboardOptionalRow.MEDIA_ROW, KeyboardOptionalRow.NAV_ROW)),
            trackpadOptionalRowOrder = runCatching {
                p.getString("trackpadOptionalRowOrder", null)
                    ?.split(",")
                    ?.map { KeyboardOptionalRow.valueOf(it.trim()) }
                    ?: listOf(KeyboardOptionalRow.MEDIA_ROW, KeyboardOptionalRow.NAV_ROW)
            }.getOrDefault(listOf(KeyboardOptionalRow.MEDIA_ROW, KeyboardOptionalRow.NAV_ROW)),

            defaultTab = p.getInt("defaultTab", 0),
        )
    }
}