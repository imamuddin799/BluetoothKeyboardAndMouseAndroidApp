package com.arena.hidcompatibilitytester.ui.screen.keyboard.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arena.hidcompatibilitytester.ui.screen.keyboard.*
import com.arena.hidcompatibilitytester.ui.screen.keyboard.components.*

@Composable
internal fun KeysTab(
    st: KbState,
    settings: KeyboardSettings,
    onKeyPress: (Key) -> Unit,
    onClearMods: () -> Unit,
    onSendKey: (Int, List<Int>) -> Unit,
    onReleaseKeys: () -> Unit,
    onConsumerKey: (Int) -> Unit,
    onTypeText: (String) -> Unit,
) {
    val style = settings.keysTabSectionStyle
    val isMedia = style == SectionStyle.MEDIA

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080F18)),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (isMedia) 8.dp else 4.dp,
                    vertical = 2.dp
                ),
            verticalArrangement = Arrangement.spacedBy(if (isMedia) 10.dp else 3.dp),
        ) {
            val showNav = settings.keysTabShowNavigation
            val showArrows = settings.keysTabShowArrowKeys

            if (isMedia) {
                // Nav + Arrows side-by-side in cards
                if (showNav && showArrows) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        KbCard("Navigation", modifier = Modifier.weight(1f)) {
                            NavigationSectionContent(st, settings, style, onKeyPress)
                        }
                        KbCard("Arrows", modifier = Modifier.weight(1f)) {
                            ArrowKeysSectionContent(st, settings, style, onKeyPress)
                        }
                    }
                } else {
                    if (showNav) {
                        KbCard("Navigation") {
                            NavigationSectionContent(st, settings, style, onKeyPress)
                        }
                    }
                    if (showArrows) {
                        KbCard("Arrow Keys") {
                            ArrowKeysSectionContent(st, settings, style, onKeyPress)
                        }
                    }
                }

                if (settings.keysTabShowSystemKeys) {
                    KbCard("System Keys") {
                        SystemKeysSectionContent(st, settings, style, onKeyPress)
                    }
                }
                if (settings.keysTabShowQuickMods) {
                    KbCard("Quick Modifiers") {
                        QuickModsSectionContent(st, settings, style, onKeyPress, onClearMods)
                    }
                }
            } else {
                if (showNav && showArrows) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(Modifier.weight(1f)) {
                            NavigationSection(st, settings, style, onKeyPress)
                        }
                        Box(Modifier.weight(1f)) {
                            ArrowKeysSection(st, settings, style, onKeyPress)
                        }
                    }
                } else {
                    if (showNav) {
                        NavigationSection(st, settings, style, onKeyPress)
                    }
                    if (showArrows) {
                        ArrowKeysSection(st, settings, style, onKeyPress)
                    }
                }
                if (settings.keysTabShowSystemKeys) {
                    SystemKeysSection(st, settings, style, onKeyPress)
                }
                if (settings.keysTabShowQuickMods) {
                    QuickModsSection(st, settings, style, onKeyPress, onClearMods)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp, bottom = 6.dp)
        ) {
            SharedCompactKeyboard(
                st             = st,
                settings       = settings,
                showDismissBar = false,
                showMediaRow   = settings.showMediaRowInKeyboard,
                showNavRow     = settings.showNavRowInKeyboard,
                onKeyPress     = onKeyPress,
                onConsumerKey  = onConsumerKey,
            )
        }
    }
}