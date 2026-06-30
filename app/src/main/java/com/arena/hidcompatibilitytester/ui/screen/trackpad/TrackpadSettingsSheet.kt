package com.arena.hidcompatibilitytester.ui.screen.trackpad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Trackpad Settings", fontWeight = FontWeight.Bold,
                    fontSize = 18.sp, color = Color.White)
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFF90CAF9))
                }
            }
            HorizontalDivider(color = Color.White.copy(0.08f))

            // Content
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f, false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                SettingsSlider("Pointer Speed", local.pointerSpeed, 0.3f..3.0f,
                    { "%.1fx".format(it) }) { local = local.copy(pointerSpeed = it) }

                SettingsSlider("Scroll Speed", local.scrollSpeed, 0.3f..3.0f,
                    { "%.1fx".format(it) }) { local = local.copy(scrollSpeed = it) }

                SettingsToggle("Invert Scroll",
                    "Natural scrolling — content follows finger direction",
                    local.invertScroll) { local = local.copy(invertScroll = it) }

                SettingsToggle("Tap to Click", "Short tap = left click",
                    local.tapToClick) { local = local.copy(tapToClick = it) }

                SettingsToggle("Two-Finger Right Click",
                    "Two-finger tap = right-click menu",
                    local.twoFingerRightClick) { local = local.copy(twoFingerRightClick = it) }

                SettingsToggle("Pointer Acceleration",
                    "Slow = precise · Fast = covers distance",
                    local.accelerationEnabled) { local = local.copy(accelerationEnabled = it) }

                SectionDivider("Double-Tap Drag")

                SettingsToggle(
                    title = "Drag Lock Mode",
                    subtitle = if (local.dragLockMode)
                        "ON — double-tap to start drag, tap again to release"
                    else "OFF — double-tap then keep finger down, lift to release",
                    checked = local.dragLockMode
                ) { local = local.copy(dragLockMode = it) }

                DragModeExplainCard(local.dragLockMode)

                SectionDivider("Controls Layout")

                SettingsToggle(
                    "Arrow Keys",
                    "Show directional arrow buttons for precise cursor movement",
                    local.showArrowKeys
                ) { local = local.copy(showArrowKeys = it) }

                if (local.showArrowKeys) {
                    PositionSelector("Arrow Keys Position",
                        "Which edge of the screen to place the arrow buttons",
                        local.arrowPosition) { local = local.copy(arrowPosition = it) }
                }

                SettingsToggle(
                    "Scroll Strip",
                    "Show the scroll strip on the trackpad edge",
                    local.showScrollStrip
                ) { local = local.copy(showScrollStrip = it) }

                if (local.showScrollStrip) {
                    PositionSelector("Scroll Strip Position",
                        "Which side of the trackpad for the scroll strip",
                        local.scrollPosition) { local = local.copy(scrollPosition = it) }
                }

                SectionDivider("Keyboard Buttons")

                SettingsToggle(
                    "System Keyboard  🌐",
                    "Show toggle button for Android system keyboard — typed text is sent to host",
                    local.showSystemKeyboard
                ) { local = local.copy(showSystemKeyboard = it) }

                SettingsToggle(
                    "In-App Keyboard  ⌨",
                    "Show toggle button for built-in HID keyboard overlay",
                    local.showInAppKeyboard
                ) { local = local.copy(showInAppKeyboard = it) }

                Spacer(Modifier.height(4.dp))
            }

            HorizontalDivider(color = Color.White.copy(0.08f))

            Box(
                Modifier.fillMaxWidth().background(Color(0xFF111C28))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Button(
                    onClick = { onSave(local); onDismiss() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90D9))
                ) {
                    Text("Save Settings", fontSize = 15.sp,
                        fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

// ── Setting components ────────────────────────────────────────────────────

@Composable
private fun SectionDivider(title: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(0.10f))
        Text(title, fontSize = 11.sp, color = Color(0xFF607D8B),
            fontWeight = FontWeight.Medium)
        HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(0.10f))
    }
}

@Composable
private fun PositionSelector(
    title: String, subtitle: String, selected: SidePosition,
    onSelect: (SidePosition) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
        Text(subtitle, fontSize = 11.sp, color = Color(0xFF607D8B))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SidePosition.values().forEach { pos ->
                val sel = selected == pos
                val label = when (pos) {
                    SidePosition.LEFT -> "◀ Left"; SidePosition.RIGHT -> "Right ▶"
                }
                SettingsChoiceButton(label, sel) { onSelect(pos) }
            }
        }
    }
}

@Composable
private fun ClickPressureSelector(
    selected: ClickPressure,
    onSelect: (ClickPressure) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ClickPressure.values().forEach { cp ->
            val sel = selected == cp
            SettingsChoiceButton(
                cp.name.lowercase().replaceFirstChar { it.uppercase() }, sel
            ) { onSelect(cp) }
        }
    }
}

@Composable
private fun RowScope.SettingsChoiceButton(
    label: String, selected: Boolean, onClick: () -> Unit,
) {
    OutlinedButton(
        onClick  = onClick,
        modifier = Modifier.weight(1f),
        colors   = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Color(0xFF4A90D9).copy(0.15f)
                             else Color.Transparent
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) Color(0xFF4A90D9) else Color.White.copy(0.2f)
        )
    ) { Text(label, fontSize = 12.sp, color = Color.White) }
}

@Composable
private fun DragModeExplainCard(isLockMode: Boolean) {
    Surface(
        color = if (isLockMode) Color(0xFF1565C0).copy(0.12f)
                else Color(0xFF6A1B9A).copy(0.12f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                if (isLockMode) "⬚  Drag Lock" else "✋  Hold to Drag",
                fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White
            )
            val steps = if (isLockMode) listOf(
                "1. Double-tap anywhere",
                "2. Lift finger — drag stays active",
                "3. Move cursor to drag",
                "4. Tap once to release drag"
            ) else listOf(
                "1. Double-tap anywhere",
                "2. On 2nd tap — keep finger held down",
                "3. Move finger to drag",
                "4. Lift finger to release drag"
            )
            steps.forEach { Text(it, fontSize = 11.sp, color = Color(0xFFB0BEC5)) }
            if (isLockMode) {
                Text("(Quick double-tap with no move = normal double-click)",
                    fontSize = 10.sp, color = Color(0xFF607D8B))
            }
        }
    }
}

@Composable
private fun SettingsSlider(
    label: String, value: Float, range: ClosedFloatingPointRange<Float>,
    display: (Float) -> String, onChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
            Text(display(value), fontSize = 13.sp, color = Color(0xFF90CAF9),
                fontWeight = FontWeight.Medium)
        }
        Slider(
            value = value, onValueChange = onChange, valueRange = range,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF4A90D9),
                activeTrackColor = Color(0xFF4A90D9),
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
private fun SettingsToggle(
    title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF607D8B))
        }
        Switch(
            checked = checked, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF4A90D9),
                checkedTrackColor = Color(0xFF4A90D9).copy(0.5f),
                uncheckedThumbColor = Color(0xFF607D8B),
                uncheckedTrackColor = Color.White.copy(0.2f)
            )
        )
    }
}