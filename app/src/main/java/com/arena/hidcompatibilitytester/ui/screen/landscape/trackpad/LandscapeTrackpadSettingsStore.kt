package com.arena.hidcompatibilitytester.ui.screen.landscape.trackpad

import android.content.Context
import android.content.SharedPreferences

object LandscapeTrackpadSettingsStore {
    private const val PREFS = "landscape_trackpad_settings"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(ctx: Context, s: LandscapeTrackpadSettings) {
        prefs(ctx).edit()
            .putFloat("pointer_speed", s.pointerSpeed)
            .putFloat("scroll_speed", s.scrollSpeed)
            .putBoolean("invert_scroll", s.invertScroll)
            .putBoolean("tap_to_click", s.tapToClick)
            .putBoolean("two_finger_rc", s.twoFingerRightClick)
            .putBoolean("acceleration", s.accelerationEnabled)
            .putBoolean("drag_lock", s.dragLockMode)
            .putString("click_pressure", s.clickPressure.name)
            .putString("scroll_position", s.scrollPosition.name)
            .putString("arrow_position", s.arrowPosition.name)
            .putBoolean("show_arrows", s.showArrowKeys)
            .putBoolean("show_scroll", s.showScrollStrip)
            .putBoolean("show_system_kb", s.showSystemKeyboard)
            .putBoolean("show_inapp_kb", s.showInAppKeyboard)
            .commit()
    }

    fun load(ctx: Context): LandscapeTrackpadSettings {
        val p = prefs(ctx)

        fun loadSide(key: String, default: LandscapeSidePosition): LandscapeSidePosition {
            val raw = p.getString(key, default.name) ?: default.name
            return when (raw.uppercase()) {
                "LEFT" -> LandscapeSidePosition.LEFT
                "RIGHT" -> LandscapeSidePosition.RIGHT
                else -> default
            }
        }

        return LandscapeTrackpadSettings(
            pointerSpeed = p.getFloat("pointer_speed", 1.0f),
            scrollSpeed = p.getFloat("scroll_speed", 0.5f),
            invertScroll = p.getBoolean("invert_scroll", false),
            tapToClick = p.getBoolean("tap_to_click", true),
            twoFingerRightClick = p.getBoolean("two_finger_rc", true),
            accelerationEnabled = p.getBoolean("acceleration", true),
            dragLockMode = p.getBoolean("drag_lock", false),
            clickPressure = safeEnum(p.getString("click_pressure", null), LandscapeClickPressure.MEDIUM),
            scrollPosition = loadSide("scroll_position", LandscapeSidePosition.RIGHT),
            arrowPosition = loadSide("arrow_position", LandscapeSidePosition.LEFT),
            showArrowKeys = p.getBoolean("show_arrows", true),
            showScrollStrip = p.getBoolean("show_scroll", true),
            showSystemKeyboard = p.getBoolean("show_system_kb", true),
            showInAppKeyboard = p.getBoolean("show_inapp_kb", true),
        )
    }

    private inline fun <reified T : Enum<T>> safeEnum(v: String?, d: T): T =
        try { if (v != null) enumValueOf<T>(v) else d } catch (_: Exception) { d }
}