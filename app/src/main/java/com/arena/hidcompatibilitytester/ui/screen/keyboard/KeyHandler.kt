package com.arena.hidcompatibilitytester.ui.screen.keyboard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun handleKeyPress(
    key: Key,
    st: KbState,
    settings: KeyboardSettings,
    scope: CoroutineScope,
    onSendKey: (Int, List<Int>) -> Unit,
): KbState {
    return when {
        key.isCaps -> {
            val newSt = st.copy(caps = !st.caps, lastKey = "CapsLk")
            onSendKey(0, listOf(0x39))
            scope.launch { delay(60); onSendKey(0, emptyList()) }
            newSt
        }

        key.isNum -> {
            val newSt = st.copy(numLock = !st.numLock, lastKey = "NumLk")
            onSendKey(0, listOf(0x53))
            scope.launch { delay(60); onSendKey(0, emptyList()) }
            newSt
        }

        key.isScroll -> {
            val newSt = st.copy(scrollLk = !st.scrollLk, lastKey = "ScrLk")
            onSendKey(0, listOf(0x47))
            scope.launch { delay(60); onSendKey(0, emptyList()) }
            newSt
        }

        key.code == 0x49 && !key.isMod -> {
            val newSt = st.copy(insertMode = !st.insertMode, lastKey = "Ins")
            onSendKey(newSt.modByte(), listOf(0x49))
            scope.launch { delay(60); onSendKey(0, emptyList()) }
            newSt.releaseMods()
        }

        key.isMod -> {
            val newSt = st.toggleMod(key.modBit)
            onSendKey(newSt.modByte(), emptyList())
            newSt
        }

        // Tab — modifiers stay held after release
        key.code == 0x2B -> {
            val mod = st.modByte()
            val keepMods = st.anyMod
            val label = st.modPrefix() + "Tab"
            onSendKey(mod, listOf(key.code))
            val newSt = st.copy(lastKey = label)
            scope.launch {
                delay(60)
                if (keepMods) {
                    onSendKey(newSt.modByte(), emptyList())
                } else {
                    onSendKey(0, emptyList())
                }
            }
            newSt
        }

        // All other keys — send then release modifiers
        key.code != 0 -> {
            var mod = st.modByte()
            val isLetter = key.label.length == 1 && key.label[0].isLetter()
            if (isLetter) {
                mod = mod and (MOD_LSHIFT or MOD_RSHIFT).inv()
                if (st.shift) mod = mod or MOD_LSHIFT
            }
            val label = st.modPrefix() + key.label.ifEmpty { "Space" }
            onSendKey(mod, listOf(key.code))
            val newSt = st.copy(lastKey = label)
            scope.launch { delay(60); onSendKey(0, emptyList()) }
            newSt.releaseMods()
        }

        else -> st
    }
}

internal fun handleNumpadKey(
    code: Int,
    label: String,
    st: KbState,
    settings: KeyboardSettings,
    scope: CoroutineScope,
    onSendKey: (Int, List<Int>) -> Unit,
): KbState {
    val newSt = st.copy(lastKey = label)
    onSendKey(st.modByte(), listOf(code))
    scope.launch { delay(60); onSendKey(0, emptyList()) }
    return newSt.releaseMods()
}

internal fun handleNumLockToggle(
    st: KbState,
    scope: CoroutineScope,
    onSendKey: (Int, List<Int>) -> Unit,
): KbState {
    val newSt = st.copy(numLock = !st.numLock, lastKey = "NumLk")
    onSendKey(0, listOf(0x53))
    scope.launch { delay(60); onSendKey(0, emptyList()) }
    return newSt
}