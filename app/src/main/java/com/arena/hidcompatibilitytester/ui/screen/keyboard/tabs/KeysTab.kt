package com.arena.hidcompatibilitytester.ui.screen.keyboard.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.ui.screen.keyboard.*
import com.arena.hidcompatibilitytester.ui.screen.keyboard.components.KBtn

@Composable
internal fun KeysTab(
    st: KbState,
    settings: KeyboardSettings,
    onKeyPress: (Key) -> Unit,
    onClearMods: () -> Unit,
    onSendKey: (Int, List<Int>) -> Unit,
    onReleaseKeys: () -> Unit,
    onConsumerKey: (Int) -> Unit,
    onTypeText: (String) -> Unit,
) {
    val navH = settings.keyHeight.navDp.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080F18)),
    ) {
        // Scrollable upper content — takes remaining space
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        "Navigation", color = Color(0xFF607D8B),
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    )
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

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        "Arrow Keys", color = Color(0xFF607D8B),
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp),
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

            Text(
                "System Keys", color = Color(0xFF607D8B),
                fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    SYSTEM_ROW1.forEach { k ->
                        KBtn(
                            key = k, modifier = Modifier.weight(1f), h = navH,
                            settings = settings, active = isKeyActive(k, st),
                            mainLabel = displayMain(k, st), scrollable = true,
                            onPress = { onKeyPress(k) },
                        )
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    SYSTEM_ROW2.forEach { k ->
                        KBtn(
                            key = k, modifier = Modifier.weight(1f), h = navH,
                            settings = settings, active = isKeyActive(k, st),
                            mainLabel = displayMain(k, st), scrollable = true,
                            onPress = { onKeyPress(k) },
                        )
                    }
                }
            }

            Text(
                "Quick Modifiers", color = Color(0xFF607D8B),
                fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                QUICK_MODIFIERS_WITH_MENU.forEach { k ->
                    KBtn(
                        key = k, modifier = Modifier.weight(1f), h = navH,
                        settings = settings, active = isKeyActive(k, st),
                        mainLabel = k.label, scrollable = true,
                        onPress = { onKeyPress(k) },
                    )
                }
            }

            if (st.anyMod) {
                TextButton(
                    onClick = onClearMods,
                    modifier = Modifier.align(Alignment.End),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text(
                        "Clear Modifiers", fontSize = 11.sp,
                        color = Color(0xFFEF9A9A),
                    )
                }
            }
        }

        // Fixed keyboard at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp, bottom = 6.dp)
        ) {
            SharedCompactKeyboard(
                st           = st,
                settings     = settings,
                showDismissBar = false,
                showMediaRow = settings.showMediaRowInKeyboard,
                onKeyPress   = onKeyPress,
                onConsumerKey = onConsumerKey,
            )
        }
    }
}