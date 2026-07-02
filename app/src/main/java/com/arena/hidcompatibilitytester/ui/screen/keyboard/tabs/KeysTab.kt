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
    val rowH = settings.keyHeight.mainDp.dp
    val fnH  = settings.keyHeight.fnDp.dp
    // fn row + divider + 5 main rows + internal spacing + outer padding
    val fixedKbHeight = fnH + 1.dp + (rowH + 2.dp) * 5 + rowH + 12.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080F18)),
    ) {
        // Scrollable upper content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = fixedKbHeight)
                .verticalScroll(rememberScrollState())
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
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
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "Arrow Keys", color = Color(0xFF607D8B),
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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

            Spacer(Modifier.height(6.dp))

            Text(
                "Quick Modifiers", color = Color(0xFF607D8B),
                fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
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
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = onClearMods,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        "Clear Modifiers", fontSize = 11.sp,
                        color = Color(0xFFEF9A9A),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // Fixed keyboard at bottom — SharedCompactKeyboard, no dismiss bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xFF080F18))
                .padding(vertical = 4.dp),
        ) {
            SharedCompactKeyboard(
                settings       = settings,
                showDismissBar = false,
                onSendKey      = onSendKey,
                onReleaseKeys  = onReleaseKeys,
                onConsumerKey  = onConsumerKey,
                onTypeText     = onTypeText,
            )
        }
    }
}