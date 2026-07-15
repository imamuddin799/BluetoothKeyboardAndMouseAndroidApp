package com.arena.hidcompatibilitytester.settings.ui.content

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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
    val latestSettings by rememberUpdatedState(settings)
    val latestOnChange by rememberUpdatedState(onSettingsChange)

    fun update(transform: (LandscapeKeyboardSettings) -> LandscapeKeyboardSettings) {
        val current = latestSettings.landscapeKeyboard
        var newSettings = latestSettings.copy(landscapeKeyboard = transform(current))
        newSettings = SettingsSyncManager.applyKeyboardSyncFromLandscape(newSettings)
        latestOnChange(newSettings)
    }

    when (subSectionId) {
        "keys" -> LandscapeKeyboardKeysSection(settings.landscapeKeyboard, isLandscape, ::update)
        "behavior" -> LandscapeKeyboardBehaviorSection(settings.landscapeKeyboard, isLandscape, ::update)
        "appearance" -> LandscapeKeyboardAppearanceSection(settings.landscapeKeyboard, isLandscape, ::update)
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
        SettingsGroupCard(title = "Key Repeat") {
            SettingsToggle(
                "Enable Key Repeat",
                subtitle = "Hold a key to repeat it",
                checked = kb.repeatEnabled,
            ) { newValue -> onUpdate { s -> s.copy(repeatEnabled = newValue) } }
        }

        SettingsGroupCard(title = "Feedback") {
            SettingsToggle("Haptic Feedback", checked = kb.hapticEnabled) {
                v -> onUpdate { s -> s.copy(hapticEnabled = v) }
            }
            SettingsToggle("Sound on Press", checked = kb.soundOnPress) {
                v -> onUpdate { s -> s.copy(soundOnPress = v) }
            }
        }

        SettingsGroupCard(title = "Section Visibility") {
            SettingsToggle("Navigation", checked = kb.keysTabShowNavigation) {
                v -> onUpdate { s -> s.copy(keysTabShowNavigation = v) }
            }
            SettingsToggle("Arrow Keys", checked = kb.keysTabShowArrowKeys) {
                v -> onUpdate { s -> s.copy(keysTabShowArrowKeys = v) }
            }
            SettingsToggle("System Keys", checked = kb.keysTabShowSystemKeys) {
                v -> onUpdate { s -> s.copy(keysTabShowSystemKeys = v) }
            }
            SettingsToggle("Quick Modifiers", checked = kb.keysTabShowQuickMods) {
                v -> onUpdate { s -> s.copy(keysTabShowQuickMods = v) }
            }
        }

        SettingsGroupCard(title = "Style") {
            SettingsChipSelector(
                label = "Section Style",
                options = SettingsSectionStyle.entries.map { it.label },
                selectedIndex = kb.keysTabSectionStyle.ordinal,
            ) { i -> onUpdate { s -> s.copy(keysTabSectionStyle = SettingsSectionStyle.entries[i]) } }
        }

        SettingsGroupCard(title = "Section Order") {
            SettingsDragReorderList(
                items = kb.keysTabSectionOrder,
                labelProvider = { it.label },
                iconProvider = { it.icon },
                onReorder = { newOrder -> onUpdate { s -> s.copy(keysTabSectionOrder = newOrder) } },
            )
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
            SettingsToggle("Sticky Modifiers", checked = kb.stickyModifiers) {
                v -> onUpdate { s -> s.copy(stickyModifiers = v) }
            }
            SettingsToggle(
                "Keep Mods After Tab",
                checked = kb.keepModsAfterTab,
                enabled = !kb.stickyModifiers,
            ) { v -> onUpdate { s -> s.copy(keepModsAfterTab = v) } }
        }

        SettingsGroupCard(title = "Landscape Layout") {
            SettingsChipSelector(
                label = "Default Layout",
                subtitle = "Single = full-width. Two = keyboard + right cluster.",
                options = SettingsLayoutMode.entries.map { it.label },
                selectedIndex = kb.landscapeLayoutMode.ordinal,
            ) { i -> onUpdate { s -> s.copy(landscapeLayoutMode = SettingsLayoutMode.entries[i]) } }
        }

        SettingsGroupCard(title = "Global Toggles") {
            SettingsToggle("Merge System & Modifiers", checked = kb.mergeSystemAndModsGlobal) {
                v -> onUpdate { s -> s.copy(mergeSystemAndModsGlobal = v) }
            }
            SettingsToggle("In-Place Reorder", checked = kb.inPlaceReorderGlobal) {
                v -> onUpdate { s -> s.copy(inPlaceReorderGlobal = v) }
            }
        }

        SettingsGroupCard(title = "Status Bar") {
            SettingsToggle("Show LED Indicators", checked = kb.showStatusBar) {
                v -> onUpdate { s -> s.copy(showStatusBar = v) }
            }
            SettingsToggle("Show Combo Preview", checked = kb.showComboPreview) {
                v -> onUpdate { s -> s.copy(showComboPreview = v) }
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

@Composable
private fun LandscapeKeyboardAppearanceSection(
    kb: LandscapeKeyboardSettings,
    isLandscape: Boolean,
    onUpdate: ((LandscapeKeyboardSettings) -> LandscapeKeyboardSettings) -> Unit,
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
            SettingsToggle("Show Key Hints", checked = kb.showKeyHints) {
                v -> onUpdate { s -> s.copy(showKeyHints = v) }
            }
            SettingsToggle("Compact Modifiers", checked = kb.compactModifiers) {
                v -> onUpdate { s -> s.copy(compactModifiers = v) }
            }
            SettingsToggle("High Contrast Mode", checked = kb.highContrastMode) {
                v -> onUpdate { s -> s.copy(highContrastMode = v) }
            }
        }

        SettingsGroupCard(title = "Optional Rows Visibility") {
            SettingsToggle("Global Visibility", checked = kb.globalOptionalRowVisibility) {
                v -> onUpdate { s -> s.copy(globalOptionalRowVisibility = v) }
            }

            if (kb.globalOptionalRowVisibility) {
                SettingsToggle("Show Media Row", checked = kb.globalShowMediaRow) {
                    v -> onUpdate { s -> s.copy(globalShowMediaRow = v) }
                }
                SettingsToggle("Show Nav Row", checked = kb.globalShowNavRow) {
                    v -> onUpdate { s -> s.copy(globalShowNavRow = v) }
                }
            } else {
                SettingsToggle("Media Row — Keys Tab", checked = kb.keysTabShowMediaRow) {
                    v -> onUpdate { s -> s.copy(keysTabShowMediaRow = v) }
                }
                SettingsToggle("Media Row — Trackpad", checked = kb.trackpadShowMediaRow) {
                    v -> onUpdate { s -> s.copy(trackpadShowMediaRow = v) }
                }
                SettingsToggle("Nav Row — Keys Tab", checked = kb.keysTabShowNavRow) {
                    v -> onUpdate { s -> s.copy(keysTabShowNavRow = v) }
                }
                SettingsToggle("Nav Row — Trackpad", checked = kb.trackpadShowNavRow) {
                    v -> onUpdate { s -> s.copy(trackpadShowNavRow = v) }
                }
            }
        }

        SettingsGroupCard(title = "Optional Rows Order") {
            SettingsToggle("Global Order", checked = kb.globalOptionalRowOrder) {
                v -> onUpdate { s -> s.copy(globalOptionalRowOrder = v) }
            }
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

@Composable
private fun LandscapeKeyboardNumpadSection(
    kb: LandscapeKeyboardSettings,
    isLandscape: Boolean,
    onUpdate: ((LandscapeKeyboardSettings) -> LandscapeKeyboardSettings) -> Unit,
) {
    val groups = @Composable {
        SettingsGroupCard(title = "Numpad Behavior") {
            SettingsToggle("Start with NumLock On", checked = kb.numpadStartsLocked) {
                v -> onUpdate { s -> s.copy(numpadStartsLocked = v) }
            }
            SettingsToggle("Show Alternate Hints", checked = kb.numpadShowHints) {
                v -> onUpdate { s -> s.copy(numpadShowHints = v) }
            }
        }

        SettingsGroupCard(title = "Nav+Numpad Tab Sections") {
            SettingsToggle("Navigation", checked = kb.navTabShowNavigation) {
                v -> onUpdate { s -> s.copy(navTabShowNavigation = v) }
            }
            SettingsToggle("Arrow Keys", checked = kb.navTabShowArrowKeys) {
                v -> onUpdate { s -> s.copy(navTabShowArrowKeys = v) }
            }
            SettingsToggle("Insert/Overwrite Toggle", checked = kb.navTabShowInsertToggle) {
                v -> onUpdate { s -> s.copy(navTabShowInsertToggle = v) }
            }
            SettingsToggle("System Keys", checked = kb.navTabShowSystemKeys) {
                v -> onUpdate { s -> s.copy(navTabShowSystemKeys = v) }
            }
            SettingsToggle("Quick Modifiers", checked = kb.navTabShowQuickMods) {
                v -> onUpdate { s -> s.copy(navTabShowQuickMods = v) }
            }
            SettingsToggle("Type & Send Text", checked = kb.navTabShowTypeText) {
                v -> onUpdate { s -> s.copy(navTabShowTypeText = v) }
            }
        }

        SettingsGroupCard(title = "Nav Tab Options") {
            SettingsToggle("Merge System & Modifiers", checked = kb.navTabMergeSystemAndMods) {
                v -> onUpdate { s -> s.copy(navTabMergeSystemAndMods = v) }
            }
            SettingsToggle("In-Place Reorder", checked = kb.navTabInPlaceReorder) {
                v -> onUpdate { s -> s.copy(navTabInPlaceReorder = v) }
            }
            SettingsToggle("Swap Nav ↔ Arrows", checked = kb.navTabNavArrowsSwapped) {
                v -> onUpdate { s -> s.copy(navTabNavArrowsSwapped = v) }
            }
        }

        SettingsGroupCard(title = "Nav Tab Section Order") {
            SettingsDragReorderList(
                items = kb.navTabSectionOrder,
                labelProvider = { it.label },
                iconProvider = { it.icon },
                onReorder = { newOrder -> onUpdate { s -> s.copy(navTabSectionOrder = newOrder) } },
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
    val groups = @Composable {
        SettingsGroupCard(title = "Media Key Size") {
            SettingsChipSelector(
                label = "Button Size",
                options = SettingsMediaKeySize.entries.map { it.label },
                selectedIndex = kb.mediaKeySize.ordinal,
            ) { i -> onUpdate { s -> s.copy(mediaKeySize = SettingsMediaKeySize.entries[i]) } }
        }

        SettingsGroupCard(title = "Media Tab Sections") {
            SettingsToggle("Navigation", checked = kb.mediaTabShowNavigation) {
                v -> onUpdate { s -> s.copy(mediaTabShowNavigation = v) }
            }
            SettingsToggle("Arrow Keys", checked = kb.mediaTabShowArrowKeys) {
                v -> onUpdate { s -> s.copy(mediaTabShowArrowKeys = v) }
            }
            SettingsToggle("System Keys", checked = kb.mediaTabShowSystemKeys) {
                v -> onUpdate { s -> s.copy(mediaTabShowSystemKeys = v) }
            }
            SettingsToggle("Quick Modifiers", checked = kb.mediaTabShowQuickMods) {
                v -> onUpdate { s -> s.copy(mediaTabShowQuickMods = v) }
            }
        }

        SettingsGroupCard(title = "Media Tab Style") {
            SettingsChipSelector(
                label = "Section Style",
                options = SettingsSectionStyle.entries.map { it.label },
                selectedIndex = kb.mediaTabSectionStyle.ordinal,
            ) { i -> onUpdate { s -> s.copy(mediaTabSectionStyle = SettingsSectionStyle.entries[i]) } }

            SettingsToggle("Merge System & Modifiers", checked = kb.mediaTabMergeSystemAndMods) {
                v -> onUpdate { s -> s.copy(mediaTabMergeSystemAndMods = v) }
            }
            SettingsToggle("In-Place Reorder", checked = kb.mediaTabInPlaceReorder) {
                v -> onUpdate { s -> s.copy(mediaTabInPlaceReorder = v) }
            }
        }

        SettingsGroupCard(title = "Media Row Groups") {
            SettingsToggle("Transport (⏮ ⏯ ⏹ ⏭)", checked = kb.mediaRowShowTransport) {
                v -> onUpdate { s -> s.copy(mediaRowShowTransport = v) }
            }
            SettingsToggle("Volume (🔇 🔉 🔊)", checked = kb.mediaRowShowVolume) {
                v -> onUpdate { s -> s.copy(mediaRowShowVolume = v) }
            }
            SettingsToggle("Brightness (🔅 🔆)", checked = kb.mediaRowShowBrightness) {
                v -> onUpdate { s -> s.copy(mediaRowShowBrightness = v) }
            }
        }

        SettingsGroupCard(title = "Media Row Repeat") {
            SettingsToggle("Repeat Volume", checked = kb.mediaRowRepeatVolume) {
                v -> onUpdate { s -> s.copy(mediaRowRepeatVolume = v) }
            }
            SettingsToggle("Repeat Brightness", checked = kb.mediaRowRepeatBrightness) {
                v -> onUpdate { s -> s.copy(mediaRowRepeatBrightness = v) }
            }
        }

        SettingsGroupCard(title = "Media Group Order") {
            SettingsDragReorderList(
                items = kb.mediaRowGroupOrder,
                labelProvider = { it.label },
                iconProvider = { it.icon },
                onReorder = { newOrder -> onUpdate { s -> s.copy(mediaRowGroupOrder = newOrder) } },
            )
        }

        SettingsGroupCard(title = "Media Tab Section Order") {
            SettingsDragReorderList(
                items = kb.mediaTabSectionOrder,
                labelProvider = { it.label },
                iconProvider = { it.icon },
                onReorder = { newOrder -> onUpdate { s -> s.copy(mediaTabSectionOrder = newOrder) } },
            )
        }
    }

    RenderKeyboardGroups(isLandscape, groups)
}