package com.arena.hidcompatibilitytester.ui.screen.landscape.trackpad

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

fun DrawScope.landscapeDrawTrackpadGrid() {
    val c = Color.White.copy(alpha = 0.020f)
    val step = 44.dp.toPx()
    var x = 0f
    while (x <= size.width) {
        drawLine(c, Offset(x, 0f), Offset(x, size.height), 1f); x += step
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(c, Offset(0f, y), Offset(size.width, y), 1f); y += step
    }
}

fun DrawScope.landscapeDrawRipple(center: Offset, radius: Float, alpha: Float) {
    drawCircle(Color.White.copy(alpha * 0.30f), radius, center, style = Stroke(2.dp.toPx()))
    drawCircle(Color.White.copy(alpha * 0.08f), radius * 0.4f, center)
}

fun DrawScope.landscapeDrawCursorGhost(pos: Offset) {
    drawCircle(Color.White.copy(0.22f), 14.dp.toPx(), pos, style = Stroke(1.5.dp.toPx()))
    drawCircle(Color.White.copy(0.08f), 5.dp.toPx(), pos)
}

fun DrawScope.landscapeDrawMouseIcon(canvasSize: Size) {
    val cx = canvasSize.width / 2f
    val cy = canvasSize.height / 2f - 20.dp.toPx()
    val w = 30.dp.toPx(); val h = 44.dp.toPx(); val r = w / 2f
    val sc = Color.White.copy(0.13f); val sw = 1.5.dp.toPx()
    drawRoundRect(sc, Offset(cx - r, cy - h / 2f), Size(w, h), CornerRadius(r), Stroke(sw))
    drawLine(sc, Offset(cx, cy - h / 2f), Offset(cx, cy - h / 2f + h * 0.38f), sw)
    drawLine(
        sc, Offset(cx - r + 2f, cy - h / 2f + h * 0.38f),
        Offset(cx + r - 2f, cy - h / 2f + h * 0.38f), sw
    )
    val wTop = cy - h / 2f + h * 0.07f; val ww = 4.dp.toPx()
    drawRoundRect(
        sc, Offset(cx - ww / 2f, wTop), Size(ww, h * 0.23f),
        CornerRadius(ww / 2f), Stroke(sw)
    )
}