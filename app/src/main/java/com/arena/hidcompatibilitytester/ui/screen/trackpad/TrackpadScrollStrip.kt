package com.arena.hidcompatibilitytester.ui.screen.trackpad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun TrackpadScrollStrip(
    onScrollUp  : () -> Unit,
    onScrollDown: () -> Unit,
    onSendRef   : State<(Int, Int, Int, Int) -> Unit>,
    settingsRef : State<TrackpadSettings>,
    scrollFlash : Boolean,
    onFlash     : () -> Unit,
    scope       : kotlinx.coroutines.CoroutineScope,
) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .fillMaxHeight()
            .background(Color(0xFF081929))
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ScrollArrowButton("▲") { onScrollUp() }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0A1F33))
                    .pointerInput(Unit) {
                        var lastY = 0f; var isFirst = true
                        awaitPointerEventScope {
                            while (true) {
                                val ev = awaitPointerEvent(PointerEventPass.Initial)
                                val ch = ev.changes.firstOrNull() ?: continue
                                if (ch.pressed) {
                                    ch.consume()
                                    if (isFirst) { lastY = ch.position.y; isFirst = false }
                                    else {
                                        val s = settingsRef.value
                                        val dy = ch.position.y - lastY
                                        val dir = if (s.invertScroll) 1f else -1f
                                        val d = (dy * s.scrollSpeed * dir * 0.5f).toInt()
                                        if (d != 0) {
                                            onSendRef.value(0, 0, 0, d.coerceIn(-127, 127))
                                            scope.launch { onFlash() }
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
                    val cx = size.width / 2f
                    val alpha = if (scrollFlash) 0.55f else 0.13f
                    val col = Color.White.copy(alpha)
                    var y = 10f
                    while (y < size.height - 10f) {
                        drawLine(col, Offset(cx, y),
                            Offset(cx, (y + 7f).coerceAtMost(size.height - 10f)), 2.5f)
                        y += 13f
                    }
                    if (scrollFlash) {
                        drawRoundRect(Color(0xFF4A90D9).copy(0.45f),
                            Offset(5f, 5f),
                            Size(size.width - 10f, size.height - 10f),
                            CornerRadius(6f))
                    }
                }
                Text("↕", fontSize = 14.sp,
                    color = Color.White.copy(if (scrollFlash) 0.9f else 0.28f))
            }

            ScrollArrowButton("▼") { onScrollDown() }
        }
    }
}