// KeyboardScreen.kt
package com.arena.hidcompatibilitytester

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation

// ═════════════════════════════════════════════════════════════════════════════
// HID Modifier bits
// ═════════════════════════════════════════════════════════════════════════════

private const val MOD_LCTRL  = 0x01
private const val MOD_LSHIFT = 0x02
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
    val label    : String,
    val shift    : String  = "",
    val altGr    : String  = "",
    val fnLabel  : String  = "",
    val w        : Float   = 1f,
    val code     : Int     = 0,
    val modBit   : Int     = 0,
    val color    : KC      = KC.NORMAL,
    val isMod    : Boolean = false,
    val isCaps   : Boolean = false,
    val isNum    : Boolean = false,
    val isFn     : Boolean = false,
    val isScroll : Boolean = false,
    val noRepeat : Boolean = false,
)

private fun Key.shouldRepeat() =
    !noRepeat && !isMod && !isCaps && !isNum && !isFn && !isScroll

// ═════════════════════════════════════════════════════════════════════════════
// Keyboard State
// ═════════════════════════════════════════════════════════════════════════════

private data class KbState(
    val lCtrl     : Boolean = false,
    val rCtrl     : Boolean = false,
    val lShift    : Boolean = false,
    val rShift    : Boolean = false,
    val lAlt      : Boolean = false,
    val rAlt      : Boolean = false,
    val lGui      : Boolean = false,
    val rGui      : Boolean = false,
    val caps      : Boolean = false,
    val numLock   : Boolean = true,
    val scrollLk  : Boolean = false,
    val fn        : Boolean = false,
    val insertMode: Boolean = true,
    val tab       : Int     = 0,
    val lastKey   : String  = "",
) {
    val shift  get() = lShift || rShift
    val ctrl   get() = lCtrl  || rCtrl
    val alt    get() = lAlt
    val altGr  get() = rAlt
    val gui    get() = lGui   || rGui
    val anyMod get() = ctrl || shift || alt || altGr || gui

    fun isUpperCase() = caps xor shift

    fun modByte(): Int {
        var m = 0
        if (lCtrl)  m = m or MOD_LCTRL
        if (rCtrl)  m = m or MOD_RCTRL
        if (lShift) m = m or MOD_LSHIFT
        if (rShift) m = m or MOD_RSHIFT
        if (lAlt)   m = m or MOD_LALT
        if (rAlt)   m = m or MOD_RALT
        if (lGui)   m = m or MOD_LGUI
        if (rGui)   m = m or MOD_RGUI
        return m
    }

    fun releaseMods() = copy(
        lCtrl  = false, rCtrl  = false,
        lShift = false, rShift = false,
        lAlt   = false, rAlt   = false,
        lGui   = false, rGui   = false,
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
// Key row definitions
// ═════════════════════════════════════════════════════════════════════════════

private val ROW_FN = listOf(
    Key("Esc",   code=0x29, w=1.0f, color=KC.DANGER,  noRepeat=true),
    Key("F1",    code=0x3A, fnLabel="Brt▼", color=KC.FN),
    Key("F2",    code=0x3B, fnLabel="Brt▲", color=KC.FN),
    Key("F3",    code=0x3C, fnLabel="Srch",  color=KC.FN),
    Key("F4",    code=0x3D, fnLabel="App",   color=KC.FN),
    Key("F5",    code=0x3E, fnLabel="Ref",   color=KC.FN),
    Key("F6",    code=0x3F, fnLabel="Prv",   color=KC.FN),
    Key("F7",    code=0x40, fnLabel="⏮",    color=KC.FN),
    Key("F8",    code=0x41, fnLabel="⏯",    color=KC.FN),
    Key("F9",    code=0x42, fnLabel="⏭",    color=KC.FN),
    Key("F10",   code=0x43, fnLabel="🔇",   color=KC.FN),
    Key("F11",   code=0x44, fnLabel="🔉",   color=KC.FN),
    Key("F12",   code=0x45, fnLabel="🔊",   color=KC.FN),
    Key("PrtSc", code=0x46, color=KC.SPECIAL, noRepeat=true),
    Key("ScrLk", code=0x47, color=KC.SPECIAL, noRepeat=true, isScroll=true),
    Key("Pause", code=0x48, color=KC.SPECIAL, noRepeat=true),
)

private val ROW_NUM = listOf(
    Key("`",  "~",  code=0x35),
    Key("1",  "!",  code=0x1E),
    Key("2",  "@",  code=0x1F),
    Key("3",  "#",  code=0x20),
    Key("4",  "$",  code=0x21),
    Key("5",  "%",  code=0x22),
    Key("6",  "^",  code=0x23),
    Key("7",  "&",  code=0x24),
    Key("8",  "*",  code=0x25),
    Key("9",  "(",  code=0x26),
    Key("0",  ")",  code=0x27),
    Key("-",  "_",  code=0x2D),
    Key("=",  "+",  code=0x2E),
    Key("⌫", "",   code=0x2A, w=2.0f, color=KC.DANGER),
)

private val ROW_QWERTY = listOf(
    Key("Tab",  code=0x2B, w=1.5f, color=KC.MOD),
    Key("Q","Q", code=0x14), Key("W","W", code=0x1A), Key("E","E", code=0x08),
    Key("R","R", code=0x15), Key("T","T", code=0x17), Key("Y","Y", code=0x1C),
    Key("U","U", code=0x18), Key("I","I", code=0x0C), Key("O","O", code=0x12),
    Key("P","P", code=0x13),
    Key("[", "{", code=0x2F),
    Key("]", "}", code=0x30),
    Key("\\","|", code=0x31, w=1.5f),
)

private val ROW_HOME = listOf(
    Key("Caps", code=0x39, w=1.75f, color=KC.MOD, isCaps=true, noRepeat=true),
    Key("A","A", code=0x04), Key("S","S", code=0x16), Key("D","D", code=0x07),
    Key("F","F", code=0x09), Key("G","G", code=0x0A), Key("H","H", code=0x0B),
    Key("J","J", code=0x0D), Key("K","K", code=0x0E), Key("L","L", code=0x0F),
    Key(";", ":", code=0x33),
    Key("'", "\"",code=0x34),
    Key("↵",  "",  code=0x28, w=2.25f, color=KC.ACCENT),
)

private val ROW_ALPHA = listOf(
    Key("⇧", modBit=MOD_LSHIFT, w=2.25f, color=KC.MOD, isMod=true, noRepeat=true),
    Key("Z","Z", code=0x1D), Key("X","X", code=0x1B), Key("C","C", code=0x06),
    Key("V","V", code=0x19), Key("B","B", code=0x05), Key("N","N", code=0x11),
    Key("M","M", code=0x10),
    Key(",", "<", code=0x36),
    Key(".", ">", code=0x37),
    Key("/", "?", code=0x38),
    Key("⇧", modBit=MOD_RSHIFT, w=2.75f, color=KC.MOD, isMod=true, noRepeat=true),
)

private val ROW_MODS = listOf(
    Key("Ctrl",  modBit=MOD_LCTRL,  w=1.5f, color=KC.MOD, isMod=true, noRepeat=true),
    Key("Win",   modBit=MOD_LGUI,   w=1.2f, color=KC.MOD, isMod=true, noRepeat=true),
    Key("Alt",   modBit=MOD_LALT,   w=1.2f, color=KC.MOD, isMod=true, noRepeat=true),
    Key("Space", code=0x2C, w=5.0f),
    Key("AltGr", modBit=MOD_RALT,   w=1.2f, color=KC.MOD, isMod=true, noRepeat=true),
    Key("Menu",  code=0x65, w=1.0f, color=KC.MOD, noRepeat=true),
    Key("Fn",    w=1.0f, color=KC.FN, isFn=true, noRepeat=true),
    Key("Ctrl",  modBit=MOD_RCTRL,  w=1.5f, color=KC.MOD, isMod=true, noRepeat=true),
)

// Nav cluster rows
private val NAV_ROW1 = listOf(
    Key("Ins",  code=0x49, color=KC.SPECIAL),
    Key("Home", code=0x4A, color=KC.SPECIAL),
    Key("PgUp", code=0x4B, color=KC.SPECIAL),
)
private val NAV_ROW2 = listOf(
    Key("Del",  code=0x4C, color=KC.DANGER),
    Key("End",  code=0x4D, color=KC.SPECIAL),
    Key("PgDn", code=0x4E, color=KC.SPECIAL),
)

// Arrow keys
private val KEY_UP    = Key("↑", code=0x52, color=KC.SPECIAL)
private val KEY_LEFT  = Key("←", code=0x50, color=KC.SPECIAL)
private val KEY_DOWN  = Key("↓", code=0x51, color=KC.SPECIAL)
private val KEY_RIGHT = Key("→", code=0x4F, color=KC.SPECIAL)

// ═════════════════════════════════════════════════════════════════════════════
// Numpad
// ═════════════════════════════════════════════════════════════════════════════

private data class NumKey(
    val onLabel  : String,
    val offLabel : String,
    val code     : Int,
    val w        : Float   = 1f,
    val color    : KC      = KC.NORMAL,
    val isNumLock: Boolean = false,
    val noRepeat : Boolean = false,
)

private val NUMPAD_ROWS = listOf(
    listOf(
        NumKey("NumLk", "NumLk", 0x53, color=KC.MOD, isNumLock=true, noRepeat=true),
        NumKey("/",     "/",     0x54, color=KC.SPECIAL),
        NumKey("*",     "*",     0x55, color=KC.SPECIAL),
        NumKey("-",     "-",     0x56, color=KC.SPECIAL),
    ),
    listOf(
        NumKey("7", "Home", 0x5F),
        NumKey("8", "↑",   0x60),
        NumKey("9", "PgUp",0x61),
        NumKey("+", "+",   0x57, color=KC.ACCENT),
    ),
    listOf(
        NumKey("4", "←",  0x5C),
        NumKey("5", "·",  0x5D),
        NumKey("6", "→",  0x5E),
    ),
    listOf(
        NumKey("1", "End", 0x59),
        NumKey("2", "↓",  0x5A),
        NumKey("3", "PgDn",0x5B),
        NumKey("↵", "↵",  0x58, color=KC.ACCENT),
    ),
    listOf(
        NumKey("0", "Ins", 0x62, w=2f),
        NumKey(".", "Del", 0x63),
    ),
)

// ═════════════════════════════════════════════════════════════════════════════
// Media keys
// ═════════════════════════════════════════════════════════════════════════════

private data class MKey(val icon: String, val label: String, val code: Int)

private val MEDIA_TRANSPORT = listOf(
    MKey("⏮", "Prev",  BleHidManager.CONSUMER_PREV_TRACK),
    MKey("⏯", "Play",  BleHidManager.CONSUMER_PLAY_PAUSE),
    MKey("⏭", "Next",  BleHidManager.CONSUMER_NEXT_TRACK),
    MKey("⏹", "Stop",  BleHidManager.CONSUMER_STOP),
)
private val MEDIA_VOLUME = listOf(
    MKey("🔇", "Mute",  BleHidManager.CONSUMER_MUTE),
    MKey("🔉", "Vol -", BleHidManager.CONSUMER_VOL_DOWN),
    MKey("🔊", "Vol +", BleHidManager.CONSUMER_VOL_UP),
)
private val MEDIA_BRIGHT = listOf(
    MKey("🔅", "Brt -", BleHidManager.CONSUMER_BRIGHTNESS_DOWN),
    MKey("🔆", "Brt +", BleHidManager.CONSUMER_BRIGHTNESS_UP),
)

private val SYSTEM_ROW1 = listOf(
    Key("Esc",   code=0x29, color=KC.DANGER,  noRepeat=true),
    Key("Tab",   code=0x2B, color=KC.MOD),
    Key("BkSp",  code=0x2A, color=KC.DANGER),
    Key("Del",   code=0x4C, color=KC.DANGER),
    Key("Enter", code=0x28, color=KC.ACCENT),
)
private val SYSTEM_ROW2 = listOf(
    Key("PrtSc", code=0x46, color=KC.SPECIAL, noRepeat=true),
    Key("ScrLk", code=0x47, color=KC.SPECIAL, noRepeat=true, isScroll=true),
    Key("Pause", code=0x48, color=KC.SPECIAL, noRepeat=true),
    Key("Ins",   code=0x49, color=KC.SPECIAL),
    Key("Menu",  code=0x65, color=KC.MOD,     noRepeat=true),
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

// ═════════════════════════════════════════════════════════════════════════════
// KBtn — single key composable with repeat support
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun KBtn(
    key       : Key,
    modifier  : Modifier,
    h         : Dp,
    active    : Boolean = false,
    topLabel  : String  = "",
    mainLabel : String,
    subLabel  : String  = "",
    onPress   : () -> Unit,
    scrollable: Boolean = false,
) {
    val haptic  = LocalHapticFeedback.current
    val scope   = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    var holdJob by remember { mutableStateOf<Job?>(null) }

    val bg by animateColorAsState(
        keyBg(key.color, active, pressed), tween(70), label = "bg"
    )
    val sc by animateFloatAsState(
        if (pressed) 0.93f else 1f, tween(55), label = "sc"
    )

    Box(
        modifier = modifier
            .height(h)
            .scale(sc)
            .padding(1.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Brush.verticalGradient(listOf(bg.copy(alpha = 0.85f), bg)))
            .border(
                width = if (active) 1.5.dp else 0.5.dp,
                color = if (active) Color(0xFF4A90D9) else Color.White.copy(0.08f),
                shape = RoundedCornerShape(5.dp)
            )
            .then(
                if (!scrollable) {
                    Modifier.pointerInput(key) {
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPress()
                                if (key.shouldRepeat()) {
                                    holdJob = scope.launch {
                                        delay(400)
                                        while (isActive) {
                                            onPress()
                                            delay(50)
                                        }
                                    }
                                }
                                tryAwaitRelease()
                                pressed = false
                                holdJob?.cancel()
                                holdJob = null
                            }
                        )
                    }
                } else {
                    Modifier.pointerInput(key) {
                        val slop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downPos = down.position
                            var wasDrag = false
                            var repeatJob: Job? = null

                            repeatJob = scope.launch {
                                delay(400)
                                while (isActive && !wasDrag) {
                                    onPress()
                                    delay(50)
                                }
                            }

                            var isDown = true
                            while (isDown) {
                                val event = awaitPointerEvent()
                                for (change in event.changes) {
                                    if (!change.pressed) {
                                        isDown = false
                                        break
                                    }
                                    val dist = (change.position - downPos).getDistance()
                                    if (dist > slop) {
                                        wasDrag = true
                                        repeatJob?.cancel()
                                        repeatJob = null
                                        pressed = false
                                        isDown = false
                                        break
                                    }
                                }
                            }

                            repeatJob?.cancel()

                            if (!wasDrag) {
                                pressed = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPress()
                                scope.launch {
                                    delay(80)
                                    pressed = false
                                }
                            } else {
                                pressed = false
                            }
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 1.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (topLabel.isNotEmpty()) {
                Text(
                    topLabel,
                    fontSize   = 7.sp,
                    color      = Color.White.copy(alpha = 0.30f),
                    modifier   = Modifier.fillMaxWidth(),
                    lineHeight = 7.sp,
                    textAlign  = TextAlign.Start,
                )
            } else {
                Spacer(Modifier.height(7.sp.value.dp))
            }
            Box(
                modifier         = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    mainLabel,
                    color      = keyFg(key.color, active),
                    fontSize   = when {
                        mainLabel.length > 5 -> 7.sp
                        mainLabel.length > 3 -> 9.sp
                        mainLabel.length > 2 -> 10.sp
                        else                 -> 12.sp
                    },
                    fontWeight  = if (key.color == KC.ACCENT || active)
                        FontWeight.Bold else FontWeight.Medium,
                    textAlign   = TextAlign.Center,
                    maxLines    = 1,
                    overflow    = TextOverflow.Clip,
                )
            }
            if (subLabel.isNotEmpty()) {
                Text(
                    subLabel,
                    fontSize  = 6.sp,
                    color     = Color(0xFFA5D6A7).copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    maxLines  = 1,
                )
            } else {
                Spacer(Modifier.height(6.sp.value.dp))
            }
        }
        if (active) {
            Box(
                Modifier
                    .size(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF4FC3F7))
                    .align(Alignment.BottomCenter)
                    .offset(y = (-2).dp)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// ResponsiveRow — renders a list of Keys as a weight-based Row
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun ResponsiveRow(
    keys    : List<Key>,
    height  : Dp,
    st      : KbState,
    onClick : (Key) -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        keys.forEach { k ->
            val active    = isKeyActive(k, st)
            val mainLabel = displayMain(k, st)
            val topLabel  = displayTop(k, st)
            val subLabel  = if (st.fn && k.fnLabel.isNotEmpty()) k.fnLabel else ""
            KBtn(
                key       = k,
                modifier  = Modifier.weight(k.w),
                h         = height,
                active    = active,
                topLabel  = topLabel,
                mainLabel = mainLabel,
                subLabel  = subLabel,
                onPress   = { onClick(k) },
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// State helpers
// ═════════════════════════════════════════════════════════════════════════════

private fun isKeyActive(k: Key, st: KbState): Boolean = when {
    k.isCaps      -> st.caps
    k.isNum       -> st.numLock
    k.isFn        -> st.fn
    k.isScroll    -> st.scrollLk
    k.modBit == MOD_LSHIFT -> st.lShift
    k.modBit == MOD_RSHIFT -> st.rShift
    k.modBit == MOD_LCTRL  -> st.lCtrl
    k.modBit == MOD_RCTRL  -> st.rCtrl
    k.modBit == MOD_LALT   -> st.lAlt
    k.modBit == MOD_RALT   -> st.rAlt
    k.modBit == MOD_LGUI   -> st.lGui
    k.modBit == MOD_RGUI   -> st.rGui
    k.code == 0x49 && !k.isMod -> !st.insertMode  // Insert overwrite active
    else          -> false
}

private fun displayMain(k: Key, st: KbState): String {
    if (k.label.isEmpty()) return "Space"
    if (k.isMod || k.isCaps || k.isFn || k.isNum || k.isScroll) return k.label
    val isLetter = k.label.length == 1 && k.label[0].isLetter()
    return when {
        st.altGr && k.altGr.isNotEmpty() -> k.altGr
        isLetter && st.isUpperCase()      -> k.label.uppercase()
        st.shift && k.shift.isNotEmpty()  -> k.shift
        else                              -> k.label
    }
}

private fun displayTop(k: Key, st: KbState): String {
    if (k.isMod || k.isCaps || k.isFn || k.isNum || k.isScroll) return ""
    if (k.label.length == 1 && k.shift.isNotEmpty()) return k.shift
    return ""
}

// ═════════════════════════════════════════════════════════════════════════════
// KeyboardScreen
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun KeyboardScreen(
    isReady      : Boolean,
    onSendKey    : (modifiers: Int, keyCodes: List<Int>) -> Unit,
    onConsumerKey: (Int) -> Unit,
    onTypeText   : (String) -> Unit,
) {
    var st       by remember { mutableStateOf(KbState()) }
    var typeText by remember { mutableStateOf("") }
    val scope    = rememberCoroutineScope()

    // ── Core key handler ─────────────────────────────────────────────────────
    fun handleKey(key: Key) {
        when {
            key.isCaps -> {
                st = st.copy(caps = !st.caps, lastKey = "CapsLk")
                onSendKey(0, listOf(0x39))
                scope.launch { delay(60); onSendKey(0, emptyList()) }
            }
            key.isNum -> {
                st = st.copy(numLock = !st.numLock, lastKey = "NumLk")
                onSendKey(0, listOf(0x53))
                scope.launch { delay(60); onSendKey(0, emptyList()) }
            }
            key.isScroll -> {
                st = st.copy(scrollLk = !st.scrollLk, lastKey = "ScrLk")
                onSendKey(0, listOf(0x47))
                scope.launch { delay(60); onSendKey(0, emptyList()) }
            }
            key.isFn -> {
                st = st.copy(fn = !st.fn)
            }
            key.code == 0x49 && !key.isMod -> {
                // Insert key toggles overwrite mode
                st = st.copy(insertMode = !st.insertMode, lastKey = "Ins")
                onSendKey(st.modByte(), listOf(0x49))
                scope.launch {
                    delay(60)
                    onSendKey(0, emptyList())
                    st = st.releaseMods()
                }
            }
            key.isMod -> {
                st = when (key.modBit) {
                    MOD_LSHIFT -> st.copy(lShift = !st.lShift)
                    MOD_RSHIFT -> st.copy(rShift = !st.rShift)
                    MOD_LCTRL  -> st.copy(lCtrl  = !st.lCtrl)
                    MOD_RCTRL  -> st.copy(rCtrl  = !st.rCtrl)
                    MOD_LALT   -> st.copy(lAlt   = !st.lAlt)
                    MOD_RALT   -> st.copy(rAlt   = !st.rAlt)
                    MOD_LGUI   -> st.copy(lGui   = !st.lGui)
                    MOD_RGUI   -> st.copy(rGui   = !st.rGui)
                    else       -> st
                }
                onSendKey(st.modByte(), emptyList())
            }

            // Special case: if Tab is pressed with modifiers, release only Tab, keep modifiers held
            key.code == 0x2B -> {
                val mod = st.modByte()
                val keepModifiersHeld = st.anyMod

                val label = buildString {
                    append(st.modPrefix())
                    append(key.label.ifEmpty { "Space" })
                }

                onSendKey(mod, listOf(key.code))
                st = st.copy(lastKey = label)

                scope.launch {
                    delay(60)
                    if (keepModifiersHeld) {
                        // Release only Tab, keep currently active modifiers held
                        onSendKey(st.modByte(), emptyList())
                    } else {
                        // No modifier was active, behave normally
                        onSendKey(0, emptyList())
                        st = st.releaseMods()
                    }
                }
            }

            key.code != 0 -> {
                var mod      = st.modByte()
                val isLetter = key.label.length == 1 && key.label[0].isLetter()
                if (isLetter) {
                    val needShift = st.caps xor st.shift
                    mod = mod and (MOD_LSHIFT or MOD_RSHIFT).inv()
                    if (needShift) mod = mod or MOD_LSHIFT
                }
                val label = buildString {
                    append(st.modPrefix())
                    append(key.label.ifEmpty { "Space" })
                }
                onSendKey(mod, listOf(key.code))
                st = st.copy(lastKey = label)
                scope.launch {
                    delay(60)
                    onSendKey(0, emptyList())
                    st = st.releaseMods()
                }
            }
        }
    }

    // ── Numpad handler ───────────────────────────────────────────────────────
    fun handleNumKey(nk: NumKey) {
        if (nk.isNumLock) {
            st = st.copy(numLock = !st.numLock, lastKey = "NumLk")
            onSendKey(0, listOf(0x53))
            scope.launch { delay(60); onSendKey(0, emptyList()) }
            return
        }
        val effective = if (st.shift) !st.numLock else st.numLock
        st = st.copy(lastKey = if (effective) nk.onLabel else nk.offLabel)
        onSendKey(st.modByte(), listOf(nk.code))
        scope.launch {
            delay(60)
            onSendKey(0, emptyList())
            st = st.releaseMods()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080F18))
    ) {

        // ── Inner tab bar (Keys / Nav / Media) ────────────────────────────────
        val tabLabels = listOf("⌨ Keys", "↕ Nav+Num", "🎵 Media")
        TabRow(
            selectedTabIndex = st.tab,
            containerColor   = Color(0xFF050C14),
            contentColor     = Color.White,
            indicator = { tabPositions ->
                if (st.tab < tabPositions.size) {
                    val t = tabPositions[st.tab]
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .wrapContentSize(Alignment.BottomStart)
                            .offset(x = t.left)
                            .width(t.width)
                            .height(2.dp)
                            .background(Color(0xFF4A90D9), RoundedCornerShape(1.dp))
                    )
                }
            }
        ) {
            tabLabels.forEachIndexed { i, title ->
                Tab(
                    selected = st.tab == i,
                    onClick  = { st = st.copy(tab = i) },
                    text = {
                        Text(
                            title, fontSize = 12.sp,
                            color      = if (st.tab == i) Color(0xFF90CAF9)
                                         else Color(0xFF546E7A),
                            fontWeight = if (st.tab == i) FontWeight.SemiBold
                                         else FontWeight.Normal,
                        )
                    }
                )
            }
        }

        // ── Status / modifier bar ─────────────────────────────────────────────
        KbStatusBar(
            st         = st,
            isReady    = isReady,
            onClearMods = {
                st = st.releaseMods().copy(lastKey = "")
                onSendKey(0, emptyList())
            }
        )

        // ── Not-ready gate ────────────────────────────────────────────────────
        if (!isReady) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF080F18)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("⏳", fontSize = 36.sp)
                    Text(
                        "Host not connected",
                        color      = Color.White,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Go to Status tab → Start BLE HID\nPair from host Bluetooth settings",
                        color     = Color.Gray,
                        fontSize  = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            return@Column
        }

        // ══════════════════════════════════════════════════════════════════════
        // TAB 0 — Full QWERTY keyboard
        // ══════════════════════════════════════════════════════════════════════
        when (st.tab) {
            0 -> Column(
                modifier            = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF080F18))
                    .padding(horizontal = 2.dp, vertical = 3.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ResponsiveRow(ROW_FN,     32.dp, st) { handleKey(it) }
                HorizontalDivider(color = Color.White.copy(0.04f), thickness = 1.dp)
                ResponsiveRow(ROW_NUM,    42.dp, st) { handleKey(it) }
                ResponsiveRow(ROW_QWERTY, 42.dp, st) { handleKey(it) }
                ResponsiveRow(ROW_HOME,   42.dp, st) { handleKey(it) }
                ResponsiveRow(ROW_ALPHA,  42.dp, st) { handleKey(it) }
                ResponsiveRow(ROW_MODS,   42.dp, st) { handleKey(it) }
            }

            // ══════════════════════════════════════════════════════════════════
            // TAB 1 — Navigation + Numpad + Type text
            // ══════════════════════════════════════════════════════════════════
            1 -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF080F18))
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // ── Nav cluster + Arrows side by side ─────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Navigation cluster
                    KbCard("Navigation", modifier = Modifier.weight(1f)) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                NAV_ROW1.forEach { k ->
                                    KBtn(
                                        key        = k,
                                        modifier   = Modifier.weight(1f),
                                        h          = 46.dp,
                                        active     = isKeyActive(k, st),
                                        mainLabel  = displayMain(k, st),
                                        onPress    = { handleKey(k) },
                                        scrollable = true,
                                    )
                                }
                            }
                            Row(Modifier.fillMaxWidth()) {
                                NAV_ROW2.forEach { k ->
                                    KBtn(
                                        key        = k,
                                        modifier   = Modifier.weight(1f),
                                        h          = 46.dp,
                                        active     = isKeyActive(k, st),
                                        mainLabel  = displayMain(k, st),
                                        onPress    = { handleKey(k) },
                                        scrollable = true,
                                    )
                                }
                            }
                        }
                    }

                    // Arrow keys
                    KbCard("Arrows", modifier = Modifier.weight(1f)) {
                        Column(
                            modifier            = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            // Up arrow centred
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                KBtn(
                                    key        = KEY_UP,
                                    modifier   = Modifier.weight(1f),
                                    h          = 46.dp,
                                    active     = false,
                                    mainLabel  = "↑",
                                    onPress    = { handleKey(KEY_UP) },
                                    scrollable = true,
                                )
                            }
                            // Left / Down / Right
                            Row(Modifier.fillMaxWidth()) {
                                listOf(KEY_LEFT, KEY_DOWN, KEY_RIGHT).forEach { k ->
                                    KBtn(
                                        key        = k,
                                        modifier   = Modifier.weight(1f),
                                        h          = 46.dp,
                                        active     = false,
                                        mainLabel  = k.label,
                                        onPress    = { handleKey(k) },
                                        scrollable = true,
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Insert mode toggle ─────────────────────────────────────────
                Surface(
                    color    = if (st.insertMode) Color(0xFF0D1F0D) else Color(0xFF2A1800),
                    shape    = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            st = st.copy(insertMode = !st.insertMode)
                            onSendKey(0, listOf(0x49))
                            scope.launch { delay(60); onSendKey(0, emptyList()) }
                        }
                ) {
                    Row(
                        modifier              = Modifier.padding(12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            if (st.insertMode) "INSERT" else "OVERWRITE",
                            color      = if (st.insertMode) Color(0xFF81C784)
                                         else Color(0xFFFFB74D),
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (st.insertMode) "Tap to switch → Overwrite mode"
                            else               "Tap to switch → Insert mode",
                            color    = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                // ── Numpad ─────────────────────────────────────────────────────
                KbCard("Numpad") {
                    // NumLock status
                    Surface(
                        color    = if (st.numLock) Color(0xFF1565C0) else Color(0xFF4A1800),
                        shape    = RoundedCornerShape(5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (st.numLock) "NumLock ON — typing numbers"
                            else            "NumLock OFF — navigation mode",
                            color    = Color.White.copy(0.85f),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))

                    // Numpad rows
                    NUMPAD_ROWS.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            row.forEach { nk ->
                                val effective = if (st.shift) !st.numLock else st.numLock
                                val mainLbl   = if (nk.isNumLock) "NumLk"
                                               else if (effective) nk.onLabel
                                               else nk.offLabel
                                val topLbl    = if (!nk.isNumLock && nk.onLabel != nk.offLabel)
                                                    if (effective) nk.offLabel else nk.onLabel
                                               else ""
                                val isActive  = nk.isNumLock && st.numLock

                                Box(
                                    modifier = Modifier
                                        .weight(nk.w)
                                        .height(52.dp)
                                        .padding(1.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    keyBg(nk.color, isActive, false).copy(0.85f),
                                                    keyBg(nk.color, isActive, false)
                                                )
                                            )
                                        )
                                        .border(
                                            if (isActive) 1.5.dp else 0.5.dp,
                                            if (isActive) Color(0xFF4A90D9)
                                            else Color.White.copy(0.08f),
                                            RoundedCornerShape(5.dp)
                                        )
                                        .pointerInput(nk) {
                                            val slop = viewConfiguration.touchSlop
                                            awaitEachGesture {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                val downPos = down.position
                                                var wasDrag = false

                                                var isDown = true
                                                while (isDown) {
                                                    val event = awaitPointerEvent()
                                                    for (change in event.changes) {
                                                        if (!change.pressed) {
                                                            isDown = false
                                                            break
                                                        }
                                                        val dist = (change.position - downPos).getDistance()
                                                        if (dist > slop) {
                                                            wasDrag = true
                                                            isDown = false
                                                            break
                                                        }
                                                    }
                                                }

                                                if (!wasDrag) {
                                                    handleNumKey(nk)
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        if (topLbl.isNotEmpty()) {
                                            Text(
                                                topLbl,
                                                fontSize  = 7.sp,
                                                color     = Color.White.copy(0.28f),
                                                textAlign = TextAlign.Center,
                                                lineHeight= 7.sp,
                                            )
                                        }
                                        Text(
                                            mainLbl,
                                            fontSize   = when {
                                                mainLbl.length > 4 -> 8.sp
                                                mainLbl.length > 2 -> 10.sp
                                                else               -> 13.sp
                                            },
                                            color      = keyFg(nk.color, isActive),
                                            fontWeight = FontWeight.Medium,
                                            textAlign  = TextAlign.Center,
                                            maxLines   = 1,
                                        )
                                    }
                                    if (isActive) {
                                        Box(
                                            Modifier
                                                .size(5.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color(0xFF4FC3F7))
                                                .align(Alignment.BottomCenter)
                                                .offset(y = (-2).dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Type & Send text ───────────────────────────────────────────
                KbCard("Type & Send Text") {
                    OutlinedTextField(
                        value         = typeText,
                        onValueChange = { typeText = it },
                        modifier      = Modifier.fillMaxWidth(),
                        placeholder   = { Text("Enter text to send…", color = Color.Gray) },
                        maxLines      = 4,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedTextColor     = Color.White,
                            unfocusedTextColor   = Color.White,
                            focusedBorderColor   = Color(0xFF4A90D9),
                            unfocusedBorderColor = Color.White.copy(0.2f),
                            cursorColor          = Color(0xFF4A90D9),
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick  = {
                                if (typeText.isNotBlank()) {
                                    onTypeText(typeText)
                                    typeText = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1565C0))
                        ) { Text("Send Text") }
                        OutlinedButton(
                            onClick  = { typeText = "" },
                            modifier = Modifier.weight(0.4f),
                            border   = androidx.compose.foundation.BorderStroke(
                                1.dp, Color.White.copy(0.2f))
                        ) { Text("Clear", color = Color.White) }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // ══════════════════════════════════════════════════════════════════
            // TAB 2 — Media + System keys
            // ══════════════════════════════════════════════════════════════════
            2 -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF080F18))
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                // ── Transport controls ─────────────────────────────────────────
                KbCard("Transport") {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MEDIA_TRANSPORT.forEach { mk ->
                            MediaKeyBtn(
                                icon     = mk.icon,
                                label    = mk.label,
                                modifier = Modifier.weight(1f),
                                h        = 64.dp,
                                onClick  = {
                                    onConsumerKey(mk.code)
                                    st = st.copy(lastKey = mk.label)
                                }
                            )
                        }
                    }
                }

                // ── Volume ─────────────────────────────────────────────────────
                KbCard("Volume & Brightness") {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (MEDIA_VOLUME + MEDIA_BRIGHT).forEach { mk ->
                            MediaKeyBtn(
                                icon     = mk.icon,
                                label    = mk.label,
                                modifier = Modifier.weight(1f),
                                h        = 56.dp,
                                onClick  = {
                                    onConsumerKey(mk.code)
                                    st = st.copy(lastKey = mk.label)
                                }
                            )
                        }
                    }
                }

                // ── System keys row 1 ──────────────────────────────────────────
                KbCard("System Keys") {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            SYSTEM_ROW1.forEach { k ->
                                KBtn(
                                    key        = k,
                                    modifier   = Modifier.weight(1f),
                                    h          = 46.dp,
                                    active     = isKeyActive(k, st),
                                    mainLabel  = displayMain(k, st),
                                    onPress    = { handleKey(k) },
                                    scrollable = true,
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth()) {
                            SYSTEM_ROW2.forEach { k ->
                                KBtn(
                                    key        = k,
                                    modifier   = Modifier.weight(1f),
                                    h          = 46.dp,
                                    active     = isKeyActive(k, st),
                                    mainLabel  = displayMain(k, st),
                                    onPress    = { handleKey(k) },
                                    scrollable = true,
                                )
                            }
                        }
                    }
                }

                // ── Quick modifier row ─────────────────────────────────────────
                KbCard("Quick Modifiers") {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            Key("Ctrl",  modBit=MOD_LCTRL,  color=KC.MOD, isMod=true, noRepeat=true),
                            Key("Shift", modBit=MOD_LSHIFT, color=KC.MOD, isMod=true, noRepeat=true),
                            Key("Alt",   modBit=MOD_LALT,   color=KC.MOD, isMod=true, noRepeat=true),
                            Key("Win",   modBit=MOD_LGUI,   color=KC.MOD, isMod=true, noRepeat=true),
                            Key("AltGr", modBit=MOD_RALT,   color=KC.MOD, isMod=true, noRepeat=true),
                        ).forEach { k ->
                            KBtn(
                                key        = k,
                                modifier   = Modifier.weight(1f),
                                h          = 44.dp,
                                active     = isKeyActive(k, st),
                                mainLabel  = k.label,
                                onPress    = { handleKey(k) },
                                scrollable = true,
                            )
                        }
                    }
                    // Clear mods button
                    if (st.anyMod) {
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick  = {
                                st = st.releaseMods().copy(lastKey = "")
                                onSendKey(0, emptyList())
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border   = androidx.compose.foundation.BorderStroke(
                                1.dp, Color(0xFFEF9A9A).copy(0.5f))
                        ) {
                            Text("✕ Clear All Modifiers",
                                color = Color(0xFFEF9A9A), fontSize = 12.sp)
                        }
                    }
                }

                // ── Arrow keys (also on media tab for convenience) ─────────────
                KbCard("Arrow Keys") {
                    Column(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(0.66f),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            KBtn(
                                key        = KEY_UP,
                                modifier   = Modifier.weight(1f),
                                h          = 46.dp,
                                mainLabel  = "↑",
                                onPress    = { handleKey(KEY_UP) },
                                scrollable = true,
                            )
                        }
                        Row(Modifier.fillMaxWidth(0.66f)) {
                            listOf(KEY_LEFT, KEY_DOWN, KEY_RIGHT).forEach { k ->
                                KBtn(
                                    key        = k,
                                    modifier   = Modifier.weight(1f),
                                    h          = 46.dp,
                                    mainLabel  = k.label,
                                    onPress    = { handleKey(k) },
                                    scrollable = true,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Status / modifier bar
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun KbStatusBar(
    st         : KbState,
    isReady    : Boolean,
    onClearMods: () -> Unit,
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
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            listOf(
                "CAPS"  to st.caps,
                "NUM"   to st.numLock,
                "SCR"   to st.scrollLk,
                "SHF"   to st.shift,
                "CTL"   to st.ctrl,
                "ALT"   to st.alt,
                "AGR"   to st.altGr,
                "WIN"   to st.gui,
                "FN"    to st.fn,
                "OVR"   to !st.insertMode,
            ).forEach { (lbl, on) -> LedBadge(lbl, on) }
            Spacer(Modifier.weight(1f))
            Text(
                if (isReady) "● Ready" else "○ Offline",
                fontSize = 9.sp,
                color    = if (isReady) Color(0xFF81C784) else Color(0xFFEF9A9A)
            )
        }

        // Combo preview
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
                        text = when {
                            st.anyMod && st.lastKey.isEmpty() ->
                                "▶ ${st.modPrefix().trimEnd('+')}+ …waiting"
                            combo.isEmpty() -> "Ready…"
                            else            -> "⌨ $combo"
                        },
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
// KbCard — dark themed card
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun KbCard(
    title   : String,
    modifier: Modifier = Modifier,
    content : @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF111C28)),
        shape    = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier            = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
// MediaKeyBtn — large icon + label button for media controls
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun MediaKeyBtn(
    icon      : String,
    label     : String,
    modifier  : Modifier,
    h         : Dp,
    onClick   : () -> Unit,
) {
    val haptic  = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val scope   = rememberCoroutineScope()
    val bg by animateColorAsState(
        if (pressed) Color(0xFF1B3A5F) else Color(0xFF0E1C2A),
        tween(60), label = "mb"
    )
    Column(
        modifier = modifier
            .height(h)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(0.5.dp, Color.White.copy(0.08f), RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPos = down.position
                    var wasDrag = false

                    var isDown = true
                    while (isDown) {
                        val event = awaitPointerEvent()
                        for (change in event.changes) {
                            if (!change.pressed) {
                                isDown = false
                                break
                            }
                            val dist = (change.position - downPos).getDistance()
                            if (dist > slop) {
                                wasDrag = true
                                isDown = false
                                break
                            }
                        }
                    }

                    if (!wasDrag) {
                        pressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                        scope.launch {
                            delay(80)
                            pressed = false
                        }
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            icon,
            fontSize  = 22.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            label,
            fontSize  = 8.sp,
            color     = Color(0xFF78909C),
            textAlign = TextAlign.Center,
            maxLines  = 1,
            modifier  = Modifier.padding(horizontal = 2.dp),
            overflow  = TextOverflow.Ellipsis,
        )
    }
}