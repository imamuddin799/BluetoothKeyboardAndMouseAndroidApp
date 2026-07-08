package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard

fun landscapeIsKeyActive(k: LandscapeKey, st: LandscapeKbState): Boolean = when {
    k.isCaps -> st.caps
    k.isNum -> st.numLock
    k.isScroll -> st.scrollLk
    k.modBit == LANDSCAPE_MOD_LSHIFT -> st.lShift
    k.modBit == LANDSCAPE_MOD_RSHIFT -> st.rShift
    k.modBit == LANDSCAPE_MOD_LCTRL -> st.lCtrl
    k.modBit == LANDSCAPE_MOD_RCTRL -> st.rCtrl
    k.modBit == LANDSCAPE_MOD_LALT -> st.lAlt
    k.modBit == LANDSCAPE_MOD_RALT -> st.rAlt
    k.modBit == LANDSCAPE_MOD_LGUI -> st.lGui
    k.modBit == LANDSCAPE_MOD_RGUI -> st.rGui
    k.code == 0x49 && !k.isMod -> !st.insertMode
    else -> false
}

fun landscapeDisplayMain(k: LandscapeKey, st: LandscapeKbState): String {
    if (k.label.isEmpty()) return "Space"
    if (k.isMod || k.isCaps || k.isNum || k.isScroll || k.isConsumer) return k.label
    val isLetter = k.label.length == 1 && k.label[0].isLetter()
    return when {
        isLetter -> if (st.isUpperCase()) k.label.uppercase() else k.label.lowercase()
        st.shift && k.shifted.isNotEmpty() -> k.shifted
        else -> k.label
    }
}

fun landscapeDisplayTop(k: LandscapeKey, st: LandscapeKbState, showHints: Boolean = true): String {
    if (!showHints) return ""
    if (k.isMod || k.isCaps || k.isNum || k.isScroll || k.isConsumer) return ""
    if (k.label.length == 1 && k.shifted.isNotEmpty()) return k.shifted
    return ""
}