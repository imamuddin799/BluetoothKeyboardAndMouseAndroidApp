package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

private enum class LKbSettingsSection(val title: String) {
    KEYS("Keys"),
    BEHAVIOR("Behavior"),
    APPEARANCE("Appearance"),
    NUMPAD("Numpad"),
    MEDIA("Media"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandscapeKeyboardSettingsSheet(
    settings: LandscapeKeyboardSettings,
    onDismiss: () -> Unit,
    onSave: (LandscapeKeyboardSettings) -> Unit,
) {
    var local by remember(settings) { mutableStateOf(settings) }
    var selectedSection by remember { mutableStateOf(LKbSettingsSection.KEYS) }

    Scaffold(
        containerColor = Color(0xFF111C28),
        topBar = {
            TopAppBar(
                title = { Text("Landscape Keyboard Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = { local = LandscapeKeyboardSettings() }) {
                        Text("Reset", color = Color(0xFF90CAF9))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF111C28))
            )
        },
        bottomBar = {
            Surface(color = Color(0xFF111C28), shadowElevation = 8.dp) {
                Button(
                    onClick = { onSave(local); onDismiss() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90D9)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save Configuration", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Row(Modifier.padding(innerPadding).fillMaxSize()) {
            // Left Navigation Rail
            NavigationRail(
                containerColor = Color(0xFF0A1520),
                modifier = Modifier.width(100.dp)
            ) {
                LKbSettingsSection.entries.forEach { section ->
                    NavigationRailItem(
                        selected = selectedSection == section,
                        onClick = { selectedSection = section },
                        icon = {},
                        label = { Text(section.title, fontSize = 11.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedTextColor = Color(0xFF4A90D9),
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFF4A90D9).copy(0.1f)
                        )
                    )
                }
            }

            // Settings Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedSection) {
                    LKbSettingsSection.KEYS -> KeysContent(local) { local = it }
                    LKbSettingsSection.BEHAVIOR -> BehaviorContent(local) { local = it }
                    LKbSettingsSection.APPEARANCE -> AppearanceContent(local) { local = it }
                    LKbSettingsSection.NUMPAD -> NumpadContent(local) { local = it }
                    LKbSettingsSection.MEDIA -> MediaContent(local) { local = it }
                }
                Spacer(Modifier.height(80.dp)) // Padding for bottom bar
            }
        }
    }
}

@Composable
private fun KeysContent(s: LandscapeKeyboardSettings, onUpdate: (LandscapeKeyboardSettings) -> Unit) {
    LSettingCard("Key Repeat") {
        LToggle("Enable Repeat", s.repeatEnabled) { onUpdate(s.copy(repeatEnabled = it)) }
        if (s.repeatEnabled) {
            LSlider("Delay", s.repeatInitialDelayMs.toFloat(), 150f..600f) { onUpdate(s.copy(repeatInitialDelayMs = it.toLong())) }
            LSlider("Speed", s.repeatIntervalMs.toFloat(), 20f..150f) { onUpdate(s.copy(repeatIntervalMs = it.toLong())) }
        }
    }
}

@Composable
private fun BehaviorContent(s: LandscapeKeyboardSettings, onUpdate: (LandscapeKeyboardSettings) -> Unit) {
    LSettingCard("Input Behavior") {
        LToggle("Sticky Modifiers", s.stickyModifiers) { onUpdate(s.copy(stickyModifiers = it)) }
        LToggle("Auto-Release Tab", !s.keepModsAfterTab) { onUpdate(s.copy(keepModsAfterTab = !it)) }
        LToggle("In-Place Reorder", s.inPlaceReorderGlobal) { onUpdate(s.copy(inPlaceReorderGlobal = it)) }
    }
}

@Composable
private fun AppearanceContent(s: LandscapeKeyboardSettings, onUpdate: (LandscapeKeyboardSettings) -> Unit) {
    LSettingCard("Visuals") {
        LToggle("Show Key Hints", s.showKeyHints) { onUpdate(s.copy(showKeyHints = it)) }
        LToggle("High Contrast", s.highContrastMode) { onUpdate(s.copy(highContrastMode = it)) }
        LToggle("Compact Modifiers", s.compactModifiers) { onUpdate(s.copy(compactModifiers = it)) }
    }
    LSettingCard("Landscape Layout") {
        Text("Default Layout Mode", color = Color.White, fontSize = 14.sp)
        Text(
            "Single = full-width rows. Two = keyboard + right cluster (arrows, nav).",
            color = Color.Gray, fontSize = 11.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LandscapeLayoutMode.entries.forEach { mode ->
                val selected = s.landscapeLayoutMode == mode
                OutlinedButton(
                    onClick = { onUpdate(s.copy(landscapeLayoutMode = mode)) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) Color(0xFF4A90D9).copy(0.2f) else Color.Transparent
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        if (selected) 2.dp else 1.dp,
                        if (selected) Color(0xFF4A90D9) else Color.White.copy(0.2f)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text(mode.label, color = Color.White, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun NumpadContent(s: LandscapeKeyboardSettings, onUpdate: (LandscapeKeyboardSettings) -> Unit) {
    LSettingCard("Numpad Setup") {
        LToggle("Default NumLock On", s.numpadStartsLocked) { onUpdate(s.copy(numpadStartsLocked = it)) }
        LToggle("Show Nav Hints", s.numpadShowHints) { onUpdate(s.copy(numpadShowHints = it)) }
    }
}

@Composable
private fun MediaContent(s: LandscapeKeyboardSettings, onUpdate: (LandscapeKeyboardSettings) -> Unit) {
    LSettingCard("Media Bar") {
        LToggle("Transport Group", s.mediaRowShowTransport) { onUpdate(s.copy(mediaRowShowTransport = it)) }
        LToggle("Volume Group", s.mediaRowShowVolume) { onUpdate(s.copy(mediaRowShowVolume = it)) }
        LToggle("Brightness Group", s.mediaRowShowBrightness) { onUpdate(s.copy(mediaRowShowBrightness = it)) }
    }
}

// UI Components for the Setting Sheet

@Composable
private fun LSettingCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1520)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = Color(0xFF4A90D9), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun LToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = Color.White, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column {
        Text("$label: ${value.toInt()}ms", color = Color.Gray, fontSize = 11.sp)
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}