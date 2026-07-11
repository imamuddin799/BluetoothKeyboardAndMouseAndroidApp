package com.arena.hidcompatibilitytester.settings.ui.content

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arena.hidcompatibilitytester.settings.*
import com.arena.hidcompatibilitytester.settings.ui.*

@Composable
fun GeneralAppearanceContent(
    subSectionId: String,
    settings: AppSettings,
    isLandscape: Boolean,
    onSettingsChange: (AppSettings) -> Unit,
) {
    val general = settings.general

    fun update(transform: (GeneralSettings) -> GeneralSettings) {
        onSettingsChange(settings.copy(general = transform(general)))
    }

    when (subSectionId) {
        "theme" -> GeneralThemeSection(general, isLandscape, ::update)
        "contrast" -> GeneralContrastSection(general, isLandscape, ::update)
    }
}

@Composable
fun GeneralBehaviorContent(
    subSectionId: String,
    settings: AppSettings,
    isLandscape: Boolean,
    onSettingsChange: (AppSettings) -> Unit,
) {
    val general = settings.general

    fun update(transform: (GeneralSettings) -> GeneralSettings) {
        onSettingsChange(settings.copy(general = transform(general)))
    }

    when (subSectionId) {
        "haptics" -> GeneralHapticsSection(general, isLandscape, ::update)
        "analytics" -> GeneralAnalyticsSection(general, isLandscape, ::update)
    }
}

@Composable
fun GeneralLanguageContent(
    subSectionId: String,
    settings: AppSettings,
    isLandscape: Boolean,
    onSettingsChange: (AppSettings) -> Unit,
) {
    val general = settings.general

    fun update(transform: (GeneralSettings) -> GeneralSettings) {
        onSettingsChange(settings.copy(general = transform(general)))
    }

    when (subSectionId) {
        "language" -> GeneralLanguageSection(general, isLandscape, ::update)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// THEME
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun GeneralThemeSection(
    general: GeneralSettings,
    isLandscape: Boolean,
    onUpdate: ((GeneralSettings) -> GeneralSettings) -> Unit,
) {
    val groups = @Composable {
        SettingsGroupCard(title = "Theme Mode") {
            SettingsChipSelector(
                label = "App Theme",
                options = SettingsThemeMode.entries.map {
                    it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                },
                selectedIndex = general.themeMode.ordinal,
            ) { i -> onUpdate { s -> s.copy(themeMode = SettingsThemeMode.entries[i]) } }
        }
    }

    RenderGeneralGroups(isLandscape, groups)
}

@Composable
private fun GeneralContrastSection(
    general: GeneralSettings,
    isLandscape: Boolean,
    onUpdate: ((GeneralSettings) -> GeneralSettings) -> Unit,
) {
    val groups = @Composable {
        SettingsGroupCard(title = "Contrast") {
            SettingsHint(
                text = "High contrast mode is configured per-device (Keyboard → Appearance).",
                variant = SettingsHintVariant.INFO,
            )
        }
    }

    RenderGeneralGroups(isLandscape, groups)
}

// ═════════════════════════════════════════════════════════════════════════════
// HAPTICS
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun GeneralHapticsSection(
    general: GeneralSettings,
    isLandscape: Boolean,
    onUpdate: ((GeneralSettings) -> GeneralSettings) -> Unit,
) {
    val groups = @Composable {
        SettingsGroupCard(title = "Global Haptic Intensity") {
            SettingsChipSelector(
                label = "Intensity",
                subtitle = "This sets the default. Per-keyboard settings override.",
                options = SettingsHapticIntensity.entries.map { it.label },
                selectedIndex = general.globalHapticIntensity.ordinal,
            ) { i -> onUpdate { s -> s.copy(globalHapticIntensity = SettingsHapticIntensity.entries[i]) } }
        }
    }

    RenderGeneralGroups(isLandscape, groups)
}

@Composable
private fun GeneralAnalyticsSection(
    general: GeneralSettings,
    isLandscape: Boolean,
    onUpdate: ((GeneralSettings) -> GeneralSettings) -> Unit,
) {
    val groups = @Composable {
        SettingsGroupCard(title = "Analytics") {
            SettingsToggle(
                "Enable Analytics",
                subtitle = "Anonymous usage data to improve the app",
                checked = general.analyticsEnabled,
            ) { v -> onUpdate { s -> s.copy(analyticsEnabled = v) } }
        }
    }

    RenderGeneralGroups(isLandscape, groups)
}

// ═════════════════════════════════════════════════════════════════════════════
// LANGUAGE
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun GeneralLanguageSection(
    general: GeneralSettings,
    isLandscape: Boolean,
    onUpdate: ((GeneralSettings) -> GeneralSettings) -> Unit,
) {
    val languages = listOf(
        "en" to "English",
        "hi" to "Hindi",
        "es" to "Spanish",
        "fr" to "French",
        "de" to "German",
        "ja" to "Japanese",
        "zh" to "Chinese",
        "ko" to "Korean",
        "ar" to "Arabic",
        "pt" to "Portuguese",
    )

    val selectedIdx = languages.indexOfFirst { it.first == general.language }.coerceAtLeast(0)

    val groups = @Composable {
        SettingsGroupCard(title = "App Language") {
            SettingsDropdown(
                label = "Language",
                subtitle = "Restart may be required",
                options = languages.map { "${it.second} (${it.first})" },
                selectedIndex = selectedIdx,
            ) { i -> onUpdate { s -> s.copy(language = languages[i].first) } }
        }
    }

    RenderGeneralGroups(isLandscape, groups)
}

@Composable
internal fun RenderGeneralGroups(
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