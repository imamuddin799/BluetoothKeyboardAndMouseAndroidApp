package com.arena.hidcompatibilitytester.settings.ui.content

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arena.hidcompatibilitytester.settings.*
import com.arena.hidcompatibilitytester.settings.ui.*

@Composable
fun LandscapeKeyboardContent(
    subSectionId: String,
    settings: AppSettings,
    isLandscape: Boolean,
    onSettingsChange: (AppSettings) -> Unit,
) {
    val latestSettings by androidx.compose.runtime.rememberUpdatedState(settings)
    val latestOnChange by androidx.compose.runtime.rememberUpdatedState(onSettingsChange)

    fun update(transform: (LandscapeKeyboardSettings) -> LandscapeKeyboardSettings) {
        val current = latestSettings.landscapeKeyboard
        var newSettings = latestSettings.copy(landscapeKeyboard = transform(current))
        newSettings = SettingsSyncManager.applyKeyboardSyncFromLandscape(newSettings)
        latestOnChange(newSettings)
    }

    when (subSectionId) {
        "keys" -> LandscapeKeyboardKeysSection(settings.landscapeKeyboard, isLandscape, ::update)
        "behavior" -> LandscapeKeyboardBehaviorSection(settings.landscapeKeyboard, isLandscape, ::update)
        "numpad" -> LandscapeKeyboardNumpadSection(settings.landscapeKeyboard, isLandscape, ::update)
        "media" -> LandscapeKeyboardMediaSection(settings.landscapeKeyboard, isLandscape, ::update)
    }
}

@Composable
private fun LandscapeKeyboardKeysSection(
    kb: LandscapeKeyboardSettings,
    isLandscape: Boolean,
    onUpdate: ((LandscapeKeyboardSettings) -> LandscapeKeyboardSettings) -> Unit,
) {
    val merge = kb.mergeSystemAndModsGlobal || kb.keysTabMergeSystemAndMods
    val bothSystemAndModsEnabled = kb.keysTabShowSystemKeys && kb.keysTabShowQuickMods

    val visibleKeySections = kb.keysTabSectionOrder.filter { section ->
        when (section) {
            SettingsKeysTabSection.NAV_ARROWS -> kb.keysTabShowNavigation || kb.keysTabShowArrowKeys
            SettingsKeysTabSection.SYSTEM_KEYS -> kb.keysTabShowSystemKeys && !merge
            SettingsKeysTabSection.QUICK_MODS -> kb.keysTabShowQuickMods && !merge
            SettingsKeysTabSection.MERGED_SYSTEM_MODS -> merge && bothSystemAndModsEnabled
        }
    }

    val groups = @Composable {
        // 1. Size
        SettingsGroupCard(title = "Size") {
            SettingsChipSelector(
                label = "Key Height",
                options = SettingsKeyHeight.entries.map { it.label },
                selectedIndex = kb.keyHeight.ordinal,
            ) { i -> onUpdate { it.copy(keyHeight = SettingsKeyHeight.entries[i]) } }

            SettingsChipSelector(
                label = "Font Size",
                options = SettingsKeyFontSize.entries.map { it.label },
                selectedIndex = kb.keyFontSize.ordinal,
            ) { i -> onUpdate { it.copy(keyFontSize = SettingsKeyFontSize.entries[i]) } }
        }

        // 2. Key Repeat
        SettingsGroupCard(title = "Key Repeat") {
            SettingsToggle(
                "Enable Key Repeat",
                subtitle = "Hold a key to repeat it",
                checked = kb.repeatEnabled,
            ) { newValue -> onUpdate { it.copy(repeatEnabled = newValue) } }

            if (!kb.repeatEnabled) {
                SettingsHint(
                    text = "Key repeat is disabled. Timing settings below have no effect.",
                    variant = SettingsHintVariant.WARNING,
                )
            }
        }

        // 3. Key Repeat Timing
        SettingsGroupCard(title = "Key Repeat Timing") {
            if (!kb.repeatEnabled) {
                SettingsHint(
                    text = "Enable Key Repeat above to use these settings.",
                    variant = SettingsHintVariant.WARNING,
                )
            }

            SettingsSlider(
                label = "Initial Delay",
                value = kb.repeatInitialDelayMs.toFloat(),
                range = 100f..800f,
                display = { "${it.toLong()}ms" },
                minLabel = "Short", maxLabel = "Long",
                enabled = kb.repeatEnabled,
            ) { v -> onUpdate { it.copy(repeatInitialDelayMs = v.toLong()) } }

            SettingsSlider(
                label = "Repeat Speed",
                value = kb.repeatIntervalMs.toFloat(),
                range = 20f..150f,
                display = { "${it.toLong()}ms" },
                minLabel = "Fast", maxLabel = "Slow",
                enabled = kb.repeatEnabled,
            ) { v -> onUpdate { it.copy(repeatIntervalMs = v.toLong()) } }
        }

        // 4. Style (Keys Tab Per-Tab Overrides)
        SettingsGroupCard(title = "Style") {
            SettingsChipSelector(
                label = "Section Style",
                options = SettingsSectionStyle.entries.map { it.label },
                selectedIndex = kb.keysTabSectionStyle.ordinal,
            ) { i -> onUpdate { it.copy(keysTabSectionStyle = SettingsSectionStyle.entries[i]) } }

            SettingsToggle(
                "Merge System & Modifiers",
                subtitle = when {
                    kb.mergeSystemAndModsGlobal -> "Controlled by global"
                    !bothSystemAndModsEnabled -> "Enable both System Keys & Quick Modifiers first"
                    else -> "Keys tab only"
                },
                checked = kb.keysTabMergeSystemAndMods,
                enabled = !kb.mergeSystemAndModsGlobal && bothSystemAndModsEnabled,
            ) { newValue -> onUpdate { it.copy(keysTabMergeSystemAndMods = newValue) } }

            if (kb.mergeSystemAndModsGlobal) {
                SettingsHint(
                    text = "Global merge is ON. Go to Behavior → Global Toggles to turn it off.",
                    icon = "🔒",
                    variant = SettingsHintVariant.INFO,
                )
            }

            SettingsToggle(
                "In-Place Reorder",
                subtitle = if (kb.inPlaceReorderGlobal) "Controlled by global" else "Keys tab only",
                checked = kb.keysTabInPlaceReorder,
                enabled = !kb.inPlaceReorderGlobal,
            ) { newValue -> onUpdate { it.copy(keysTabInPlaceReorder = newValue) } }

            if (kb.inPlaceReorderGlobal) {
                SettingsHint(
                    text = "Global in-place reorder is ON. Go to Behavior → Global Toggles to turn it off.",
                    icon = "🔒",
                    variant = SettingsHintVariant.INFO,
                )
            }

            SettingsToggle("Swap Nav ↔ Arrows", checked = kb.keysTabNavArrowsSwapped) {
                newValue -> onUpdate { it.copy(keysTabNavArrowsSwapped = newValue) }
            }
        }

        // 5. Section Visibility
        SettingsGroupCard(title = "Section Visibility") {
            SettingsToggle("Navigation", checked = kb.keysTabShowNavigation) {
                newValue -> onUpdate { it.copy(keysTabShowNavigation = newValue) }
            }
            SettingsToggle("Arrow Keys", checked = kb.keysTabShowArrowKeys) {
                newValue -> onUpdate { it.copy(keysTabShowArrowKeys = newValue) }
            }
            SettingsToggle("System Keys", checked = kb.keysTabShowSystemKeys) {
                newValue -> onUpdate { it.copy(keysTabShowSystemKeys = newValue) }
            }
            SettingsToggle("Quick Modifiers", checked = kb.keysTabShowQuickMods) {
                newValue -> onUpdate { it.copy(keysTabShowQuickMods = newValue) }
            }
        }

        // 6. Section Order
        SettingsGroupCard(title = "Section Order") {
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

        // 7. Optional Rows Visibility
        SettingsGroupCard(title = "Optional Rows Visibility") {
            SettingsToggle(
                "Global Visibility",
                subtitle = "Same visibility across all keyboards",
                checked = kb.globalOptionalRowVisibility,
            ) { newValue -> onUpdate { it.copy(globalOptionalRowVisibility = newValue) } }

            if (kb.globalOptionalRowVisibility) {
                SettingsToggle("Show Media Row", checked = kb.globalShowMediaRow) {
                    newValue -> onUpdate { it.copy(globalShowMediaRow = newValue) }
                }
                SettingsToggle("Show Nav Row", checked = kb.globalShowNavRow) {
                    newValue -> onUpdate { it.copy(globalShowNavRow = newValue) }
                }
            } else {
                SettingsToggle("Media Row — Keys Tab", checked = kb.keysTabShowMediaRow) {
                    newValue -> onUpdate { it.copy(keysTabShowMediaRow = newValue) }
                }
                SettingsToggle("Media Row — Trackpad", checked = kb.trackpadShowMediaRow) {
                    newValue -> onUpdate { it.copy(trackpadShowMediaRow = newValue) }
                }
                SettingsToggle("Nav Row — Keys Tab", checked = kb.keysTabShowNavRow) {
                    newValue -> onUpdate { it.copy(keysTabShowNavRow = newValue) }
                }
                SettingsToggle("Nav Row — Trackpad", checked = kb.trackpadShowNavRow) {
                    newValue -> onUpdate { it.copy(trackpadShowNavRow = newValue) }
                }
            }

            if (kb.globalOptionalRowVisibility) {
                SettingsHint(
                    text = "Global visibility is ON. Turn it off to customize per keyboard.",
                    icon = "🔒",
                    variant = SettingsHintVariant.INFO,
                )
            }
        }

        // 8. Optional Row Order
        if (kb.globalOptionalRowVisibility && kb.globalShowMediaRow && kb.globalShowNavRow) {
            SettingsGroupCard(title = "Optional Row Order") {
                SettingsDragReorderList(
                    items = kb.keyboardOptionalRowOrder,
                    labelProvider = { it.label },
                    iconProvider = { it.icon },
                    onReorder = { newOrder ->
                        onUpdate {
                            it.copy(
                                keyboardOptionalRowOrder = newOrder,
                                keysTabOptionalRowOrder = newOrder,
                                trackpadOptionalRowOrder = newOrder,
                            )
                        }
                    },
                )
            }
        } else if (!kb.globalOptionalRowVisibility) {
            val keysTabHasRows = kb.keysTabShowMediaRow || kb.keysTabShowNavRow
            val trackpadHasRows = kb.trackpadShowMediaRow || kb.trackpadShowNavRow

            if (keysTabHasRows || trackpadHasRows) {
                SettingsGroupCard(title = "Optional Row Order") {
                    if (keysTabHasRows) {
                        SettingsHint(
                            text = "Keys Tab Order",
                            icon = "⌨",
                            variant = SettingsHintVariant.INFO,
                        )
                        SettingsDragReorderList(
                            items = kb.keysTabOptionalRowOrder.filter { row ->
                                when (row) {
                                    SettingsOptionalRow.MEDIA_ROW -> kb.keysTabShowMediaRow
                                    SettingsOptionalRow.NAV_ROW -> kb.keysTabShowNavRow
                                }
                            },
                            labelProvider = { it.label },
                            iconProvider = { it.icon },
                            onReorder = { newOrder ->
                                val visible = newOrder.toSet()
                                val hidden = kb.keysTabOptionalRowOrder.filter { it !in visible }
                                onUpdate { it.copy(keysTabOptionalRowOrder = newOrder + hidden) }
                            },
                        )
                    }

                    if (trackpadHasRows) {
                        SettingsHint(
                            text = "Trackpad Keyboard Order",
                            icon = "🖱",
                            variant = SettingsHintVariant.INFO,
                        )
                        SettingsDragReorderList(
                            items = kb.trackpadOptionalRowOrder.filter { row ->
                                when (row) {
                                    SettingsOptionalRow.MEDIA_ROW -> kb.trackpadShowMediaRow
                                    SettingsOptionalRow.NAV_ROW -> kb.trackpadShowNavRow
                                }
                            },
                            labelProvider = { it.label },
                            iconProvider = { it.icon },
                            onReorder = { newOrder ->
                                val visible = newOrder.toSet()
                                val hidden = kb.trackpadOptionalRowOrder.filter { it !in visible }
                                onUpdate { it.copy(trackpadOptionalRowOrder = newOrder + hidden) }
                            },
                        )
                    }
                }
            }
        }

        // 9. Status Bar
        SettingsGroupCard(title = "Status Bar") {
            SettingsToggle("Show LED Indicators", checked = kb.showStatusBar) {
                newValue -> onUpdate { it.copy(showStatusBar = newValue) }
            }
            SettingsToggle("Show Combo Preview", checked = kb.showComboPreview) {
                newValue -> onUpdate { it.copy(showComboPreview = newValue) }
            }
        }

        // 10. Key Labels
        SettingsGroupCard(title = "Key Labels") {
            SettingsToggle("Show Key Hints", checked = kb.showKeyHints) {
                newValue -> onUpdate { it.copy(showKeyHints = newValue) }
            }
            SettingsToggle("Compact Modifiers", checked = kb.compactModifiers) {
                newValue -> onUpdate { it.copy(compactModifiers = newValue) }
            }
            SettingsToggle("High Contrast Mode", checked = kb.highContrastMode) {
                newValue -> onUpdate { it.copy(highContrastMode = newValue) }
            }
        }

        // 11. Feedback
        SettingsGroupCard(title = "Feedback") {
            SettingsToggle("Haptic Feedback", checked = kb.hapticEnabled, subtitle = "Vibrate on key press") {
                newValue -> onUpdate { it.copy(hapticEnabled = newValue) }
            }
            SettingsToggle("Sound on Press", checked = kb.soundOnPress) {
                newValue -> onUpdate { it.copy(soundOnPress = newValue) }
            }
        }

        // 12. Haptics
        SettingsGroupCard(title = "Haptics") {
            if (!kb.hapticEnabled) {
                SettingsHint(
                    text = "Enable Haptic Feedback above to use intensity settings.",
                    variant = SettingsHintVariant.WARNING,
                )
            }

            SettingsChipSelector(
                label = "Intensity",
                options = SettingsHapticIntensity.entries.map { it.label },
                selectedIndex = kb.hapticIntensity.ordinal,
                enabled = kb.hapticEnabled,
            ) { i -> onUpdate { it.copy(hapticIntensity = SettingsHapticIntensity.entries[i]) } }
        }
    }

    RenderKeyboardGroups(isLandscape, groups)
}

@Composable
private fun LandscapeKeyboardBehaviorSection(
    kb: LandscapeKeyboardSettings,
    isLandscape: Boolean,
    onUpdate: ((LandscapeKeyboardSettings) -> LandscapeKeyboardSettings) -> Unit,
) {
    val groups = @Composable {
        SettingsGroupCard(title = "Modifier Keys") {
            SettingsToggle(
                "Sticky Modifiers",
                subtitle = "Mods stay held until cleared",
                checked = kb.stickyModifiers,
            ) { newValue -> onUpdate { it.copy(stickyModifiers = newValue) } }

            SettingsToggle(
                "Keep Mods After Tab",
                subtitle = "Mod+Tab releases only Tab",
                checked = kb.keepModsAfterTab,
                enabled = !kb.stickyModifiers,
            ) { newValue -> onUpdate { it.copy(keepModsAfterTab = newValue) } }

            if (kb.stickyModifiers) {
                SettingsHint(
                    text = "Sticky ON: Mod + Key → release only Key, Mod stays held. Mod + Tab → release only Tab. Tap Mod again or Clear to release.",
                    icon = "🔒",
                    variant = SettingsHintVariant.INFO,
                )
            } else {
                SettingsHint(
                    text = if (kb.keepModsAfterTab)
                        "Sticky OFF: Mod + Key → release both. Mod + Tab → release only Tab, Mod stays held."
                    else
                        "Sticky OFF: Mod + Key → release both. Mod + Tab → release both.",
                    icon = "⚡",
                    variant = SettingsHintVariant.SUCCESS,
                )
            }
        }

        SettingsGroupCard(title = "Landscape Layout") {
            SettingsChipSelector(
                label = "Default Layout",
                subtitle = "Single = full-width. Two = keyboard + right cluster.",
                options = SettingsLayoutMode.entries.map { it.label },
                selectedIndex = kb.landscapeLayoutMode.ordinal,
            ) { i -> onUpdate { it.copy(landscapeLayoutMode = SettingsLayoutMode.entries[i]) } }
        }

        SettingsGroupCard(title = "Global Toggles") {
            SettingsToggle("Merge System & Modifiers", checked = kb.mergeSystemAndModsGlobal,
                subtitle = "Combine into one section across all tabs") {
                newValue -> onUpdate { it.copy(
                    mergeSystemAndModsGlobal = newValue,
                    keysTabMergeSystemAndMods = newValue,
                    mediaTabMergeSystemAndMods = newValue,
                    navTabMergeSystemAndMods = newValue,
                ) }
            }

            if (kb.mergeSystemAndModsGlobal) {
                SettingsHint(
                    text = "System Keys and Quick Modifiers are merged into one section on all tabs. Per-tab merge settings are overridden.",
                    icon = "🔒",
                    variant = SettingsHintVariant.INFO,
                )
            }

            SettingsToggle("In-Place Reorder", checked = kb.inPlaceReorderGlobal,
                subtitle = "Long-press and drag to reorder on any tab") {
                newValue -> onUpdate { it.copy(
                    inPlaceReorderGlobal = newValue,
                    keysTabInPlaceReorder = newValue,
                    mediaTabInPlaceReorder = newValue,
                    navTabInPlaceReorder = newValue,
                ) }
            }

            if (kb.inPlaceReorderGlobal) {
                SettingsHint(
                    text = "In-place reorder is enabled on all tabs. Per-tab reorder settings are overridden.",
                    icon = "🔒",
                    variant = SettingsHintVariant.INFO,
                )
            }
        }

        SettingsGroupCard(title = "Default Tab") {
            SettingsChipSelector(
                label = "Opens First",
                options = listOf("⌨ Keys", "↕ Nav+Num", "🎵 Media"),
                selectedIndex = kb.defaultTab.coerceIn(0, 2),
            ) { i -> onUpdate { it.copy(defaultTab = i) } }
        }
    }

    RenderKeyboardGroups(isLandscape, groups)
}

@Composable
private fun LandscapeKeyboardNumpadSection(
    kb: LandscapeKeyboardSettings,
    isLandscape: Boolean,
    onUpdate: ((LandscapeKeyboardSettings) -> LandscapeKeyboardSettings) -> Unit,
) {
    val merge = kb.mergeSystemAndModsGlobal || kb.navTabMergeSystemAndMods
    val bothSystemAndModsEnabled = kb.navTabShowSystemKeys && kb.navTabShowQuickMods

    val visibleNavSections = kb.navTabSectionOrder.filter { section ->
        when (section) {
            SettingsNavTabSection.NAV_ARROWS -> kb.navTabShowNavigation || kb.navTabShowArrowKeys
            SettingsNavTabSection.INSERT_TOGGLE -> kb.navTabShowInsertToggle
            SettingsNavTabSection.SYSTEM_KEYS -> kb.navTabShowSystemKeys && !merge
            SettingsNavTabSection.QUICK_MODS -> kb.navTabShowQuickMods && !merge
            SettingsNavTabSection.MERGED_SYSTEM_MODS -> merge && bothSystemAndModsEnabled
            SettingsNavTabSection.TYPE_TEXT -> kb.navTabShowTypeText
        }
    }

    val groups = @Composable {
        // 1. Numpad Behavior
        SettingsGroupCard(title = "Numpad Behavior") {
            SettingsToggle(
                "Start with NumLock On",
                subtitle = "Default to number mode",
                checked = kb.numpadStartsLocked,
            ) { newValue -> onUpdate { it.copy(numpadStartsLocked = newValue) } }

            SettingsToggle(
                "Show Alternate Hints",
                subtitle = "Show nav labels when toggled",
                checked = kb.numpadShowHints,
            ) { newValue -> onUpdate { it.copy(numpadShowHints = newValue) } }
        }

        // 2. Nav Tab Options
        SettingsGroupCard(title = "Nav Tab Options") {
            SettingsChipSelector(
                label = "Section Style",
                subtitle = "Visual style for sections in Nav+Numpad tab",
                options = SettingsSectionStyle.entries.map { it.label },
                selectedIndex = kb.navTabSectionStyle.ordinal,
            ) { i -> onUpdate { it.copy(navTabSectionStyle = SettingsSectionStyle.entries[i]) } }

            SettingsToggle(
                "Merge System & Modifiers",
                subtitle = when {
                    kb.mergeSystemAndModsGlobal -> "Controlled by global"
                    !bothSystemAndModsEnabled -> "Enable both System Keys & Quick Modifiers first"
                    else -> "Nav tab only"
                },
                checked = kb.navTabMergeSystemAndMods,
                enabled = !kb.mergeSystemAndModsGlobal && bothSystemAndModsEnabled,
            ) { newValue -> onUpdate { it.copy(navTabMergeSystemAndMods = newValue) } }

            if (kb.mergeSystemAndModsGlobal) {
                SettingsHint(
                    text = "Global merge is ON. Go to Behavior → Global Toggles to turn it off.",
                    icon = "🔒",
                    variant = SettingsHintVariant.INFO,
                )
            }

            SettingsToggle(
                "In-Place Reorder",
                subtitle = if (kb.inPlaceReorderGlobal) "Controlled by global" else "Nav tab only",
                checked = kb.navTabInPlaceReorder,
                enabled = !kb.inPlaceReorderGlobal,
            ) { newValue -> onUpdate { it.copy(navTabInPlaceReorder = newValue) } }

            if (kb.inPlaceReorderGlobal) {
                SettingsHint(
                    text = "Global in-place reorder is ON. Go to Behavior → Global Toggles to turn it off.",
                    icon = "🔒",
                    variant = SettingsHintVariant.INFO,
                )
            }

            SettingsToggle("Swap Nav ↔ Arrows", checked = kb.navTabNavArrowsSwapped) {
                newValue -> onUpdate { it.copy(navTabNavArrowsSwapped = newValue) }
            }
        }

        // 3. Nav+Numpad Tab Sections
        SettingsGroupCard(title = "Nav+Numpad Tab Sections") {
            SettingsToggle("Navigation", checked = kb.navTabShowNavigation) {
                newValue -> onUpdate { it.copy(navTabShowNavigation = newValue) }
            }
            SettingsToggle("Arrow Keys", checked = kb.navTabShowArrowKeys) {
                newValue -> onUpdate { it.copy(navTabShowArrowKeys = newValue) }
            }
            SettingsToggle("Insert/Overwrite Toggle", checked = kb.navTabShowInsertToggle) {
                newValue -> onUpdate { it.copy(navTabShowInsertToggle = newValue) }
            }
            SettingsToggle("System Keys", checked = kb.navTabShowSystemKeys) {
                newValue -> onUpdate { it.copy(navTabShowSystemKeys = newValue) }
            }
            SettingsToggle("Quick Modifiers", checked = kb.navTabShowQuickMods) {
                newValue -> onUpdate { it.copy(navTabShowQuickMods = newValue) }
            }
            SettingsToggle("Type & Send Text", checked = kb.navTabShowTypeText) {
                newValue -> onUpdate { it.copy(navTabShowTypeText = newValue) }
            }
        }

        // 4. Nav Tab Section Order
        SettingsGroupCard(title = "Nav Tab Section Order") {
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

@Composable
private fun LandscapeKeyboardMediaSection(
    kb: LandscapeKeyboardSettings,
    isLandscape: Boolean,
    onUpdate: ((LandscapeKeyboardSettings) -> LandscapeKeyboardSettings) -> Unit,
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

            if (kb.mergeSystemAndModsGlobal) {
                SettingsHint(
                    text = "Global merge is ON. Go to Behavior → Global Toggles to turn it off.",
                    icon = "🔒",
                    variant = SettingsHintVariant.INFO,
                )
            }

            SettingsToggle(
                "In-Place Reorder",
                subtitle = if (kb.inPlaceReorderGlobal) "Controlled by global" else "Media tab only",
                checked = kb.mediaTabInPlaceReorder,
                enabled = !kb.inPlaceReorderGlobal,
            ) { newValue -> onUpdate { it.copy(mediaTabInPlaceReorder = newValue) } }

            if (kb.inPlaceReorderGlobal) {
                SettingsHint(
                    text = "Global in-place reorder is ON. Go to Behavior → Global Toggles to turn it off.",
                    icon = "🔒",
                    variant = SettingsHintVariant.INFO,
                )
            }
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
            SettingsHint(
                text = "Transport keys never repeat.",
                variant = SettingsHintVariant.INFO,
            )

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
            SettingsHint(
                text = "Drag to reorder groups left → right",
                variant = SettingsHintVariant.INFO,
            )

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