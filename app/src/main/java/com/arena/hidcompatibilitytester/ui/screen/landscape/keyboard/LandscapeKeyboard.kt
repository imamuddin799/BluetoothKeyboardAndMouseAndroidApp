package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arena.hidcompatibilitytester.ui.screen.keyboard.*
import com.arena.hidcompatibilitytester.ui.screen.keyboard.components.KBtn
import com.arena.hidcompatibilitytester.ui.screen.keyboard.components.MediaKeyBtn

@Composable
fun LandscapeKeyboard(
    settings: KeyboardSettings,
    onSendKey: (Int, List<Int>) -> Unit,
    onReleaseKeys: () -> Unit,
    onConsumerKey: (Int) -> Unit,
    onTypeText: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var kbSt by remember { mutableStateOf(KbState()) }
    val scope = rememberCoroutineScope()

    val currentKbSt    by rememberUpdatedState(kbSt)
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
        // ── Media Row ────────────────────────────────────────────────────
        LandscapeMediaRow(
            settings       = settings,
            rowHeight      = fnH,
            onConsumerKey  = onConsumerKey
        )

        // ── Function Row ─────────────────────────────────────────────────
        LandscapeKeyRow(
            keys      = ROW_FN,
            st        = kbSt,
            settings  = settings,
            rowHeight = fnH,
            onKeyPress = ::pressKey
        )

        // ── Number Row ───────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Main number keys
            Box(modifier = Modifier.weight(0.78f)) {
                LandscapeKeyRow(
                    keys      = ROW_NUM,
                    st        = kbSt,
                    settings  = settings,
                    rowHeight = rowH,
                    onKeyPress = ::pressKey
                )
            }
            // Nav cluster top
            Box(modifier = Modifier.weight(0.22f)) {
                LandscapeKeyRow(
                    keys      = NAV_ROW1,
                    st        = kbSt,
                    settings  = settings,
                    rowHeight = rowH,
                    onKeyPress = ::pressKey
                )
            }
        }

        // ── QWERTY Row ───────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(modifier = Modifier.weight(0.78f)) {
                LandscapeKeyRow(
                    keys      = ROW_QWERTY,
                    st        = kbSt,
                    settings  = settings,
                    rowHeight = rowH,
                    onKeyPress = ::pressKey
                )
            }
            Box(modifier = Modifier.weight(0.22f)) {
                LandscapeKeyRow(
                    keys      = NAV_ROW2,
                    st        = kbSt,
                    settings  = settings,
                    rowHeight = rowH,
                    onKeyPress = ::pressKey
                )
            }
        }

        // ── Home Row ─────────────────────────────────────────────────────
        LandscapeKeyRow(
            keys      = ROW_HOME,
            st        = kbSt,
            settings  = settings,
            rowHeight = rowH,
            onKeyPress = ::pressKey
        )

        // ── Alpha Row ────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(modifier = Modifier.weight(0.78f)) {
                LandscapeKeyRow(
                    keys      = ROW_ALPHA,
                    st        = kbSt,
                    settings  = settings,
                    rowHeight = rowH,
                    onKeyPress = ::pressKey
                )
            }
            // Arrow up only
            Box(modifier = Modifier.weight(0.22f)) {
                LandscapeKeyRow(
                    keys      = listOf(KEY_UP),
                    st        = kbSt,
                    settings  = settings,
                    rowHeight = rowH,
                    onKeyPress = ::pressKey
                )
            }
        }

        // ── Modifier Row ─────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(modifier = Modifier.weight(0.78f)) {
                LandscapeKeyRow(
                    keys = if (settings.compactModifiers) ROW_MODS_COMPACT else ROW_MODS,
                    st        = kbSt,
                    settings  = settings,
                    rowHeight = rowH,
                    onKeyPress = ::pressKey
                )
            }
            // Arrow left / down / right
            Box(modifier = Modifier.weight(0.22f)) {
                LandscapeKeyRow(
                    keys      = listOf(KEY_LEFT, KEY_DOWN, KEY_RIGHT),
                    st        = kbSt,
                    settings  = settings,
                    rowHeight = rowH,
                    onKeyPress = ::pressKey
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Media row — uses existing MediaKeyBtn with correct parameter names
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LandscapeMediaRow(
    settings: KeyboardSettings,
    rowHeight: Dp,
    onConsumerKey: (Int) -> Unit,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Volume
        MEDIA_VOLUME.forEach { mk ->
            MediaKeyBtn(
                icon       = mk.icon,
                label      = mk.label,
                modifier   = Modifier.weight(1f),
                h          = rowHeight,
                settings   = settings,
                mediaGroup = MediaRowGroup.VOLUME,
                onClick    = { onConsumerKey(mk.code) }
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Transport
        MEDIA_TRANSPORT.forEach { mk ->
            MediaKeyBtn(
                icon       = mk.icon,
                label      = mk.label,
                modifier   = Modifier.weight(1f),
                h          = rowHeight,
                settings   = settings,
                mediaGroup = MediaRowGroup.TRANSPORT,
                onClick    = { onConsumerKey(mk.code) }
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Brightness
        MEDIA_BRIGHT.forEach { mk ->
            MediaKeyBtn(
                icon       = mk.icon,
                label      = mk.label,
                modifier   = Modifier.weight(1f),
                h          = rowHeight,
                settings   = settings,
                mediaGroup = MediaRowGroup.BRIGHTNESS,
                onClick    = { onConsumerKey(mk.code) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Generic key row — uses existing KBtn with correct parameter names
// ─────────────────────────────────────────────────────────────────────────────

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