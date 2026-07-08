package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun landscapeHandleKeyPress(
    key: LandscapeKey,
    st: LandscapeKbState,
    settings: LandscapeKeyboardSettings,
    scope: CoroutineScope,
    onSendKey: (Int, List<Int>) -> Unit,
    onDelayedStateUpdate: (LandscapeKbState) -> Unit,
): LandscapeKbState {
    return when {

        key.isCaps -> {
            onSendKey(0, listOf(0x39))
            scope.launch { delay(60); onSendKey(0, emptyList()) }
            st.copy(caps = !st.caps, lastKey = "CapsLk")
        }

        key.isNum -> {
            onSendKey(0, listOf(0x53))
            scope.launch { delay(60); onSendKey(0, emptyList()) }
            st.copy(numLock = !st.numLock, lastKey = "NumLk")
        }

        key.isScroll -> {
            onSendKey(0, listOf(0x47))
            scope.launch { delay(60); onSendKey(0, emptyList()) }
            st.copy(scrollLk = !st.scrollLk, lastKey = "ScrLk")
        }

        key.code == 0x49 && !key.isMod -> {
            val newSt = st.copy(insertMode = !st.insertMode, lastKey = "Ins")
            val mod = newSt.modByte()
            onSendKey(mod, listOf(0x49))
            if (settings.stickyModifiers) {
                scope.launch { delay(60); onSendKey(mod, emptyList()) }
            } else {
                val clearedSt = newSt.releaseMods()
                scope.launch {
                    delay(60)
                    onSendKey(0, emptyList())
                    onDelayedStateUpdate(clearedSt)
                }
            }
            newSt
        }

        key.isMod -> {
            val newSt = st.toggleMod(key.modBit).copy(lastKey = "")
            onSendKey(newSt.modByte(), emptyList())
            newSt
        }

        key.code == 0x2B -> {
            val mod = st.modByte()
            val hadMods = st.anyMod
            onSendKey(mod, listOf(key.code))

            val keepMods = when {
                settings.stickyModifiers -> true
                settings.keepModsAfterTab && hadMods -> true
                else -> false
            }

            val stWithTab = st.copy(lastKey = "Tab")
            val stTabCleared = st.releaseMods().copy(lastKey = "Tab")

            scope.launch {
                delay(60)
                if (keepMods) {
                    onSendKey(mod, emptyList())
                    onDelayedStateUpdate(stWithTab)
                } else {
                    onSendKey(0, emptyList())
                    onDelayedStateUpdate(stTabCleared)
                }
            }

            stWithTab
        }

        key.code != 0 -> {
            var mod = st.modByte()
            val isLetter = key.label.length == 1 && key.label[0].isLetter()
            if (isLetter) {
                mod = mod and (LANDSCAPE_MOD_LSHIFT or LANDSCAPE_MOD_RSHIFT).inv()
                if (st.shift) mod = mod or LANDSCAPE_MOD_LSHIFT
            }
            onSendKey(mod, listOf(key.code))
            val newSt = st.copy(lastKey = key.label.ifEmpty { "Space" })

            if (settings.stickyModifiers) {
                scope.launch { delay(60); onSendKey(mod, emptyList()) }
            } else {
                val clearedSt = newSt.releaseMods()
                scope.launch {
                    delay(60)
                    onSendKey(0, emptyList())
                    onDelayedStateUpdate(clearedSt)
                }
            }
            newSt
        }

        else -> st
    }
}

internal fun landscapeHandleNumpadKey(
    code: Int,
    label: String,
    st: LandscapeKbState,
    settings: LandscapeKeyboardSettings,
    scope: CoroutineScope,
    onSendKey: (Int, List<Int>) -> Unit,
    onDelayedStateUpdate: (LandscapeKbState) -> Unit,
): LandscapeKbState {
    val newSt = st.copy(lastKey = label)
    val mod = st.modByte()
    onSendKey(mod, listOf(code))

    if (settings.stickyModifiers) {
        scope.launch { delay(60); onSendKey(mod, emptyList()) }
    } else {
        val clearedSt = newSt.releaseMods()
        scope.launch {
            delay(60)
            onSendKey(0, emptyList())
            onDelayedStateUpdate(clearedSt)
        }
    }
    return newSt
}

internal fun landscapeHandleNumLockToggle(
    st: LandscapeKbState,
    scope: CoroutineScope,
    onSendKey: (Int, List<Int>) -> Unit,
): LandscapeKbState {
    onSendKey(0, listOf(0x53))
    scope.launch { delay(60); onSendKey(0, emptyList()) }
    return st.copy(numLock = !st.numLock, lastKey = "NumLk")
}