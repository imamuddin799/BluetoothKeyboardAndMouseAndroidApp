package com.arena.hidcompatibilitytester.ui.screen.trackpad

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.ui.screen.keyboard.KeyboardSettings
import com.arena.hidcompatibilitytester.ui.screen.keyboard.SharedCompactKeyboard

@Composable
fun TrackpadScreen(
    isReady          : Boolean,
    settings         : TrackpadSettings,
    keyboardSettings : KeyboardSettings,
    onSendMouse      : (dx: Int, dy: Int, buttons: Int, wheel: Int) -> Unit,
    onShowSettings   : () -> Unit,
    onSendKey        : (Int, List<Int>) -> Unit,
    onReleaseKeys    : () -> Unit,
    onConsumerKey    : (Int) -> Unit,
    onTypeText       : (String) -> Unit,
) {
    val physHoldActive = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var systemKbVisible by remember { mutableStateOf(false) }
    var inAppKbVisible  by remember { mutableStateOf(false) }

    var hiddenText by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }

    fun showSystemKeyboard() {
        inAppKbVisible = false
        systemKbVisible = true
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    fun hideSystemKeyboard() {
        systemKbVisible = false
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
            isReady          = isReady,
            settings         = settings,
            onShowSettings   = onShowSettings,
            onToggleSystemKb = { toggleSystemKb() },
            onToggleInAppKb  = { toggleInAppKb() },
            systemKbVisible  = systemKbVisible,
            inAppKbVisible   = inAppKbVisible,
        )

        BasicTextField(
            value         = hiddenText,
            onValueChange = { newValue ->
                val oldText = hiddenText.text
                val newText = newValue.text
                when {
                    newText.length > oldText.length -> {
                        val added = newText.substring(oldText.length)
                        if (added.isNotEmpty()) onTypeText(added)
                    }
                    newText.length < oldText.length -> {
                        repeat(oldText.length - newText.length) {
                            onSendKey(0, listOf(0x2A))
                        }
                    }
                }
                hiddenText = newValue
            },
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester),
            textStyle   = TextStyle(fontSize = 1.sp, color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
        )

        if (!isReady) {
            TrackpadNotReadyCard()
        } else {
            TrackpadSurface(
                modifier       = Modifier.weight(1f),
                settings       = settings,
                onSendMouse    = onSendMouse,
                physHoldActive = physHoldActive
            )

            if (inAppKbVisible) {
                TrackpadClickButtons(
                    onSendMouse    = onSendMouse,
                    physHoldActive = physHoldActive
                )

                SharedCompactKeyboard(
                    settings       = keyboardSettings,
                    showDismissBar = true,
                    onSendKey      = onSendKey,
                    onReleaseKeys  = onReleaseKeys,
                    onConsumerKey  = onConsumerKey,
                    onTypeText     = onTypeText,
                    onDismiss      = { inAppKbVisible = false },
                )

                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color(0xFF080F18))
                )
            } else {
                TrackpadClickButtons(
                    onSendMouse    = onSendMouse,
                    physHoldActive = physHoldActive
                )
            }
        }
    }
}