package com.arena.hidcompatibilitytester.ui.screen.keyboard

import androidx.compose.animation.AnimatedVisibility
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
import com.arena.hidcompatibilitytester.ui.components.AdaptiveSpacing
import com.arena.hidcompatibilitytester.ui.components.rememberAdaptiveSpacing

private enum class KbSettingsSection(val title: String) {
    KEYS("Keys"),
    BEHAVIOR("Behavior"),
    APPEARANCE("Appearance"),
    NUMPAD("Numpad"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardSettingsSheet(
    settings: KeyboardSettings,
    onDismiss: () -> Unit,
    onSave: (KeyboardSettings) -> Unit,
) {
    var local by remember(settings) { mutableStateOf(settings) }
    var selectedSection by remember { mutableStateOf(KbSettingsSection.KEYS) }
    val spacing = rememberAdaptiveSpacing()

    Scaffold(
        containerColor = Color(0xFF111C28),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Keyboard Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                        maxLines = 1
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
                    TextButton(onClick = { local = KeyboardSettings() }) {
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
                        .padding(horizontal = spacing.horizontal, vertical = 14.dp)
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
            TabRow(
                selectedTabIndex = selectedSection.ordinal,
                containerColor = Color(0xFF111C28),
                contentColor = Color.White,
                divider = { HorizontalDivider(color = Color.White.copy(0.08f)) }
            ) {
                KbSettingsSection.entries.forEach { section ->
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
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
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
                    .padding(
                        horizontal = spacing.horizontal,
                        vertical = spacing.vertical
                    ),
                verticalArrangement = Arrangement.spacedBy(spacing.cardSpacing)
            ) {
                when (selectedSection) {
                    KbSettingsSection.KEYS -> KeysSection(local, spacing) { local = it }
                    KbSettingsSection.BEHAVIOR -> BehaviorSection(local, spacing) { local = it }
                    KbSettingsSection.APPEARANCE -> AppearanceSection(local, spacing) { local = it }
                    KbSettingsSection.NUMPAD -> NumpadMediaSection(local, spacing) { local = it }
                }

                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun KeysSection(
    settings: KeyboardSettings,
    spacing: AdaptiveSpacing,
    onChange: (KeyboardSettings) -> Unit,
) {
    SettingsCard("Key Repeat", spacing) {
        SettingsToggle(
            "Enable Key Repeat",
            "Hold a key to repeat it automatically",
            settings.repeatEnabled
        ) {
            onChange(settings.copy(repeatEnabled = it))
        }

        AnimatedVisibility(visible = settings.repeatEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.itemSpacing)) {
                SettingsSlider(
                    label = "Initial Delay",
                    value = settings.repeatInitialDelayMs.toFloat(),
                    range = 100f..800f,
                    display = { "${it.toLong()}ms" },
                    slowLabel = "Short",
                    fastLabel = "Long"
                ) {
                    onChange(settings.copy(repeatInitialDelayMs = it.toLong()))
                }

                SettingsSlider(
                    label = "Repeat Speed",
                    value = settings.repeatIntervalMs.toFloat(),
                    range = 20f..150f,
                    display = { "${it.toLong()}ms" },
                    slowLabel = "Fast",
                    fastLabel = "Slow"
                ) {
                    onChange(settings.copy(repeatIntervalMs = it.toLong()))
                }
            }
        }
    }

    SettingsCard("Feedback", spacing) {
        SettingsToggle(
            "Haptic Feedback",
            "Vibrate on key press",
            settings.hapticEnabled
        ) {
            onChange(settings.copy(hapticEnabled = it))
        }

        AnimatedVisibility(visible = settings.hapticEnabled) {
            SizeSelector(
                title = "Haptic Intensity",
                subtitle = "Strength of vibration feedback",
                options = HapticIntensity.entries.map { it.label },
                selectedIndex = settings.hapticIntensity.ordinal,
                spacing = spacing,
            ) {
                onChange(settings.copy(hapticIntensity = HapticIntensity.entries[it]))
            }
        }

        SettingsToggle(
            "Sound on Press",
            "Play a click sound when pressing keys",
            settings.soundOnPress
        ) {
            onChange(settings.copy(soundOnPress = it))
        }
    }
}

@Composable
private fun BehaviorSection(
    settings: KeyboardSettings,
    spacing: AdaptiveSpacing,
    onChange: (KeyboardSettings) -> Unit,
) {
    SettingsCard("Modifier Keys", spacing) {
        SettingsToggle(
            "Sticky Modifiers",
            "Mods stay held across key presses until manually cleared",
            settings.stickyModifiers
        ) {
            onChange(settings.copy(stickyModifiers = it))
        }

        AnimatedVisibility(visible = !settings.stickyModifiers) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.itemSpacing)) {
                SettingsToggle(
                    "Keep Mods After Tab",
                    "Mod+Tab releases only Tab, keeps modifiers held",
                    settings.keepModsAfterTab
                ) {
                    onChange(settings.copy(keepModsAfterTab = it))
                }
            }
        }

        StickyModsExplainCard(settings.stickyModifiers, settings.keepModsAfterTab)
    }

    SettingsCard("Status Bar", spacing) {
        SettingsToggle(
            "Show LED Indicators",
            "Show Caps / Num / Scroll lock and modifier badges",
            settings.showStatusBar
        ) {
            onChange(settings.copy(showStatusBar = it))
        }

        SettingsToggle(
            "Show Key Combo Preview",
            "Show current modifier/key combo and Clear button",
            settings.showComboPreview
        ) {
            onChange(settings.copy(showComboPreview = it))
        }
    }

    SettingsCard("Default Tab", spacing) {
        DefaultTabSelector(
            selected = settings.defaultTab,
            spacing = spacing
        ) {
            onChange(settings.copy(defaultTab = it))
        }
    }
}

@Composable
private fun AppearanceSection(
    settings: KeyboardSettings,
    spacing: AdaptiveSpacing,
    onChange: (KeyboardSettings) -> Unit,
) {
    SettingsCard("Size", spacing) {
        SizeSelector(
            title = "Key Height",
            subtitle = "Adjust the height of keyboard keys",
            options = KeyHeight.entries.map { it.label },
            selectedIndex = settings.keyHeight.ordinal,
            spacing = spacing,
        ) {
            onChange(settings.copy(keyHeight = KeyHeight.entries[it]))
        }

        Spacer(Modifier.height(4.dp))

        SizeSelector(
            title = "Font Size",
            subtitle = "Text size on key labels",
            options = KeyFontSize.entries.map { it.label },
            selectedIndex = settings.keyFontSize.ordinal,
            spacing = spacing,
        ) {
            onChange(settings.copy(keyFontSize = KeyFontSize.entries[it]))
        }
    }

    SettingsCard("Key Labels", spacing) {
        SettingsToggle(
            "Show Key Hints",
            "Show shift characters above keys (e.g. ! above 1)",
            settings.showKeyHints
        ) {
            onChange(settings.copy(showKeyHints = it))
        }

        SettingsToggle(
            "Compact Modifiers",
            "Smaller modifier keys — more space for spacebar",
            settings.compactModifiers
        ) {
            onChange(settings.copy(compactModifiers = it))
        }
    }

    SettingsCard("Visibility", spacing) {
        SettingsToggle(
            "High Contrast Mode",
            "Brighter key colors for better visibility",
            settings.highContrastMode
        ) {
            onChange(settings.copy(highContrastMode = it))
        }
    }
}

@Composable
private fun NumpadMediaSection(
    settings: KeyboardSettings,
    spacing: AdaptiveSpacing,
    onChange: (KeyboardSettings) -> Unit,
) {
    SettingsCard("Numpad", spacing) {
        SettingsToggle(
            "Start with NumLock On",
            "Numpad defaults to number mode instead of navigation",
            settings.numpadStartsLocked
        ) {
            onChange(settings.copy(numpadStartsLocked = it))
        }

        SettingsToggle(
            "Show Alternate Hints",
            "Show navigation labels when NumLock is toggled",
            settings.numpadShowHints
        ) {
            onChange(settings.copy(numpadShowHints = it))
        }
    }

    SettingsCard("Media Keys", spacing) {
        SizeSelector(
            title = "Media Key Size",
            subtitle = "Height of transport and volume buttons",
            options = MediaKeySize.entries.map { it.label },
            selectedIndex = settings.mediaKeySize.ordinal,
            spacing = spacing,
        ) {
            onChange(settings.copy(mediaKeySize = MediaKeySize.entries[it]))
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    spacing: AdaptiveSpacing,
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
            Text(
                title,
                color = Color(0xFF4A90D9),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            content()
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
                fontSize = 14.sp, color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle, fontSize = 11.sp, color = Color(0xFF607D8B),
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
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

@Composable
private fun SettingsSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: (Float) -> String,
    slowLabel: String = "Slow",
    fastLabel: String = "Fast",
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
            Text(slowLabel, fontSize = 10.sp, color = Color(0xFF607D8B))
            Text(fastLabel, fontSize = 10.sp, color = Color(0xFF607D8B))
        }
    }
}

@Composable
private fun SizeSelector(
    title: String,
    subtitle: String,
    options: List<String>,
    selectedIndex: Int,
    spacing: AdaptiveSpacing,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
        Text(subtitle, fontSize = 11.sp, color = Color(0xFF607D8B))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(spacing.chipSpacing)
        ) {
            options.forEachIndexed { i, label ->
                SettingsChoiceButton(
                    label = label,
                    selected = selectedIndex == i,
                    modifier = Modifier.widthIn(min = spacing.chipMinWidth)
                ) {
                    onSelect(i)
                }
            }
        }
    }
}

@Composable
private fun SettingsChoiceButton(
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
            containerColor = if (selected) Color(0xFF4A90D9).copy(0.15f)
            else Color.Transparent
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) Color(0xFF4A90D9) else Color.White.copy(0.2f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DefaultTabSelector(
    selected: Int,
    spacing: AdaptiveSpacing,
    onSelect: (Int) -> Unit,
) {
    val tabNames = listOf("⌨ Keys", "↕ Nav", "🎵 Media")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Default Tab",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Color.White
        )
        Text(
            "Choose which keyboard tab opens first",
            fontSize = 11.sp,
            color = Color(0xFF607D8B)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(spacing.chipSpacing)
        ) {
            tabNames.forEachIndexed { i, label ->
                SettingsChoiceButton(
                    label = label,
                    selected = selected == i,
                    modifier = Modifier.widthIn(min = spacing.chipMinWidth)
                ) {
                    onSelect(i)
                }
            }
        }
    }
}

@Composable
private fun StickyModsExplainCard(stickyMods: Boolean, keepModsAfterTab: Boolean) {
    val (bg, title, lines) = if (stickyMods) {
        Triple(
            Color(0xFF1565C0).copy(0.12f),
            "🔒  Sticky Modifiers ON",
            listOf(
                "• Mod + Key → release only Key, Mod stays held",
                "• Mod + Tab → release only Tab, Mod stays held",
                "• Tap Mod again or Clear to release",
            )
        )
    } else {
        Triple(
            Color(0xFF2E7D32).copy(0.12f),
            "⚡  Sticky Modifiers OFF",
            listOf(
                "• Mod + Key → release both Mod and Key",
                if (keepModsAfterTab)
                    "• Mod + Tab → release only Tab, Mod stays held"
                else
                    "• Mod + Tab → release both Mod and Tab",
            )
        )
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                title, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                color = Color.White, maxLines = 1
            )
            lines.forEach {
                Text(it, fontSize = 11.sp, color = Color(0xFFB0BEC5))
            }
        }
    }
}