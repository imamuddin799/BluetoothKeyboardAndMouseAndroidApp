package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.arena.hidcompatibilitytester.ui.screen.landscape.LandscapeAdaptiveSpacing
import com.arena.hidcompatibilitytester.ui.screen.landscape.landscapeRememberAdaptiveSpacing
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class LKbSettingsSection(val title: String) {
    KEYS("Keys"),
    BEHAVIOR("Behavior"),
    APPEARANCE("Appearance"),
    NUMPAD("Numpad"),
    MEDIA_ROW("Media"),
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
    val spacing = landscapeRememberAdaptiveSpacing()

    Scaffold(
        containerColor = Color(0xFF111C28),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Landscape Keyboard Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = { local = LandscapeKeyboardSettings() }) {
                        Text("Defaults", color = Color(0xFF90CAF9), fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF111C28))
            )
        },
        bottomBar = {
            Column(
                Modifier.fillMaxWidth().background(Color(0xFF111C28))
            ) {
                HorizontalDivider(color = Color.White.copy(0.08f))
                Box(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = spacing.horizontal, vertical = 14.dp)
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
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(
                selectedTabIndex = selectedSection.ordinal,
                containerColor = Color(0xFF111C28),
                contentColor = Color.White,
                divider = { HorizontalDivider(color = Color.White.copy(0.08f)) }
            ) {
                LKbSettingsSection.entries.forEach { section ->
                    val isSelected = selectedSection == section
                    Tab(
                        selected = isSelected,
                        onClick = { selectedSection = section },
                        selectedContentColor = Color(0xFF4A90D9),
                        unselectedContentColor = Color(0xFFB0BEC5)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth()
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
                    LKbSettingsSection.KEYS       -> LKeysSection(local, spacing) { local = it }
                    LKbSettingsSection.BEHAVIOR   -> LBehaviorSection(local, spacing) { local = it }
                    LKbSettingsSection.APPEARANCE -> LAppearanceSection(local, spacing) { local = it }
                    LKbSettingsSection.NUMPAD     -> LNumpadSection(local, spacing) { local = it }
                    LKbSettingsSection.MEDIA_ROW  -> LMediaSection(local, spacing) { local = it }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SECTIONS
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun LKeysSection(
    settings: LandscapeKeyboardSettings,
    spacing: LandscapeAdaptiveSpacing,
    onChange: (LandscapeKeyboardSettings) -> Unit,
) {
    LSettingsCard("Key Repeat", spacing) {
        LSettingsToggle("Enable Key Repeat", "Hold a key to repeat it automatically",
            settings.repeatEnabled) { onChange(settings.copy(repeatEnabled = it)) }

        AnimatedVisibility(visible = settings.repeatEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.itemSpacing)) {
                LSettingsSlider("Initial Delay", settings.repeatInitialDelayMs.toFloat(),
                    100f..800f, { "${it.toLong()}ms" }, "Short", "Long") {
                    onChange(settings.copy(repeatInitialDelayMs = it.toLong()))
                }
                LSettingsSlider("Repeat Speed", settings.repeatIntervalMs.toFloat(),
                    20f..150f, { "${it.toLong()}ms" }, "Fast", "Slow") {
                    onChange(settings.copy(repeatIntervalMs = it.toLong()))
                }
            }
        }
    }

    LSettingsCard("Feedback", spacing) {
        LSettingsToggle("Haptic Feedback", "Vibrate on key press",
            settings.hapticEnabled) { onChange(settings.copy(hapticEnabled = it)) }

        AnimatedVisibility(visible = settings.hapticEnabled) {
            LSizeSelector("Haptic Intensity", "Strength of vibration feedback",
                LandscapeHapticIntensity.entries.map { it.label },
                settings.hapticIntensity.ordinal, spacing) {
                onChange(settings.copy(hapticIntensity = LandscapeHapticIntensity.entries[it]))
            }
        }

        LSettingsToggle("Sound on Press", "Play a click sound when pressing keys",
            settings.soundOnPress) { onChange(settings.copy(soundOnPress = it)) }
    }
}

@Composable
private fun LBehaviorSection(
    settings: LandscapeKeyboardSettings,
    spacing: LandscapeAdaptiveSpacing,
    onChange: (LandscapeKeyboardSettings) -> Unit,
) {
    LSettingsCard("Modifier Keys", spacing) {
        LSettingsToggle("Sticky Modifiers",
            "Mods stay held across key presses until manually cleared",
            settings.stickyModifiers) { onChange(settings.copy(stickyModifiers = it)) }

        AnimatedVisibility(visible = !settings.stickyModifiers) {
            LSettingsToggle("Keep Mods After Tab",
                "Mod+Tab releases only Tab, keeps modifiers held",
                settings.keepModsAfterTab) { onChange(settings.copy(keepModsAfterTab = it)) }
        }

        LStickyModsExplainCard(settings.stickyModifiers, settings.keepModsAfterTab)
    }

    LSettingsCard("Optional Rows", spacing) {
        LSettingsToggle("Global Visibility",
            "Use same optional row visibility across all keyboards",
            settings.globalOptionalRowVisibility) {
            onChange(settings.copy(globalOptionalRowVisibility = it))
        }

        HorizontalDivider(color = Color.White.copy(0.06f), modifier = Modifier.padding(vertical = 4.dp))

        Text("🎵 Media Row", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
        Text("Transport, volume & brightness above function keys",
            fontSize = 11.sp, color = Color(0xFF607D8B))

        if (settings.globalOptionalRowVisibility) {
            LSettingsToggle("Show Media Row", "Shown in all keyboards when enabled",
                settings.globalShowMediaRow) { onChange(settings.copy(globalShowMediaRow = it)) }
        }

        LSettingsToggle("Keys Tab Keyboard",
            if (settings.globalOptionalRowVisibility) "Controlled by global — turn off global to customize"
            else "Show media row in keyboard tab",
            if (settings.globalOptionalRowVisibility) settings.globalShowMediaRow
            else settings.keysTabShowMediaRow,
            enabled = !settings.globalOptionalRowVisibility) {
            onChange(settings.copy(keysTabShowMediaRow = it))
        }

        LSettingsToggle("Trackpad Keyboard",
            if (settings.globalOptionalRowVisibility) "Controlled by global — turn off global to customize"
            else "Show media row in trackpad keyboard",
            if (settings.globalOptionalRowVisibility) settings.globalShowMediaRow
            else settings.trackpadShowMediaRow,
            enabled = !settings.globalOptionalRowVisibility) {
            onChange(settings.copy(trackpadShowMediaRow = it))
        }

        if (settings.globalOptionalRowVisibility) LGlobalOptionalRowHint()

        HorizontalDivider(color = Color.White.copy(0.06f), modifier = Modifier.padding(vertical = 4.dp))

        Text("↕ Navigation Row", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
        Text("Home · End · PgUp · PgDn · Ins · ← ↑ ↓ →",
            fontSize = 11.sp, color = Color(0xFF607D8B))

        if (settings.globalOptionalRowVisibility) {
            LSettingsToggle("Show Nav Row", "Shown in all keyboards when enabled",
                settings.globalShowNavRow) { onChange(settings.copy(globalShowNavRow = it)) }
        }

        LSettingsToggle("Keys Tab Keyboard",
            if (settings.globalOptionalRowVisibility) "Controlled by global — turn off global to customize"
            else "Show nav row in keyboard tab",
            if (settings.globalOptionalRowVisibility) settings.globalShowNavRow
            else settings.keysTabShowNavRow,
            enabled = !settings.globalOptionalRowVisibility) {
            onChange(settings.copy(keysTabShowNavRow = it))
        }

        LSettingsToggle("Trackpad Keyboard",
            if (settings.globalOptionalRowVisibility) "Controlled by global — turn off global to customize"
            else "Show nav row in trackpad keyboard",
            if (settings.globalOptionalRowVisibility) settings.globalShowNavRow
            else settings.trackpadShowNavRow,
            enabled = !settings.globalOptionalRowVisibility) {
            onChange(settings.copy(trackpadShowNavRow = it))
        }

        if (settings.globalOptionalRowVisibility) LGlobalOptionalRowHint()
    }

    LSettingsCard("Status Bar", spacing) {
        LSettingsToggle("Show LED Indicators",
            "Show Caps / Num / Scroll lock and modifier badges",
            settings.showStatusBar) { onChange(settings.copy(showStatusBar = it)) }

        LSettingsToggle("Show Key Combo Preview",
            "Show current modifier/key combo and Clear button",
            settings.showComboPreview) { onChange(settings.copy(showComboPreview = it)) }
    }

    LSettingsCard("Merge System & Modifiers", spacing) {
        LSettingsToggle("Merge All Tabs",
            "Combine System Keys + Quick Modifiers into one section across all tabs",
            settings.mergeSystemAndModsGlobal) {
            onChange(settings.copy(
                mergeSystemAndModsGlobal = it,
                keysTabMergeSystemAndMods = it,
                mediaTabMergeSystemAndMods = it,
                navTabMergeSystemAndMods = it,
            ))
        }
    }

    LSettingsCard("In-Place Section Reorder", spacing) {
        LSettingsToggle("Enable All Tabs",
            "Long-press and drag to reorder sections directly on any tab",
            settings.inPlaceReorderGlobal) {
            onChange(settings.copy(
                inPlaceReorderGlobal = it,
                keysTabInPlaceReorder = it,
                mediaTabInPlaceReorder = it,
                navTabInPlaceReorder = it,
            ))
        }
    }

    LSettingsCard("Landscape Layout", spacing) {
        LSizeSelector("Default Layout Mode",
            "Single = full-width rows. Two = keyboard + right cluster.",
            LandscapeLayoutMode.entries.map { it.label },
            settings.landscapeLayoutMode.ordinal, spacing) {
            onChange(settings.copy(landscapeLayoutMode = LandscapeLayoutMode.entries[it]))
        }
    }

    LSettingsCard("Default Tab", spacing) {
        LDefaultTabSelector(settings.defaultTab, spacing) {
            onChange(settings.copy(defaultTab = it))
        }
    }
}

@Composable
private fun LAppearanceSection(
    settings: LandscapeKeyboardSettings,
    spacing: LandscapeAdaptiveSpacing,
    onChange: (LandscapeKeyboardSettings) -> Unit,
) {
    LSettingsCard("Size", spacing) {
        LSizeSelector("Key Height", "Adjust the height of keyboard keys",
            LandscapeKeyHeight.entries.map { it.label },
            settings.keyHeight.ordinal, spacing) {
            onChange(settings.copy(keyHeight = LandscapeKeyHeight.entries[it]))
        }

        Spacer(Modifier.height(4.dp))

        LSizeSelector("Font Size", "Text size on key labels",
            LandscapeKeyFontSize.entries.map { it.label },
            settings.keyFontSize.ordinal, spacing) {
            onChange(settings.copy(keyFontSize = LandscapeKeyFontSize.entries[it]))
        }
    }

    LSettingsCard("Key Labels", spacing) {
        LSettingsToggle("Show Key Hints",
            "Show shift characters above keys (e.g. ! above 1)",
            settings.showKeyHints) { onChange(settings.copy(showKeyHints = it)) }

        LSettingsToggle("Compact Modifiers",
            "Smaller modifier keys — more space for spacebar",
            settings.compactModifiers) { onChange(settings.copy(compactModifiers = it)) }
    }

    LSettingsCard("Keys Tab Sections", spacing) {
        LSizeSelector("Section Style", "Visual style for sections in Keys tab",
            LandscapeSectionStyle.entries.map { it.label },
            settings.keysTabSectionStyle.ordinal, spacing) {
            onChange(settings.copy(keysTabSectionStyle = LandscapeSectionStyle.entries[it]))
        }

        Spacer(Modifier.height(4.dp))

        LSettingsToggle("Navigation", "Home, End, PgUp, PgDn, Ins, Del",
            settings.keysTabShowNavigation) { onChange(settings.copy(keysTabShowNavigation = it)) }
        LSettingsToggle("Arrow Keys", "← ↑ ↓ → directional keys",
            settings.keysTabShowArrowKeys) { onChange(settings.copy(keysTabShowArrowKeys = it)) }
        LSettingsToggle("System Keys",
            "Esc, Tab, BkSp, Del, Enter, PrtSc, ScrLk, Pause, Ins, Menu",
            settings.keysTabShowSystemKeys) { onChange(settings.copy(keysTabShowSystemKeys = it)) }
        LSettingsToggle("Quick Modifiers", "Ctrl, Shift, Alt, Win, AltGr, Menu",
            settings.keysTabShowQuickMods) { onChange(settings.copy(keysTabShowQuickMods = it)) }

        AnimatedVisibility(visible = settings.keysTabShowSystemKeys && settings.keysTabShowQuickMods) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LSettingsToggle("Merge System & Modifiers",
                    if (settings.mergeSystemAndModsGlobal)
                        "Controlled by global merge — turn off global to customize"
                    else "Combine into one section with 3 rows",
                    settings.keysTabMergeSystemAndMods,
                    enabled = !settings.mergeSystemAndModsGlobal) {
                    onChange(settings.copy(keysTabMergeSystemAndMods = it))
                }
                if (settings.mergeSystemAndModsGlobal) LGlobalMergeHint()
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LSettingsToggle("In-Place Reorder",
                if (settings.inPlaceReorderGlobal)
                    "Controlled by global setting — turn off global to customize"
                else "Long-press and drag sections to reorder on Keys tab",
                settings.keysTabInPlaceReorder,
                enabled = !settings.inPlaceReorderGlobal) {
                onChange(settings.copy(keysTabInPlaceReorder = it))
            }
            if (settings.inPlaceReorderGlobal) LGlobalReorderHint()

            LSettingsToggle("Swap Nav ↔ Arrows",
                "Swap left/right position of Navigation and Arrow Keys",
                settings.keysTabNavArrowsSwapped) {
                onChange(settings.copy(keysTabNavArrowsSwapped = it))
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("Section Order", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
        Text("Drag to reorder sections", fontSize = 11.sp, color = Color(0xFF607D8B))
        Spacer(Modifier.height(4.dp))

        LGenericDragToReorderList(
            items = settings.keysTabSectionOrder.filter { section ->
                val merge = settings.shouldMergeSystemMods(settings.keysTabMergeSystemAndMods)
                when (section) {
                    LandscapeKeysTabSection.NAV_ARROWS -> settings.keysTabShowNavigation || settings.keysTabShowArrowKeys
                    LandscapeKeysTabSection.SYSTEM_KEYS -> settings.keysTabShowSystemKeys && !merge
                    LandscapeKeysTabSection.QUICK_MODS -> settings.keysTabShowQuickMods && !merge
                    LandscapeKeysTabSection.MERGED_SYSTEM_MODS -> merge
                }
            },
            labelProvider = { it.label },
            iconProvider = { it.icon },
            onReorder = { newOrder ->
                val hidden = settings.keysTabSectionOrder.filter { it !in newOrder.toSet() }
                onChange(settings.copy(keysTabSectionOrder = newOrder + hidden))
            }
        )
    }

    LSettingsCard("Optional Row Order", spacing) {
        LSettingsToggle("Global Order",
            "Use same optional row order across all tabs and trackpad",
            settings.globalOptionalRowOrder) {
            onChange(settings.copy(globalOptionalRowOrder = it))
        }

        Spacer(Modifier.height(4.dp))

        if (settings.globalOptionalRowOrder) {
            Text("Global Order", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
            Text("Drag to reorder optional rows (applies everywhere)",
                fontSize = 11.sp, color = Color(0xFF607D8B))
            Spacer(Modifier.height(4.dp))
            LGenericDragToReorderList(
                items = settings.keyboardOptionalRowOrder,
                labelProvider = { it.label },
                iconProvider = { it.icon },
                onReorder = { newOrder ->
                    onChange(settings.copy(
                        keyboardOptionalRowOrder = newOrder,
                        keysTabOptionalRowOrder = newOrder,
                        trackpadOptionalRowOrder = newOrder,
                    ))
                }
            )
        } else {
            Text("Keys Tab Order", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
            Text("Drag to reorder optional rows for Keys tab",
                fontSize = 11.sp, color = Color(0xFF607D8B))
            Spacer(Modifier.height(4.dp))
            LGenericDragToReorderList(
                items = settings.keysTabOptionalRowOrder,
                labelProvider = { it.label },
                iconProvider = { it.icon },
                onReorder = { onChange(settings.copy(keysTabOptionalRowOrder = it)) }
            )

            Spacer(Modifier.height(12.dp))

            Text("Trackpad Keyboard Order", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
            Text("Drag to reorder optional rows for trackpad keyboard",
                fontSize = 11.sp, color = Color(0xFF607D8B))
            Spacer(Modifier.height(4.dp))
            LGenericDragToReorderList(
                items = settings.trackpadOptionalRowOrder,
                labelProvider = { it.label },
                iconProvider = { it.icon },
                onReorder = { onChange(settings.copy(trackpadOptionalRowOrder = it)) }
            )
        }
    }

    LSettingsCard("Visibility", spacing) {
        LSettingsToggle("High Contrast Mode",
            "Brighter key colors for better visibility",
            settings.highContrastMode) { onChange(settings.copy(highContrastMode = it)) }
    }
}

@Composable
private fun LNumpadSection(
    settings: LandscapeKeyboardSettings,
    spacing: LandscapeAdaptiveSpacing,
    onChange: (LandscapeKeyboardSettings) -> Unit,
) {
    LSettingsCard("Nav+Numpad Tab Sections", spacing) {
        LSettingsToggle("Navigation", "Home, End, PgUp, PgDn, Ins, Del",
            settings.navTabShowNavigation) { onChange(settings.copy(navTabShowNavigation = it)) }
        LSettingsToggle("Arrow Keys", "← ↑ ↓ → directional keys",
            settings.navTabShowArrowKeys) { onChange(settings.copy(navTabShowArrowKeys = it)) }
        LSettingsToggle("Insert/Overwrite Toggle", "Switch between insert and overwrite mode",
            settings.navTabShowInsertToggle) { onChange(settings.copy(navTabShowInsertToggle = it)) }
        LSettingsToggle("System Keys",
            "Esc, Tab, BkSp, Del, Enter, PrtSc, ScrLk, Pause, Ins, Menu",
            settings.navTabShowSystemKeys) { onChange(settings.copy(navTabShowSystemKeys = it)) }
        LSettingsToggle("Quick Modifiers", "Ctrl, Shift, Alt, Win, AltGr, Menu",
            settings.navTabShowQuickMods) { onChange(settings.copy(navTabShowQuickMods = it)) }

        AnimatedVisibility(visible = settings.navTabShowSystemKeys && settings.navTabShowQuickMods) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LSettingsToggle("Merge System & Modifiers",
                    if (settings.mergeSystemAndModsGlobal)
                        "Controlled by global merge — turn off global to customize"
                    else "Combine into one section with 3 rows",
                    settings.navTabMergeSystemAndMods,
                    enabled = !settings.mergeSystemAndModsGlobal) {
                    onChange(settings.copy(navTabMergeSystemAndMods = it))
                }
                if (settings.mergeSystemAndModsGlobal) LGlobalMergeHint()
            }
        }

        LSettingsToggle("Type & Send Text",
            "Text field to type and send via system keyboard",
            settings.navTabShowTypeText) { onChange(settings.copy(navTabShowTypeText = it)) }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LSettingsToggle("In-Place Reorder",
                if (settings.inPlaceReorderGlobal)
                    "Controlled by global setting — turn off global to customize"
                else "Long-press and drag sections to reorder on Nav+Numpad tab",
                settings.navTabInPlaceReorder,
                enabled = !settings.inPlaceReorderGlobal) {
                onChange(settings.copy(navTabInPlaceReorder = it))
            }
            if (settings.inPlaceReorderGlobal) LGlobalReorderHint()

            LSettingsToggle("Swap Nav ↔ Arrows",
                "Swap left/right position of Navigation and Arrow Keys",
                settings.navTabNavArrowsSwapped) {
                onChange(settings.copy(navTabNavArrowsSwapped = it))
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("Section Order", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
        Text("Drag to reorder sections", fontSize = 11.sp, color = Color(0xFF607D8B))
        Spacer(Modifier.height(4.dp))

        LGenericDragToReorderList(
            items = settings.navTabSectionOrder.filter { section ->
                val merge = settings.shouldMergeSystemMods(settings.navTabMergeSystemAndMods)
                when (section) {
                    LandscapeNavTabSection.NAV_ARROWS -> settings.navTabShowNavigation || settings.navTabShowArrowKeys
                    LandscapeNavTabSection.INSERT_TOGGLE -> settings.navTabShowInsertToggle
                    LandscapeNavTabSection.SYSTEM_KEYS -> settings.navTabShowSystemKeys && !merge
                    LandscapeNavTabSection.QUICK_MODS -> settings.navTabShowQuickMods && !merge
                    LandscapeNavTabSection.MERGED_SYSTEM_MODS -> merge
                    LandscapeNavTabSection.TYPE_TEXT -> settings.navTabShowTypeText
                }
            },
            labelProvider = { it.label },
            iconProvider = { it.icon },
            onReorder = { newOrder ->
                val hidden = settings.navTabSectionOrder.filter { it !in newOrder.toSet() }
                onChange(settings.copy(navTabSectionOrder = newOrder + hidden))
            }
        )
    }

    LSettingsCard("Numpad", spacing) {
        LSettingsToggle("Start with NumLock On",
            "Numpad defaults to number mode instead of navigation",
            settings.numpadStartsLocked) { onChange(settings.copy(numpadStartsLocked = it)) }

        LSettingsToggle("Show Alternate Hints",
            "Show navigation labels when NumLock is toggled",
            settings.numpadShowHints) { onChange(settings.copy(numpadShowHints = it)) }
    }
}

@Composable
private fun LMediaSection(
    settings: LandscapeKeyboardSettings,
    spacing: LandscapeAdaptiveSpacing,
    onChange: (LandscapeKeyboardSettings) -> Unit,
) {
    LSettingsCard("Media Key Size", spacing) {
        LSizeSelector("Button Size", "Height of transport and volume buttons",
            LandscapeMediaKeySize.entries.map { it.label },
            settings.mediaKeySize.ordinal, spacing) {
            onChange(settings.copy(mediaKeySize = LandscapeMediaKeySize.entries[it]))
        }
    }

    LSettingsCard("Media Tab Sections", spacing) {
        LSizeSelector("Section Style", "Visual style for sections in Media tab",
            LandscapeSectionStyle.entries.map { it.label },
            settings.mediaTabSectionStyle.ordinal, spacing) {
            onChange(settings.copy(mediaTabSectionStyle = LandscapeSectionStyle.entries[it]))
        }

        Spacer(Modifier.height(4.dp))

        LSettingsToggle("Navigation", "Home, End, PgUp, PgDn, Ins, Del",
            settings.mediaTabShowNavigation) { onChange(settings.copy(mediaTabShowNavigation = it)) }
        LSettingsToggle("Arrow Keys", "← ↑ ↓ → directional keys",
            settings.mediaTabShowArrowKeys) { onChange(settings.copy(mediaTabShowArrowKeys = it)) }
        LSettingsToggle("System Keys",
            "Esc, Tab, BkSp, Del, Enter, PrtSc, ScrLk, Pause, Ins, Menu",
            settings.mediaTabShowSystemKeys) { onChange(settings.copy(mediaTabShowSystemKeys = it)) }
        LSettingsToggle("Quick Modifiers", "Ctrl, Shift, Alt, Win, AltGr, Menu",
            settings.mediaTabShowQuickMods) { onChange(settings.copy(mediaTabShowQuickMods = it)) }

        AnimatedVisibility(visible = settings.mediaTabShowSystemKeys && settings.mediaTabShowQuickMods) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LSettingsToggle("Merge System & Modifiers",
                    if (settings.mergeSystemAndModsGlobal)
                        "Controlled by global merge — turn off global to customize"
                    else "Combine into one section with 3 rows",
                    settings.mediaTabMergeSystemAndMods,
                    enabled = !settings.mergeSystemAndModsGlobal) {
                    onChange(settings.copy(mediaTabMergeSystemAndMods = it))
                }
                if (settings.mergeSystemAndModsGlobal) LGlobalMergeHint()
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LSettingsToggle("In-Place Reorder",
                if (settings.inPlaceReorderGlobal)
                    "Controlled by global setting — turn off global to customize"
                else "Long-press and drag sections to reorder on Media tab",
                settings.mediaTabInPlaceReorder,
                enabled = !settings.inPlaceReorderGlobal) {
                onChange(settings.copy(mediaTabInPlaceReorder = it))
            }
            if (settings.inPlaceReorderGlobal) LGlobalReorderHint()
        }

        Spacer(Modifier.height(4.dp))
        Text("Section Order", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
        Text("Drag to reorder sections", fontSize = 11.sp, color = Color(0xFF607D8B))
        Spacer(Modifier.height(4.dp))

        LGenericDragToReorderList(
            items = settings.mediaTabSectionOrder.filter { section ->
                val merge = settings.shouldMergeSystemMods(settings.mediaTabMergeSystemAndMods)
                when (section) {
                    LandscapeMediaTabSection.TRANSPORT -> true
                    LandscapeMediaTabSection.VOLUME_BRIGHTNESS -> true
                    LandscapeMediaTabSection.NAVIGATION -> settings.mediaTabShowNavigation
                    LandscapeMediaTabSection.ARROW_KEYS -> settings.mediaTabShowArrowKeys
                    LandscapeMediaTabSection.SYSTEM_KEYS -> settings.mediaTabShowSystemKeys && !merge
                    LandscapeMediaTabSection.QUICK_MODS -> settings.mediaTabShowQuickMods && !merge
                    LandscapeMediaTabSection.MERGED_SYSTEM_MODS -> merge
                }
            },
            labelProvider = { it.label },
            iconProvider = { it.icon },
            onReorder = { newOrder ->
                val hidden = settings.mediaTabSectionOrder.filter { it !in newOrder.toSet() }
                onChange(settings.copy(mediaTabSectionOrder = newOrder + hidden))
            }
        )
    }

    LSettingsCard("Groups", spacing) {
        LSettingsToggle("Transport", "⏮ ⏯ ⏹ ⏭ — Play, pause, stop, skip",
            settings.mediaRowShowTransport) { onChange(settings.copy(mediaRowShowTransport = it)) }
        LSettingsToggle("Volume", "🔇 🔉 🔊 — Mute, volume down, volume up",
            settings.mediaRowShowVolume) { onChange(settings.copy(mediaRowShowVolume = it)) }
        LSettingsToggle("Brightness", "🔅 🔆 — Brightness down, brightness up",
            settings.mediaRowShowBrightness) { onChange(settings.copy(mediaRowShowBrightness = it)) }
    }

    LSettingsCard("Key Repeat", spacing) {
        Text("Transport keys never repeat", fontSize = 11.sp,
            color = Color(0xFF607D8B), fontWeight = FontWeight.Medium)

        LSettingsToggle("Repeat Volume Keys", "Hold volume up/down to repeat",
            settings.mediaRowRepeatVolume) { onChange(settings.copy(mediaRowRepeatVolume = it)) }
        LSettingsToggle("Repeat Brightness Keys", "Hold brightness up/down to repeat",
            settings.mediaRowRepeatBrightness) { onChange(settings.copy(mediaRowRepeatBrightness = it)) }
    }

    LSettingsCard("Group Order", spacing) {
        Text("Drag to reorder groups left → right", fontSize = 11.sp,
            color = Color(0xFF607D8B), fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        LMediaGroupDragList(settings.mediaRowGroupOrder) { newOrder ->
            onChange(settings.copy(mediaRowGroupOrder = newOrder))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SHARED WIDGETS
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun LSettingsCard(
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
            Text(title, color = Color(0xFF4A90D9), fontSize = 13.sp,
                fontWeight = FontWeight.Bold, maxLines = 1)
            content()
        }
    }
}

@Composable
private fun LSettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onToggle: (Boolean) -> Unit,
) {
    val alpha = if (enabled) 1f else 0.4f

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                color = Color.White.copy(alpha = alpha),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 11.sp,
                color = Color(0xFF607D8B).copy(alpha = alpha),
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Switch(
            checked = checked,
            onCheckedChange = { if (enabled) onToggle(it) },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF4A90D9),
                checkedTrackColor = Color(0xFF4A90D9).copy(0.5f),
                uncheckedThumbColor = Color(0xFF607D8B),
                uncheckedTrackColor = Color.White.copy(0.2f),
                disabledCheckedThumbColor = Color(0xFF4A90D9).copy(0.4f),
                disabledCheckedTrackColor = Color(0xFF4A90D9).copy(0.2f),
                disabledUncheckedThumbColor = Color(0xFF607D8B).copy(0.4f),
                disabledUncheckedTrackColor = Color.White.copy(0.1f),
            )
        )
    }
}

@Composable
private fun LSettingsSlider(
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
            Text(slowLabel, fontSize = 10.sp, color = Color(0xFF607D8B))
            Text(fastLabel, fontSize = 10.sp, color = Color(0xFF607D8B))
        }
    }
}

@Composable
private fun LSizeSelector(
    title: String,
    subtitle: String,
    options: List<String>,
    selectedIndex: Int,
    spacing: LandscapeAdaptiveSpacing,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
        Text(subtitle, fontSize = 11.sp, color = Color(0xFF607D8B))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(spacing.chipSpacing)
        ) {
            options.forEachIndexed { i, label ->
                LChoiceButton(label, selectedIndex == i,
                    Modifier.widthIn(min = spacing.chipMinWidth)) { onSelect(i) }
            }
        }
    }
}

@Composable
private fun LChoiceButton(
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
        Text(label, fontSize = 12.sp, color = Color.White,
            maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LDefaultTabSelector(
    selected: Int,
    spacing: LandscapeAdaptiveSpacing,
    onSelect: (Int) -> Unit,
) {
    val tabNames = listOf("⌨ Keys", "↕ Nav", "🎵 Media")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Default Tab", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
        Text("Choose which keyboard tab opens first", fontSize = 11.sp, color = Color(0xFF607D8B))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(spacing.chipSpacing)
        ) {
            tabNames.forEachIndexed { i, label ->
                LChoiceButton(label, selected == i,
                    Modifier.widthIn(min = spacing.chipMinWidth)) { onSelect(i) }
            }
        }
    }
}

@Composable
private fun LStickyModsExplainCard(stickyMods: Boolean, keepModsAfterTab: Boolean) {
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

    Surface(color = bg, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                color = Color.White, maxLines = 1)
            lines.forEach { Text(it, fontSize = 11.sp, color = Color(0xFFB0BEC5)) }
        }
    }
}

@Composable
private fun LGlobalOptionalRowHint() {
    Surface(color = Color(0xFF1565C0).copy(0.1f), shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🔒", fontSize = 14.sp)
            Text("Global visibility is ON. Turn it off to customize per keyboard.",
                fontSize = 11.sp, color = Color(0xFF90CAF9), maxLines = 2)
        }
    }
}

@Composable
private fun LGlobalMergeHint() {
    Surface(color = Color(0xFF1565C0).copy(0.1f), shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🔒", fontSize = 14.sp)
            Text("Global merge is ON. Go to Behavior → Merge System & Modifiers to turn it off.",
                fontSize = 11.sp, color = Color(0xFF90CAF9), maxLines = 2)
        }
    }
}

@Composable
private fun LGlobalReorderHint() {
    Surface(color = Color(0xFF1565C0).copy(0.1f), shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🔒", fontSize = 14.sp)
            Text("Global in-place reorder is ON. Go to Behavior to turn it off.",
                fontSize = 11.sp, color = Color(0xFF90CAF9), maxLines = 2)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// DRAG-TO-REORDER LISTS
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun <T> LGenericDragToReorderList(
    items: List<T>,
    labelProvider: (T) -> String,
    iconProvider: (T) -> String,
    onReorder: (List<T>) -> Unit,
) {
    var orderList by remember(items) { mutableStateOf(items.toList()) }
    var draggingIdx by remember { mutableIntStateOf(-1) }
    var dragYAccum by remember { mutableFloatStateOf(0f) }
    var lastTargetIdx by remember { mutableIntStateOf(-1) }
    var isCommitting by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val itemHeightDp = 56.dp
    val spacingDp = 4.dp
    val slotPx = with(density) { (itemHeightDp + spacingDp).toPx() }
    val coroutineScope = rememberCoroutineScope()

    val targetIdx = if (draggingIdx >= 0) {
        (draggingIdx + (dragYAccum / slotPx).roundToInt()).coerceIn(0, orderList.size - 1)
    } else -1

    LaunchedEffect(targetIdx) { if (targetIdx >= 0) lastTargetIdx = targetIdx }

    val offsetAnimatables = remember { mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>() }
    orderList.indices.forEach { idx ->
        if (!offsetAnimatables.containsKey(idx)) offsetAnimatables[idx] = Animatable(0f)
    }

    orderList.indices.forEach { index ->
        val isDragged = draggingIdx == index
        val targetOffsetPx = when {
            isDragged -> dragYAccum
            draggingIdx < 0 -> 0f
            targetIdx > draggingIdx && index in (draggingIdx + 1)..targetIdx -> -slotPx
            targetIdx < draggingIdx && index in targetIdx until draggingIdx -> slotPx
            else -> 0f
        }
        LaunchedEffect(index, targetOffsetPx, isDragged) {
            if (isCommitting) return@LaunchedEffect
            val anim = offsetAnimatables[index] ?: return@LaunchedEffect
            if (isDragged) anim.snapTo(targetOffsetPx)
            else anim.animateTo(targetOffsetPx, spring(dampingRatio = 0.8f, stiffness = 300f))
        }
    }

    fun commitReorder() {
        val from = draggingIdx; val to = lastTargetIdx
        draggingIdx = -1; dragYAccum = 0f; lastTargetIdx = -1
        if (from < 0 || to < 0 || to == from) return
        val newList = orderList.toMutableList()
        val item = newList.removeAt(from)
        newList.add(to, item)
        coroutineScope.launch {
            isCommitting = true
            orderList.indices.forEach { offsetAnimatables[it]?.snapTo(0f) }
            orderList = newList
            onReorder(newList)
            isCommitting = false
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(spacingDp),
        modifier = Modifier.fillMaxWidth().pointerInput(orderList) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    if (isCommitting) return@detectDragGesturesAfterLongPress
                    val idx = (offset.y / slotPx).toInt().coerceIn(orderList.indices)
                    draggingIdx = idx; dragYAccum = 0f; lastTargetIdx = idx
                },
                onDrag = { change, amount ->
                    if (draggingIdx >= 0 && !isCommitting) {
                        change.consume(); dragYAccum += amount.y
                    }
                },
                onDragEnd = { if (!isCommitting) commitReorder() },
                onDragCancel = { draggingIdx = -1; dragYAccum = 0f; lastTargetIdx = -1 },
            )
        }
    ) {
        orderList.forEachIndexed { index, item ->
            val isDragged = draggingIdx == index
            val offsetDp = with(density) { (offsetAnimatables[index]?.value ?: 0f).toDp() }

            Surface(
                color = if (isDragged) Color(0xFF4A90D9).copy(0.25f) else Color(0xFF0A1520),
                shape = RoundedCornerShape(10.dp),
                shadowElevation = if (isDragged) 8.dp else 0.dp,
                modifier = Modifier
                    .fillMaxWidth().height(itemHeightDp)
                    .zIndex(if (isDragged) 10f else 0f)
                    .offset(y = offsetDp)
                    .scale(if (isDragged) 1.03f else 1f)
                    .border(
                        if (isDragged) 1.5.dp else 0.5.dp,
                        if (isDragged) Color(0xFF4A90D9) else Color.White.copy(0.08f),
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier.size(22.dp)
                                .background(Color(0xFF4A90D9).copy(0.15f), RoundedCornerShape(5.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${index + 1}", fontSize = 11.sp,
                                color = Color(0xFF90CAF9), fontWeight = FontWeight.Bold)
                        }
                        Text(iconProvider(item), fontSize = 16.sp)
                        Text(labelProvider(item), fontSize = 13.sp, color = Color.White,
                            fontWeight = FontWeight.Medium, maxLines = 1)
                    }
                    Text("⠿", fontSize = 18.sp,
                        color = if (isDragged) Color(0xFF90CAF9) else Color(0xFF546E7A))
                }
            }
        }
    }
}

@Composable
private fun LMediaGroupDragList(
    items: List<LandscapeMediaRowGroup>,
    onReorder: (List<LandscapeMediaRowGroup>) -> Unit,
) {
    var orderList by remember(items) { mutableStateOf(items.toList()) }
    var draggingIdx by remember { mutableIntStateOf(-1) }
    var dragYAccum by remember { mutableFloatStateOf(0f) }
    var lastTargetIdx by remember { mutableIntStateOf(-1) }
    var isCommitting by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val itemHeightDp = 72.dp
    val spacingDp = 6.dp
    val slotPx = with(density) { (itemHeightDp + spacingDp).toPx() }

    val targetIdx = if (draggingIdx >= 0)
        (draggingIdx + (dragYAccum / slotPx).roundToInt()).coerceIn(0, orderList.size - 1)
    else -1

    LaunchedEffect(targetIdx) { if (targetIdx >= 0) lastTargetIdx = targetIdx }

    val offsetAnimatables = remember {
        mutableStateMapOf<LandscapeMediaRowGroup, Animatable<Float, AnimationVector1D>>()
    }
    orderList.forEach { g ->
        if (!offsetAnimatables.containsKey(g)) offsetAnimatables[g] = Animatable(0f)
    }

    val coroutineScope = rememberCoroutineScope()

    orderList.forEachIndexed { index, group ->
        val isDragged = draggingIdx == index
        val targetOffsetPx = when {
            isDragged -> dragYAccum
            draggingIdx < 0 -> 0f
            targetIdx > draggingIdx && index in (draggingIdx + 1)..targetIdx -> -slotPx
            targetIdx < draggingIdx && index in targetIdx until draggingIdx -> slotPx
            else -> 0f
        }
        LaunchedEffect(group, targetOffsetPx, isDragged) {
            if (isCommitting) return@LaunchedEffect
            val anim = offsetAnimatables[group] ?: return@LaunchedEffect
            if (isDragged) anim.snapTo(targetOffsetPx)
            else anim.animateTo(targetOffsetPx, spring(dampingRatio = 0.8f, stiffness = 300f))
        }
    }

    fun commitReorder() {
        val from = draggingIdx; val to = lastTargetIdx
        draggingIdx = -1; dragYAccum = 0f; lastTargetIdx = -1
        if (from < 0 || to < 0 || to == from) return
        val newList = orderList.toMutableList()
        val item = newList.removeAt(from)
        newList.add(to, item)
        coroutineScope.launch {
            isCommitting = true
            orderList.forEach { offsetAnimatables[it]?.snapTo(0f) }
            orderList = newList
            onReorder(newList)
            isCommitting = false
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(spacingDp),
        modifier = Modifier.fillMaxWidth().pointerInput(orderList) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    if (isCommitting) return@detectDragGesturesAfterLongPress
                    val idx = (offset.y / slotPx).toInt().coerceIn(orderList.indices)
                    draggingIdx = idx; dragYAccum = 0f; lastTargetIdx = idx
                },
                onDrag = { change, amount ->
                    if (draggingIdx >= 0 && !isCommitting) {
                        change.consume(); dragYAccum += amount.y
                    }
                },
                onDragEnd = { if (!isCommitting) commitReorder() },
                onDragCancel = { draggingIdx = -1; dragYAccum = 0f; lastTargetIdx = -1 },
            )
        }
    ) {
        orderList.forEachIndexed { index, group ->
            val isDragged = draggingIdx == index
            val offsetDp = with(density) { (offsetAnimatables[group]?.value ?: 0f).toDp() }

            Surface(
                color = if (isDragged) Color(0xFF4A90D9).copy(0.3f) else Color(0xFF0A1520),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = if (isDragged) 12.dp else 0.dp,
                modifier = Modifier
                    .fillMaxWidth().height(itemHeightDp)
                    .zIndex(if (isDragged) 10f else 0f)
                    .offset(y = offsetDp)
                    .scale(if (isDragged) 1.04f else 1f)
                    .border(
                        if (isDragged) 2.dp else 0.5.dp,
                        if (isDragged) Color(0xFF4A90D9) else Color.White.copy(0.08f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.size(24.dp)
                                .background(
                                    if (isDragged) Color(0xFF4A90D9).copy(0.3f)
                                    else Color(0xFF4A90D9).copy(0.15f),
                                    RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${index + 1}", fontSize = 12.sp,
                                color = if (isDragged) Color.White else Color(0xFF90CAF9),
                                fontWeight = FontWeight.Bold)
                        }
                        Text(group.icon, fontSize = 20.sp)
                        Column {
                            Text(group.label, fontSize = 14.sp,
                                color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(
                                when (group) {
                                    LandscapeMediaRowGroup.TRANSPORT -> "⏮ ⏯ ⏹ ⏭"
                                    LandscapeMediaRowGroup.VOLUME -> "🔇 🔉 🔊"
                                    LandscapeMediaRowGroup.BRIGHTNESS -> "🔅 🔆"
                                },
                                fontSize = 11.sp, color = Color(0xFF607D8B),
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("⠿", fontSize = 22.sp,
                            color = if (isDragged) Color(0xFF90CAF9) else Color(0xFF546E7A))
                        Text("Hold & drag", fontSize = 7.sp,
                            color = Color(0xFF455A64), maxLines = 1)
                    }
                }
            }
        }
    }
}