package com.arena.hidcompatibilitytester.settings.ui.content

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arena.hidcompatibilitytester.settings.*
import com.arena.hidcompatibilitytester.settings.ui.*

@Composable
fun PortraitKeyboardContent(
    subSectionId: String,
    settings: AppSettings,
    isLandscape: Boolean,
    onSettingsChange: (AppSettings) -> Unit,
) {
    val latestSettings by rememberUpdatedState(settings)
    val latestOnChange by rememberUpdatedState(onSettingsChange)

    fun update(transform: (PortraitKeyboardSettings) -> PortraitKeyboardSettings) {
        val current = latestSettings.portraitKeyboard
        var newSettings = latestSettings.copy(portraitKeyboard = transform(current))
        newSettings = SettingsSyncManager.applyKeyboardSyncFromPortrait(newSettings)
        latestOnChange(newSettings)
    }

    when (subSectionId) {
        "keys" -> KeyboardKeysSection(settings.portraitKeyboard, isLandscape, ::update)
        "behavior" -> PortraitKeyboardBehaviorSection(settings.portraitKeyboard, isLandscape, ::update)
        "appearance" -> PortraitKeyboardAppearanceSection(settings.portraitKeyboard, isLandscape, ::update)
        "numpad" -> KeyboardNumpadSection(settings.portraitKeyboard, isLandscape, ::update)
        "media" -> KeyboardMediaSection(settings.portraitKeyboard, isLandscape, ::update)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// KEYS SECTION (shared shape between portrait/landscape)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun KeyboardKeysSection(
    kb: PortraitKeyboardSettings,
    isLandscape: Boolean,
    onUpdate: ((PortraitKeyboardSettings) -> PortraitKeyboardSettings) -> Unit,
) {
    val groups = @Composable {
        // ── Key Repeat ─────────────────────────────
        SettingsGroupCard(title = "Key Repeat") {
            SettingsToggle(
                label = "Enable Key Repeat",
                subtitle = "Hold a key to repeat it",
                checked = kb.repeatEnabled,
            ) { newValue -> onUpdate { s -> s.copy(repeatEnabled = newValue) } }
        }

        // ── Feedback ───────────────────────────────
        SettingsGroupCard(title = "Feedback") {
            SettingsToggle("Haptic Feedback", checked = kb.hapticEnabled, subtitle = "Vibrate on key press") {
                onUpdate { s -> s.copy(hapticEnabled = it) }
            }
            SettingsToggle("Sound on Press", checked = kb.soundOnPress) {
                onUpdate { s -> s.copy(soundOnPress = it) }
            }
        }

        // ── Section Visibility ─────────────────────
        SettingsGroupCard(title = "Section Visibility") {
            SettingsToggle("Navigation", checked = kb.keysTabShowNavigation) {
                onUpdate { s -> s.copy(keysTabShowNavigation = it) }
            }
            SettingsToggle("Arrow Keys", checked = kb.keysTabShowArrowKeys) {
                onUpdate { s -> s.copy(keysTabShowArrowKeys = it) }
            }
            SettingsToggle("System Keys", checked = kb.keysTabShowSystemKeys) {
                onUpdate { s -> s.copy(keysTabShowSystemKeys = it) }
            }
            SettingsToggle("Quick Modifiers", checked = kb.keysTabShowQuickMods) {
                onUpdate { s -> s.copy(keysTabShowQuickMods = it) }
            }
        }

        // ── Style ──────────────────────────────────
        SettingsGroupCard(title = "Style") {
            SettingsChipSelector(
                label = "Section Style",
                options = SettingsSectionStyle.entries.map { it.label },
                selectedIndex = kb.keysTabSectionStyle.ordinal,
            ) { i -> onUpdate { s -> s.copy(keysTabSectionStyle = SettingsSectionStyle.entries[i]) } }
        }

        // ── Section Order ──────────────────────────
        SettingsGroupCard(title = "Section Order") {
            val merge = kb.mergeSystemAndModsGlobal || kb.keysTabMergeSystemAndMods
            val visibleKeySections = kb.keysTabSectionOrder.filter { section ->
                when (section) {
                    SettingsKeysTabSection.NAV_ARROWS -> kb.keysTabShowNavigation || kb.keysTabShowArrowKeys
                    SettingsKeysTabSection.SYSTEM_KEYS -> kb.keysTabShowSystemKeys && !merge
                    SettingsKeysTabSection.QUICK_MODS -> kb.keysTabShowQuickMods && !merge
                    SettingsKeysTabSection.MERGED_SYSTEM_MODS -> merge && kb.keysTabShowSystemKeys && kb.keysTabShowQuickMods
                }
            }
            SettingsDragReorderList(
                items = visibleKeySections,
                labelProvider = { it.label },
                iconProvider = { it.icon },
                onReorder = { newOrder ->
                    val visible = newOrder.toSet()
                    val hidden = kb.keysTabSectionOrder.filter { it !in visible }
                    onUpdate { it.copy(keysTabSectionOrder = newOrder + hidden) }
                },
            )
        }
    }

    RenderKeyboardGroups(isLandscape, groups)
}

// ═════════════════════════════════════════════════════════════════════════════
// BEHAVIOR SECTION (portrait-only fields)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun PortraitKeyboardBehaviorSection(
    kb: PortraitKeyboardSettings,
    isLandscape: Boolean,
    onUpdate: ((PortraitKeyboardSettings) -> PortraitKeyboardSettings) -> Unit,
) {
    val groups = @Composable {
        SettingsGroupCard(title = "Key Repeat Timing") {
            SettingsSlider(
                label = "Initial Delay",
                value = kb.repeatInitialDelayMs.toFloat(),
                range = 100f..800f,
                display = { "${it.toLong()}ms" },
                minLabel = "Short", maxLabel = "Long",
                enabled = kb.repeatEnabled,
            ) { v -> onUpdate { s -> s.copy(repeatInitialDelayMs = v.toLong()) } }

            SettingsSlider(
                label = "Repeat Speed",
                value = kb.repeatIntervalMs.toFloat(),
                range = 20f..150f,
                display = { "${it.toLong()}ms" },
                minLabel = "Fast", maxLabel = "Slow",
                enabled = kb.repeatEnabled,
            ) { v -> onUpdate { s -> s.copy(repeatIntervalMs = v.toLong()) } }
        }

        SettingsGroupCard(title = "Haptics") {
            SettingsChipSelector(
                label = "Intensity",
                options = SettingsHapticIntensity.entries.map { it.label },
                selectedIndex = kb.hapticIntensity.ordinal,
                enabled = kb.hapticEnabled,
            ) { i -> onUpdate { s -> s.copy(hapticIntensity = SettingsHapticIntensity.entries[i]) } }
        }

        SettingsGroupCard(title = "Modifier Keys") {
            SettingsToggle(
                "Sticky Modifiers",
                subtitle = "Mods stay held until cleared",
                checked = kb.stickyModifiers,
            ) { onUpdate { s -> s.copy(stickyModifiers = it) } }

            SettingsToggle(
                "Keep Mods After Tab",
                subtitle = "Mod+Tab releases only Tab",
                checked = kb.keepModsAfterTab,
                enabled = !kb.stickyModifiers,
            ) { onUpdate { s -> s.copy(keepModsAfterTab = it) } }
        }

        SettingsGroupCard(title = "Global Toggles") {
            SettingsToggle("Merge System & Modifiers (all tabs)", checked = kb.mergeSystemAndModsGlobal) {
                onUpdate { s -> s.copy(mergeSystemAndModsGlobal = it) }
            }
            SettingsToggle("In-Place Reorder (all tabs)", checked = kb.inPlaceReorderGlobal) {
                onUpdate { s -> s.copy(inPlaceReorderGlobal = it) }
            }
        }

        SettingsGroupCard(title = "Status Bar") {
            SettingsToggle("Show LED Indicators", checked = kb.showStatusBar) {
                onUpdate { s -> s.copy(showStatusBar = it) }
            }
            SettingsToggle("Show Combo Preview", checked = kb.showComboPreview) {
                onUpdate { s -> s.copy(showComboPreview = it) }
            }
        }

        SettingsGroupCard(title = "Default Tab") {
            SettingsChipSelector(
                label = "Opens First",
                options = listOf("⌨ Keys", "↕ Nav+Num", "🎵 Media"),
                selectedIndex = kb.defaultTab.coerceIn(0, 2),
            ) { i -> onUpdate { s -> s.copy(defaultTab = i) } }
        }
    }

    RenderKeyboardGroups(isLandscape, groups)
}

// ═════════════════════════════════════════════════════════════════════════════
// APPEARANCE SECTION (portrait-only fields)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun PortraitKeyboardAppearanceSection(
    kb: PortraitKeyboardSettings,
    isLandscape: Boolean,
    onUpdate: ((PortraitKeyboardSettings) -> PortraitKeyboardSettings) -> Unit,
) {
    val groups = @Composable {
        SettingsGroupCard(title = "Size") {
            SettingsChipSelector(
                label = "Key Height",
                options = SettingsKeyHeight.entries.map { it.label },
                selectedIndex = kb.keyHeight.ordinal,
            ) { i -> onUpdate { s -> s.copy(keyHeight = SettingsKeyHeight.entries[i]) } }

            SettingsChipSelector(
                label = "Font Size",
                options = SettingsKeyFontSize.entries.map { it.label },
                selectedIndex = kb.keyFontSize.ordinal,
            ) { i -> onUpdate { s -> s.copy(keyFontSize = SettingsKeyFontSize.entries[i]) } }
        }

        SettingsGroupCard(title = "Labels") {
            SettingsToggle("Show Key Hints", subtitle = "Shift chars above keys", checked = kb.showKeyHints) {
                onUpdate { s -> s.copy(showKeyHints = it) }
            }
            SettingsToggle("Compact Modifiers", checked = kb.compactModifiers) {
                onUpdate { s -> s.copy(compactModifiers = it) }
            }
            SettingsToggle("High Contrast Mode", checked = kb.highContrastMode) {
                onUpdate { s -> s.copy(highContrastMode = it) }
            }
        }

        SettingsGroupCard(title = "Optional Rows Visibility") {
            SettingsToggle(
                "Global Visibility",
                subtitle = "Same visibility across all keyboards",
                checked = kb.globalOptionalRowVisibility,
            ) { onUpdate { s -> s.copy(globalOptionalRowVisibility = it) } }

            if (kb.globalOptionalRowVisibility) {
                SettingsToggle("Show Media Row", checked = kb.globalShowMediaRow) {
                    onUpdate { s -> s.copy(globalShowMediaRow = it) }
                }
                SettingsToggle("Show Nav Row", checked = kb.globalShowNavRow) {
                    onUpdate { s -> s.copy(globalShowNavRow = it) }
                }
            } else {
                SettingsToggle("Media Row — Keys Tab", checked = kb.keysTabShowMediaRow) {
                    onUpdate { s -> s.copy(keysTabShowMediaRow = it) }
                }
                SettingsToggle("Media Row — Trackpad", checked = kb.trackpadShowMediaRow) {
                    onUpdate { s -> s.copy(trackpadShowMediaRow = it) }
                }
                SettingsToggle("Nav Row — Keys Tab", checked = kb.keysTabShowNavRow) {
                    onUpdate { s -> s.copy(keysTabShowNavRow = it) }
                }
                SettingsToggle("Nav Row — Trackpad", checked = kb.trackpadShowNavRow) {
                    onUpdate { s -> s.copy(trackpadShowNavRow = it) }
                }
            }
        }

        SettingsGroupCard(title = "Optional Rows Order") {
            SettingsToggle(
                "Global Order",
                subtitle = "Same order across all keyboards",
                checked = kb.globalOptionalRowOrder,
            ) { onUpdate { s -> s.copy(globalOptionalRowOrder = it) } }

            SettingsDragReorderList(
                items = if (kb.globalOptionalRowOrder) kb.keyboardOptionalRowOrder else kb.keysTabOptionalRowOrder,
                labelProvider = { it.label },
                iconProvider = { it.icon },
                onReorder = { newOrder ->
                    onUpdate { s ->
                        if (s.globalOptionalRowOrder) {
                            s.copy(
                                keyboardOptionalRowOrder = newOrder,
                                keysTabOptionalRowOrder = newOrder,
                                trackpadOptionalRowOrder = newOrder,
                            )
                        } else {
                            s.copy(keysTabOptionalRowOrder = newOrder)
                        }
                    }
                },
            )
        }
    }

    RenderKeyboardGroups(isLandscape, groups)
}

// ═════════════════════════════════════════════════════════════════════════════
// NUMPAD SECTION
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun KeyboardNumpadSection(
    kb: PortraitKeyboardSettings,
    isLandscape: Boolean,
    onUpdate: ((PortraitKeyboardSettings) -> PortraitKeyboardSettings) -> Unit,
) {
    val groups = @Composable {
        SettingsGroupCard(title = "Numpad Behavior") {
            SettingsToggle(
                "Start with NumLock On",
                subtitle = "Default to number mode",
                checked = kb.numpadStartsLocked,
            ) { onUpdate { s -> s.copy(numpadStartsLocked = it) } }

            SettingsToggle(
                "Show Alternate Hints",
                subtitle = "Show nav labels when toggled",
                checked = kb.numpadShowHints,
            ) { onUpdate { s -> s.copy(numpadShowHints = it) } }
        }

        SettingsGroupCard(title = "Nav+Numpad Tab Sections") {
            SettingsToggle("Navigation", checked = kb.navTabShowNavigation) {
                onUpdate { s -> s.copy(navTabShowNavigation = it) }
            }
            SettingsToggle("Arrow Keys", checked = kb.navTabShowArrowKeys) {
                onUpdate { s -> s.copy(navTabShowArrowKeys = it) }
            }
            SettingsToggle("Insert/Overwrite Toggle", checked = kb.navTabShowInsertToggle) {
                onUpdate { s -> s.copy(navTabShowInsertToggle = it) }
            }
            SettingsToggle("System Keys", checked = kb.navTabShowSystemKeys) {
                onUpdate { s -> s.copy(navTabShowSystemKeys = it) }
            }
            SettingsToggle("Quick Modifiers", checked = kb.navTabShowQuickMods) {
                onUpdate { s -> s.copy(navTabShowQuickMods = it) }
            }
            SettingsToggle("Type & Send Text", checked = kb.navTabShowTypeText) {
                onUpdate { s -> s.copy(navTabShowTypeText = it) }
            }
        }

        SettingsGroupCard(title = "Nav Tab Options") {
            SettingsToggle("Merge System & Modifiers", checked = kb.navTabMergeSystemAndMods) {
                onUpdate { s -> s.copy(navTabMergeSystemAndMods = it) }
            }
            SettingsToggle("In-Place Reorder", checked = kb.navTabInPlaceReorder) {
                onUpdate { s -> s.copy(navTabInPlaceReorder = it) }
            }
            SettingsToggle("Swap Nav ↔ Arrows", checked = kb.navTabNavArrowsSwapped) {
                onUpdate { s -> s.copy(navTabNavArrowsSwapped = it) }
            }
        }

        SettingsGroupCard(title = "Nav Tab Section Order") {
            val merge = kb.mergeSystemAndModsGlobal || kb.navTabMergeSystemAndMods
            val visibleNavSections = kb.navTabSectionOrder.filter { section ->
                when (section) {
                    SettingsNavTabSection.NAV_ARROWS -> kb.navTabShowNavigation || kb.navTabShowArrowKeys
                    SettingsNavTabSection.INSERT_TOGGLE -> kb.navTabShowInsertToggle
                    SettingsNavTabSection.SYSTEM_KEYS -> kb.navTabShowSystemKeys && !merge
                    SettingsNavTabSection.QUICK_MODS -> kb.navTabShowQuickMods && !merge
                    SettingsNavTabSection.MERGED_SYSTEM_MODS -> merge && kb.navTabShowSystemKeys && kb.navTabShowQuickMods
                    SettingsNavTabSection.TYPE_TEXT -> kb.navTabShowTypeText
                }
            }
            SettingsDragReorderList(
                items = visibleNavSections,
                labelProvider = { it.label },
                iconProvider = { it.icon },
                onReorder = { newOrder ->
                    val visible = newOrder.toSet()
                    val hidden = kb.navTabSectionOrder.filter { it !in visible }
                    onUpdate { it.copy(navTabSectionOrder = newOrder + hidden) }
                },
            )
        }
    }

    RenderKeyboardGroups(isLandscape, groups)
}

// ═════════════════════════════════════════════════════════════════════════════
// MEDIA SECTION
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun KeyboardMediaSection(
    kb: PortraitKeyboardSettings,
    isLandscape: Boolean,
    onUpdate: ((PortraitKeyboardSettings) -> PortraitKeyboardSettings) -> Unit,
) {
    val merge = kb.mergeSystemAndModsGlobal || kb.mediaTabMergeSystemAndMods
    val bothSystemAndModsEnabled = kb.mediaTabShowSystemKeys && kb.mediaTabShowQuickMods

    val visibleMediaSections = kb.mediaTabSectionOrder.filter { section ->
        when (section) {
            SettingsMediaTabSection.TRANSPORT -> true
            SettingsMediaTabSection.VOLUME_BRIGHTNESS -> true
            SettingsMediaTabSection.NAVIGATION -> kb.mediaTabShowNavigation
            SettingsMediaTabSection.ARROW_KEYS -> kb.mediaTabShowArrowKeys
            SettingsMediaTabSection.SYSTEM_KEYS -> kb.mediaTabShowSystemKeys && !merge
            SettingsMediaTabSection.QUICK_MODS -> kb.mediaTabShowQuickMods && !merge
            SettingsMediaTabSection.MERGED_SYSTEM_MODS -> merge && bothSystemAndModsEnabled
        }
    }

    val visibleGroups = kb.mediaRowGroupOrder.filter { group ->
        when (group) {
            SettingsMediaRowGroup.TRANSPORT -> kb.mediaRowShowTransport
            SettingsMediaRowGroup.VOLUME -> kb.mediaRowShowVolume
            SettingsMediaRowGroup.BRIGHTNESS -> kb.mediaRowShowBrightness
        }
    }

    val groups = @Composable {
        // 1. Media Key Size
        SettingsGroupCard(title = "Media Key Size") {
            SettingsChipSelector(
                label = "Button Size",
                options = SettingsMediaKeySize.entries.map { it.label },
                selectedIndex = kb.mediaKeySize.ordinal,
            ) { i -> onUpdate { it.copy(mediaKeySize = SettingsMediaKeySize.entries[i]) } }
        }

        // 2. Media Tab Style
        SettingsGroupCard(title = "Media Tab Style") {
            SettingsChipSelector(
                label = "Section Style",
                options = SettingsSectionStyle.entries.map { it.label },
                selectedIndex = kb.mediaTabSectionStyle.ordinal,
            ) { i -> onUpdate { it.copy(mediaTabSectionStyle = SettingsSectionStyle.entries[i]) } }

            SettingsToggle(
                "Merge System & Modifiers",
                subtitle = when {
                    kb.mergeSystemAndModsGlobal -> "Controlled by global"
                    !bothSystemAndModsEnabled -> "Enable both System Keys & Quick Modifiers first"
                    else -> "Media tab only"
                },
                checked = kb.mediaTabMergeSystemAndMods,
                enabled = !kb.mergeSystemAndModsGlobal && bothSystemAndModsEnabled,
            ) { newValue -> onUpdate { it.copy(mediaTabMergeSystemAndMods = newValue) } }

            SettingsToggle(
                "In-Place Reorder",
                subtitle = if (kb.inPlaceReorderGlobal) "Controlled by global" else "Media tab only",
                checked = kb.mediaTabInPlaceReorder,
                enabled = !kb.inPlaceReorderGlobal,
            ) { newValue -> onUpdate { it.copy(mediaTabInPlaceReorder = newValue) } }
        }

        // 3. Media Tab Sections
        SettingsGroupCard(title = "Media Tab Sections") {
            SettingsToggle("Navigation", checked = kb.mediaTabShowNavigation) {
                newValue -> onUpdate { it.copy(mediaTabShowNavigation = newValue) }
            }
            SettingsToggle("Arrow Keys", checked = kb.mediaTabShowArrowKeys) {
                newValue -> onUpdate { it.copy(mediaTabShowArrowKeys = newValue) }
            }
            SettingsToggle("System Keys", checked = kb.mediaTabShowSystemKeys) {
                newValue -> onUpdate { it.copy(mediaTabShowSystemKeys = newValue) }
            }
            SettingsToggle("Quick Modifiers", checked = kb.mediaTabShowQuickMods) {
                newValue -> onUpdate { it.copy(mediaTabShowQuickMods = newValue) }
            }
        }

        // 4. Media Tab Section Order
        SettingsGroupCard(title = "Media Tab Section Order") {
            SettingsDragReorderList(
                items = visibleMediaSections,
                labelProvider = { it.label },
                iconProvider = { it.icon },
                onReorder = { newOrder ->
                    val visible = newOrder.toSet()
                    val hidden = kb.mediaTabSectionOrder.filter { it !in visible }
                    onUpdate { it.copy(mediaTabSectionOrder = newOrder + hidden) }
                },
            )
        }

        // 5. Media Row Repeat
        SettingsGroupCard(title = "Media Row Repeat") {
            SettingsToggle("Repeat Volume", checked = kb.mediaRowRepeatVolume,
                subtitle = "Hold volume up/down to repeat") {
                newValue -> onUpdate { it.copy(mediaRowRepeatVolume = newValue) }
            }
            SettingsToggle("Repeat Brightness", checked = kb.mediaRowRepeatBrightness,
                subtitle = "Hold brightness up/down to repeat") {
                newValue -> onUpdate { it.copy(mediaRowRepeatBrightness = newValue) }
            }
        }

        // 6. Media Row Groups
        SettingsGroupCard(title = "Media Row Groups") {
            SettingsToggle("Transport (⏮ ⏯ ⏹ ⏭)", checked = kb.mediaRowShowTransport) {
                newValue -> onUpdate { it.copy(mediaRowShowTransport = newValue) }
            }
            SettingsToggle("Volume (🔇 🔉 🔊)", checked = kb.mediaRowShowVolume) {
                newValue -> onUpdate { it.copy(mediaRowShowVolume = newValue) }
            }
            SettingsToggle("Brightness (🔅 🔆)", checked = kb.mediaRowShowBrightness) {
                newValue -> onUpdate { it.copy(mediaRowShowBrightness = newValue) }
            }
        }

        // 7. Media Group Order
        SettingsGroupCard(title = "Media Group Order") {
            SettingsDragReorderList(
                items = visibleGroups,
                labelProvider = { it.label },
                iconProvider = { it.icon },
                onReorder = { newOrder ->
                    val visible = newOrder.toSet()
                    val hidden = kb.mediaRowGroupOrder.filter { it !in visible }
                    onUpdate { it.copy(mediaRowGroupOrder = newOrder + hidden) }
                },
            )
        }
    }

    RenderKeyboardGroups(isLandscape, groups)
}

// ═════════════════════════════════════════════════════════════════════════════
// LAYOUT HELPER — Option F for landscape, single column for portrait
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun RenderKeyboardGroups(
    isLandscape: Boolean,
    content: @Composable () -> Unit,
) {
    if (isLandscape) {
        AdaptiveHybridGrid(spacing = 8, tallThresholdDp = 250) {
            content()
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            content()
        }
    }
}