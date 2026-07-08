package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard

import android.content.Context

object LandscapeKeyboardSettingsStore {

    private const val PREFS = "landscape_keyboard_settings"

    fun save(context: Context, s: LandscapeKeyboardSettings) {
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
            putBoolean("globalOptionalRowVisibility", s.globalOptionalRowVisibility)
            putBoolean("globalShowMediaRow", s.globalShowMediaRow)
            putBoolean("globalShowNavRow", s.globalShowNavRow)
            putBoolean("keysTabShowMediaRow", s.keysTabShowMediaRow)
            putBoolean("keysTabShowNavRow", s.keysTabShowNavRow)
            putBoolean("trackpadShowMediaRow", s.trackpadShowMediaRow)
            putBoolean("trackpadShowNavRow", s.trackpadShowNavRow)
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

    fun load(context: Context): LandscapeKeyboardSettings {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val oldMediaInKb = p.getBoolean("showMediaRowInKeyboard", false)
        val oldMediaInTp = p.getBoolean("showMediaRowInTrackpad", true)
        val oldNavInKb = p.getBoolean("showNavRowInKeyboard", false)
        val oldNavInTp = p.getBoolean("showNavRowInTrackpad", false)

        return LandscapeKeyboardSettings(
            repeatEnabled = p.getBoolean("repeatEnabled", true),
            repeatInitialDelayMs = p.getLong("repeatInitialDelayMs", 350L),
            repeatIntervalMs = p.getLong("repeatIntervalMs", 30L),
            hapticEnabled = p.getBoolean("hapticEnabled", true),
            hapticIntensity = safeEnum(p.getString("hapticIntensity", "MEDIUM"), LandscapeHapticIntensity.MEDIUM),
            soundOnPress = p.getBoolean("soundOnPress", true),
            keyHeight = safeEnum(p.getString("keyHeight", "MEDIUM"), LandscapeKeyHeight.MEDIUM),
            showStatusBar = p.getBoolean("showStatusBar", true),
            showComboPreview = p.getBoolean("showComboPreview", true),
            compactModifiers = p.getBoolean("compactModifiers", false),
            stickyModifiers = p.getBoolean("stickyModifiers", false),
            keepModsAfterTab = p.getBoolean("keepModsAfterTab", true),
            keyFontSize = safeEnum(p.getString("keyFontSize", "MEDIUM"), LandscapeKeyFontSize.MEDIUM),
            showKeyHints = p.getBoolean("showKeyHints", true),
            highContrastMode = p.getBoolean("highContrastMode", false),
            keysTabShowNavigation = p.getBoolean("keysTabShowNavigation", true),
            keysTabShowArrowKeys = p.getBoolean("keysTabShowArrowKeys", true),
            keysTabShowSystemKeys = p.getBoolean("keysTabShowSystemKeys", true),
            keysTabShowQuickMods = p.getBoolean("keysTabShowQuickMods", false),
            keysTabSectionStyle = safeEnum(p.getString("keysTabSectionStyle", "MEDIA"), LandscapeSectionStyle.MEDIA),
            numpadStartsLocked = p.getBoolean("numpadStartsLocked", true),
            numpadShowHints = p.getBoolean("numpadShowHints", true),
            mediaKeySize = safeEnum(p.getString("mediaKeySize", "MEDIUM"), LandscapeMediaKeySize.MEDIUM),
            mediaTabShowNavigation = p.getBoolean("mediaTabShowNavigation", false),
            mediaTabShowArrowKeys = p.getBoolean("mediaTabShowArrowKeys", true),
            mediaTabShowSystemKeys = p.getBoolean("mediaTabShowSystemKeys", true),
            mediaTabShowQuickMods = p.getBoolean("mediaTabShowQuickMods", false),
            mediaTabSectionStyle = safeEnum(p.getString("mediaTabSectionStyle", "MEDIA"), LandscapeSectionStyle.MEDIA),
            navTabShowNavigation = p.getBoolean("navTabShowNavigation", false),
            navTabShowArrowKeys = p.getBoolean("navTabShowArrowKeys", false),
            navTabShowInsertToggle = p.getBoolean("navTabShowInsertToggle", false),
            navTabShowTypeText = p.getBoolean("navTabShowTypeText", true),
            navTabShowSystemKeys = p.getBoolean("navTabShowSystemKeys", true),
            navTabShowQuickMods = p.getBoolean("navTabShowQuickMods", false),
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
            mediaRowGroupOrder = safeEnumList(p.getString("mediaRowGroupOrder", null), listOf(LandscapeMediaRowGroup.VOLUME, LandscapeMediaRowGroup.BRIGHTNESS, LandscapeMediaRowGroup.TRANSPORT)),
            mergeSystemAndModsGlobal = p.getBoolean("mergeSystemAndModsGlobal", false),
            keysTabMergeSystemAndMods = p.getBoolean("keysTabMergeSystemAndMods", false),
            mediaTabMergeSystemAndMods = p.getBoolean("mediaTabMergeSystemAndMods", false),
            navTabMergeSystemAndMods = p.getBoolean("navTabMergeSystemAndMods", false),
            keysTabSectionOrder = safeEnumList(p.getString("keysTabSectionOrder", null), listOf(LandscapeKeysTabSection.NAV_ARROWS, LandscapeKeysTabSection.SYSTEM_KEYS, LandscapeKeysTabSection.QUICK_MODS)),
            keysTabNavArrowsSwapped = p.getBoolean("keysTabNavArrowsSwapped", false),
            navTabSectionOrder = safeEnumList(p.getString("navTabSectionOrder", null), listOf(LandscapeNavTabSection.NAV_ARROWS, LandscapeNavTabSection.INSERT_TOGGLE, LandscapeNavTabSection.SYSTEM_KEYS, LandscapeNavTabSection.QUICK_MODS, LandscapeNavTabSection.TYPE_TEXT)),
            navTabNavArrowsSwapped = p.getBoolean("navTabNavArrowsSwapped", false),
            mediaTabSectionOrder = safeEnumList(p.getString("mediaTabSectionOrder", null), listOf(LandscapeMediaTabSection.TRANSPORT, LandscapeMediaTabSection.VOLUME_BRIGHTNESS, LandscapeMediaTabSection.NAVIGATION, LandscapeMediaTabSection.ARROW_KEYS, LandscapeMediaTabSection.SYSTEM_KEYS, LandscapeMediaTabSection.QUICK_MODS)),
            inPlaceReorderGlobal = p.getBoolean("inPlaceReorderGlobal", false),
            keysTabInPlaceReorder = p.getBoolean("keysTabInPlaceReorder", false),
            mediaTabInPlaceReorder = p.getBoolean("mediaTabInPlaceReorder", false),
            navTabInPlaceReorder = p.getBoolean("navTabInPlaceReorder", false),
            globalOptionalRowOrder = p.getBoolean("globalOptionalRowOrder", false),
            keyboardOptionalRowOrder = safeEnumList(p.getString("keyboardOptionalRowOrder", null), listOf(LandscapeKeyboardOptionalRow.MEDIA_ROW, LandscapeKeyboardOptionalRow.NAV_ROW)),
            keysTabOptionalRowOrder = safeEnumList(p.getString("keysTabOptionalRowOrder", null), listOf(LandscapeKeyboardOptionalRow.MEDIA_ROW, LandscapeKeyboardOptionalRow.NAV_ROW)),
            trackpadOptionalRowOrder = safeEnumList(p.getString("trackpadOptionalRowOrder", null), listOf(LandscapeKeyboardOptionalRow.MEDIA_ROW, LandscapeKeyboardOptionalRow.NAV_ROW)),
            defaultTab = p.getInt("defaultTab", 0),
        )
    }

    private inline fun <reified T : Enum<T>> safeEnum(v: String?, d: T): T =
        try { if (v != null) enumValueOf<T>(v) else d } catch (_: Exception) { d }

    private inline fun <reified T : Enum<T>> safeEnumList(v: String?, d: List<T>): List<T> =
        try {
            v?.split(",")?.map { enumValueOf<T>(it.trim()) } ?: d
        } catch (_: Exception) { d }
}