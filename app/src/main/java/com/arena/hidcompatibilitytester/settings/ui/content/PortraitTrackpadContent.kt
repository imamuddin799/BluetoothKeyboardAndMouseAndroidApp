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
fun PortraitTrackpadContent(
    subSectionId: String,
    settings: AppSettings,
    isLandscape: Boolean,
    onSettingsChange: (AppSettings) -> Unit,
) {
    val latestSettings by rememberUpdatedState(settings)
    val latestOnChange by rememberUpdatedState(onSettingsChange)

    fun update(transform: (PortraitTrackpadSettings) -> PortraitTrackpadSettings) {
        val current = latestSettings.portraitTrackpad
        var newSettings = latestSettings.copy(portraitTrackpad = transform(current))
        newSettings = SettingsSyncManager.applyTrackpadSyncFromPortrait(newSettings)
        latestOnChange(newSettings)
    }

    when (subSectionId) {
        "pointer" -> PortraitTrackpadPointerSection(settings.portraitTrackpad, isLandscape, ::update)
        "gestures" -> PortraitTrackpadGesturesSection(settings.portraitTrackpad, isLandscape, ::update)
        "layout" -> PortraitTrackpadLayoutSection(settings.portraitTrackpad, isLandscape, ::update)
        "keyboard" -> PortraitTrackpadKeyboardSection(settings.portraitTrackpad, isLandscape, ::update)
    }
}

@Composable
private fun PortraitTrackpadPointerSection(
    tp: PortraitTrackpadSettings,
    isLandscape: Boolean,
    onUpdate: ((PortraitTrackpadSettings) -> PortraitTrackpadSettings) -> Unit,
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
private fun PortraitTrackpadGesturesSection(
    tp: PortraitTrackpadSettings,
    isLandscape: Boolean,
    onUpdate: ((PortraitTrackpadSettings) -> PortraitTrackpadSettings) -> Unit,
) {
    val groups = @Composable {
        SettingsGroupCard(title = "Tap") {
            SettingsToggle(
                "Tap to Click",
                subtitle = "Single tap = left click",
                checked = tp.tapToClick,
            ) { v -> onUpdate { s -> s.copy(tapToClick = v) } }

            SettingsToggle(
                "Two-Finger Right Click",
                subtitle = "Two-finger tap = right click",
                checked = tp.twoFingerRightClick,
            ) { v -> onUpdate { s -> s.copy(twoFingerRightClick = v) } }
        }

        SettingsGroupCard(title = "Scroll") {
            SettingsToggle(
                "Invert Scroll Direction",
                subtitle = "Natural (macOS-style) scrolling",
                checked = tp.invertScroll,
            ) { v -> onUpdate { s -> s.copy(invertScroll = v) } }
        }

        SettingsGroupCard(title = "Drag") {
            SettingsToggle(
                "Drag Lock",
                subtitle = "Lift finger without releasing drag",
                checked = tp.dragLockMode,
            ) { v -> onUpdate { s -> s.copy(dragLockMode = v) } }
        }
    }

    RenderTrackpadGroups(isLandscape, groups)
}

@Composable
private fun PortraitTrackpadLayoutSection(
    tp: PortraitTrackpadSettings,
    isLandscape: Boolean,
    onUpdate: ((PortraitTrackpadSettings) -> PortraitTrackpadSettings) -> Unit,
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
    }

    RenderTrackpadGroups(isLandscape, groups)
}

@Composable
private fun PortraitTrackpadKeyboardSection(
    tp: PortraitTrackpadSettings,
    isLandscape: Boolean,
    onUpdate: ((PortraitTrackpadSettings) -> PortraitTrackpadSettings) -> Unit,
) {
    val groups = @Composable {
        SettingsGroupCard(title = "Keyboard Access") {
            SettingsToggle(
                "Show System Keyboard Button",
                subtitle = "Toggle Android system keyboard",
                checked = tp.showSystemKeyboard,
            ) { v -> onUpdate { s -> s.copy(showSystemKeyboard = v) } }

            SettingsToggle(
                "Show In-App Keyboard Button",
                subtitle = "Toggle built-in HID keyboard",
                checked = tp.showInAppKeyboard,
            ) { v -> onUpdate { s -> s.copy(showInAppKeyboard = v) } }
        }
    }

    RenderTrackpadGroups(isLandscape, groups)
}

@Composable
internal fun RenderTrackpadGroups(
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