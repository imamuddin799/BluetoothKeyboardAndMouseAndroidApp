package com.arena.hidcompatibilitytester.ui.screen.keyboard

data class KbState(
    val lCtrl     : Boolean = false,
    val rCtrl     : Boolean = false,
    val lShift    : Boolean = false,
    val rShift    : Boolean = false,
    val lAlt      : Boolean = false,
    val rAlt      : Boolean = false,
    val lGui      : Boolean = false,
    val rGui      : Boolean = false,
    val caps      : Boolean = false,
    val numLock   : Boolean = true,
    val scrollLk  : Boolean = false,
    val insertMode: Boolean = true,
    val tab       : Int     = 0,
    val lastKey   : String  = "",
) {
    val shift  get() = lShift || rShift
    val ctrl   get() = lCtrl  || rCtrl
    val alt    get() = lAlt
    val altGr  get() = rAlt
    val gui    get() = lGui   || rGui
    val anyMod get() = ctrl || shift || alt || altGr || gui

    fun isUpperCase() = caps xor shift

    fun modByte(): Int {
        var m = 0
        if (lCtrl)  m = m or MOD_LCTRL
        if (rCtrl)  m = m or MOD_RCTRL
        if (lShift) m = m or MOD_LSHIFT
        if (rShift) m = m or MOD_RSHIFT
        if (lAlt)   m = m or MOD_LALT
        if (rAlt)   m = m or MOD_RALT
        if (lGui)   m = m or MOD_LGUI
        if (rGui)   m = m or MOD_RGUI
        return m
    }

    fun releaseMods() = copy(
        lCtrl = false, rCtrl = false, lShift = false, rShift = false,
        lAlt  = false, rAlt  = false, lGui   = false, rGui   = false,
    )

    fun modPrefix() = buildString {
        if (ctrl)  append("Ctrl+")
        if (shift) append("Shift+")
        if (alt)   append("Alt+")
        if (altGr) append("AltGr+")
        if (gui)   append("Win+")
    }

    fun toggleMod(modBit: Int): KbState = when (modBit) {
        MOD_LSHIFT -> copy(lShift = !lShift)
        MOD_RSHIFT -> copy(rShift = !rShift)
        MOD_LCTRL  -> copy(lCtrl  = !lCtrl)
        MOD_RCTRL  -> copy(rCtrl  = !rCtrl)
        MOD_LALT   -> copy(lAlt   = !lAlt)
        MOD_RALT   -> copy(rAlt   = !rAlt)
        MOD_LGUI   -> copy(lGui   = !lGui)
        MOD_RGUI   -> copy(rGui   = !rGui)
        else       -> this
    }
}