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
    onDelayedStateUpdate: (KbState) -> Unit,
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

        // ── Tab ───────────────────────────────────────────────────────────
        // Sticky ON               → keep mods after Tab (always)
        // Sticky OFF + KMaT ON   → keep mods after Tab (only when mods were held)
        // Sticky OFF + KMaT OFF  → release mods + Tab
        key.code == 0x2B -> {
            val mod = st.modByte()
            val hadMods = st.anyMod
            onSendKey(mod, listOf(key.code))

            val keepMods = when {
                settings.stickyModifiers             -> true
                settings.keepModsAfterTab && hadMods -> true
                else                                 -> false
            }

            val stWithTab    = st.copy(lastKey = "Tab")
            val stTabCleared = st.releaseMods().copy(lastKey = "Tab")

            scope.launch {
                delay(60)
                if (keepMods) {
                    onSendKey(mod, emptyList())
                    // Force Compose to re-affirm mods-held state in case anything
                    // raced and cleared it between Tab press and this coroutine
                    onDelayedStateUpdate(stWithTab)
                } else {
                    onSendKey(0, emptyList())
                    onDelayedStateUpdate(stTabCleared)
                }
            }

            stWithTab
        }

        // ── All other keys ────────────────────────────────────────────────
        // Sticky ON  → keep mods held after key
        // Sticky OFF → release mods (keepModsAfterTab has no effect here)
        key.code != 0 -> {
            var mod = st.modByte()
            val isLetter = key.label.length == 1 && key.label[0].isLetter()
            if (isLetter) {
                mod = mod and (MOD_LSHIFT or MOD_RSHIFT).inv()
                if (st.shift) mod = mod or MOD_LSHIFT
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

internal fun handleNumpadKey(
    code: Int,
    label: String,
    st: KbState,
    settings: KeyboardSettings,
    scope: CoroutineScope,
    onSendKey: (Int, List<Int>) -> Unit,
    onDelayedStateUpdate: (KbState) -> Unit,
): KbState {
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

internal fun handleNumLockToggle(
    st: KbState,
    scope: CoroutineScope,
    onSendKey: (Int, List<Int>) -> Unit,
): KbState {
    onSendKey(0, listOf(0x53))
    scope.launch { delay(60); onSendKey(0, emptyList()) }
    return st.copy(numLock = !st.numLock, lastKey = "NumLk")
}