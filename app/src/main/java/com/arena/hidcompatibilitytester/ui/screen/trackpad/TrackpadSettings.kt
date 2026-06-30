package com.arena.hidcompatibilitytester.ui.screen.trackpad

import android.content.Context
import android.content.SharedPreferences

data class TrackpadSettings(
    val pointerSpeed        : Float         = 1.2f,
    val scrollSpeed         : Float         = 1.0f,
    val invertScroll        : Boolean       = false,
    val tapToClick          : Boolean       = true,
    val twoFingerRightClick : Boolean       = true,
    val accelerationEnabled : Boolean       = true,
    val dragLockMode        : Boolean       = false,
    val clickPressure       : ClickPressure = ClickPressure.MEDIUM,
    val scrollPosition      : SidePosition  = SidePosition.RIGHT,
    val arrowPosition       : SidePosition  = SidePosition.LEFT,
    val showArrowKeys       : Boolean       = true,
    val showSystemKeyboard  : Boolean       = true,
    val showInAppKeyboard   : Boolean       = true,
)

enum class ClickPressure { LIGHT, MEDIUM, FIRM }
enum class SidePosition { LEFT, RIGHT }

object TrackpadSettingsStore {
    private const val PREFS = "trackpad_settings"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(ctx: Context, s: TrackpadSettings) {
        prefs(ctx).edit().apply {
            putFloat("pointer_speed", s.pointerSpeed)
            putFloat("scroll_speed", s.scrollSpeed)
            putBoolean("invert_scroll", s.invertScroll)
            putBoolean("tap_to_click", s.tapToClick)
            putBoolean("two_finger_rc", s.twoFingerRightClick)
            putBoolean("acceleration", s.accelerationEnabled)
            putBoolean("drag_lock", s.dragLockMode)
            putString("click_pressure", s.clickPressure.name)
            putString("scroll_position", s.scrollPosition.name)
            putString("arrow_position", s.arrowPosition.name)
            putBoolean("show_arrows", s.showArrowKeys)
            putBoolean("show_system_kb", s.showSystemKeyboard)
            putBoolean("show_inapp_kb", s.showInAppKeyboard)
            apply()
        }
    }

    fun load(ctx: Context): TrackpadSettings {
        val p = prefs(ctx)
        return TrackpadSettings(
            pointerSpeed        = p.getFloat("pointer_speed", 1.2f),
            scrollSpeed         = p.getFloat("scroll_speed", 1.0f),
            invertScroll        = p.getBoolean("invert_scroll", false),
            tapToClick          = p.getBoolean("tap_to_click", true),
            twoFingerRightClick = p.getBoolean("two_finger_rc", true),
            accelerationEnabled = p.getBoolean("acceleration", true),
            dragLockMode        = p.getBoolean("drag_lock", false),
            clickPressure       = safeEnum(p.getString("click_pressure", null), ClickPressure.MEDIUM),
            scrollPosition      = safeEnum(p.getString("scroll_position", null), SidePosition.RIGHT),
            arrowPosition       = safeEnum(p.getString("arrow_position", null), SidePosition.LEFT),
            showArrowKeys       = p.getBoolean("show_arrows", true),
            showSystemKeyboard  = p.getBoolean("show_system_kb", true),
            showInAppKeyboard   = p.getBoolean("show_inapp_kb", true),
        )
    }

    private inline fun <reified T : Enum<T>> safeEnum(v: String?, d: T): T =
        try { if (v != null) enumValueOf<T>(v) else d } catch (_: Exception) { d }
}