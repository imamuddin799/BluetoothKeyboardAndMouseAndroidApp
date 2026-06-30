package com.arena.hidcompatibilitytester.ui.screen.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

// Re-use the key definitions and helpers from KeyboardScreen
// (They are file-private there, so we duplicate the minimal needed ones here
//  OR make them internal. For clean code, we make them internal.)

@Composable
fun InAppKeyboardPanel(
    onSendKey     : (Int, List<Int>) -> Unit,
    onReleaseKeys : () -> Unit,
    onConsumerKey : (Int) -> Unit,
    onTypeText    : (String) -> Unit,
    onDismiss     : () -> Unit,
) {
    var st by remember { mutableStateOf(InAppKbState()) }
    val scope = rememberCoroutineScope()

    fun handleKey(key: InAppKey) {
        when {
            key.isCaps -> {
                st = st.copy(caps = !st.caps, lastKey = "CapsLk")
                onSendKey(0, listOf(0x39))
                scope.launch { delay(60); onSendKey(0, emptyList()) }
            }
            key.isFn -> { st = st.copy(fn = !st.fn) }
            key.isMod -> {
                st = when (key.modBit) {
                    0x02 -> st.copy(lShift = !st.lShift)
                    0x20 -> st.copy(rShift = !st.rShift)
                    0x01 -> st.copy(lCtrl = !st.lCtrl)
                    0x10 -> st.copy(rCtrl = !st.rCtrl)
                    0x04 -> st.copy(lAlt = !st.lAlt)
                    0x40 -> st.copy(rAlt = !st.rAlt)
                    0x08 -> st.copy(lGui = !st.lGui)
                    0x80 -> st.copy(rGui = !st.rGui)
                    else -> st
                }
                onSendKey(st.modByte(), emptyList())
            }
            key.code != 0 -> {
                var mod = st.modByte()
                val isLetter = key.label.length == 1 && key.label[0].isLetter()
                if (isLetter) {
                    mod = mod and (0x02 or 0x20).inv()
                    if (st.shift) mod = mod or 0x02
                }
                onSendKey(mod, listOf(key.code))
                st = st.copy(lastKey = key.label)
                scope.launch {
                    delay(60); onSendKey(0, emptyList())
                    st = st.releaseMods()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF080F18))
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        // Dismiss bar
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(Color(0xFF050C14))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("In-App Keyboard", fontSize = 10.sp, color = Color(0xFF607D8B),
                fontWeight = FontWeight.SemiBold)

            // Modifier indicators
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (st.caps)  MiniLed("CAP")
                if (st.shift) MiniLed("SHF")
                if (st.ctrl)  MiniLed("CTL")
                if (st.alt)   MiniLed("ALT")
                if (st.gui)   MiniLed("WIN")
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Text("✕", fontSize = 12.sp, color = Color(0xFFEF9A9A))
            }
        }

        HorizontalDivider(color = Color.White.copy(0.04f), thickness = 1.dp)

        // Function row
        InAppKeyRow(INAPP_ROW_FN, 34.dp, st) { handleKey(it) }
        HorizontalDivider(color = Color.White.copy(0.04f), thickness = 1.dp)
        // Number row
        InAppKeyRow(INAPP_ROW_NUM, 36.dp, st) { handleKey(it) }
        // QWERTY
        InAppKeyRow(INAPP_ROW_QWERTY, 36.dp, st) { handleKey(it) }
        // Home row
        InAppKeyRow(INAPP_ROW_HOME, 36.dp, st) { handleKey(it) }
        // Alpha row
        InAppKeyRow(INAPP_ROW_ALPHA, 36.dp, st) { handleKey(it) }
        // Modifier row
        InAppKeyRow(INAPP_ROW_MODS, 36.dp, st) { handleKey(it) }
    }
}

@Composable
private fun MiniLed(label: String) {
    Text(
        label, fontSize = 6.sp, color = Color(0xFF90CAF9),
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(Color(0xFF1565C0).copy(0.3f), androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
            .padding(horizontal = 3.dp, vertical = 1.dp)
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// InApp key data & state — lightweight version for the panel
// ═════════════════════════════════════════════════════════════════════════════

internal data class InAppKey(
    val label  : String,
    val shift  : String = "",
    val w      : Float  = 1f,
    val code   : Int    = 0,
    val modBit : Int    = 0,
    val isMod  : Boolean = false,
    val isCaps : Boolean = false,
    val isFn   : Boolean = false,
    val color  : Int     = 0, // 0=normal, 1=mod, 2=accent, 3=danger, 4=fn
)

internal data class InAppKbState(
    val lCtrl: Boolean = false, val rCtrl: Boolean = false,
    val lShift: Boolean = false, val rShift: Boolean = false,
    val lAlt: Boolean = false, val rAlt: Boolean = false,
    val lGui: Boolean = false, val rGui: Boolean = false,
    val caps: Boolean = false, val fn: Boolean = false,
    val lastKey: String = "",
) {
    val shift get() = lShift || rShift
    val ctrl  get() = lCtrl || rCtrl
    val alt   get() = lAlt
    val gui   get() = lGui || rGui
    val anyMod get() = ctrl || shift || alt || rAlt || gui

    fun modByte(): Int {
        var m = 0
        if (lCtrl)  m = m or 0x01; if (rCtrl)  m = m or 0x10
        if (lShift) m = m or 0x02; if (rShift) m = m or 0x20
        if (lAlt)   m = m or 0x04; if (rAlt)   m = m or 0x40
        if (lGui)   m = m or 0x08; if (rGui)   m = m or 0x80
        return m
    }

    fun releaseMods() = copy(
        lCtrl = false, rCtrl = false, lShift = false, rShift = false,
        lAlt = false, rAlt = false, lGui = false, rGui = false
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// Key row definitions
// ═════════════════════════════════════════════════════════════════════════════

internal val INAPP_ROW_FN = listOf(
    InAppKey("Esc", code=0x29, w=1.4f, color=3),
    InAppKey("F1", code=0x3A, color=4), InAppKey("F2", code=0x3B, color=4),
    InAppKey("F3", code=0x3C, color=4), InAppKey("F4", code=0x3D, color=4),
    InAppKey("F5", code=0x3E, color=4), InAppKey("F6", code=0x3F, color=4),
    InAppKey("F7", code=0x40, color=4), InAppKey("F8", code=0x41, color=4),
    InAppKey("F9", code=0x42, color=4), InAppKey("F10", code=0x43, color=4),
    InAppKey("F11", code=0x44, color=4), InAppKey("F12", code=0x45, color=4),
    InAppKey("Del", code=0x4C, w=1.4f, color=3),
)

internal val INAPP_ROW_NUM = listOf(
    InAppKey("`","~",code=0x35), InAppKey("1","!",code=0x1E), InAppKey("2","@",code=0x1F),
    InAppKey("3","#",code=0x20), InAppKey("4","$",code=0x21), InAppKey("5","%",code=0x22),
    InAppKey("6","^",code=0x23), InAppKey("7","&",code=0x24), InAppKey("8","*",code=0x25),
    InAppKey("9","(",code=0x26), InAppKey("0",")",code=0x27), InAppKey("-","_",code=0x2D),
    InAppKey("=","+",code=0x2E), InAppKey("⌫","",code=0x2A,w=2.0f,color=3),
)

internal val INAPP_ROW_QWERTY = listOf(
    InAppKey("Tab",code=0x2B,w=1.5f,color=1),
    InAppKey("Q",code=0x14),InAppKey("W",code=0x1A),InAppKey("E",code=0x08),
    InAppKey("R",code=0x15),InAppKey("T",code=0x17),InAppKey("Y",code=0x1C),
    InAppKey("U",code=0x18),InAppKey("I",code=0x0C),InAppKey("O",code=0x12),
    InAppKey("P",code=0x13),InAppKey("[","{",code=0x2F),InAppKey("]","}",code=0x30),
    InAppKey("\\","|",code=0x31,w=1.5f),
)

internal val INAPP_ROW_HOME = listOf(
    InAppKey("Caps",code=0x39,w=1.75f,color=1,isCaps=true),
    InAppKey("A",code=0x04),InAppKey("S",code=0x16),InAppKey("D",code=0x07),
    InAppKey("F",code=0x09),InAppKey("G",code=0x0A),InAppKey("H",code=0x0B),
    InAppKey("J",code=0x0D),InAppKey("K",code=0x0E),InAppKey("L",code=0x0F),
    InAppKey(";",":",code=0x33),InAppKey("'","\"",code=0x34),
    InAppKey("↵","",code=0x28,w=2.25f,color=2),
)

internal val INAPP_ROW_ALPHA = listOf(
    InAppKey("⇧",modBit=0x02,w=2.25f,color=1,isMod=true),
    InAppKey("Z",code=0x1D),InAppKey("X",code=0x1B),InAppKey("C",code=0x06),
    InAppKey("V",code=0x19),InAppKey("B",code=0x05),InAppKey("N",code=0x11),
    InAppKey("M",code=0x10),InAppKey(",","<",code=0x36),InAppKey(".",">" ,code=0x37),
    InAppKey("/","?",code=0x38),
    InAppKey("⇧",modBit=0x20,w=2.75f,color=1,isMod=true),
)

internal val INAPP_ROW_MODS = listOf(
    InAppKey("Ctrl",modBit=0x01,w=1.5f,color=1,isMod=true),
    InAppKey("Win",modBit=0x08,w=1.2f,color=1,isMod=true),
    InAppKey("Alt",modBit=0x04,w=1.2f,color=1,isMod=true),
    InAppKey("Space",code=0x2C,w=5.0f),
    InAppKey("AltGr",modBit=0x40,w=1.2f,color=1,isMod=true),
    InAppKey("Fn",w=1.0f,color=4,isFn=true),
    InAppKey("Ctrl",modBit=0x10,w=1.5f,color=1,isMod=true),
)

// ═════════════════════════════════════════════════════════════════════════════
// InAppKeyRow — renders a single row of keys
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun InAppKeyRow(
    keys    : List<InAppKey>,
    height  : androidx.compose.ui.unit.Dp,
    st      : InAppKbState,
    onClick : (InAppKey) -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        keys.forEach { k ->
            val active = when {
                k.isCaps -> st.caps
                k.isFn   -> st.fn
                k.modBit == 0x02 -> st.lShift
                k.modBit == 0x20 -> st.rShift
                k.modBit == 0x01 -> st.lCtrl
                k.modBit == 0x10 -> st.rCtrl
                k.modBit == 0x04 -> st.lAlt
                k.modBit == 0x40 -> st.rAlt
                k.modBit == 0x08 -> st.lGui
                k.modBit == 0x80 -> st.rGui
                else -> false
            }

            val mainLabel = when {
                k.isMod || k.isCaps || k.isFn -> k.label
                k.label.length == 1 && k.label[0].isLetter() ->
                    if (st.caps xor st.shift) k.label.uppercase() else k.label.lowercase()
                st.shift && k.shift.isNotEmpty() -> k.shift
                else -> k.label
            }

            val bg = when {
                active -> Color(0xFF1E4A7A)
                k.color == 1 -> Color(0xFF1A2332)
                k.color == 2 -> Color(0xFF1A3A5C)
                k.color == 3 -> Color(0xFF3A1A1A)
                k.color == 4 -> Color(0xFF1A2A1A)
                else -> Color(0xFF2A3240)
            }

            val fg = when {
                active -> Color(0xFF90CAF9)
                k.color == 2 -> Color(0xFF64B5F6)
                k.color == 3 -> Color(0xFFEF9A9A)
                k.color == 4 -> Color(0xFFA5D6A7)
                k.color == 1 -> Color(0xFFB0BEC5)
                else -> Color(0xFFECEFF1)
            }

            InAppKeyButton(
                label    = mainLabel,
                topLabel = if (!k.isMod && !k.isCaps && !k.isFn && k.shift.isNotEmpty()
                    && k.label.length == 1) k.shift else "",
                bg       = bg,
                fg       = fg,
                active   = active,
                modifier = Modifier.weight(k.w).height(height),
                onClick  = { onClick(k) }
            )
        }
    }
}

@Composable
private fun InAppKeyButton(
    label    : String,
    topLabel : String,
    bg       : Color,
    fg       : Color,
    active   : Boolean,
    modifier : Modifier,
    onClick  : () -> Unit,
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .padding(0.5.dp)
            .background(
                if (pressed) Color(0xFF3A7ABD) else bg,
                androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
            )
            .then(
                if (active) Modifier.background(Color.Transparent)
                    .padding(0.dp) // placeholder
                else Modifier
            )
            .pointerInput(label) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        haptic.performHapticFeedback(
                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                        )
                        onClick()
                        tryAwaitRelease()
                        pressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 1.dp, vertical = 1.dp)
        ) {
            if (topLabel.isNotEmpty()) {
                Text(topLabel, fontSize = 6.sp, color = Color.White.copy(0.25f),
                    lineHeight = 6.sp)
            }
            Text(
                label,
                color = fg,
                fontSize = when {
                    label.length > 4 -> 7.sp
                    label.length > 2 -> 9.sp
                    else -> 11.sp
                },
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
