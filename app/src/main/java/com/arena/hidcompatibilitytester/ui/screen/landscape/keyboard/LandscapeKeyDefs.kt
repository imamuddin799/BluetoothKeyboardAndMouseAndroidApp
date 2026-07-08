package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard

import com.arena.hidcompatibilitytester.bluetooth.BleHidManager

enum class LandscapeKC { NORMAL, MOD, SPECIAL, ACCENT, DANGER, MEDIA }

data class LandscapeKey(
    val label: String,
    val shifted: String = "",
    val code: Int = 0,
    val w: Float = 1f,
    val color: LandscapeKC = LandscapeKC.NORMAL,
    val isMod: Boolean = false,
    val modBit: Int = 0,
    val isCaps: Boolean = false,
    val noRepeat: Boolean = false,
    val isConsumer: Boolean = false,
    val consumerCode: Int = 0,
    val isNum: Boolean = false,
    val isScroll: Boolean = false,
    val mediaGroup: LandscapeMediaRowGroup? = null,
)

fun LandscapeKey.shouldRepeat() =
    !noRepeat && !isMod && !isCaps && !isNum && !isScroll

internal data class LandscapeMKey(val icon: String, val label: String, val code: Int)

// ═══════════════════════════════════════════════════════════════
// Row definitions — Main keyboard
// ═══════════════════════════════════════════════════════════════

internal val LANDSCAPE_ROW_FN = listOf(
    LandscapeKey("Esc", code = 0x29, w = 1.4f, color = LandscapeKC.DANGER, noRepeat = true),
    LandscapeKey("F1", code = 0x3A, color = LandscapeKC.SPECIAL),
    LandscapeKey("F2", code = 0x3B, color = LandscapeKC.SPECIAL),
    LandscapeKey("F3", code = 0x3C, color = LandscapeKC.SPECIAL),
    LandscapeKey("F4", code = 0x3D, color = LandscapeKC.SPECIAL),
    LandscapeKey("F5", code = 0x3E, color = LandscapeKC.SPECIAL),
    LandscapeKey("F6", code = 0x3F, color = LandscapeKC.SPECIAL),
    LandscapeKey("F7", code = 0x40, color = LandscapeKC.SPECIAL),
    LandscapeKey("F8", code = 0x41, color = LandscapeKC.SPECIAL),
    LandscapeKey("F9", code = 0x42, color = LandscapeKC.SPECIAL),
    LandscapeKey("F10", code = 0x43, color = LandscapeKC.SPECIAL),
    LandscapeKey("F11", code = 0x44, color = LandscapeKC.SPECIAL),
    LandscapeKey("F12", code = 0x45, color = LandscapeKC.SPECIAL),
    LandscapeKey("Del", code = 0x4C, w = 1.4f, color = LandscapeKC.DANGER),
)

internal val LANDSCAPE_ROW_NUM = listOf(
    LandscapeKey("`", "~", code = 0x35), LandscapeKey("1", "!", code = 0x1E), LandscapeKey("2", "@", code = 0x1F),
    LandscapeKey("3", "#", code = 0x20), LandscapeKey("4", "$", code = 0x21), LandscapeKey("5", "%", code = 0x22),
    LandscapeKey("6", "^", code = 0x23), LandscapeKey("7", "&", code = 0x24), LandscapeKey("8", "*", code = 0x25),
    LandscapeKey("9", "(", code = 0x26), LandscapeKey("0", ")", code = 0x27), LandscapeKey("-", "_", code = 0x2D),
    LandscapeKey("=", "+", code = 0x2E), LandscapeKey("⌫", "", code = 0x2A, w = 2.0f, color = LandscapeKC.DANGER),
)

internal val LANDSCAPE_ROW_QWERTY = listOf(
    LandscapeKey("Tab", code = 0x2B, w = 1.5f, color = LandscapeKC.MOD),
    LandscapeKey("Q", "Q", code = 0x14), LandscapeKey("W", "W", code = 0x1A), LandscapeKey("E", "E", code = 0x08),
    LandscapeKey("R", "R", code = 0x15), LandscapeKey("T", "T", code = 0x17), LandscapeKey("Y", "Y", code = 0x1C),
    LandscapeKey("U", "U", code = 0x18), LandscapeKey("I", "I", code = 0x0C), LandscapeKey("O", "O", code = 0x12),
    LandscapeKey("P", "P", code = 0x13), LandscapeKey("[", "{", code = 0x2F), LandscapeKey("]", "}", code = 0x30),
    LandscapeKey("\\", "|", code = 0x31, w = 1.5f),
)

internal val LANDSCAPE_ROW_HOME = listOf(
    LandscapeKey("Caps", code = 0x39, w = 1.75f, color = LandscapeKC.MOD, isCaps = true, noRepeat = true),
    LandscapeKey("A", "A", code = 0x04), LandscapeKey("S", "S", code = 0x16), LandscapeKey("D", "D", code = 0x07),
    LandscapeKey("F", "F", code = 0x09), LandscapeKey("G", "G", code = 0x0A), LandscapeKey("H", "H", code = 0x0B),
    LandscapeKey("J", "J", code = 0x0D), LandscapeKey("K", "K", code = 0x0E), LandscapeKey("L", "L", code = 0x0F),
    LandscapeKey(";", ":", code = 0x33), LandscapeKey("'", "\"", code = 0x34),
    LandscapeKey("↵", "", code = 0x28, w = 2.25f, color = LandscapeKC.ACCENT),
)

internal val LANDSCAPE_ROW_ALPHA = listOf(
    LandscapeKey("⇧", modBit = LANDSCAPE_MOD_LSHIFT, w = 2.25f, color = LandscapeKC.MOD, isMod = true, noRepeat = true),
    LandscapeKey("Z", "Z", code = 0x1D), LandscapeKey("X", "X", code = 0x1B), LandscapeKey("C", "C", code = 0x06),
    LandscapeKey("V", "V", code = 0x19), LandscapeKey("B", "B", code = 0x05), LandscapeKey("N", "N", code = 0x11),
    LandscapeKey("M", "M", code = 0x10), LandscapeKey(",", "<", code = 0x36), LandscapeKey(".", ">", code = 0x37),
    LandscapeKey("/", "?", code = 0x38),
    LandscapeKey("⇧", modBit = LANDSCAPE_MOD_RSHIFT, w = 2.75f, color = LandscapeKC.MOD, isMod = true, noRepeat = true),
)

internal val LANDSCAPE_ROW_MODS = listOf(
    LandscapeKey("Ctrl", modBit = LANDSCAPE_MOD_LCTRL, w = 1.5f, color = LandscapeKC.MOD, isMod = true, noRepeat = true),
    LandscapeKey("Win", modBit = LANDSCAPE_MOD_LGUI, w = 1.2f, color = LandscapeKC.MOD, isMod = true, noRepeat = true),
    LandscapeKey("Alt", modBit = LANDSCAPE_MOD_LALT, w = 1.2f, color = LandscapeKC.MOD, isMod = true, noRepeat = true),
    LandscapeKey("Space", code = 0x2C, w = 4.0f),
    LandscapeKey("AltGr", modBit = LANDSCAPE_MOD_RALT, w = 1.2f, color = LandscapeKC.MOD, isMod = true, noRepeat = true),
    LandscapeKey("Menu", code = 0x65, w = 1.5f, color = LandscapeKC.MOD, noRepeat = true),
    LandscapeKey("Ctrl", modBit = LANDSCAPE_MOD_RCTRL, w = 1.5f, color = LandscapeKC.MOD, isMod = true, noRepeat = true),
)

internal val LANDSCAPE_ROW_MODS_COMPACT = listOf(
    LandscapeKey("Ctrl", modBit = LANDSCAPE_MOD_LCTRL, w = 1.25f, color = LandscapeKC.MOD, isMod = true, noRepeat = true),
    LandscapeKey("Win", modBit = LANDSCAPE_MOD_LGUI, w = 1.0f, color = LandscapeKC.MOD, isMod = true, noRepeat = true),
    LandscapeKey("Alt", modBit = LANDSCAPE_MOD_LALT, w = 1.0f, color = LandscapeKC.MOD, isMod = true, noRepeat = true),
    LandscapeKey("Space", code = 0x2C, w = 4.5f),
    LandscapeKey("AltGr", modBit = LANDSCAPE_MOD_RALT, w = 1.0f, color = LandscapeKC.MOD, isMod = true, noRepeat = true),
    LandscapeKey("Menu", code = 0x65, w = 1.5f, color = LandscapeKC.MOD, noRepeat = true),
    LandscapeKey("Ctrl", modBit = LANDSCAPE_MOD_RCTRL, w = 1.25f, color = LandscapeKC.MOD, isMod = true, noRepeat = true),
)

// ═══════════════════════════════════════════════════════════════
// Navigation keys
// ═══════════════════════════════════════════════════════════════

internal val LANDSCAPE_NAV_ROW1 = listOf(
    LandscapeKey("Ins", code = 0x49, color = LandscapeKC.SPECIAL),
    LandscapeKey("Home", code = 0x4A, color = LandscapeKC.SPECIAL),
    LandscapeKey("PgUp", code = 0x4B, color = LandscapeKC.SPECIAL),
)

internal val LANDSCAPE_NAV_ROW2 = listOf(
    LandscapeKey("Del", code = 0x4C, color = LandscapeKC.DANGER),
    LandscapeKey("End", code = 0x4D, color = LandscapeKC.SPECIAL),
    LandscapeKey("PgDn", code = 0x4E, color = LandscapeKC.SPECIAL),
)

internal val LANDSCAPE_KEY_UP = LandscapeKey("↑", code = 0x52, color = LandscapeKC.SPECIAL)
internal val LANDSCAPE_KEY_LEFT = LandscapeKey("←", code = 0x50, color = LandscapeKC.SPECIAL)
internal val LANDSCAPE_KEY_DOWN = LandscapeKey("↓", code = 0x51, color = LandscapeKC.SPECIAL)
internal val LANDSCAPE_KEY_RIGHT = LandscapeKey("→", code = 0x4F, color = LandscapeKC.SPECIAL)

// ═══════════════════════════════════════════════════════════════
// System key rows
// ═══════════════════════════════════════════════════════════════

internal val LANDSCAPE_SYSTEM_ROW1 = listOf(
    LandscapeKey("Esc", code = 0x29, color = LandscapeKC.DANGER, noRepeat = true),
    LandscapeKey("Tab", code = 0x2B, color = LandscapeKC.MOD),
    LandscapeKey("BkSp", code = 0x2A, color = LandscapeKC.DANGER),
    LandscapeKey("Del", code = 0x4C, color = LandscapeKC.DANGER),
    LandscapeKey("Enter", code = 0x28, color = LandscapeKC.ACCENT),
)

internal val LANDSCAPE_SYSTEM_ROW2 = listOf(
    LandscapeKey("PrtSc", code = 0x46, color = LandscapeKC.SPECIAL, noRepeat = true),
    LandscapeKey("ScrLk", code = 0x47, color = LandscapeKC.SPECIAL, noRepeat = true, isScroll = true),
    LandscapeKey("Pause", code = 0x48, color = LandscapeKC.SPECIAL, noRepeat = true),
    LandscapeKey("Ins", code = 0x49, color = LandscapeKC.SPECIAL),
    LandscapeKey("Menu", code = 0x65, color = LandscapeKC.MOD, noRepeat = true),
)

// ═══════════════════════════════════════════════════════════════
// Quick modifier keys
// ═══════════════════════════════════════════════════════════════

internal val LANDSCAPE_QUICK_MODIFIERS = listOf(
    LandscapeKey("Ctrl", modBit = LANDSCAPE_MOD_LCTRL, color = LandscapeKC.MOD, isMod = true, noRepeat = true),
    LandscapeKey("Shift", modBit = LANDSCAPE_MOD_LSHIFT, color = LandscapeKC.MOD, isMod = true, noRepeat = true),
    LandscapeKey("Alt", modBit = LANDSCAPE_MOD_LALT, color = LandscapeKC.MOD, isMod = true, noRepeat = true),
    LandscapeKey("Win", modBit = LANDSCAPE_MOD_LGUI, color = LandscapeKC.MOD, isMod = true, noRepeat = true),
    LandscapeKey("AltGr", modBit = LANDSCAPE_MOD_RALT, color = LandscapeKC.MOD, isMod = true, noRepeat = true),
)

internal val LANDSCAPE_QUICK_MODIFIERS_WITH_MENU = LANDSCAPE_QUICK_MODIFIERS + listOf(
    LandscapeKey("Menu", code = 0x65, w = 1.0f, color = LandscapeKC.MOD, noRepeat = true),
)

// ═══════════════════════════════════════════════════════════════
// Media key rows
// ═══════════════════════════════════════════════════════════════

internal val LANDSCAPE_MEDIA_TRANSPORT = listOf(
    LandscapeMKey("⏮", "Prev", BleHidManager.CONSUMER_PREV_TRACK),
    LandscapeMKey("⏯", "Play", BleHidManager.CONSUMER_PLAY_PAUSE),
    LandscapeMKey("⏭", "Next", BleHidManager.CONSUMER_NEXT_TRACK),
    LandscapeMKey("⏹", "Stop", BleHidManager.CONSUMER_STOP),
)

internal val LANDSCAPE_MEDIA_VOLUME = listOf(
    LandscapeMKey("🔇", "Mute", BleHidManager.CONSUMER_MUTE),
    LandscapeMKey("🔉", "Vol -", BleHidManager.CONSUMER_VOL_DOWN),
    LandscapeMKey("🔊", "Vol +", BleHidManager.CONSUMER_VOL_UP),
)

internal val LANDSCAPE_MEDIA_BRIGHT = listOf(
    LandscapeMKey("🔅", "Brt -", BleHidManager.CONSUMER_BRIGHTNESS_DOWN),
    LandscapeMKey("🔆", "Brt +", BleHidManager.CONSUMER_BRIGHTNESS_UP),
)