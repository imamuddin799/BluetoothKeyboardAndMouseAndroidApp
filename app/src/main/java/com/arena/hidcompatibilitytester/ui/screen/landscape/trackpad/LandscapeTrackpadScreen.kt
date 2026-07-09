package com.arena.hidcompatibilitytester.ui.screen.landscape.trackpad

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
import androidx.compose.ui.zIndex
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.LandscapeKbState
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.LandscapeKeyboardSettings
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.LandscapeLayoutMode
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.LandscapeRightColumnMode
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.LandscapeSharedCompactKeyboard
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.landscapeHandleKeyPress
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.landscapeHandleNumpadKey
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.landscapeHandleNumLockToggle

private const val IME_SENTINEL = "\u200B"

@Composable
fun LandscapeTrackpadScreen(
    isReady: Boolean,
    settings: LandscapeTrackpadSettings,
    keyboardSettings: LandscapeKeyboardSettings,
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

    // Trackpad-independent runtime overrides (session-scoped)
    var layoutModeOverride by remember(settings.trackpadKbDefaultLayoutMode) {
        mutableStateOf(settings.trackpadKbDefaultLayoutMode)
    }
    var rightColumnMode by remember(settings.trackpadKbDefaultRightColumn) {
        mutableStateOf(settings.trackpadKbDefaultRightColumn)
    }

    var hiddenText by remember {
        mutableStateOf(
            TextFieldValue(text = IME_SENTINEL, selection = TextRange(IME_SENTINEL.length))
        )
    }
    val focusRequester = remember { FocusRequester() }

    var kbSt by remember { mutableStateOf(LandscapeKbState()) }
    val kbScope = rememberCoroutineScope()

    val currentKbSt by rememberUpdatedState(kbSt)
    val currentKbSettings by rememberUpdatedState(keyboardSettings)

    val optionalRowOrder = keyboardSettings.getOptionalRowOrder(
        keyboardSettings.trackpadOptionalRowOrder
    )

    fun resetHiddenText() {
        hiddenText = TextFieldValue(text = IME_SENTINEL, selection = TextRange(IME_SENTINEL.length))
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

    fun handleKbKeyPress(key: com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.LandscapeKey) {
        kbSt = landscapeHandleKeyPress(
            key = key,
            st = currentKbSt,
            settings = currentKbSettings,
            scope = kbScope,
            onSendKey = onSendKey,
            onDelayedStateUpdate = { delayedSt -> kbSt = delayedSt }
        )
    }

    fun handleNumpadKey(code: Int, label: String) {
        kbSt = landscapeHandleNumpadKey(code, label, kbSt, currentKbSettings, kbScope, onSendKey) { kbSt = it }
    }

    fun handleNumLock() {
        kbSt = landscapeHandleNumLockToggle(kbSt, kbScope, onSendKey)
    }

    val hasOptionalRows = keyboardSettings.showMediaRowInTrackpad() ||
                          keyboardSettings.showNavRowInTrackpad()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080F18))
    ) {
        LandscapeTrackpadStatusBar(
            isReady = isReady,
            settings = settings,
            onShowSettings = onShowSettings,
            onToggleSystemKb = { toggleSystemKb() },
            onToggleInAppKb = { toggleInAppKb() },
            onToggleOptionalRows = { showOptionalRows = !showOptionalRows },
            systemKbVisible = systemKbVisible,
            inAppKbVisible = inAppKbVisible,
            showOptionalRows = showOptionalRows,
            hasOptionalRows = hasOptionalRows,
            kbState = kbSt,
            currentLayoutMode = layoutModeOverride,
            currentRightColumn = rightColumnMode,
            onToggleLayoutMode = {
                layoutModeOverride = if (layoutModeOverride == LandscapeLayoutMode.SINGLE_COLUMN)
                    LandscapeLayoutMode.TWO_COLUMN
                else
                    LandscapeLayoutMode.SINGLE_COLUMN
            },
            onToggleRightColumn = {
                rightColumnMode = if (rightColumnMode == LandscapeRightColumnMode.NAV_CLUSTER)
                    LandscapeRightColumnMode.NUMPAD
                else
                    LandscapeRightColumnMode.NAV_CLUSTER
            },
            onClearMods = {
                kbSt = kbSt.releaseMods().copy(lastKey = "")
                onSendKey(0, emptyList())
            },
        )

        // Hidden IME field for system keyboard
        BasicTextField(
            value = hiddenText,
            onValueChange = { newValue ->
                val newText = newValue.text
                when {
                    newText.length > IME_SENTINEL.length -> {
                        val added = newText.removePrefix(IME_SENTINEL)
                        if (added.isNotEmpty()) onTypeText(added)
                    }
                    newText.length < IME_SENTINEL.length -> sendBackspaceTap()
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
            LandscapeTrackpadNotReadyCard()
        } else {
            // Trackpad + keyboard overlay
            Box(modifier = Modifier.fillMaxSize()) {
                // Trackpad — always full size, never shifts
                Column(modifier = Modifier.fillMaxSize()) {
                    LandscapeTrackpadSurface(
                        modifier = Modifier.weight(1f),
                        settings = settings,
                        onSendMouse = onSendMouse,
                        physHoldActive = physHoldActive
                    )
                    LandscapeTrackpadClickButtons(
                        onSendMouse = onSendMouse,
                        physHoldActive = physHoldActive
                    )
                }

                // In-app keyboard overlay — natural height, sits at bottom
                androidx.compose.animation.AnimatedVisibility(
                    visible = inAppKbVisible,
                    enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxSize()
                        .zIndex(10f),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        LandscapeSharedCompactKeyboard(
                            st = kbSt,
                            settings = keyboardSettings,
                            showDismissBar = true,
                            showMediaRow = keyboardSettings.showMediaRowInTrackpad(),
                            showNavRow = keyboardSettings.showNavRowInTrackpad(),
                            showOptionalRows = showOptionalRows,
                            optionalRowOrder = optionalRowOrder,
                            showComboPreview = false,
                            layoutMode = layoutModeOverride,
                            rightColumnMode = rightColumnMode,
                            onKeyPress = ::handleKbKeyPress,
                            onConsumerKey = onConsumerKey,
                            onClearMods = {
                                kbSt = kbSt.releaseMods().copy(lastKey = "")
                                onSendKey(0, emptyList())
                            },
                            onNumpadKey = ::handleNumpadKey,
                            onNumLockToggle = ::handleNumLock,
                            onDismiss = { inAppKbVisible = false },
                        )
                    }
                }
            }
        }
    }
}