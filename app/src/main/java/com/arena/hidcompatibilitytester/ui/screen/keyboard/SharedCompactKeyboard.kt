package com.arena.hidcompatibilitytester.ui.screen.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.arena.hidcompatibilitytester.ui.screen.keyboard.components.KBtn

@Composable
fun SharedCompactKeyboard(
    settings      : KeyboardSettings,
    showDismissBar: Boolean = false,
    onSendKey     : (Int, List<Int>) -> Unit,
    onReleaseKeys : () -> Unit,
    onConsumerKey : (Int) -> Unit,
    onTypeText    : (String) -> Unit,
    onDismiss     : (() -> Unit)? = null,
) {
    var st by remember { mutableStateOf(SharedKbState()) }
    val scope = rememberCoroutineScope()

    fun handleKey(key: Key) {
        when {
            key.isCaps -> {
                st = st.copy(caps = !st.caps, lastKey = "CapsLk")
                onSendKey(0, listOf(0x39))
                scope.launch { delay(60); onSendKey(0, emptyList()) }
            }
            key.isMod -> {
                st = st.toggleMod(key.modBit)
                onSendKey(st.modByte(), emptyList())
            }
            key.code == 0x2B -> {
                val mod = st.modByte()
                onSendKey(mod, listOf(key.code))
                st = st.copy(lastKey = st.modPrefix() + "Tab")
                scope.launch {
                    delay(60)
                    onSendKey(st.modByte(), emptyList())
                }
            }
            key.code != 0 -> {
                var mod = st.modByte()
                val isLetter = key.label.length == 1 && key.label[0].isLetter()
                if (isLetter) {
                    mod = mod and (MOD_LSHIFT or MOD_RSHIFT).inv()
                    if (st.shift) mod = mod or MOD_LSHIFT
                }
                onSendKey(mod, listOf(key.code))
                st = st.copy(lastKey = key.label)
                scope.launch {
                    delay(60); onSendKey(0, emptyList())
                    if (settings.autoReleaseModsAfterKey && !settings.stickyModifiers) {
                        st = st.releaseMods()
                    }
                }
            }
        }
    }

    fun isActive(k: Key): Boolean = when {
        k.isCaps -> st.caps
        k.modBit == MOD_LSHIFT || k.modBit == MOD_RSHIFT -> st.shift
        k.modBit == MOD_LCTRL  || k.modBit == MOD_RCTRL  -> st.ctrl
        k.modBit == MOD_LALT                              -> st.alt
        k.modBit == MOD_RALT                              -> st.altGr
        k.modBit == MOD_LGUI   || k.modBit == MOD_RGUI   -> st.gui
        else -> false
    }

    fun mainLabel(k: Key): String = when {
        k.label.isEmpty() -> "Space"
        k.isMod || k.isCaps -> k.label
        k.label.length == 1 && k.label[0].isLetter() ->
            if (st.caps xor st.shift) k.label.uppercase() else k.label.lowercase()
        st.shift && k.shift.isNotEmpty() -> k.shift
        else -> k.label
    }

    fun topLabel(k: Key): String {
        if (!settings.showKeyHints) return ""
        if (k.isMod || k.isCaps) return ""
        if (k.label.length == 1 && k.shift.isNotEmpty()) return k.shift
        return ""
    }

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
                    if (st.caps)  SharedMiniLed("CAP")
                    if (st.shift) SharedMiniLed("SHF")
                    if (st.ctrl)  SharedMiniLed("CTL")
                    if (st.alt)   SharedMiniLed("ALT")
                    if (st.gui)   SharedMiniLed("WIN")
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

        val rowH = settings.keyHeight.mainDp.dp
        val fnH  = settings.keyHeight.fnDp.dp

        SharedStyledKeyRow(SHARED_ROW_FN, fnH, settings, ::isActive, ::mainLabel, ::topLabel) { handleKey(it) }
        HorizontalDivider(color = Color.White.copy(0.04f), thickness = 1.dp)
        SharedStyledKeyRow(SHARED_ROW_NUM, rowH, settings, ::isActive, ::mainLabel, ::topLabel) { handleKey(it) }
        SharedStyledKeyRow(SHARED_ROW_QWERTY, rowH, settings, ::isActive, ::mainLabel, ::topLabel) { handleKey(it) }
        SharedStyledKeyRow(SHARED_ROW_HOME, rowH, settings, ::isActive, ::mainLabel, ::topLabel) { handleKey(it) }
        SharedStyledKeyRow(SHARED_ROW_ALPHA, rowH, settings, ::isActive, ::mainLabel, ::topLabel) { handleKey(it) }
        SharedStyledKeyRow(
            if (settings.compactModifiers) SHARED_ROW_MODS_COMPACT else SHARED_ROW_MODS,
            rowH, settings, ::isActive, ::mainLabel, ::topLabel
        ) { handleKey(it) }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Private composables — scoped to this file only
// ═════════════════════════════════════════════════════════════════════════════

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

// ═════════════════════════════════════════════════════════════════════════════
// Shared keyboard state
// ═════════════════════════════════════════════════════════════════════════════

internal data class SharedKbState(
    val ctrl: Boolean = false,
    val shift: Boolean = false,
    val alt: Boolean = false,
    val altGr: Boolean = false,
    val gui: Boolean = false,
    val caps: Boolean = false,
    val lastKey: String = "",
) {
    val anyMod get() = ctrl || shift || alt || altGr || gui

    fun modByte(): Int {
        var m = 0
        if (ctrl)  m = m or MOD_LCTRL
        if (shift) m = m or MOD_LSHIFT
        if (alt)   m = m or MOD_LALT
        if (altGr) m = m or MOD_RALT
        if (gui)   m = m or MOD_LGUI
        return m
    }

    fun toggleMod(modBit: Int): SharedKbState = when (modBit) {
        MOD_LSHIFT, MOD_RSHIFT -> copy(shift = !shift)
        MOD_LCTRL, MOD_RCTRL  -> copy(ctrl = !ctrl)
        MOD_LALT               -> copy(alt = !alt)
        MOD_RALT               -> copy(altGr = !altGr)
        MOD_LGUI, MOD_RGUI    -> copy(gui = !gui)
        else                   -> this
    }

    fun releaseMods() = copy(
        ctrl = false, shift = false, alt = false, altGr = false, gui = false,
    )

    fun modPrefix() = buildString {
        if (ctrl)  append("Ctrl+")
        if (shift) append("Shift+")
        if (alt)   append("Alt+")
        if (altGr) append("AltGr+")
        if (gui)   append("Win+")
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Key row definitions — reuses Key from KeyData.kt
// ═════════════════════════════════════════════════════════════════════════════

private val SHARED_ROW_FN = listOf(
    Key("Esc", code = 0x29, w = 1.4f, color = KC.DANGER, noRepeat = true),
    Key("F1",  code = 0x3A, color = KC.SPECIAL),
    Key("F2",  code = 0x3B, color = KC.SPECIAL),
    Key("F3",  code = 0x3C, color = KC.SPECIAL),
    Key("F4",  code = 0x3D, color = KC.SPECIAL),
    Key("F5",  code = 0x3E, color = KC.SPECIAL),
    Key("F6",  code = 0x3F, color = KC.SPECIAL),
    Key("F7",  code = 0x40, color = KC.SPECIAL),
    Key("F8",  code = 0x41, color = KC.SPECIAL),
    Key("F9",  code = 0x42, color = KC.SPECIAL),
    Key("F10", code = 0x43, color = KC.SPECIAL),
    Key("F11", code = 0x44, color = KC.SPECIAL),
    Key("F12", code = 0x45, color = KC.SPECIAL),
    Key("Del", code = 0x4C, w = 1.4f, color = KC.DANGER),
)

private val SHARED_ROW_NUM = listOf(
    Key("`","~",code=0x35), Key("1","!",code=0x1E), Key("2","@",code=0x1F),
    Key("3","#",code=0x20), Key("4","$",code=0x21), Key("5","%",code=0x22),
    Key("6","^",code=0x23), Key("7","&",code=0x24), Key("8","*",code=0x25),
    Key("9","(",code=0x26), Key("0",")",code=0x27), Key("-","_",code=0x2D),
    Key("=","+",code=0x2E), Key("⌫","",code=0x2A,w=2.0f,color=KC.DANGER),
)

private val SHARED_ROW_QWERTY = listOf(
    Key("Tab",code=0x2B,w=1.5f,color=KC.MOD),
    Key("Q","Q",code=0x14),Key("W","W",code=0x1A),Key("E","E",code=0x08),
    Key("R","R",code=0x15),Key("T","T",code=0x17),Key("Y","Y",code=0x1C),
    Key("U","U",code=0x18),Key("I","I",code=0x0C),Key("O","O",code=0x12),
    Key("P","P",code=0x13),Key("[","{",code=0x2F),Key("]","}",code=0x30),
    Key("\\","|",code=0x31,w=1.5f),
)

private val SHARED_ROW_HOME = listOf(
    Key("Caps",code=0x39,w=1.75f,color=KC.MOD,isCaps=true,noRepeat=true),
    Key("A","A",code=0x04),Key("S","S",code=0x16),Key("D","D",code=0x07),
    Key("F","F",code=0x09),Key("G","G",code=0x0A),Key("H","H",code=0x0B),
    Key("J","J",code=0x0D),Key("K","K",code=0x0E),Key("L","L",code=0x0F),
    Key(";",":",code=0x33),Key("'","\"",code=0x34),
    Key("↵","",code=0x28,w=2.25f,color=KC.ACCENT),
)

private val SHARED_ROW_ALPHA = listOf(
    Key("⇧",modBit=MOD_LSHIFT,w=2.25f,color=KC.MOD,isMod=true,noRepeat=true),
    Key("Z","Z",code=0x1D),Key("X","X",code=0x1B),Key("C","C",code=0x06),
    Key("V","V",code=0x19),Key("B","B",code=0x05),Key("N","N",code=0x11),
    Key("M","M",code=0x10),Key(",","<",code=0x36),Key(".",">" ,code=0x37),
    Key("/","?",code=0x38),
    Key("⇧",modBit=MOD_RSHIFT,w=2.75f,color=KC.MOD,isMod=true,noRepeat=true),
)

private val SHARED_ROW_MODS = listOf(
    Key("Ctrl", modBit=MOD_LCTRL, w=1.5f,color=KC.MOD,isMod=true,noRepeat=true),
    Key("Win",  modBit=MOD_LGUI,  w=1.2f,color=KC.MOD,isMod=true,noRepeat=true),
    Key("Alt",  modBit=MOD_LALT,  w=1.2f,color=KC.MOD,isMod=true,noRepeat=true),
    Key("Space",code=0x2C,w=4.0f),
    Key("AltGr",modBit=MOD_RALT,  w=1.2f,color=KC.MOD,isMod=true,noRepeat=true),
    Key("Menu", code=0x65,w=1.5f,color=KC.MOD,noRepeat=true),
    Key("Ctrl", modBit=MOD_RCTRL, w=1.5f,color=KC.MOD,isMod=true,noRepeat=true),
)

private val SHARED_ROW_MODS_COMPACT = listOf(
    Key("Ctrl", modBit=MOD_LCTRL, w=1.25f,color=KC.MOD,isMod=true,noRepeat=true),
    Key("Win",  modBit=MOD_LGUI,  w=1.0f,color=KC.MOD,isMod=true,noRepeat=true),
    Key("Alt",  modBit=MOD_LALT,  w=1.0f,color=KC.MOD,isMod=true,noRepeat=true),
    Key("Space",code=0x2C,w=4.5f),
    Key("AltGr",modBit=MOD_RALT,  w=1.0f,color=KC.MOD,isMod=true,noRepeat=true),
    Key("Menu", code=0x65,w=1.5f,color=KC.MOD,noRepeat=true),
    Key("Ctrl", modBit=MOD_RCTRL, w=1.25f,color=KC.MOD,isMod=true,noRepeat=true),
)