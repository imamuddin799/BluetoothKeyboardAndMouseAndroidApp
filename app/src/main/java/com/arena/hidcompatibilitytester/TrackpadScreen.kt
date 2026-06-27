// TrackpadScreen.kt — complete rewrite fixing tap-click bug
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// ═════════════════════════════════════════════════════════════════════════════
// Settings data class
// ═════════════════════════════════════════════════════════════════════════════

data class TrackpadSettings(
    val pointerSpeed        : Float   = 1.2f,
    val scrollSpeed         : Float   = 1.0f,
    val naturalScrolling    : Boolean = false,
    val tapToClick          : Boolean = true,
    val twoFingerRightClick : Boolean = true,
    val accelerationEnabled : Boolean = true,
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
    val haptic  = LocalHapticFeedback.current
    val scope   = rememberCoroutineScope()
    val density = LocalDensity.current

    // ── Sub-pixel accumulators — plain array, never triggers recomposition ────
    val acc = remember { floatArrayOf(0f, 0f) }       // [0]=x  [1]=y
    val scr = remember { floatArrayOf(0f, 0f) }       // [0]=scrollY

    // ── Scroll-bar ─────────────────────────────────────────────────────────
    var scrollBarPos      by remember { mutableFloatStateOf(0.5f) }
    var scrollBarVisible  by remember { mutableStateOf(false) }
    var scrollHideJob     by remember { mutableStateOf<Job?>(null) }
    var scrollBarDragging by remember { mutableStateOf(false) }
    var sbDragStartY      by remember { mutableFloatStateOf(0f) }
    var sbDragStartPos    by remember { mutableFloatStateOf(0f) }
    var trackHeightPx     by remember { mutableFloatStateOf(1f) }

    // ── Visual ─────────────────────────────────────────────────────────────
    var rippleCenter  by remember { mutableStateOf<Offset?>(null) }
    val rippleRadius  = remember { Animatable(0f) }
    val rippleAlpha   = remember { Animatable(0f) }
    var cursorPos     by remember { mutableStateOf<Offset?>(null) }
    var fingerCount   by remember { mutableIntStateOf(0) }
    var rightFlash    by remember { mutableStateOf(false) }

    // ── Tap state ─────────────────────────────────────────────────────────
    //
    // THE FIX: We use a separate "tap snapshot" that captures everything we
    // need about a gesture BEFORE pts is cleared.  This means even after
    // pts.clear() we can still decide whether it was a tap.
    //
    // A tap is ONLY fired when:
    //   • totalMove (max distance finger ever moved from start) < TAP_SLOP
    //   • duration < TAP_MAX_MS
    // If the user dragged the cursor, totalMove will be large and we skip.
    //
    val TAP_SLOP    = 18f   // px — any movement larger than this = NOT a tap
    val TAP_MAX_MS  = 200L  // ms — longer than this = NOT a tap

    var lastTapMs    by remember { mutableLongStateOf(0L) }
    var tapCount     by remember { mutableIntStateOf(0) }
    var tapPending   by remember { mutableStateOf(false) }

    fun fireRipple(c: Offset) = scope.launch {
        rippleCenter = c
        rippleRadius.snapTo(0f); rippleAlpha.snapTo(0.7f)
        rippleRadius.animateTo(90f, tween(280))
        rippleAlpha.animateTo(0f, tween(160))
        rippleCenter = null
    }

    fun showScrollBar(pos: Float) {
        scrollBarPos    = pos.coerceIn(0f, 1f)
        scrollBarVisible = true
        scrollHideJob?.cancel()
        scrollHideJob = scope.launch { delay(900); scrollBarVisible = false }
    }

    fun accel(raw: Float, speed: Float): Float {
        if (!settings.accelerationEnabled) return raw * speed
        val a = abs(raw)
        val k = when {
            a < 1.5f -> 0.6f
            a < 4f   -> 1.0f
            a < 9f   -> 1.5f
            else     -> 2.0f
        }
        return raw * k * speed
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Layout
    // ═══════════════════════════════════════════════════════════════════════

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Header ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A1F33))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("Trackpad",
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    if (isReady) "Ready · $fingerCount finger(s)" else "Not connected",
                    color    = if (isReady) Color(0xFF81C784) else Color(0xFFEF9A9A),
                    fontSize = 11.sp
                )
            }
            IconButton(onClick = onShowSettings) {
                Text("⚙", fontSize = 20.sp, color = Color.White)
            }
        }

        // ── Surface + scrollbar row ────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0D2B45))
        ) {

            // ── Gesture area ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(end = 36.dp)
                    .pointerInput(isReady, settings) {
                        if (!isReady) return@pointerInput

                        // ── Per-finger tracking ──────────────────────────
                        // Each entry: [startX, startY, prevX, prevY,
                        //              curX,  curY,  downMs, maxMove]
                        //              idx:  0      1      2      3
                        //                    4      5      6      7
                        val pts = mutableMapOf<Long, FloatArray>()

                        awaitPointerEventScope {
                            while (true) {

                                val evt = awaitPointerEvent(PointerEventPass.Main)
                                var anyUp = false

                                for (ch in evt.changes) {
                                    val id = ch.id.value
                                    if (ch.pressed) {
                                        val existing = pts[id]
                                        if (existing == null) {
                                            // New finger down
                                            pts[id] = floatArrayOf(
                                                ch.position.x, ch.position.y,  // 0,1 start
                                                ch.position.x, ch.position.y,  // 2,3 prev
                                                ch.position.x, ch.position.y,  // 4,5 cur
                                                System.currentTimeMillis().toFloat(), // 6 downMs
                                                0f                              // 7 maxMove
                                            )
                                        } else {
                                            // Update prev → cur
                                            existing[2] = existing[4]  // prev = cur
                                            existing[3] = existing[5]
                                            existing[4] = ch.position.x
                                            existing[5] = ch.position.y
                                            // Track total movement from start
                                            val dx = existing[4] - existing[0]
                                            val dy = existing[5] - existing[1]
                                            val move = kotlin.math.sqrt(dx*dx + dy*dy)
                                            if (move > existing[7]) existing[7] = move
                                        }
                                    } else {
                                        anyUp = true
                                    }
                                    ch.consume()
                                }

                                val n   = pts.size
                                val all = pts.values.toList()

                                // Update UI state
                                fingerCount = n
                                cursorPos   = if (n > 0) Offset(all[0][4], all[0][5]) else null

                                // ── Movement ────────────────────────────

                                when (n) {
                                    1 -> {
                                        val fp  = all[0]
                                        val rdx = fp[4] - fp[2]   // cur - prev
                                        val rdy = fp[5] - fp[3]

                                        acc[0] += accel(rdx, settings.pointerSpeed)
                                        acc[1] += accel(rdy, settings.pointerSpeed)

                                        val ix = acc[0].toInt()
                                        val iy = acc[1].toInt()
                                        if (ix != 0 || iy != 0) {
                                            onSendMouse(
                                                ix.coerceIn(-127, 127),
                                                iy.coerceIn(-127, 127),
                                                0, 0
                                            )
                                            acc[0] -= ix
                                            acc[1] -= iy
                                        }
                                    }

                                    2 -> {
                                        val avgDy = ((all[0][5] - all[0][3]) +
                                                     (all[1][5] - all[1][3])) / 2f
                                        val dir   = if (settings.naturalScrolling) 1f else -1f

                                        scr[0] += avgDy * settings.scrollSpeed * dir

                                        val sw = scr[0].toInt()
                                        if (sw != 0) {
                                            onSendMouse(0, 0, 0, sw.coerceIn(-127, 127))
                                            scr[0] -= sw
                                            val newPos = (scrollBarPos + sw * 0.004f)
                                                .coerceIn(0f, 1f)
                                            showScrollBar(newPos)
                                        }
                                    }

                                    3 -> {
                                        val fp  = all[0]
                                        val rdx = fp[4] - fp[2]
                                        val rdy = fp[5] - fp[3]
                                        acc[0] += rdx * settings.pointerSpeed * 1.6f
                                        acc[1] += rdy * settings.pointerSpeed * 1.6f
                                        val ix = acc[0].toInt()
                                        val iy = acc[1].toInt()
                                        if (ix != 0 || iy != 0) {
                                            onSendMouse(
                                                ix.coerceIn(-127, 127),
                                                iy.coerceIn(-127, 127),
                                                0, 0
                                            )
                                            acc[0] -= ix; acc[1] -= iy
                                        }
                                    }
                                }

                                // ── Finger lifted ────────────────────────
                                // KEY: we snapshot tap data BEFORE removing
                                // the finger from pts, so we can check maxMove.

                                if (anyUp) {
                                    val now = System.currentTimeMillis()

                                    // Collect lifted fingers
                                    val lifted = evt.changes.filter { !it.pressed }

                                    for (lc in lifted) {
                                        val fp = pts[lc.id.value] ?: continue

                                        val duration = now - fp[6].toLong()
                                        val maxMove  = fp[7]           // max distance from start
                                        val downFc   = evt.changes.size // finger count at lift

                                        // ── Tap detection ───────────────
                                        // Only a tap if short AND barely moved.
                                        // maxMove < TAP_SLOP ensures that any
                                        // cursor-movement gesture is excluded.
                                        val isTap = duration < TAP_MAX_MS &&
                                                    maxMove  < TAP_SLOP

                                        val tapPos = Offset(lc.position.x, lc.position.y)

                                        if (isTap) {
                                            if (downFc == 1 && settings.tapToClick) {
                                                // Single-finger tap
                                                if (now - lastTapMs < 300 && tapCount >= 1) {
                                                    // Double tap → double click
                                                    tapCount  = 0
                                                    tapPending = false
                                                    scope.launch {
                                                        onSendMouse(0,0,1,0); delay(50)
                                                        onSendMouse(0,0,0,0); delay(50)
                                                        onSendMouse(0,0,1,0); delay(50)
                                                        onSendMouse(0,0,0,0)
                                                    }
                                                    fireRipple(tapPos)
                                                } else {
                                                    // First tap — wait briefly for double
                                                    tapCount  = 1
                                                    lastTapMs = now
                                                    tapPending = true
                                                    val savedPos = tapPos
                                                    scope.launch {
                                                        delay(210)
                                                        if (tapPending) {
                                                            tapPending = false
                                                            // Single click
                                                            onSendMouse(0,0,1,0)
                                                            delay(60)
                                                            onSendMouse(0,0,0,0)
                                                            haptic.performHapticFeedback(
                                                                HapticFeedbackType.LongPress)
                                                            fireRipple(savedPos)
                                                            tapCount = 0
                                                        }
                                                    }
                                                }
                                            }

                                            if (downFc >= 2 &&
                                                settings.twoFingerRightClick) {
                                                // Two-finger tap → right click
                                                onSendMouse(0, 0, 2, 0)
                                                scope.launch {
                                                    delay(60)
                                                    onSendMouse(0, 0, 0, 0)
                                                }
                                                haptic.performHapticFeedback(
                                                    HapticFeedbackType.LongPress)
                                                rightFlash = true
                                                scope.launch {
                                                    delay(160)
                                                    rightFlash = false
                                                }
                                                fireRipple(tapPos)
                                            }
                                        }

                                        // Remove lifted finger from map
                                        pts.remove(lc.id.value)
                                    }

                                    // Reset accumulators when all fingers lift
                                    if (pts.isEmpty()) {
                                        acc[0] = 0f; acc[1] = 0f
                                        scr[0] = 0f
                                        fingerCount = 0
                                        cursorPos   = null
                                    }
                                }
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawTrackpadGrid()
                    if (rightFlash) drawRect(Color(0xFF7B1FA2).copy(alpha = 0.15f))
                    rippleCenter?.let { drawRipple(it, rippleRadius.value, rippleAlpha.value) }
                    cursorPos?.let    { drawCursorGhost(it) }
                    drawMouseIcon(size)
                }
            }

            // ── Right scrollbar column ─────────────────────────────────────
            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(36.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF081929))
            ) {
                val arrowH    = 44.dp
                val trackH    = maxHeight - arrowH * 2
                trackHeightPx = with(density) { trackH.toPx() }.coerceAtLeast(1f)

                Column(
                    modifier            = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // ── Up arrow ───────────────────────────────────────────
                    ScrollArrowChip("︿") { onSendMouse(0, 0, 0, 3) }

                    // ── Draggable track ────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .weight(1f)
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val evt = awaitPointerEvent(PointerEventPass.Main)
                                        val ch  = evt.changes.firstOrNull() ?: continue
                                        if (ch.pressed) {
                                            if (!scrollBarDragging) {
                                                scrollBarDragging = true
                                                sbDragStartY      = ch.position.y
                                                sbDragStartPos    = scrollBarPos
                                            } else {
                                                val dy    = ch.position.y - sbDragStartY
                                                val ratio = dy / trackHeightPx
                                                val newPos = (sbDragStartPos + ratio)
                                                    .coerceIn(0f, 1f)
                                                val delta  = newPos - scrollBarPos
                                                val wheel  = (delta * 80f)
                                                    .roundToInt().coerceIn(-127, 127)
                                                if (wheel != 0) {
                                                    val dir = if (settings.naturalScrolling) 1 else -1
                                                    onSendMouse(0, 0, 0, wheel * dir)
                                                    showScrollBar(newPos)
                                                }
                                            }
                                            ch.consume()
                                        } else {
                                            if (scrollBarDragging) scrollBarDragging = false
                                        }
                                    }
                                }
                            }
                    ) {
                        // Thumb — visible when scrolling or dragging
                        if (scrollBarVisible || scrollBarDragging) {
                            val thumbFrac = 0.18f
                            val maxOffsetPx = trackHeightPx * (1f - thumbFrac)
                            val offsetDp = with(density) {
                                (scrollBarPos * maxOffsetPx).toDp()
                            }
                            Box(
                                modifier = Modifier
                                    .offset(y = offsetDp)
                                    .fillMaxWidth()
                                    .fillMaxHeight(thumbFrac)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (scrollBarDragging) Color.White.copy(alpha = 0.85f)
                                        else Color.White.copy(alpha = 0.50f)
                                    )
                            )
                        }
                    }

                    // ── Down arrow ─────────────────────────────────────────
                    ScrollArrowChip("﹀") { onSendMouse(0, 0, 0, -3) }
                }
            }
        }

        // ── Bottom click buttons ───────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .background(Color(0xFF081929))
        ) {
            ClickZoneButton("Left",   Modifier.weight(1f),    Color(0xFF1565C0)) {
                onSendMouse(0, 0, 1, 0)
                scope.launch { delay(90); onSendMouse(0, 0, 0, 0) }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            Box(
                Modifier.width(1.dp).fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.12f))
            )
            ClickZoneButton("Middle", Modifier.weight(0.55f), Color(0xFF1B5E20)) {
                onSendMouse(0, 0, 4, 0)
                scope.launch { delay(90); onSendMouse(0, 0, 0, 0) }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            Box(
                Modifier.width(1.dp).fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.12f))
            )
            ClickZoneButton("Right",  Modifier.weight(1f),    Color(0xFF4A148C)) {
                onSendMouse(0, 0, 2, 0)
                scope.launch { delay(90); onSendMouse(0, 0, 0, 0) }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Scroll arrow — tap or hold to repeat
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ScrollArrowChip(label: String, onScroll: () -> Unit) {
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color(0xFF0F2840))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onScroll()
                        val job = scope.launch {
                            delay(380)
                            while (isActive) { onScroll(); delay(80) }
                        }
                        tryAwaitRelease()
                        job.cancel()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Click zone button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ClickZoneButton(
    label   : String,
    modifier: Modifier,
    tint    : Color,
    onClick : () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(
                if (pressed) tint.copy(alpha = 0.45f)
                else         tint.copy(alpha = 0.13f)
            )
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true; onClick()
                    tryAwaitRelease(); pressed = false
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label,
            color      = Color.White.copy(alpha = 0.88f),
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign  = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Canvas helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawTrackpadGrid() {
    val c    = Color.White.copy(alpha = 0.022f)
    val step = 44.dp.toPx()
    var x = 0f
    while (x <= size.width)  { drawLine(c, Offset(x,0f), Offset(x,size.height), 1f); x += step }
    var y = 0f
    while (y <= size.height) { drawLine(c, Offset(0f,y), Offset(size.width,y),  1f); y += step }
}

private fun DrawScope.drawRipple(center: Offset, radius: Float, alpha: Float) {
    drawCircle(Color.White.copy(alpha = alpha * 0.35f), radius, center, style = Stroke(2.dp.toPx()))
    drawCircle(Color.White.copy(alpha = alpha * 0.10f), radius * 0.5f, center)
}

private fun DrawScope.drawCursorGhost(pos: Offset) {
    drawCircle(Color.White.copy(alpha = 0.28f), 13.dp.toPx(), pos, style = Stroke(1.5f.dp.toPx()))
    drawCircle(Color.White.copy(alpha = 0.10f), 5.dp.toPx(),  pos)
}

private fun DrawScope.drawMouseIcon(canvasSize: Size) {
    val cx = canvasSize.width  / 2f
    val cy = canvasSize.height / 2f - 24.dp.toPx()
    val w  = 30.dp.toPx(); val h = 44.dp.toPx(); val r = w / 2f
    val sc = Color.White.copy(alpha = 0.16f); val sw = 1.5f.dp.toPx()
    drawRoundRect(sc, Offset(cx-r, cy-h/2f), Size(w,h), CornerRadius(r), Stroke(sw))
    drawLine(sc, Offset(cx, cy-h/2f), Offset(cx, cy-h/2f+h*0.38f), sw)
    drawLine(sc, Offset(cx-r+2, cy-h/2f+h*0.38f), Offset(cx+r-2, cy-h/2f+h*0.38f), sw)
    val wTop = cy-h/2f+h*0.07f; val wBot = cy-h/2f+h*0.30f; val ww = 4.dp.toPx()
    drawRoundRect(sc, Offset(cx-ww/2f,wTop), Size(ww,wBot-wTop), CornerRadius(ww/2f), Stroke(sw))
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
        containerColor   = MaterialTheme.colorScheme.surface,
        dragHandle       = null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Title bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Trackpad Settings",
                    fontWeight = FontWeight.Bold, fontSize = 18.sp)
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            }

            HorizontalDivider()

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                PadSlider("Pointer Speed", local.pointerSpeed, 0.3f..3.0f,
                    { "%.1fx".format(it) }) { local = local.copy(pointerSpeed = it) }

                PadSlider("Scroll Speed", local.scrollSpeed, 0.3f..3.0f,
                    { "%.1fx".format(it) }) { local = local.copy(scrollSpeed = it) }

                PadToggle("Natural Scrolling",
                    "Content follows finger (like phone)",
                    local.naturalScrolling) { local = local.copy(naturalScrolling = it) }

                PadToggle("Tap to Click",
                    "Light short tap = left click",
                    local.tapToClick) { local = local.copy(tapToClick = it) }

                PadToggle("Two-Finger Right Click",
                    "Two-finger tap = right-click menu",
                    local.twoFingerRightClick) { local = local.copy(twoFingerRightClick = it) }

                PadToggle("Pointer Acceleration",
                    "Slow = precise, fast = covers distance",
                    local.accelerationEnabled) { local = local.copy(accelerationEnabled = it) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Click Pressure",
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else Color.Transparent),
                                border   = androidx.compose.foundation.BorderStroke(
                                    if (sel) 2.dp else 1.dp,
                                    if (sel) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline)
                            ) {
                                Text(cp.name.lowercase()
                                    .replaceFirstChar { it.uppercase() }, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
            }

            // ── Pinned Save ─────────────────────────────────────────────────
            HorizontalDivider()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Button(
                    onClick  = { onSave(local); onDismiss() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save Settings",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            // Bottom system nav inset
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun PadSlider(
    label  : String, value: Float,
    range  : ClosedFloatingPointRange<Float>,
    display: (Float) -> String,
    onChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(display(value), fontSize = 13.sp,
                color      = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium)
        }
        Slider(value = value, onValueChange = onChange,
            valueRange = range, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Slow", fontSize = 10.sp, color = Color.Gray)
            Text("Fast", fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun PadToggle(
    title   : String, subtitle: String,
    checked : Boolean, onToggle: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}