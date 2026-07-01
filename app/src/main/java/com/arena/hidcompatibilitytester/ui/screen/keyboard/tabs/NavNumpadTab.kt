// ui/screen/keyboard/tabs/NavNumpadTab.kt
package com.arena.hidcompatibilitytester.ui.screen.keyboard.tabs

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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.arena.hidcompatibilitytester.ui.screen.keyboard.*
import com.arena.hidcompatibilitytester.ui.screen.keyboard.components.*

@Composable
internal fun NavNumpadTab(
    st: KbState,
    settings: KeyboardSettings,
    typeText: String,
    onTypeTextChanged: (String) -> Unit,
    onKeyPress: (Key) -> Unit,
    onNumpadKey: (Int, String) -> Unit,
    onNumLock: () -> Unit,
    onInsertToggle: () -> Unit,
    onClearMods: () -> Unit,
    onSendKey: (Int, List<Int>) -> Unit,
    onTypeText: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val navH  = settings.keyHeight.navDp.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080F18))
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Nav cluster + Arrows
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KbCard("Navigation", modifier = Modifier.weight(1f)) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        NAV_ROW1.forEach { k ->
                            KBtn(
                                key = k, modifier = Modifier.weight(1f), h = navH,
                                settings = settings, active = isKeyActive(k, st),
                                mainLabel = displayMain(k, st), scrollable = true,
                                onPress = { onKeyPress(k) },
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        NAV_ROW2.forEach { k ->
                            KBtn(
                                key = k, modifier = Modifier.weight(1f), h = navH,
                                settings = settings, active = isKeyActive(k, st),
                                mainLabel = displayMain(k, st), scrollable = true,
                                onPress = { onKeyPress(k) },
                            )
                        }
                    }
                }
            }
            KbCard("Arrows", modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        KBtn(
                            key = KEY_UP, modifier = Modifier.weight(1f), h = navH,
                            settings = settings, mainLabel = "↑", scrollable = true,
                            onPress = { onKeyPress(KEY_UP) },
                        )
                    }
                    Row(Modifier.fillMaxWidth()) {
                        listOf(KEY_LEFT, KEY_DOWN, KEY_RIGHT).forEach { k ->
                            KBtn(
                                key = k, modifier = Modifier.weight(1f), h = navH,
                                settings = settings, mainLabel = k.label, scrollable = true,
                                onPress = { onKeyPress(k) },
                            )
                        }
                    }
                }
            }
        }

        // Insert mode toggle
        Surface(
            color = if (st.insertMode) Color(0xFF0D1F0D) else Color(0xFF2A1800),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onInsertToggle() },
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    if (st.insertMode) "INSERT" else "OVERWRITE",
                    color = if (st.insertMode) Color(0xFF81C784) else Color(0xFFFFB74D),
                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                )
                Text(
                    if (st.insertMode) "Tap to switch → Overwrite mode"
                    else "Tap to switch → Insert mode",
                    color = Color.Gray, fontSize = 11.sp,
                )
            }
        }

        // Numpad
        KbCard("Numpad") {
            Surface(
                color = if (st.numLock) Color(0xFF1565C0) else Color(0xFF4A1800),
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (st.numLock) "NumLock ON — typing numbers"
                    else "NumLock OFF — navigation mode",
                    color = Color.White.copy(0.85f), fontSize = 10.sp,
                    modifier = Modifier.padding(8.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            NumpadLayout(
                numLock = st.numLock,
                shift = st.shift,
                settings = settings,
                onNumLock = onNumLock,
                onKey = onNumpadKey,
            )
        }

        // Type & Send Text
        val bringIntoViewRequester = remember { BringIntoViewRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        KbCard("Type & Send Text") {
            OutlinedTextField(
                value = typeText,
                onValueChange = { newText ->
                    when {
                        newText.length > typeText.length -> {
                            val added = newText.substring(typeText.length)
                            if (added.isNotEmpty()) onTypeText(added)
                        }
                        newText.length < typeText.length -> {
                            repeat(typeText.length - newText.length) {
                                onSendKey(0, listOf(0x2A))
                            }
                        }
                    }
                    onTypeTextChanged(newText)
                },
                modifier = Modifier
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
                placeholder = { Text("Type here…", color = Color.Gray) },
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4A90D9),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    cursorColor = Color(0xFF4A90D9),
                ),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onTypeTextChanged("") },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            ) {
                Text("Clear", color = Color.White)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}