package com.arena.hidcompatibilitytester.ui.screen.trackpad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class SettingsSection(val title: String) {
    POINTER("Pointer"),
    KEYBOARD("Keyboard"),
    LAYOUT("Layout"),
    GESTURES("Gestures")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackpadSettingsSheet(
    settings: TrackpadSettings,
    onDismiss: () -> Unit,
    onSave: (TrackpadSettings) -> Unit,
) {
    var local by remember(settings) { mutableStateOf(settings) }
    var selectedSection by remember { mutableStateOf(SettingsSection.POINTER) }

    Scaffold(
        containerColor = Color(0xFF111C28),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Trackpad Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { local = TrackpadSettings() }) {
                        Text("Defaults", color = Color(0xFF90CAF9), fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF111C28)
                )
            )
        },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111C28))
            ) {
                HorizontalDivider(color = Color.White.copy(0.08f))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = {
                            onSave(local)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4A90D9)
                        )
                    ) {
                        Text(
                            "Save Settings",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Section tabs — same style as main app tabs
            TabRow(
                selectedTabIndex = selectedSection.ordinal,
                containerColor = Color(0xFF111C28),
                contentColor = Color.White,
                divider = { HorizontalDivider(color = Color.White.copy(0.08f)) }
            ) {
                SettingsSection.values().forEachIndexed { index, section ->
                    Tab(
                        selected = selectedSection == section,
                        onClick = { selectedSection = section },
                        text = {
                            Text(
                                section.title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedSection == section)
                                    FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = Color(0xFF4A90D9),
                        unselectedContentColor = Color(0xFFB0BEC5)
                    )
                }
            }

            // Section content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                when (selectedSection) {
                    SettingsSection.POINTER -> PointerSection(local) {
                        local = it
                    }

                    SettingsSection.KEYBOARD -> KeyboardSection(local) {
                        local = it
                    }

                    SettingsSection.LAYOUT -> LayoutSection(local) {
                        local = it
                    }

                    SettingsSection.GESTURES -> GesturesSection(local) {
                        local = it
                    }
                }

                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ── Section composables ───────────────────────────────────────────────

@Composable
private fun PointerSection(
    settings: TrackpadSettings,
    onChange: (TrackpadSettings) -> Unit,
) {
    SettingsSlider(
        label = "Pointer Speed",
        value = settings.pointerSpeed,
        range = 0.3f..3.0f,
        display = { "%.1fx".format(it) }
    ) {
        onChange(settings.copy(pointerSpeed = it))
    }

    SettingsSlider(
        label = "Scroll Speed",
        value = settings.scrollSpeed,
        range = 0.3f..3.0f,
        display = { "%.1fx".format(it) }
    ) {
        onChange(settings.copy(scrollSpeed = it))
    }

    SettingsToggle(
        "Invert Scroll",
        "Natural scrolling — content follows finger direction",
        settings.invertScroll
    ) {
        onChange(settings.copy(invertScroll = it))
    }

    SettingsToggle(
        "Pointer Acceleration",
        "Slow = precise · Fast = covers distance",
        settings.accelerationEnabled
    ) {
        onChange(settings.copy(accelerationEnabled = it))
    }
}

@Composable
private fun KeyboardSection(
    settings: TrackpadSettings,
    onChange: (TrackpadSettings) -> Unit,
) {
    SettingsToggle(
        "System Keyboard  🌐",
        "Show toggle button for Android system keyboard — typed text is sent to host",
        settings.showSystemKeyboard
    ) {
        onChange(settings.copy(showSystemKeyboard = it))
    }

    SettingsToggle(
        "In-App Keyboard  ⌨",
        "Show toggle button for built-in HID keyboard overlay",
        settings.showInAppKeyboard
    ) {
        onChange(settings.copy(showInAppKeyboard = it))
    }
}

@Composable
private fun LayoutSection(
    settings: TrackpadSettings,
    onChange: (TrackpadSettings) -> Unit,
) {
    SettingsToggle(
        "Arrow Keys",
        "Show directional arrow buttons for precise cursor movement",
        settings.showArrowKeys
    ) {
        onChange(settings.copy(showArrowKeys = it))
    }

    if (settings.showArrowKeys) {
        PositionSelector(
            title = "Arrow Keys Position",
            subtitle = "Choose which side shows the arrow keys",
            selected = settings.arrowPosition
        ) {
            onChange(settings.copy(arrowPosition = it))
        }
    }

    SettingsToggle(
        "Scroll Strip",
        "Show the scroll strip on the trackpad edge",
        settings.showScrollStrip
    ) {
        onChange(settings.copy(showScrollStrip = it))
    }

    if (settings.showScrollStrip) {
        PositionSelector(
            title = "Scroll Strip Position",
            subtitle = "Choose which side shows the scroll strip",
            selected = settings.scrollPosition
        ) {
            onChange(settings.copy(scrollPosition = it))
        }
    }
}

@Composable
private fun GesturesSection(
    settings: TrackpadSettings,
    onChange: (TrackpadSettings) -> Unit,
) {
    SettingsToggle(
        "Tap to Click",
        "Short tap = left click",
        settings.tapToClick
    ) {
        onChange(settings.copy(tapToClick = it))
    }

    SettingsToggle(
        "Two-Finger Right Click",
        "Two-finger tap = right-click menu",
        settings.twoFingerRightClick
    ) {
        onChange(settings.copy(twoFingerRightClick = it))
    }

    SectionDivider("Double-Tap Drag")

    SettingsToggle(
        title = "Drag Lock Mode",
        subtitle = if (settings.dragLockMode)
            "ON — double-tap to start drag, tap again to release"
        else
            "OFF — double-tap then keep finger down, lift to release",
        checked = settings.dragLockMode
    ) {
        onChange(settings.copy(dragLockMode = it))
    }

    DragModeExplainCard(settings.dragLockMode)
}

// ── Reusable setting components ───────────────────────────────────────

@Composable
private fun SectionDivider(title: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(0.10f))
        Text(
            title, fontSize = 11.sp, color = Color(0xFF607D8B),
            fontWeight = FontWeight.Medium
        )
        HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(0.10f))
    }
}

@Composable
private fun PositionSelector(
    title: String,
    subtitle: String,
    selected: SidePosition,
    onSelect: (SidePosition) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
        Text(subtitle, fontSize = 11.sp, color = Color(0xFF607D8B))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SidePosition.values().forEach { pos ->
                val sel = selected == pos
                val label = when (pos) {
                    SidePosition.LEFT -> "◀ Left"
                    SidePosition.RIGHT -> "Right ▶"
                }
                SettingsChoiceButton(label, sel) { onSelect(pos) }
            }
        }
    }
}

@Composable
private fun RowScope.SettingsChoiceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Color(0xFF4A90D9).copy(0.15f)
            else Color.Transparent
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) Color(0xFF4A90D9) else Color.White.copy(0.2f)
        )
    ) {
        Text(label, fontSize = 12.sp, color = Color.White)
    }
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
            steps.forEach {
                Text(it, fontSize = 11.sp, color = Color(0xFFB0BEC5))
            }
            if (isLockMode) {
                Text(
                    "(Quick double-tap with no move = normal double-click)",
                    fontSize = 10.sp, color = Color(0xFF607D8B)
                )
            }
        }
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                label, fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp, color = Color.White
            )
            Text(
                display(value), fontSize = 13.sp, color = Color(0xFF90CAF9),
                fontWeight = FontWeight.Medium
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
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
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                title, fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp, color = Color.White
            )
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF607D8B))
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF4A90D9),
                checkedTrackColor = Color(0xFF4A90D9).copy(0.5f),
                uncheckedThumbColor = Color(0xFF607D8B),
                uncheckedTrackColor = Color.White.copy(0.2f)
            )
        )
    }
}