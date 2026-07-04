package com.arena.hidcompatibilitytester.ui.screen.keyboard.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import kotlin.math.roundToInt

@Composable
fun <T> ReorderableSectionColumn(
    items: List<T>,
    enabled: Boolean,
    sectionSpacing: Dp = 3.dp,
    onReorder: (List<T>) -> Unit,
    itemContent: @Composable (index: Int, item: T, isDragged: Boolean) -> Unit,
) {
    if (!enabled) {
        // Just render normally, no drag
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
    var draggingIdx by remember { mutableIntStateOf(-1) }
    var dragYAccum by remember { mutableFloatStateOf(0f) }
    var lastTargetIdx by remember { mutableIntStateOf(-1) }
    var isCommitting by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // We need to measure actual section heights dynamically
    // For simplicity, estimate based on content
    val sectionHeights = remember { mutableStateMapOf<Int, Float>() }
    val spacingPx = with(density) { sectionSpacing.toPx() }

    fun getSlotCenter(index: Int): Float {
        var y = 0f
        for (i in 0 until index) {
            y += (sectionHeights[i] ?: 100f) + spacingPx
        }
        y += (sectionHeights[index] ?: 100f) / 2f
        return y
    }

    fun findTargetIndex(draggedIdx: Int, accumY: Float): Int {
        val draggedCenter = getSlotCenter(draggedIdx) + accumY
        var closest = draggedIdx
        var minDist = Float.MAX_VALUE
        for (i in orderList.indices) {
            val center = getSlotCenter(i)
            val dist = kotlin.math.abs(draggedCenter - center)
            if (dist < minDist) {
                minDist = dist
                closest = i
            }
        }
        return closest
    }

    val targetIdx = if (draggingIdx >= 0) {
        findTargetIndex(draggingIdx, dragYAccum)
    } else -1

    LaunchedEffect(targetIdx) { if (targetIdx >= 0) lastTargetIdx = targetIdx }

    val offsetAnimatables = remember {
        mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>()
    }

    orderList.indices.forEach { idx ->
        if (!offsetAnimatables.containsKey(idx)) {
            offsetAnimatables[idx] = Animatable(0f)
        }
    }

    // Update offsets
    orderList.indices.forEach { index ->
        val isDragged = draggingIdx == index

        val targetOffsetPx = when {
            isDragged -> dragYAccum
            draggingIdx < 0 -> 0f
            targetIdx > draggingIdx && index in (draggingIdx + 1)..targetIdx -> {
                -(sectionHeights[draggingIdx] ?: 100f) - spacingPx
            }
            targetIdx < draggingIdx && index in targetIdx until draggingIdx -> {
                (sectionHeights[draggingIdx] ?: 100f) + spacingPx
            }
            else -> 0f
        }

        LaunchedEffect(index, targetOffsetPx, isDragged) {
            if (isCommitting) return@LaunchedEffect
            val anim = offsetAnimatables[index] ?: return@LaunchedEffect
            if (isDragged) {
                anim.snapTo(targetOffsetPx)
            } else {
                anim.animateTo(
                    targetOffsetPx,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
                )
            }
        }
    }

    fun commitReorder() {
        val from = draggingIdx
        val to = lastTargetIdx

        draggingIdx = -1
        dragYAccum = 0f
        lastTargetIdx = -1

        if (from < 0 || to < 0 || to == from) {
            coroutineScope.launch {
                offsetAnimatables[from]?.animateTo(
                    0f, spring(dampingRatio = 0.7f, stiffness = 250f)
                )
            }
            return
        }

        val newList = orderList.toMutableList()
        val item = newList.removeAt(from)
        newList.add(to, item)

        coroutineScope.launch {
            isCommitting = true
            // Snap all to 0
            orderList.indices.forEach { idx ->
                offsetAnimatables[idx]?.snapTo(0f)
            }
            orderList = newList
            onReorder(newList)
            isCommitting = false
        }
    }

    fun cancelDrag() {
        val idx = draggingIdx
        draggingIdx = -1
        dragYAccum = 0f
        lastTargetIdx = -1
        coroutineScope.launch {
            offsetAnimatables[idx]?.animateTo(
                0f, spring(dampingRatio = 0.65f, stiffness = 200f)
            )
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(sectionSpacing),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(orderList, enabled) {
                if (!enabled) return@pointerInput
                detectDragGesturesAfterLongPress(
                    onDragStart = { startOffset ->
                        if (isCommitting) return@detectDragGesturesAfterLongPress
                        var accumulated = 0f
                        for (i in orderList.indices) {
                            val h = sectionHeights[i] ?: 100f
                            if (startOffset.y < accumulated + h + spacingPx) {
                                draggingIdx = i
                                dragYAccum = 0f
                                lastTargetIdx = i
                                break
                            }
                            accumulated += h + spacingPx
                        }
                    },
                    onDrag = { change, amount ->
                        if (draggingIdx >= 0 && !isCommitting) {
                            change.consume()
                            dragYAccum += amount.y
                        }
                    },
                    onDragEnd = { if (!isCommitting) commitReorder() },
                    onDragCancel = { if (!isCommitting) cancelDrag() },
                )
            }
    ) {
        orderList.forEachIndexed { index, item ->
            val isDragged = draggingIdx == index
            val offsetDp = with(density) {
                (offsetAnimatables[index]?.value ?: 0f).toDp()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isDragged) 10f else 0f)
                    .offset(y = offsetDp)
                    .scale(if (isDragged) 1.02f else 1f)
                    .onGloballyPositioned { coordinates ->
                        sectionHeights[index] = coordinates.size.height.toFloat()
                    }
            ) {
                if (isDragged) {
                    Surface(
                        color = Color(0xFF4A90D9).copy(0.08f),
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