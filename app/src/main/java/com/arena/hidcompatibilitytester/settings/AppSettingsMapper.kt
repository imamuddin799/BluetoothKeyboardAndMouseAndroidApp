package com.arena.hidcompatibilitytester.settings

import com.arena.hidcompatibilitytester.ui.screen.keyboard.*
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.*
import com.arena.hidcompatibilitytester.ui.screen.trackpad.*
import com.arena.hidcompatibilitytester.ui.screen.landscape.trackpad.*

// ═════════════════════════════════════════════════════════════════════════════
// Portrait Keyboard: KeyboardSettings ↔ PortraitKeyboardSettings
// ═════════════════════════════════════════════════════════════════════════════

fun KeyboardSettings.toPortraitSettings(): PortraitKeyboardSettings {
    return PortraitKeyboardSettings(
        repeatEnabled = repeatEnabled,
        repeatInitialDelayMs = repeatInitialDelayMs,
        repeatIntervalMs = repeatIntervalMs,
        hapticEnabled = hapticEnabled,
        hapticIntensity = hapticIntensity.toSettings(),
        soundOnPress = soundOnPress,
        stickyModifiers = stickyModifiers,
        keepModsAfterTab = keepModsAfterTab,
        showKeyHints = showKeyHints,
        highContrastMode = highContrastMode,
        compactModifiers = compactModifiers,
        numpadStartsLocked = numpadStartsLocked,
        numpadShowHints = numpadShowHints,
        showStatusBar = showStatusBar,
        showComboPreview = showComboPreview,
        keyHeight = keyHeight.toSettings(),
        keyFontSize = keyFontSize.toSettings(),
        mediaKeySize = mediaKeySize.toSettings(),
        defaultTab = defaultTab,
        keysTabShowNavigation = keysTabShowNavigation,
        keysTabShowArrowKeys = keysTabShowArrowKeys,
        keysTabShowSystemKeys = keysTabShowSystemKeys,
        keysTabShowQuickMods = keysTabShowQuickMods,
        keysTabSectionStyle = keysTabSectionStyle.toSettings(),
        keysTabMergeSystemAndMods = keysTabMergeSystemAndMods,
        keysTabInPlaceReorder = keysTabInPlaceReorder,
        keysTabNavArrowsSwapped = keysTabNavArrowsSwapped,
        keysTabSectionOrder = keysTabSectionOrder.map { it.toSettings() },
        navTabShowNavigation = navTabShowNavigation,
        navTabShowArrowKeys = navTabShowArrowKeys,
        navTabShowInsertToggle = navTabShowInsertToggle,
        navTabShowTypeText = navTabShowTypeText,
        navTabShowSystemKeys = navTabShowSystemKeys,
        navTabShowQuickMods = navTabShowQuickMods,
        navTabMergeSystemAndMods = navTabMergeSystemAndMods,
        navTabInPlaceReorder = navTabInPlaceReorder,
        navTabNavArrowsSwapped = navTabNavArrowsSwapped,
        navTabSectionOrder = navTabSectionOrder.map { it.toSettings() },
        mediaTabShowNavigation = mediaTabShowNavigation,
        mediaTabShowArrowKeys = mediaTabShowArrowKeys,
        mediaTabShowSystemKeys = mediaTabShowSystemKeys,
        mediaTabShowQuickMods = mediaTabShowQuickMods,
        mediaTabSectionStyle = mediaTabSectionStyle.toSettings(),
        mediaTabMergeSystemAndMods = mediaTabMergeSystemAndMods,
        mediaTabInPlaceReorder = mediaTabInPlaceReorder,
        mediaTabSectionOrder = mediaTabSectionOrder.map { it.toSettings() },
        mergeSystemAndModsGlobal = mergeSystemAndModsGlobal,
        inPlaceReorderGlobal = inPlaceReorderGlobal,
        globalOptionalRowVisibility = globalOptionalRowVisibility,
        globalShowMediaRow = globalShowMediaRow,
        globalShowNavRow = globalShowNavRow,
        keysTabShowMediaRow = keysTabShowMediaRow,
        keysTabShowNavRow = keysTabShowNavRow,
        trackpadShowMediaRow = trackpadShowMediaRow,
        trackpadShowNavRow = trackpadShowNavRow,
        mediaRowShowTransport = mediaRowShowTransport,
        mediaRowShowVolume = mediaRowShowVolume,
        mediaRowShowBrightness = mediaRowShowBrightness,
        mediaRowRepeatVolume = mediaRowRepeatVolume,
        mediaRowRepeatBrightness = mediaRowRepeatBrightness,
        mediaRowGroupOrder = mediaRowGroupOrder.map { it.toSettings() },
        globalOptionalRowOrder = globalOptionalRowOrder,
        keyboardOptionalRowOrder = keyboardOptionalRowOrder.map { it.toSettings() },
        keysTabOptionalRowOrder = keysTabOptionalRowOrder.map { it.toSettings() },
        trackpadOptionalRowOrder = trackpadOptionalRowOrder.map { it.toSettings() },
    )
}

fun PortraitKeyboardSettings.toScreenSettings(): KeyboardSettings {
    return KeyboardSettings(
        repeatEnabled = repeatEnabled,
        repeatInitialDelayMs = repeatInitialDelayMs,
        repeatIntervalMs = repeatIntervalMs,
        hapticEnabled = hapticEnabled,
        hapticIntensity = hapticIntensity.toScreen(),
        soundOnPress = soundOnPress,
        stickyModifiers = stickyModifiers,
        keepModsAfterTab = keepModsAfterTab,
        showKeyHints = showKeyHints,
        highContrastMode = highContrastMode,
        compactModifiers = compactModifiers,
        numpadStartsLocked = numpadStartsLocked,
        numpadShowHints = numpadShowHints,
        showStatusBar = showStatusBar,
        showComboPreview = showComboPreview,
        keyHeight = keyHeight.toScreen(),
        keyFontSize = keyFontSize.toScreen(),
        mediaKeySize = mediaKeySize.toScreen(),
        defaultTab = defaultTab,
        keysTabShowNavigation = keysTabShowNavigation,
        keysTabShowArrowKeys = keysTabShowArrowKeys,
        keysTabShowSystemKeys = keysTabShowSystemKeys,
        keysTabShowQuickMods = keysTabShowQuickMods,
        keysTabSectionStyle = keysTabSectionStyle.toScreen(),
        keysTabMergeSystemAndMods = keysTabMergeSystemAndMods,
        keysTabInPlaceReorder = keysTabInPlaceReorder,
        keysTabNavArrowsSwapped = keysTabNavArrowsSwapped,
        keysTabSectionOrder = keysTabSectionOrder.map { it.toScreen() },
        navTabShowNavigation = navTabShowNavigation,
        navTabShowArrowKeys = navTabShowArrowKeys,
        navTabShowInsertToggle = navTabShowInsertToggle,
        navTabShowTypeText = navTabShowTypeText,
        navTabShowSystemKeys = navTabShowSystemKeys,
        navTabShowQuickMods = navTabShowQuickMods,
        navTabMergeSystemAndMods = navTabMergeSystemAndMods,
        navTabInPlaceReorder = navTabInPlaceReorder,
        navTabNavArrowsSwapped = navTabNavArrowsSwapped,
        navTabSectionOrder = navTabSectionOrder.map { it.toScreen() },
        mediaTabShowNavigation = mediaTabShowNavigation,
        mediaTabShowArrowKeys = mediaTabShowArrowKeys,
        mediaTabShowSystemKeys = mediaTabShowSystemKeys,
        mediaTabShowQuickMods = mediaTabShowQuickMods,
        mediaTabSectionStyle = mediaTabSectionStyle.toScreen(),
        mediaTabMergeSystemAndMods = mediaTabMergeSystemAndMods,
        mediaTabInPlaceReorder = mediaTabInPlaceReorder,
        mediaTabSectionOrder = mediaTabSectionOrder.map { it.toScreen() },
        mergeSystemAndModsGlobal = mergeSystemAndModsGlobal,
        inPlaceReorderGlobal = inPlaceReorderGlobal,
        globalOptionalRowVisibility = globalOptionalRowVisibility,
        globalShowMediaRow = globalShowMediaRow,
        globalShowNavRow = globalShowNavRow,
        keysTabShowMediaRow = keysTabShowMediaRow,
        keysTabShowNavRow = keysTabShowNavRow,
        trackpadShowMediaRow = trackpadShowMediaRow,
        trackpadShowNavRow = trackpadShowNavRow,
        mediaRowShowTransport = mediaRowShowTransport,
        mediaRowShowVolume = mediaRowShowVolume,
        mediaRowShowBrightness = mediaRowShowBrightness,
        mediaRowRepeatVolume = mediaRowRepeatVolume,
        mediaRowRepeatBrightness = mediaRowRepeatBrightness,
        mediaRowGroupOrder = mediaRowGroupOrder.map { it.toScreen() },
        globalOptionalRowOrder = globalOptionalRowOrder,
        keyboardOptionalRowOrder = keyboardOptionalRowOrder.map { it.toScreen() },
        keysTabOptionalRowOrder = keysTabOptionalRowOrder.map { it.toScreen() },
        trackpadOptionalRowOrder = trackpadOptionalRowOrder.map { it.toScreen() },
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// Landscape Keyboard: LandscapeKeyboardSettings ↔ LandscapeKeyboardSettings (settings pkg)
// ═════════════════════════════════════════════════════════════════════════════

fun com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.LandscapeKeyboardSettings.toLandscapeSettings(): LandscapeKeyboardSettings {
    return LandscapeKeyboardSettings(
        repeatEnabled = repeatEnabled,
        repeatInitialDelayMs = repeatInitialDelayMs,
        repeatIntervalMs = repeatIntervalMs,
        hapticEnabled = hapticEnabled,
        hapticIntensity = hapticIntensity.toSettings(),
        soundOnPress = soundOnPress,
        stickyModifiers = stickyModifiers,
        keepModsAfterTab = keepModsAfterTab,
        showKeyHints = showKeyHints,
        highContrastMode = highContrastMode,
        compactModifiers = compactModifiers,
        numpadStartsLocked = numpadStartsLocked,
        numpadShowHints = numpadShowHints,
        showStatusBar = showStatusBar,
        showComboPreview = showComboPreview,
        keyHeight = keyHeight.toSettings(),
        keyFontSize = keyFontSize.toSettings(),
        mediaKeySize = mediaKeySize.toSettings(),
        landscapeLayoutMode = landscapeLayoutMode.toSettings(),
        defaultTab = defaultTab,
        keysTabShowNavigation = keysTabShowNavigation,
        keysTabShowArrowKeys = keysTabShowArrowKeys,
        keysTabShowSystemKeys = keysTabShowSystemKeys,
        keysTabShowQuickMods = keysTabShowQuickMods,
        keysTabSectionStyle = keysTabSectionStyle.toSettings(),
        keysTabMergeSystemAndMods = keysTabMergeSystemAndMods,
        keysTabInPlaceReorder = keysTabInPlaceReorder,
        keysTabNavArrowsSwapped = keysTabNavArrowsSwapped,
        keysTabSectionOrder = keysTabSectionOrder.map { it.toSettings() },
        navTabShowNavigation = navTabShowNavigation,
        navTabShowArrowKeys = navTabShowArrowKeys,
        navTabShowInsertToggle = navTabShowInsertToggle,
        navTabShowTypeText = navTabShowTypeText,
        navTabShowSystemKeys = navTabShowSystemKeys,
        navTabShowQuickMods = navTabShowQuickMods,
        navTabMergeSystemAndMods = navTabMergeSystemAndMods,
        navTabInPlaceReorder = navTabInPlaceReorder,
        navTabNavArrowsSwapped = navTabNavArrowsSwapped,
        navTabSectionOrder = navTabSectionOrder.map { it.toSettings() },
        mediaTabShowNavigation = mediaTabShowNavigation,
        mediaTabShowArrowKeys = mediaTabShowArrowKeys,
        mediaTabShowSystemKeys = mediaTabShowSystemKeys,
        mediaTabShowQuickMods = mediaTabShowQuickMods,
        mediaTabSectionStyle = mediaTabSectionStyle.toSettings(),
        mediaTabMergeSystemAndMods = mediaTabMergeSystemAndMods,
        mediaTabInPlaceReorder = mediaTabInPlaceReorder,
        mediaTabSectionOrder = mediaTabSectionOrder.map { it.toSettings() },
        mergeSystemAndModsGlobal = mergeSystemAndModsGlobal,
        inPlaceReorderGlobal = inPlaceReorderGlobal,
        globalOptionalRowVisibility = globalOptionalRowVisibility,
        globalShowMediaRow = globalShowMediaRow,
        globalShowNavRow = globalShowNavRow,
        keysTabShowMediaRow = keysTabShowMediaRow,
        keysTabShowNavRow = keysTabShowNavRow,
        trackpadShowMediaRow = trackpadShowMediaRow,
        trackpadShowNavRow = trackpadShowNavRow,
        mediaRowShowTransport = mediaRowShowTransport,
        mediaRowShowVolume = mediaRowShowVolume,
        mediaRowShowBrightness = mediaRowShowBrightness,
        mediaRowRepeatVolume = mediaRowRepeatVolume,
        mediaRowRepeatBrightness = mediaRowRepeatBrightness,
        mediaRowGroupOrder = mediaRowGroupOrder.map { it.toSettings() },
        globalOptionalRowOrder = globalOptionalRowOrder,
        keyboardOptionalRowOrder = keyboardOptionalRowOrder.map { it.toSettings() },
        keysTabOptionalRowOrder = keysTabOptionalRowOrder.map { it.toSettings() },
        trackpadOptionalRowOrder = trackpadOptionalRowOrder.map { it.toSettings() },
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// Enum converters: Screen → Settings
// ═════════════════════════════════════════════════════════════════════════════

// Portrait keyboard enums
private fun HapticIntensity.toSettings() = when (this) {
    HapticIntensity.LIGHT -> SettingsHapticIntensity.LIGHT
    HapticIntensity.MEDIUM -> SettingsHapticIntensity.MEDIUM
    HapticIntensity.STRONG -> SettingsHapticIntensity.STRONG
}

private fun KeyHeight.toSettings() = when (this) {
    KeyHeight.SMALL -> SettingsKeyHeight.SMALL
    KeyHeight.MEDIUM -> SettingsKeyHeight.MEDIUM
    KeyHeight.LARGE -> SettingsKeyHeight.LARGE
}

private fun KeyFontSize.toSettings() = when (this) {
    KeyFontSize.SMALL -> SettingsKeyFontSize.SMALL
    KeyFontSize.MEDIUM -> SettingsKeyFontSize.MEDIUM
    KeyFontSize.LARGE -> SettingsKeyFontSize.LARGE
}

private fun MediaKeySize.toSettings() = when (this) {
    MediaKeySize.SMALL -> SettingsMediaKeySize.SMALL
    MediaKeySize.MEDIUM -> SettingsMediaKeySize.MEDIUM
    MediaKeySize.LARGE -> SettingsMediaKeySize.LARGE
}

private fun SectionStyle.toSettings() = when (this) {
    SectionStyle.COMPACT -> SettingsSectionStyle.COMPACT
    SectionStyle.MEDIA -> SettingsSectionStyle.MEDIA
}

private fun KeysTabSection.toSettings() = when (this) {
    KeysTabSection.NAV_ARROWS -> SettingsKeysTabSection.NAV_ARROWS
    KeysTabSection.SYSTEM_KEYS -> SettingsKeysTabSection.SYSTEM_KEYS
    KeysTabSection.QUICK_MODS -> SettingsKeysTabSection.QUICK_MODS
    KeysTabSection.MERGED_SYSTEM_MODS -> SettingsKeysTabSection.MERGED_SYSTEM_MODS
}

private fun NavTabSection.toSettings() = when (this) {
    NavTabSection.NAV_ARROWS -> SettingsNavTabSection.NAV_ARROWS
    NavTabSection.INSERT_TOGGLE -> SettingsNavTabSection.INSERT_TOGGLE
    NavTabSection.SYSTEM_KEYS -> SettingsNavTabSection.SYSTEM_KEYS
    NavTabSection.QUICK_MODS -> SettingsNavTabSection.QUICK_MODS
    NavTabSection.MERGED_SYSTEM_MODS -> SettingsNavTabSection.MERGED_SYSTEM_MODS
    NavTabSection.TYPE_TEXT -> SettingsNavTabSection.TYPE_TEXT
}

private fun MediaTabSection.toSettings() = when (this) {
    MediaTabSection.TRANSPORT -> SettingsMediaTabSection.TRANSPORT
    MediaTabSection.VOLUME_BRIGHTNESS -> SettingsMediaTabSection.VOLUME_BRIGHTNESS
    MediaTabSection.NAVIGATION -> SettingsMediaTabSection.NAVIGATION
    MediaTabSection.ARROW_KEYS -> SettingsMediaTabSection.ARROW_KEYS
    MediaTabSection.SYSTEM_KEYS -> SettingsMediaTabSection.SYSTEM_KEYS
    MediaTabSection.QUICK_MODS -> SettingsMediaTabSection.QUICK_MODS
    MediaTabSection.MERGED_SYSTEM_MODS -> SettingsMediaTabSection.MERGED_SYSTEM_MODS
}

private fun MediaRowGroup.toSettings() = when (this) {
    MediaRowGroup.TRANSPORT -> SettingsMediaRowGroup.TRANSPORT
    MediaRowGroup.VOLUME -> SettingsMediaRowGroup.VOLUME
    MediaRowGroup.BRIGHTNESS -> SettingsMediaRowGroup.BRIGHTNESS
}

private fun KeyboardOptionalRow.toSettings() = when (this) {
    KeyboardOptionalRow.MEDIA_ROW -> SettingsOptionalRow.MEDIA_ROW
    KeyboardOptionalRow.NAV_ROW -> SettingsOptionalRow.NAV_ROW
}

// ═════════════════════════════════════════════════════════════════════════════
// Enum converters: Settings → Screen
// ═════════════════════════════════════════════════════════════════════════════

private fun SettingsHapticIntensity.toScreen() = when (this) {
    SettingsHapticIntensity.LIGHT -> HapticIntensity.LIGHT
    SettingsHapticIntensity.MEDIUM -> HapticIntensity.MEDIUM
    SettingsHapticIntensity.STRONG -> HapticIntensity.STRONG
}

private fun SettingsKeyHeight.toScreen() = when (this) {
    SettingsKeyHeight.SMALL -> KeyHeight.SMALL
    SettingsKeyHeight.MEDIUM -> KeyHeight.MEDIUM
    SettingsKeyHeight.LARGE -> KeyHeight.LARGE
}

private fun SettingsKeyFontSize.toScreen() = when (this) {
    SettingsKeyFontSize.SMALL -> KeyFontSize.SMALL
    SettingsKeyFontSize.MEDIUM -> KeyFontSize.MEDIUM
    SettingsKeyFontSize.LARGE -> KeyFontSize.LARGE
}

private fun SettingsMediaKeySize.toScreen() = when (this) {
    SettingsMediaKeySize.SMALL -> MediaKeySize.SMALL
    SettingsMediaKeySize.MEDIUM -> MediaKeySize.MEDIUM
    SettingsMediaKeySize.LARGE -> MediaKeySize.LARGE
}

private fun SettingsSectionStyle.toScreen() = when (this) {
    SettingsSectionStyle.COMPACT -> SectionStyle.COMPACT
    SettingsSectionStyle.MEDIA -> SectionStyle.MEDIA
}

private fun SettingsKeysTabSection.toScreen() = when (this) {
    SettingsKeysTabSection.NAV_ARROWS -> KeysTabSection.NAV_ARROWS
    SettingsKeysTabSection.SYSTEM_KEYS -> KeysTabSection.SYSTEM_KEYS
    SettingsKeysTabSection.QUICK_MODS -> KeysTabSection.QUICK_MODS
    SettingsKeysTabSection.MERGED_SYSTEM_MODS -> KeysTabSection.MERGED_SYSTEM_MODS
}

private fun SettingsNavTabSection.toScreen() = when (this) {
    SettingsNavTabSection.NAV_ARROWS -> NavTabSection.NAV_ARROWS
    SettingsNavTabSection.INSERT_TOGGLE -> NavTabSection.INSERT_TOGGLE
    SettingsNavTabSection.SYSTEM_KEYS -> NavTabSection.SYSTEM_KEYS
    SettingsNavTabSection.QUICK_MODS -> NavTabSection.QUICK_MODS
    SettingsNavTabSection.MERGED_SYSTEM_MODS -> NavTabSection.MERGED_SYSTEM_MODS
    SettingsNavTabSection.TYPE_TEXT -> NavTabSection.TYPE_TEXT
}

private fun SettingsMediaTabSection.toScreen() = when (this) {
    SettingsMediaTabSection.TRANSPORT -> MediaTabSection.TRANSPORT
    SettingsMediaTabSection.VOLUME_BRIGHTNESS -> MediaTabSection.VOLUME_BRIGHTNESS
    SettingsMediaTabSection.NAVIGATION -> MediaTabSection.NAVIGATION
    SettingsMediaTabSection.ARROW_KEYS -> MediaTabSection.ARROW_KEYS
    SettingsMediaTabSection.SYSTEM_KEYS -> MediaTabSection.SYSTEM_KEYS
    SettingsMediaTabSection.QUICK_MODS -> MediaTabSection.QUICK_MODS
    SettingsMediaTabSection.MERGED_SYSTEM_MODS -> MediaTabSection.MERGED_SYSTEM_MODS
}

private fun SettingsMediaRowGroup.toScreen() = when (this) {
    SettingsMediaRowGroup.TRANSPORT -> MediaRowGroup.TRANSPORT
    SettingsMediaRowGroup.VOLUME -> MediaRowGroup.VOLUME
    SettingsMediaRowGroup.BRIGHTNESS -> MediaRowGroup.BRIGHTNESS
}

private fun SettingsOptionalRow.toScreen() = when (this) {
    SettingsOptionalRow.MEDIA_ROW -> KeyboardOptionalRow.MEDIA_ROW
    SettingsOptionalRow.NAV_ROW -> KeyboardOptionalRow.NAV_ROW
}

// ═════════════════════════════════════════════════════════════════════════════
// Landscape keyboard enum converters
// ═════════════════════════════════════════════════════════════════════════════

private fun LandscapeHapticIntensity.toSettings() = when (this) {
    LandscapeHapticIntensity.LIGHT -> SettingsHapticIntensity.LIGHT
    LandscapeHapticIntensity.MEDIUM -> SettingsHapticIntensity.MEDIUM
    LandscapeHapticIntensity.STRONG -> SettingsHapticIntensity.STRONG
}

private fun LandscapeKeyHeight.toSettings() = when (this) {
    LandscapeKeyHeight.SMALL -> SettingsKeyHeight.SMALL
    LandscapeKeyHeight.MEDIUM -> SettingsKeyHeight.MEDIUM
    LandscapeKeyHeight.LARGE -> SettingsKeyHeight.LARGE
}

private fun LandscapeKeyFontSize.toSettings() = when (this) {
    LandscapeKeyFontSize.SMALL -> SettingsKeyFontSize.SMALL
    LandscapeKeyFontSize.MEDIUM -> SettingsKeyFontSize.MEDIUM
    LandscapeKeyFontSize.LARGE -> SettingsKeyFontSize.LARGE
}

private fun LandscapeMediaKeySize.toSettings() = when (this) {
    LandscapeMediaKeySize.SMALL -> SettingsMediaKeySize.SMALL
    LandscapeMediaKeySize.MEDIUM -> SettingsMediaKeySize.MEDIUM
    LandscapeMediaKeySize.LARGE -> SettingsMediaKeySize.LARGE
}

private fun LandscapeSectionStyle.toSettings() = when (this) {
    LandscapeSectionStyle.COMPACT -> SettingsSectionStyle.COMPACT
    LandscapeSectionStyle.MEDIA -> SettingsSectionStyle.MEDIA
}

private fun LandscapeLayoutMode.toSettings() = when (this) {
    LandscapeLayoutMode.SINGLE_COLUMN -> SettingsLayoutMode.SINGLE_COLUMN
    LandscapeLayoutMode.TWO_COLUMN -> SettingsLayoutMode.TWO_COLUMN
}

private fun LandscapeKeysTabSection.toSettings() = when (this) {
    LandscapeKeysTabSection.NAV_ARROWS -> SettingsKeysTabSection.NAV_ARROWS
    LandscapeKeysTabSection.SYSTEM_KEYS -> SettingsKeysTabSection.SYSTEM_KEYS
    LandscapeKeysTabSection.QUICK_MODS -> SettingsKeysTabSection.QUICK_MODS
    LandscapeKeysTabSection.MERGED_SYSTEM_MODS -> SettingsKeysTabSection.MERGED_SYSTEM_MODS
}

private fun LandscapeNavTabSection.toSettings() = when (this) {
    LandscapeNavTabSection.NAV_ARROWS -> SettingsNavTabSection.NAV_ARROWS
    LandscapeNavTabSection.INSERT_TOGGLE -> SettingsNavTabSection.INSERT_TOGGLE
    LandscapeNavTabSection.SYSTEM_KEYS -> SettingsNavTabSection.SYSTEM_KEYS
    LandscapeNavTabSection.QUICK_MODS -> SettingsNavTabSection.QUICK_MODS
    LandscapeNavTabSection.MERGED_SYSTEM_MODS -> SettingsNavTabSection.MERGED_SYSTEM_MODS
    LandscapeNavTabSection.TYPE_TEXT -> SettingsNavTabSection.TYPE_TEXT
}

private fun LandscapeMediaTabSection.toSettings() = when (this) {
    LandscapeMediaTabSection.TRANSPORT -> SettingsMediaTabSection.TRANSPORT
    LandscapeMediaTabSection.VOLUME_BRIGHTNESS -> SettingsMediaTabSection.VOLUME_BRIGHTNESS
    LandscapeMediaTabSection.NAVIGATION -> SettingsMediaTabSection.NAVIGATION
    LandscapeMediaTabSection.ARROW_KEYS -> SettingsMediaTabSection.ARROW_KEYS
    LandscapeMediaTabSection.SYSTEM_KEYS -> SettingsMediaTabSection.SYSTEM_KEYS
    LandscapeMediaTabSection.QUICK_MODS -> SettingsMediaTabSection.QUICK_MODS
    LandscapeMediaTabSection.MERGED_SYSTEM_MODS -> SettingsMediaTabSection.MERGED_SYSTEM_MODS
}

private fun LandscapeMediaRowGroup.toSettings() = when (this) {
    LandscapeMediaRowGroup.TRANSPORT -> SettingsMediaRowGroup.TRANSPORT
    LandscapeMediaRowGroup.VOLUME -> SettingsMediaRowGroup.VOLUME
    LandscapeMediaRowGroup.BRIGHTNESS -> SettingsMediaRowGroup.BRIGHTNESS
}

private fun LandscapeKeyboardOptionalRow.toSettings() = when (this) {
    LandscapeKeyboardOptionalRow.MEDIA_ROW -> SettingsOptionalRow.MEDIA_ROW
    LandscapeKeyboardOptionalRow.NAV_ROW -> SettingsOptionalRow.NAV_ROW
}