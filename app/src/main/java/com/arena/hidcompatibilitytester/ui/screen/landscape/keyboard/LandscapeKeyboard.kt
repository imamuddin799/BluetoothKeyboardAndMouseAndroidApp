package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arena.hidcompatibilitytester.ui.screen.keyboard.*
import com.arena.hidcompatibilitytester.ui.screen.keyboard.components.KBtn
import com.arena.hidcompatibilitytester.ui.screen.keyboard.components.MediaKeyBtn

// ─────────────────────────────────────────────────────────────────────────────
// Two-column layout — extra row definitions
// ─────────────────────────────────────────────────────────────────────────────

private val LANDSCAPE_ROW_FN_NO_DEL = listOf(
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
)

private val LANDSCAPE_RIGHT_FN = listOf(
    Key("PrtSc", code = 0x46, color = KC.SPECIAL, noRepeat = true),
    Key("ScrLk", code = 0x47, color = KC.SPECIAL, noRepeat = true, isScroll = true),
    Key("Pause", code = 0x48, color = KC.SPECIAL, noRepeat = true),
)

private val LANDSCAPE_RIGHT_NUM = listOf(
    Key("Ins",  code = 0x49, color = KC.SPECIAL),
    Key("Home", code = 0x4A, color = KC.SPECIAL),
    Key("PgUp", code = 0x4B, color = KC.SPECIAL),
)

private val LANDSCAPE_RIGHT_QWERTY = listOf(
    Key("Del",  code = 0x4C, color = KC.DANGER),
    Key("End",  code = 0x4D, color = KC.SPECIAL),
    Key("PgDn", code = 0x4E, color = KC.SPECIAL),
)

private val LANDSCAPE_RIGHT_ALPHA = listOf(KEY_UP)

private val LANDSCAPE_RIGHT_MODS = listOf(KEY_LEFT, KEY_DOWN, KEY_RIGHT)

// ─────────────────────────────────────────────────────────────────────────────
// Main entry — chooses layout based on effective mode
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LandscapeKeyboard(
    settings: KeyboardSettings,
    onSendKey: (Int, List<Int>) -> Unit,
    onReleaseKeys: () -> Unit,
    onConsumerKey: (Int) -> Unit,
    onTypeText: (String) -> Unit,
    modifier: Modifier = Modifier,
    // NEW: layout mode (effective, incl. runtime override). Default = SINGLE.
    layoutMode: LandscapeLayoutMode = LandscapeLayoutMode.SINGLE_COLUMN,
) {
    var kbSt by remember { mutableStateOf(KbState()) }
    val scope = rememberCoroutineScope()

    val currentKbSt     by rememberUpdatedState(kbSt)
    val currentSettings by rememberUpdatedState(settings)

    fun pressKey(key: Key) {
        kbSt = handleKeyPress(
            key                  = key,
            st                   = currentKbSt,
            settings             = currentSettings,
            scope                = scope,
            onSendKey            = onSendKey,
            onDelayedStateUpdate = { newSt -> kbSt = newSt }
        )
    }

    val rowH: Dp = settings.keyHeight.mainDp.dp
    val fnH:  Dp = settings.keyHeight.fnDp.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080F18))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Media row always at top
        LandscapeMediaRow(settings, fnH, onConsumerKey)

        when (layoutMode) {
            LandscapeLayoutMode.SINGLE_COLUMN ->
                SingleColumnBody(kbSt, settings, rowH, fnH, ::pressKey)
            LandscapeLayoutMode.TWO_COLUMN ->
                TwoColumnBody(kbSt, settings, rowH, fnH, ::pressKey)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Layout A — Single Column (existing)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SingleColumnBody(
    st: KbState,
    settings: KeyboardSettings,
    rowH: Dp,
    fnH: Dp,
    onKey: (Key) -> Unit,
) {
    // Function row WITH Del (original)
    LandscapeKeyRow(ROW_FN, st, settings, fnH, onKey)
    LandscapeKeyRow(ROW_NUM, st, settings, rowH, onKey)
    LandscapeKeyRow(ROW_QWERTY, st, settings, rowH, onKey)
    LandscapeKeyRow(ROW_HOME, st, settings, rowH, onKey)
    LandscapeKeyRow(ROW_ALPHA, st, settings, rowH, onKey)
    LandscapeKeyRow(
        if (settings.compactModifiers) ROW_MODS_COMPACT else ROW_MODS,
        st, settings, rowH, onKey
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Layout B — Two Column (responsive)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TwoColumnBody(
    st: KbState,
    settings: KeyboardSettings,
    rowH: Dp,
    fnH: Dp,
    onKey: (Key) -> Unit,
) {
    // Responsive weights — wider screens give more room to the right cluster
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    val (mainWeight, rightWeight) = when {
        screenWidthDp < 600  -> 0.82f to 0.18f
        screenWidthDp < 840  -> 0.80f to 0.20f
        screenWidthDp < 1080 -> 0.78f to 0.22f
        else                 -> 0.76f to 0.24f
    }

    val gap = 4.dp

    // Function row + PrtSc/ScrLk/Pause  (Del removed from FN)
    TwoColRow(mainWeight, rightWeight, gap) {
        left  { LandscapeKeyRow(LANDSCAPE_ROW_FN_NO_DEL, st, settings, fnH, onKey) }
        right { LandscapeKeyRow(LANDSCAPE_RIGHT_FN,      st, settings, fnH, onKey) }
    }

    // Number row + Ins/Home/PgUp
    TwoColRow(mainWeight, rightWeight, gap) {
        left  { LandscapeKeyRow(ROW_NUM,            st, settings, rowH, onKey) }
        right { LandscapeKeyRow(LANDSCAPE_RIGHT_NUM, st, settings, rowH, onKey) }
    }

    // QWERTY row + Del/End/PgDn   (Del now lives here)
    TwoColRow(mainWeight, rightWeight, gap) {
        left  { LandscapeKeyRow(ROW_QWERTY,             st, settings, rowH, onKey) }
        right { LandscapeKeyRow(LANDSCAPE_RIGHT_QWERTY, st, settings, rowH, onKey) }
    }

    // Home row + empty right
    TwoColRow(mainWeight, rightWeight, gap) {
        left  { LandscapeKeyRow(ROW_HOME, st, settings, rowH, onKey) }
        right { Spacer(Modifier.fillMaxWidth().height(rowH)) }
    }

    // Alpha row + centered Up arrow
    TwoColRow(mainWeight, rightWeight, gap) {
        left { LandscapeKeyRow(ROW_ALPHA, st, settings, rowH, onKey) }
        right {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Spacer(Modifier.weight(1f))
                Box(Modifier.weight(1f)) {
                    LandscapeKeyRow(LANDSCAPE_RIGHT_ALPHA, st, settings, rowH, onKey)
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }

    // Modifier row + ← ↓ →
    TwoColRow(mainWeight, rightWeight, gap) {
        left {
            LandscapeKeyRow(
                if (settings.compactModifiers) ROW_MODS_COMPACT else ROW_MODS,
                st, settings, rowH, onKey
            )
        }
        right { LandscapeKeyRow(LANDSCAPE_RIGHT_MODS, st, settings, rowH, onKey) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DSL helper for a two-column row
// ─────────────────────────────────────────────────────────────────────────────

private class TwoColScope {
    var leftContent: @Composable () -> Unit = {}
    var rightContent: @Composable () -> Unit = {}
    fun left(content: @Composable () -> Unit)  { leftContent = content }
    fun right(content: @Composable () -> Unit) { rightContent = content }
}

@Composable
private fun TwoColRow(
    mainWeight: Float,
    rightWeight: Float,
    gap: Dp,
    block: TwoColScope.() -> Unit,
) {
    val scope = remember { TwoColScope() }.apply(block)
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        Box(Modifier.weight(mainWeight))  { scope.leftContent() }
        Box(Modifier.weight(rightWeight)) { scope.rightContent() }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Media row + generic key row (unchanged)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LandscapeMediaRow(
    settings: KeyboardSettings,
    rowHeight: Dp,
    onConsumerKey: (Int) -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        MEDIA_VOLUME.forEach { mk ->
            MediaKeyBtn(mk.icon, mk.label, Modifier.weight(1f), rowHeight, settings,
                mediaGroup = MediaRowGroup.VOLUME, onClick = { onConsumerKey(mk.code) })
        }
        Spacer(Modifier.width(4.dp))
        MEDIA_TRANSPORT.forEach { mk ->
            MediaKeyBtn(mk.icon, mk.label, Modifier.weight(1f), rowHeight, settings,
                mediaGroup = MediaRowGroup.TRANSPORT, onClick = { onConsumerKey(mk.code) })
        }
        Spacer(Modifier.width(4.dp))
        MEDIA_BRIGHT.forEach { mk ->
            MediaKeyBtn(mk.icon, mk.label, Modifier.weight(1f), rowHeight, settings,
                mediaGroup = MediaRowGroup.BRIGHTNESS, onClick = { onConsumerKey(mk.code) })
        }
    }
}

@Composable
private fun LandscapeKeyRow(
    keys: List<Key>,
    st: KbState,
    settings: KeyboardSettings,
    rowHeight: Dp,
    onKeyPress: (Key) -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        keys.forEach { key ->
            KBtn(
                key       = key,
                modifier  = Modifier.weight(key.w),
                h         = rowHeight,
                settings  = settings,
                active    = isKeyActive(key, st),
                topLabel  = displayTop(key, st, settings.showKeyHints),
                mainLabel = displayMain(key, st),
                onPress   = { onKeyPress(key) }
            )
        }
    }
}