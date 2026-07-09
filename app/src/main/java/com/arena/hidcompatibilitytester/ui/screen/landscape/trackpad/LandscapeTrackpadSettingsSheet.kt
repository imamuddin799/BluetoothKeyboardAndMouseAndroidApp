package com.arena.hidcompatibilitytester.ui.screen.landscape.trackpad

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.ui.screen.landscape.LandscapeAdaptiveSpacing
import com.arena.hidcompatibilitytester.ui.screen.landscape.landscapeRememberAdaptiveSpacing

private enum class LandscapeTrackpadSettingsSection(val title: String) {
    POINTER("Pointer"),
    KEYBOARD("Keyboard"),
    LAYOUT("Layout"),
    GESTURES("Gestures")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandscapeTrackpadSettingsSheet(
    settings: LandscapeTrackpadSettings,
    onDismiss: () -> Unit,
    onSave: (LandscapeTrackpadSettings) -> Unit,
) {
    var local by remember(settings) { mutableStateOf(settings) }
    var selectedSection by remember { mutableStateOf(LandscapeTrackpadSettingsSection.POINTER) }
    val spacing = landscapeRememberAdaptiveSpacing()

    Scaffold(
        containerColor = Color(0xFF111C28),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Trackpad Settings",
                        fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        color = Color.White, maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = { local = LandscapeTrackpadSettings() }) {
                        Text("Defaults", color = Color(0xFF90CAF9), fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF111C28))
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
                        .padding(horizontal = spacing.horizontal, vertical = 14.dp)
                ) {
                    Button(
                        onClick = { onSave(local); onDismiss() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90D9))
                    ) {
                        Text("Save Settings", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
            TabRow(
                selectedTabIndex = selectedSection.ordinal,
                containerColor = Color(0xFF111C28),
                contentColor = Color.White,
                divider = { HorizontalDivider(color = Color.White.copy(0.08f)) }
            ) {
                LandscapeTrackpadSettingsSection.entries.forEach { section ->
                    val isSelected = selectedSection == section
                    Tab(
                        selected = isSelected,
                        onClick = { selectedSection = section },
                        selectedContentColor = Color(0xFF4A90D9),
                        unselectedContentColor = Color(0xFFB0BEC5)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = section.title,
                                fontSize = if (isSelected) 13.sp else 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.horizontal, vertical = spacing.vertical),
                verticalArrangement = Arrangement.spacedBy(spacing.cardSpacing)
            ) {
                when (selectedSection) {
                    LandscapeTrackpadSettingsSection.POINTER -> LandscapePointerSection(local, spacing) { local = it }
                    LandscapeTrackpadSettingsSection.KEYBOARD -> LandscapeKeyboardSection(local, spacing) { local = it }
                    LandscapeTrackpadSettingsSection.LAYOUT -> LandscapeLayoutSection(local, spacing) { local = it }
                    LandscapeTrackpadSettingsSection.GESTURES -> LandscapeGesturesSection(local, spacing) { local = it }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ── Sections ──────────────────────────────────────────────────────

@Composable
private fun LandscapePointerSection(
    settings: LandscapeTrackpadSettings,
    spacing: LandscapeAdaptiveSpacing,
    onChange: (LandscapeTrackpadSettings) -> Unit,
) {
    LandscapeSettingsCard("Speed", spacing) {
        LandscapeSettingsSlider("Pointer Speed", settings.pointerSpeed, 0.3f..3.0f, { "%.1fx".format(it) }) {
            onChange(settings.copy(pointerSpeed = it))
        }
        LandscapeSettingsSlider("Scroll Speed", settings.scrollSpeed, 0.3f..3.0f, { "%.1fx".format(it) }) {
            onChange(settings.copy(scrollSpeed = it))
        }
    }
    LandscapeSettingsCard("Behavior", spacing) {
        LandscapeSettingsToggle("Invert Scroll", "Natural scrolling — content follows finger direction", settings.invertScroll) {
            onChange(settings.copy(invertScroll = it))
        }
        LandscapeSettingsToggle("Pointer Acceleration", "Slow = precise · Fast = covers distance", settings.accelerationEnabled) {
            onChange(settings.copy(accelerationEnabled = it))
        }
    }
}

@Composable
private fun LandscapeKeyboardSection(
    settings: LandscapeTrackpadSettings,
    spacing: LandscapeAdaptiveSpacing,
    onChange: (LandscapeTrackpadSettings) -> Unit,
) {
    LandscapeSettingsCard("Keyboard Overlays", spacing) {
        LandscapeSettingsToggle("System Keyboard  📱", "Show toggle button for Android system keyboard — typed text is sent to host", settings.showSystemKeyboard) {
            onChange(settings.copy(showSystemKeyboard = it))
        }
        LandscapeSettingsToggle("In-App Keyboard  ⌨", "Show toggle button for built-in HID keyboard overlay", settings.showInAppKeyboard) {
            onChange(settings.copy(showInAppKeyboard = it))
        }
    }

    LandscapeSettingsCard("In-App Keyboard Defaults", spacing) {
        Text(
            "These control the initial state of toggles when the trackpad's in-app keyboard opens. Session-only changes made from the status bar don't persist.",
            fontSize = 11.sp, color = Color(0xFF607D8B), lineHeight = 14.sp,
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Default Layout Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(spacing.chipSpacing)
            ) {
                com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.LandscapeLayoutMode.entries.forEach { mode ->
                    val sel = settings.trackpadKbDefaultLayoutMode == mode
                    LandscapeSettingsChoiceButton(mode.label, sel, Modifier.widthIn(min = spacing.chipMinWidth)) {
                        onChange(settings.copy(trackpadKbDefaultLayoutMode = mode))
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Default Right Column", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
            Text("Applies only when Two Column layout is active", fontSize = 11.sp, color = Color(0xFF607D8B))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(spacing.chipSpacing)
            ) {
                com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.LandscapeRightColumnMode.entries.forEach { mode ->
                    val sel = settings.trackpadKbDefaultRightColumn == mode
                    LandscapeSettingsChoiceButton(mode.label, sel, Modifier.widthIn(min = spacing.chipMinWidth)) {
                        onChange(settings.copy(trackpadKbDefaultRightColumn = mode))
                    }
                }
            }
        }

        LandscapeSettingsToggle(
            "Show Combo Preview",
            "Show mod+key combo next to LED indicators",
            settings.trackpadKbShowComboPreview
        ) {
            onChange(settings.copy(trackpadKbShowComboPreview = it))
        }
    }
}

@Composable
private fun LandscapeLayoutSection(
    settings: LandscapeTrackpadSettings,
    spacing: LandscapeAdaptiveSpacing,
    onChange: (LandscapeTrackpadSettings) -> Unit,
) {
    LandscapeSettingsCard("Arrow Keys", spacing) {
        LandscapeSettingsToggle("Arrow Keys", "Show directional arrow buttons for precise cursor movement", settings.showArrowKeys) {
            onChange(settings.copy(showArrowKeys = it))
        }
        if (settings.showArrowKeys) {
            LandscapePositionSelector("Arrow Keys Position", "Choose which side shows the arrow keys", settings.arrowPosition, spacing) {
                onChange(settings.copy(arrowPosition = it))
            }
        }
    }
    LandscapeSettingsCard("Scroll Strip", spacing) {
        LandscapeSettingsToggle("Scroll Strip", "Show the scroll strip on the trackpad edge", settings.showScrollStrip) {
            onChange(settings.copy(showScrollStrip = it))
        }
        if (settings.showScrollStrip) {
            LandscapePositionSelector("Scroll Strip Position", "Choose which side shows the scroll strip", settings.scrollPosition, spacing) {
                onChange(settings.copy(scrollPosition = it))
            }
        }
    }
}

@Composable
private fun LandscapeGesturesSection(
    settings: LandscapeTrackpadSettings,
    spacing: LandscapeAdaptiveSpacing,
    onChange: (LandscapeTrackpadSettings) -> Unit,
) {
    LandscapeSettingsCard("Tap", spacing) {
        LandscapeSettingsToggle("Tap to Click", "Short tap = left click", settings.tapToClick) {
            onChange(settings.copy(tapToClick = it))
        }
        LandscapeSettingsToggle("Two-Finger Right Click", "Two-finger tap = right-click menu", settings.twoFingerRightClick) {
            onChange(settings.copy(twoFingerRightClick = it))
        }
    }
    LandscapeSettingsCard("Double-Tap Drag", spacing) {
        LandscapeSettingsToggle(
            title = "Drag Lock Mode",
            subtitle = if (settings.dragLockMode)
                "ON — double-tap to start drag, tap again to release"
            else
                "OFF — double-tap then keep finger down, lift to release",
            checked = settings.dragLockMode
        ) { onChange(settings.copy(dragLockMode = it)) }
        LandscapeDragModeExplainCard(settings.dragLockMode)
    }
}

// ── Reusable components ──────────────────────────────────────────

@Composable
private fun LandscapeSettingsCard(
    title: String,
    spacing: LandscapeAdaptiveSpacing,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1520)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(spacing.innerCardPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.itemSpacing),
        ) {
            Text(title, color = Color(0xFF4A90D9), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            content()
        }
    }
}

@Composable
private fun LandscapePositionSelector(
    title: String,
    subtitle: String,
    selected: LandscapeSidePosition,
    spacing: LandscapeAdaptiveSpacing,
    onSelect: (LandscapeSidePosition) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
        Text(subtitle, fontSize = 11.sp, color = Color(0xFF607D8B))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(spacing.chipSpacing)
        ) {
            LandscapeSidePosition.entries.forEach { pos ->
                val sel = selected == pos
                val label = when (pos) {
                    LandscapeSidePosition.LEFT -> "◀ Left"
                    LandscapeSidePosition.RIGHT -> "Right ▶"
                }
                LandscapeSettingsChoiceButton(label, sel, Modifier.widthIn(min = spacing.chipMinWidth)) { onSelect(pos) }
            }
        }
    }
}

@Composable
private fun LandscapeSettingsChoiceButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Color(0xFF4A90D9).copy(0.15f) else Color.Transparent
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) Color(0xFF4A90D9) else Color.White.copy(0.2f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(label, fontSize = 12.sp, color = Color.White, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LandscapeDragModeExplainCard(isLockMode: Boolean) {
    Surface(
        color = if (isLockMode) Color(0xFF1565C0).copy(0.12f) else Color(0xFF6A1B9A).copy(0.12f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                if (isLockMode) "⬚  Drag Lock" else "✋  Hold to Drag",
                fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, maxLines = 1
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
                Text("(Quick double-tap with no move = normal double-click)", fontSize = 10.sp, color = Color(0xFF607D8B))
            }
        }
    }
}

@Composable
private fun LandscapeSettingsSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
            Text(display(value), fontSize = 13.sp, color = Color(0xFF90CAF9), fontWeight = FontWeight.Medium)
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
private fun LandscapeSettingsToggle(
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
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF607D8B), maxLines = 2, overflow = TextOverflow.Ellipsis)
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