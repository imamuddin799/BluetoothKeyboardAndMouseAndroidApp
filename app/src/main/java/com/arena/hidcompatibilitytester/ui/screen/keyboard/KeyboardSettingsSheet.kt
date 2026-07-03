package com.arena.hidcompatibilitytester.ui.screen.keyboard

import android.R.attr.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt

import com.arena.hidcompatibilitytester.ui.components.AdaptiveSpacing
import com.arena.hidcompatibilitytester.ui.components.rememberAdaptiveSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class KbSettingsSection(val title: String) {
    KEYS("Keys"),
    BEHAVIOR("Behavior"),
    APPEARANCE("Appearance"),
    NUMPAD("Numpad"),
    MEDIA_ROW("Media"),
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
                    KbSettingsSection.MEDIA_ROW -> MediaRowSection(local, spacing) { local = it }
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

    SettingsCard("Media Quick Row", spacing) {
        SettingsToggle(
            "Show in Keyboard Tab",
            "Transport, volume & brightness row above function keys",
            settings.showMediaRowInKeyboard
        ) {
            onChange(settings.copy(showMediaRowInKeyboard = it))
        }

        SettingsToggle(
            "Show in Trackpad Keyboard",
            "Same row when keyboard is open on trackpad screen",
            settings.showMediaRowInTrackpad
        ) {
            onChange(settings.copy(showMediaRowInTrackpad = it))
        }
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

@Composable
private fun MediaRowSection(
    settings: KeyboardSettings,
    spacing: AdaptiveSpacing,
    onChange: (KeyboardSettings) -> Unit,
) {
    SettingsCard("Visibility", spacing) {
        SettingsToggle(
            "Show in Keyboard Tab",
            "Media row above function keys in keyboard",
            settings.showMediaRowInKeyboard
        ) {
            onChange(settings.copy(showMediaRowInKeyboard = it))
        }

        SettingsToggle(
            "Show in Trackpad Keyboard",
            "Media row when keyboard is open on trackpad",
            settings.showMediaRowInTrackpad
        ) {
            onChange(settings.copy(showMediaRowInTrackpad = it))
        }
    }

    SettingsCard("Groups", spacing) {
        SettingsToggle(
            "Transport",
            "⏮ ⏯ ⏹ ⏭ — Play, pause, stop, skip",
            settings.mediaRowShowTransport
        ) {
            onChange(settings.copy(mediaRowShowTransport = it))
        }

        SettingsToggle(
            "Volume",
            "🔇 🔉 🔊 — Mute, volume down, volume up",
            settings.mediaRowShowVolume
        ) {
            onChange(settings.copy(mediaRowShowVolume = it))
        }

        SettingsToggle(
            "Brightness",
            "🔅 🔆 — Brightness down, brightness up",
            settings.mediaRowShowBrightness
        ) {
            onChange(settings.copy(mediaRowShowBrightness = it))
        }
    }

    SettingsCard("Key Repeat", spacing) {
        Text(
            "Transport keys never repeat",
            fontSize = 11.sp,
            color = Color(0xFF607D8B),
            fontWeight = FontWeight.Medium,
        )

        SettingsToggle(
            "Repeat Volume Keys",
            "Hold volume up/down to repeat",
            settings.mediaRowRepeatVolume
        ) {
            onChange(settings.copy(mediaRowRepeatVolume = it))
        }

        SettingsToggle(
            "Repeat Brightness Keys",
            "Hold brightness up/down to repeat",
            settings.mediaRowRepeatBrightness
        ) {
            onChange(settings.copy(mediaRowRepeatBrightness = it))
        }
    }

    SettingsCard("Group Order", spacing) {
        Text(
            "Drag to reorder groups left → right",
            fontSize = 11.sp,
            color = Color(0xFF607D8B),
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        DragToReorderList(
            items = settings.mediaRowGroupOrder,
            onReorder = { newOrder ->
                onChange(settings.copy(mediaRowGroupOrder = newOrder))
            }
        )
    }
}

@Composable
private fun DragToReorderList(
    items: List<MediaRowGroup>,
    onReorder: (List<MediaRowGroup>) -> Unit,
) {
    var orderList     by remember(items) { mutableStateOf(items.toList()) }
    var draggingIdx   by remember { mutableIntStateOf(-1) }
    var dragYAccum    by remember { mutableFloatStateOf(0f) }
    var lastTargetIdx by remember { mutableIntStateOf(-1) }

    val density      = LocalDensity.current
    val itemHeightDp = 72.dp
    val spacingDp    = 6.dp
    val slotPx       = with(density) { (itemHeightDp + spacingDp).toPx() }

    val targetIdx = if (draggingIdx >= 0) {
        (draggingIdx + (dragYAccum / slotPx).roundToInt())
            .coerceIn(0, orderList.size - 1)
    } else -1

    LaunchedEffect(targetIdx) {
        if (targetIdx >= 0) lastTargetIdx = targetIdx
    }

    // Stable animatables keyed by item identity, not index
    val offsetAnimatables   = remember { mutableStateMapOf<MediaRowGroup, Animatable<Float, AnimationVector1D>>() }
    val scaleAnimatables    = remember { mutableStateMapOf<MediaRowGroup, Animatable<Float, AnimationVector1D>>() }
    val elevationAnimatables = remember { mutableStateMapOf<MediaRowGroup, Animatable<Float, AnimationVector1D>>() }

    orderList.forEach { group ->
        if (!offsetAnimatables.containsKey(group))    offsetAnimatables[group]    = Animatable(0f)
        if (!scaleAnimatables.containsKey(group))     scaleAnimatables[group]     = Animatable(1f)
        if (!elevationAnimatables.containsKey(group)) elevationAnimatables[group] = Animatable(0f)
    }

    val coroutineScope = rememberCoroutineScope()

    // Drive shuffle offsets while dragging
    orderList.forEachIndexed { index, group ->
        val isDragged = draggingIdx == index

        val targetOffsetPx = when {
            isDragged  -> dragYAccum
            draggingIdx < 0 -> 0f
            targetIdx > draggingIdx && index in (draggingIdx + 1)..targetIdx -> -slotPx
            targetIdx < draggingIdx && index in targetIdx until draggingIdx  ->  slotPx
            else -> 0f
        }

        LaunchedEffect(group, targetOffsetPx, isDragged) {
            val anim = offsetAnimatables[group] ?: return@LaunchedEffect
            if (isDragged) {
                anim.snapTo(targetOffsetPx)
            } else {
                anim.animateTo(
                    targetOffsetPx,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
                )
            }
        }

        LaunchedEffect(group, isDragged) {
            scaleAnimatables[group]?.animateTo(
                if (isDragged) 1.04f else 1f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 200f)
            )
            elevationAnimatables[group]?.animateTo(
                if (isDragged) 12f else 0f,
                animationSpec = spring()
            )
        }
    }

    fun commitReorder() {
        val from = draggingIdx
        val to   = lastTargetIdx

        draggingIdx   = -1
        dragYAccum    = 0f
        lastTargetIdx = -1

        if (from < 0 || to < 0 || to == from) {
            // No actual reorder but still animate dragged item back to 0
            coroutineScope.launch {
                orderList.getOrNull(from)?.let { group ->
                    offsetAnimatables[group]?.animateTo(
                        0f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 250f)
                    )
                }
            }
            return
        }

        val newList = orderList.toMutableList()
        val draggedItem = newList.removeAt(from)
        newList.add(to, draggedItem)

        coroutineScope.launch {
            // ── CRITICAL: Freeze every item at its current visual pixel position
            //    expressed in new-list coordinate space, BEFORE the list reorders ──
            newList.forEachIndexed { newIndex, group ->
                val oldIndex         = orderList.indexOf(group)
                val currentAnimValue = offsetAnimatables[group]?.value ?: 0f
                // Where this item visually is right now, relative to new slot
                val frozenOffset     = (oldIndex - newIndex) * slotPx + currentAnimValue
                offsetAnimatables[group]?.snapTo(frozenOffset)
            }

            // Reorder the list — items look frozen because offsets compensate exactly
            orderList = newList
            onReorder(newList)

            // Now spring every item from frozen position into its final resting place
            newList.map { group ->
                launch {
                    offsetAnimatables[group]?.animateTo(
                        0f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 250f)
                    )
                }
            }
        }
    }

    fun cancelDrag() {
        val cancelledGroup = orderList.getOrNull(draggingIdx)
        draggingIdx   = -1
        dragYAccum    = 0f
        lastTargetIdx = -1

        coroutineScope.launch {
            cancelledGroup?.let { group ->
                offsetAnimatables[group]?.animateTo(
                    0f,
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 200f)
                )
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(spacingDp),
        modifier = Modifier.fillMaxWidth()
    ) {
        orderList.forEachIndexed { index, group ->
            val isDragged = draggingIdx == index
            val isTarget  = !isDragged && index == targetIdx

            val offsetDp    = with(density) { (offsetAnimatables[group]?.value ?: 0f).toDp() }
            val scale       = scaleAnimatables[group]?.value ?: 1f
            val elevationDp = with(density) { (elevationAnimatables[group]?.value ?: 0f).toDp() }

            val bgColor = when {
                isDragged -> Color(0xFF4A90D9).copy(0.3f)
                isTarget  -> Color(0xFF4A90D9).copy(0.08f)
                else      -> Color(0xFF0A1520)
            }
            val borderColor = when {
                isDragged -> Color(0xFF4A90D9)
                isTarget  -> Color(0xFF4A90D9).copy(0.3f)
                else      -> Color.White.copy(0.08f)
            }
            val borderWidth = when {
                isDragged -> 2.dp
                isTarget  -> 1.dp
                else      -> 0.5.dp
            }

            Surface(
                color           = bgColor,
                shape           = RoundedCornerShape(12.dp),
                shadowElevation = elevationDp,
                modifier        = Modifier
                    .fillMaxWidth()
                    .height(itemHeightDp)
                    .zIndex(if (isDragged) 10f else 0f)
                    .offset(y = offsetDp)
                    .scale(scale)
                    .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
                    .pointerInput(index) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingIdx   = index
                                dragYAccum    = 0f
                                lastTargetIdx = index
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragYAccum += amount.y
                            },
                            onDragEnd    = { commitReorder() },
                            onDragCancel = { cancelDrag() }
                        )
                    }
            ) {
                Row(
                    modifier              = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier              = Modifier.weight(1f),
                    ) {
                        val displayIndex = when {
                            isDragged        -> targetIdx + 1
                            draggingIdx >= 0 -> {
                                val wouldBe = when {
                                    targetIdx > draggingIdx && index in (draggingIdx + 1)..targetIdx -> index - 1
                                    targetIdx < draggingIdx && index in targetIdx until draggingIdx  -> index + 1
                                    else -> index
                                }
                                wouldBe + 1
                            }
                            else -> index + 1
                        }

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    if (isDragged) Color(0xFF4A90D9).copy(0.3f)
                                    else Color(0xFF4A90D9).copy(0.15f),
                                    RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$displayIndex",
                                fontSize   = 12.sp,
                                color      = if (isDragged) Color.White else Color(0xFF90CAF9),
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Text(group.icon, fontSize = 20.sp)

                        Column {
                            Text(group.label, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(
                                when (group) {
                                    MediaRowGroup.TRANSPORT  -> "⏮ ⏯ ⏹ ⏭"
                                    MediaRowGroup.VOLUME     -> "🔇 🔉 🔊"
                                    MediaRowGroup.BRIGHTNESS -> "🔅 🔆"
                                },
                                fontSize = 11.sp,
                                color    = Color(0xFF607D8B),
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        modifier            = Modifier.padding(start = 8.dp)
                    ) {
                        Text("⠿", fontSize = 22.sp, color = if (isDragged) Color(0xFF90CAF9) else Color(0xFF546E7A))
                        Text("Hold & drag", fontSize = 7.sp, color = Color(0xFF455A64), maxLines = 1)
                    }
                }
            }
        }
    }
}