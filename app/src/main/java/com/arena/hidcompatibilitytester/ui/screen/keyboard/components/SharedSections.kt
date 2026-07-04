package com.arena.hidcompatibilitytester.ui.screen.keyboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.ui.screen.keyboard.*

// ═════════════════════════════════════════════════════════════════
// Navigation
// ═════════════════════════════════════════════════════════════════

@Composable
fun NavigationSection(
    st: KbState,
    settings: KeyboardSettings,
    style: SectionStyle,
    onKeyPress: (Key) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        SectionTitle("Navigation")
        NavigationSectionContent(st, settings, style, onKeyPress)
    }
}

@Composable
fun NavigationSectionContent(
    st: KbState,
    settings: KeyboardSettings,
    style: SectionStyle,
    onKeyPress: (Key) -> Unit,
) {
    val navH = settings.keyHeight.navDp.dp
    val gap = if (style == SectionStyle.MEDIA) 4.dp else 1.dp

    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            NAV_ROW1.forEach { k ->
                KBtn(
                    key = k, modifier = Modifier.weight(1f), h = navH,
                    settings = settings, active = isKeyActive(k, st),
                    mainLabel = displayMain(k, st), scrollable = true,
                    onPress = { onKeyPress(k) },
                )
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
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

// ═════════════════════════════════════════════════════════════════
// Arrow Keys
// ═════════════════════════════════════════════════════════════════

@Composable
fun ArrowKeysSection(
    st: KbState,
    settings: KeyboardSettings,
    style: SectionStyle,
    onKeyPress: (Key) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        SectionTitle("Arrow Keys")
        ArrowKeysSectionContent(st, settings, style, onKeyPress)
    }
}

@Composable
fun ArrowKeysSectionContent(
    st: KbState,
    settings: KeyboardSettings,
    style: SectionStyle,
    onKeyPress: (Key) -> Unit,
) {
    when (style) {
        SectionStyle.COMPACT -> CompactArrowKeysContent(settings, onKeyPress)
        SectionStyle.MEDIA -> MediaArrowKeysContent(settings, onKeyPress)
    }
}

@Composable
private fun CompactArrowKeysContent(
    settings: KeyboardSettings,
    onKeyPress: (Key) -> Unit,
) {
    val navH = settings.keyHeight.navDp.dp
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
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
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

@Composable
private fun MediaArrowKeysContent(
    settings: KeyboardSettings,
    onKeyPress: (Key) -> Unit,
) {
    val navH = settings.keyHeight.navDp.dp
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
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

// ═════════════════════════════════════════════════════════════════
// System Keys
// ═════════════════════════════════════════════════════════════════

@Composable
fun SystemKeysSection(
    st: KbState,
    settings: KeyboardSettings,
    style: SectionStyle,
    onKeyPress: (Key) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        SectionTitle("System Keys")
        SystemKeysSectionContent(st, settings, style, onKeyPress)
    }
}

@Composable
fun SystemKeysSectionContent(
    st: KbState,
    settings: KeyboardSettings,
    style: SectionStyle,
    onKeyPress: (Key) -> Unit,
) {
    val navH = settings.keyHeight.navDp.dp
    val gap = if (style == SectionStyle.MEDIA) 4.dp else 1.dp

    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            SYSTEM_ROW1.forEach { k ->
                KBtn(
                    key = k, modifier = Modifier.weight(1f), h = navH,
                    settings = settings, active = isKeyActive(k, st),
                    mainLabel = displayMain(k, st), scrollable = true,
                    onPress = { onKeyPress(k) },
                )
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
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

// ═════════════════════════════════════════════════════════════════
// Quick Modifiers
// ═════════════════════════════════════════════════════════════════

@Composable
fun QuickModsSection(
    st: KbState,
    settings: KeyboardSettings,
    style: SectionStyle,
    onKeyPress: (Key) -> Unit,
    onClearMods: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        SectionTitle("Quick Modifiers")
        QuickModsSectionContent(st, settings, style, onKeyPress, onClearMods)
    }
}

@Composable
fun QuickModsSectionContent(
    st: KbState,
    settings: KeyboardSettings,
    style: SectionStyle,
    onKeyPress: (Key) -> Unit,
    onClearMods: () -> Unit,
) {
    val navH = settings.keyHeight.navDp.dp

    when (style) {
        SectionStyle.COMPACT -> {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
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
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = onClearMods,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Text(
                                "Clear Modifiers", fontSize = 11.sp,
                                color = Color(0xFFEF9A9A),
                            )
                        }
                    }
                }
            }
        }
        SectionStyle.MEDIA -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    QUICK_MODIFIERS_WITH_MENU.forEach { k ->
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
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// Merged System Keys + Quick Modifiers
// ═════════════════════════════════════════════════════════════════

@Composable
fun MergedSystemModsSection(
    st: KbState,
    settings: KeyboardSettings,
    style: SectionStyle,
    onKeyPress: (Key) -> Unit,
    onClearMods: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        SectionTitle("System & Modifiers")
        MergedSystemModsSectionContent(st, settings, style, onKeyPress, onClearMods)
    }
}

@Composable
fun MergedSystemModsSectionContent(
    st: KbState,
    settings: KeyboardSettings,
    style: SectionStyle,
    onKeyPress: (Key) -> Unit,
    onClearMods: () -> Unit,
) {
    val navH = settings.keyHeight.navDp.dp
    val gap = if (style == SectionStyle.MEDIA) 4.dp else 1.dp

    when (style) {
        SectionStyle.COMPACT -> {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    SYSTEM_ROW1.forEach { k ->
                        KBtn(
                            key = k, modifier = Modifier.weight(1f), h = navH,
                            settings = settings, active = isKeyActive(k, st),
                            mainLabel = displayMain(k, st), scrollable = true,
                            onPress = { onKeyPress(k) },
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    SYSTEM_ROW2.forEach { k ->
                        KBtn(
                            key = k, modifier = Modifier.weight(1f), h = navH,
                            settings = settings, active = isKeyActive(k, st),
                            mainLabel = displayMain(k, st), scrollable = true,
                            onPress = { onKeyPress(k) },
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    QUICK_MODIFIERS.forEach { k ->
                        KBtn(
                            key = k, modifier = Modifier.weight(1f), h = navH,
                            settings = settings, active = isKeyActive(k, st),
                            mainLabel = k.label, scrollable = true,
                            onPress = { onKeyPress(k) },
                        )
                    }
                }
                if (st.anyMod) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = onClearMods,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Text(
                                "Clear Modifiers", fontSize = 11.sp,
                                color = Color(0xFFEF9A9A),
                            )
                        }
                    }
                }
            }
        }
        SectionStyle.MEDIA -> {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    SYSTEM_ROW1.forEach { k ->
                        KBtn(
                            key = k, modifier = Modifier.weight(1f), h = navH,
                            settings = settings, active = isKeyActive(k, st),
                            mainLabel = displayMain(k, st), scrollable = true,
                            onPress = { onKeyPress(k) },
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    SYSTEM_ROW2.forEach { k ->
                        KBtn(
                            key = k, modifier = Modifier.weight(1f), h = navH,
                            settings = settings, active = isKeyActive(k, st),
                            mainLabel = displayMain(k, st), scrollable = true,
                            onPress = { onKeyPress(k) },
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
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
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// Shared helpers
// ═════════════════════════════════════════════════════════════════

@Composable
private fun SectionTitle(title: String) {
    Text(
        title, color = Color(0xFF607D8B),
        fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
    )
}