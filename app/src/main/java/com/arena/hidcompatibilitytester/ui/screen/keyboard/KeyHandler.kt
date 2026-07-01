// ui/screen/keyboard/KeyHandler.kt
package com.arena.hidcompatibilitytester.ui.screen.keyboard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Centralized key press handler — returns the new KbState.
 */
internal fun handleKeyPress(
    key: Key,
    st: KbState,
    settings: KeyboardSettings,
    scope: CoroutineScope,
    onSendKey: (Int, List<Int>) -> Unit,
): KbState {
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

        key.isFn -> st.copy(fn = !st.fn)

        key.code == 0x49 && !key.isMod -> {
            val newSt = st.copy(insertMode = !st.insertMode, lastKey = "Ins")
            onSendKey(newSt.modByte(), listOf(0x49))
            scope.launch {
                delay(60)
                onSendKey(0, emptyList())
            }
            if (settings.autoReleaseModsAfterKey) newSt.releaseMods() else newSt
        }

        key.isMod -> {
            val newSt = st.toggleMod(key.modBit)
            onSendKey(newSt.modByte(), emptyList())
            newSt
        }

        key.code == 0x2B -> { // Tab
            val mod = st.modByte()
            val keepMods = st.anyMod || settings.stickyModifiers
            val label = st.modPrefix() + key.label.ifEmpty { "Space" }
            onSendKey(mod, listOf(key.code))
            scope.launch {
                delay(60)
                if (keepMods) onSendKey(st.modByte(), emptyList())
                else onSendKey(0, emptyList())
            }
            val newSt = st.copy(lastKey = label)
            if (!keepMods && settings.autoReleaseModsAfterKey) newSt.releaseMods() else newSt
        }

        key.code != 0 -> {
            var mod = st.modByte()
            val isLetter = key.label.length == 1 && key.label[0].isLetter()
            if (isLetter) {
                mod = mod and (MOD_LSHIFT or MOD_RSHIFT).inv()
                if (st.shift) mod = mod or MOD_LSHIFT
            }
            val label = st.modPrefix() + key.label.ifEmpty { "Space" }
            onSendKey(mod, listOf(key.code))
            scope.launch {
                delay(60)
                onSendKey(0, emptyList())
            }
            val newSt = st.copy(lastKey = label)
            if (settings.autoReleaseModsAfterKey && !settings.stickyModifiers) {
                newSt.releaseMods()
            } else newSt
        }

        else -> st
    }
}

/**
 * Handle numpad key press.
 */
internal fun handleNumpadKey(
    code: Int,
    label: String,
    st: KbState,
    settings: KeyboardSettings,
    scope: CoroutineScope,
    onSendKey: (Int, List<Int>) -> Unit,
): KbState {
    onSendKey(st.modByte(), listOf(code))
    scope.launch { delay(60); onSendKey(0, emptyList()) }
    val newSt = st.copy(lastKey = label)
    return if (settings.autoReleaseModsAfterKey && !settings.stickyModifiers) {
        newSt.releaseMods()
    } else newSt
}

/**
 * Handle NumLock toggle.
 */
internal fun handleNumLockToggle(
    st: KbState,
    scope: CoroutineScope,
    onSendKey: (Int, List<Int>) -> Unit,
): KbState {
    onSendKey(0, listOf(0x53))
    scope.launch { delay(60); onSendKey(0, emptyList()) }
    return st.copy(numLock = !st.numLock, lastKey = "NumLk")
}