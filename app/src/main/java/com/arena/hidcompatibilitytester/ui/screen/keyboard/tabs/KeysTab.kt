package com.arena.hidcompatibilitytester.ui.screen.keyboard.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.arena.hidcompatibilitytester.ui.screen.keyboard.*
import com.arena.hidcompatibilitytester.ui.screen.keyboard.components.*

@Composable
internal fun KeysTab(
    st: KbState,
    settings: KeyboardSettings,
    showKeyboard: Boolean,
    showOptionalRows: Boolean,
    onKeyPress: (Key) -> Unit,
    onClearMods: () -> Unit,
    onSendKey: (Int, List<Int>) -> Unit,
    onReleaseKeys: () -> Unit,
    onConsumerKey: (Int) -> Unit,
    onTypeText: (String) -> Unit,
    onSettingsChange: ((KeyboardSettings) -> Unit)? = null,
) {
    val style = settings.keysTabSectionStyle
    val isMedia = style == SectionStyle.MEDIA
    val merge = settings.shouldMergeSystemMods(settings.keysTabMergeSystemAndMods)
    val inPlaceReorder = settings.shouldAllowInPlaceReorder(settings.keysTabInPlaceReorder)
    val swapped = settings.keysTabNavArrowsSwapped

    val optionalRowOrder = settings.getOptionalRowOrder(settings.keysTabOptionalRowOrder)

    val visibleSections = settings.keysTabSectionOrder.filter { section ->
        when (section) {
            KeysTabSection.NAV_ARROWS -> settings.keysTabShowNavigation || settings.keysTabShowArrowKeys
            KeysTabSection.SYSTEM_KEYS -> settings.keysTabShowSystemKeys && !merge
            KeysTabSection.QUICK_MODS -> settings.keysTabShowQuickMods && !merge
            KeysTabSection.MERGED_SYSTEM_MODS -> merge && settings.keysTabShowSystemKeys && settings.keysTabShowQuickMods
        }
    }

    val scrollState = rememberScrollState()
    var viewportTopPx by remember { mutableStateOf(0f) }
    var viewportBottomPx by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080F18))
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .onGloballyPositioned { coords ->
                    viewportTopPx = coords.positionInRoot().y
                    viewportBottomPx = viewportTopPx + coords.size.height
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        horizontal = if (isMedia) 8.dp else 4.dp,
                        vertical = 2.dp
                    )
            ) {
                ReorderableSectionColumn(
                    items = visibleSections,
                    enabled = inPlaceReorder,
                    scrollState = scrollState,
                    viewportTopPx = viewportTopPx,
                    viewportBottomPx = viewportBottomPx,
                    sectionSpacing = if (isMedia) 10.dp else 3.dp,
                    onReorder = { newOrder ->
                        val reordered = newOrder.toMutableList()
                        KeysTabSection.entries.forEach { s ->
                            if (s !in reordered) reordered.add(s)
                        }
                        onSettingsChange?.invoke(
                            settings.copy(keysTabSectionOrder = reordered)
                        )
                    }
                ) { _, section, _ ->
                    when (section) {
                        KeysTabSection.NAV_ARROWS ->
                            KeysTabNavArrowsSection(
                                st = st,
                                settings = settings,
                                style = style,
                                swapped = swapped,
                                isMedia = isMedia,
                                onKeyPress = onKeyPress
                            )

                        KeysTabSection.SYSTEM_KEYS ->
                            if (isMedia) KbCard("System Keys") {
                                SystemKeysSectionContent(st, settings, style, onKeyPress)
                            } else SystemKeysSection(st, settings, style, onKeyPress)

                        KeysTabSection.QUICK_MODS ->
                            if (isMedia) KbCard("Quick Modifiers") {
                                QuickModsSectionContent(st, settings, style, onKeyPress, onClearMods)
                            } else QuickModsSection(st, settings, style, onKeyPress, onClearMods)

                        KeysTabSection.MERGED_SYSTEM_MODS ->
                            if (isMedia) KbCard("System & Modifiers") {
                                MergedSystemModsSectionContent(st, settings, style, onKeyPress, onClearMods)
                            } else MergedSystemModsSection(st, settings, style, onKeyPress, onClearMods)
                    }
                }
            }
        }

        // Fixed compact keyboard — animated show/hide
        AnimatedVisibility(
            visible = showKeyboard,
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 3.dp, bottom = 6.dp)
            ) {
                SharedCompactKeyboard(
                    st = st,
                    settings = settings,
                    showDismissBar = false,
                    showMediaRow = settings.showMediaRowInKeysTab(),
                    showNavRow = settings.showNavRowInKeysTab(),
                    showOptionalRows = showOptionalRows,
                    optionalRowOrder = optionalRowOrder,
                    onKeyPress = onKeyPress,
                    onConsumerKey = onConsumerKey,
                )
            }
        }
    }
}

@Composable
private fun KeysTabNavArrowsSection(
    st: KbState,
    settings: KeyboardSettings,
    style: SectionStyle,
    swapped: Boolean,
    isMedia: Boolean,
    onKeyPress: (Key) -> Unit,
) {
    val showNav = settings.keysTabShowNavigation
    val showArrows = settings.keysTabShowArrowKeys

    if (isMedia) {
        if (showNav && showArrows) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (swapped) {
                    KbCard("Arrows", Modifier.weight(1f)) {
                        ArrowKeysSectionContent(st, settings, style, onKeyPress)
                    }
                    KbCard("Navigation", Modifier.weight(1f)) {
                        NavigationSectionContent(st, settings, style, onKeyPress)
                    }
                } else {
                    KbCard("Navigation", Modifier.weight(1f)) {
                        NavigationSectionContent(st, settings, style, onKeyPress)
                    }
                    KbCard("Arrows", Modifier.weight(1f)) {
                        ArrowKeysSectionContent(st, settings, style, onKeyPress)
                    }
                }
            }
        } else {
            if (showNav) KbCard("Navigation") { NavigationSectionContent(st, settings, style, onKeyPress) }
            if (showArrows) KbCard("Arrow Keys") { ArrowKeysSectionContent(st, settings, style, onKeyPress) }
        }
    } else {
        if (showNav && showArrows) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (swapped) {
                    Box(Modifier.weight(1f)) { ArrowKeysSection(st, settings, style, onKeyPress) }
                    Box(Modifier.weight(1f)) { NavigationSection(st, settings, style, onKeyPress) }
                } else {
                    Box(Modifier.weight(1f)) { NavigationSection(st, settings, style, onKeyPress) }
                    Box(Modifier.weight(1f)) { ArrowKeysSection(st, settings, style, onKeyPress) }
                }
            }
        } else {
            if (showNav) NavigationSection(st, settings, style, onKeyPress)
            if (showArrows) ArrowKeysSection(st, settings, style, onKeyPress)
        }
    }
}