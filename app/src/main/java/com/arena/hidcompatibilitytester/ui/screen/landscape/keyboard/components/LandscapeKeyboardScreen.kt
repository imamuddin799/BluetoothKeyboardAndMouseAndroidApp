package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.components.LandscapeKbStatusBar
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.tabs.*

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
    var st by remember(settings.defaultTab) { mutableStateOf(LandscapeKbState(tab = settings.defaultTab)) }
    var showKeyboard by remember { mutableStateOf(true) }
    var showNumpad by remember { mutableStateOf(true) }
    var showOptionalRows by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // ── Runtime layout-mode override (session-only, not persisted) ────────
    // Starts as null → uses settings.landscapeLayoutMode.
    // When toggled, holds an override for THIS keyboard instance only.
    var layoutModeOverride by remember(settings.landscapeLayoutMode) {
        mutableStateOf<LandscapeLayoutMode?>(null)
    }
    val effectiveLayoutMode = layoutModeOverride ?: settings.landscapeLayoutMode

    fun handleKeyPress(key: LandscapeKey) {
        st = landscapeHandleKeyPress(key, st, settings, scope, onSendKey) { st = it }
    }

    fun handleNumpadKey(code: Int, label: String) {
        st = landscapeHandleNumpadKey(code, label, st, settings, scope, onSendKey) { st = it }
    }

    fun handleNumLock() {
        st = landscapeHandleNumLockToggle(st, scope, onSendKey)
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF080F18))) {
        LandscapeKbStatusBar(
            st = st, isReady = isReady,
            showFullStatus = settings.showStatusBar,
            showComboPreview = settings.showComboPreview,
            showKeyboard = showKeyboard,
            showNumpad = showNumpad,
            showOptionalRows = showOptionalRows,
            hasOptionalRows = true,
            currentTab = st.tab,
            onClearMods = { st = st.releaseMods(); onSendKey(0, emptyList()) },
            onShowSettings = onShowSettings,
            onToggleKeyboard = { showKeyboard = !showKeyboard },
            onToggleNumpad = { showNumpad = !showNumpad },
            onToggleOptionalRows = { showOptionalRows = !showOptionalRows },
            currentLayoutMode = effectiveLayoutMode,
            onToggleLayoutMode = {
                layoutModeOverride = if (effectiveLayoutMode == LandscapeLayoutMode.SINGLE_COLUMN)
                    LandscapeLayoutMode.TWO_COLUMN
                else
                    LandscapeLayoutMode.SINGLE_COLUMN
            },
        )

        TabRow(selectedTabIndex = st.tab, containerColor = Color(0xFF050C14), contentColor = Color.White) {
            listOf("Keys", "Nav+Num", "Media").forEachIndexed { i, title ->
                Tab(
                    selected = st.tab == i,
                    onClick = { st = st.copy(tab = i) },
                    text = { Text(title) },
                    selectedContentColor = Color(0xFF90CAF9),
                    unselectedContentColor = Color(0xFF546E7A)
                )
            }
        }

        when (st.tab) {
            0 -> LandscapeKeysTab(
                st = st,
                settings = settings,
                showKeyboard = showKeyboard,
                showOptionalRows = showOptionalRows,
                layoutMode = effectiveLayoutMode,   // NEW — forwards status-bar override
                onKeyPress = ::handleKeyPress,
                onClearMods = { st = st.releaseMods(); onSendKey(0, emptyList()) },
                onConsumerKey = onConsumerKey,
                onSettingsChange = onSettingsChange
            )
            1 -> LandscapeNavNumpadTab(
                st = st,
                settings = settings,
                showNumpad = showNumpad,
                onKeyPress = ::handleKeyPress,
                onNumpadKey = ::handleNumpadKey,
                onNumLock = ::handleNumLock,
                onClearMods = { st = st.releaseMods(); onSendKey(0, emptyList()) },
                onSettingsChange = onSettingsChange
            )
            2 -> LandscapeMediaTab(
                st = st,
                settings = settings,
                onKeyPress = ::handleKeyPress,
                onConsumerKey = onConsumerKey,
                onClearMods = { st = st.releaseMods(); onSendKey(0, emptyList()) },
                onSettingsChange = onSettingsChange
            )
        }
    }
}