package com.arena.hidcompatibilitytester.settings

import kotlinx.serialization.Serializable

// ═════════════════════════════════════════════════════════════════════════════
// COMMON INTERFACES
// ═════════════════════════════════════════════════════════════════════════════

interface KeyboardCommonSettings {
    val repeatEnabled: Boolean
    val repeatInitialDelayMs: Long
    val repeatIntervalMs: Long
    val hapticEnabled: Boolean
    val hapticIntensity: SettingsHapticIntensity
    val soundOnPress: Boolean
    val stickyModifiers: Boolean
    val keepModsAfterTab: Boolean
    val showKeyHints: Boolean
    val highContrastMode: Boolean
    val compactModifiers: Boolean
    val numpadStartsLocked: Boolean
    val numpadShowHints: Boolean
    val showStatusBar: Boolean
    val showComboPreview: Boolean
}

interface TrackpadCommonSettings {
    val pointerSpeed: Float
    val scrollSpeed: Float
    val invertScroll: Boolean
    val tapToClick: Boolean
    val twoFingerRightClick: Boolean
    val accelerationEnabled: Boolean
    val dragLockMode: Boolean
    val clickPressure: SettingsClickPressure
}

// ═════════════════════════════════════════════════════════════════════════════
// SHARED ENUMS
// ═════════════════════════════════════════════════════════════════════════════

@Serializable
enum class SettingsHapticIntensity(val label: String) {
    LIGHT("Light"),
    MEDIUM("Medium"),
    STRONG("Strong"),
}

@Serializable
enum class SettingsClickPressure { LIGHT, MEDIUM, FIRM }

@Serializable
enum class SettingsSidePosition { LEFT, RIGHT }

@Serializable
enum class SettingsSectionStyle(val label: String) {
    COMPACT("Standard"),
    MEDIA("Comfort"),
}

@Serializable
enum class SettingsKeyHeight(val label: String, val mainDp: Int, val fnDp: Int, val navDp: Int) {
    SMALL("Small", 36, 34, 40),
    MEDIUM("Medium", 42, 40, 48),
    LARGE("Large", 50, 46, 54),
}

@Serializable
enum class SettingsKeyFontSize(val label: String, val baseSp: Int) {
    SMALL("Small", 10),
    MEDIUM("Medium", 12),
    LARGE("Large", 14),
}

@Serializable
enum class SettingsMediaKeySize(val label: String, val heightDp: Int) {
    SMALL("Small", 48),
    MEDIUM("Medium", 64),
    LARGE("Large", 80),
}

@Serializable
enum class SettingsMediaRowGroup(val label: String, val icon: String) {
    TRANSPORT("Transport", "⏯"),
    VOLUME("Volume", "🔊"),
    BRIGHTNESS("Brightness", "🔆"),
}

@Serializable
enum class SettingsKeysTabSection(val label: String, val icon: String) {
    NAV_ARROWS("Navigation + Arrows", "↕"),
    SYSTEM_KEYS("System Keys", "⌨"),
    QUICK_MODS("Quick Modifiers", "⇧"),
    MERGED_SYSTEM_MODS("System & Modifiers", "⌨⇧"),
}

@Serializable
enum class SettingsNavTabSection(val label: String, val icon: String) {
    NAV_ARROWS("Navigation + Arrows", "↕"),
    INSERT_TOGGLE("Insert/Overwrite", "⎀"),
    SYSTEM_KEYS("System Keys", "⌨"),
    QUICK_MODS("Quick Modifiers", "⇧"),
    MERGED_SYSTEM_MODS("System & Modifiers", "⌨⇧"),
    TYPE_TEXT("Type & Send Text", "✎"),
}

@Serializable
enum class SettingsMediaTabSection(val label: String, val icon: String) {
    TRANSPORT("Transport", "⏯"),
    VOLUME_BRIGHTNESS("Volume & Brightness", "🔊"),
    NAVIGATION("Navigation", "↕"),
    ARROW_KEYS("Arrow Keys", "←→"),
    SYSTEM_KEYS("System Keys", "⌨"),
    QUICK_MODS("Quick Modifiers", "⇧"),
    MERGED_SYSTEM_MODS("System & Modifiers", "⌨⇧"),
}

@Serializable
enum class SettingsOptionalRow(val label: String, val icon: String) {
    MEDIA_ROW("Media Row", "🎵"),
    NAV_ROW("Navigation Row", "↕"),
}

@Serializable
enum class SettingsLayoutMode(val label: String) {
    SINGLE_COLUMN("Single Column"),
    TWO_COLUMN("Two Column"),
}

@Serializable
enum class SettingsRightColumnMode(val label: String) {
    NAV_CLUSTER("Nav Cluster"),
    NUMPAD("Numpad"),
}

@Serializable
enum class SettingsThemeMode { LIGHT, DARK, SYSTEM }

// ═════════════════════════════════════════════════════════════════════════════
// PORTRAIT KEYBOARD
// ═════════════════════════════════════════════════════════════════════════════

@Serializable
data class PortraitKeyboardSettings(
    // Common
    override val repeatEnabled: Boolean = true,
    override val repeatInitialDelayMs: Long = 350L,
    override val repeatIntervalMs: Long = 30L,
    override val hapticEnabled: Boolean = true,
    override val hapticIntensity: SettingsHapticIntensity = SettingsHapticIntensity.MEDIUM,
    override val soundOnPress: Boolean = true,
    override val stickyModifiers: Boolean = false,
    override val keepModsAfterTab: Boolean = true,
    override val showKeyHints: Boolean = true,
    override val highContrastMode: Boolean = false,
    override val compactModifiers: Boolean = false,
    override val numpadStartsLocked: Boolean = true,
    override val numpadShowHints: Boolean = true,
    override val showStatusBar: Boolean = true,
    override val showComboPreview: Boolean = true,

    // Portrait-only
    val keyHeight: SettingsKeyHeight = SettingsKeyHeight.MEDIUM,
    val keyFontSize: SettingsKeyFontSize = SettingsKeyFontSize.MEDIUM,
    val mediaKeySize: SettingsMediaKeySize = SettingsMediaKeySize.MEDIUM,
    val defaultTab: Int = 0,

    // Keys tab
    val keysTabShowNavigation: Boolean = true,
    val keysTabShowArrowKeys: Boolean = true,
    val keysTabShowSystemKeys: Boolean = true,
    val keysTabShowQuickMods: Boolean = false,
    val keysTabSectionStyle: SettingsSectionStyle = SettingsSectionStyle.MEDIA,
    val keysTabMergeSystemAndMods: Boolean = false,
    val keysTabInPlaceReorder: Boolean = false,
    val keysTabNavArrowsSwapped: Boolean = false,
    val keysTabSectionOrder: List<SettingsKeysTabSection> = listOf(
        SettingsKeysTabSection.NAV_ARROWS,
        SettingsKeysTabSection.SYSTEM_KEYS,
        SettingsKeysTabSection.QUICK_MODS,
    ),

    // Nav+Numpad tab
    val navTabShowNavigation: Boolean = false,
    val navTabShowArrowKeys: Boolean = false,
    val navTabShowInsertToggle: Boolean = false,
    val navTabShowTypeText: Boolean = true,
    val navTabShowSystemKeys: Boolean = true,
    val navTabShowQuickMods: Boolean = false,
    val navTabMergeSystemAndMods: Boolean = false,
    val navTabInPlaceReorder: Boolean = false,
    val navTabNavArrowsSwapped: Boolean = false,
    val navTabSectionOrder: List<SettingsNavTabSection> = listOf(
        SettingsNavTabSection.NAV_ARROWS,
        SettingsNavTabSection.INSERT_TOGGLE,
        SettingsNavTabSection.SYSTEM_KEYS,
        SettingsNavTabSection.QUICK_MODS,
        SettingsNavTabSection.TYPE_TEXT,
    ),

    // Media tab
    val mediaTabShowNavigation: Boolean = false,
    val mediaTabShowArrowKeys: Boolean = true,
    val mediaTabShowSystemKeys: Boolean = true,
    val mediaTabShowQuickMods: Boolean = false,
    val mediaTabSectionStyle: SettingsSectionStyle = SettingsSectionStyle.MEDIA,
    val mediaTabMergeSystemAndMods: Boolean = false,
    val mediaTabInPlaceReorder: Boolean = false,
    val mediaTabSectionOrder: List<SettingsMediaTabSection> = listOf(
        SettingsMediaTabSection.TRANSPORT,
        SettingsMediaTabSection.VOLUME_BRIGHTNESS,
        SettingsMediaTabSection.NAVIGATION,
        SettingsMediaTabSection.ARROW_KEYS,
        SettingsMediaTabSection.SYSTEM_KEYS,
        SettingsMediaTabSection.QUICK_MODS,
    ),

    // Global toggles
    val mergeSystemAndModsGlobal: Boolean = false,
    val inPlaceReorderGlobal: Boolean = false,

    // Optional rows
    val globalOptionalRowVisibility: Boolean = false,
    val globalShowMediaRow: Boolean = false,
    val globalShowNavRow: Boolean = false,
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
    val mediaRowGroupOrder: List<SettingsMediaRowGroup> = listOf(
        SettingsMediaRowGroup.VOLUME,
        SettingsMediaRowGroup.BRIGHTNESS,
        SettingsMediaRowGroup.TRANSPORT,
    ),

    // Optional row order
    val globalOptionalRowOrder: Boolean = false,
    val keyboardOptionalRowOrder: List<SettingsOptionalRow> = listOf(
        SettingsOptionalRow.MEDIA_ROW,
        SettingsOptionalRow.NAV_ROW,
    ),
    val keysTabOptionalRowOrder: List<SettingsOptionalRow> = listOf(
        SettingsOptionalRow.MEDIA_ROW,
        SettingsOptionalRow.NAV_ROW,
    ),
    val trackpadOptionalRowOrder: List<SettingsOptionalRow> = listOf(
        SettingsOptionalRow.MEDIA_ROW,
        SettingsOptionalRow.NAV_ROW,
    ),
) : KeyboardCommonSettings

// ═════════════════════════════════════════════════════════════════════════════
// LANDSCAPE KEYBOARD
// ═════════════════════════════════════════════════════════════════════════════

@Serializable
data class LandscapeKeyboardSettings(
    // Common
    override val repeatEnabled: Boolean = true,
    override val repeatInitialDelayMs: Long = 350L,
    override val repeatIntervalMs: Long = 30L,
    override val hapticEnabled: Boolean = true,
    override val hapticIntensity: SettingsHapticIntensity = SettingsHapticIntensity.MEDIUM,
    override val soundOnPress: Boolean = true,
    override val stickyModifiers: Boolean = false,
    override val keepModsAfterTab: Boolean = true,
    override val showKeyHints: Boolean = true,
    override val highContrastMode: Boolean = false,
    override val compactModifiers: Boolean = false,
    override val numpadStartsLocked: Boolean = true,
    override val numpadShowHints: Boolean = true,
    override val showStatusBar: Boolean = true,
    override val showComboPreview: Boolean = true,

    // Landscape-only
    val keyHeight: SettingsKeyHeight = SettingsKeyHeight.MEDIUM,
    val keyFontSize: SettingsKeyFontSize = SettingsKeyFontSize.MEDIUM,
    val mediaKeySize: SettingsMediaKeySize = SettingsMediaKeySize.MEDIUM,
    val landscapeLayoutMode: SettingsLayoutMode = SettingsLayoutMode.TWO_COLUMN,
    val defaultTab: Int = 0,

    // Keys tab
    val keysTabShowNavigation: Boolean = true,
    val keysTabShowArrowKeys: Boolean = true,
    val keysTabShowSystemKeys: Boolean = true,
    val keysTabShowQuickMods: Boolean = false,
    val keysTabSectionStyle: SettingsSectionStyle = SettingsSectionStyle.MEDIA,
    val keysTabMergeSystemAndMods: Boolean = false,
    val keysTabInPlaceReorder: Boolean = false,
    val keysTabNavArrowsSwapped: Boolean = false,
    val keysTabSectionOrder: List<SettingsKeysTabSection> = listOf(
        SettingsKeysTabSection.NAV_ARROWS,
        SettingsKeysTabSection.SYSTEM_KEYS,
        SettingsKeysTabSection.QUICK_MODS,
    ),

    // Nav+Numpad tab
    val navTabShowNavigation: Boolean = false,
    val navTabShowArrowKeys: Boolean = false,
    val navTabShowInsertToggle: Boolean = false,
    val navTabShowTypeText: Boolean = true,
    val navTabShowSystemKeys: Boolean = true,
    val navTabShowQuickMods: Boolean = false,
    val navTabMergeSystemAndMods: Boolean = false,
    val navTabInPlaceReorder: Boolean = false,
    val navTabNavArrowsSwapped: Boolean = false,
    val navTabSectionOrder: List<SettingsNavTabSection> = listOf(
        SettingsNavTabSection.NAV_ARROWS,
        SettingsNavTabSection.INSERT_TOGGLE,
        SettingsNavTabSection.SYSTEM_KEYS,
        SettingsNavTabSection.QUICK_MODS,
        SettingsNavTabSection.TYPE_TEXT,
    ),

    // Media tab
    val mediaTabShowNavigation: Boolean = false,
    val mediaTabShowArrowKeys: Boolean = true,
    val mediaTabShowSystemKeys: Boolean = true,
    val mediaTabShowQuickMods: Boolean = false,
    val mediaTabSectionStyle: SettingsSectionStyle = SettingsSectionStyle.MEDIA,
    val mediaTabMergeSystemAndMods: Boolean = false,
    val mediaTabInPlaceReorder: Boolean = false,
    val mediaTabSectionOrder: List<SettingsMediaTabSection> = listOf(
        SettingsMediaTabSection.TRANSPORT,
        SettingsMediaTabSection.VOLUME_BRIGHTNESS,
        SettingsMediaTabSection.NAVIGATION,
        SettingsMediaTabSection.ARROW_KEYS,
        SettingsMediaTabSection.SYSTEM_KEYS,
        SettingsMediaTabSection.QUICK_MODS,
    ),

    // Global toggles
    val mergeSystemAndModsGlobal: Boolean = false,
    val inPlaceReorderGlobal: Boolean = false,

    // Optional rows
    val globalOptionalRowVisibility: Boolean = false,
    val globalShowMediaRow: Boolean = false,
    val globalShowNavRow: Boolean = false,
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
    val mediaRowGroupOrder: List<SettingsMediaRowGroup> = listOf(
        SettingsMediaRowGroup.VOLUME,
        SettingsMediaRowGroup.BRIGHTNESS,
        SettingsMediaRowGroup.TRANSPORT,
    ),

    // Optional row order
    val globalOptionalRowOrder: Boolean = false,
    val keyboardOptionalRowOrder: List<SettingsOptionalRow> = listOf(
        SettingsOptionalRow.MEDIA_ROW,
        SettingsOptionalRow.NAV_ROW,
    ),
    val keysTabOptionalRowOrder: List<SettingsOptionalRow> = listOf(
        SettingsOptionalRow.MEDIA_ROW,
        SettingsOptionalRow.NAV_ROW,
    ),
    val trackpadOptionalRowOrder: List<SettingsOptionalRow> = listOf(
        SettingsOptionalRow.MEDIA_ROW,
        SettingsOptionalRow.NAV_ROW,
    ),
) : KeyboardCommonSettings

// ═════════════════════════════════════════════════════════════════════════════
// PORTRAIT TRACKPAD
// ═════════════════════════════════════════════════════════════════════════════

@Serializable
data class PortraitTrackpadSettings(
    // Common
    override val pointerSpeed: Float = 1.0f,
    override val scrollSpeed: Float = 0.5f,
    override val invertScroll: Boolean = false,
    override val tapToClick: Boolean = true,
    override val twoFingerRightClick: Boolean = true,
    override val accelerationEnabled: Boolean = true,
    override val dragLockMode: Boolean = false,
    override val clickPressure: SettingsClickPressure = SettingsClickPressure.MEDIUM,

    // Portrait-only
    val scrollPosition: SettingsSidePosition = SettingsSidePosition.RIGHT,
    val arrowPosition: SettingsSidePosition = SettingsSidePosition.LEFT,
    val showArrowKeys: Boolean = true,
    val showScrollStrip: Boolean = true,
    val showSystemKeyboard: Boolean = true,
    val showInAppKeyboard: Boolean = true,
) : TrackpadCommonSettings

// ═════════════════════════════════════════════════════════════════════════════
// LANDSCAPE TRACKPAD
// ═════════════════════════════════════════════════════════════════════════════

@Serializable
data class LandscapeTrackpadSettings(
    // Common
    override val pointerSpeed: Float = 1.0f,
    override val scrollSpeed: Float = 0.5f,
    override val invertScroll: Boolean = false,
    override val tapToClick: Boolean = true,
    override val twoFingerRightClick: Boolean = true,
    override val accelerationEnabled: Boolean = true,
    override val dragLockMode: Boolean = false,
    override val clickPressure: SettingsClickPressure = SettingsClickPressure.MEDIUM,

    // Landscape-only
    val scrollPosition: SettingsSidePosition = SettingsSidePosition.RIGHT,
    val arrowPosition: SettingsSidePosition = SettingsSidePosition.LEFT,
    val showArrowKeys: Boolean = true,
    val showScrollStrip: Boolean = true,
    val showSystemKeyboard: Boolean = true,
    val showInAppKeyboard: Boolean = true,
    val trackpadKbDefaultLayoutMode: SettingsLayoutMode = SettingsLayoutMode.TWO_COLUMN,
    val trackpadKbDefaultRightColumn: SettingsRightColumnMode = SettingsRightColumnMode.NAV_CLUSTER,
    val trackpadKbShowComboPreview: Boolean = true,
) : TrackpadCommonSettings

// ═════════════════════════════════════════════════════════════════════════════
// GENERAL & SYNC
// ═════════════════════════════════════════════════════════════════════════════

@Serializable
data class GeneralSettings(
    val themeMode: SettingsThemeMode = SettingsThemeMode.SYSTEM,
    val language: String = "en",
    val globalHapticIntensity: SettingsHapticIntensity = SettingsHapticIntensity.MEDIUM,
    val analyticsEnabled: Boolean = false,
)

@Serializable
data class SyncFlags(
    val syncKeyboardPortraitLandscape: Boolean = false,
    val syncTrackpadPortraitLandscape: Boolean = false,
)

// ═════════════════════════════════════════════════════════════════════════════
// ROOT
// ═════════════════════════════════════════════════════════════════════════════

@Serializable
data class AppSettings(
    val portraitKeyboard: PortraitKeyboardSettings = PortraitKeyboardSettings(),
    val landscapeKeyboard: LandscapeKeyboardSettings = LandscapeKeyboardSettings(),
    val portraitTrackpad: PortraitTrackpadSettings = PortraitTrackpadSettings(),
    val landscapeTrackpad: LandscapeTrackpadSettings = LandscapeTrackpadSettings(),
    val general: GeneralSettings = GeneralSettings(),
    val syncFlags: SyncFlags = SyncFlags(),
)