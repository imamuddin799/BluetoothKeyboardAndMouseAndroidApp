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
// Data
// ═════════════════════════════════════════════════════════════════════════════

data class TrackpadSettings(
    val pointerSpeed        : Float         = 1.2f,
    val scrollSpeed         : Float         = 1.0f,
    val invertScroll        : Boolean       = false,
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
// Complete rewrite — single pointerInput, coroutineScope for all actions
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun TrackpadSurface(
    modifier   : Modifier = Modifier,
    settings   : TrackpadSettings,
    onSendMouse: (dx: Int, dy: Int, buttons: Int, wheel: Int) -> Unit,
) {
    val haptic  = LocalHapticFeedback.current
    val scope   = rememberCoroutineScope()
    val density = LocalDensity.current

    // Keep latest values without restarting pointerInput
    val onSendRef   = rememberUpdatedState(onSendMouse)
    val settingsRef = rememberUpdatedState(settings)

    // Tap config
    val TAP_MS     = 200L
    val TAP_SLOP   = with(density) { 18.dp.toPx() }
    val DTAP_MS    = 350L

    // Visual state
    var fingerCount    by remember { mutableIntStateOf(0) }
    var cursorPos      by remember { mutableStateOf<Offset?>(null) }
    var dragLock       by remember { mutableStateOf(false) }
    var rightFlash     by remember { mutableStateOf(false) }
    var scrollFlash    by remember { mutableStateOf(false) }
    var scrollFlashJob by remember { mutableStateOf<Job?>(null) }

    val ripplePos    = remember { mutableStateOf<Offset?>(null) }
    val rippleRadius = remember { Animatable(0f) }
    val rippleAlpha  = remember { Animatable(0f) }

    // Mutable gesture state — all touched only from LaunchedEffect / scope
    // Use Ref objects so closures always see latest value
    val dragLockRef    = remember { mutableStateOf(false) }
    val lastTapMs      = remember { mutableLongStateOf(0L) }
    val dtapArmed      = remember { mutableStateOf(false) }

    // Sync dragLock display state
    LaunchedEffect(dragLockRef.value) { dragLock = dragLockRef.value }

    fun send(dx: Int, dy: Int, btn: Int, w: Int) = onSendRef.value(dx, dy, btn, w)

    fun ripple(pos: Offset) = scope.launch {
        ripplePos.value = pos
        rippleRadius.snapTo(0f)
        rippleAlpha.snapTo(0.85f)
        rippleRadius.animateTo(120f, tween(320))
        rippleAlpha.animateTo(0f, tween(200))
        ripplePos.value = null
    }

    fun flashScroll() {
        scrollFlashJob?.cancel()
        scrollFlashJob = scope.launch {
            scrollFlash = true
            delay(400)
            scrollFlash = false
        }
    }

    fun accel(v: Float, speed: Float): Float {
        if (!settingsRef.value.accelerationEnabled) return v * speed
        val a = abs(v)
        val k = when {
            a < 1f  -> 0.5f
            a < 3f  -> 0.9f
            a < 7f  -> 1.5f
            a < 12f -> 2.1f
            else    -> 2.8f
        }
        return v * k * speed
    }

    Box(modifier = modifier.fillMaxWidth().background(Color(0xFF0D2B45))) {

        // ── Main trackpad area ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 40.dp)
                .pointerInput(Unit) {

                    // Pointer store
                    // [0]=downX [1]=downY [2]=prevX [3]=prevY
                    // [4]=curX  [5]=curY  [6]=downMs [7]=maxMove
                    val pts = mutableMapOf<Long, FloatArray>()

                    // Accumulators — only written here in pointer thread
                    var accX = 0f
                    var accY = 0f
                    var accS = 0f
                    var prevN = 0

                    // Tap tracking
                    var tapDownMs  = 0L
                    var tapMaxMove = 0f
                    var tapDownN   = 0     // finger count at tap-down

                    awaitPointerEventScope {
                        while (true) {
                            val ev  = awaitPointerEvent(PointerEventPass.Initial)
                            val now = System.currentTimeMillis()
                            val s   = settingsRef.value

                            // ── Classify ──────────────────────────────────────
                            val pressing  = ev.changes.filter { it.pressed }
                            val lifting   = ev.changes.filter { !it.pressed && it.previousPressed }

                            // ── Register new / update existing ────────────────
                            for (ch in pressing) {
                                val id = ch.id.value
                                ch.consume()
                                val ex = pts[id]
                                if (ex == null) {
                                    val x = ch.position.x; val y = ch.position.y
                                    pts[id] = floatArrayOf(x, y, x, y, x, y, now.toFloat(), 0f)
                                } else {
                                    ex[2] = ex[4]; ex[3] = ex[5]
                                    ex[4] = ch.position.x; ex[5] = ch.position.y
                                    val d = hypot(ex[4]-ex[0], ex[5]-ex[1])
                                    if (d > ex[7]) ex[7] = d
                                }
                            }

                            val n   = pts.size
                            val all = pts.values.toList()

                            // Reset acc when finger count changes
                            if (n != prevN) {
                                accX = 0f; accY = 0f; accS = 0f
                                // Record tap-down state
                                if (n > prevN) {
                                    // New finger(s) added
                                    tapDownMs  = now
                                    tapMaxMove = 0f
                                    tapDownN   = n
                                }
                                prevN = n
                            }

                            // Update max move every frame
                            for (fp in all) {
                                val d = hypot(fp[4]-fp[0], fp[5]-fp[1])
                                if (d > tapMaxMove) tapMaxMove = d
                            }

                            // Update display
                            scope.launch {
                                fingerCount = n
                                cursorPos   = all.firstOrNull()?.let { Offset(it[4], it[5]) }
                            }

                            // ── Movement ──────────────────────────────────────
                            when {
                                n == 1 -> {
                                    val fp  = all[0]
                                    val dx  = fp[4] - fp[2]
                                    val dy  = fp[5] - fp[3]
                                    val btn = if (dragLockRef.value) 1 else 0
                                    accX += accel(dx, s.pointerSpeed)
                                    accY += accel(dy, s.pointerSpeed)
                                    val ix = accX.toInt(); val iy = accY.toInt()
                                    if (ix != 0 || iy != 0) {
                                        send(ix.coerceIn(-127,127), iy.coerceIn(-127,127), btn, 0)
                                        accX -= ix; accY -= iy
                                    }
                                }
                                n == 2 -> {
                                    if (dragLockRef.value) {
                                        scope.launch {
                                            dragLockRef.value = false
                                            send(0, 0, 0, 0)
                                        }
                                    }
                                    val dy0 = all[0][5] - all[0][3]
                                    val dy1 = all[1][5] - all[1][3]
                                    val avg = (dy0 + dy1) * 0.5f
                                    val dir = if (s.invertScroll) 1f else -1f
                                    accS += avg * s.scrollSpeed * dir * 0.6f
                                    val sw = accS.toInt()
                                    if (sw != 0) {
                                        send(0, 0, 0, sw.coerceIn(-127,127))
                                        accS -= sw
                                        scope.launch { flashScroll() }
                                    }
                                }
                                n == 3 -> {
                                    if (dragLockRef.value) {
                                        scope.launch {
                                            dragLockRef.value = false
                                            send(0, 0, 0, 0)
                                        }
                                    }
                                    val fp = all[0]
                                    val dx = fp[4] - fp[2]
                                    val dy = fp[5] - fp[3]
                                    accX += dx * s.pointerSpeed * 1.8f
                                    accY += dy * s.pointerSpeed * 1.8f
                                    val ix = accX.toInt(); val iy = accY.toInt()
                                    if (ix != 0 || iy != 0) {
                                        send(ix.coerceIn(-127,127), iy.coerceIn(-127,127), 0, 0)
                                        accX -= ix; accY -= iy
                                    }
                                }
                            }

                            // ── Releases ──────────────────────────────────────
                            for (ch in lifting) {
                                ch.consume()
                                val id = ch.id.value
                                pts.remove(id)
                            }

                            if (lifting.isNotEmpty()) {
                                val s2        = settingsRef.value
                                val duration  = now - tapDownMs
                                val isTap     = duration in 1L..TAP_MS &&
                                                tapMaxMove < TAP_SLOP &&
                                                pts.isEmpty()   // all fingers up

                                if (isTap) {
                                    val liftPos = lifting.first().let {
                                        Offset(it.position.x, it.position.y)
                                    }
                                    val fingers = tapDownN

                                    scope.launch {
                                        when {
                                            // ── 1-finger tap ─────────────────
                                            fingers == 1 && s2.tapToClick -> {
                                                if (dragLockRef.value) {
                                                    // Release drag lock
                                                    dragLockRef.value = false
                                                    send(0, 0, 0, 0)
                                                    haptic.performHapticFeedback(
                                                        HapticFeedbackType.LongPress)
                                                    ripple(liftPos)
                                                    dtapArmed.value = false
                                                } else {
                                                    // Check double-tap-drag
                                                    val gap = now - lastTapMs.longValue
                                                    if (dtapArmed.value && gap in 30L..DTAP_MS) {
                                                        // Double-tap drag!
                                                        dtapArmed.value   = false
                                                        dragLockRef.value = true
                                                        send(0, 0, 1, 0)
                                                        haptic.performHapticFeedback(
                                                            HapticFeedbackType.LongPress)
                                                        ripple(liftPos)
                                                    } else {
                                                        // Single click
                                                        dtapArmed.value = false
                                                        send(0, 0, 1, 0)
                                                        delay(45)
                                                        send(0, 0, 0, 0)
                                                        haptic.performHapticFeedback(
                                                            HapticFeedbackType.LongPress)
                                                        ripple(liftPos)
                                                        // Arm for next tap
                                                        lastTapMs.longValue = now
                                                        dtapArmed.value     = true
                                                        launch {
                                                            delay(DTAP_MS + 60)
                                                            dtapArmed.value = false
                                                        }
                                                    }
                                                }
                                            }

                                            // ── 2-finger tap ─────────────────
                                            fingers >= 2 && s2.twoFingerRightClick -> {
                                                if (dragLockRef.value) {
                                                    dragLockRef.value = false
                                                    send(0, 0, 0, 0)
                                                }
                                                dtapArmed.value = false
                                                send(0, 0, 2, 0)
                                                delay(55)
                                                send(0, 0, 0, 0)
                                                haptic.performHapticFeedback(
                                                    HapticFeedbackType.LongPress)
                                                rightFlash = true
                                                delay(200)
                                                rightFlash = false
                                                ripple(liftPos)
                                            }
                                        }
                                    }
                                } else if (pts.isEmpty()) {
                                    // Non-tap, all fingers up
                                    scope.launch { dtapArmed.value = false }
                                }

                                if (pts.isEmpty()) {
                                    accX = 0f; accY = 0f; accS = 0f; prevN = 0
                                    scope.launch {
                                        fingerCount = 0
                                        cursorPos   = null
                                        tapMaxMove  = 0f
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawTrackpadGrid()
                if (rightFlash) drawRect(Color(0xFF4A148C).copy(alpha = 0.22f))
                if (dragLock) {
                    drawRect(Color(0xFF1565C0).copy(alpha = 0.09f))
                    drawRoundRect(
                        Color(0xFF4A90D9).copy(alpha = 0.7f),
                        Offset(3f, 3f),
                        Size(size.width - 6f, size.height - 6f),
                        CornerRadius(12f),
                        style = Stroke(3f)
                    )
                }
                ripplePos.value?.let { drawRipple(it, rippleRadius.value, rippleAlpha.value) }
                cursorPos?.let { drawCursorGhost(it) }
                drawMouseIcon(size)
            }

            if (dragLock) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .background(Color(0xFF1565C0).copy(0.93f), RoundedCornerShape(20.dp))
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
                    Modifier
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
                Modifier.fillMaxSize(),
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
                            var isFirst    = true
                            awaitPointerEventScope {
                                while (true) {
                                    val ev = awaitPointerEvent(PointerEventPass.Initial)
                                    val ch = ev.changes.firstOrNull() ?: continue
                                    if (ch.pressed) {
                                        ch.consume()
                                        if (isFirst) {
                                            lastY  = ch.position.y
                                            isFirst = false
                                        } else {
                                            val s   = settingsRef.value
                                            val dy  = ch.position.y - lastY
                                            val dir = if (s.invertScroll) 1f else -1f
                                            val d   = (dy * s.scrollSpeed * dir * 0.5f).toInt()
                                            if (d != 0) {
                                                send(0, 0, 0, d.coerceIn(-127, 127))
                                                scope.launch { flashScroll() }
                                            }
                                            lastY = ch.position.y
                                        }
                                    } else {
                                        isFirst = true
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val cx    = size.width / 2f
                        val alpha = if (scrollFlash) 0.55f else 0.13f
                        val col   = Color.White.copy(alpha)
                        var y = 10f
                        while (y < size.height - 10f) {
                            drawLine(
                                col, Offset(cx, y),
                                Offset(cx, (y + 7f).coerceAtMost(size.height - 10f)),
                                2.5f
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
                    Text(
                        "↕",
                        fontSize = 14.sp,
                        color    = Color.White.copy(if (scrollFlash) 0.9f else 0.28f)
                    )
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
    val haptic  = LocalHapticFeedback.current
    val scope   = rememberCoroutineScope()
    val sendRef = rememberUpdatedState(onSendMouse)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0xFF081929))
    ) {
        ClickZoneButton("Left", Modifier.weight(1f), Color(0xFF1565C0)) {
            sendRef.value(0, 0, 1, 0)
            scope.launch { delay(80); sendRef.value(0, 0, 0, 0) }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(0.10f)))
        ClickZoneButton("Mid", Modifier.weight(0.6f), Color(0xFF1B5E20)) {
            sendRef.value(0, 0, 4, 0)
            scope.launch { delay(80); sendRef.value(0, 0, 0, 0) }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(0.10f)))
        ClickZoneButton("Right", Modifier.weight(1f), Color(0xFF4A148C)) {
            sendRef.value(0, 0, 2, 0)
            scope.launch { delay(80); sendRef.value(0, 0, 0, 0) }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Scroll arrow
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun ScrollArrowButton(label: String, onScroll: () -> Unit) {
    val scope        = rememberCoroutineScope()
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
    var pressed     by remember { mutableStateOf(false) }
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
    val c = Color.White.copy(alpha = 0.020f)
    val step = 44.dp.toPx()
    var x = 0f
    while (x <= size.width)  { drawLine(c, Offset(x, 0f), Offset(x, size.height), 1f); x += step }
    var y = 0f
    while (y <= size.height) { drawLine(c, Offset(0f, y), Offset(size.width, y),  1f); y += step }
}

private fun DrawScope.drawRipple(center: Offset, radius: Float, alpha: Float) {
    drawCircle(Color.White.copy(alpha * 0.30f), radius,        center, style = Stroke(2.dp.toPx()))
    drawCircle(Color.White.copy(alpha * 0.08f), radius * 0.4f, center)
}

private fun DrawScope.drawCursorGhost(pos: Offset) {
    drawCircle(Color.White.copy(0.22f), 14.dp.toPx(), pos, style = Stroke(1.5.dp.toPx()))
    drawCircle(Color.White.copy(0.08f),  5.dp.toPx(), pos)
}

private fun DrawScope.drawMouseIcon(canvasSize: Size) {
    val cx = canvasSize.width / 2f
    val cy = canvasSize.height / 2f - 20.dp.toPx()
    val w  = 30.dp.toPx(); val h = 44.dp.toPx(); val r = w / 2f
    val sc = Color.White.copy(0.13f); val sw = 1.5.dp.toPx()
    drawRoundRect(sc, Offset(cx-r, cy-h/2f), Size(w, h), CornerRadius(r), Stroke(sw))
    drawLine(sc, Offset(cx, cy-h/2f), Offset(cx, cy-h/2f+h*0.38f), sw)
    drawLine(sc,
        Offset(cx-r+2f, cy-h/2f+h*0.38f),
        Offset(cx+r-2f, cy-h/2f+h*0.38f), sw)
    val wTop = cy - h/2f + h*0.07f
    val ww   = 4.dp.toPx()
    drawRoundRect(sc, Offset(cx-ww/2f, wTop), Size(ww, h*0.23f), CornerRadius(ww/2f), Stroke(sw))
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
                    "Natural scrolling — content follows finger direction",
                    local.invertScroll
                ) { local = local.copy(invertScroll = it) }

                TrackpadToggle(
                    "Tap to Click",
                    "Short tap = left click · double-tap drag = drag lock",
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
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A90D9))
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
            Text(
                display(value),
                fontSize   = 13.sp,
                color      = Color(0xFF90CAF9),
                fontWeight = FontWeight.Medium
            )
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
                containerColor = Color(0xFFF57F17).copy(0.1f)
            )
        ) {
            Column(
                modifier            = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⏳", fontSize = 32.sp)
                Text(
                    "Not Connected",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    color      = Color.White
                )
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