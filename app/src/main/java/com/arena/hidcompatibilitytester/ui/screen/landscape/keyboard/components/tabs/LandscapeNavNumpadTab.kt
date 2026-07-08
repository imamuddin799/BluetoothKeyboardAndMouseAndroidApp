package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.tabs

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.*
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.components.*

@Composable
internal fun LandscapeNavNumpadTab(
    st: LandscapeKbState,
    settings: LandscapeKeyboardSettings,
    showNumpad: Boolean,
    onKeyPress: (LandscapeKey) -> Unit,
    onNumpadKey: (Int, String) -> Unit,
    onNumLock: () -> Unit,
    onClearMods: () -> Unit,
    onSettingsChange: (LandscapeKeyboardSettings) -> Unit,
) {
    val style = settings.keysTabSectionStyle
    val merge = settings.shouldMergeSystemMods(settings.navTabMergeSystemAndMods)
    val inPlaceReorder = settings.shouldAllowInPlaceReorder(settings.navTabInPlaceReorder)

    val scrollState = rememberScrollState()
    var viewportTopPx by remember { mutableFloatStateOf(0f) }
    var viewportBottomPx by remember { mutableFloatStateOf(0f) }

    val visibleSections = settings.navTabSectionOrder.filter { section ->
        when (section) {
            LandscapeNavTabSection.NAV_ARROWS -> settings.navTabShowNavigation || settings.navTabShowArrowKeys
            LandscapeNavTabSection.SYSTEM_KEYS -> settings.navTabShowSystemKeys && !merge
            LandscapeNavTabSection.QUICK_MODS -> settings.navTabShowQuickMods && !merge
            LandscapeNavTabSection.MERGED_SYSTEM_MODS -> merge
            else -> false // Type text often omitted or handled separately in landscape
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
                    onReorder = { onSettingsChange(settings.copy(navTabSectionOrder = it)) }
                ) { _, section, _ ->
                    when (section) {
                        LandscapeNavTabSection.NAV_ARROWS -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (settings.navTabShowNavigation) LandscapeKbCard("Navigation", Modifier.weight(1f)) { LandscapeNavigationSectionContent(st, settings, style, onKeyPress) }
                                if (settings.navTabShowArrowKeys) LandscapeKbCard("Arrows", Modifier.weight(1f)) { LandscapeArrowKeysSectionContent(st, settings, style, onKeyPress) }
                            }
                        }
                        LandscapeNavTabSection.SYSTEM_KEYS -> LandscapeKbCard("System Keys") { LandscapeSystemKeysSectionContent(st, settings, style, onKeyPress) }
                        LandscapeNavTabSection.QUICK_MODS -> LandscapeKbCard("Quick Modifiers") { LandscapeQuickModsSectionContent(st, settings, style, onKeyPress, onClearMods) }
                        LandscapeNavTabSection.MERGED_SYSTEM_MODS -> LandscapeKbCard("System & Modifiers") { LandscapeMergedSystemModsSectionContent(st, settings, style, onKeyPress, onClearMods) }
                        else -> {}
                    }
                }
            }
        }

        // Fixed Numpad Section
        AnimatedVisibility(
            visible = showNumpad,
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
        ) {
            Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    color = if (st.numLock) Color(0xFF1565C0) else Color(0xFF4A1800),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                ) {
                    Text(
                        if (st.numLock) "NumLock ON" else "NumLock OFF (Nav Mode)",
                        color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(4.dp)
                    )
                }
                LandscapeNumpadLayout(st.numLock, st.shift, settings, onNumLock, onNumpadKey)
            }
        }
    }
}