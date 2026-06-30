package com.arena.hidcompatibilitytester.ui.screen.trackpad

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

import com.arena.hidcompatibilitytester.ui.screen.trackpad.TrackpadSettings
import com.arena.hidcompatibilitytester.ui.screen.trackpad.ClickPressure

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
    val physHoldActive = remember { mutableStateOf(false) }

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
        } else {
            TrackpadSurface(
                modifier       = Modifier.weight(1f),
                settings       = settings,
                onSendMouse    = onSendMouse,
                physHoldActive = physHoldActive
            )

            TrackpadClickButtons(
                onSendMouse    = onSendMouse,
                physHoldActive = physHoldActive
            )
        }
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
            LedBadge("LCK", settings.dragLockMode)
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
                        (if (settings.invertScroll) " · inv✓" else "") +
                        (if (settings.dragLockMode) " · lock✓" else " · hold-drag"),
                fontSize   = 11.sp,
                color      = Color(0xFF90CAF9),
                fontWeight = FontWeight.Medium,
                modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// TrackpadSurface — rewritten gesture engine
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun TrackpadSurface(
    modifier   : Modifier = Modifier,
    settings   : TrackpadSettings,
    onSendMouse: (dx: Int, dy: Int, buttons: Int, wheel: Int) -> Unit,
    physHoldActive : MutableState<Boolean>,   // <-- new parameter
) {
    val haptic  = LocalHapticFeedback.current
    val scope   = rememberCoroutineScope()
    val density = LocalDensity.current

    val onSendRef   = rememberUpdatedState(onSendMouse)
    val settingsRef = rememberUpdatedState(settings)

    // ── Tap thresholds ────────────────────────────────────────────────────────
    val TAP_MAX_MS  = 200L   // max duration for a tap
    val TAP_SLOP    = with(density) { 18.dp.toPx() }  // max movement for a tap
    val DTAP_GAP_MS = 350L   // max gap between two taps to count as double-tap
    // After double-tap-1 we wait this long for finger-2 before deciding it's
    // a normal double-click (not a drag gesture)
    val DRAG_WAIT_MS = 120L  // how long we wait for the 2nd tap to start

    // ── Visual state ──────────────────────────────────────────────────────────
    var fingerCount    by remember { mutableIntStateOf(0) }
    var cursorPos      by remember { mutableStateOf<Offset?>(null) }
    var dragActive     by remember { mutableStateOf(false) }
    var rightFlash     by remember { mutableStateOf(false) }
    var scrollFlash    by remember { mutableStateOf(false) }
    var scrollFlashJob by remember { mutableStateOf<Job?>(null) }

    val ripplePos    = remember { mutableStateOf<Offset?>(null) }
    val rippleRadius = remember { Animatable(0f) }
    val rippleAlpha  = remember { Animatable(0f) }

    // ── Drag state (read from both pointer-thread and coroutines) ─────────────
    //
    //  dragActiveRef  = left button is currently held down (we sent btn=1)
    //
    //  holdDragActive = we are in HOLD-TO-DRAG mode:
    //                   drag was started on tap-2 touchdown,
    //                   the button will be released as soon as the finger lifts
    //
    //  lockDragActive = we are in LOCK-DRAG mode:
    //                   drag was started on tap-2 touchdown,
    //                   finger can lift freely,
    //                   drag stays until the NEXT single tap
    //
    val dragActiveRef  = remember { mutableStateOf(false) }
    val holdDragActive = remember { mutableStateOf(false) }
    val lockDragActive = remember { mutableStateOf(false) }

    // ── Double-tap state ──────────────────────────────────────────────────────
    //
    //  dtapArmed      = tap-1 just completed cleanly, we are waiting to see
    //                   if tap-2 arrives within DTAP_GAP_MS
    //
    //  dtapArmJob     = coroutine that clears dtapArmed after the window expires
    //                   AND sends the deferred single-click if no tap-2 arrived
    //
    //  pendingSingleClick = we have sent btn=1 for tap-1 but not btn=0 yet,
    //                       because we need to wait for the double-tap window
    //
    // NOTE: for the click we do the following so that double-click on the PC
    // desktop opens a file correctly:
    //
    //   Tap-1  → wait DTAP_GAP_MS
    //              ├── no tap-2 → send click (press + release)
    //              └── tap-2 within window
    //                    ├── dragLockMode OFF → start hold-drag on tap-2 touchdown
    //                    └── dragLockMode ON  → start lock-drag on tap-2 touchdown,
    //                                           but ALSO emit a fast double-click
    //                                           so files/folders open on desktop
    //
    val dtapArmed           = remember { mutableStateOf(false) }
    val dtapArmJob          = remember { mutableStateOf<Job?>(null) }
    val lastTapMs           = remember { mutableLongStateOf(0L) }

    // Sync visual drag indicator
    LaunchedEffect(dragActiveRef.value) { dragActive = dragActiveRef.value }

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun send(dx: Int, dy: Int, btn: Int, w: Int) = onSendRef.value(dx, dy, btn, w)

    fun ripple(pos: Offset) = scope.launch {
        ripplePos.value = pos
        rippleRadius.snapTo(0f); rippleAlpha.snapTo(0.85f)
        rippleRadius.animateTo(120f, tween(320))
        rippleAlpha.animateTo(0f,   tween(200))
        ripplePos.value = null
    }

    fun flashScroll() {
        scrollFlashJob?.cancel()
        scrollFlashJob = scope.launch {
            scrollFlash = true; delay(400); scrollFlash = false
        }
    }

    /** Release whatever drag is active and send button-up */
    fun releaseDrag(pos: Offset? = null) {
        if (!dragActiveRef.value) return
        dragActiveRef.value  = false
        holdDragActive.value = false
        lockDragActive.value = false
        send(0, 0, 0, 0)
        if (pos != null) ripple(pos)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /** Start drag (presses left button) */
    fun startDrag(hold: Boolean, lock: Boolean, pos: Offset) {
        dragActiveRef.value  = true
        holdDragActive.value = hold
        lockDragActive.value = lock
        send(0, 0, 1, 0)
        ripple(pos)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /** Perform a quick left-click (press + release with small delay) */
    suspend fun quickClick(pos: Offset) {
        send(0, 0, 1, 0)
        delay(45)
        send(0, 0, 0, 0)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        ripple(pos)
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

        // ── Main touch area ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 40.dp)
                .pointerInput(Unit) {

                    // Active pointers: id → [downX, downY, prevX, prevY, curX, curY,
                    //                         downTimeMs, maxMove]
                    val pts = mutableMapOf<Long, FloatArray>()

                    var accX  = 0f
                    var accY  = 0f
                    var accS  = 0f
                    var prevN = 0

                    // Per-gesture tap tracking
                    var tapDownMs   = 0L
                    var tapMaxMove  = 0f
                    var tapDownN    = 0   // finger count at the time fingers went down

                    // For hold-drag: are we currently tracking a held tap-2?
                    // (finger is still down after double-tap was recognised)
                    var inTap2Hold  = false

                    awaitPointerEventScope {
                        while (true) {
                            val ev  = awaitPointerEvent(PointerEventPass.Initial)
                            val now = System.currentTimeMillis()
                            val s   = settingsRef.value

                            val pressing = ev.changes.filter { it.pressed }
                            val lifting  = ev.changes.filter {
                                !it.pressed && it.previousPressed
                            }

                            // ── Register / update pointers ────────────────────
                            for (ch in pressing) {
                                ch.consume()
                                val id = ch.id.value
                                val existing = pts[id]
                                if (existing == null) {
                                    val x = ch.position.x
                                    val y = ch.position.y
                                    pts[id] = floatArrayOf(
                                        x, y,   // [0,1] down pos
                                        x, y,   // [2,3] prev pos
                                        x, y,   // [4,5] cur pos
                                        now.toFloat(), // [6] down time
                                        0f       // [7] max move
                                    )
                                } else {
                                    existing[2] = existing[4]
                                    existing[3] = existing[5]
                                    existing[4] = ch.position.x
                                    existing[5] = ch.position.y
                                    val d = hypot(
                                        existing[4] - existing[0],
                                        existing[5] - existing[1]
                                    )
                                    if (d > existing[7]) existing[7] = d
                                }
                            }

                            val n   = pts.size
                            val all = pts.values.toList()

                            // ── Finger-count change ────────────────────────────
                            if (n != prevN) {
                                accX = 0f; accY = 0f; accS = 0f

                                if (n > prevN) {
                                    // Fingers added
                                    tapDownMs  = now
                                    tapMaxMove = 0f
                                    tapDownN   = n

                                    // ── HOLD-DRAG tap-2 detection ──────────────
                                    // If tap-1 just completed (dtapArmed) and a
                                    // new finger goes down within DTAP_GAP_MS,
                                    // this is tap-2: start the drag immediately.
                                    if (n == 1 &&
                                        dtapArmed.value &&
                                        !s.dragLockMode)
                                    {
                                        // Cancel the deferred single-click job
                                        dtapArmJob.value?.cancel()
                                        dtapArmJob.value  = null
                                        dtapArmed.value   = false
                                        inTap2Hold        = true
                                        val pos = Offset(all[0][4], all[0][5])
                                        scope.launch { startDrag(hold = true, lock = false, pos) }
                                    }

                                    // ── LOCK-DRAG tap-2 detection ──────────────
                                    if (n == 1 &&
                                        dtapArmed.value &&
                                        s.dragLockMode)
                                    {
                                        dtapArmJob.value?.cancel()
                                        dtapArmJob.value  = null
                                        dtapArmed.value   = false
                                        inTap2Hold        = false
                                        val pos = Offset(all[0][4], all[0][5])
                                        scope.launch {
                                            // Send a double-click so OS double-click
                                            // (open file/folder) also works.
                                            // We send press here; release after
                                            // the drag ends (or immediately if the
                                            // user just wanted a double-click).
                                            // Strategy: start drag immediately,
                                            // the OS sees btn held = drag.
                                            // If the user just tapped fast and
                                            // didn't move, we detect that on lift
                                            // and release quickly → OS sees
                                            // double-click.
                                            startDrag(hold = false, lock = true, pos)
                                        }
                                    }
                                }
                                prevN = n
                            }

                            // ── Track max movement ────────────────────────────
                            for (fp in all) {
                                val d = hypot(fp[4] - fp[0], fp[5] - fp[1])
                                if (d > tapMaxMove) tapMaxMove = d
                            }

                            // ── Update visual state ───────────────────────────
                            scope.launch {
                                fingerCount = n
                                cursorPos   = all.firstOrNull()
                                    ?.let { Offset(it[4], it[5]) }
                            }

                            // ── Movement ──────────────────────────────────────
                            when {
                                // ── 1 finger: move cursor ─────────────────────
                                n == 1 -> {
                                    val fp  = all[0]
                                    val dx  = fp[4] - fp[2]
                                    val dy  = fp[5] - fp[3]

                                    // btn=1 if: drag lock active, OR hold-drag
                                    // active, OR physical left button is held
                                    val btn = if (dragActiveRef.value ||
                                                  physHoldActive.value) 1 else 0

                                    accX += accel(dx, s.pointerSpeed)
                                    accY += accel(dy, s.pointerSpeed)
                                    val ix = accX.toInt()
                                    val iy = accY.toInt()
                                    if (ix != 0 || iy != 0) {
                                        send(
                                            ix.coerceIn(-127, 127),
                                            iy.coerceIn(-127, 127),
                                            btn, 0
                                        )
                                        accX -= ix; accY -= iy
                                    }
                                }

                                // ── 2 fingers: scroll ─────────────────────────
                                n == 2 -> {
                                    // Release any lock-drag when a second finger
                                    // is placed (user is scrolling, not dragging)
                                    if (lockDragActive.value) {
                                        scope.launch { releaseDrag() }
                                    }
                                    val dy0 = all[0][5] - all[0][3]
                                    val dy1 = all[1][5] - all[1][3]
                                    val avg = (dy0 + dy1) * 0.5f
                                    val dir = if (s.invertScroll) 1f else -1f
                                    accS += avg * s.scrollSpeed * dir * 0.6f
                                    val sw = accS.toInt()
                                    if (sw != 0) {
                                        send(0, 0, 0, sw.coerceIn(-127, 127))
                                        accS -= sw
                                        scope.launch { flashScroll() }
                                    }
                                }

                                // ── 3 fingers: fast pan ────────────────────────
                                n == 3 -> {
                                    if (dragActiveRef.value) {
                                        scope.launch { releaseDrag() }
                                    }
                                    val fp = all[0]
                                    val dx = fp[4] - fp[2]
                                    val dy = fp[5] - fp[3]
                                    accX += dx * s.pointerSpeed * 1.8f
                                    accY += dy * s.pointerSpeed * 1.8f
                                    val ix = accX.toInt()
                                    val iy = accY.toInt()
                                    if (ix != 0 || iy != 0) {
                                        send(
                                            ix.coerceIn(-127, 127),
                                            iy.coerceIn(-127, 127),
                                            0, 0
                                        )
                                        accX -= ix; accY -= iy
                                    }
                                }
                            }

                            // ── Releases ──────────────────────────────────────
                            for (ch in lifting) {
                                ch.consume()
                                pts.remove(ch.id.value)
                            }

                            if (lifting.isNotEmpty()) {
                                val liftPos  = lifting.first().position
                                    .let { Offset(it.x, it.y) }
                                val duration = now - tapDownMs
                                val wasTap   = duration in 1L..TAP_MAX_MS &&
                                               tapMaxMove < TAP_SLOP &&
                                               pts.isEmpty()

                                // ── HOLD-DRAG release ──────────────────────────
                                // If we are in hold-drag mode and finger lifted,
                                // stop the drag (regardless of tap or not).
                                if (pts.isEmpty() && holdDragActive.value) {
                                    inTap2Hold = false
                                    scope.launch { releaseDrag(liftPos) }
                                    accX = 0f; accY = 0f; accS = 0f; prevN = 0
                                    scope.launch { fingerCount = 0; cursorPos = null }
                                    continue   // skip all other tap logic
                                }

                                // ── LOCK-DRAG: tap to release ──────────────────
                                // If lock-drag is active and user does a clean tap,
                                // release the drag.
                                // If they tapped but barely moved (double-click
                                // intent) we handle it below.
                                if (wasTap && tapDownN == 1 &&
                                    lockDragActive.value)
                                {
                                    // Check: did the cursor move much during this
                                    // tap-release? If not, it might be the user
                                    // just wanted to confirm / stop dragging.
                                    scope.launch { releaseDrag(liftPos) }
                                    accX = 0f; accY = 0f; accS = 0f; prevN = 0
                                    scope.launch { fingerCount = 0; cursorPos = null }
                                    continue
                                }

                                // ── Normal tap logic ───────────────────────────
                                if (wasTap && s.tapToClick) {
                                    scope.launch {
                                        when {

                                            // ────────────────────────────────────
                                            // 1-finger tap
                                            // ────────────────────────────────────
                                            tapDownN == 1 -> {
                                                val gap = now - lastTapMs.longValue

                                                if (dtapArmed.value &&
                                                    gap in 30L..DTAP_GAP_MS)
                                                {
                                                    // ── This is tap-2 in a
                                                    //    double-tap sequence ──────
                                                    // (We reach here only in
                                                    //  LOCK mode — hold mode is
                                                    //  handled on touchdown above)
                                                    dtapArmJob.value?.cancel()
                                                    dtapArmJob.value = null
                                                    dtapArmed.value  = false

                                                    if (s.dragLockMode) {
                                                        // Lock mode: drag should
                                                        // already be active from
                                                        // the touchdown handler.
                                                        // If finger barely moved
                                                        // it's a double-click,
                                                        // not a drag — release
                                                        // immediately.
                                                        if (tapMaxMove < TAP_SLOP) {
                                                            // Treat as plain
                                                            // double-click
                                                            releaseDrag(liftPos)
                                                            // OS already saw the
                                                            // fast press from
                                                            // startDrag; send
                                                            // another quick click
                                                            // so it's 2 clicks
                                                            delay(30)
                                                            quickClick(liftPos)
                                                        }
                                                        // else: drag stays active,
                                                        // user is dragging
                                                    }
                                                } else {
                                                    // ── Tap-1: arm double-tap
                                                    //    window, defer the click ──
                                                    dtapArmed.value     = false
                                                    lastTapMs.longValue = now

                                                    val capturedPos = liftPos
                                                    dtapArmed.value  = true
                                                    val job = launch {
                                                        delay(DTAP_GAP_MS + 30)
                                                        if (dtapArmed.value) {
                                                            // No tap-2 arrived →
                                                            // fire single click
                                                            dtapArmed.value = false
                                                            quickClick(capturedPos)
                                                        }
                                                    }
                                                    dtapArmJob.value = job
                                                }
                                            }

                                            // ────────────────────────────────────
                                            // 2-finger tap → right-click
                                            // ────────────────────────────────────
                                            tapDownN >= 2 && s.twoFingerRightClick -> {
                                                if (dragActiveRef.value) releaseDrag()
                                                dtapArmed.value = false
                                                dtapArmJob.value?.cancel()
                                                dtapArmJob.value = null
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
                                    // Non-tap lift with all fingers up
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
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawTrackpadGrid()
                if (rightFlash) drawRect(Color(0xFF4A148C).copy(alpha = 0.22f))
                if (dragActive) {
                    drawRect(Color(0xFF1565C0).copy(alpha = 0.09f))
                    drawRoundRect(
                        Color(0xFF4A90D9).copy(alpha = 0.7f),
                        Offset(3f, 3f),
                        Size(size.width - 6f, size.height - 6f),
                        CornerRadius(12f),
                        style = Stroke(3f)
                    )
                }
                ripplePos.value?.let {
                    drawRipple(it, rippleRadius.value, rippleAlpha.value)
                }
                cursorPos?.let { drawCursorGhost(it) }
                drawMouseIcon(size)
            }

            // ── Drag status banner ────────────────────────────────────────────
            if (dragActive) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .background(
                            if (settings.dragLockMode)
                                Color(0xFF1565C0).copy(0.93f)
                            else
                                Color(0xFF6A1B9A).copy(0.93f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(
                        if (settings.dragLockMode)
                            "⬚ Drag Lock · tap to release"
                        else
                            "✋ Hold-Drag · lift to release",
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
                        .background(
                            Color.Black.copy(0.50f),
                            RoundedCornerShape(20.dp)
                        )
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
                ScrollArrowButton("▲") { onSendRef.value(0, 0, 0, 3) }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0A1F33))
                        .pointerInput(Unit) {
                            var lastY   = 0f
                            var isFirst = true
                            awaitPointerEventScope {
                                while (true) {
                                    val ev = awaitPointerEvent(
                                        PointerEventPass.Initial)
                                    val ch = ev.changes.firstOrNull() ?: continue
                                    if (ch.pressed) {
                                        ch.consume()
                                        if (isFirst) {
                                            lastY   = ch.position.y
                                            isFirst = false
                                        } else {
                                            val s   = settingsRef.value
                                            val dy  = ch.position.y - lastY
                                            val dir =
                                                if (s.invertScroll) 1f else -1f
                                            val d   = (dy * s.scrollSpeed *
                                                    dir * 0.5f).toInt()
                                            if (d != 0) {
                                                onSendRef.value(
                                                    0, 0, 0,
                                                    d.coerceIn(-127, 127)
                                                )
                                                scope.launch { flashScroll() }
                                            }
                                            lastY = ch.position.y
                                        }
                                    } else { isFirst = true }
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
                        "↕", fontSize = 14.sp,
                        color = Color.White.copy(if (scrollFlash) 0.9f else 0.28f)
                    )
                }

                ScrollArrowButton("▼") { onSendRef.value(0, 0, 0, -3) }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Click buttons  (left button now supports hold + drag)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun TrackpadClickButtons(
    onSendMouse: (dx: Int, dy: Int, buttons: Int, wheel: Int) -> Unit,
    physHoldActive : MutableState<Boolean>,   // <-- new parameter
) {
    val haptic  = LocalHapticFeedback.current
    val scope   = rememberCoroutineScope()
    val sendRef = rememberUpdatedState(onSendMouse)

    // Track whether the physical LEFT button is being held so the trackpad
    // surface can send btn=1 while the user drags with one finger.
    // We expose this via a shared state that TrackpadClickButtons owns.
    // Because TrackpadSurface is a sibling composable we pass it up through
    // the parent — but to keep this self-contained we hoist it here and let
    // the button directly control BLE output while held.

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0xFF081929))
    ) {
        // ── LEFT — supports hold-to-drag ──────────────────────────────────────
        HoldClickButton(
            label    = "Left",
            modifier = Modifier.weight(1f),
            tint     = Color(0xFF1565C0),
            btnBit   = 1,
            sendRef  = sendRef,
            haptic   = haptic,
            scope    = scope,
            physHoldActive = physHoldActive   // <-- pass down
        )
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.White.copy(0.10f))
        )
        // ── MIDDLE ────────────────────────────────────────────────────────────
        ClickZoneButton("Mid", Modifier.weight(0.6f), Color(0xFF1B5E20)) {
            sendRef.value(0, 0, 4, 0)
            scope.launch { delay(80); sendRef.value(0, 0, 0, 0) }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.White.copy(0.10f))
        )
        // ── RIGHT ─────────────────────────────────────────────────────────────
        ClickZoneButton("Right", Modifier.weight(1f), Color(0xFF4A148C)) {
            sendRef.value(0, 0, 2, 0)
            scope.launch { delay(80); sendRef.value(0, 0, 0, 0) }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

/**
 * Left button that sends btn=1 on press-down and btn=0 on release,
 * allowing the user to hold it while moving a finger on the trackpad
 * to perform a physical click-and-drag.
 */
@Composable
private fun HoldClickButton(
    label   : String,
    modifier: Modifier,
    tint    : Color,
    btnBit  : Int,
    sendRef : State<(Int, Int, Int, Int) -> Unit>,
    haptic  : androidx.compose.ui.hapticfeedback.HapticFeedback,
    scope   : kotlinx.coroutines.CoroutineScope,
    physHoldActive : MutableState<Boolean>,   // <-- new parameter
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (pressed) tint.copy(0.55f) else tint.copy(0.18f))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val ev = awaitPointerEvent(PointerEventPass.Initial)
                        for (ch in ev.changes) {
                            val wasPressed = ch.previousPressed
                            val isPressed  = ch.pressed

                            if (!wasPressed && isPressed) {
                                // Finger just went down
                                ch.consume()
                                pressed = true
                                physHoldActive.value   = true   // <-- set shared state
                                sendRef.value(0, 0, btnBit, 0)
                                haptic.performHapticFeedback(
                                    HapticFeedbackType.LongPress)
                            } else if (wasPressed && !isPressed) {
                                // Finger just lifted
                                ch.consume()
                                pressed = false
                                physHoldActive.value   = false  // <-- clear shared state
                                sendRef.value(0, 0, 0, 0)
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                color      = Color.White.copy(0.9f),
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign  = TextAlign.Center
            )
            if (pressed) {
                Text(
                    "● hold",
                    color    = Color.White.copy(0.6f),
                    fontSize = 9.sp
                )
            }
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
                        while (isActive) {
                            latestScroll.value(); delay(75)
                        }
                    }
                    tryAwaitRelease(); job.cancel()
                })
            },
        contentAlignment = Alignment.Center
    ) { Text(label, color = Color.White.copy(0.55f), fontSize = 13.sp) }
}

// ═════════════════════════════════════════════════════════════════════════════
// Click zone button (tap-only, no hold semantics needed)
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
                    pressed = true; latestClick.value()
                    tryAwaitRelease(); pressed = false
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
    while (x <= size.width)  {
        drawLine(c, Offset(x, 0f), Offset(x, size.height), 1f); x += step
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(c, Offset(0f, y), Offset(size.width, y), 1f);  y += step
    }
}

private fun DrawScope.drawRipple(center: Offset, radius: Float, alpha: Float) {
    drawCircle(Color.White.copy(alpha * 0.30f), radius, center,
        style = Stroke(2.dp.toPx()))
    drawCircle(Color.White.copy(alpha * 0.08f), radius * 0.4f, center)
}

private fun DrawScope.drawCursorGhost(pos: Offset) {
    drawCircle(Color.White.copy(0.22f), 14.dp.toPx(), pos,
        style = Stroke(1.5.dp.toPx()))
    drawCircle(Color.White.copy(0.08f),  5.dp.toPx(), pos)
}

private fun DrawScope.drawMouseIcon(canvasSize: Size) {
    val cx = canvasSize.width  / 2f
    val cy = canvasSize.height / 2f - 20.dp.toPx()
    val w  = 30.dp.toPx(); val h = 44.dp.toPx(); val r = w / 2f
    val sc = Color.White.copy(0.13f); val sw = 1.5.dp.toPx()
    drawRoundRect(sc, Offset(cx - r, cy - h / 2f), Size(w, h), CornerRadius(r), Stroke(sw))
    drawLine(sc, Offset(cx, cy - h / 2f), Offset(cx, cy - h / 2f + h * 0.38f), sw)
    drawLine(sc, Offset(cx - r + 2f, cy - h / 2f + h * 0.38f),
                 Offset(cx + r - 2f, cy - h / 2f + h * 0.38f), sw)
    val wTop = cy - h / 2f + h * 0.07f; val ww = 4.dp.toPx()
    drawRoundRect(sc, Offset(cx - ww / 2f, wTop), Size(ww, h * 0.23f),
        CornerRadius(ww / 2f), Stroke(sw))
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
                    { "%.1fx".format(it) }) { local = local.copy(pointerSpeed = it) }

                TrackpadSlider(
                    "Scroll Speed", local.scrollSpeed, 0.3f..3.0f,
                    { "%.1fx".format(it) }) { local = local.copy(scrollSpeed = it) }

                TrackpadToggle(
                    "Invert Scroll",
                    "Natural scrolling — content follows finger direction",
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

                SectionDivider("Double-Tap Drag")

                TrackpadToggle(
                    title    = "Drag Lock Mode",
                    subtitle = if (local.dragLockMode)
                        "ON — double-tap to start drag, tap again to release"
                    else
                        "OFF — double-tap then keep finger down to drag, lift to release",
                    checked  = local.dragLockMode
                ) { local = local.copy(dragLockMode = it) }

                DragModeExplainCard(local.dragLockMode)

                SectionDivider("Click Pressure")

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    cp.name.lowercase()
                                        .replaceFirstChar { it.uppercase() },
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

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun SectionDivider(title: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(0.10f))
        Text(
            title, fontSize = 11.sp,
            color      = Color(0xFF607D8B),
            fontWeight = FontWeight.Medium
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(0.10f))
    }
}

@Composable
private fun DragModeExplainCard(isLockMode: Boolean) {
    Surface(
        color    = if (isLockMode) Color(0xFF1565C0).copy(0.12f)
                   else            Color(0xFF6A1B9A).copy(0.12f),
        shape    = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                if (isLockMode) "⬚  Drag Lock" else "✋  Hold to Drag",
                fontWeight = FontWeight.Bold,
                fontSize   = 13.sp,
                color      = Color.White
            )
            if (isLockMode) {
                Text("1. Double-tap anywhere",
                    fontSize = 11.sp, color = Color(0xFFB0BEC5))
                Text("2. Lift finger — drag stays active",
                    fontSize = 11.sp, color = Color(0xFFB0BEC5))
                Text("3. Move cursor to drag",
                    fontSize = 11.sp, color = Color(0xFFB0BEC5))
                Text("4. Tap once to release drag",
                    fontSize = 11.sp, color = Color(0xFFB0BEC5))
                Text("(Quick double-tap with no move = normal double-click)",
                    fontSize = 10.sp, color = Color(0xFF607D8B))
            } else {
                Text("1. Double-tap anywhere",
                    fontSize = 11.sp, color = Color(0xFFB0BEC5))
                Text("2. On 2nd tap — keep finger held down",
                    fontSize = 11.sp, color = Color(0xFFB0BEC5))
                Text("3. Move finger to drag",
                    fontSize = 11.sp, color = Color(0xFFB0BEC5))
                Text("4. Lift finger to release drag",
                    fontSize = 11.sp, color = Color(0xFFB0BEC5))
            }
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
            Text(label, fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp, color = Color.White)
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
            Text(title,    fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp, color = Color.White)
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF607D8B))
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
private fun TrackpadNotReadyCard() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors   = CardDefaults.cardColors(
                containerColor = Color(0xFFF57F17).copy(0.1f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
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