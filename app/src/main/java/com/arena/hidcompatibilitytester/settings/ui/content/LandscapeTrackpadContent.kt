package com.arena.hidcompatibilitytester.settings.ui.content

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arena.hidcompatibilitytester.settings.*
import com.arena.hidcompatibilitytester.settings.ui.*

@Composable
fun LandscapeTrackpadContent(
    subSectionId: String,
    settings: AppSettings,
    isLandscape: Boolean,
    onSettingsChange: (AppSettings) -> Unit,
) {
    val latestSettings by androidx.compose.runtime.rememberUpdatedState(settings)
    val latestOnChange by androidx.compose.runtime.rememberUpdatedState(onSettingsChange)

    fun update(transform: (LandscapeTrackpadSettings) -> LandscapeTrackpadSettings) {
        val current = latestSettings.landscapeTrackpad
        var newSettings = latestSettings.copy(landscapeTrackpad = transform(current))
        newSettings = SettingsSyncManager.applyTrackpadSyncFromLandscape(newSettings)
        latestOnChange(newSettings)
    }

    when (subSectionId) {
        "pointer" -> LandscapeTrackpadPointerSection(settings.landscapeTrackpad, isLandscape, ::update)
        "gestures" -> LandscapeTrackpadGesturesSection(settings.landscapeTrackpad, isLandscape, ::update)
        "keyboard" -> LandscapeTrackpadKeyboardSection(settings.landscapeTrackpad, isLandscape, ::update)
    }
}

@Composable
private fun LandscapeTrackpadPointerSection(
    tp: LandscapeTrackpadSettings,
    isLandscape: Boolean,
    onUpdate: ((LandscapeTrackpadSettings) -> LandscapeTrackpadSettings) -> Unit,
) {
    val groups = @Composable {
        // 1. Speed
        SettingsGroupCard(title = "Speed") {
            SettingsSlider(
                label = "Pointer Speed",
                value = tp.pointerSpeed,
                range = 0.1f..3.0f,
                display = { "%.1fx".format(it) },
                minLabel = "Slow",
                maxLabel = "Fast",
            ) { v -> onUpdate { it.copy(pointerSpeed = v) } }

            SettingsSlider(
                label = "Scroll Speed",
                value = tp.scrollSpeed,
                range = 0.1f..2.0f,
                display = { "%.1fx".format(it) },
                minLabel = "Slow",
                maxLabel = "Fast",
            ) { v -> onUpdate { it.copy(scrollSpeed = v) } }
        }

        // 2. Acceleration
        SettingsGroupCard(title = "Acceleration") {
            SettingsToggle(
                "Mouse Acceleration",
                subtitle = "Slow = precise · Fast = covers distance",
                checked = tp.accelerationEnabled,
            ) { newValue -> onUpdate { it.copy(accelerationEnabled = newValue) } }

            if (!tp.accelerationEnabled) {
                SettingsHint(
                    text = "Acceleration disabled. Pointer moves at constant speed regardless of finger velocity.",
                    icon = "ℹ",
                    variant = SettingsHintVariant.INFO,
                )
            }

            SettingsChipSelector(
                label = "Click Sensitivity",
                subtitle = "How much pressure registers a tap",
                options = SettingsClickPressure.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                selectedIndex = tp.clickPressure.ordinal,
            ) { i -> onUpdate { it.copy(clickPressure = SettingsClickPressure.entries[i]) } }
        }

        // 3. Arrow Keys
        SettingsGroupCard(title = "Arrow Keys") {
            SettingsToggle(
                "Show Arrow Keys",
                subtitle = "Directional buttons for precise cursor movement",
                checked = tp.showArrowKeys,
            ) { newValue -> onUpdate { it.copy(showArrowKeys = newValue) } }

            if (tp.showArrowKeys) {
                SettingsChipSelector(
                    label = "Arrow Keys Position",
                    subtitle = "Which side of the trackpad",
                    options = SettingsSidePosition.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                    selectedIndex = tp.arrowPosition.ordinal,
                ) { i -> onUpdate { it.copy(arrowPosition = SettingsSidePosition.entries[i]) } }
            } else {
                SettingsHint(
                    text = "Arrow keys are hidden. Enable to show directional buttons on the trackpad.",
                    icon = "↕",
                    variant = SettingsHintVariant.INFO,
                )
            }
        }

        // 4. Scroll Strip
        SettingsGroupCard(title = "Scroll Strip") {
            SettingsToggle(
                "Show Scroll Strip",
                subtitle = "Vertical scroll strip on the trackpad edge",
                checked = tp.showScrollStrip,
            ) { newValue -> onUpdate { it.copy(showScrollStrip = newValue) } }

            if (tp.showScrollStrip) {
                SettingsChipSelector(
                    label = "Scroll Strip Position",
                    subtitle = "Which side of the trackpad",
                    options = SettingsSidePosition.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                    selectedIndex = tp.scrollPosition.ordinal,
                ) { i -> onUpdate { it.copy(scrollPosition = SettingsSidePosition.entries[i]) } }
            } else {
                SettingsHint(
                    text = "Scroll strip is hidden. You can still scroll with two-finger gestures.",
                    icon = "↕",
                    variant = SettingsHintVariant.INFO,
                )
            }
        }

        // 5. Landscape Keyboard Layout
        SettingsGroupCard(title = "Landscape Keyboard Layout") {
            SettingsChipSelector(
                label = "Default Layout",
                subtitle = "When keyboard opens from trackpad",
                options = SettingsLayoutMode.entries.map { it.label },
                selectedIndex = tp.trackpadKbDefaultLayoutMode.ordinal,
            ) { i -> onUpdate { it.copy(trackpadKbDefaultLayoutMode = SettingsLayoutMode.entries[i]) } }

            SettingsChipSelector(
                label = "Right Column Content",
                subtitle = "Applies only when Two Column layout is active",
                options = SettingsRightColumnMode.entries.map { it.label },
                selectedIndex = tp.trackpadKbDefaultRightColumn.ordinal,
            ) { i -> onUpdate { it.copy(trackpadKbDefaultRightColumn = SettingsRightColumnMode.entries[i]) } }

            SettingsToggle(
                "Show Combo Preview",
                subtitle = "Show mod+key combo next to LED indicators",
                checked = tp.trackpadKbShowComboPreview,
            ) { newValue -> onUpdate { it.copy(trackpadKbShowComboPreview = newValue) } }

            SettingsHint(
                text = "These control the initial state when the trackpad's in-app keyboard opens. Session-only changes from the status bar don't persist.",
                icon = "ℹ",
                variant = SettingsHintVariant.INFO,
            )
        }
    }

    RenderTrackpadGroups(isLandscape, groups)
}

@Composable
private fun LandscapeTrackpadGesturesSection(
    tp: LandscapeTrackpadSettings,
    isLandscape: Boolean,
    onUpdate: ((LandscapeTrackpadSettings) -> LandscapeTrackpadSettings) -> Unit,
) {
    val groups = @Composable {
        SettingsGroupCard(title = "Tap") {
            SettingsToggle(
                "Tap to Click",
                subtitle = "Short tap = left click",
                checked = tp.tapToClick,
            ) { newValue -> onUpdate { it.copy(tapToClick = newValue) } }

            if (!tp.tapToClick) {
                SettingsHint(
                    text = "Tap to click disabled. Use the click buttons at the bottom instead.",
                    icon = "👆",
                    variant = SettingsHintVariant.INFO,
                )
            }

            SettingsToggle(
                "Two-Finger Right Click",
                subtitle = "Two-finger tap = right-click menu",
                checked = tp.twoFingerRightClick,
            ) { newValue -> onUpdate { it.copy(twoFingerRightClick = newValue) } }
        }

        SettingsGroupCard(title = "Scroll") {
            SettingsToggle(
                "Invert Scroll Direction",
                subtitle = "Natural (macOS-style) scrolling",
                checked = tp.invertScroll,
            ) { newValue -> onUpdate { it.copy(invertScroll = newValue) } }

            SettingsHint(
                text = if (tp.invertScroll)
                    "Natural scrolling: content follows finger direction (like a touchscreen)."
                else
                    "Traditional scrolling: scroll bar follows finger direction.",
                icon = if (tp.invertScroll) "🔄" else "↕",
                variant = SettingsHintVariant.INFO,
            )
        }

        SettingsGroupCard(title = "Double-Tap Drag") {
            SettingsToggle(
                "Drag Lock Mode",
                subtitle = if (tp.dragLockMode)
                    "ON — double-tap to start drag, tap again to release"
                else
                    "OFF — double-tap then keep finger down, lift to release",
                checked = tp.dragLockMode,
            ) { newValue -> onUpdate { it.copy(dragLockMode = newValue) } }

            if (tp.dragLockMode) {
                SettingsHint(
                    text = "1. Double-tap anywhere\n2. Lift finger — drag stays active\n3. Move cursor to drag\n4. Tap once to release drag\n(Quick double-tap with no move = normal double-click)",
                    icon = "⬚",
                    variant = SettingsHintVariant.INFO,
                )
            } else {
                SettingsHint(
                    text = "1. Double-tap anywhere\n2. On 2nd tap — keep finger held down\n3. Move finger to drag\n4. Lift finger to release drag",
                    icon = "✋",
                    variant = SettingsHintVariant.SUCCESS,
                )
            }
        }
    }

    RenderTrackpadGroups(isLandscape, groups)
}

@Composable
private fun LandscapeTrackpadKeyboardSection(
    tp: LandscapeTrackpadSettings,
    isLandscape: Boolean,
    onUpdate: ((LandscapeTrackpadSettings) -> LandscapeTrackpadSettings) -> Unit,
) {
    val groups = @Composable {
        SettingsGroupCard(title = "Keyboard Access") {
            SettingsToggle(
                "Show System Keyboard Button  📱",
                subtitle = "Toggle Android system keyboard — typed text is sent to host",
                checked = tp.showSystemKeyboard,
            ) { newValue -> onUpdate { it.copy(showSystemKeyboard = newValue) } }

            if (!tp.showSystemKeyboard) {
                SettingsHint(
                    text = "System keyboard button hidden from trackpad status bar.",
                    icon = "📱",
                    variant = SettingsHintVariant.INFO,
                )
            }

            SettingsToggle(
                "Show In-App Keyboard Button  ⌨",
                subtitle = "Toggle built-in HID keyboard overlay",
                checked = tp.showInAppKeyboard,
            ) { newValue -> onUpdate { it.copy(showInAppKeyboard = newValue) } }

            if (!tp.showInAppKeyboard) {
                SettingsHint(
                    text = "In-app keyboard button hidden from trackpad status bar.",
                    icon = "⌨",
                    variant = SettingsHintVariant.INFO,
                )
            }

            if (!tp.showSystemKeyboard && !tp.showInAppKeyboard) {
                SettingsHint(
                    text = "Both keyboard buttons are hidden. You can still access the keyboard from the Keyboard tab.",
                    icon = "⚠",
                    variant = SettingsHintVariant.WARNING,
                )
            }
        }
    }

    RenderTrackpadGroups(isLandscape, groups)
}