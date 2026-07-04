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
internal fun MediaTab(
    st: KbState,
    settings: KeyboardSettings,
    onKeyPress: (Key) -> Unit,
    onConsumerKey: (Int) -> Unit,
    onClearMods: () -> Unit,
    onUpdateLastKey: (String) -> Unit,
) {
    val mediaH = settings.mediaKeySize.heightDp.dp
    val style = settings.mediaTabSectionStyle
    val isCompact = style == SectionStyle.COMPACT

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080F18))
            .verticalScroll(rememberScrollState())
            .padding(if (isCompact) 4.dp else 8.dp),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 3.dp else 10.dp),
    ) {
        // ── Transport — always media card ──
        KbCard("Transport") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MEDIA_TRANSPORT.forEach { mk ->
                    MediaKeyBtn(mk.icon, mk.label, Modifier.weight(1f), mediaH, settings) {
                        onConsumerKey(mk.code); onUpdateLastKey(mk.label)
                    }
                }
            }
        }

        // ── Volume & Brightness — always media card ──
        KbCard("Volume & Brightness") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                (MEDIA_VOLUME + MEDIA_BRIGHT).forEach { mk ->
                    MediaKeyBtn(mk.icon, mk.label, Modifier.weight(1f), mediaH - 8.dp, settings) {
                        onConsumerKey(mk.code); onUpdateLastKey(mk.label)
                    }
                }
            }
        }

        // ── Shared sections ──
        if (isCompact) {
            val showNav = settings.mediaTabShowNavigation
            val showArrows = settings.mediaTabShowArrowKeys

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

            if (settings.mediaTabShowSystemKeys) {
                SystemKeysSection(st, settings, style, onKeyPress)
            }
            if (settings.mediaTabShowQuickMods) {
                QuickModsSection(st, settings, style, onKeyPress, onClearMods)
            }
        } else {
            if (settings.mediaTabShowNavigation) {
                KbCard("Navigation") {
                    NavigationSectionContent(st, settings, style, onKeyPress)
                }
            }
            if (settings.mediaTabShowArrowKeys) {
                KbCard("Arrow Keys") {
                    ArrowKeysSectionContent(st, settings, style, onKeyPress)
                }
            }
            if (settings.mediaTabShowSystemKeys) {
                KbCard("System Keys") {
                    SystemKeysSectionContent(st, settings, style, onKeyPress)
                }
            }
            if (settings.mediaTabShowQuickMods) {
                KbCard("Quick Modifiers") {
                    QuickModsSectionContent(st, settings, style, onKeyPress, onClearMods)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}