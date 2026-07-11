package com.arena.hidcompatibilitytester.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Option F — Adaptive Hybrid Grid.
 *
 * Lays out children in a 2-column grid for landscape.
 * Small children (< threshold height) pair side-by-side.
 * Tall children get full width.
 *
 * Strategy:
 *  1. Measure all children with half-width constraints.
 *  2. Walk through children: if current + next both fit threshold, pair them in one row.
 *     Otherwise, give current child full width.
 *  3. Place all items.
 */
@Composable
fun AdaptiveHybridGrid(
    modifier: Modifier = Modifier,
    spacing: Int = 8,
    tallThresholdDp: Int = 250,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        if (measurables.isEmpty()) {
            return@Layout layout(constraints.maxWidth, 0) {}
        }

        val spacingPx = (spacing * density).toInt()
        val totalWidth = constraints.maxWidth
        val halfWidth = (totalWidth - spacingPx) / 2
        val tallThresholdPx = (tallThresholdDp * density).toInt()

        val fullConstraints = Constraints(
            minWidth = 0,
            maxWidth = totalWidth,
            minHeight = 0,
            maxHeight = constraints.maxHeight,
        )
        val halfConstraints = Constraints(
            minWidth = 0,
            maxWidth = halfWidth,
            minHeight = 0,
            maxHeight = constraints.maxHeight,
        )

        // Phase 1: probe heights using intrinsics — no measure() call
        val probeHeights = measurables.map { m ->
            m.maxIntrinsicHeight(halfWidth)
        }
        val isTall = probeHeights.map { it > tallThresholdPx }

        // Phase 2: decide row plan (unchanged logic)
        data class RowPlan(
            val indices: List<Int>,
            val fullWidth: Boolean,
        )

        val rows = mutableListOf<RowPlan>()
        var i = 0
        while (i < measurables.size) {
            when {
                isTall[i] -> {
                    rows.add(RowPlan(listOf(i), fullWidth = true))
                    i++
                }
                i + 1 < measurables.size && !isTall[i + 1] -> {
                    rows.add(RowPlan(listOf(i, i + 1), fullWidth = false))
                    i += 2
                }
                else -> {
                    rows.add(RowPlan(listOf(i), fullWidth = true))
                    i++
                }
            }
        }

        // Phase 3: measure each child ONCE with its correct final constraints
        // Build a map from child index → final constraints before measuring
        val finalConstraintsPerIndex = IntArray(measurables.size) { -1 } // -1 = half, 0 = full
        for (row in rows) {
            val useFullWidth = row.fullWidth || row.indices.size == 1
            for (idx in row.indices) {
                finalConstraintsPerIndex[idx] = if (useFullWidth) 0 else -1
            }
        }

        val placeables = measurables.mapIndexed { idx, m ->
            if (finalConstraintsPerIndex[idx] == 0) {
                m.measure(fullConstraints)
            } else {
                m.measure(halfConstraints)
            }
        }

        // Phase 4: compute positions
        data class PlacedItem(val x: Int, val y: Int, val index: Int)

        val placedItems = mutableListOf<PlacedItem>()
        var currentY = 0

        for (row in rows) {
            val useFullWidth = row.fullWidth || row.indices.size == 1

            if (useFullWidth) {
                for ((position, idx) in row.indices.withIndex()) {
                    if (position > 0) currentY += placeables[row.indices[position - 1]].height + spacingPx
                    placedItems.add(PlacedItem(0, currentY, idx))
                }
                currentY += placeables[row.indices.last()].height + spacingPx
            } else {
                val idx1 = row.indices[0]
                val idx2 = row.indices[1]
                val rowHeight = max(placeables[idx1].height, placeables[idx2].height)
                placedItems.add(PlacedItem(0, currentY, idx1))
                placedItems.add(PlacedItem(halfWidth + spacingPx, currentY, idx2))
                currentY += rowHeight + spacingPx
            }
        }

        val totalHeight = if (currentY > spacingPx) currentY - spacingPx else 0

        layout(totalWidth, totalHeight) {
            placedItems.forEach { item ->
                placeables[item.index].place(item.x, item.y)
            }
        }
    }
}