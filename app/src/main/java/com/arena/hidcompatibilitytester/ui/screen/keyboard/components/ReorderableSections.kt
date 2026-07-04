package com.arena.hidcompatibilitytester.ui.screen.keyboard.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val SCROLL_ZONE_DP = 72.dp
private const val MAX_SPEED = 20f
private const val AUTO_SCROLL_TICK_MS = 16L

// 0.35f means reorder once ~35% of the neighbour item is crossed
private const val SWAP_THRESHOLD_RATIO = 0.35f

@Composable
fun <T> ReorderableSectionColumn(
    items: List<T>,
    enabled: Boolean,
    scrollState: ScrollState,
    viewportTopPx: Float,
    viewportBottomPx: Float,
    sectionSpacing: Dp = 3.dp,
    onReorder: (List<T>) -> Unit,
    itemContent: @Composable (index: Int, item: T, isDragged: Boolean) -> Unit,
) {
    if (!enabled) {
        Column(
            verticalArrangement = Arrangement.spacedBy(sectionSpacing),
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEachIndexed { index, item ->
                itemContent(index, item, false)
            }
        }
        return
    }

    var orderList by remember(items) { mutableStateOf(items.toList()) }
    var draggingIdx by remember { mutableStateOf(-1) }
    var dragYAccum by remember { mutableStateOf(0f) }
    var lastTargetIdx by remember { mutableStateOf(-1) }
    var isCommitting by remember { mutableStateOf(false) }

    var fingerYInRoot by remember { mutableStateOf(0f) }

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val scrollZonePx = with(density) { SCROLL_ZONE_DP.toPx() }
    val spacingPx = with(density) { sectionSpacing.toPx() }

    val sectionHeights = remember { mutableStateMapOf<Int, Float>() }
    val offsetAnims = remember { mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>() }

    orderList.indices.forEach { i ->
        offsetAnims.getOrPut(i) { Animatable(0f) }
    }

    LaunchedEffect(orderList.size) {
        val valid = orderList.indices.toSet()
        offsetAnims.keys.toList().forEach { if (it !in valid) offsetAnims.remove(it) }
        sectionHeights.keys.toList().forEach { if (it !in valid) sectionHeights.remove(it) }
    }

    var autoScrollJob by remember { mutableStateOf<Job?>(null) }

    fun stopAutoScroll() {
        autoScrollJob?.cancel()
        autoScrollJob = null
    }

    fun calcScrollSpeed(): Float {
        val distTop = fingerYInRoot - viewportTopPx
        val distBottom = viewportBottomPx - fingerYInRoot

        return when {
            distTop in 0f..scrollZonePx ->
                -MAX_SPEED * (1f - distTop / scrollZonePx)

            distBottom in 0f..scrollZonePx ->
                MAX_SPEED * (1f - distBottom / scrollZonePx)

            else -> 0f
        }
    }

    fun startAutoScrollIfNeeded() {
        if (draggingIdx < 0 || isCommitting) {
            stopAutoScroll()
            return
        }

        val speed = calcScrollSpeed()
        if (speed == 0f) {
            stopAutoScroll()
            return
        }

        if (autoScrollJob?.isActive == true) return

        autoScrollJob = scope.launch {
            while (isActive && draggingIdx >= 0 && !isCommitting) {
                val spd = calcScrollSpeed()
                if (spd == 0f) break

                val current = scrollState.value
                val target = (current + spd).roundToInt()
                    .coerceIn(0, scrollState.maxValue)

                if (target == current) break

                scrollState.scrollTo(target)
                delay(AUTO_SCROLL_TICK_MS)
            }
            autoScrollJob = null
        }
    }

    fun getSlotTop(index: Int): Float {
        var y = 0f
        for (i in 0 until index) {
            y += (sectionHeights[i] ?: 100f) + spacingPx
        }
        return y
    }

    fun getItemHeight(index: Int): Float {
        return sectionHeights[index] ?: 100f
    }

    /**
     * Finds target index using edge-crossing threshold instead of waiting too long.
     *
     * Dragging downward:
     *   swap once dragged bottom crosses ~35% of next item
     *
     * Dragging upward:
     *   swap once dragged top crosses ~35% of previous item from its bottom side
     */
    fun findTargetIndex(draggedIdx: Int, accumY: Float): Int {
        val draggedTop = getSlotTop(draggedIdx) + accumY
        val draggedHeight = getItemHeight(draggedIdx)
        val draggedBottom = draggedTop + draggedHeight

        var target = draggedIdx

        if (accumY > 0f) {
            // Moving downward
            while (target < orderList.lastIndex) {
                val next = target + 1
                val nextTop = getSlotTop(next)
                val nextHeight = getItemHeight(next)

                val swapLine = nextTop + (nextHeight * SWAP_THRESHOLD_RATIO)

                if (draggedBottom > swapLine) {
                    target++
                } else {
                    break
                }
            }
        } else if (accumY < 0f) {
            // Moving upward
            while (target > 0) {
                val prev = target - 1
                val prevTop = getSlotTop(prev)
                val prevHeight = getItemHeight(prev)

                val swapLine = prevTop + (prevHeight * (1f - SWAP_THRESHOLD_RATIO))

                if (draggedTop < swapLine) {
                    target--
                } else {
                    break
                }
            }
        }

        return target
    }

    val targetIdx =
        if (draggingIdx >= 0) findTargetIndex(draggingIdx, dragYAccum) else -1

    LaunchedEffect(targetIdx) {
        if (targetIdx >= 0) lastTargetIdx = targetIdx
    }

    LaunchedEffect(draggingIdx) {
        if (draggingIdx < 0) return@LaunchedEffect

        var previousScroll = scrollState.value

        snapshotFlow { scrollState.value }.collect { currentScroll ->
            val delta = currentScroll - previousScroll
            previousScroll = currentScroll

            if (draggingIdx >= 0 && delta != 0 && !isCommitting) {
                // keep dragged item visually under finger during auto-scroll
                dragYAccum += delta.toFloat()
            }
        }
    }

    orderList.indices.forEach { index ->
        val isDragged = draggingIdx == index
        val draggedHeight = if (draggingIdx >= 0) getItemHeight(draggingIdx) else 0f

        val targetPx = when {
            isDragged -> dragYAccum
            draggingIdx < 0 -> 0f

            targetIdx > draggingIdx &&
                index in (draggingIdx + 1)..targetIdx ->
                -(draggedHeight + spacingPx)

            targetIdx < draggingIdx &&
                index in targetIdx until draggingIdx ->
                draggedHeight + spacingPx

            else -> 0f
        }

        LaunchedEffect(index, targetPx, isDragged, isCommitting) {
            if (isCommitting) return@LaunchedEffect
            val anim = offsetAnims[index] ?: return@LaunchedEffect

            if (isDragged) {
                anim.snapTo(targetPx)
            } else {
                anim.animateTo(
                    targetPx,
                    spring(dampingRatio = 0.8f, stiffness = 300f)
                )
            }
        }
    }

    fun commitReorder() {
        stopAutoScroll()

        val from = draggingIdx
        val to = lastTargetIdx

        draggingIdx = -1
        dragYAccum = 0f
        lastTargetIdx = -1

        if (from < 0) return

        if (to < 0 || to == from) {
            scope.launch {
                offsetAnims[from]?.animateTo(
                    0f,
                    spring(dampingRatio = 0.7f, stiffness = 250f)
                )
            }
            return
        }

        val newList = orderList.toMutableList().also {
            val moved = it.removeAt(from)
            it.add(to, moved)
        }

        scope.launch {
            isCommitting = true
            orderList.indices.forEach { idx -> offsetAnims[idx]?.snapTo(0f) }
            orderList = newList
            onReorder(newList)
            isCommitting = false
        }
    }

    fun cancelDrag() {
        stopAutoScroll()

        val idx = draggingIdx
        draggingIdx = -1
        dragYAccum = 0f
        lastTargetIdx = -1

        if (idx >= 0) {
            scope.launch {
                offsetAnims[idx]?.animateTo(
                    0f,
                    spring(dampingRatio = 0.65f, stiffness = 200f)
                )
            }
        }
    }

    var columnTopInRoot by remember { mutableStateOf(0f) }

    Column(
        verticalArrangement = Arrangement.spacedBy(sectionSpacing),
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                columnTopInRoot = it.positionInRoot().y
            }
            .pointerInput(orderList, enabled) {
                if (!enabled) return@pointerInput

                detectDragGesturesAfterLongPress(
                    onDragStart = { startOffset ->
                        if (isCommitting) return@detectDragGesturesAfterLongPress

                        var accumulated = 0f
                        for (i in orderList.indices) {
                            val h = getItemHeight(i)
                            if (startOffset.y <= accumulated + h) {
                                draggingIdx = i
                                dragYAccum = 0f
                                lastTargetIdx = i
                                fingerYInRoot = columnTopInRoot + startOffset.y
                                break
                            }
                            accumulated += h + spacingPx
                        }
                    },
                    onDrag = { change, amount ->
                        if (draggingIdx < 0 || isCommitting) {
                            return@detectDragGesturesAfterLongPress
                        }

                        change.consume()
                        dragYAccum += amount.y
                        fingerYInRoot += amount.y
                        startAutoScrollIfNeeded()
                    },
                    onDragEnd = {
                        if (!isCommitting) commitReorder()
                    },
                    onDragCancel = {
                        if (!isCommitting) cancelDrag()
                    }
                )
            }
    ) {
        orderList.forEachIndexed { index, item ->
            val isDragged = draggingIdx == index
            val offsetDp = with(density) { (offsetAnims[index]?.value ?: 0f).toDp() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isDragged) 10f else 0f)
                    .offset(y = offsetDp)
                    .scale(if (isDragged) 1.02f else 1f)
                    .onGloballyPositioned {
                        sectionHeights[index] = it.size.height.toFloat()
                    }
            ) {
                if (isDragged) {
                    Surface(
                        color = Color(0xFF4A90D9).copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(Modifier.fillMaxWidth()) {
                            itemContent(index, item, true)
                        }
                    }
                } else {
                    itemContent(index, item, false)
                }
            }
        }
    }
}