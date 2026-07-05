package com.arena.hidcompatibilitytester.ui.screen.keyboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.ui.screen.keyboard.components.KBtn

@Composable
fun SharedCompactKeyboard(
    st: KbState,
    settings: KeyboardSettings,
    showDismissBar: Boolean = false,
    showMediaRow: Boolean = false,
    showNavRow: Boolean = false,
    showOptionalRows: Boolean = true,
    showComboPreview: Boolean = false,
    optionalRowOrder: List<KeyboardOptionalRow> = listOf(
        KeyboardOptionalRow.MEDIA_ROW,
        KeyboardOptionalRow.NAV_ROW,
    ),
    onKeyPress: (Key) -> Unit,
    onConsumerKey: ((Int) -> Unit)? = null,
    onClearMods: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    fun isActive(k: Key): Boolean = isKeyActive(k, st)
    fun mainLabel(k: Key): String = displayMain(k, st)
    fun topLabel(k: Key): String = displayTop(k, st, settings.showKeyHints)

    fun handleKeyClick(k: Key) {
        if (k.isConsumer && onConsumerKey != null) {
            onConsumerKey(k.consumerCode)
        } else {
            onKeyPress(k)
        }
    }

    val hasVisibleOptionalRows =
        (showMediaRow && buildMediaRow(settings).isNotEmpty()) || showNavRow

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF080F18))
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        if (showDismissBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF050C14))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Keyboard",
                    fontSize = 10.sp,
                    color = Color(0xFF607D8B),
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (st.caps) SharedMiniLed("CAP")
                    if (st.shift) SharedMiniLed("SHF")
                    if (st.ctrl) SharedMiniLed("CTL")
                    if (st.alt) SharedMiniLed("ALT")
                    if (st.gui) SharedMiniLed("WIN")
                }
                IconButton(
                    onClick = { onDismiss?.invoke() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("✕", fontSize = 12.sp, color = Color(0xFFEF9A9A))
                }
            }
            HorizontalDivider(color = Color.White.copy(0.04f), thickness = 1.dp)
        }

        if (showComboPreview && (st.anyMod || st.lastKey.isNotEmpty())) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF050C14))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val combo = st.modPrefix() + st.lastKey
                Surface(
                    color = Color(0xFF0A1828),
                    shape = RoundedCornerShape(5.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = when {
                            st.anyMod && st.lastKey.isEmpty() ->
                                "▶ ${st.modPrefix().trimEnd('+')}+ …waiting"
                            combo.isEmpty() -> "Ready…"
                            else -> "⌨ $combo"
                        },
                        fontSize = 11.sp,
                        color = if (combo.isEmpty()) Color(0xFF546E7A)
                        else Color(0xFF90CAF9),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
                if (st.anyMod && onClearMods != null) {
                    TextButton(
                        onClick = onClearMods,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text("✕ Clear", fontSize = 10.sp, color = Color(0xFFEF9A9A), maxLines = 1)
                    }
                }
            }
            HorizontalDivider(color = Color.White.copy(0.04f), thickness = 1.dp)
        }

        val rowH = settings.keyHeight.mainDp.dp
        val fnH = settings.keyHeight.fnDp.dp

        AnimatedVisibility(
            visible = showOptionalRows && hasVisibleOptionalRows,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                optionalRowOrder.forEach { optionalRow ->
                    when (optionalRow) {
                        KeyboardOptionalRow.MEDIA_ROW -> {
                            if (showMediaRow) {
                                val mediaKeys = buildMediaRow(settings)
                                if (mediaKeys.isNotEmpty()) {
                                    SharedStyledKeyRow(
                                        mediaKeys, fnH, settings,
                                        ::isActive, ::mainLabel, ::topLabel
                                    ) { handleKeyClick(it) }
                                    HorizontalDivider(
                                        color = Color.White.copy(0.04f),
                                        thickness = 1.dp
                                    )
                                }
                            }
                        }

                        KeyboardOptionalRow.NAV_ROW -> {
                            if (showNavRow) {
                                SharedStyledKeyRow(
                                    SHARED_ROW_NAV, fnH, settings,
                                    ::isActive, ::mainLabel, ::topLabel
                                ) { handleKeyClick(it) }
                                HorizontalDivider(
                                    color = Color.White.copy(0.04f),
                                    thickness = 1.dp
                                )
                            }
                        }
                    }
                }
            }
        }

        SharedStyledKeyRow(SHARED_ROW_FN, fnH, settings, ::isActive, ::mainLabel, ::topLabel) {
            handleKeyClick(it)
        }
        HorizontalDivider(color = Color.White.copy(0.04f), thickness = 1.dp)
        SharedStyledKeyRow(SHARED_ROW_NUM, rowH, settings, ::isActive, ::mainLabel, ::topLabel) {
            handleKeyClick(it)
        }
        SharedStyledKeyRow(SHARED_ROW_QWERTY, rowH, settings, ::isActive, ::mainLabel, ::topLabel) {
            handleKeyClick(it)
        }
        SharedStyledKeyRow(SHARED_ROW_HOME, rowH, settings, ::isActive, ::mainLabel, ::topLabel) {
            handleKeyClick(it)
        }
        SharedStyledKeyRow(SHARED_ROW_ALPHA, rowH, settings, ::isActive, ::mainLabel, ::topLabel) {
            handleKeyClick(it)
        }
        SharedStyledKeyRow(
            if (settings.compactModifiers) SHARED_ROW_MODS_COMPACT else SHARED_ROW_MODS,
            rowH, settings, ::isActive, ::mainLabel, ::topLabel
        ) {
            handleKeyClick(it)
        }
    }
}

@Composable
private fun SharedMiniLed(label: String) {
    Text(
        label, fontSize = 6.sp, color = Color(0xFF90CAF9),
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(
                Color(0xFF1565C0).copy(0.3f),
                RoundedCornerShape(2.dp),
            )
            .padding(horizontal = 3.dp, vertical = 1.dp),
    )
}

@Composable
private fun SharedStyledKeyRow(
    keys: List<Key>,
    height: Dp,
    settings: KeyboardSettings,
    isActive: (Key) -> Boolean,
    mainLabel: (Key) -> String,
    topLabel: (Key) -> String,
    onClick: (Key) -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        keys.forEach { k ->
            KBtn(
                key = k,
                modifier = Modifier.weight(k.w),
                h = height,
                settings = settings,
                active = isActive(k),
                topLabel = topLabel(k),
                mainLabel = mainLabel(k),
                scrollable = false,
                onPress = { onClick(k) },
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Media row — per-group key definitions
// ═══════════════════════════════════════════════════════════════

private val MEDIA_KEYS_TRANSPORT = listOf(
    Key("⏮", consumerCode = 0xB6, color = KC.MEDIA, isConsumer = true, noRepeat = true, mediaGroup = MediaRowGroup.TRANSPORT),
    Key("⏯", consumerCode = 0xCD, color = KC.MEDIA, isConsumer = true, noRepeat = true, mediaGroup = MediaRowGroup.TRANSPORT),
    Key("⏹", consumerCode = 0xB7, color = KC.MEDIA, isConsumer = true, noRepeat = true, mediaGroup = MediaRowGroup.TRANSPORT),
    Key("⏭", consumerCode = 0xB5, color = KC.MEDIA, isConsumer = true, noRepeat = true, mediaGroup = MediaRowGroup.TRANSPORT),
)

private val MEDIA_KEYS_VOLUME = listOf(
    Key("🔇", consumerCode = 0xE2, color = KC.MEDIA, isConsumer = true, noRepeat = false, mediaGroup = MediaRowGroup.VOLUME),
    Key("🔉", consumerCode = 0xEA, color = KC.MEDIA, isConsumer = true, noRepeat = false, mediaGroup = MediaRowGroup.VOLUME),
    Key("🔊", consumerCode = 0xE9, color = KC.MEDIA, isConsumer = true, noRepeat = false, mediaGroup = MediaRowGroup.VOLUME),
)

private val MEDIA_KEYS_BRIGHTNESS = listOf(
    Key("🔅", consumerCode = 0x0070, color = KC.MEDIA, isConsumer = true, noRepeat = false, mediaGroup = MediaRowGroup.BRIGHTNESS),
    Key("🔆", consumerCode = 0x006F, color = KC.MEDIA, isConsumer = true, noRepeat = false, mediaGroup = MediaRowGroup.BRIGHTNESS),
)

private fun buildMediaRow(settings: KeyboardSettings): List<Key> {
    val groupKeys = mapOf(
        MediaRowGroup.TRANSPORT to MEDIA_KEYS_TRANSPORT,
        MediaRowGroup.VOLUME to MEDIA_KEYS_VOLUME,
        MediaRowGroup.BRIGHTNESS to MEDIA_KEYS_BRIGHTNESS,
    )
    val visibleGroups = settings.mediaRowGroupOrder.filter { group ->
        when (group) {
            MediaRowGroup.TRANSPORT -> settings.mediaRowShowTransport
            MediaRowGroup.VOLUME -> settings.mediaRowShowVolume
            MediaRowGroup.BRIGHTNESS -> settings.mediaRowShowBrightness
        }
    }
    val allKeys = visibleGroups.flatMap { groupKeys[it] ?: emptyList() }
    if (allKeys.isEmpty()) return emptyList()

    return allKeys.map { it.copy(w = 1f) }
}

// ═══════════════════════════════════════════════════════════════
// Navigation quick row
// ═══════════════════════════════════════════════════════════════

private val SHARED_ROW_NAV = listOf(
    Key("PgUp", code = 0x4B, color = KC.SPECIAL),
    Key("PgDn", code = 0x4E, color = KC.SPECIAL),
    Key("Ins", code = 0x49, color = KC.SPECIAL),
    Key("Home", code = 0x4A, color = KC.SPECIAL),
    Key("End", code = 0x4D, color = KC.SPECIAL),
    Key("←", code = 0x50, color = KC.SPECIAL),
    Key("↑", code = 0x52, color = KC.SPECIAL),
    Key("↓", code = 0x51, color = KC.SPECIAL),
    Key("→", code = 0x4F, color = KC.SPECIAL),
)

// ═══════════════════════════════════════════════════════════════
// Key row definitions
// ═══════════════════════════════════════════════════════════════

private val SHARED_ROW_FN = listOf(
    Key("Esc", code = 0x29, w = 1.4f, color = KC.DANGER, noRepeat = true),
    Key("F1", code = 0x3A, color = KC.SPECIAL),
    Key("F2", code = 0x3B, color = KC.SPECIAL),
    Key("F3", code = 0x3C, color = KC.SPECIAL),
    Key("F4", code = 0x3D, color = KC.SPECIAL),
    Key("F5", code = 0x3E, color = KC.SPECIAL),
    Key("F6", code = 0x3F, color = KC.SPECIAL),
    Key("F7", code = 0x40, color = KC.SPECIAL),
    Key("F8", code = 0x41, color = KC.SPECIAL),
    Key("F9", code = 0x42, color = KC.SPECIAL),
    Key("F10", code = 0x43, color = KC.SPECIAL),
    Key("F11", code = 0x44, color = KC.SPECIAL),
    Key("F12", code = 0x45, color = KC.SPECIAL),
    Key("Del", code = 0x4C, w = 1.4f, color = KC.DANGER),
)

private val SHARED_ROW_NUM = listOf(
    Key("`", "~", code = 0x35), Key("1", "!", code = 0x1E), Key("2", "@", code = 0x1F),
    Key("3", "#", code = 0x20), Key("4", "$", code = 0x21), Key("5", "%", code = 0x22),
    Key("6", "^", code = 0x23), Key("7", "&", code = 0x24), Key("8", "*", code = 0x25),
    Key("9", "(", code = 0x26), Key("0", ")", code = 0x27), Key("-", "_", code = 0x2D),
    Key("=", "+", code = 0x2E), Key("⌫", "", code = 0x2A, w = 2.0f, color = KC.DANGER),
)

private val SHARED_ROW_QWERTY = listOf(
    Key("Tab", code = 0x2B, w = 1.5f, color = KC.MOD),
    Key("Q", "Q", code = 0x14), Key("W", "W", code = 0x1A), Key("E", "E", code = 0x08),
    Key("R", "R", code = 0x15), Key("T", "T", code = 0x17), Key("Y", "Y", code = 0x1C),
    Key("U", "U", code = 0x18), Key("I", "I", code = 0x0C), Key("O", "O", code = 0x12),
    Key("P", "P", code = 0x13), Key("[", "{", code = 0x2F), Key("]", "}", code = 0x30),
    Key("\\", "|", code = 0x31, w = 1.5f),
)

private val SHARED_ROW_HOME = listOf(
    Key("Caps", code = 0x39, w = 1.75f, color = KC.MOD, isCaps = true, noRepeat = true),
    Key("A", "A", code = 0x04), Key("S", "S", code = 0x16), Key("D", "D", code = 0x07),
    Key("F", "F", code = 0x09), Key("G", "G", code = 0x0A), Key("H", "H", code = 0x0B),
    Key("J", "J", code = 0x0D), Key("K", "K", code = 0x0E), Key("L", "L", code = 0x0F),
    Key(";", ":", code = 0x33), Key("'", "\"", code = 0x34),
    Key("↵", "", code = 0x28, w = 2.25f, color = KC.ACCENT),
)

private val SHARED_ROW_ALPHA = listOf(
    Key("⇧", modBit = MOD_LSHIFT, w = 2.25f, color = KC.MOD, isMod = true, noRepeat = true),
    Key("Z", "Z", code = 0x1D), Key("X", "X", code = 0x1B), Key("C", "C", code = 0x06),
    Key("V", "V", code = 0x19), Key("B", "B", code = 0x05), Key("N", "N", code = 0x11),
    Key("M", "M", code = 0x10), Key(",", "<", code = 0x36), Key(".", ">", code = 0x37),
    Key("/", "?", code = 0x38),
    Key("⇧", modBit = MOD_RSHIFT, w = 2.75f, color = KC.MOD, isMod = true, noRepeat = true),
)

private val SHARED_ROW_MODS = listOf(
    Key("Ctrl", modBit = MOD_LCTRL, w = 1.5f, color = KC.MOD, isMod = true, noRepeat = true),
    Key("Win", modBit = MOD_LGUI, w = 1.2f, color = KC.MOD, isMod = true, noRepeat = true),
    Key("Alt", modBit = MOD_LALT, w = 1.2f, color = KC.MOD, isMod = true, noRepeat = true),
    Key("Space", code = 0x2C, w = 4.0f),
    Key("AltGr", modBit = MOD_RALT, w = 1.2f, color = KC.MOD, isMod = true, noRepeat = true),
    Key("Menu", code = 0x65, w = 1.5f, color = KC.MOD, noRepeat = true),
    Key("Ctrl", modBit = MOD_RCTRL, w = 1.5f, color = KC.MOD, isMod = true, noRepeat = true),
)

private val SHARED_ROW_MODS_COMPACT = listOf(
    Key("Ctrl", modBit = MOD_LCTRL, w = 1.25f, color = KC.MOD, isMod = true, noRepeat = true),
    Key("Win", modBit = MOD_LGUI, w = 1.0f, color = KC.MOD, isMod = true, noRepeat = true),
    Key("Alt", modBit = MOD_LALT, w = 1.0f, color = KC.MOD, isMod = true, noRepeat = true),
    Key("Space", code = 0x2C, w = 4.5f),
    Key("AltGr", modBit = MOD_RALT, w = 1.0f, color = KC.MOD, isMod = true, noRepeat = true),
    Key("Menu", code = 0x65, w = 1.5f, color = KC.MOD, noRepeat = true),
    Key("Ctrl", modBit = MOD_RCTRL, w = 1.25f, color = KC.MOD, isMod = true, noRepeat = true),
)