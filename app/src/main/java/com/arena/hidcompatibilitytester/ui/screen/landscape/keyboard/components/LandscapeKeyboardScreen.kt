package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.components.LandscapeKbStatusBar

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

    // Runtime layout override (session-only)
    var layoutModeOverride by remember(settings.landscapeLayoutMode) {
        mutableStateOf<LandscapeLayoutMode?>(null)
    }
    val effectiveLayoutMode = layoutModeOverride ?: settings.landscapeLayoutMode

    // Runtime right-column mode (session-only)
    var rightColumnMode by remember { mutableStateOf(LandscapeRightColumnMode.NAV_CLUSTER) }

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
            onToggleKeyboard = { showKeyboard = !showKeyboard },
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
        )

        if (showKeyboard) {
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
    }
}