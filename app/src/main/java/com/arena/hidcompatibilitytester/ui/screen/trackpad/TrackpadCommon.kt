package com.arena.hidcompatibilitytester.ui.screen.trackpad

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun LedBadge(label: String, active: Boolean) {
    Surface(
        shape = RoundedCornerShape(3.dp),
        color = if (active) Color(0xFF1565C0) else Color.White.copy(0.04f)
    ) {
        Text(
            label, fontSize = 7.sp,
            color      = if (active) Color.White else Color(0xFF3A4A5A),
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            modifier   = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun TrackpadNotReadyCard() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors   = CardDefaults.cardColors(containerColor = Color(0xFFF57F17).copy(0.1f))
        ) {
            Column(
                modifier            = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⏳", fontSize = 32.sp)
                Text("Not Connected", fontWeight = FontWeight.Bold,
                    fontSize = 16.sp, color = Color.White)
                Text(
                    "Start BLE HID → pair from host Bluetooth settings → come back here.",
                    fontSize = 13.sp, color = Color(0xFF607D8B), textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun HoldClickButton(
    label          : String,
    modifier       : Modifier,
    tint           : Color,
    btnBit         : Int,
    sendRef        : State<(Int, Int, Int, Int) -> Unit>,
    haptic         : androidx.compose.ui.hapticfeedback.HapticFeedback,
    physHoldActive : MutableState<Boolean>,
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
                            if (!ch.previousPressed && ch.pressed) {
                                ch.consume(); pressed = true; physHoldActive.value = true
                                sendRef.value(0, 0, btnBit, 0)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } else if (ch.previousPressed && !ch.pressed) {
                                ch.consume(); pressed = false; physHoldActive.value = false
                                sendRef.value(0, 0, 0, 0)
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color.White.copy(0.9f), fontSize = 13.sp,
                fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            if (pressed) Text("● hold", color = Color.White.copy(0.6f), fontSize = 9.sp)
        }
    }
}

@Composable
fun ClickZoneButton(
    label   : String,
    modifier: Modifier,
    tint    : Color,
    onClick : () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val latest  = rememberUpdatedState(onClick)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (pressed) tint.copy(0.5f) else tint.copy(0.15f))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true; latest.value()
                    tryAwaitRelease(); pressed = false
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White.copy(0.9f), fontSize = 13.sp,
            fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

@Composable
fun ScrollArrowButton(label: String, onScroll: () -> Unit) {
    val scope  = rememberCoroutineScope()
    val latest = rememberUpdatedState(onScroll)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color(0xFF0F2840))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    latest.value()
                    val job = scope.launch {
                        delay(380); while (isActive) { latest.value(); delay(75) }
                    }
                    tryAwaitRelease(); job.cancel()
                })
            },
        contentAlignment = Alignment.Center
    ) { Text(label, color = Color.White.copy(0.55f), fontSize = 13.sp) }
}