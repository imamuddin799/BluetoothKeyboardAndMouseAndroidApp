package com.arena.hidcompatibilitytester.ui.screen.landscape.trackpad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun LandscapeArrowKeysPad(
    modifier: Modifier = Modifier,
    onMove: (dx: Int, dy: Int) -> Unit,
) {
    val step = 4

    Column(
        modifier = modifier
            .background(Color(0xFF0A1929).copy(0.92f), RoundedCornerShape(6.dp))
            .border(0.5.dp, Color.White.copy(0.10f), RoundedCornerShape(6.dp))
            .padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        LandscapeArrowBtn("▲", Modifier.size(44.dp)) { onMove(0, -step) }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            LandscapeArrowBtn("◀", Modifier.size(44.dp)) { onMove(-step, 0) }
            LandscapeArrowBtn("▼", Modifier.size(44.dp)) { onMove(0, step) }
            LandscapeArrowBtn("▶", Modifier.size(44.dp)) { onMove(step, 0) }
        }
    }
}

@Composable
private fun LandscapeArrowBtn(
    label: String,
    modifier: Modifier = Modifier.size(44.dp),
    onAction: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    var holdJob by remember { mutableStateOf<Job?>(null) }

    Box(
        modifier = modifier
            .background(
                if (pressed) Color(0xFF1565C0).copy(0.7f) else Color(0xFF0F2840),
                RoundedCornerShape(5.dp)
            )
            .border(0.5.dp, Color.White.copy(0.18f), RoundedCornerShape(5.dp))
            .pointerInput(label) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        onAction()
                        holdJob = scope.launch {
                            delay(280)
                            while (isActive) { onAction(); delay(30) }
                        }
                        tryAwaitRelease()
                        pressed = false
                        holdJob?.cancel()
                        holdJob = null
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 16.sp,
            color = Color.White.copy(if (pressed) 1f else 0.85f),
            fontWeight = FontWeight.Bold
        )
    }
}