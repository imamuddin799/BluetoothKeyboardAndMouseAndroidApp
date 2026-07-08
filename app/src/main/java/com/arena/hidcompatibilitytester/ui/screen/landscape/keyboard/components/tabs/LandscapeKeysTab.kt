package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.tabs

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.*
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.components.*

@Composable
fun LandscapeKeysTab(
    st: LandscapeKbState,
    settings: LandscapeKeyboardSettings,
    showKeyboard: Boolean,
    showOptionalRows: Boolean,
    onKeyPress: (LandscapeKey) -> Unit,
    onClearMods: () -> Unit,
    onConsumerKey: (Int) -> Unit,
    onSettingsChange: (LandscapeKeyboardSettings) -> Unit,
) {
    val style = settings.keysTabSectionStyle
    val inPlaceReorder = settings.shouldAllowInPlaceReorder(settings.keysTabInPlaceReorder)
    val merge = settings.shouldMergeSystemMods(settings.keysTabMergeSystemAndMods)
    
    val scrollState = rememberScrollState()
    var viewportTopPx by remember { mutableStateOf(0f) }
    var viewportBottomPx by remember { mutableStateOf(0f) }

    val visibleSections = settings.keysTabSectionOrder.filter { section ->
        when (section) {
            LandscapeKeysTabSection.NAV_ARROWS -> settings.keysTabShowNavigation || settings.keysTabShowArrowKeys
            LandscapeKeysTabSection.SYSTEM_KEYS -> settings.keysTabShowSystemKeys && !merge
            LandscapeKeysTabSection.QUICK_MODS -> settings.keysTabShowQuickMods && !merge
            LandscapeKeysTabSection.MERGED_SYSTEM_MODS -> merge
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF080F18))) {
        Box(Modifier.weight(1f).onGloballyPositioned {
            viewportTopPx = it.positionInRoot().y
            viewportBottomPx = viewportTopPx + it.size.height
        }) {
            Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(8.dp)) {
                LandscapeReorderableSectionColumn(
                    items = visibleSections,
                    enabled = inPlaceReorder,
                    scrollState = scrollState,
                    viewportTopPx = viewportTopPx,
                    viewportBottomPx = viewportBottomPx,
                    onReorder = { onSettingsChange(settings.copy(keysTabSectionOrder = it)) }
                ) { _, section, _ ->
                    when (section) {
                        LandscapeKeysTabSection.NAV_ARROWS -> LandscapeKeysTabNavArrows(st, settings, style, onKeyPress)
                        LandscapeKeysTabSection.SYSTEM_KEYS -> LandscapeKbCard("System Keys") { LandscapeSystemKeysSectionContent(st, settings, style, onKeyPress) }
                        LandscapeKeysTabSection.QUICK_MODS -> LandscapeKbCard("Quick Modifiers") { LandscapeQuickModsSectionContent(st, settings, style, onKeyPress, onClearMods) }
                        LandscapeKeysTabSection.MERGED_SYSTEM_MODS -> LandscapeKbCard("System & Modifiers") { LandscapeMergedSystemModsSectionContent(st, settings, style, onKeyPress, onClearMods) }
                    }
                }
            }
        }

        AnimatedVisibility(visible = showKeyboard) {
            LandscapeSharedCompactKeyboard(
                st = st, settings = settings,
                showMediaRow = settings.showMediaRowInKeysTab(),
                showNavRow = settings.showNavRowInKeysTab(),
                showOptionalRows = showOptionalRows,
                onKeyPress = onKeyPress, onConsumerKey = onConsumerKey
            )
        }
    }
}

@Composable
private fun LandscapeKeysTabNavArrows(st: LandscapeKbState, settings: LandscapeKeyboardSettings, style: LandscapeSectionStyle, onKeyPress: (LandscapeKey) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (settings.keysTabShowNavigation) LandscapeKbCard("Navigation", Modifier.weight(1f)) { LandscapeNavigationSectionContent(st, settings, style, onKeyPress) }
        if (settings.keysTabShowArrowKeys) LandscapeKbCard("Arrows", Modifier.weight(1f)) { LandscapeArrowKeysSectionContent(st, settings, style, onKeyPress) }
    }
}