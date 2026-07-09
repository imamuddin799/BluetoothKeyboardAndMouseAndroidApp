package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.components.LandscapeKbStatusBar

private const val TYPE_SENTINEL = "\u200B"

@Composable
fun LandscapeKeyboardScreen(
    isReady: Boolean,
    settings: LandscapeKeyboardSettings,
    onSendKey: (Int, List<Int>) -> Unit,
    onReleaseKeys: () -> Unit,
    onConsumerKey: (Int) -> Unit,
    onTypeText: (String) -> Unit,
    onShowSettings: () -> Unit,
    onSettingsChange: (LandscapeKeyboardSettings) -> Unit,
) {
    var st by remember { mutableStateOf(LandscapeKbState()) }
    var showKeyboard by remember { mutableStateOf(true) }
    var showOptionalRows by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    var layoutModeOverride by remember(settings.landscapeLayoutMode) {
        mutableStateOf<LandscapeLayoutMode?>(null)
    }
    val effectiveLayoutMode = layoutModeOverride ?: settings.landscapeLayoutMode

    var rightColumnMode by remember { mutableStateOf(LandscapeRightColumnMode.NAV_CLUSTER) }

    var systemKeyboardActive by remember { mutableStateOf(false) }

    fun handleKeyPress(key: LandscapeKey) {
        st = landscapeHandleKeyPress(key, st, settings, scope, onSendKey) { st = it }
    }

    fun handleNumpadKey(code: Int, label: String) {
        st = landscapeHandleNumpadKey(code, label, st, settings, scope, onSendKey) { st = it }
    }

    fun handleNumLock() {
        st = landscapeHandleNumLockToggle(st, scope, onSendKey)
    }

    val hasOptional = settings.showMediaRowInKeysTab() || settings.showNavRowInKeysTab()

    Column(Modifier.fillMaxSize().background(Color(0xFF080F18))) {
        LandscapeKbStatusBar(
            st = st,
            isReady = isReady,
            showFullStatus = settings.showStatusBar,
            showComboPreview = settings.showComboPreview,
            showKeyboard = showKeyboard,
            showOptionalRows = showOptionalRows,
            hasOptionalRows = hasOptional,
            onClearMods = { st = st.releaseMods(); onSendKey(0, emptyList()) },
            onShowSettings = onShowSettings,
            onToggleKeyboard = {
                if (systemKeyboardActive) {
                    systemKeyboardActive = false
                    showKeyboard = true
                } else {
                    showKeyboard = !showKeyboard
                }
            },
            onToggleOptionalRows = { showOptionalRows = !showOptionalRows },
            currentLayoutMode = effectiveLayoutMode,
            onToggleLayoutMode = {
                layoutModeOverride = if (effectiveLayoutMode == LandscapeLayoutMode.SINGLE_COLUMN)
                    LandscapeLayoutMode.TWO_COLUMN
                else
                    LandscapeLayoutMode.SINGLE_COLUMN
            },
            currentRightColumn = rightColumnMode,
            onToggleRightColumn = {
                rightColumnMode = if (rightColumnMode == LandscapeRightColumnMode.NAV_CLUSTER)
                    LandscapeRightColumnMode.NUMPAD
                else
                    LandscapeRightColumnMode.NAV_CLUSTER
            },
            systemKeyboardActive = systemKeyboardActive,
            onToggleSystemKeyboard = {
                systemKeyboardActive = !systemKeyboardActive
                if (systemKeyboardActive) {
                    showKeyboard = false
                } else {
                    showKeyboard = true
                }
            },
        )

        if (systemKeyboardActive) {
            LandscapeSystemKeyboardBar(
                onSendKey = onSendKey,
                onReleaseKeys = onReleaseKeys,
                onTypeText = onTypeText,
            )
        }

        if (showKeyboard) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LandscapeSharedCompactKeyboard(
                    st = st,
                    settings = settings,
                    showMediaRow = settings.showMediaRowInKeysTab(),
                    showNavRow = settings.showNavRowInKeysTab(),
                    showOptionalRows = showOptionalRows,
                    layoutMode = effectiveLayoutMode,
                    rightColumnMode = rightColumnMode,
                    onKeyPress = ::handleKeyPress,
                    onConsumerKey = onConsumerKey,
                    onClearMods = { st = st.releaseMods(); onSendKey(0, emptyList()) },
                    onNumpadKey = ::handleNumpadKey,
                    onNumLockToggle = ::handleNumLock,
                )
            }
        } else if (systemKeyboardActive) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun LandscapeSystemKeyboardBar(
    onSendKey: (Int, List<Int>) -> Unit,
    onReleaseKeys: () -> Unit,
    onTypeText: (String) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var fieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = TYPE_SENTINEL,
                selection = TextRange(TYPE_SENTINEL.length)
            )
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A1520))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                val newRaw = newValue.text
                val oldContent = fieldValue.text.removePrefix(TYPE_SENTINEL)
                val newContent = newRaw.removePrefix(TYPE_SENTINEL)

                when {
                    !newRaw.startsWith(TYPE_SENTINEL) -> {
                        onSendKey(0, listOf(0x2A))
                        onReleaseKeys()
                        val remaining = if (oldContent.isNotEmpty()) oldContent.dropLast(1) else ""
                        val restored = TYPE_SENTINEL + remaining
                        fieldValue = TextFieldValue(
                            text = restored,
                            selection = TextRange(restored.length)
                        )
                    }

                    newContent.length > oldContent.length -> {
                        val added = newContent.substring(oldContent.length)
                        if (added.isNotEmpty()) onTypeText(added)
                        fieldValue = TextFieldValue(
                            text = TYPE_SENTINEL + newContent,
                            selection = TextRange((TYPE_SENTINEL + newContent).length)
                        )
                    }

                    newContent.length < oldContent.length -> {
                        val deletedCount = oldContent.length - newContent.length
                        repeat(deletedCount) {
                            onSendKey(0, listOf(0x2A))
                            onReleaseKeys()
                        }
                        fieldValue = TextFieldValue(
                            text = TYPE_SENTINEL + newContent,
                            selection = TextRange((TYPE_SENTINEL + newContent).length)
                        )
                    }

                    else -> {
                        fieldValue = TextFieldValue(
                            text = TYPE_SENTINEL + newContent,
                            selection = TextRange((TYPE_SENTINEL + newContent).length)
                        )
                    }
                }
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            placeholder = {
                Text("Type here…", color = Color.Gray, fontSize = 13.sp)
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF4A90D9),
                unfocusedBorderColor = Color.White.copy(0.2f),
                cursorColor = Color(0xFF4A90D9),
                focusedContainerColor = Color(0xFF111C28),
                unfocusedContainerColor = Color(0xFF111C28),
            ),
            visualTransformation = LandscapeSentinelVisualTransformation(),
        )

        OutlinedButton(
            onClick = {
                fieldValue = TextFieldValue(
                    text = TYPE_SENTINEL,
                    selection = TextRange(TYPE_SENTINEL.length)
                )
            },
            border = BorderStroke(1.dp, Color.White.copy(0.3f)),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text("Clear", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

private class LandscapeSentinelVisualTransformation : VisualTransformation {
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
                override fun originalToTransformed(offset: Int): Int =
                    (offset - sentinelLen).coerceAtLeast(0)

                override fun transformedToOriginal(offset: Int): Int =
                    offset + sentinelLen
            }
        )
    }
}