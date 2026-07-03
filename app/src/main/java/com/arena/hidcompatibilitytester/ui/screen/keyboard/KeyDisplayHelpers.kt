package com.arena.hidcompatibilitytester.ui.screen.keyboard

fun isKeyActive(k: Key, st: KbState): Boolean = when {
    k.isCaps   -> st.caps
    k.isNum    -> st.numLock
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

fun displayMain(k: Key, st: KbState): String {
    if (k.label.isEmpty()) return "Space"
    if (k.isMod || k.isCaps || k.isNum || k.isScroll || k.isConsumer) return k.label
    val isLetter = k.label.length == 1 && k.label[0].isLetter()
    return when {
        isLetter -> if (st.isUpperCase()) k.label.uppercase() else k.label.lowercase()
        st.shift && k.shifted.isNotEmpty() -> k.shifted
        else -> k.label
    }
}

fun displayTop(k: Key, st: KbState, showHints: Boolean = true): String {
    if (!showHints) return ""
    if (k.isMod || k.isCaps || k.isNum || k.isScroll || k.isConsumer) return ""
    if (k.label.length == 1 && k.shifted.isNotEmpty()) return k.shifted
    return ""
}