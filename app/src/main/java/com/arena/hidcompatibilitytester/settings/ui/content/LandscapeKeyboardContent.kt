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
        val newSettings = latestSettings.copy(
            landscapeKeyboard = transform(current)
        )
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
                label = "Enable Key Repeat",
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
                minLabel = "Short",
                maxLabel = "Long",
                enabled = kb.repeatEnabled,
            ) { v -> onUpdate { it.copy(repeatInitialDelayMs = v.toLong()) } }

            SettingsSlider(
                label = "Repeat Speed",
                value = kb.repeatIntervalMs.toFloat(),
                range = 20f..150f,
                display = { "${it.toLong()}ms" },
                minLabel = "Fast",
                maxLabel = "Slow",
                enabled = kb.repeatEnabled,
            ) { v -> onUpdate { it.copy(repeatIntervalMs = v.toLong()) } }
        }

        // 4. Optional Rows Visibility
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

                SettingsHint(
                    text = "Global visibility is ON. Turn it off to customize per keyboard.",
                    icon = "🔒",
                    variant = SettingsHintVariant.INFO,
                )
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
        }

        // 5. Optional Row Order
        if (kb.globalOptionalRowVisibility && kb.globalShowMediaRow && kb.globalShowNavRow) {
            SettingsGroupCard(title = "Optional Row Order") {
                SettingsHint(
                    text = "Global Order",
                    icon = "🌐",
                    variant = SettingsHintVariant.INFO,
                )

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

        // 6. Status Bar
        SettingsGroupCard(title = "Status Bar") {
            SettingsToggle("Show LED Indicators", checked = kb.showStatusBar) {
                newValue -> onUpdate { it.copy(showStatusBar = newValue) }
            }
            SettingsToggle("Show Combo Preview", checked = kb.showComboPreview) {
                newValue -> onUpdate { it.copy(showComboPreview = newValue) }
            }
        }

        // 7. Key Labels
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

        // 8. Feedback
        SettingsGroupCard(title = "Feedback") {
            SettingsToggle("Haptic Feedback", checked = kb.hapticEnabled, subtitle = "Vibrate on key press") {
                newValue -> onUpdate { it.copy(hapticEnabled = newValue) }
            }
            SettingsToggle("Sound on Press", checked = kb.soundOnPress) {
                newValue -> onUpdate { it.copy(soundOnPress = newValue) }
            }
        }

        // 9. Haptics
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

            SettingsHint(
                text = "This controls how the main landscape keyboard opens.",
                icon = "🖥",
                variant = SettingsHintVariant.INFO,
            )
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
    val groups = @Composable {
        SettingsGroupCard(title = "Numpad Display") {
            SettingsToggle(
                "Show Alternate Hints",
                subtitle = "Show navigation labels on numpad keys",
                checked = kb.numpadShowHints,
            ) { newValue -> onUpdate { it.copy(numpadShowHints = newValue) } }

            SettingsHint(
                text = "Landscape keyboard always uses the current live numpad state. This setting only affects hint labels.",
                icon = "🔢",
                variant = SettingsHintVariant.INFO,
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
    val visibleGroups = kb.mediaRowGroupOrder.filter { group ->
        when (group) {
            SettingsMediaRowGroup.TRANSPORT -> kb.mediaRowShowTransport
            SettingsMediaRowGroup.VOLUME -> kb.mediaRowShowVolume
            SettingsMediaRowGroup.BRIGHTNESS -> kb.mediaRowShowBrightness
        }
    }

    val groups = @Composable {
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

        SettingsGroupCard(title = "Media Row Repeat") {
            SettingsHint(
                text = "Transport keys never repeat.",
                variant = SettingsHintVariant.INFO,
            )

            SettingsToggle(
                "Repeat Volume",
                subtitle = "Hold volume up/down to repeat",
                checked = kb.mediaRowRepeatVolume,
            ) { newValue -> onUpdate { it.copy(mediaRowRepeatVolume = newValue) } }

            SettingsToggle(
                "Repeat Brightness",
                subtitle = "Hold brightness up/down to repeat",
                checked = kb.mediaRowRepeatBrightness,
            ) { newValue -> onUpdate { it.copy(mediaRowRepeatBrightness = newValue) } }
        }

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