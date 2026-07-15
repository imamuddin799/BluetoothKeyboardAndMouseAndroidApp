package com.arena.hidcompatibilitytester.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

object AppSettingsStore {

    private val KEY_SETTINGS_JSON = stringPreferencesKey("settings_json")
    private val KEY_MIGRATED = stringPreferencesKey("migrated_from_v1")

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    // ── Flow of current settings ──────────────────────────────────────────
    fun settingsFlow(context: Context): Flow<AppSettings> {
        return context.settingsDataStore.data.map { prefs ->
            val raw = prefs[KEY_SETTINGS_JSON]
            if (raw.isNullOrBlank()) AppSettings()
            else runCatching { json.decodeFromString<AppSettings>(raw) }
                .getOrDefault(AppSettings())
        }
    }

    // ── One-shot read ─────────────────────────────────────────────────────
    suspend fun load(context: Context): AppSettings {
        maybeMigrate(context)
        return settingsFlow(context).first()
    }

    // ── Save ──────────────────────────────────────────────────────────────
    suspend fun save(context: Context, settings: AppSettings) {
        val encoded = json.encodeToString(settings)
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_SETTINGS_JSON] = encoded
        }
    }

    // ── Reset to defaults ─────────────────────────────────────────────────
    suspend fun reset(context: Context) {
        save(context, AppSettings())
    }

    // ═════════════════════════════════════════════════════════════════════
    // MIGRATION FROM OLD 4 SHAREDPREFERENCES STORES
    // ═════════════════════════════════════════════════════════════════════

    private suspend fun maybeMigrate(context: Context) {
        val prefs = context.settingsDataStore.data.first()
        if (prefs[KEY_MIGRATED] == "true") return

        // Check if any old prefs exist
        val hasOldPrefs = listOf(
            "keyboard_settings",
            "trackpad_settings",
            "landscape_keyboard_settings",
            "landscape_trackpad_settings",
        ).any { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE).all.isNotEmpty()
        }

        if (!hasOldPrefs) {
            // Fresh install — no migration needed, just mark as done
            context.settingsDataStore.edit { it[KEY_MIGRATED] = "true" }
            return
        }

        // Perform migration
        val migrated = performMigration(context)
        save(context, migrated)
        context.settingsDataStore.edit { it[KEY_MIGRATED] = "true" }
    }

    private fun performMigration(context: Context): AppSettings {
        return AppSettings(
            portraitKeyboard = migratePortraitKeyboard(context),
            landscapeKeyboard = migrateLandscapeKeyboard(context),
            portraitTrackpad = migratePortraitTrackpad(context),
            landscapeTrackpad = migrateLandscapeTrackpad(context),
            general = GeneralSettings(),
            syncFlags = SyncFlags(),
        )
    }

    // ── Portrait Keyboard ─────────────────────────────────────────────────
    private fun migratePortraitKeyboard(context: Context): PortraitKeyboardSettings {
        val p = context.getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE)
        val defaults = PortraitKeyboardSettings()

        return PortraitKeyboardSettings(
            repeatEnabled = p.getBoolean("repeatEnabled", defaults.repeatEnabled),
            repeatInitialDelayMs = p.getLong("repeatInitialDelayMs", defaults.repeatInitialDelayMs),
            repeatIntervalMs = p.getLong("repeatIntervalMs", defaults.repeatIntervalMs),
            hapticEnabled = p.getBoolean("hapticEnabled", defaults.hapticEnabled),
            hapticIntensity = safeEnum(p.getString("hapticIntensity", null), defaults.hapticIntensity),
            soundOnPress = p.getBoolean("soundOnPress", defaults.soundOnPress),
            stickyModifiers = p.getBoolean("stickyModifiers", defaults.stickyModifiers),
            keepModsAfterTab = p.getBoolean("keepModsAfterTab", defaults.keepModsAfterTab),
            showKeyHints = p.getBoolean("showKeyHints", defaults.showKeyHints),
            highContrastMode = p.getBoolean("highContrastMode", defaults.highContrastMode),
            compactModifiers = p.getBoolean("compactModifiers", defaults.compactModifiers),
            numpadStartsLocked = p.getBoolean("numpadStartsLocked", defaults.numpadStartsLocked),
            numpadShowHints = p.getBoolean("numpadShowHints", defaults.numpadShowHints),
            showStatusBar = p.getBoolean("showStatusBar", defaults.showStatusBar),
            showComboPreview = p.getBoolean("showComboPreview", defaults.showComboPreview),
            keyHeight = safeEnum(p.getString("keyHeight", null), defaults.keyHeight),
            keyFontSize = safeEnum(p.getString("keyFontSize", null), defaults.keyFontSize),
            mediaKeySize = safeEnum(p.getString("mediaKeySize", null), defaults.mediaKeySize),
            defaultTab = p.getInt("defaultTab", defaults.defaultTab),

            keysTabShowNavigation = p.getBoolean("keysTabShowNavigation", defaults.keysTabShowNavigation),
            keysTabShowArrowKeys = p.getBoolean("keysTabShowArrowKeys", defaults.keysTabShowArrowKeys),
            keysTabShowSystemKeys = p.getBoolean("keysTabShowSystemKeys", defaults.keysTabShowSystemKeys),
            keysTabShowQuickMods = p.getBoolean("keysTabShowQuickMods", defaults.keysTabShowQuickMods),
            keysTabSectionStyle = safeEnum(p.getString("keysTabSectionStyle", null), defaults.keysTabSectionStyle),
            keysTabMergeSystemAndMods = p.getBoolean("keysTabMergeSystemAndMods", defaults.keysTabMergeSystemAndMods),
            keysTabInPlaceReorder = p.getBoolean("keysTabInPlaceReorder", defaults.keysTabInPlaceReorder),
            keysTabNavArrowsSwapped = p.getBoolean("keysTabNavArrowsSwapped", defaults.keysTabNavArrowsSwapped),
            keysTabSectionOrder = safeEnumList(p.getString("keysTabSectionOrder", null), defaults.keysTabSectionOrder),

            navTabShowNavigation = p.getBoolean("navTabShowNavigation", defaults.navTabShowNavigation),
            navTabShowArrowKeys = p.getBoolean("navTabShowArrowKeys", defaults.navTabShowArrowKeys),
            navTabShowInsertToggle = p.getBoolean("navTabShowInsertToggle", defaults.navTabShowInsertToggle),
            navTabShowTypeText = p.getBoolean("navTabShowTypeText", defaults.navTabShowTypeText),
            navTabShowSystemKeys = p.getBoolean("navTabShowSystemKeys", defaults.navTabShowSystemKeys),
            navTabShowQuickMods = p.getBoolean("navTabShowQuickMods", defaults.navTabShowQuickMods),
            navTabSectionStyle = safeEnum(p.getString("navTabSectionStyle", null), defaults.navTabSectionStyle),
            navTabMergeSystemAndMods = p.getBoolean("navTabMergeSystemAndMods", defaults.navTabMergeSystemAndMods),
            navTabInPlaceReorder = p.getBoolean("navTabInPlaceReorder", defaults.navTabInPlaceReorder),
            navTabNavArrowsSwapped = p.getBoolean("navTabNavArrowsSwapped", defaults.navTabNavArrowsSwapped),
            navTabSectionOrder = safeEnumList(p.getString("navTabSectionOrder", null), defaults.navTabSectionOrder),

            mediaTabShowNavigation = p.getBoolean("mediaTabShowNavigation", defaults.mediaTabShowNavigation),
            mediaTabShowArrowKeys = p.getBoolean("mediaTabShowArrowKeys", defaults.mediaTabShowArrowKeys),
            mediaTabShowSystemKeys = p.getBoolean("mediaTabShowSystemKeys", defaults.mediaTabShowSystemKeys),
            mediaTabShowQuickMods = p.getBoolean("mediaTabShowQuickMods", defaults.mediaTabShowQuickMods),
            mediaTabSectionStyle = safeEnum(p.getString("mediaTabSectionStyle", null), defaults.mediaTabSectionStyle),
            mediaTabMergeSystemAndMods = p.getBoolean("mediaTabMergeSystemAndMods", defaults.mediaTabMergeSystemAndMods),
            mediaTabInPlaceReorder = p.getBoolean("mediaTabInPlaceReorder", defaults.mediaTabInPlaceReorder),
            mediaTabSectionOrder = safeEnumList(p.getString("mediaTabSectionOrder", null), defaults.mediaTabSectionOrder),

            mergeSystemAndModsGlobal = p.getBoolean("mergeSystemAndModsGlobal", defaults.mergeSystemAndModsGlobal),
            inPlaceReorderGlobal = p.getBoolean("inPlaceReorderGlobal", defaults.inPlaceReorderGlobal),

            globalOptionalRowVisibility = p.getBoolean("globalOptionalRowVisibility", defaults.globalOptionalRowVisibility),
            globalShowMediaRow = p.getBoolean("globalShowMediaRow", defaults.globalShowMediaRow),
            globalShowNavRow = p.getBoolean("globalShowNavRow", defaults.globalShowNavRow),
            keysTabShowMediaRow = p.getBoolean("keysTabShowMediaRow", defaults.keysTabShowMediaRow),
            keysTabShowNavRow = p.getBoolean("keysTabShowNavRow", defaults.keysTabShowNavRow),
            trackpadShowMediaRow = p.getBoolean("trackpadShowMediaRow", defaults.trackpadShowMediaRow),
            trackpadShowNavRow = p.getBoolean("trackpadShowNavRow", defaults.trackpadShowNavRow),

            mediaRowShowTransport = p.getBoolean("mediaRowShowTransport", defaults.mediaRowShowTransport),
            mediaRowShowVolume = p.getBoolean("mediaRowShowVolume", defaults.mediaRowShowVolume),
            mediaRowShowBrightness = p.getBoolean("mediaRowShowBrightness", defaults.mediaRowShowBrightness),
            mediaRowRepeatVolume = p.getBoolean("mediaRowRepeatVolume", defaults.mediaRowRepeatVolume),
            mediaRowRepeatBrightness = p.getBoolean("mediaRowRepeatBrightness", defaults.mediaRowRepeatBrightness),
            mediaRowGroupOrder = safeEnumList(p.getString("mediaRowGroupOrder", null), defaults.mediaRowGroupOrder),

            globalOptionalRowOrder = p.getBoolean("globalOptionalRowOrder", defaults.globalOptionalRowOrder),
            keyboardOptionalRowOrder = safeEnumList(p.getString("keyboardOptionalRowOrder", null), defaults.keyboardOptionalRowOrder),
            keysTabOptionalRowOrder = safeEnumList(p.getString("keysTabOptionalRowOrder", null), defaults.keysTabOptionalRowOrder),
            trackpadOptionalRowOrder = safeEnumList(p.getString("trackpadOptionalRowOrder", null), defaults.trackpadOptionalRowOrder),
        )
    }

    // ── Landscape Keyboard ────────────────────────────────────────────────
    private fun migrateLandscapeKeyboard(context: Context): LandscapeKeyboardSettings {
        val p = context.getSharedPreferences("landscape_keyboard_settings", Context.MODE_PRIVATE)
        val d = LandscapeKeyboardSettings()

        return LandscapeKeyboardSettings(
            repeatEnabled = p.getBoolean("repeatEnabled", d.repeatEnabled),
            repeatInitialDelayMs = p.getLong("repeatInitialDelayMs", d.repeatInitialDelayMs),
            repeatIntervalMs = p.getLong("repeatIntervalMs", d.repeatIntervalMs),
            hapticEnabled = p.getBoolean("hapticEnabled", d.hapticEnabled),
            hapticIntensity = safeEnum(p.getString("hapticIntensity", null), d.hapticIntensity),
            soundOnPress = p.getBoolean("soundOnPress", d.soundOnPress),
            stickyModifiers = p.getBoolean("stickyModifiers", d.stickyModifiers),
            keepModsAfterTab = p.getBoolean("keepModsAfterTab", d.keepModsAfterTab),
            showKeyHints = p.getBoolean("showKeyHints", d.showKeyHints),
            highContrastMode = p.getBoolean("highContrastMode", d.highContrastMode),
            compactModifiers = p.getBoolean("compactModifiers", d.compactModifiers),
            numpadStartsLocked = p.getBoolean("numpadStartsLocked", d.numpadStartsLocked),
            numpadShowHints = p.getBoolean("numpadShowHints", d.numpadShowHints),
            showStatusBar = p.getBoolean("showStatusBar", d.showStatusBar),
            showComboPreview = p.getBoolean("showComboPreview", d.showComboPreview),
            keyHeight = safeEnum(p.getString("keyHeight", null), d.keyHeight),
            keyFontSize = safeEnum(p.getString("keyFontSize", null), d.keyFontSize),
            mediaKeySize = safeEnum(p.getString("mediaKeySize", null), d.mediaKeySize),
            landscapeLayoutMode = safeEnum(p.getString("landscapeLayoutMode", null), d.landscapeLayoutMode),
            defaultTab = p.getInt("defaultTab", d.defaultTab),

            keysTabShowNavigation = p.getBoolean("keysTabShowNavigation", d.keysTabShowNavigation),
            keysTabShowArrowKeys = p.getBoolean("keysTabShowArrowKeys", d.keysTabShowArrowKeys),
            keysTabShowSystemKeys = p.getBoolean("keysTabShowSystemKeys", d.keysTabShowSystemKeys),
            keysTabShowQuickMods = p.getBoolean("keysTabShowQuickMods", d.keysTabShowQuickMods),
            keysTabSectionStyle = safeEnum(p.getString("keysTabSectionStyle", null), d.keysTabSectionStyle),
            keysTabMergeSystemAndMods = p.getBoolean("keysTabMergeSystemAndMods", d.keysTabMergeSystemAndMods),
            keysTabInPlaceReorder = p.getBoolean("keysTabInPlaceReorder", d.keysTabInPlaceReorder),
            keysTabNavArrowsSwapped = p.getBoolean("keysTabNavArrowsSwapped", d.keysTabNavArrowsSwapped),
            keysTabSectionOrder = safeEnumList(p.getString("keysTabSectionOrder", null), d.keysTabSectionOrder),

            navTabShowNavigation = p.getBoolean("navTabShowNavigation", d.navTabShowNavigation),
            navTabShowArrowKeys = p.getBoolean("navTabShowArrowKeys", d.navTabShowArrowKeys),
            navTabShowInsertToggle = p.getBoolean("navTabShowInsertToggle", d.navTabShowInsertToggle),
            navTabShowTypeText = p.getBoolean("navTabShowTypeText", d.navTabShowTypeText),
            navTabShowSystemKeys = p.getBoolean("navTabShowSystemKeys", d.navTabShowSystemKeys),
            navTabShowQuickMods = p.getBoolean("navTabShowQuickMods", d.navTabShowQuickMods),
            navTabSectionStyle = safeEnum(p.getString("navTabSectionStyle", null), d.navTabSectionStyle),
            navTabMergeSystemAndMods = p.getBoolean("navTabMergeSystemAndMods", d.navTabMergeSystemAndMods),
            navTabInPlaceReorder = p.getBoolean("navTabInPlaceReorder", d.navTabInPlaceReorder),
            navTabNavArrowsSwapped = p.getBoolean("navTabNavArrowsSwapped", d.navTabNavArrowsSwapped),
            navTabSectionOrder = safeEnumList(p.getString("navTabSectionOrder", null), d.navTabSectionOrder),

            mediaTabShowNavigation = p.getBoolean("mediaTabShowNavigation", d.mediaTabShowNavigation),
            mediaTabShowArrowKeys = p.getBoolean("mediaTabShowArrowKeys", d.mediaTabShowArrowKeys),
            mediaTabShowSystemKeys = p.getBoolean("mediaTabShowSystemKeys", d.mediaTabShowSystemKeys),
            mediaTabShowQuickMods = p.getBoolean("mediaTabShowQuickMods", d.mediaTabShowQuickMods),
            mediaTabSectionStyle = safeEnum(p.getString("mediaTabSectionStyle", null), d.mediaTabSectionStyle),
            mediaTabMergeSystemAndMods = p.getBoolean("mediaTabMergeSystemAndMods", d.mediaTabMergeSystemAndMods),
            mediaTabInPlaceReorder = p.getBoolean("mediaTabInPlaceReorder", d.mediaTabInPlaceReorder),
            mediaTabSectionOrder = safeEnumList(p.getString("mediaTabSectionOrder", null), d.mediaTabSectionOrder),

            mergeSystemAndModsGlobal = p.getBoolean("mergeSystemAndModsGlobal", d.mergeSystemAndModsGlobal),
            inPlaceReorderGlobal = p.getBoolean("inPlaceReorderGlobal", d.inPlaceReorderGlobal),

            globalOptionalRowVisibility = p.getBoolean("globalOptionalRowVisibility", d.globalOptionalRowVisibility),
            globalShowMediaRow = p.getBoolean("globalShowMediaRow", d.globalShowMediaRow),
            globalShowNavRow = p.getBoolean("globalShowNavRow", d.globalShowNavRow),
            keysTabShowMediaRow = p.getBoolean("keysTabShowMediaRow", d.keysTabShowMediaRow),
            keysTabShowNavRow = p.getBoolean("keysTabShowNavRow", d.keysTabShowNavRow),
            trackpadShowMediaRow = p.getBoolean("trackpadShowMediaRow", d.trackpadShowMediaRow),
            trackpadShowNavRow = p.getBoolean("trackpadShowNavRow", d.trackpadShowNavRow),

            mediaRowShowTransport = p.getBoolean("mediaRowShowTransport", d.mediaRowShowTransport),
            mediaRowShowVolume = p.getBoolean("mediaRowShowVolume", d.mediaRowShowVolume),
            mediaRowShowBrightness = p.getBoolean("mediaRowShowBrightness", d.mediaRowShowBrightness),
            mediaRowRepeatVolume = p.getBoolean("mediaRowRepeatVolume", d.mediaRowRepeatVolume),
            mediaRowRepeatBrightness = p.getBoolean("mediaRowRepeatBrightness", d.mediaRowRepeatBrightness),
            mediaRowGroupOrder = safeEnumList(p.getString("mediaRowGroupOrder", null), d.mediaRowGroupOrder),

            globalOptionalRowOrder = p.getBoolean("globalOptionalRowOrder", d.globalOptionalRowOrder),
            keyboardOptionalRowOrder = safeEnumList(p.getString("keyboardOptionalRowOrder", null), d.keyboardOptionalRowOrder),
            keysTabOptionalRowOrder = safeEnumList(p.getString("keysTabOptionalRowOrder", null), d.keysTabOptionalRowOrder),
            trackpadOptionalRowOrder = safeEnumList(p.getString("trackpadOptionalRowOrder", null), d.trackpadOptionalRowOrder),
        )
    }

    // ── Portrait Trackpad ─────────────────────────────────────────────────
    private fun migratePortraitTrackpad(context: Context): PortraitTrackpadSettings {
        val p = context.getSharedPreferences("trackpad_settings", Context.MODE_PRIVATE)
        val d = PortraitTrackpadSettings()

        return PortraitTrackpadSettings(
            pointerSpeed = p.getFloat("pointer_speed", d.pointerSpeed),
            scrollSpeed = p.getFloat("scroll_speed", d.scrollSpeed),
            invertScroll = p.getBoolean("invert_scroll", d.invertScroll),
            tapToClick = p.getBoolean("tap_to_click", d.tapToClick),
            twoFingerRightClick = p.getBoolean("two_finger_rc", d.twoFingerRightClick),
            accelerationEnabled = p.getBoolean("acceleration", d.accelerationEnabled),
            dragLockMode = p.getBoolean("drag_lock", d.dragLockMode),
            clickPressure = safeEnum(p.getString("click_pressure", null), d.clickPressure),
            scrollPosition = safeEnum(p.getString("scroll_position", null), d.scrollPosition),
            arrowPosition = safeEnum(p.getString("arrow_position", null), d.arrowPosition),
            showArrowKeys = p.getBoolean("show_arrows", d.showArrowKeys),
            showScrollStrip = p.getBoolean("show_scroll", d.showScrollStrip),
            showSystemKeyboard = p.getBoolean("show_system_kb", d.showSystemKeyboard),
            showInAppKeyboard = p.getBoolean("show_inapp_kb", d.showInAppKeyboard),
        )
    }

    // ── Landscape Trackpad ────────────────────────────────────────────────
    private fun migrateLandscapeTrackpad(context: Context): LandscapeTrackpadSettings {
        val p = context.getSharedPreferences("landscape_trackpad_settings", Context.MODE_PRIVATE)
        val d = LandscapeTrackpadSettings()

        return LandscapeTrackpadSettings(
            pointerSpeed = p.getFloat("pointer_speed", d.pointerSpeed),
            scrollSpeed = p.getFloat("scroll_speed", d.scrollSpeed),
            invertScroll = p.getBoolean("invert_scroll", d.invertScroll),
            tapToClick = p.getBoolean("tap_to_click", d.tapToClick),
            twoFingerRightClick = p.getBoolean("two_finger_rc", d.twoFingerRightClick),
            accelerationEnabled = p.getBoolean("acceleration", d.accelerationEnabled),
            dragLockMode = p.getBoolean("drag_lock", d.dragLockMode),
            clickPressure = safeEnum(p.getString("click_pressure", null), d.clickPressure),
            scrollPosition = safeEnum(p.getString("scroll_position", null), d.scrollPosition),
            arrowPosition = safeEnum(p.getString("arrow_position", null), d.arrowPosition),
            showArrowKeys = p.getBoolean("show_arrows", d.showArrowKeys),
            showScrollStrip = p.getBoolean("show_scroll", d.showScrollStrip),
            showSystemKeyboard = p.getBoolean("show_system_kb", d.showSystemKeyboard),
            showInAppKeyboard = p.getBoolean("show_inapp_kb", d.showInAppKeyboard),
            trackpadKbDefaultLayoutMode = safeEnum(p.getString("tp_kb_layout_mode", null), d.trackpadKbDefaultLayoutMode),
            trackpadKbDefaultRightColumn = safeEnum(p.getString("tp_kb_right_column", null), d.trackpadKbDefaultRightColumn),
            trackpadKbShowComboPreview = p.getBoolean("tp_kb_show_combo", d.trackpadKbShowComboPreview),
        )
    }

    // ── Enum helpers ──────────────────────────────────────────────────────
    private inline fun <reified T : Enum<T>> safeEnum(raw: String?, default: T): T {
        if (raw == null) return default
        return try { enumValueOf<T>(raw) } catch (_: Exception) { default }
    }

    private inline fun <reified T : Enum<T>> safeEnumList(raw: String?, default: List<T>): List<T> {
        if (raw.isNullOrBlank()) return default
        return try {
            raw.split(",").map { enumValueOf<T>(it.trim()) }
        } catch (_: Exception) { default }
    }
}