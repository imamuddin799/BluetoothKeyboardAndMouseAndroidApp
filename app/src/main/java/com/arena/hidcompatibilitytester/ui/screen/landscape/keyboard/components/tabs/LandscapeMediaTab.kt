package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.tabs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.*
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.components.*

@Composable
internal fun LandscapeMediaTab(
    st: LandscapeKbState,
    settings: LandscapeKeyboardSettings,
    onKeyPress: (LandscapeKey) -> Unit,
    onConsumerKey: (Int) -> Unit,
    onClearMods: () -> Unit,
    onSettingsChange: (LandscapeKeyboardSettings) -> Unit,
) {
    val mediaH = settings.mediaKeySize.heightDp.dp
    val style = settings.mediaTabSectionStyle
    val isCompact = style == LandscapeSectionStyle.COMPACT
    val merge = settings.shouldMergeSystemMods(settings.mediaTabMergeSystemAndMods)
    val inPlaceReorder = settings.shouldAllowInPlaceReorder(settings.mediaTabInPlaceReorder)

    val scrollState = rememberScrollState()
    var viewportTopPx by remember { mutableFloatStateOf(0f) }
    var viewportBottomPx by remember { mutableFloatStateOf(0f) }

    val visibleSections = settings.mediaTabSectionOrder.filter { section ->
        when (section) {
            LandscapeMediaTabSection.TRANSPORT -> true
            LandscapeMediaTabSection.VOLUME_BRIGHTNESS -> true
            LandscapeMediaTabSection.NAVIGATION -> settings.mediaTabShowNavigation
            LandscapeMediaTabSection.ARROW_KEYS -> settings.mediaTabShowArrowKeys
            LandscapeMediaTabSection.SYSTEM_KEYS -> settings.mediaTabShowSystemKeys && !merge
            LandscapeMediaTabSection.QUICK_MODS -> settings.mediaTabShowQuickMods && !merge
            LandscapeMediaTabSection.MERGED_SYSTEM_MODS -> merge
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF080F18)).onGloballyPositioned {
        viewportTopPx = it.positionInRoot().y
        viewportBottomPx = viewportTopPx + it.size.height
    }) {
        Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(if (isCompact) 4.dp else 8.dp)) {
            LandscapeReorderableSectionColumn(
                items = visibleSections,
                enabled = inPlaceReorder,
                scrollState = scrollState,
                viewportTopPx = viewportTopPx,
                viewportBottomPx = viewportBottomPx,
                onReorder = { onSettingsChange(settings.copy(mediaTabSectionOrder = it)) }
            ) { _, section, _ ->
                when (section) {
                    LandscapeMediaTabSection.TRANSPORT ->
                        if (isCompact) {
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    "Transport",
                                    color = Color(0xFF607D8B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    LANDSCAPE_MEDIA_TRANSPORT.forEach { mk ->
                                        LandscapeMediaKeyBtn(mk.icon, mk.label, Modifier.weight(1f), mediaH, settings, LandscapeMediaRowGroup.TRANSPORT) { onConsumerKey(mk.code) }
                                    }
                                }
                            }
                        } else {
                            LandscapeKbCard("Transport") {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    LANDSCAPE_MEDIA_TRANSPORT.forEach { mk ->
                                        LandscapeMediaKeyBtn(mk.icon, mk.label, Modifier.weight(1f), mediaH, settings, LandscapeMediaRowGroup.TRANSPORT) { onConsumerKey(mk.code) }
                                    }
                                }
                            }
                        }

                    LandscapeMediaTabSection.VOLUME_BRIGHTNESS ->
                        if (isCompact) {
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    "Volume & Brightness",
                                    color = Color(0xFF607D8B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    LANDSCAPE_MEDIA_VOLUME.forEach { mk ->
                                        LandscapeMediaKeyBtn(mk.icon, mk.label, Modifier.weight(1f), mediaH, settings, LandscapeMediaRowGroup.VOLUME) { onConsumerKey(mk.code) }
                                    }
                                    LANDSCAPE_MEDIA_BRIGHT.forEach { mk ->
                                        LandscapeMediaKeyBtn(mk.icon, mk.label, Modifier.weight(1f), mediaH, settings, LandscapeMediaRowGroup.BRIGHTNESS) { onConsumerKey(mk.code) }
                                    }
                                }
                            }
                        } else {
                            LandscapeKbCard("Volume & Brightness") {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    LANDSCAPE_MEDIA_VOLUME.forEach { mk ->
                                        LandscapeMediaKeyBtn(mk.icon, mk.label, Modifier.weight(1f), mediaH, settings, LandscapeMediaRowGroup.VOLUME) { onConsumerKey(mk.code) }
                                    }
                                    LANDSCAPE_MEDIA_BRIGHT.forEach { mk ->
                                        LandscapeMediaKeyBtn(mk.icon, mk.label, Modifier.weight(1f), mediaH, settings, LandscapeMediaRowGroup.BRIGHTNESS) { onConsumerKey(mk.code) }
                                    }
                                }
                            }
                        }

                    LandscapeMediaTabSection.NAVIGATION ->
                        if (isCompact) {
                            LandscapeNavigationSection(st, settings, style, onKeyPress)
                        } else {
                            LandscapeKbCard("Navigation") { LandscapeNavigationSectionContent(st, settings, style, onKeyPress) }
                        }

                    LandscapeMediaTabSection.ARROW_KEYS ->
                        if (isCompact) {
                            LandscapeArrowKeysSection(st, settings, style, onKeyPress)
                        } else {
                            LandscapeKbCard("Arrows") { LandscapeArrowKeysSectionContent(st, settings, style, onKeyPress) }
                        }

                    LandscapeMediaTabSection.SYSTEM_KEYS ->
                        if (isCompact) {
                            LandscapeSystemKeysSection(st, settings, style, onKeyPress)
                        } else {
                            LandscapeKbCard("System Keys") { LandscapeSystemKeysSectionContent(st, settings, style, onKeyPress) }
                        }

                    LandscapeMediaTabSection.QUICK_MODS ->
                        if (isCompact) {
                            LandscapeQuickModsSection(st, settings, style, onKeyPress, onClearMods)
                        } else {
                            LandscapeKbCard("Quick Modifiers") { LandscapeQuickModsSectionContent(st, settings, style, onKeyPress, onClearMods) }
                        }

                    LandscapeMediaTabSection.MERGED_SYSTEM_MODS ->
                        if (isCompact) {
                            LandscapeMergedSystemModsSection(st, settings, style, onKeyPress, onClearMods)
                        } else {
                            LandscapeKbCard("System & Modifiers") { LandscapeMergedSystemModsSectionContent(st, settings, style, onKeyPress, onClearMods) }
                        }
                }
            }
        }
    }
}