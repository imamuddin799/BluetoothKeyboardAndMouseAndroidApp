package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.components

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.*

// ═══════════════════════════════════════════════════════════════
// Navigation
// ═══════════════════════════════════════════════════════════════

@Composable
fun LandscapeNavigationSection(st: LandscapeKbState, settings: LandscapeKeyboardSettings, style: LandscapeSectionStyle, onKeyPress: (LandscapeKey) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        LandscapeSectionTitle("Navigation")
        LandscapeNavigationSectionContent(st, settings, style, onKeyPress)
    }
}

@Composable
fun LandscapeNavigationSectionContent(st: LandscapeKbState, settings: LandscapeKeyboardSettings, style: LandscapeSectionStyle, onKeyPress: (LandscapeKey) -> Unit) {
    val navH = settings.keyHeight.navDp.dp
    val gap = if (style == LandscapeSectionStyle.MEDIA) 4.dp else 1.dp
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            LANDSCAPE_NAV_ROW1.forEach { k ->
                LandscapeKBtn(key = k, modifier = Modifier.weight(1f), h = navH, settings = settings, active = landscapeIsKeyActive(k, st), mainLabel = landscapeDisplayMain(k, st), scrollable = true, onPress = { onKeyPress(k) })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            LANDSCAPE_NAV_ROW2.forEach { k ->
                LandscapeKBtn(key = k, modifier = Modifier.weight(1f), h = navH, settings = settings, active = landscapeIsKeyActive(k, st), mainLabel = landscapeDisplayMain(k, st), scrollable = true, onPress = { onKeyPress(k) })
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Arrow Keys
// ═══════════════════════════════════════════════════════════════

@Composable
fun LandscapeArrowKeysSection(st: LandscapeKbState, settings: LandscapeKeyboardSettings, style: LandscapeSectionStyle, onKeyPress: (LandscapeKey) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        LandscapeSectionTitle("Arrow Keys")
        LandscapeArrowKeysSectionContent(st, settings, style, onKeyPress)
    }
}

@Composable
fun LandscapeArrowKeysSectionContent(st: LandscapeKbState, settings: LandscapeKeyboardSettings, style: LandscapeSectionStyle, onKeyPress: (LandscapeKey) -> Unit) {
    val navH = settings.keyHeight.navDp.dp
    val gap = if (style == LandscapeSectionStyle.MEDIA) 4.dp else 1.dp
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            LandscapeKBtn(key = LANDSCAPE_KEY_UP, modifier = Modifier.weight(1f), h = navH, settings = settings, mainLabel = "↑", scrollable = true, onPress = { onKeyPress(LANDSCAPE_KEY_UP) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            listOf(LANDSCAPE_KEY_LEFT, LANDSCAPE_KEY_DOWN, LANDSCAPE_KEY_RIGHT).forEach { k ->
                LandscapeKBtn(key = k, modifier = Modifier.weight(1f), h = navH, settings = settings, mainLabel = k.label, scrollable = true, onPress = { onKeyPress(k) })
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// System Keys
// ═══════════════════════════════════════════════════════════════

@Composable
fun LandscapeSystemKeysSection(st: LandscapeKbState, settings: LandscapeKeyboardSettings, style: LandscapeSectionStyle, onKeyPress: (LandscapeKey) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        LandscapeSectionTitle("System Keys")
        LandscapeSystemKeysSectionContent(st, settings, style, onKeyPress)
    }
}

@Composable
fun LandscapeSystemKeysSectionContent(st: LandscapeKbState, settings: LandscapeKeyboardSettings, style: LandscapeSectionStyle, onKeyPress: (LandscapeKey) -> Unit) {
    val navH = settings.keyHeight.navDp.dp
    val gap = if (style == LandscapeSectionStyle.MEDIA) 4.dp else 1.dp
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            LANDSCAPE_SYSTEM_ROW1.forEach { k ->
                LandscapeKBtn(key = k, modifier = Modifier.weight(1f), h = navH, settings = settings, active = landscapeIsKeyActive(k, st), mainLabel = landscapeDisplayMain(k, st), scrollable = true, onPress = { onKeyPress(k) })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            LANDSCAPE_SYSTEM_ROW2.forEach { k ->
                LandscapeKBtn(key = k, modifier = Modifier.weight(1f), h = navH, settings = settings, active = landscapeIsKeyActive(k, st), mainLabel = landscapeDisplayMain(k, st), scrollable = true, onPress = { onKeyPress(k) })
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Quick Modifiers
// ═══════════════════════════════════════════════════════════════

@Composable
fun LandscapeQuickModsSection(st: LandscapeKbState, settings: LandscapeKeyboardSettings, style: LandscapeSectionStyle, onKeyPress: (LandscapeKey) -> Unit, onClearMods: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        LandscapeSectionTitle("Quick Modifiers")
        LandscapeQuickModsSectionContent(st, settings, style, onKeyPress, onClearMods)
    }
}

@Composable
fun LandscapeQuickModsSectionContent(st: LandscapeKbState, settings: LandscapeKeyboardSettings, style: LandscapeSectionStyle, onKeyPress: (LandscapeKey) -> Unit, onClearMods: () -> Unit) {
    val navH = settings.keyHeight.navDp.dp
    when (style) {
        LandscapeSectionStyle.COMPACT -> {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    LANDSCAPE_QUICK_MODIFIERS_WITH_MENU.forEach { k ->
                        LandscapeKBtn(key = k, modifier = Modifier.weight(1f), h = navH, settings = settings, active = landscapeIsKeyActive(k, st), mainLabel = k.label, scrollable = true, onPress = { onKeyPress(k) })
                    }
                }
                if (st.anyMod) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onClearMods, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                            Text("Clear Modifiers", fontSize = 11.sp, color = Color(0xFFEF9A9A))
                        }
                    }
                }
            }
        }
        LandscapeSectionStyle.MEDIA -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LANDSCAPE_QUICK_MODIFIERS_WITH_MENU.forEach { k ->
                        LandscapeKBtn(key = k, modifier = Modifier.weight(1f), h = navH - 2.dp, settings = settings, active = landscapeIsKeyActive(k, st), mainLabel = k.label, scrollable = true, onPress = { onKeyPress(k) })
                    }
                }
                if (st.anyMod) {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(onClick = onClearMods, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Color(0xFFEF9A9A).copy(0.5f))) {
                        Text("✕ Clear All Modifiers", color = Color(0xFFEF9A9A), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Merged System Keys + Quick Modifiers
// ═══════════════════════════════════════════════════════════════

@Composable
fun LandscapeMergedSystemModsSection(st: LandscapeKbState, settings: LandscapeKeyboardSettings, style: LandscapeSectionStyle, onKeyPress: (LandscapeKey) -> Unit, onClearMods: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        LandscapeSectionTitle("System & Modifiers")
        LandscapeMergedSystemModsSectionContent(st, settings, style, onKeyPress, onClearMods)
    }
}

@Composable
fun LandscapeMergedSystemModsSectionContent(st: LandscapeKbState, settings: LandscapeKeyboardSettings, style: LandscapeSectionStyle, onKeyPress: (LandscapeKey) -> Unit, onClearMods: () -> Unit) {
    val navH = settings.keyHeight.navDp.dp
    val gap = if (style == LandscapeSectionStyle.MEDIA) 4.dp else 1.dp
    when (style) {
        LandscapeSectionStyle.COMPACT -> {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    LANDSCAPE_SYSTEM_ROW1.forEach { k -> LandscapeKBtn(key = k, modifier = Modifier.weight(1f), h = navH, settings = settings, active = landscapeIsKeyActive(k, st), mainLabel = landscapeDisplayMain(k, st), scrollable = true, onPress = { onKeyPress(k) }) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    LANDSCAPE_SYSTEM_ROW2.forEach { k -> LandscapeKBtn(key = k, modifier = Modifier.weight(1f), h = navH, settings = settings, active = landscapeIsKeyActive(k, st), mainLabel = landscapeDisplayMain(k, st), scrollable = true, onPress = { onKeyPress(k) }) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    LANDSCAPE_QUICK_MODIFIERS.forEach { k -> LandscapeKBtn(key = k, modifier = Modifier.weight(1f), h = navH, settings = settings, active = landscapeIsKeyActive(k, st), mainLabel = k.label, scrollable = true, onPress = { onKeyPress(k) }) }
                }
                if (st.anyMod) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onClearMods, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                            Text("Clear Modifiers", fontSize = 11.sp, color = Color(0xFFEF9A9A))
                        }
                    }
                }
            }
        }
        LandscapeSectionStyle.MEDIA -> {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    LANDSCAPE_SYSTEM_ROW1.forEach { k -> LandscapeKBtn(key = k, modifier = Modifier.weight(1f), h = navH, settings = settings, active = landscapeIsKeyActive(k, st), mainLabel = landscapeDisplayMain(k, st), scrollable = true, onPress = { onKeyPress(k) }) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    LANDSCAPE_SYSTEM_ROW2.forEach { k -> LandscapeKBtn(key = k, modifier = Modifier.weight(1f), h = navH, settings = settings, active = landscapeIsKeyActive(k, st), mainLabel = landscapeDisplayMain(k, st), scrollable = true, onPress = { onKeyPress(k) }) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    LANDSCAPE_QUICK_MODIFIERS.forEach { k -> LandscapeKBtn(key = k, modifier = Modifier.weight(1f), h = navH - 2.dp, settings = settings, active = landscapeIsKeyActive(k, st), mainLabel = k.label, scrollable = true, onPress = { onKeyPress(k) }) }
                }
                if (st.anyMod) {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(onClick = onClearMods, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Color(0xFFEF9A9A).copy(0.5f))) {
                        Text("✕ Clear All Modifiers", color = Color(0xFFEF9A9A), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Shared helpers
// ═══════════════════════════════════════════════════════════════

@Composable
private fun LandscapeSectionTitle(title: String) {
    Text(title, color = Color(0xFF607D8B), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
}