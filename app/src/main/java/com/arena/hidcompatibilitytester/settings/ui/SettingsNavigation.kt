package com.arena.hidcompatibilitytester.settings.ui

// ═════════════════════════════════════════════════════════════════════════════
// SETTINGS NAVIGATION MODEL
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Top-level categories on Page 1 (Settings Home).
 */
enum class SettingsCategory(val label: String, val icon: String) {
    GENERAL("General", "⚙"),
    PORTRAIT("Portrait", "📱"),
    LANDSCAPE("Landscape", "🖥"),
    FUTURE("Future", "🔮"),
    ABOUT("About", "ℹ"),
}

/**
 * Individual leaf destinations that open detail pages (Page 3 in portrait, Page 2 in landscape).
 */
enum class SettingsDestination(
    val label: String,
    val icon: String,
    val category: SettingsCategory,
    val enabled: Boolean = true,
) {
    // Portrait
    PORTRAIT_KEYBOARD("Keyboard", "⌨", SettingsCategory.PORTRAIT),
    PORTRAIT_TRACKPAD("Trackpad", "🖱", SettingsCategory.PORTRAIT),

    // Landscape
    LANDSCAPE_KEYBOARD("Keyboard", "⌨", SettingsCategory.LANDSCAPE),
    LANDSCAPE_TRACKPAD("Trackpad", "🖱", SettingsCategory.LANDSCAPE),

    // General
    GENERAL_APPEARANCE("Appearance", "🎨", SettingsCategory.GENERAL),
    GENERAL_BEHAVIOR("Behavior", "⚡", SettingsCategory.GENERAL),
    GENERAL_LANGUAGE("Language", "🌐", SettingsCategory.GENERAL),

    // Future (stubs)
    FUTURE_GAMEPAD("Gamepad", "🎮", SettingsCategory.FUTURE, enabled = false),
    FUTURE_PRESENTER("Presenter", "📽", SettingsCategory.FUTURE, enabled = false),
    FUTURE_SHORTCUTS("Shortcut Profiles", "⚡", SettingsCategory.FUTURE, enabled = false),

    // About
    ABOUT_VERSION("Version", "📋", SettingsCategory.ABOUT),
    ABOUT_LICENSES("Licenses", "📜", SettingsCategory.ABOUT),
    ABOUT_RESET("Reset All Settings", "↺", SettingsCategory.ABOUT),
}

/**
 * Sub-sections within a destination — used as tabs/rail items on the detail page.
 * Each destination has its own set of sub-sections defined below.
 */
data class SettingsSubSection(
    val id: String,
    val label: String,
    val icon: String,
)

// ─── Sub-sections for keyboards ─────────────────────────────────────────────
val KEYBOARD_SUB_SECTIONS = listOf(
    SettingsSubSection("keys", "Keys", "⌨"),
    SettingsSubSection("behavior", "Behavior", "⚡"),
    SettingsSubSection("appearance", "Appearance", "🎨"),
    SettingsSubSection("numpad", "Numpad", "🔢"),
    SettingsSubSection("media", "Media", "🎵"),
)

// ─── Sub-sections for trackpads ─────────────────────────────────────────────
val TRACKPAD_SUB_SECTIONS = listOf(
    SettingsSubSection("pointer", "Pointer", "🖱"),
    SettingsSubSection("gestures", "Gestures", "✋"),
    SettingsSubSection("layout", "Layout", "📐"),
    SettingsSubSection("keyboard", "Keyboard", "⌨"),
)

// ─── Sub-sections for general ───────────────────────────────────────────────
val GENERAL_APPEARANCE_SUB_SECTIONS = listOf(
    SettingsSubSection("theme", "Theme", "🎨"),
    SettingsSubSection("contrast", "Contrast", "◐"),
)

val GENERAL_BEHAVIOR_SUB_SECTIONS = listOf(
    SettingsSubSection("haptics", "Haptics", "📳"),
    SettingsSubSection("analytics", "Analytics", "📊"),
)

val GENERAL_LANGUAGE_SUB_SECTIONS = listOf(
    SettingsSubSection("language", "Language", "🌐"),
)

/**
 * Get the sub-sections list for a given destination.
 */
fun getSubSections(destination: SettingsDestination): List<SettingsSubSection> {
    return when (destination) {
        SettingsDestination.PORTRAIT_KEYBOARD,
        SettingsDestination.LANDSCAPE_KEYBOARD -> KEYBOARD_SUB_SECTIONS

        SettingsDestination.PORTRAIT_TRACKPAD,
        SettingsDestination.LANDSCAPE_TRACKPAD -> TRACKPAD_SUB_SECTIONS

        SettingsDestination.GENERAL_APPEARANCE -> GENERAL_APPEARANCE_SUB_SECTIONS
        SettingsDestination.GENERAL_BEHAVIOR -> GENERAL_BEHAVIOR_SUB_SECTIONS
        SettingsDestination.GENERAL_LANGUAGE -> GENERAL_LANGUAGE_SUB_SECTIONS

        else -> emptyList()
    }
}

/**
 * Navigation state for portrait 3-level flow.
 */
sealed class SettingsPage {
    /** Page 1 — Home (categories list) */
    object Home : SettingsPage()

    /** Page 2 — Category (list of destinations in a category) — PORTRAIT ONLY */
    data class Category(val category: SettingsCategory) : SettingsPage()

    /** Page 3 / Page 2 — Detail (sub-tabs + settings for a destination) */
    data class Detail(val destination: SettingsDestination) : SettingsPage()
}