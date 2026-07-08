package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard

data class LandscapeKeyboardSettings(
    val repeatEnabled: Boolean = true,
    val repeatInitialDelayMs: Long = 350L,
    val repeatIntervalMs: Long = 30L,
    val hapticEnabled: Boolean = true,
    val hapticIntensity: LandscapeHapticIntensity = LandscapeHapticIntensity.MEDIUM,
    val soundOnPress: Boolean = true,
    val keyHeight: LandscapeKeyHeight = LandscapeKeyHeight.MEDIUM,
    val showStatusBar: Boolean = true,
    val showComboPreview: Boolean = true,
    val compactModifiers: Boolean = false,
    val stickyModifiers: Boolean = false,
    val keepModsAfterTab: Boolean = true,
    val keyFontSize: LandscapeKeyFontSize = LandscapeKeyFontSize.MEDIUM,
    val showKeyHints: Boolean = true,
    val highContrastMode: Boolean = false,
    val keysTabShowNavigation: Boolean = true,
    val keysTabShowArrowKeys: Boolean = true,
    val keysTabShowSystemKeys: Boolean = true,
    val keysTabShowQuickMods: Boolean = false,
    val keysTabSectionStyle: LandscapeSectionStyle = LandscapeSectionStyle.MEDIA,
    val numpadStartsLocked: Boolean = true,
    val numpadShowHints: Boolean = true,
    val mediaKeySize: LandscapeMediaKeySize = LandscapeMediaKeySize.MEDIUM,
    val mediaTabShowNavigation: Boolean = false,
    val mediaTabShowArrowKeys: Boolean = true,
    val mediaTabShowSystemKeys: Boolean = true,
    val mediaTabShowQuickMods: Boolean = false,
    val mediaTabSectionStyle: LandscapeSectionStyle = LandscapeSectionStyle.MEDIA,
    val navTabShowNavigation: Boolean = false,
    val navTabShowArrowKeys: Boolean = false,
    val navTabShowInsertToggle: Boolean = false,
    val navTabShowSystemKeys: Boolean = true,
    val navTabShowQuickMods: Boolean = false,
    val navTabShowTypeText: Boolean = true,
    val globalOptionalRowVisibility: Boolean = false,
    val globalShowMediaRow: Boolean = false,
    val globalShowNavRow: Boolean = false,
    val keysTabShowMediaRow: Boolean = false,
    val keysTabShowNavRow: Boolean = false,
    val trackpadShowMediaRow: Boolean = true,
    val trackpadShowNavRow: Boolean = false,
    val mediaRowShowTransport: Boolean = true,
    val mediaRowShowVolume: Boolean = true,
    val mediaRowShowBrightness: Boolean = true,
    val mediaRowRepeatVolume: Boolean = true,
    val mediaRowRepeatBrightness: Boolean = true,
    val mediaRowGroupOrder: List<LandscapeMediaRowGroup> = listOf(
        LandscapeMediaRowGroup.VOLUME,
        LandscapeMediaRowGroup.BRIGHTNESS,
        LandscapeMediaRowGroup.TRANSPORT,
    ),
    val mergeSystemAndModsGlobal: Boolean = false,
    val keysTabMergeSystemAndMods: Boolean = false,
    val mediaTabMergeSystemAndMods: Boolean = false,
    val navTabMergeSystemAndMods: Boolean = false,
    val keysTabSectionOrder: List<LandscapeKeysTabSection> = listOf(
        LandscapeKeysTabSection.NAV_ARROWS,
        LandscapeKeysTabSection.SYSTEM_KEYS,
        LandscapeKeysTabSection.QUICK_MODS,
    ),
    val keysTabNavArrowsSwapped: Boolean = false,
    val navTabSectionOrder: List<LandscapeNavTabSection> = listOf(
        LandscapeNavTabSection.NAV_ARROWS,
        LandscapeNavTabSection.INSERT_TOGGLE,
        LandscapeNavTabSection.SYSTEM_KEYS,
        LandscapeNavTabSection.QUICK_MODS,
        LandscapeNavTabSection.TYPE_TEXT,
    ),
    val navTabNavArrowsSwapped: Boolean = false,
    val mediaTabSectionOrder: List<LandscapeMediaTabSection> = listOf(
        LandscapeMediaTabSection.TRANSPORT,
        LandscapeMediaTabSection.VOLUME_BRIGHTNESS,
        LandscapeMediaTabSection.NAVIGATION,
        LandscapeMediaTabSection.ARROW_KEYS,
        LandscapeMediaTabSection.SYSTEM_KEYS,
        LandscapeMediaTabSection.QUICK_MODS,
    ),
    val inPlaceReorderGlobal: Boolean = false,
    val keysTabInPlaceReorder: Boolean = false,
    val mediaTabInPlaceReorder: Boolean = false,
    val navTabInPlaceReorder: Boolean = false,
    val globalOptionalRowOrder: Boolean = false,
    val keyboardOptionalRowOrder: List<LandscapeKeyboardOptionalRow> = listOf(
        LandscapeKeyboardOptionalRow.MEDIA_ROW,
        LandscapeKeyboardOptionalRow.NAV_ROW,
    ),
    val keysTabOptionalRowOrder: List<LandscapeKeyboardOptionalRow> = listOf(
        LandscapeKeyboardOptionalRow.MEDIA_ROW,
        LandscapeKeyboardOptionalRow.NAV_ROW,
    ),
    val trackpadOptionalRowOrder: List<LandscapeKeyboardOptionalRow> = listOf(
        LandscapeKeyboardOptionalRow.MEDIA_ROW,
        LandscapeKeyboardOptionalRow.NAV_ROW,
    ),
    val defaultTab: Int = 0,
) {
    fun shouldMergeSystemMods(tabMerge: Boolean): Boolean =
        mergeSystemAndModsGlobal || tabMerge

    fun shouldAllowInPlaceReorder(tabReorder: Boolean): Boolean =
        inPlaceReorderGlobal || tabReorder

    fun getOptionalRowOrder(tabOrder: List<LandscapeKeyboardOptionalRow>): List<LandscapeKeyboardOptionalRow> =
        if (globalOptionalRowOrder) keyboardOptionalRowOrder else tabOrder

    fun showMediaRowInKeysTab(): Boolean =
        if (globalOptionalRowVisibility) globalShowMediaRow else keysTabShowMediaRow

    fun showNavRowInKeysTab(): Boolean =
        if (globalOptionalRowVisibility) globalShowNavRow else keysTabShowNavRow

    fun showMediaRowInTrackpad(): Boolean =
        if (globalOptionalRowVisibility) globalShowMediaRow else trackpadShowMediaRow

    fun showNavRowInTrackpad(): Boolean =
        if (globalOptionalRowVisibility) globalShowNavRow else trackpadShowNavRow
}

enum class LandscapeSectionStyle(val label: String) {
    COMPACT("Standard"),
    MEDIA("Comfort"),
}

enum class LandscapeMediaRowGroup(val label: String, val icon: String) {
    TRANSPORT("Transport", "⏯"),
    VOLUME("Volume", "🔊"),
    BRIGHTNESS("Brightness", "🔆"),
}

enum class LandscapeHapticIntensity(val label: String) {
    LIGHT("Light"),
    MEDIUM("Medium"),
    STRONG("Strong"),
}

enum class LandscapeKeyHeight(val label: String, val mainDp: Int, val fnDp: Int, val navDp: Int) {
    SMALL("Small", 34, 32, 38),
    MEDIUM("Medium", 40, 38, 46),
    LARGE("Large", 48, 44, 52),
}

enum class LandscapeKeyFontSize(val label: String, val baseSp: Int) {
    SMALL("Small", 10),
    MEDIUM("Medium", 12),
    LARGE("Large", 14),
}

enum class LandscapeMediaKeySize(val label: String, val heightDp: Int) {
    SMALL("Small", 48),
    MEDIUM("Medium", 64),
    LARGE("Large", 80),
}

enum class LandscapeKeysTabSection(val label: String, val icon: String) {
    NAV_ARROWS("Navigation + Arrows", "↕"),
    SYSTEM_KEYS("System Keys", "⌨"),
    QUICK_MODS("Quick Modifiers", "⇧"),
    MERGED_SYSTEM_MODS("System & Modifiers", "⌨⇧"),
}

enum class LandscapeNavTabSection(val label: String, val icon: String) {
    NAV_ARROWS("Navigation + Arrows", "↕"),
    INSERT_TOGGLE("Insert/Overwrite", "⎀"),
    SYSTEM_KEYS("System Keys", "⌨"),
    QUICK_MODS("Quick Modifiers", "⇧"),
    MERGED_SYSTEM_MODS("System & Modifiers", "⌨⇧"),
    TYPE_TEXT("Type & Send Text", "✎"),
}

enum class LandscapeMediaTabSection(val label: String, val icon: String) {
    TRANSPORT("Transport", "⏯"),
    VOLUME_BRIGHTNESS("Volume & Brightness", "🔊"),
    NAVIGATION("Navigation", "↕"),
    ARROW_KEYS("Arrow Keys", "←→"),
    SYSTEM_KEYS("System Keys", "⌨"),
    QUICK_MODS("Quick Modifiers", "⇧"),
    MERGED_SYSTEM_MODS("System & Modifiers", "⌨⇧"),
}

enum class LandscapeKeyboardOptionalRow(val label: String, val icon: String) {
    MEDIA_ROW("Media Row", "🎵"),
    NAV_ROW("Navigation Row", "↕"),
}