package com.arena.hidcompatibilitytester.ui.screen.keyboard.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.ui.screen.keyboard.*
import com.arena.hidcompatibilitytester.ui.screen.keyboard.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TYPE_SENTINEL = "\u200B"

@Composable
internal fun NavNumpadTab(
    st: KbState,
    settings: KeyboardSettings,
    showNumpad: Boolean,
    typeText: String,
    onTypeTextChanged: (String) -> Unit,
    onKeyPress: (Key) -> Unit,
    onNumpadKey: (Int, String) -> Unit,
    onNumLock: () -> Unit,
    onInsertToggle: () -> Unit,
    onClearMods: () -> Unit,
    onSendKey: (Int, List<Int>) -> Unit,
    onReleaseKeys: () -> Unit,
    onTypeText: (String) -> Unit,
    onSettingsChange: ((KeyboardSettings) -> Unit)? = null,
) {
    val scope          = rememberCoroutineScope()
    val style          = settings.keysTabSectionStyle
    val isComfort      = style == SectionStyle.MEDIA
    val merge          = settings.shouldMergeSystemMods(settings.navTabMergeSystemAndMods)
    val inPlaceReorder = settings.shouldAllowInPlaceReorder(settings.navTabInPlaceReorder)
    val swapped        = settings.navTabNavArrowsSwapped

    val visibleSections = settings.navTabSectionOrder.filter { section ->
        when (section) {
            NavTabSection.NAV_ARROWS         -> settings.navTabShowNavigation || settings.navTabShowArrowKeys
            NavTabSection.INSERT_TOGGLE      -> settings.navTabShowInsertToggle
            NavTabSection.SYSTEM_KEYS        -> settings.navTabShowSystemKeys && !merge
            NavTabSection.QUICK_MODS         -> settings.navTabShowQuickMods && !merge
            NavTabSection.MERGED_SYSTEM_MODS -> merge && settings.navTabShowSystemKeys && settings.navTabShowQuickMods
            NavTabSection.TYPE_TEXT          -> settings.navTabShowTypeText
        }
    }

    val scrollState = rememberScrollState()
    var viewportTopPx    by remember { mutableStateOf(0f) }
    var viewportBottomPx by remember { mutableStateOf(0f) }

    // Track if system keyboard is visible via IME
    var isImeVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080F18))
    ) {
        // Scrollable sections area
        Box(
            modifier = Modifier
                .weight(1f)
                .onGloballyPositioned { coords ->
                    viewportTopPx    = coords.positionInRoot().y
                    viewportBottomPx = viewportTopPx + coords.size.height
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .then(
                        if (!showNumpad) Modifier.imePadding()
                        else Modifier
                    )
                    .padding(
                        horizontal = if (isComfort) 8.dp else 4.dp,
                        vertical   = if (isComfort) 4.dp else 2.dp
                    )
            ) {
                ReorderableSectionColumn(
                    items            = visibleSections,
                    enabled          = inPlaceReorder,
                    scrollState      = scrollState,
                    viewportTopPx    = viewportTopPx,
                    viewportBottomPx = viewportBottomPx,
                    sectionSpacing   = if (isComfort) 10.dp else 3.dp,
                    onReorder        = { newOrder ->
                        val reordered = newOrder.toMutableList()
                        NavTabSection.entries.forEach { s ->
                            if (s !in reordered) reordered.add(s)
                        }
                        onSettingsChange?.invoke(
                            settings.copy(navTabSectionOrder = reordered)
                        )
                    }
                ) { _, section, _ ->
                    when (section) {
                        NavTabSection.NAV_ARROWS ->
                            NavTabNavArrowsSection(st, settings, style, swapped, isComfort, onKeyPress)

                        NavTabSection.INSERT_TOGGLE ->
                            if (isComfort) KbCard("Mode") {
                                InsertToggleContent(st, onInsertToggle)
                            } else InsertToggleContent(st, onInsertToggle)

                        NavTabSection.SYSTEM_KEYS ->
                            if (isComfort) KbCard("System Keys") {
                                SystemKeysSectionContent(st, settings, style, onKeyPress)
                            } else SystemKeysSection(st, settings, style, onKeyPress)

                        NavTabSection.QUICK_MODS ->
                            if (isComfort) KbCard("Quick Modifiers") {
                                QuickModsSectionContent(st, settings, style, onKeyPress, onClearMods)
                            } else QuickModsSection(st, settings, style, onKeyPress, onClearMods)

                        NavTabSection.MERGED_SYSTEM_MODS ->
                            if (isComfort) KbCard("System & Modifiers") {
                                MergedSystemModsSectionContent(st, settings, style, onKeyPress, onClearMods)
                            } else MergedSystemModsSection(st, settings, style, onKeyPress, onClearMods)

                        NavTabSection.TYPE_TEXT -> {
                            val bringIntoViewRequester = remember { BringIntoViewRequester() }
                            val keyboardController     = LocalSoftwareKeyboardController.current
                            if (isComfort) KbCard("Type & Send Text") {
                                TypeTextContent(
                                    typeText, onTypeTextChanged,
                                    onSendKey, onReleaseKeys, onTypeText,
                                    bringIntoViewRequester, keyboardController, scope
                                )
                            } else {
                                TypeTextContent(
                                    typeText, onTypeTextChanged,
                                    onSendKey, onReleaseKeys, onTypeText,
                                    bringIntoViewRequester, keyboardController, scope
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        // Fixed numpad — animated show/hide
        AnimatedVisibility(
            visible   = showNumpad,
            enter     = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit      = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF080F18))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color    = if (st.numLock) Color(0xFF1565C0) else Color(0xFF4A1800),
                        shape    = RoundedCornerShape(5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (st.numLock) "NumLock ON — typing numbers"
                            else "NumLock OFF — navigation mode",
                            color    = Color.White.copy(0.85f),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    NumpadLayout(
                        numLock  = st.numLock,
                        shift    = st.shift,
                        settings = settings,
                        onNumLock = onNumLock,
                        onKey    = onNumpadKey
                    )
                }
            }
        }
    }
}

@Composable
private fun NavTabNavArrowsSection(
    st: KbState,
    settings: KeyboardSettings,
    style: SectionStyle,
    swapped: Boolean,
    isComfort: Boolean,
    onKeyPress: (Key) -> Unit,
) {
    val showNav    = settings.navTabShowNavigation
    val showArrows = settings.navTabShowArrowKeys

    if (isComfort) {
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
            if (showNav)    KbCard("Navigation") { NavigationSectionContent(st, settings, style, onKeyPress) }
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
            if (showNav)    NavigationSection(st, settings, style, onKeyPress)
            if (showArrows) ArrowKeysSection(st, settings, style, onKeyPress)
        }
    }
}

@Composable
private fun InsertToggleContent(
    st: KbState,
    onInsertToggle: () -> Unit,
) {
    Surface(
        color    = if (st.insertMode) Color(0xFF0D1F0D) else Color(0xFF2A1800),
        shape    = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInsertToggle() }
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (st.insertMode) "INSERT" else "OVERWRITE",
                color      = if (st.insertMode) Color(0xFF81C784) else Color(0xFFFFB74D),
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (st.insertMode) "Tap to switch → Overwrite mode"
                else "Tap to switch → Insert mode",
                color    = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TypeTextContent(
    typeText: String,
    onTypeTextChanged: (String) -> Unit,
    onSendKey: (Int, List<Int>) -> Unit,
    onReleaseKeys: () -> Unit,
    onTypeText: (String) -> Unit,
    bringIntoViewRequester: BringIntoViewRequester,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    // Use sentinel-based TextFieldValue to reliably detect backspace even when text is empty
    var fieldValue by remember(typeText) {
        mutableStateOf(
            TextFieldValue(
                text = TYPE_SENTINEL + typeText,
                selection = TextRange((TYPE_SENTINEL + typeText).length)
            )
        )
    }

    // Sync when typeText changes externally (e.g. Clear button)
    LaunchedEffect(typeText) {
        val expected = TYPE_SENTINEL + typeText
        if (fieldValue.text != expected) {
            fieldValue = TextFieldValue(
                text = expected,
                selection = TextRange(expected.length)
            )
        }
    }

    Column {
        OutlinedTextField(
            value         = fieldValue,
            onValueChange = { newValue ->
                val newRaw = newValue.text
                val oldContent = fieldValue.text.removePrefix(TYPE_SENTINEL)
                val newContent = newRaw.removePrefix(TYPE_SENTINEL)

                val expectedEnd = (TYPE_SENTINEL + newContent).length

                when {
                    // Sentinel was deleted — treat as backspace
                    !newRaw.startsWith(TYPE_SENTINEL) -> {
                        val deletedCount = (TYPE_SENTINEL.length + oldContent.length) - newRaw.length
                        repeat(deletedCount.coerceAtLeast(1)) {
                            onSendKey(0, listOf(0x2A))
                            onReleaseKeys()
                        }
                        val remaining = if (oldContent.isNotEmpty()) {
                            oldContent.dropLast(deletedCount.coerceAtMost(oldContent.length))
                        } else ""
                        val restored = TYPE_SENTINEL + remaining
                        fieldValue = TextFieldValue(
                            text = restored,
                            selection = TextRange(restored.length)
                        )
                        onTypeTextChanged(remaining)
                    }

                    newContent.length > oldContent.length -> {
                        // Check if insertion was at the end
                        val insertedAtEnd = newContent.endsWith(
                            newContent.takeLast(newContent.length - oldContent.length)
                        ) && newContent.dropLast(newContent.length - oldContent.length) == oldContent

                        if (insertedAtEnd) {
                            val added = newContent.substring(oldContent.length)
                            if (added.isNotEmpty()) onTypeText(added)
                            fieldValue = TextFieldValue(
                                text = TYPE_SENTINEL + newContent,
                                selection = TextRange(expectedEnd)
                            )
                            onTypeTextChanged(newContent)
                        } else {
                            // Mid-text insertion — reject, keep old value, snap cursor to end
                            val old = TYPE_SENTINEL + oldContent
                            fieldValue = TextFieldValue(
                                text = old,
                                selection = TextRange(old.length)
                            )
                        }
                    }

                    newContent.length < oldContent.length -> {
                        // Check if deletion was from the end
                        val deletedFromEnd = oldContent.startsWith(newContent)

                        if (deletedFromEnd) {
                            val deletedCount = oldContent.length - newContent.length
                            repeat(deletedCount) {
                                onSendKey(0, listOf(0x2A))
                                onReleaseKeys()
                            }
                            fieldValue = TextFieldValue(
                                text = TYPE_SENTINEL + newContent,
                                selection = TextRange(expectedEnd)
                            )
                            onTypeTextChanged(newContent)
                        } else {
                            // Mid-text deletion — reject, keep old value, snap cursor to end
                            val old = TYPE_SENTINEL + oldContent
                            fieldValue = TextFieldValue(
                                text = old,
                                selection = TextRange(old.length)
                            )
                        }
                    }

                    else -> {
                        // Text same but cursor moved — snap cursor back to end
                        fieldValue = TextFieldValue(
                            text = TYPE_SENTINEL + newContent,
                            selection = TextRange(expectedEnd)
                        )
                    }
                }
            },
            modifier      = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        keyboardController?.show()
                        scope.launch {
                            delay(250)
                            bringIntoViewRequester.bringIntoView()
                        }
                    }
                },
            placeholder   = {
                Text("Type here…", color = Color.Gray)
            },
            maxLines      = 4,
            colors        = OutlinedTextFieldDefaults.colors(
                focusedTextColor     = Color.White,
                unfocusedTextColor   = Color.White,
                focusedBorderColor   = Color(0xFF4A90D9),
                unfocusedBorderColor = Color.White.copy(0.2f),
                cursorColor          = Color(0xFF4A90D9)
            ),
            // Hide the sentinel visually
            visualTransformation = SentinelVisualTransformation()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick  = { onTypeTextChanged("") },
            modifier = Modifier.fillMaxWidth(),
            border   = BorderStroke(1.dp, Color.White.copy(0.2f))
        ) {
            Text("Clear", color = Color.White)
        }
    }
}

private class SentinelVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val sentinelLen = TYPE_SENTINEL.length
        val filtered = if (text.text.startsWith(TYPE_SENTINEL)) {
            text.text.removePrefix(TYPE_SENTINEL)
        } else {
            text.text
        }

        return TransformedText(
            AnnotatedString(filtered),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    return (offset - sentinelLen).coerceAtLeast(0)
                }

                override fun transformedToOriginal(offset: Int): Int {
                    return offset + sentinelLen
                }
            }
        )
    }
}