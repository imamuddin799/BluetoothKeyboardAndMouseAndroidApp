package com.arena.hidcompatibilitytester.ui.screen.landscape.trackpad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LandscapeTrackpadClickButtons(
    onSendMouse: (dx: Int, dy: Int, buttons: Int, wheel: Int) -> Unit,
    physHoldActive: MutableState<Boolean>,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val sendRef = rememberUpdatedState(onSendMouse)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF081929))
    ) {
        LandscapeHoldClickButton(
            label = "Left",
            modifier = Modifier.weight(1f),
            tint = Color(0xFF1565C0),
            btnBit = 1,
            sendRef = sendRef,
            haptic = haptic,
            physHoldActive = physHoldActive
        )
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.White.copy(0.10f))
        )
        LandscapeClickZoneButton("Mid", Modifier.weight(0.6f), Color(0xFF1B5E20)) {
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
        LandscapeClickZoneButton("Right", Modifier.weight(1f), Color(0xFF4A148C)) {
            sendRef.value(0, 0, 2, 0)
            scope.launch { delay(80); sendRef.value(0, 0, 0, 0) }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}