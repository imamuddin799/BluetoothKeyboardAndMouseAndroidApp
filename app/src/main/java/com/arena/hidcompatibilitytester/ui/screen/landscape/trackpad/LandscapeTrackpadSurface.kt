package com.arena.hidcompatibilitytester.ui.screen.landscape.trackpad

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

@Composable
fun LandscapeTrackpadSurface(
    modifier: Modifier = Modifier,
    settings: LandscapeTrackpadSettings,
    onSendMouse: (dx: Int, dy: Int, buttons: Int, wheel: Int) -> Unit,
    physHoldActive: MutableState<Boolean>,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val onSendRef = rememberUpdatedState(onSendMouse)
    val settingsRef = rememberUpdatedState(settings)

    val TAP_MAX_MS = 200L
    val TAP_SLOP = with(density) { 18.dp.toPx() }
    val DTAP_GAP_MS = 350L

    var fingerCount by remember { mutableIntStateOf(0) }
    var cursorPos by remember { mutableStateOf<Offset?>(null) }
    var dragActive by remember { mutableStateOf(false) }
    var rightFlash by remember { mutableStateOf(false) }
    var scrollFlash by remember { mutableStateOf(false) }
    var scrollFlashJob by remember { mutableStateOf<Job?>(null) }

    val ripplePos = remember { mutableStateOf<Offset?>(null) }
    val rippleRadius = remember { Animatable(0f) }
    val rippleAlpha = remember { Animatable(0f) }

    val dragActiveRef = remember { mutableStateOf(false) }
    val holdDragActive = remember { mutableStateOf(false) }
    val lockDragActive = remember { mutableStateOf(false) }

    val dtapArmed = remember { mutableStateOf(false) }
    val dtapArmJob = remember { mutableStateOf<Job?>(null) }
    val lastTapMs = remember { mutableLongStateOf(0L) }

    LaunchedEffect(dragActiveRef.value) { dragActive = dragActiveRef.value }

    fun send(dx: Int, dy: Int, btn: Int, w: Int) = onSendRef.value(dx, dy, btn, w)

    fun ripple(pos: Offset) = scope.launch {
        ripplePos.value = pos
        rippleRadius.snapTo(0f); rippleAlpha.snapTo(0.85f)
        rippleRadius.animateTo(120f, tween(320))
        rippleAlpha.animateTo(0f, tween(200))
        ripplePos.value = null
    }

    fun flashScroll() {
        scrollFlashJob?.cancel()
        scrollFlashJob = scope.launch { scrollFlash = true; delay(400); scrollFlash = false }
    }

    fun releaseDrag(pos: Offset? = null) {
        if (!dragActiveRef.value) return
        dragActiveRef.value = false; holdDragActive.value = false; lockDragActive.value = false
        send(0, 0, 0, 0); if (pos != null) ripple(pos)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun startDrag(hold: Boolean, lock: Boolean, pos: Offset) {
        dragActiveRef.value = true; holdDragActive.value = hold; lockDragActive.value = lock
        send(0, 0, 1, 0); ripple(pos); haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    suspend fun quickClick(pos: Offset) {
        send(0, 0, 1, 0); delay(45); send(0, 0, 0, 0)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress); ripple(pos)
    }

    fun accel(v: Float, speed: Float): Float {
        if (!settingsRef.value.accelerationEnabled) return v * speed
        val a = abs(v)
        val k = when {
            a < 1f -> 0.5f; a < 3f -> 0.9f; a < 7f -> 1.5f; a < 12f -> 2.1f; else -> 2.8f
        }
        return v * k * speed
    }

    val arrowOnLeft = settings.arrowPosition == LandscapeSidePosition.LEFT
    val scrollOnLeft = settings.scrollPosition == LandscapeSidePosition.LEFT

    Box(modifier = modifier.fillMaxWidth().background(Color(0xFF0D2B45))) {
        Row(Modifier.fillMaxSize()) {
            if (scrollOnLeft && settings.showScrollStrip) {
                LandscapeTrackpadScrollStrip(
                    onScrollUp = { onSendRef.value(0, 0, 0, 3) },
                    onScrollDown = { onSendRef.value(0, 0, 0, -3) },
                    onSendRef = onSendRef,
                    settingsRef = settingsRef,
                    scrollFlash = scrollFlash,
                    onFlash = { flashScroll() },
                    scope = scope
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            val pts = mutableMapOf<Long, FloatArray>()
                            var accX = 0f; var accY = 0f; var accS = 0f; var prevN = 0
                            var tapDownMs = 0L; var tapMaxMove = 0f; var tapDownN = 0

                            awaitPointerEventScope {
                                while (true) {
                                    val ev = awaitPointerEvent(PointerEventPass.Final)
                                    val now = System.currentTimeMillis()
                                    val s = settingsRef.value
                                    val pressing = ev.changes.filter { it.pressed && !it.isConsumed }
                                    val lifting = ev.changes.filter { !it.pressed && it.previousPressed && !it.isConsumed }

                                    for (ch in pressing) {
                                        ch.consume()
                                        val id = ch.id.value; val ex = pts[id]
                                        if (ex == null) {
                                            val x = ch.position.x; val y = ch.position.y
                                            pts[id] = floatArrayOf(x, y, x, y, x, y, now.toFloat(), 0f)
                                        } else {
                                            ex[2] = ex[4]; ex[3] = ex[5]
                                            ex[4] = ch.position.x; ex[5] = ch.position.y
                                            val d = hypot(ex[4] - ex[0], ex[5] - ex[1])
                                            if (d > ex[7]) ex[7] = d
                                        }
                                    }

                                    val n = pts.size; val all = pts.values.toList()

                                    if (n != prevN) {
                                        accX = 0f; accY = 0f; accS = 0f
                                        if (n > prevN) {
                                            tapDownMs = now; tapMaxMove = 0f; tapDownN = n
                                            if (n == 1 && dtapArmed.value && !s.dragLockMode) {
                                                dtapArmJob.value?.cancel(); dtapArmJob.value = null; dtapArmed.value = false
                                                scope.launch { startDrag(true, false, Offset(all[0][4], all[0][5])) }
                                            }
                                            if (n == 1 && dtapArmed.value && s.dragLockMode) {
                                                dtapArmJob.value?.cancel(); dtapArmJob.value = null; dtapArmed.value = false
                                                scope.launch { startDrag(false, true, Offset(all[0][4], all[0][5])) }
                                            }
                                        }
                                        prevN = n
                                    }

                                    for (fp in all) {
                                        val d = hypot(fp[4] - fp[0], fp[5] - fp[1])
                                        if (d > tapMaxMove) tapMaxMove = d
                                    }

                                    scope.launch {
                                        fingerCount = n
                                        cursorPos = all.firstOrNull()?.let { Offset(it[4], it[5]) }
                                    }

                                    when {
                                        n == 1 -> {
                                            val fp = all[0]; val dx = fp[4] - fp[2]; val dy = fp[5] - fp[3]
                                            val btn = if (dragActiveRef.value || physHoldActive.value) 1 else 0
                                            accX += accel(dx, s.pointerSpeed); accY += accel(dy, s.pointerSpeed)
                                            val ix = accX.toInt(); val iy = accY.toInt()
                                            if (ix != 0 || iy != 0) {
                                                send(ix.coerceIn(-127, 127), iy.coerceIn(-127, 127), btn, 0)
                                                accX -= ix; accY -= iy
                                            }
                                        }
                                        n == 2 -> {
                                            if (lockDragActive.value) scope.launch { releaseDrag() }
                                            val dy0 = all[0][5] - all[0][3]; val dy1 = all[1][5] - all[1][3]
                                            val avg = (dy0 + dy1) * 0.5f
                                            val dir = if (s.invertScroll) 1f else -1f
                                            accS += avg * s.scrollSpeed * dir * 0.6f
                                            val sw = accS.toInt()
                                            if (sw != 0) {
                                                send(0, 0, 0, sw.coerceIn(-127, 127)); accS -= sw
                                                scope.launch { flashScroll() }
                                            }
                                        }
                                        n == 3 -> {
                                            if (dragActiveRef.value) scope.launch { releaseDrag() }
                                            val fp = all[0]; val dx = fp[4] - fp[2]; val dy = fp[5] - fp[3]
                                            accX += dx * s.pointerSpeed * 1.8f; accY += dy * s.pointerSpeed * 1.8f
                                            val ix = accX.toInt(); val iy = accY.toInt()
                                            if (ix != 0 || iy != 0) {
                                                send(ix.coerceIn(-127, 127), iy.coerceIn(-127, 127), 0, 0)
                                                accX -= ix; accY -= iy
                                            }
                                        }
                                    }

                                    for (ch in lifting) { ch.consume(); pts.remove(ch.id.value) }

                                    if (lifting.isNotEmpty()) {
                                        val lp = lifting.first().position.let { Offset(it.x, it.y) }
                                        val dur = now - tapDownMs
                                        val wasTap = dur in 1L..TAP_MAX_MS && tapMaxMove < TAP_SLOP && pts.isEmpty()

                                        if (pts.isEmpty() && holdDragActive.value) {
                                            scope.launch { releaseDrag(lp) }
                                            accX = 0f; accY = 0f; accS = 0f; prevN = 0
                                            scope.launch { fingerCount = 0; cursorPos = null }; continue
                                        }
                                        if (wasTap && tapDownN == 1 && lockDragActive.value) {
                                            scope.launch { releaseDrag(lp) }
                                            accX = 0f; accY = 0f; accS = 0f; prevN = 0
                                            scope.launch { fingerCount = 0; cursorPos = null }; continue
                                        }
                                        if (wasTap && s.tapToClick) {
                                            scope.launch {
                                                when {
                                                    tapDownN == 1 -> {
                                                        val gap = now - lastTapMs.longValue
                                                        if (dtapArmed.value && gap in 30L..DTAP_GAP_MS) {
                                                            dtapArmJob.value?.cancel(); dtapArmJob.value = null; dtapArmed.value = false
                                                            if (s.dragLockMode && tapMaxMove < TAP_SLOP) {
                                                                releaseDrag(lp); delay(30); quickClick(lp)
                                                            }
                                                        } else {
                                                            dtapArmed.value = false; lastTapMs.longValue = now; dtapArmed.value = true
                                                            val cp = lp
                                                            dtapArmJob.value = launch {
                                                                delay(DTAP_GAP_MS + 30)
                                                                if (dtapArmed.value) { dtapArmed.value = false; quickClick(cp) }
                                                            }
                                                        }
                                                    }
                                                    tapDownN >= 2 && s.twoFingerRightClick -> {
                                                        if (dragActiveRef.value) releaseDrag()
                                                        dtapArmed.value = false; dtapArmJob.value?.cancel(); dtapArmJob.value = null
                                                        send(0, 0, 2, 0); delay(55); send(0, 0, 0, 0)
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        rightFlash = true; delay(200); rightFlash = false; ripple(lp)
                                                    }
                                                }
                                            }
                                        } else if (pts.isEmpty()) {
                                            scope.launch { dtapArmed.value = false }
                                        }
                                        if (pts.isEmpty()) {
                                            accX = 0f; accY = 0f; accS = 0f; prevN = 0
                                            scope.launch { fingerCount = 0; cursorPos = null }
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        landscapeDrawTrackpadGrid()
                        if (rightFlash) drawRect(Color(0xFF4A148C).copy(alpha = 0.22f))
                        if (dragActive) {
                            drawRect(Color(0xFF1565C0).copy(alpha = 0.09f))
                            drawRoundRect(
                                Color(0xFF4A90D9).copy(0.7f), Offset(3f, 3f),
                                Size(size.width - 6f, size.height - 6f), CornerRadius(12f),
                                style = Stroke(3f)
                            )
                        }
                        ripplePos.value?.let { landscapeDrawRipple(it, rippleRadius.value, rippleAlpha.value) }
                        cursorPos?.let { landscapeDrawCursorGhost(it) }
                        landscapeDrawMouseIcon(size)
                    }
                }

                if (dragActive) {
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                            .background(
                                if (settings.dragLockMode) Color(0xFF1565C0).copy(0.93f)
                                else Color(0xFF6A1B9A).copy(0.93f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Text(
                            if (settings.dragLockMode) "⬚ Drag Lock · tap to release"
                            else "✋ Hold-Drag · lift to release",
                            fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold
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
                            fontSize = 10.sp, color = Color.White.copy(0.75f)
                        )
                    }
                }

                if (settings.showArrowKeys) {
                    LandscapeArrowKeysPad(
                        modifier = Modifier
                            .align(if (arrowOnLeft) Alignment.BottomStart else Alignment.BottomEnd)
                            .padding(2.dp),
                        onMove = { dx, dy ->
                            val btn = if (physHoldActive.value || dragActiveRef.value) 1 else 0
                            onSendRef.value(dx, dy, btn, 0)
                        }
                    )
                }
            }

            if (!scrollOnLeft && settings.showScrollStrip) {
                LandscapeTrackpadScrollStrip(
                    onScrollUp = { onSendRef.value(0, 0, 0, 3) },
                    onScrollDown = { onSendRef.value(0, 0, 0, -3) },
                    onSendRef = onSendRef,
                    settingsRef = settingsRef,
                    scrollFlash = scrollFlash,
                    onFlash = { flashScroll() },
                    scope = scope
                )
            }
        }
    }
}