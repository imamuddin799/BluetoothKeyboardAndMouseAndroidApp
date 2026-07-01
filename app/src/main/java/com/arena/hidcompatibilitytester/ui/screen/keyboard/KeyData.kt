package com.arena.hidcompatibilitytester.ui.screen.keyboard

import com.arena.hidcompatibilitytester.bluetooth.BleHidManager

// ═════════════════════════════════════════════════════════════════════════════
// HID Modifier bits
// ═════════════════════════════════════════════════════════════════════════════

internal const val MOD_LCTRL  = 0x01
internal const val MOD_LSHIFT = 0x02
internal const val MOD_LALT   = 0x04
internal const val MOD_LGUI   = 0x08
internal const val MOD_RCTRL  = 0x10
internal const val MOD_RSHIFT = 0x20
internal const val MOD_RALT   = 0x40
internal const val MOD_RGUI   = 0x80

// ═════════════════════════════════════════════════════════════════════════════
// Key color categories
// ═════════════════════════════════════════════════════════════════════════════

internal enum class KC { NORMAL, MOD, SPECIAL, ACCENT, DANGER }

// ═════════════════════════════════════════════════════════════════════════════
// Key model
// ═════════════════════════════════════════════════════════════════════════════

internal data class Key(
    val label    : String,
    val shift    : String  = "",
    val altGr    : String  = "",
    val w        : Float   = 1f,
    val code     : Int     = 0,
    val modBit   : Int     = 0,
    val color    : KC      = KC.NORMAL,
    val isMod    : Boolean = false,
    val isCaps   : Boolean = false,
    val isNum    : Boolean = false,
    val isScroll : Boolean = false,
    val noRepeat : Boolean = false,
)

internal fun Key.shouldRepeat() =
    !noRepeat && !isMod && !isCaps && !isNum && !isScroll

// ═════════════════════════════════════════════════════════════════════════════
// Media key model
// ═════════════════════════════════════════════════════════════════════════════

internal data class MKey(val icon: String, val label: String, val code: Int)

// ═════════════════════════════════════════════════════════════════════════════
// Row definitions — Main keyboard
// ═════════════════════════════════════════════════════════════════════════════

internal val ROW_FN = listOf(
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

internal val ROW_NUM = listOf(
    Key("`","~",code=0x35), Key("1","!",code=0x1E), Key("2","@",code=0x1F),
    Key("3","#",code=0x20), Key("4","$",code=0x21), Key("5","%",code=0x22),
    Key("6","^",code=0x23), Key("7","&",code=0x24), Key("8","*",code=0x25),
    Key("9","(",code=0x26), Key("0",")",code=0x27), Key("-","_",code=0x2D),
    Key("=","+",code=0x2E), Key("⌫","",code=0x2A,w=2.0f,color=KC.DANGER),
)

internal val ROW_QWERTY = listOf(
    Key("Tab",code=0x2B,w=1.5f,color=KC.MOD),
    Key("Q","Q",code=0x14),Key("W","W",code=0x1A),Key("E","E",code=0x08),
    Key("R","R",code=0x15),Key("T","T",code=0x17),Key("Y","Y",code=0x1C),
    Key("U","U",code=0x18),Key("I","I",code=0x0C),Key("O","O",code=0x12),
    Key("P","P",code=0x13),Key("[","{",code=0x2F),Key("]","}",code=0x30),
    Key("\\","|",code=0x31,w=1.5f),
)

internal val ROW_HOME = listOf(
    Key("Caps",code=0x39,w=1.75f,color=KC.MOD,isCaps=true,noRepeat=true),
    Key("A","A",code=0x04),Key("S","S",code=0x16),Key("D","D",code=0x07),
    Key("F","F",code=0x09),Key("G","G",code=0x0A),Key("H","H",code=0x0B),
    Key("J","J",code=0x0D),Key("K","K",code=0x0E),Key("L","L",code=0x0F),
    Key(";",":",code=0x33),Key("'","\"",code=0x34),
    Key("↵","",code=0x28,w=2.25f,color=KC.ACCENT),
)

internal val ROW_ALPHA = listOf(
    Key("⇧",modBit=MOD_LSHIFT,w=2.25f,color=KC.MOD,isMod=true,noRepeat=true),
    Key("Z","Z",code=0x1D),Key("X","X",code=0x1B),Key("C","C",code=0x06),
    Key("V","V",code=0x19),Key("B","B",code=0x05),Key("N","N",code=0x11),
    Key("M","M",code=0x10),Key(",","<",code=0x36),Key(".",">" ,code=0x37),
    Key("/","?",code=0x38),
    Key("⇧",modBit=MOD_RSHIFT,w=2.75f,color=KC.MOD,isMod=true,noRepeat=true),
)

internal val ROW_MODS = listOf(
    Key("Ctrl", modBit=MOD_LCTRL, w=1.5f,color=KC.MOD,isMod=true,noRepeat=true),
    Key("Win",  modBit=MOD_LGUI,  w=1.2f,color=KC.MOD,isMod=true,noRepeat=true),
    Key("Alt",  modBit=MOD_LALT,  w=1.2f,color=KC.MOD,isMod=true,noRepeat=true),
    Key("Space",code=0x2C,w=4.0f),
    Key("AltGr",modBit=MOD_RALT,  w=1.2f,color=KC.MOD,isMod=true,noRepeat=true),
    Key("Menu", code=0x65,w=1.5f,color=KC.MOD,noRepeat=true),
    Key("Ctrl", modBit=MOD_RCTRL, w=1.5f,color=KC.MOD,isMod=true,noRepeat=true),
)

internal val ROW_MODS_COMPACT = listOf(
    Key("Ctrl", modBit=MOD_LCTRL, w=1.25f,color=KC.MOD,isMod=true,noRepeat=true),
    Key("Win",  modBit=MOD_LGUI,  w=1.0f,color=KC.MOD,isMod=true,noRepeat=true),
    Key("Alt",  modBit=MOD_LALT,  w=1.0f,color=KC.MOD,isMod=true,noRepeat=true),
    Key("Space",code=0x2C,w=4.5f),
    Key("AltGr",modBit=MOD_RALT,  w=1.0f,color=KC.MOD,isMod=true,noRepeat=true),
    Key("Menu", code=0x65,w=1.5f,color=KC.MOD,noRepeat=true),
    Key("Ctrl", modBit=MOD_RCTRL, w=1.25f,color=KC.MOD,isMod=true,noRepeat=true),
)

// ═════════════════════════════════════════════════════════════════════════════
// Navigation keys
// ═════════════════════════════════════════════════════════════════════════════

internal val NAV_ROW1 = listOf(
    Key("Ins", code=0x49,color=KC.SPECIAL),
    Key("Home",code=0x4A,color=KC.SPECIAL),
    Key("PgUp",code=0x4B,color=KC.SPECIAL),
)

internal val NAV_ROW2 = listOf(
    Key("Del", code=0x4C,color=KC.DANGER),
    Key("End", code=0x4D,color=KC.SPECIAL),
    Key("PgDn",code=0x4E,color=KC.SPECIAL),
)

internal val KEY_UP    = Key("↑",code=0x52,color=KC.SPECIAL)
internal val KEY_LEFT  = Key("←",code=0x50,color=KC.SPECIAL)
internal val KEY_DOWN  = Key("↓",code=0x51,color=KC.SPECIAL)
internal val KEY_RIGHT = Key("→",code=0x4F,color=KC.SPECIAL)

// ═════════════════════════════════════════════════════════════════════════════
// System key rows
// ═════════════════════════════════════════════════════════════════════════════

internal val SYSTEM_ROW1 = listOf(
    Key("Esc",  code=0x29,color=KC.DANGER, noRepeat=true),
    Key("Tab",  code=0x2B,color=KC.MOD),
    Key("BkSp", code=0x2A,color=KC.DANGER),
    Key("Del",  code=0x4C,color=KC.DANGER),
    Key("Enter",code=0x28,color=KC.ACCENT),
)

internal val SYSTEM_ROW2 = listOf(
    Key("PrtSc",code=0x46,color=KC.SPECIAL,noRepeat=true),
    Key("ScrLk",code=0x47,color=KC.SPECIAL,noRepeat=true,isScroll=true),
    Key("Pause",code=0x48,color=KC.SPECIAL,noRepeat=true),
    Key("Ins",  code=0x49,color=KC.SPECIAL),
    Key("Menu", code=0x65,color=KC.MOD,    noRepeat=true),
)

// ═════════════════════════════════════════════════════════════════════════════
// Quick modifier keys (for Nav/Media tabs)
// ═════════════════════════════════════════════════════════════════════════════

internal val QUICK_MODIFIERS = listOf(
    Key("Ctrl", modBit=MOD_LCTRL, color=KC.MOD,isMod=true,noRepeat=true),
    Key("Shift",modBit=MOD_LSHIFT,color=KC.MOD,isMod=true,noRepeat=true),
    Key("Alt",  modBit=MOD_LALT,  color=KC.MOD,isMod=true,noRepeat=true),
    Key("Win",  modBit=MOD_LGUI,  color=KC.MOD,isMod=true,noRepeat=true),
    Key("AltGr",modBit=MOD_RALT,  color=KC.MOD,isMod=true,noRepeat=true),
)

internal val QUICK_MODIFIERS_WITH_MENU = QUICK_MODIFIERS + listOf(
    Key("Menu", code=0x65,w=1.0f,color=KC.MOD,noRepeat=true),
)

// ═════════════════════════════════════════════════════════════════════════════
// Media key rows
// ═════════════════════════════════════════════════════════════════════════════

internal val MEDIA_TRANSPORT = listOf(
    MKey("⏮","Prev", BleHidManager.CONSUMER_PREV_TRACK),
    MKey("⏯","Play", BleHidManager.CONSUMER_PLAY_PAUSE),
    MKey("⏭","Next", BleHidManager.CONSUMER_NEXT_TRACK),
    MKey("⏹","Stop", BleHidManager.CONSUMER_STOP),
)

internal val MEDIA_VOLUME = listOf(
    MKey("🔇","Mute",  BleHidManager.CONSUMER_MUTE),
    MKey("🔉","Vol -", BleHidManager.CONSUMER_VOL_DOWN),
    MKey("🔊","Vol +", BleHidManager.CONSUMER_VOL_UP),
)

internal val MEDIA_BRIGHT = listOf(
    MKey("🔅","Brt -", BleHidManager.CONSUMER_BRIGHTNESS_DOWN),
    MKey("🔆","Brt +", BleHidManager.CONSUMER_BRIGHTNESS_UP),
)