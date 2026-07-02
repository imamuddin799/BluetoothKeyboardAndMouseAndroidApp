package com.arena.hidcompatibilitytester.ui.screen.keyboard

internal data class KbState(
    val ctrl      : Boolean = false,
    val shift     : Boolean = false,
    val alt       : Boolean = false,
    val altGr     : Boolean = false,
    val gui       : Boolean = false,
    val caps      : Boolean = false,
    val numLock   : Boolean = true,
    val scrollLk  : Boolean = false,
    val insertMode: Boolean = true,
    val tab       : Int     = 0,
    val lastKey   : String  = "",
) {
    val anyMod get() = ctrl || shift || alt || altGr || gui

    fun isUpperCase() = caps xor shift

    fun modByte(): Int {
        var m = 0
        if (ctrl)  m = m or MOD_LCTRL
        if (shift) m = m or MOD_LSHIFT
        if (alt)   m = m or MOD_LALT
        if (altGr) m = m or MOD_RALT
        if (gui)   m = m or MOD_LGUI
        return m
    }

    fun releaseMods() = copy(
        ctrl = false, shift = false, alt = false, altGr = false, gui = false,
    )

    fun modPrefix() = buildString {
        if (ctrl)  append("Ctrl+")
        if (shift) append("Shift+")
        if (alt)   append("Alt+")
        if (altGr) append("AltGr+")
        if (gui)   append("Win+")
    }

    fun toggleMod(modBit: Int): KbState = when (modBit) {
        MOD_LSHIFT, MOD_RSHIFT -> copy(shift = !shift)
        MOD_LCTRL, MOD_RCTRL  -> copy(ctrl = !ctrl)
        MOD_LALT               -> copy(alt = !alt)
        MOD_RALT               -> copy(altGr = !altGr)
        MOD_LGUI, MOD_RGUI    -> copy(gui = !gui)
        else                   -> this
    }
}