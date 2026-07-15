package com.arena.hidcompatibilitytester.ui.screen.keyboard.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
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
    onSettingsChange: ((KeyboardSettings) -> Unit)? = null,
) {
    val mediaH = settings.mediaKeySize.heightDp.dp
    val style = settings.mediaTabSectionStyle
    val isCompact = style == SectionStyle.COMPACT
    val merge = settings.shouldMergeSystemMods(settings.mediaTabMergeSystemAndMods)
    val inPlaceReorder = settings.shouldAllowInPlaceReorder(settings.mediaTabInPlaceReorder)

    val visibleSections = settings.mediaTabSectionOrder.filter { section ->
        when (section) {
            MediaTabSection.TRANSPORT -> true
            MediaTabSection.VOLUME_BRIGHTNESS -> true
            MediaTabSection.NAVIGATION -> settings.mediaTabShowNavigation
            MediaTabSection.ARROW_KEYS -> settings.mediaTabShowArrowKeys
            MediaTabSection.SYSTEM_KEYS -> settings.mediaTabShowSystemKeys && !merge
            MediaTabSection.QUICK_MODS -> settings.mediaTabShowQuickMods && !merge
            MediaTabSection.MERGED_SYSTEM_MODS ->
                merge && settings.mediaTabShowSystemKeys && settings.mediaTabShowQuickMods
        }
    }

    val scrollState = rememberScrollState()
    var viewportTopPx by remember { mutableStateOf(0f) }
    var viewportBottomPx by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080F18))
            .onGloballyPositioned { coords ->
                viewportTopPx = coords.positionInRoot().y
                viewportBottomPx = viewportTopPx + coords.size.height
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(if (isCompact) 4.dp else 8.dp)
        ) {
            ReorderableSectionColumn(
                items = visibleSections,
                enabled = inPlaceReorder,
                scrollState = scrollState,
                viewportTopPx = viewportTopPx,
                viewportBottomPx = viewportBottomPx,
                sectionSpacing = if (isCompact) 3.dp else 10.dp,
                onReorder = { newOrder ->
                    val reordered = newOrder.toMutableList()
                    MediaTabSection.entries.forEach { s ->
                        if (s !in reordered) reordered.add(s)
                    }
                    onSettingsChange?.invoke(
                        settings.copy(mediaTabSectionOrder = reordered)
                    )
                }
            ) { _, section, _ ->
                when (section) {
                    MediaTabSection.TRANSPORT ->
                        if (isCompact) {
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    "Transport",
                                    color = Color(0xFF607D8B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    MEDIA_TRANSPORT.forEach { mk ->
                                        MediaKeyBtn(
                                            mk.icon, mk.label,
                                            Modifier.weight(1f), mediaH, settings,
                                            mediaGroup = MediaRowGroup.TRANSPORT,
                                        ) {
                                            onConsumerKey(mk.code)
                                            onUpdateLastKey(mk.label)
                                        }
                                    }
                                }
                            }
                        } else {
                            KbCard("Transport") {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    MEDIA_TRANSPORT.forEach { mk ->
                                        MediaKeyBtn(
                                            mk.icon, mk.label,
                                            Modifier.weight(1f), mediaH, settings,
                                            mediaGroup = MediaRowGroup.TRANSPORT,
                                        ) {
                                            onConsumerKey(mk.code)
                                            onUpdateLastKey(mk.label)
                                        }
                                    }
                                }
                            }
                        }

                    MediaTabSection.VOLUME_BRIGHTNESS ->
                        if (isCompact) {
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    "Volume & Brightness",
                                    color = Color(0xFF607D8B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    MEDIA_VOLUME.forEach { mk ->
                                        MediaKeyBtn(
                                            mk.icon, mk.label,
                                            Modifier.weight(1f), mediaH - 8.dp, settings,
                                            mediaGroup = MediaRowGroup.VOLUME,
                                        ) {
                                            onConsumerKey(mk.code)
                                            onUpdateLastKey(mk.label)
                                        }
                                    }
                                    MEDIA_BRIGHT.forEach { mk ->
                                        MediaKeyBtn(
                                            mk.icon, mk.label,
                                            Modifier.weight(1f), mediaH - 8.dp, settings,
                                            mediaGroup = MediaRowGroup.BRIGHTNESS,
                                        ) {
                                            onConsumerKey(mk.code)
                                            onUpdateLastKey(mk.label)
                                        }
                                    }
                                }
                            }
                        } else {
                            KbCard("Volume & Brightness") {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    MEDIA_VOLUME.forEach { mk ->
                                        MediaKeyBtn(
                                            mk.icon, mk.label,
                                            Modifier.weight(1f), mediaH - 8.dp, settings,
                                            mediaGroup = MediaRowGroup.VOLUME,
                                        ) {
                                            onConsumerKey(mk.code)
                                            onUpdateLastKey(mk.label)
                                        }
                                    }
                                    MEDIA_BRIGHT.forEach { mk ->
                                        MediaKeyBtn(
                                            mk.icon, mk.label,
                                            Modifier.weight(1f), mediaH - 8.dp, settings,
                                            mediaGroup = MediaRowGroup.BRIGHTNESS,
                                        ) {
                                            onConsumerKey(mk.code)
                                            onUpdateLastKey(mk.label)
                                        }
                                    }
                                }
                            }
                        }

                    MediaTabSection.NAVIGATION ->
                        if (isCompact) {
                            NavigationSection(st, settings, style, onKeyPress)
                        } else {
                            KbCard("Navigation") {
                                NavigationSectionContent(st, settings, style, onKeyPress)
                            }
                        }

                    MediaTabSection.ARROW_KEYS ->
                        if (isCompact) {
                            ArrowKeysSection(st, settings, style, onKeyPress)
                        } else {
                            KbCard("Arrow Keys") {
                                ArrowKeysSectionContent(st, settings, style, onKeyPress)
                            }
                        }

                    MediaTabSection.SYSTEM_KEYS ->
                        if (isCompact) {
                            SystemKeysSection(st, settings, style, onKeyPress)
                        } else {
                            KbCard("System Keys") {
                                SystemKeysSectionContent(st, settings, style, onKeyPress)
                            }
                        }

                    MediaTabSection.QUICK_MODS ->
                        if (isCompact) {
                            QuickModsSection(st, settings, style, onKeyPress, onClearMods)
                        } else {
                            KbCard("Quick Modifiers") {
                                QuickModsSectionContent(st, settings, style, onKeyPress, onClearMods)
                            }
                        }

                    MediaTabSection.MERGED_SYSTEM_MODS ->
                        if (isCompact) {
                            MergedSystemModsSection(st, settings, style, onKeyPress, onClearMods)
                        } else {
                            KbCard("System & Modifiers") {
                                MergedSystemModsSectionContent(st, settings, style, onKeyPress, onClearMods)
                            }
                        }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}