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
fun LandscapeTrackpadContent(
    subSectionId: String,
    settings: AppSettings,
    isLandscape: Boolean,
    onSettingsChange: (AppSettings) -> Unit,
) {
    val latestSettings by rememberUpdatedState(settings)
    val latestOnChange by rememberUpdatedState(onSettingsChange)

    fun update(transform: (LandscapeTrackpadSettings) -> LandscapeTrackpadSettings) {
        val current = latestSettings.landscapeTrackpad
        var newSettings = latestSettings.copy(landscapeTrackpad = transform(current))
        newSettings = SettingsSyncManager.applyTrackpadSyncFromLandscape(newSettings)
        latestOnChange(newSettings)
    }

    when (subSectionId) {
        "pointer" -> LandscapeTrackpadPointerSection(settings.landscapeTrackpad, isLandscape, ::update)
        "gestures" -> LandscapeTrackpadGesturesSection(settings.landscapeTrackpad, isLandscape, ::update)
        "layout" -> LandscapeTrackpadLayoutSection(settings.landscapeTrackpad, isLandscape, ::update)
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
        SettingsGroupCard(title = "Speed") {
            SettingsSlider(
                label = "Pointer Speed",
                value = tp.pointerSpeed,
                range = 0.1f..3.0f,
                display = { "%.1fx".format(it) },
                minLabel = "Slow",
                maxLabel = "Fast",
            ) { v -> onUpdate { s -> s.copy(pointerSpeed = v) } }

            SettingsSlider(
                label = "Scroll Speed",
                value = tp.scrollSpeed,
                range = 0.1f..2.0f,
                display = { "%.1fx".format(it) },
                minLabel = "Slow",
                maxLabel = "Fast",
            ) { v -> onUpdate { s -> s.copy(scrollSpeed = v) } }
        }

        SettingsGroupCard(title = "Acceleration") {
            SettingsToggle(
                "Mouse Acceleration",
                subtitle = "Faster finger = larger cursor movement",
                checked = tp.accelerationEnabled,
            ) { v -> onUpdate { s -> s.copy(accelerationEnabled = v) } }
        }

        SettingsGroupCard(title = "Click Pressure") {
            SettingsChipSelector(
                label = "Click Sensitivity",
                options = SettingsClickPressure.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                selectedIndex = tp.clickPressure.ordinal,
            ) { i -> onUpdate { s -> s.copy(clickPressure = SettingsClickPressure.entries[i]) } }
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
            SettingsToggle("Tap to Click", checked = tp.tapToClick) {
                v -> onUpdate { s -> s.copy(tapToClick = v) }
            }
            SettingsToggle("Two-Finger Right Click", checked = tp.twoFingerRightClick) {
                v -> onUpdate { s -> s.copy(twoFingerRightClick = v) }
            }
        }

        SettingsGroupCard(title = "Scroll") {
            SettingsToggle("Invert Scroll Direction", checked = tp.invertScroll) {
                v -> onUpdate { s -> s.copy(invertScroll = v) }
            }
        }

        SettingsGroupCard(title = "Drag") {
            SettingsToggle("Drag Lock", checked = tp.dragLockMode) {
                v -> onUpdate { s -> s.copy(dragLockMode = v) }
            }
        }
    }

    RenderTrackpadGroups(isLandscape, groups)
}

@Composable
private fun LandscapeTrackpadLayoutSection(
    tp: LandscapeTrackpadSettings,
    isLandscape: Boolean,
    onUpdate: ((LandscapeTrackpadSettings) -> LandscapeTrackpadSettings) -> Unit,
) {
    val groups = @Composable {
        SettingsGroupCard(title = "Scroll Strip") {
            SettingsToggle("Show Scroll Strip", checked = tp.showScrollStrip) {
                v -> onUpdate { s -> s.copy(showScrollStrip = v) }
            }
            SettingsChipSelector(
                label = "Scroll Strip Position",
                options = SettingsSidePosition.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                selectedIndex = tp.scrollPosition.ordinal,
                enabled = tp.showScrollStrip,
            ) { i -> onUpdate { s -> s.copy(scrollPosition = SettingsSidePosition.entries[i]) } }
        }

        SettingsGroupCard(title = "Arrow Keys") {
            SettingsToggle("Show Arrow Keys", checked = tp.showArrowKeys) {
                v -> onUpdate { s -> s.copy(showArrowKeys = v) }
            }
            SettingsChipSelector(
                label = "Arrow Keys Position",
                options = SettingsSidePosition.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                selectedIndex = tp.arrowPosition.ordinal,
                enabled = tp.showArrowKeys,
            ) { i -> onUpdate { s -> s.copy(arrowPosition = SettingsSidePosition.entries[i]) } }
        }

        SettingsGroupCard(title = "Landscape Keyboard Layout") {
            SettingsChipSelector(
                label = "Default Layout",
                subtitle = "When keyboard opens from trackpad",
                options = SettingsLayoutMode.entries.map { it.label },
                selectedIndex = tp.trackpadKbDefaultLayoutMode.ordinal,
            ) { i -> onUpdate { s -> s.copy(trackpadKbDefaultLayoutMode = SettingsLayoutMode.entries[i]) } }

            SettingsChipSelector(
                label = "Right Column Content",
                options = SettingsRightColumnMode.entries.map { it.label },
                selectedIndex = tp.trackpadKbDefaultRightColumn.ordinal,
            ) { i -> onUpdate { s -> s.copy(trackpadKbDefaultRightColumn = SettingsRightColumnMode.entries[i]) } }

            SettingsToggle(
                "Show Combo Preview",
                subtitle = "Show key combo in trackpad keyboard",
                checked = tp.trackpadKbShowComboPreview,
            ) { v -> onUpdate { s -> s.copy(trackpadKbShowComboPreview = v) } }
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
                "Show System Keyboard Button",
                checked = tp.showSystemKeyboard,
            ) { v -> onUpdate { s -> s.copy(showSystemKeyboard = v) } }

            SettingsToggle(
                "Show In-App Keyboard Button",
                checked = tp.showInAppKeyboard,
            ) { v -> onUpdate { s -> s.copy(showInAppKeyboard = v) } }
        }
    }

    RenderTrackpadGroups(isLandscape, groups)
}