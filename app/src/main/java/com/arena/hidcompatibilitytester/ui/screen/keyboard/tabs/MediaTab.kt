// ui/screen/keyboard/tabs/MediaTab.kt
package com.arena.hidcompatibilitytester.ui.screen.keyboard.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.ui.screen.keyboard.*
import com.arena.hidcompatibilitytester.ui.screen.keyboard.components.*

@Composable
internal fun MediaTab(
    st: KbState,
    settings: KeyboardSettings,
    onKeyPress: (Key) -> Unit,
    onConsumerKey: (Int) -> Unit,
    onClearMods: () -> Unit,
    onUpdateLastKey: (String) -> Unit,
) {
    val navH   = settings.keyHeight.navDp.dp
    val mediaH = settings.mediaKeySize.heightDp.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080F18))
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        KbCard("Transport") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MEDIA_TRANSPORT.forEach { mk ->
                    MediaKeyBtn(mk.icon, mk.label, Modifier.weight(1f), mediaH, settings) {
                        onConsumerKey(mk.code); onUpdateLastKey(mk.label)
                    }
                }
            }
        }

        KbCard("Volume & Brightness") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                (MEDIA_VOLUME + MEDIA_BRIGHT).forEach { mk ->
                    MediaKeyBtn(mk.icon, mk.label, Modifier.weight(1f), mediaH - 8.dp, settings) {
                        onConsumerKey(mk.code); onUpdateLastKey(mk.label)
                    }
                }
            }
        }

        KbCard("Arrow Keys") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(0.66f),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    KBtn(
                        key = KEY_UP, modifier = Modifier.weight(1f), h = navH,
                        settings = settings, mainLabel = "↑", scrollable = true,
                        onPress = { onKeyPress(KEY_UP) },
                    )
                }
                Row(Modifier.fillMaxWidth(0.66f)) {
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

        KbCard("System Keys") {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
        }

        KbCard("Quick Modifiers") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                QUICK_MODIFIERS.forEach { k ->
                    KBtn(
                        key = k, modifier = Modifier.weight(1f), h = navH - 2.dp,
                        settings = settings, active = isKeyActive(k, st),
                        mainLabel = k.label, scrollable = true,
                        onPress = { onKeyPress(k) },
                    )
                }
            }
            if (st.anyMod) {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onClearMods,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color(0xFFEF9A9A).copy(0.5f)),
                ) {
                    Text("✕ Clear All Modifiers", color = Color(0xFFEF9A9A), fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}