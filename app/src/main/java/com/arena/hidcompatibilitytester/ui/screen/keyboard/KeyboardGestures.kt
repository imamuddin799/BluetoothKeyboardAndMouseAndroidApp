package com.arena.hidcompatibilitytester.ui.screen.keyboard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

suspend fun PointerInputScope.scrollSafeTap(
    scope           : CoroutineScope,
    slopPx          : Float   = 18f,
    repeatMe        : Boolean = false,
    initialDelayMs  : Long    = 400L,
    repeatIntervalMs: Long    = 60L,
    onTap           : () -> Unit,
) {
    awaitPointerEventScope {
        var ev = awaitPointerEvent(PointerEventPass.Initial)
        while (ev.changes.none { it.pressed }) {
            ev = awaitPointerEvent(PointerEventPass.Initial)
        }
        val startPos: Offset = ev.changes.first().position
        var moved = false

        var repeatJob: Job? = null
        if (repeatMe) {
            repeatJob = scope.launch {
                delay(initialDelayMs)
                while (isActive) {
                    if (!moved) onTap()
                    delay(repeatIntervalMs)
                }
            }
        }

        try {
            while (true) {
                ev = awaitPointerEvent(PointerEventPass.Initial)
                val c = ev.changes.firstOrNull() ?: break

                if (!c.pressed) {
                    if (!moved) { c.consume(); onTap() }
                    break
                }

                val dx = abs(c.position.x - startPos.x)
                val dy = abs(c.position.y - startPos.y)
                if (dx > slopPx || dy > slopPx) {
                    moved = true
                    repeatJob?.cancel()
                }
            }
        } finally {
            repeatJob?.cancel()
        }
    }
}

suspend fun PointerInputScope.repeatScrollSafeTap(
    scope           : CoroutineScope,
    slopPx          : Float   = 18f,
    repeatMe        : Boolean = false,
    initialDelayMs  : Long    = 400L,
    repeatIntervalMs: Long    = 60L,
    onTap           : () -> Unit,
) {
    while (true) {
        scrollSafeTap(scope, slopPx, repeatMe, initialDelayMs, repeatIntervalMs, onTap)
    }
}