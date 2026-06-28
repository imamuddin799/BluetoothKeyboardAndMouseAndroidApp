// TrackpadScreen.kt
package com.arena.hidcompatibilitytester

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

// ═════════════════════════════════════════════════════════════════════════════
// Data classes / enums
// ═════════════════════════════════════════════════════════════════════════════

data class TrackpadSettings(
    val pointerSpeed        : Float         = 1.2f,
    val scrollSpeed         : Float         = 1.0f,
    val invertScroll        : Boolean       = false,   // true = natural / phone-style
    val tapToClick          : Boolean       = true,
    val twoFingerRightClick : Boolean       = true,
    val accelerationEnabled : Boolean       = true,
    val clickPressure       : ClickPressure = ClickPressure.MEDIUM
)

enum class ClickPressure { LIGHT, MEDIUM, FIRM }

// ═════════════════════════════════════════════════════════════════════════════
// TrackpadScreen
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun TrackpadScreen(
    isReady       : Boolean,
    settings      : TrackpadSettings,
    onSendMouse   : (dx: Int, dy: Int, buttons: Int, wheel: Int) -> Unit,
    onShowSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080F18))
    ) {
        TrackpadStatusBar(
            isReady        = isReady,
            settings       = settings,
            onShowSettings = onShowSettings
        )

        if (!isReady) {
            TrackpadNotReadyCard()
            return
        }

        TrackpadSurface(
            modifier    = Modifier.weight(1f),
            isReady     = isReady,
            settings    = settings,
            onSendMouse = onSendMouse
        )

        TrackpadClickButtons(onSendMouse = onSendMouse)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Status bar
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun TrackpadStatusBar(
    isReady        : Boolean,
    settings       : TrackpadSettings,
    onShowSettings : () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF050C14))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            LedBadge("TPD", isReady)
            LedBadge("TAP", settings.tapToClick)
            LedBadge("INV", settings.invertScroll)
            LedBadge("ACC", settings.accelerationEnabled)
            LedBadge("2FC", settings.twoFingerRightClick)
            Spacer(Modifier.weight(1f))
            Text(
                if (isReady) "● Ready" else "○ Offline",
                fontSize = 9.sp,
                color    = if (isReady) Color(0xFF81C784) else Color(0xFFEF9A9A)
            )
            IconButton(onClick = onShowSettings, modifier = Modifier.size(28.dp)) {
                Text("⚙", fontSize = 14.sp, color = Color(0xFF607D8B))
            }
        }
        Surface(
            color    = Color(0xFF0A1828),
            shape    = RoundedCornerShape(5.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "🖱 Trackpad · ${settings.pointerSpeed}x" +
                        (if (settings.tapToClick) " · tap✓" else "") +
                        (if (settings.invertScroll) " · inv✓" else ""),
                fontSize   = 11.sp,
                color      = Color(0xFF90CAF9),
                fontWeight = FontWeight.Medium,
                modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// TrackpadSurface
//
// KEY FIXES:
//  1. onSendMouse wrapped in a lambdaRef (rememberUpdatedState) so the
//     pointerInput block always calls the LATEST lambda without restarting.
//  2. settings also via rememberUpdatedState — same reason.
//  3. pointerInput keyed on Unit (runs once, never restarts) — stale-closure
//     problem is solved by the rememberUpdatedState refs above.
//  4. isReady passed so we can guard sends.
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun TrackpadSurface(
    modifier   : Modifier = Modifier,
    isReady    : Boolean,
    settings   : TrackpadSettings,
    onSendMouse: (dx: Int, dy: Int, buttons: Int, wheel: Int) -> Unit,
) {
    val haptic  = LocalHapticFeedback.current
    val scope   = rememberCoroutineScope()
    val density = LocalDensity.current

    // ── Always-current refs — pointerInput block reads these, never goes stale ─
    val latestSend     = rememberUpdatedState(onSendMouse)
    val latestSettings = rememberUpdatedState(settings)
    val latestReady    = rememberUpdatedState(isReady)

    // Tap thresholds
    val TAP_MAX_MS  = 200L
    val TAP_SLOP_PX = with(density) { 18.dp.toPx() }
    val DTAP_GAP_MS = 350L

    // Sub-pixel accumulators — plain arrays, no recompose on write
    val accX = remember { floatArrayOf(0f) }
    val accY = remember { floatArrayOf(0f) }
    val scrA = remember { floatArrayOf(0f) }

    // UI state (only what Canvas / overlays need)
    var cursorPos      by remember { mutableStateOf<Offset?>(null) }
    var fingerCount    by remember { mutableIntStateOf(0) }
    var rightFlash     by remember { mutableStateOf(false) }
    var scrollFlash    by remember { mutableStateOf(false) }
    var dragLockActive by remember { mutableStateOf(false) }
    var scrollFlashJob by remember { mutableStateOf<Job?>(null) }

    // Ripple animation
    val ripplePos    = remember { mutableStateOf<Offset?>(null) }
    val rippleRadius = remember { Animatable(0f) }
    val rippleAlpha  = remember { Animatable(0f) }

    // Double-tap tracking — plain arrays (no recompose needed)
    val lastTapUpMs    = remember { longArrayOf(0L) }
    val doubleTapArmed = remember { booleanArrayOf(false) }

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun send(dx: Int, dy: Int, btn: Int, wheel: Int) {
        if (latestReady.value) latestSend.value(dx, dy, btn, wheel)
    }

    fun fireRipple(pos: Offset) = scope.launch {
        ripplePos.value = pos
        rippleRadius.snapTo(0f)
        rippleAlpha.snapTo(0.8f)
        rippleRadius.animateTo(110f, tween(320, easing = FastOutSlowInEasing))
        rippleAlpha.animateTo(0f, tween(200))
        ripplePos.value = null
    }

    fun flashScroll() {
        scrollFlash = true
        scrollFlashJob?.cancel()
        scrollFlashJob = scope.launch { delay(400); scrollFlash = false }
    }

    fun releaseDragLock() {
        if (dragLockActive) {
            dragLockActive = false
            send(0, 0, 0, 0)
        }
    }

    fun applyAccel(raw: Float, speed: Float): Float {
        val s = latestSettings.value
        if (!s.accelerationEnabled) return raw * speed
        val a = abs(raw)
        val k = when {
            a < 1f  -> 0.5f
            a < 3f  -> 0.9f
            a < 7f  -> 1.5f
            a < 12f -> 2.0f
            else    -> 2.6f
        }
        return raw * k * speed
    }

    fun resetAcc() { accX[0] = 0f; accY[0] = 0f; scrA[0] = 0f }

    // ── Layout ────────────────────────────────────────────────────────────────

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0D2B45))
    ) {

        // ── Main touch surface ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 40.dp)
                // KEY: keyed on Unit so the block is created ONCE and never
                // restarted. Stale-closure is avoided via latestSend /
                // latestSettings / latestReady above.
                .pointerInput(Unit) {
                    val pts       = LinkedHashMap<Long, FloatArray>(8)
                    var prevCount = 0

                    awaitPointerEventScope {
                        while (true) {
                            val evt = awaitPointerEvent(PointerEventPass.Initial)
                            val now = System.currentTimeMillis()
                            val s   = latestSettings.value   // snapshot for this frame

                            var gotNewFinger = false

                            // ── 1. Register / update pointers ─────────────────
                            for (ch in evt.changes) {
                                val id = ch.id.value
                                if (ch.pressed) {
                                    val existing = pts[id]
                                    if (existing == null) {
                                        gotNewFinger = true
                                        val x = ch.position.x; val y = ch.position.y
                                        pts[id] = floatArrayOf(
                                            x, y,               // [0,1] down pos
                                            x, y,               // [2,3] prev pos
                                            x, y,               // [4,5] cur  pos
                                            now.toFloat(), 0f,  // [6] downTime [7] maxMove
                                            x, y                // [8,9] lift pos
                                        )
                                    } else {
                                        existing[2] = existing[4]
                                        existing[3] = existing[5]
                                        existing[4] = ch.position.x
                                        existing[5] = ch.position.y
                                        val dx = existing[4] - existing[0]
                                        val dy = existing[5] - existing[1]
                                        val d  = hypot(dx, dy)
                                        if (d > existing[7]) existing[7] = d
                                    }
                                    ch.consume()
                                } else if (ch.previousPressed) {
                                    // Finger lifting — record lift pos
                                    pts[id]?.let { fp ->
                                        fp[8] = ch.position.x
                                        fp[9] = ch.position.y
                                        val dx = fp[8] - fp[0]; val dy = fp[9] - fp[1]
                                        val d  = hypot(dx, dy)
                                        if (d > fp[7]) fp[7] = d
                                    }
                                    ch.consume()
                                }
                            }

                            val curCount = pts.size
                            val all      = pts.values.toList()

                            // ── 2. Reset acc on finger-count change ────────────
                            if (curCount != prevCount) {
                                resetAcc()
                                prevCount = curCount
                            }

                            fingerCount = curCount
                            cursorPos   = all.firstOrNull()?.let { Offset(it[4], it[5]) }

                            // ── 3. Double-tap-drag: detect 2nd finger-down ─────
                            if (gotNewFinger && curCount == 1 &&
                                s.tapToClick && doubleTapArmed[0]
                            ) {
                                val gap = now - lastTapUpMs[0]
                                if (gap in 30L..DTAP_GAP_MS) {
                                    doubleTapArmed[0] = false
                                    dragLockActive    = true
                                    send(0, 0, 1, 0)        // hold left button
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    all.firstOrNull()?.let { fireRipple(Offset(it[4], it[5])) }
                                } else {
                                    doubleTapArmed[0] = false
                                }
                            }

                            // ── 4. Movement ────────────────────────────────────
                            when (curCount) {
                                1 -> {
                                    val fp  = all[0]
                                    val rdx = fp[4] - fp[2]
                                    val rdy = fp[5] - fp[3]
                                    val btn = if (dragLockActive) 1 else 0
                                    accX[0] += applyAccel(rdx, s.pointerSpeed)
                                    accY[0] += applyAccel(rdy, s.pointerSpeed)
                                    val ix = accX[0].toInt()
                                    val iy = accY[0].toInt()
                                    if (ix != 0 || iy != 0) {
                                        send(ix.coerceIn(-127, 127), iy.coerceIn(-127, 127), btn, 0)
                                        accX[0] -= ix; accY[0] -= iy
                                    }
                                }
                                2 -> {
                                    if (dragLockActive) releaseDragLock()
                                    val dy0   = all[0][5] - all[0][3]
                                    val dy1   = all[1][5] - all[1][3]
                                    val avgDy = (dy0 + dy1) * 0.5f
                                    // invertScroll = natural (finger direction = content direction)
                                    val dir   = if (s.invertScroll) 1f else -1f
                                    scrA[0]  += avgDy * s.scrollSpeed * dir * 0.6f
                                    val sw    = scrA[0].toInt()
                                    if (sw != 0) {
                                        send(0, 0, 0, sw.coerceIn(-127, 127))
                                        scrA[0] -= sw
                                        flashScroll()
                                    }
                                }
                                3 -> {
                                    if (dragLockActive) releaseDragLock()
                                    val fp  = all[0]
                                    val rdx = fp[4] - fp[2]
                                    val rdy = fp[5] - fp[3]
                                    accX[0] += rdx * s.pointerSpeed * 1.8f
                                    accY[0] += rdy * s.pointerSpeed * 1.8f
                                    val ix  = accX[0].toInt()
                                    val iy  = accY[0].toInt()
                                    if (ix != 0 || iy != 0) {
                                        send(ix.coerceIn(-127, 127), iy.coerceIn(-127, 127), 0, 0)
                                        accX[0] -= ix; accY[0] -= iy
                                    }
                                }
                            }

                            // ── 5. Handle releases ─────────────────────────────
                            val released = evt.changes.filter { !it.pressed && it.previousPressed }
                            if (released.isNotEmpty()) {
                                val totalDown = pts.size   // before removal

                                for (ch in released) {
                                    val id = ch.id.value
                                    val fp = pts[id] ?: continue

                                    val duration = now - fp[6].toLong()
                                    val maxMove  = fp[7]
                                    val liftPos  = Offset(fp[8], fp[9])
                                    val isTap    = duration in 1L..TAP_MAX_MS &&
                                                   maxMove < TAP_SLOP_PX

                                    if (isTap) {
                                        when {
                                            // 1-finger tap → left click
                                            totalDown == 1 && s.tapToClick -> {
                                                if (dragLockActive) {
                                                    releaseDragLock()
                                                    haptic.performHapticFeedback(
                                                        HapticFeedbackType.LongPress)
                                                    fireRipple(liftPos)
                                                } else {
                                                    send(0, 0, 1, 0)
                                                    scope.launch {
                                                        delay(40)
                                                        send(0, 0, 0, 0)
                                                    }
                                                    haptic.performHapticFeedback(
                                                        HapticFeedbackType.LongPress)
                                                    fireRipple(liftPos)
                                                    lastTapUpMs[0]    = now
                                                    doubleTapArmed[0] = true
                                                    scope.launch {
                                                        delay(DTAP_GAP_MS + 30)
                                                        doubleTapArmed[0] = false
                                                    }
                                                }
                                            }
                                            // 2-finger tap → right click
                                            totalDown >= 2 && s.twoFingerRightClick -> {
                                                if (dragLockActive) releaseDragLock()
                                                doubleTapArmed[0] = false
                                                send(0, 0, 2, 0)
                                                scope.launch { delay(50); send(0, 0, 0, 0) }
                                                haptic.performHapticFeedback(
                                                    HapticFeedbackType.LongPress)
                                                rightFlash = true
                                                scope.launch { delay(180); rightFlash = false }
                                                fireRipple(liftPos)
                                            }
                                        }
                                    } else {
                                        // Non-tap lift
                                        doubleTapArmed[0] = false
                                    }

                                    pts.remove(id)
                                }

                                if (pts.isEmpty()) {
                                    resetAcc()
                                    fingerCount = 0
                                    cursorPos   = null
                                    prevCount   = 0
                                    // dragLockActive intentionally NOT cleared —
                                    // user can re-place finger to continue drag
                                }
                            }
                        }
                    }
                }
        ) {
            // ── Canvas overlays ───────────────────────────────────────────────
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawTrackpadGrid()
                if (rightFlash) drawRect(Color(0xFF4A148C).copy(alpha = 0.20f))
                if (dragLockActive) {
                    drawRect(Color(0xFF1565C0).copy(alpha = 0.08f))
                    drawRoundRect(
                        color        = Color(0xFF4A90D9).copy(alpha = 0.65f),
                        topLeft      = Offset(3f, 3f),
                        size         = Size(size.width - 6f, size.height - 6f),
                        cornerRadius = CornerRadius(12f),
                        style        = Stroke(3f)
                    )
                }
                ripplePos.value?.let { drawRipple(it, rippleRadius.value, rippleAlpha.value) }
                cursorPos?.let { drawCursorGhost(it) }
                drawMouseIcon(size)
            }

            if (dragLockActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .background(Color(0xFF1565C0).copy(0.92f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(
                        "⬚ Drag Lock · tap to release",
                        fontSize   = 11.sp,
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (fingerCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .background(Color.Black.copy(0.50f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                ) {
                    Text(
                        "$fingerCount finger${if (fingerCount > 1) "s" else ""}",
                        fontSize = 10.sp,
                        color    = Color.White.copy(0.75f)
                    )
                }
            }
        }

        // ── Scroll strip ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(40.dp)
                .fillMaxHeight()
                .background(Color(0xFF081929))
        ) {
            Column(
                modifier            = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                ScrollArrowButton("▲") { send(0, 0, 0, 3) }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0A1F33))
                        .pointerInput(Unit) {
                            var lastY      = 0f
                            var firstEvent = true
                            awaitPointerEventScope {
                                while (true) {
                                    val evt = awaitPointerEvent(PointerEventPass.Initial)
                                    val ch  = evt.changes.firstOrNull() ?: continue
                                    val s   = latestSettings.value
                                    if (ch.pressed) {
                                        if (firstEvent) {
                                            lastY      = ch.position.y
                                            firstEvent = false
                                        } else {
                                            val dy    = ch.position.y - lastY
                                            val dir   = if (s.invertScroll) 1f else -1f
                                            val delta = (dy * s.scrollSpeed * dir * 0.5f).toInt()
                                            if (delta != 0) {
                                                send(0, 0, 0, delta.coerceIn(-127, 127))
                                                flashScroll()
                                            }
                                            lastY = ch.position.y
                                        }
                                        ch.consume()
                                    } else {
                                        firstEvent = true
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx    = size.width / 2f
                        val alpha = if (scrollFlash) 0.55f else 0.13f
                        val color = Color.White.copy(alpha = alpha)
                        var y = 10f
                        while (y < size.height - 10f) {
                            drawLine(
                                color, Offset(cx, y),
                                Offset(cx, (y + 7f).coerceAtMost(size.height - 10f)), 2.5f
                            )
                            y += 13f
                        }
                        if (scrollFlash) {
                            drawRoundRect(
                                Color(0xFF4A90D9).copy(0.45f),
                                Offset(5f, 5f),
                                Size(size.width - 10f, size.height - 10f),
                                CornerRadius(6f)
                            )
                        }
                    }
                    Text("↕", fontSize = 14.sp,
                        color = Color.White.copy(if (scrollFlash) 0.9f else 0.28f))
                }

                ScrollArrowButton("▼") { send(0, 0, 0, -3) }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Click buttons
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun TrackpadClickButtons(
    onSendMouse: (dx: Int, dy: Int, buttons: Int, wheel: Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val scope  = rememberCoroutineScope()
    // Always-current ref so buttons work immediately after connect
    val latestSend = rememberUpdatedState(onSendMouse)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0xFF081929))
    ) {
        ClickZoneButton("Left", Modifier.weight(1f), Color(0xFF1565C0)) {
            latestSend.value(0, 0, 1, 0)
            scope.launch { delay(80); latestSend.value(0, 0, 0, 0) }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(0.1f)))
        ClickZoneButton("Mid", Modifier.weight(0.6f), Color(0xFF1B5E20)) {
            latestSend.value(0, 0, 4, 0)
            scope.launch { delay(80); latestSend.value(0, 0, 0, 0) }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(0.1f)))
        ClickZoneButton("Right", Modifier.weight(1f), Color(0xFF4A148C)) {
            latestSend.value(0, 0, 2, 0)
            scope.launch { delay(80); latestSend.value(0, 0, 0, 0) }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Scroll arrow (hold-to-repeat)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun ScrollArrowButton(label: String, onScroll: () -> Unit) {
    val scope      = rememberCoroutineScope()
    val latestScroll = rememberUpdatedState(onScroll)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color(0xFF0F2840))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    latestScroll.value()
                    val job = scope.launch {
                        delay(380)
                        while (isActive) { latestScroll.value(); delay(75) }
                    }
                    tryAwaitRelease()
                    job.cancel()
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White.copy(0.55f), fontSize = 13.sp)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Click zone button
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun ClickZoneButton(
    label   : String,
    modifier: Modifier,
    tint    : Color,
    onClick : () -> Unit,
) {
    var pressed    by remember { mutableStateOf(false) }
    val latestClick = rememberUpdatedState(onClick)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (pressed) tint.copy(0.5f) else tint.copy(0.15f))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true
                    latestClick.value()
                    tryAwaitRelease()
                    pressed = false
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color      = Color.White.copy(0.9f),
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign  = TextAlign.Center
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Canvas helpers
// ═════════════════════════════════════════════════════════════════════════════

private fun DrawScope.drawTrackpadGrid() {
    val c    = Color.White.copy(alpha = 0.020f)
    val step = 44.dp.toPx()
    var x = 0f
    while (x <= size.width)  { drawLine(c, Offset(x, 0f), Offset(x, size.height), 1f); x += step }
    var y = 0f
    while (y <= size.height) { drawLine(c, Offset(0f, y), Offset(size.width, y),  1f); y += step }
}

private fun DrawScope.drawRipple(center: Offset, radius: Float, alpha: Float) {
    drawCircle(Color.White.copy(alpha * 0.30f), radius,         center, style = Stroke(2.dp.toPx()))
    drawCircle(Color.White.copy(alpha * 0.08f), radius * 0.40f, center)
}

private fun DrawScope.drawCursorGhost(pos: Offset) {
    drawCircle(Color.White.copy(0.22f), 14.dp.toPx(), pos, style = Stroke(1.5.dp.toPx()))
    drawCircle(Color.White.copy(0.08f), 5.dp.toPx(),  pos)
}

private fun DrawScope.drawMouseIcon(canvasSize: Size) {
    val cx = canvasSize.width / 2f
    val cy = canvasSize.height / 2f - 20.dp.toPx()
    val w  = 30.dp.toPx(); val h = 44.dp.toPx(); val r = w / 2f
    val sc = Color.White.copy(0.13f); val sw = 1.5.dp.toPx()
    drawRoundRect(sc, Offset(cx - r, cy - h / 2f), Size(w, h), CornerRadius(r), Stroke(sw))
    drawLine(sc, Offset(cx, cy - h / 2f), Offset(cx, cy - h / 2f + h * 0.38f), sw)
    drawLine(sc,
        Offset(cx - r + 2f, cy - h / 2f + h * 0.38f),
        Offset(cx + r - 2f, cy - h / 2f + h * 0.38f), sw)
    val wTop = cy - h / 2f + h * 0.07f
    val ww   = 4.dp.toPx()
    drawRoundRect(sc, Offset(cx - ww / 2f, wTop), Size(ww, h * 0.23f), CornerRadius(ww / 2f), Stroke(sw))
}

// ═════════════════════════════════════════════════════════════════════════════
// Settings sheet
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackpadSettingsSheet(
    settings : TrackpadSettings,
    onDismiss: () -> Unit,
    onSave   : (TrackpadSettings) -> Unit,
) {
    var local by remember { mutableStateOf(settings) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Color(0xFF111C28),
        dragHandle       = null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Trackpad Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp,
                    color      = Color.White
                )
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFF90CAF9))
                }
            }

            HorizontalDivider(color = Color.White.copy(0.08f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                TrackpadSlider(
                    "Pointer Speed", local.pointerSpeed, 0.3f..3.0f,
                    { "%.1fx".format(it) }
                ) { local = local.copy(pointerSpeed = it) }

                TrackpadSlider(
                    "Scroll Speed", local.scrollSpeed, 0.3f..3.0f,
                    { "%.1fx".format(it) }
                ) { local = local.copy(scrollSpeed = it) }

                TrackpadToggle(
                    "Invert Scroll",
                    "Natural / phone-style scrolling (content follows finger)",
                    local.invertScroll
                ) { local = local.copy(invertScroll = it) }

                TrackpadToggle(
                    "Tap to Click",
                    "Short tap = left click",
                    local.tapToClick
                ) { local = local.copy(tapToClick = it) }

                TrackpadToggle(
                    "Two-Finger Right Click",
                    "Two-finger tap = right-click menu",
                    local.twoFingerRightClick
                ) { local = local.copy(twoFingerRightClick = it) }

                TrackpadToggle(
                    "Pointer Acceleration",
                    "Slow = precise · Fast = covers distance",
                    local.accelerationEnabled
                ) { local = local.copy(accelerationEnabled = it) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Click Pressure",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp,
                        color      = Color.White
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ClickPressure.values().forEach { cp ->
                            val sel = local.clickPressure == cp
                            OutlinedButton(
                                onClick  = { local = local.copy(clickPressure = cp) },
                                modifier = Modifier.weight(1f),
                                colors   = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (sel)
                                        Color(0xFF4A90D9).copy(0.15f)
                                    else Color.Transparent
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (sel) 2.dp else 1.dp,
                                    if (sel) Color(0xFF4A90D9)
                                    else Color.White.copy(0.2f)
                                )
                            ) {
                                Text(
                                    cp.name.lowercase().replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp,
                                    color    = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
            }

            HorizontalDivider(color = Color.White.copy(0.08f))

            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111C28))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Button(
                    onClick  = { onSave(local); onDismiss() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90D9))
                ) {
                    Text(
                        "Save Settings",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TrackpadSlider(
    label   : String,
    value   : Float,
    range   : ClosedFloatingPointRange<Float>,
    display : (Float) -> String,
    onChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
            Text(display(value), fontSize = 13.sp, color = Color(0xFF90CAF9),
                fontWeight = FontWeight.Medium)
        }
        Slider(
            value         = value,
            onValueChange = onChange,
            valueRange    = range,
            modifier      = Modifier.fillMaxWidth(),
            colors        = SliderDefaults.colors(
                thumbColor         = Color(0xFF4A90D9),
                activeTrackColor   = Color(0xFF4A90D9),
                inactiveTrackColor = Color.White.copy(0.2f)
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Slow", fontSize = 10.sp, color = Color(0xFF607D8B))
            Text("Fast", fontSize = 10.sp, color = Color(0xFF607D8B))
        }
    }
}

@Composable
private fun TrackpadToggle(
    title   : String,
    subtitle: String,
    checked : Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title,    fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
            Text(subtitle, fontSize   = 11.sp,               color    = Color(0xFF607D8B))
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = Color(0xFF4A90D9),
                checkedTrackColor   = Color(0xFF4A90D9).copy(0.5f),
                uncheckedThumbColor = Color(0xFF607D8B),
                uncheckedTrackColor = Color.White.copy(0.2f)
            )
        )
    }
}

@Composable
private fun LedBadge(label: String, active: Boolean) {
    Surface(
        shape = RoundedCornerShape(3.dp),
        color = if (active) Color(0xFF1565C0) else Color.White.copy(0.04f)
    ) {
        Text(
            label,
            fontSize   = 7.sp,
            color      = if (active) Color.White else Color(0xFF3A4A5A),
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            modifier   = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun TrackpadNotReadyCard() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors   = CardDefaults.cardColors(
                containerColor = Color(0xFFF57F17).copy(0.1f))
        ) {
            Column(
                modifier            = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⏳", fontSize = 32.sp)
                Text("Not Connected",
                    fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                Text(
                    "Start BLE HID → pair from host Bluetooth settings → come back here.",
                    fontSize  = 13.sp,
                    color     = Color(0xFF607D8B),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}