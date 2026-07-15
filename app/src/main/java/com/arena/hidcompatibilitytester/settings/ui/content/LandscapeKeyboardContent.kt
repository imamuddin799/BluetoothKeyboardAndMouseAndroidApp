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
            SettingsToggle("Start with NumLock On", checked = kb.numpadStartsLocked) {
                newValue -> onUpdate { it.copy(numpadStartsLocked = newValue) }
            }
            SettingsToggle("Show Alternate Hints", checked = kb.numpadShowHints) {
                newValue -> onUpdate { it.copy(numpadShowHints = newValue) }
            }
        }

        // 2. Nav Tab Options
        SettingsGroupCard(title = "Nav Tab Options") {
            // Make sure this uses navTabSectionStyle NOT keysTabSectionStyle
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

            SettingsToggle(
                "In-Place Reorder",
                subtitle = if (kb.inPlaceReorderGlobal) "Controlled by global" else "Nav tab only",
                checked = kb.navTabInPlaceReorder,
                enabled = !kb.inPlaceReorderGlobal,
            ) { newValue -> onUpdate { it.copy(navTabInPlaceReorder = newValue) } }

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
            SettingsToggle("Repeat Volume", checked = kb.mediaRowRepeatVolume) {
                newValue -> onUpdate { it.copy(mediaRowRepeatVolume = newValue) }
            }
            SettingsToggle("Repeat Brightness", checked = kb.mediaRowRepeatBrightness) {
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