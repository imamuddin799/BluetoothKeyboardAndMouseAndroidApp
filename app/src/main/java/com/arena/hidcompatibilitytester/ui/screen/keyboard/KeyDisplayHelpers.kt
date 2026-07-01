// ui/screen/keyboard/KeyDisplayHelpers.kt
package com.arena.hidcompatibilitytester.ui.screen.keyboard

internal fun isKeyActive(k: Key, st: KbState): Boolean = when {
    k.isCaps   -> st.caps
    k.isNum    -> st.numLock
    k.isFn     -> st.fn
    k.isScroll -> st.scrollLk
    k.modBit == MOD_LSHIFT -> st.lShift
    k.modBit == MOD_RSHIFT -> st.rShift
    k.modBit == MOD_LCTRL  -> st.lCtrl
    k.modBit == MOD_RCTRL  -> st.rCtrl
    k.modBit == MOD_LALT   -> st.lAlt
    k.modBit == MOD_RALT   -> st.rAlt
    k.modBit == MOD_LGUI   -> st.lGui
    k.modBit == MOD_RGUI   -> st.rGui
    k.code == 0x49 && !k.isMod -> !st.insertMode
    else       -> false
}

internal fun displayMain(k: Key, st: KbState): String {
    if (k.label.isEmpty()) return "Space"
    if (k.isMod || k.isCaps || k.isFn || k.isNum || k.isScroll) return k.label
    val isLetter = k.label.length == 1 && k.label[0].isLetter()
    return when {
        st.altGr && k.altGr.isNotEmpty() -> k.altGr
        isLetter -> if (st.isUpperCase()) k.label.uppercase() else k.label.lowercase()
        st.shift && k.shift.isNotEmpty() -> k.shift
        else -> k.label
    }
}

internal fun displayTop(k: Key, st: KbState, showHints: Boolean = true): String {
    if (!showHints) return ""
    if (k.isMod || k.isCaps || k.isFn || k.isNum || k.isScroll) return ""
    if (k.label.length == 1 && k.shift.isNotEmpty()) return k.shift
    return ""
}