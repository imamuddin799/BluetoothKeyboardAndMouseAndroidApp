package com.arena.hidcompatibilitytester.ui.screen.keyboard

data class KeyboardSettings(
    // Key repeat
    val repeatEnabled: Boolean = true,
    val repeatInitialDelayMs: Long = 350L,
    val repeatIntervalMs: Long = 30L,

    // Haptics
    val hapticEnabled: Boolean = true,
    val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,

    // Sound
    val soundOnPress: Boolean = true,

    // Layout
    val keyHeight: KeyHeight = KeyHeight.MEDIUM,
    val showStatusBar: Boolean = true,
    val showComboPreview: Boolean = true,
    val compactModifiers: Boolean = false,

    // Behavior
    val stickyModifiers: Boolean = false,
    val keepModsAfterTab: Boolean = true,

    // Appearance
    val keyFontSize: KeyFontSize = KeyFontSize.MEDIUM,
    val showKeyHints: Boolean = true,
    val highContrastMode: Boolean = false,

    // Keys tab — section visibility
    val keysTabShowNavigation: Boolean = true,
    val keysTabShowArrowKeys: Boolean = true,
    val keysTabShowSystemKeys: Boolean = true,
    val keysTabShowQuickMods: Boolean = false,
    val keysTabSectionStyle: SectionStyle = SectionStyle.MEDIA,

    // Numpad
    val numpadStartsLocked: Boolean = true,
    val numpadShowHints: Boolean = true,

    // Media
    val mediaKeySize: MediaKeySize = MediaKeySize.MEDIUM,

    // Media tab — section visibility
    val mediaTabShowNavigation: Boolean = false,
    val mediaTabShowArrowKeys: Boolean = true,
    val mediaTabShowSystemKeys: Boolean = true,
    val mediaTabShowQuickMods: Boolean = false,
    val mediaTabSectionStyle: SectionStyle = SectionStyle.MEDIA,

    // Nav+Numpad tab — section visibility
    val navTabShowNavigation: Boolean = false,
    val navTabShowArrowKeys: Boolean = false,
    val navTabShowInsertToggle: Boolean = false,
    val navTabShowSystemKeys: Boolean = true,
    val navTabShowQuickMods: Boolean = false,
    val navTabShowTypeText: Boolean = true,

    // Optional Row Visibility
    val globalOptionalRowVisibility: Boolean = false,

    // Global toggles
    val globalShowMediaRow: Boolean = false,
    val globalShowNavRow: Boolean = false,

    // Per-keyboard toggles
    val keysTabShowMediaRow: Boolean = false,
    val keysTabShowNavRow: Boolean = false,
    val trackpadShowMediaRow: Boolean = true,
    val trackpadShowNavRow: Boolean = false,

    // Media row sub-settings
    val mediaRowShowTransport: Boolean = true,
    val mediaRowShowVolume: Boolean = true,
    val mediaRowShowBrightness: Boolean = true,
    val mediaRowRepeatVolume: Boolean = true,
    val mediaRowRepeatBrightness: Boolean = true,
    val mediaRowGroupOrder: List<MediaRowGroup> = listOf(
        MediaRowGroup.VOLUME,
        MediaRowGroup.BRIGHTNESS,
        MediaRowGroup.TRANSPORT,
    ),

    // Merge System Keys + Quick Modifiers
    val mergeSystemAndModsGlobal: Boolean = false,
    val keysTabMergeSystemAndMods: Boolean = false,
    val mediaTabMergeSystemAndMods: Boolean = false,
    val navTabMergeSystemAndMods: Boolean = false,

    // Section order per tab
    val keysTabSectionOrder: List<KeysTabSection> = listOf(
        KeysTabSection.NAV_ARROWS,
        KeysTabSection.SYSTEM_KEYS,
        KeysTabSection.QUICK_MODS,
    ),
    val keysTabNavArrowsSwapped: Boolean = false,

    val navTabSectionOrder: List<NavTabSection> = listOf(
        NavTabSection.NAV_ARROWS,
        NavTabSection.INSERT_TOGGLE,
        NavTabSection.SYSTEM_KEYS,
        NavTabSection.QUICK_MODS,
        NavTabSection.TYPE_TEXT,
    ),
    val navTabNavArrowsSwapped: Boolean = false,

    val mediaTabSectionOrder: List<MediaTabSection> = listOf(
        MediaTabSection.TRANSPORT,
        MediaTabSection.VOLUME_BRIGHTNESS,
        MediaTabSection.NAVIGATION,
        MediaTabSection.ARROW_KEYS,
        MediaTabSection.SYSTEM_KEYS,
        MediaTabSection.QUICK_MODS,
    ),

    // In-place reorder
    val inPlaceReorderGlobal: Boolean = false,
    val keysTabInPlaceReorder: Boolean = false,
    val mediaTabInPlaceReorder: Boolean = false,
    val navTabInPlaceReorder: Boolean = false,

    // Optional row order
    val globalOptionalRowOrder: Boolean = false,
    val keyboardOptionalRowOrder: List<KeyboardOptionalRow> = listOf(
        KeyboardOptionalRow.MEDIA_ROW,
        KeyboardOptionalRow.NAV_ROW,
    ),
    val keysTabOptionalRowOrder: List<KeyboardOptionalRow> = listOf(
        KeyboardOptionalRow.MEDIA_ROW,
        KeyboardOptionalRow.NAV_ROW,
    ),
    val trackpadOptionalRowOrder: List<KeyboardOptionalRow> = listOf(
        KeyboardOptionalRow.MEDIA_ROW,
        KeyboardOptionalRow.NAV_ROW,
    ),

    // Default tab
    val defaultTab: Int = 0,
) {
    fun shouldMergeSystemMods(tabMerge: Boolean): Boolean {
        return mergeSystemAndModsGlobal || tabMerge
    }

    fun shouldAllowInPlaceReorder(tabReorder: Boolean): Boolean {
        return inPlaceReorderGlobal || tabReorder
    }

    fun getOptionalRowOrder(tabOrder: List<KeyboardOptionalRow>): List<KeyboardOptionalRow> {
        return if (globalOptionalRowOrder) keyboardOptionalRowOrder else tabOrder
    }

    // Resolved visibility helpers
    fun showMediaRowInKeysTab(): Boolean {
        return if (globalOptionalRowVisibility) globalShowMediaRow else keysTabShowMediaRow
    }

    fun showNavRowInKeysTab(): Boolean {
        return if (globalOptionalRowVisibility) globalShowNavRow else keysTabShowNavRow
    }

    fun showMediaRowInTrackpad(): Boolean {
        return if (globalOptionalRowVisibility) globalShowMediaRow else trackpadShowMediaRow
    }

    fun showNavRowInTrackpad(): Boolean {
        return if (globalOptionalRowVisibility) globalShowNavRow else trackpadShowNavRow
    }
}

enum class SectionStyle(val label: String) {
    COMPACT("Standard"),
    MEDIA("Comfort"),
}

enum class MediaRowGroup(val label: String, val icon: String) {
    TRANSPORT("Transport", "⏯"),
    VOLUME("Volume", "🔊"),
    BRIGHTNESS("Brightness", "🔆"),
}

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

enum class KeysTabSection(val label: String, val icon: String) {
    NAV_ARROWS("Navigation + Arrows", "↕"),
    SYSTEM_KEYS("System Keys", "⌨"),
    QUICK_MODS("Quick Modifiers", "⇧"),
    MERGED_SYSTEM_MODS("System & Modifiers", "⌨⇧"),
}

enum class NavTabSection(val label: String, val icon: String) {
    NAV_ARROWS("Navigation + Arrows", "↕"),
    INSERT_TOGGLE("Insert/Overwrite", "⎀"),
    SYSTEM_KEYS("System Keys", "⌨"),
    QUICK_MODS("Quick Modifiers", "⇧"),
    MERGED_SYSTEM_MODS("System & Modifiers", "⌨⇧"),
    TYPE_TEXT("Type & Send Text", "✎"),
}

enum class MediaTabSection(val label: String, val icon: String) {
    TRANSPORT("Transport", "⏯"),
    VOLUME_BRIGHTNESS("Volume & Brightness", "🔊"),
    NAVIGATION("Navigation", "↕"),
    ARROW_KEYS("Arrow Keys", "←→"),
    SYSTEM_KEYS("System Keys", "⌨"),
    QUICK_MODS("Quick Modifiers", "⇧"),
    MERGED_SYSTEM_MODS("System & Modifiers", "⌨⇧"),
}

enum class KeyboardOptionalRow(val label: String, val icon: String) {
    MEDIA_ROW("Media Row", "🎵"),
    NAV_ROW("Navigation Row", "↕"),
}