// KeyboardScreen.kt
package com.arena.hidcompatibilitytester

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ═════════════════════════════════════════════════════════════════════════════
// HID Modifier bits
// ═════════════════════════════════════════════════════════════════════════════
private const val MOD_LSHIFT = 0x02
private const val MOD_LCTRL  = 0x01
private const val MOD_LALT   = 0x04
private const val MOD_LGUI   = 0x08
private const val MOD_RCTRL  = 0x10
private const val MOD_RSHIFT = 0x20
private const val MOD_RALT   = 0x40
private const val MOD_RGUI   = 0x80

// ═════════════════════════════════════════════════════════════════════════════
// Key color type
// ═════════════════════════════════════════════════════════════════════════════

private enum class KC { NORMAL, MOD, SPECIAL, ACCENT, DANGER, FN }

// ═════════════════════════════════════════════════════════════════════════════
// Key data class
// ═════════════════════════════════════════════════════════════════════════════

private data class Key(
    val label: String, val shift: String = "", val altGr: String = "", val fnLabel: String = "",
    val w: Float = 1f, val code: Int = 0, val modBit: Int = 0, val color: KC = KC.NORMAL,
    val isMod: Boolean = false, val isCaps: Boolean = false, val isNum: Boolean = false,
    val isFn: Boolean = false, val isScroll: Boolean = false, val noRepeat: Boolean = false,
)

private fun Key.shouldRepeat() = !noRepeat && !isMod && !isCaps && !isNum && !isFn && !isScroll

// ═════════════════════════════════════════════════════════════════════════════
// Keyboard State — full physical model
// ═════════════════════════════════════════════════════════════════════════════

// 1. Updated KbState with missing properties
private data class KbState(
    val lCtrl: Boolean = false, val rCtrl: Boolean = false,
    val lShift: Boolean = false, val rShift: Boolean = false,
    val lAlt: Boolean = false, val rAlt: Boolean = false,
    val lGui: Boolean = false, val rGui: Boolean = false,
    val caps: Boolean = false, val numLock: Boolean = true,
    val scrollLk: Boolean = false, val fn: Boolean = false,
    val insertMode: Boolean = true, val tab: Int = 0, val lastKey: String = "",
) {
    val shift get() = lShift || rShift
    val ctrl get() = lCtrl || rCtrl
    
    // --- The missing properties fixed here ---
    val alt get() = lAlt
    val altGr get() = rAlt
    val gui get() = lGui || rGui
    
    val anyMod get() = ctrl || shift || alt || altGr || gui
    
    fun isUpperCase() = caps xor shift
    
    fun modByte(): Int {
        var m = 0
        if (lCtrl) m = m or MOD_LCTRL
        if (rCtrl) m = m or MOD_RCTRL
        if (lShift) m = m or MOD_LSHIFT
        if (rShift) m = m or MOD_RSHIFT
        if (lAlt) m = m or MOD_LALT
        if (rAlt) m = m or MOD_RALT
        if (lGui) m = m or MOD_LGUI
        if (rGui) m = m or MOD_RGUI
        return m
    }
    
    fun releaseMods() = copy(
        lCtrl=false, rCtrl=false, lShift=false, rShift=false, 
        lAlt=false, rAlt=false, lGui=false, rGui=false
    )
    
    fun modPrefix() = buildString {
        if (ctrl) append("Ctrl+")
        if (shift) append("Shift+")
        if (alt) append("Alt+")
        if (altGr) append("AltGr+")
        if (gui) append("Win+")
    }
}

// 2. Updated getDisplayLabel to resolve st.altGr
private fun getDisplayLabel(k: Key, st: KbState): String {
    val isLetter = k.label.length == 1 && k.label[0].isLetter()
    return when {
        // Now resolves correctly
        st.altGr && k.altGr.isNotEmpty() -> k.altGr 
        isLetter && st.isUpperCase() -> k.label.uppercase()
        st.shift && k.shift.isNotEmpty() -> k.shift
        else -> k.label
    }
}

// 3. Updated isKeyActive to handle all modifiers
private fun isKeyActive(k: Key, st: KbState) = when {
    k.isCaps -> st.caps
    k.isNum -> st.numLock
    k.isFn -> st.fn
    k.isScroll -> st.scrollLk
    k.modBit == MOD_LSHIFT -> st.lShift
    k.modBit == MOD_RSHIFT -> st.rShift
    k.modBit == MOD_LCTRL -> st.lCtrl
    k.modBit == MOD_RCTRL -> st.rCtrl
    k.modBit == MOD_LALT -> st.lAlt
    k.modBit == MOD_RALT -> st.rAlt
    k.modBit == MOD_LGUI -> st.lGui
    else -> false
}

// ═════════════════════════════════════════════════════════════════════════════
// Key rows — exact physical keyboard layout
// ═════════════════════════════════════════════════════════════════════════════

// ── Fn / F-key row ────────────────────────────────────────────────────────
private val ROW_FN = listOf(
    Key("Esc", code=0x29, color=KC.DANGER, noRepeat=true),
    Key("F1", code=0x3A), Key("F2", code=0x3B), Key("F3", code=0x3C), Key("F4", code=0x3D),
    Key("F5", code=0x3E), Key("F6", code=0x3F), Key("F7", code=0x40), Key("F8", code=0x41),
    Key("F9", code=0x42), Key("F10", code=0x43), Key("F11", code=0x44), Key("F12", code=0x45),
    Key("Prt", code=0x46, color=KC.SPECIAL), Key("Scr", code=0x47, color=KC.SPECIAL, isScroll=true),
    Key("Pse", code=0x48, color=KC.SPECIAL)
)

// ── Number row ─────────────────────────────────────────────────────────────
private val ROW_NUM = listOf(
    Key("`","~", code=0x35), Key("1","!", code=0x1E), Key("2","@", code=0x1F), Key("3","#", code=0x20),
    Key("4","$", code=0x21), Key("5","%", code=0x22), Key("6","^", code=0x23), Key("7","&", code=0x24),
    Key("8","*", code=0x25), Key("9","(", code=0x26), Key("0",")", code=0x27), Key("-","_", code=0x2D),
    Key("=","+", code=0x2E), Key("⌫", code=0x2A, w=2f, color=KC.DANGER)
)

// ── QWERTY row ─────────────────────────────────────────────────────────────
private val ROW_QWERTY = listOf(
    Key("Tab", code=0x2B, w=1.5f, color=KC.MOD), Key("Q", code=0x04), Key("W", code=0x1A), Key("E", code=0x08),
    Key("R", code=0x15), Key("T", code=0x17), Key("Y", code=0x1C), Key("U", code=0x18), Key("I", code=0x0C),
    Key("O", code=0x12), Key("P", code=0x13), Key("[","{", code=0x2F), Key("]","}", code=0x30), Key("\\","|", code=0x31, w=1.5f)
)

// ── Home row ───────────────────────────────────────────────────────────────
private val ROW_HOME = listOf(
    Key("Caps", code=0x39, w=1.8f, color=KC.MOD, isCaps=true), Key("A", code=0x04), Key("S", code=0x16),
    Key("D", code=0x07), Key("F", code=0x09), Key("G", code=0x0A), Key("H", code=0x0B), Key("J", code=0x0D),
    Key("K", code=0x0E), Key("L", code=0x0F), Key(";",":", code=0x33), Key("'","\"", code=0x34), Key("↵", code=0x28, w=2.2f, color=KC.ACCENT)
)

// ── Bottom alpha row ───────────────────────────────────────────────────────
private val ROW_ALPHA = listOf(
    Key("⇧", modBit=MOD_LSHIFT, w=2.3f, color=KC.MOD, isMod=true), Key("Z", code=0x1D), Key("X", code=0x1B),
    Key("C", code=0x06), Key("V", code=0x19), Key("B", code=0x05), Key("N", code=0x11), Key("M", code=0x10),
    Key(",", "<", code=0x36), Key(".", ">", code=0x37), Key("/", "?", code=0x38), Key("⇧", modBit=MOD_RSHIFT, w=2.8f, color=KC.MOD, isMod=true)
)

// ── Modifier / Space row ───────────────────────────────────────────────────
private val ROW_MODS = listOf(
    Key("Ctrl", modBit=MOD_LCTRL, w=1.5f, color=KC.MOD, isMod=true), Key("Win", modBit=MOD_LGUI, w=1.2f, color=KC.MOD, isMod=true),
    Key("Alt", modBit=MOD_LALT, w=1.2f, color=KC.MOD, isMod=true), Key("Space", code=0x2C, w=5f),
    Key("AltGr", modBit=MOD_RALT, w=1.2f, color=KC.MOD, isMod=true), Key("Menu", code=0x65, color=KC.MOD),
    Key("Fn", isFn=true, color=KC.FN), Key("Ctrl", modBit=MOD_RCTRL, w=1.5f, color=KC.MOD, isMod=true)
)

// ── Navigation cluster ─────────────────────────────────────────────────────
private val NAV_CLUSTER = listOf(
    listOf(
        Key("Ins",  code=HidKeyCodes.KEY_INSERT,    color=KC.SPECIAL),
        Key("Home", code=HidKeyCodes.KEY_HOME,      color=KC.SPECIAL),
        Key("PgUp", code=HidKeyCodes.KEY_PAGE_UP,   color=KC.SPECIAL),
    ),
    listOf(
        Key("Del",  code=HidKeyCodes.KEY_DELETE,    color=KC.DANGER),
        Key("End",  code=HidKeyCodes.KEY_END,       color=KC.SPECIAL),
        Key("PgDn", code=HidKeyCodes.KEY_PAGE_DOWN, color=KC.SPECIAL),
    ),
)

// ═════════════════════════════════════════════════════════════════════════════
// Numpad rows
// ═════════════════════════════════════════════════════════════════════════════

// numLock ON label / numLock OFF label / HID code
private data class NumKey(
    val onLabel  : String,
    val offLabel : String,
    val code     : Int,
    val w        : Float = 1f,
    val color    : KC    = KC.NORMAL,
    val isNumLock: Boolean = false,
    val noRepeat : Boolean = false,
)

private val NUMPAD_ROWS = listOf(
    listOf(
        NumKey("NumLk","NumLk", 0x53, color=KC.MOD, isNumLock=true, noRepeat=true),
        NumKey("/",    "/",     0x54, color=KC.SPECIAL),
        NumKey("*",    "*",     0x55, color=KC.SPECIAL),
        NumKey("-",    "-",     0x56, color=KC.SPECIAL),
    ),
    listOf(
        NumKey("7","Home", 0x5F),
        NumKey("8","↑",    0x60),
        NumKey("9","PgUp", 0x61),
        NumKey("+","+",    0x57, color=KC.ACCENT),
    ),
    listOf(
        NumKey("4","←", 0x5C),
        NumKey("5","",  0x5D),
        NumKey("6","→", 0x5E),
    ),
    listOf(
        NumKey("1","End",  0x59),
        NumKey("2","↓",   0x5A),
        NumKey("3","PgDn",0x5B),
        NumKey("↵","↵",   0x58, color=KC.ACCENT),
    ),
    listOf(
        NumKey("0","Ins", 0x62, w=2f),
        NumKey(".","Del", 0x63),
    ),
)

// ═════════════════════════════════════════════════════════════════════════════
// Colour helpers
// ═════════════════════════════════════════════════════════════════════════════

private fun keyBg(c: KC, active: Boolean, pressed: Boolean): Color = when {
    pressed -> Color(0xFF3A7ABD)
    active  -> Color(0xFF1E4A7A)
    else    -> when (c) {
        KC.NORMAL  -> Color(0xFF2A3240)
        KC.MOD     -> Color(0xFF1A2332)
        KC.SPECIAL -> Color(0xFF1A2535)
        KC.ACCENT  -> Color(0xFF1A3A5C)
        KC.DANGER  -> Color(0xFF3A1A1A)
        KC.FN      -> Color(0xFF1A2A1A)
    }
}

private fun keyFg(c: KC, active: Boolean): Color = when {
    active         -> Color(0xFF90CAF9)
    c == KC.ACCENT -> Color(0xFF64B5F6)
    c == KC.DANGER -> Color(0xFFEF9A9A)
    c == KC.FN     -> Color(0xFFA5D6A7)
    c == KC.MOD    -> Color(0xFFB0BEC5)
    else           -> Color(0xFFECEFF1)
}

@Composable
private fun ResponsiveRow(keys: List<Key>, height: Dp, st: KbState, onClick: (Key) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        keys.forEach { k ->
            KBtn(
                key = k,
                modifier = Modifier.weight(k.w),
                h = height,
                active = isKeyActive(k, st),
                mainLabel = getDisplayLabel(k, st),
                onPress = { onClick(k) }
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// KBtn — single physical key widget
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun KBtn(
    key: Key,
    modifier: Modifier,
    h: Dp,
    active: Boolean,
    mainLabel: String,
    onPress: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val bg by animateColorAsState(if (pressed) Color(0xFF3A7ABD) else if (active) Color(0xFF1E4A7A) else Color(0xFF2A3240))

    Box(
        modifier = modifier
            .height(h)
            .padding(1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .pointerInput(key) {
                detectTapGestures(onPress = {
                    pressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPress()
                    tryAwaitRelease()
                    pressed = false
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(mainLabel, color = Color.White, fontSize = if (mainLabel.length > 3) 10.sp else 14.sp)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// KeyboardScreen — main entry point
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun KeyboardScreen(
    isReady      : Boolean,
    onSendKey    : (modifiers: Int, keyCodes: List<Int>) -> Unit,
    onConsumerKey: (Int) -> Unit,
    onTypeText   : (String) -> Unit,
) {
    var st by remember { mutableStateOf(KbState()) }
    var typeText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // ── Core key handler ─────────────────────────────────────────────────────
    fun handleKey(key: Key) {
        when {
            // CapsLock toggle
            key.isCaps -> { st = st.copy(caps = !st.caps); onSendKey(0, listOf(0x39)); scope.launch { delay(50); onSendKey(0, emptyList()) } }

            // NumLock toggle
            key.isNum -> { st = st.copy(numLock = !st.numLock); onSendKey(0, listOf(0x53)); scope.launch { delay(50); onSendKey(0, emptyList()) } }

            // ScrollLock toggle
            key.isScroll -> {
                st = st.copy(scrollLk = !st.scrollLk, lastKey = "ScrollLock")
                onSendKey(0, listOf(HidKeyCodes.KEY_SCROLL_LOCK))
                scope.launch { delay(60); onSendKey(0, emptyList()) }
            }

            // Fn toggle
            key.isFn -> st = st.copy(fn = !st.fn)

            // Insert mode toggle
            key.code == HidKeyCodes.KEY_INSERT && !key.isMod -> {
                st = st.copy(insertMode = !st.insertMode, lastKey = "Insert")
                onSendKey(st.modByte(), listOf(HidKeyCodes.KEY_INSERT))
                scope.launch {
                    delay(60)
                    onSendKey(0, emptyList())
                    st = st.releaseMods()
                }
            }

            // Sticky modifier — tap to latch, tap again to unlatch
             key.isMod -> {
                st = when(key.modBit) {
                    MOD_LSHIFT -> st.copy(lShift = !st.lShift); MOD_RSHIFT -> st.copy(rShift = !st.rShift)
                    MOD_LCTRL -> st.copy(lCtrl = !st.lCtrl); MOD_RCTRL -> st.copy(rCtrl = !st.rCtrl)
                    MOD_LALT -> st.copy(lAlt = !st.lAlt); MOD_RALT -> st.copy(rAlt = !st.rAlt)
                    MOD_LGUI -> st.copy(lGui = !st.lGui)
                    else -> st
                }
                onSendKey(st.modByte(), emptyList())
            }

            // Regular key with a HID code
            key.code != 0 -> {
                val isLetter = key.label.length == 1 && key.label[0].isLetter()
                var mod = st.modByte()
                if (isLetter) {
                    val upper = st.caps xor st.shift
                    mod = if (upper) mod or MOD_LSHIFT else mod and MOD_LSHIFT.inv()
                }
                onSendKey(mod, listOf(key.code))
                scope.launch { delay(50); onSendKey(0, emptyList()); st = st.releaseMods() }
                st = st.copy(lastKey = key.label)
            }
        }
    }

    // ── Numpad key handler ───────────────────────────────────────────────────
    fun handleNumKey(nk: NumKey) {
        if (nk.isNumLock) {
            st = st.copy(numLock = !st.numLock, lastKey = "NumLock")
            onSendKey(0, listOf(0x53))
            scope.launch { delay(60); onSendKey(0, emptyList()) }
            return
        }
        // Shift temporarily inverts NumLock
        val effectiveNumLock = if (st.shift) !st.numLock else st.numLock
        val label = if (effectiveNumLock) nk.onLabel else nk.offLabel
        st = st.copy(lastKey = label)
        onSendKey(st.modByte(), listOf(nk.code))
        scope.launch {
            delay(60)
            onSendKey(0, emptyList())
            st = st.releaseMods()
        }
    }

    // ── isActive predicate ───────────────────────────────────────────────────
    fun isActive(key: Key) = when {
        key.isCaps   -> st.caps
        key.isNum    -> st.numLock
        key.isScroll -> st.scrollLk
        key.isFn     -> st.fn
        key.isMod    -> when (key.modBit) {
            MOD_LSHIFT -> st.lShift
            MOD_RSHIFT -> st.rShift
            MOD_LCTRL  -> st.lCtrl
            MOD_RCTRL  -> st.rCtrl
            MOD_LALT   -> st.lAlt
            MOD_RALT   -> st.rAlt
            MOD_LGUI   -> st.lGui
            MOD_RGUI   -> st.rGui
            else       -> false
        }
        key.code == HidKeyCodes.KEY_INSERT -> !st.insertMode
        else -> false
    }

    // ── Compute display label for a key given current state ──────────────────
    fun displayMain(key: Key): String {
        if (key.label.isEmpty()) return "Space"
        if (key.isMod || key.isCaps || key.isFn ||
            key.isNum || key.isScroll) return key.label
        val isLetter = key.label.length == 1 && key.label[0].isLetter()
        return when {
            st.altGr && key.altGr.isNotEmpty() -> key.altGr
            isLetter && st.isUpperCase()        -> key.label.uppercase()
            st.shift && key.shift.isNotEmpty()  -> key.shift
            else                                -> key.label
        }
    }

    fun displayTop(key: Key): String = when {
        key.isMod || key.isCaps || key.isFn ||
        key.isNum || key.isScroll              -> ""
        key.label.length == 1                  -> key.shift
        else                                   -> ""
    }

    val tabTitles = listOf("⌨ Keys", "↕ Nav+Num", "🎵 Media")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080F18))
    ) {
        // ── Tab bar ──────────────────────────────────────────────────────────
        TabRow(selectedTabIndex = st.tab) {
            listOf("Keys", "Nav", "Media").forEachIndexed { i, t ->
                Tab(selected = st.tab == i, onClick = { st = st.copy(tab = i) }, text = { Text(t, fontSize = 12.sp) })
            }
        }

        // ── Status bar ───────────────────────────────────────────────────────
        StatusBar(st = st, isReady = isReady, onClearMods = {
            st = st.releaseMods().copy(lastKey = "")
            onSendKey(0, emptyList())
        })

         if (!isReady) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Host Not Ready", color = Color.Gray) }
            return@Column
        }

        // ── Tab content ──────────────────────────────────────────────────────
        when (st.tab) {

            // ═════════════════════════════════════════════════════════════════
            // TAB 0 — Full QWERTY keyboard
            // ═════════════════════════════════════════════════════════════════
            // ... inside KeyboardScreen when st.tab == 0
            0 -> Column(Modifier.padding(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ResponsiveRow(ROW_FN, 32.dp, st) { handleKey(it) }
                ResponsiveRow(ROW_NUM, 42.dp, st) { handleKey(it) }
                ResponsiveRow(ROW_QWERTY, 42.dp, st) { handleKey(it) }
                ResponsiveRow(ROW_HOME, 42.dp, st) { handleKey(it) }
                ResponsiveRow(ROW_ALPHA, 42.dp, st) { handleKey(it) }
                ResponsiveRow(ROW_MODS, 42.dp, st) { handleKey(it) }
            }

            // ═════════════════════════════════════════════════════════════════
            // TAB 1 — Navigation cluster + Numpad + Type text
            // ═════════════════════════════════════════════════════════════════
            1 -> Column(Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
                Text("Numpad", color = Color.Gray, fontSize = 12.sp)
                // Add Numpad/Nav rows here using ResponsiveRow if needed
                OutlinedTextField(value = typeText, onValueChange = {typeText = it}, label = {Text("Type Text")}, modifier = Modifier.fillMaxWidth())
                Button(onClick = { onTypeText(typeText); typeText = "" }, Modifier.fillMaxWidth()) { Text("Send") }
            }

            // ═════════════════════════════════════════════════════════════════
            // TAB 2 — Media + Consumer keys
            // ═════════════════════════════════════════════════════════════════
            2 -> Column(Modifier.padding(16.dp)) {
                Button(onClick = { onConsumerKey(0xB5) }) { Text("Next Track") }
                Button(onClick = { onConsumerKey(0xE9) }) { Text("Volume Up") }
                Button(onClick = { onConsumerKey(0xEA) }) { Text("Volume Down") }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Status bar — modifier badges + combo preview
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun StatusBar(
    st          : KbState,
    isReady     : Boolean,
    onClearMods : () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF050C14))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Badge row
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            listOf(
                "CAPS"   to st.caps,
                "NUM"    to st.numLock,
                "SCR"    to st.scrollLk,
                "SHF"    to st.shift,
                "CTL"    to st.ctrl,
                "ALT"    to st.alt,
                "ALTGR"  to st.altGr,
                "WIN"    to st.gui,
                "FN"     to st.fn,
                "OVR"    to !st.insertMode,
            ).forEach { (lbl, on) -> LedBadge(lbl, on) }
            Spacer(Modifier.weight(1f))
            Text(
                if (isReady) "● Ready" else "○ Offline",
                fontSize = 9.sp,
                color    = if (isReady) Color(0xFF81C784) else Color(0xFFEF9A9A)
            )
        }

        // Combo preview row
        if (st.anyMod || st.lastKey.isNotEmpty()) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val combo = buildString {
                    append(st.modPrefix())
                    if (st.lastKey.isNotEmpty()) append(st.lastKey)
                }
                Surface(
                    color    = Color(0xFF0A1828),
                    shape    = RoundedCornerShape(5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text       = if (st.anyMod && st.lastKey.isEmpty())
                                         "▶ ${st.modPrefix().trimEnd('+')}+ ?"
                                     else if (combo.isEmpty()) "Ready…"
                                     else "⌨ $combo",
                        fontSize   = 11.sp,
                        color      = if (combo.isEmpty()) Color(0xFF546E7A)
                                     else Color(0xFF90CAF9),
                        fontWeight = FontWeight.Medium,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                if (st.anyMod) {
                    TextButton(
                        onClick        = onClearMods,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("✕ Clear", fontSize = 10.sp, color = Color(0xFFEF9A9A))
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// LED Badge
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun LedBadge(label: String, active: Boolean) {
    val bg by animateColorAsState(
        if (active) Color(0xFF1565C0) else Color.White.copy(0.04f),
        tween(100), label = "led"
    )
    Surface(shape = RoundedCornerShape(3.dp), color = bg) {
        Text(
            label,
            fontSize   = 7.sp,
            color      = if (active) Color.White else Color(0xFF3A4A5A),
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            modifier   = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// DarkCard
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun DarkCard(
    title    : String,
    modifier : Modifier = Modifier,
    content  : @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF111C28)),
        shape    = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier            = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                color      = Color(0xFF607D8B),
                fontSize   = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// MediaBtn
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun MediaBtn(
    label   : String,
    modifier: Modifier,
    h       : Dp,
    onPress : () -> Unit
) {
    val haptic  = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val bg by animateColorAsState(
        if (pressed) Color(0xFF1B3A5F) else Color(0xFF0E1C2A),
        tween(60), label = "mb"
    )
    Box(
        modifier = modifier
            .height(h)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(0.6.dp, Color.White.copy(0.08f), RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPress()
                    tryAwaitRelease()
                    pressed = false
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize  = if (label.length > 4) 9.sp else 16.sp,
            color     = Color(0xFFB0BEC5),
            textAlign = TextAlign.Center,
            maxLines  = 1,
            modifier  = Modifier.padding(horizontal = 4.dp)
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Numpad tap helper extension
// ═════════════════════════════════════════════════════════════════════════════

private fun Modifier.numpadTap(onPress: () -> Unit): Modifier =
    this.pointerInput(Unit) {
        detectTapGestures(onPress = {
            onPress()
            tryAwaitRelease()
        })
    }