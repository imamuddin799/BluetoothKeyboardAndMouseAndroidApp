package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard

const val LANDSCAPE_MOD_LCTRL  = 0x01
const val LANDSCAPE_MOD_LSHIFT = 0x02
const val LANDSCAPE_MOD_LALT   = 0x04
const val LANDSCAPE_MOD_LGUI   = 0x08
const val LANDSCAPE_MOD_RCTRL  = 0x10
const val LANDSCAPE_MOD_RSHIFT = 0x20
const val LANDSCAPE_MOD_RALT   = 0x40
const val LANDSCAPE_MOD_RGUI   = 0x80

data class LandscapeKbState(
    val lCtrl: Boolean = false,
    val rCtrl: Boolean = false,
    val lShift: Boolean = false,
    val rShift: Boolean = false,
    val lAlt: Boolean = false,
    val rAlt: Boolean = false,
    val lGui: Boolean = false,
    val rGui: Boolean = false,
    val caps: Boolean = false,
    val numLock: Boolean = true,
    val scrollLk: Boolean = false,
    val insertMode: Boolean = true,
    val tab: Int = 0,
    val lastKey: String = "",
) {
    val shift get() = lShift || rShift
    val ctrl get() = lCtrl || rCtrl
    val alt get() = lAlt
    val altGr get() = rAlt
    val gui get() = lGui || rGui
    val anyMod get() = ctrl || shift || alt || altGr || gui

    fun isUpperCase() = caps xor shift

    fun modByte(): Int {
        var m = 0
        if (lCtrl) m = m or LANDSCAPE_MOD_LCTRL
        if (rCtrl) m = m or LANDSCAPE_MOD_RCTRL
        if (lShift) m = m or LANDSCAPE_MOD_LSHIFT
        if (rShift) m = m or LANDSCAPE_MOD_RSHIFT
        if (lAlt) m = m or LANDSCAPE_MOD_LALT
        if (rAlt) m = m or LANDSCAPE_MOD_RALT
        if (lGui) m = m or LANDSCAPE_MOD_LGUI
        if (rGui) m = m or LANDSCAPE_MOD_RGUI
        return m
    }

    fun releaseMods() = copy(
        lCtrl = false, rCtrl = false, lShift = false, rShift = false,
        lAlt = false, rAlt = false, lGui = false, rGui = false,
    )

    fun modPrefix() = buildString {
        if (ctrl) append("Ctrl+")
        if (shift) append("Shift+")
        if (alt) append("Alt+")
        if (altGr) append("AltGr+")
        if (gui) append("Win+")
    }

    fun toggleMod(modBit: Int): LandscapeKbState {
        return when (modBit) {
            LANDSCAPE_MOD_LCTRL, LANDSCAPE_MOD_RCTRL -> {
                val newVal = !ctrl
                copy(lCtrl = newVal, rCtrl = newVal)
            }
            LANDSCAPE_MOD_LSHIFT, LANDSCAPE_MOD_RSHIFT -> {
                val newVal = !shift
                copy(lShift = newVal, rShift = newVal)
            }
            LANDSCAPE_MOD_LALT -> copy(lAlt = !lAlt)
            LANDSCAPE_MOD_RALT -> copy(rAlt = !rAlt)
            LANDSCAPE_MOD_LGUI, LANDSCAPE_MOD_RGUI -> {
                val newVal = !gui
                copy(lGui = newVal, rGui = newVal)
            }
            else -> this
        }
    }
}