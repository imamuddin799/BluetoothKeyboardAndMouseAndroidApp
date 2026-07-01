package com.arena.hidcompatibilitytester.ui.screen.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.arena.hidcompatibilitytester.ui.screen.keyboard.components.KbStatusBar
import com.arena.hidcompatibilitytester.ui.screen.keyboard.tabs.KeysTab
import com.arena.hidcompatibilitytester.ui.screen.keyboard.tabs.MediaTab
import com.arena.hidcompatibilitytester.ui.screen.keyboard.tabs.NavNumpadTab

@Composable
fun KeyboardScreen(
    isReady: Boolean,
    settings: KeyboardSettings,
    onSendKey: (modifiers: Int, keyCodes: List<Int>) -> Unit,
    onConsumerKey: (Int) -> Unit,
    onTypeText: (String) -> Unit,
    onShowSettings: () -> Unit,
) {
    var st by remember(settings.numpadStartsLocked, settings.defaultTab) {
        mutableStateOf(
            KbState(
                numLock = settings.numpadStartsLocked,
                tab = settings.defaultTab,
            )
        )
    }
    var typeText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun handleKey(key: Key) {
        st = handleKeyPress(key, st, settings, scope, onSendKey)
    }

    fun handleNumCode(code: Int, label: String) {
        st = handleNumpadKey(code, label, st, settings, scope, onSendKey)
    }

    fun doHandleNumLockToggle() {
        st = handleNumLockToggle(st, scope, onSendKey)
    }

    fun handleInsertToggle() {
        st = st.copy(insertMode = !st.insertMode)
        onSendKey(0, listOf(0x49))
        scope.launch { delay(60); onSendKey(0, emptyList()) }
    }

    fun clearMods() {
        st = st.releaseMods().copy(lastKey = "")
        onSendKey(0, emptyList())
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF080F18))) {

        val tabLabels = listOf("⌨ Keys", "↕ Nav+Num", "🎵 Media")
        TabRow(
            selectedTabIndex = st.tab,
            containerColor = Color(0xFF050C14),
            contentColor = Color.White,
            indicator = { tabPositions ->
                if (st.tab < tabPositions.size) {
                    val t = tabPositions[st.tab]
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .wrapContentSize(Alignment.BottomStart)
                            .offset(x = t.left)
                            .width(t.width)
                            .height(2.dp)
                            .background(Color(0xFF4A90D9), RoundedCornerShape(1.dp))
                    )
                }
            }
        ) {
            tabLabels.forEachIndexed { i, title ->
                Tab(
                    selected = st.tab == i,
                    onClick = { st = st.copy(tab = i) },
                    text = {
                        Text(
                            title,
                            fontSize = 12.sp,
                            color = if (st.tab == i) Color(0xFF90CAF9) else Color(0xFF546E7A),
                            fontWeight = if (st.tab == i) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        // Status bar always shown — Ready + ⚙ always visible
        // LEDs + combo bar only when showStatusBar = true
        KbStatusBar(
            st = st,
            isReady = isReady,
            showFullStatus = settings.showStatusBar,
            onClearMods = { clearMods() },
            onShowSettings = onShowSettings,
        )

        if (!isReady) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF080F18)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("⏳", fontSize = 36.sp)
                    Text(
                        "Host not connected",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Go to Status tab → Start BLE HID\nPair from host Bluetooth settings",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            return@Column
        }

        when (st.tab) {
            0 -> KeysTab(
                st = st,
                settings = settings,
                onKeyPress = { handleKey(it) },
                onClearMods = { clearMods() },
            )

            1 -> NavNumpadTab(
                st = st,
                settings = settings,
                typeText = typeText,
                onTypeTextChanged = { typeText = it },
                onKeyPress = { handleKey(it) },
                onNumpadKey = { code, label -> handleNumCode(code, label) },
                onNumLock = { doHandleNumLockToggle() },
                onInsertToggle = { handleInsertToggle() },
                onClearMods = { clearMods() },
                onSendKey = onSendKey,
                onTypeText = onTypeText,
            )

            2 -> MediaTab(
                st = st,
                settings = settings,
                onKeyPress = { handleKey(it) },
                onConsumerKey = onConsumerKey,
                onClearMods = { clearMods() },
                onUpdateLastKey = { st = st.copy(lastKey = it) },
            )
        }
    }
}