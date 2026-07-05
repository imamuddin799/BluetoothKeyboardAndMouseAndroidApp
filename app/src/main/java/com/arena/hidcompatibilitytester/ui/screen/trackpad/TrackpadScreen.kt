package com.arena.hidcompatibilitytester.ui.screen.trackpad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.ui.screen.keyboard.KbState
import com.arena.hidcompatibilitytester.ui.screen.keyboard.KeyboardSettings
import com.arena.hidcompatibilitytester.ui.screen.keyboard.SharedCompactKeyboard
import com.arena.hidcompatibilitytester.ui.screen.keyboard.handleKeyPress

private const val IME_SENTINEL = "\u200B"

@Composable
fun TrackpadScreen(
    isReady: Boolean,
    settings: TrackpadSettings,
    keyboardSettings: KeyboardSettings,
    onSendMouse: (dx: Int, dy: Int, buttons: Int, wheel: Int) -> Unit,
    onShowSettings: () -> Unit,
    onSendKey: (Int, List<Int>) -> Unit,
    onReleaseKeys: () -> Unit,
    onConsumerKey: (Int) -> Unit,
    onTypeText: (String) -> Unit,
) {
    val physHoldActive = remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var systemKbVisible by remember { mutableStateOf(false) }
    var inAppKbVisible by remember { mutableStateOf(false) }
    var showOptionalRows by remember { mutableStateOf(true) }

    var hiddenText by remember {
        mutableStateOf(
            TextFieldValue(
                text = IME_SENTINEL,
                selection = TextRange(IME_SENTINEL.length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }

    var kbSt by remember { mutableStateOf(KbState()) }
    val kbScope = rememberCoroutineScope()

    val currentKbSt by rememberUpdatedState(kbSt)
    val currentKbSettings by rememberUpdatedState(keyboardSettings)

    val optionalRowOrder = keyboardSettings.getOptionalRowOrder(
        keyboardSettings.trackpadOptionalRowOrder
    )

    fun resetHiddenText() {
        hiddenText = TextFieldValue(
            text = IME_SENTINEL,
            selection = TextRange(IME_SENTINEL.length)
        )
    }

    fun sendBackspaceTap() {
        onSendKey(0, listOf(0x2A))
        onReleaseKeys()
    }

    fun showSystemKeyboard() {
        inAppKbVisible = false
        systemKbVisible = true
        resetHiddenText()
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    fun hideSystemKeyboard() {
        systemKbVisible = false
        resetHiddenText()
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    fun toggleSystemKb() {
        if (systemKbVisible) hideSystemKeyboard() else showSystemKeyboard()
    }

    fun toggleInAppKb() {
        if (inAppKbVisible) {
            inAppKbVisible = false
        } else {
            if (systemKbVisible) hideSystemKeyboard()
            inAppKbVisible = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080F18))
    ) {
        TrackpadStatusBar(
            isReady = isReady,
            settings = settings,
            onShowSettings = onShowSettings,
            onToggleSystemKb = { toggleSystemKb() },
            onToggleInAppKb = { toggleInAppKb() },
            onToggleOptionalRows = { showOptionalRows = !showOptionalRows },
            systemKbVisible = systemKbVisible,
            inAppKbVisible = inAppKbVisible,
            showOptionalRows = showOptionalRows,
            hasOptionalRows = keyboardSettings.showMediaRowInTrackpad() || keyboardSettings.showNavRowInTrackpad(),
        )

        BasicTextField(
            value = hiddenText,
            onValueChange = { newValue ->
                val newText = newValue.text

                when {
                    newText.length > IME_SENTINEL.length -> {
                        val added = newText.removePrefix(IME_SENTINEL)
                        if (added.isNotEmpty()) {
                            onTypeText(added)
                        }
                    }

                    newText.length < IME_SENTINEL.length -> {
                        sendBackspaceTap()
                    }
                }

                resetHiddenText()
            },
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester),
            textStyle = TextStyle(fontSize = 1.sp, color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
        )

        if (!isReady) {
            TrackpadNotReadyCard()
        } else {
            TrackpadSurface(
                modifier = Modifier.weight(1f),
                settings = settings,
                onSendMouse = onSendMouse,
                physHoldActive = physHoldActive
            )

            TrackpadClickButtons(
                onSendMouse = onSendMouse,
                physHoldActive = physHoldActive
            )

            AnimatedVisibility(
                visible = inAppKbVisible,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
            ) {
                Column {
                    SharedCompactKeyboard(
                        st = kbSt,
                        settings = keyboardSettings,
                        showDismissBar = true,
                        showMediaRow = keyboardSettings.showMediaRowInTrackpad(),
                        showNavRow = keyboardSettings.showNavRowInTrackpad(),
                        showOptionalRows = showOptionalRows,
                        optionalRowOrder = optionalRowOrder,
                        showComboPreview = false,
                        onKeyPress = { key ->
                            kbSt = handleKeyPress(
                                key = key,
                                st = currentKbSt,
                                settings = currentKbSettings,
                                scope = kbScope,
                                onSendKey = onSendKey,
                                onDelayedStateUpdate = { delayedSt -> kbSt = delayedSt }
                            )
                        },
                        onConsumerKey = onConsumerKey,
                        onClearMods = {
                            kbSt = kbSt.releaseMods().copy(lastKey = "")
                            onSendKey(0, emptyList())
                        },
                        onDismiss = { inAppKbVisible = false },
                    )

                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color(0xFF080F18))
                    )
                }
            }
        }
    }
}