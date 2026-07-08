package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.components.LandscapeKBtn

// ─── Two-column layout row definitions ───────────────────────────────────────

private val L_ROW_FN_NO_DEL = listOf(
    LandscapeKey("Esc", code = 0x29, w = 1.4f, color = LandscapeKC.DANGER, noRepeat = true),
    LandscapeKey("F1",  code = 0x3A, color = LandscapeKC.SPECIAL),
    LandscapeKey("F2",  code = 0x3B, color = LandscapeKC.SPECIAL),
    LandscapeKey("F3",  code = 0x3C, color = LandscapeKC.SPECIAL),
    LandscapeKey("F4",  code = 0x3D, color = LandscapeKC.SPECIAL),
    LandscapeKey("F5",  code = 0x3E, color = LandscapeKC.SPECIAL),
    LandscapeKey("F6",  code = 0x3F, color = LandscapeKC.SPECIAL),
    LandscapeKey("F7",  code = 0x40, color = LandscapeKC.SPECIAL),
    LandscapeKey("F8",  code = 0x41, color = LandscapeKC.SPECIAL),
    LandscapeKey("F9",  code = 0x42, color = LandscapeKC.SPECIAL),
    LandscapeKey("F10", code = 0x43, color = LandscapeKC.SPECIAL),
    LandscapeKey("F11", code = 0x44, color = LandscapeKC.SPECIAL),
    LandscapeKey("F12", code = 0x45, color = LandscapeKC.SPECIAL),
)

private val L_RIGHT_FN = listOf(
    LandscapeKey("PrtSc", code = 0x46, color = LandscapeKC.SPECIAL, noRepeat = true),
    LandscapeKey("ScrLk", code = 0x47, color = LandscapeKC.SPECIAL, noRepeat = true, isScroll = true),
    LandscapeKey("Pause", code = 0x48, color = LandscapeKC.SPECIAL, noRepeat = true),
)

private val L_RIGHT_NUM = listOf(
    LandscapeKey("Ins",  code = 0x49, color = LandscapeKC.SPECIAL),
    LandscapeKey("Home", code = 0x4A, color = LandscapeKC.SPECIAL),
    LandscapeKey("PgUp", code = 0x4B, color = LandscapeKC.SPECIAL),
)

private val L_RIGHT_QWERTY = listOf(
    LandscapeKey("Del",  code = 0x4C, color = LandscapeKC.DANGER),
    LandscapeKey("End",  code = 0x4D, color = LandscapeKC.SPECIAL),
    LandscapeKey("PgDn", code = 0x4E, color = LandscapeKC.SPECIAL),
)

private val L_RIGHT_ALPHA = listOf(LANDSCAPE_KEY_UP)

private val L_RIGHT_MODS = listOf(LANDSCAPE_KEY_LEFT, LANDSCAPE_KEY_DOWN, LANDSCAPE_KEY_RIGHT)

// ─── Main composable ─────────────────────────────────────────────────────────

@Composable
fun LandscapeSharedCompactKeyboard(
    st: LandscapeKbState,
    settings: LandscapeKeyboardSettings,
    showDismissBar: Boolean = false,
    showMediaRow: Boolean = false,
    showNavRow: Boolean = false,
    showOptionalRows: Boolean = true,
    showComboPreview: Boolean = false,
    optionalRowOrder: List<LandscapeKeyboardOptionalRow> = listOf(
        LandscapeKeyboardOptionalRow.MEDIA_ROW,
        LandscapeKeyboardOptionalRow.NAV_ROW,
    ),
    // NEW — effective layout mode (from parent / status-bar override)
    layoutMode: LandscapeLayoutMode = LandscapeLayoutMode.SINGLE_COLUMN,
    onKeyPress: (LandscapeKey) -> Unit,
    onConsumerKey: ((Int) -> Unit)? = null,
    onClearMods: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    fun handleKeyClick(k: LandscapeKey) {
        if (k.isConsumer && onConsumerKey != null) {
            onConsumerKey(k.consumerCode)
        } else {
            onKeyPress(k)
        }
    }

    val rowH = settings.keyHeight.mainDp.dp
    val fnH = settings.keyHeight.fnDp.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF080F18))
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        // ── Dismiss bar ────────────────────────────────────────────────────
        if (showDismissBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF050C14))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Keyboard", fontSize = 10.sp,
                    color = Color(0xFF607D8B), fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (st.caps) LandscapeSharedMiniLed("CAP")
                    if (st.shift) LandscapeSharedMiniLed("SHF")
                    if (st.ctrl) LandscapeSharedMiniLed("CTL")
                    if (st.alt) LandscapeSharedMiniLed("ALT")
                    if (st.gui) LandscapeSharedMiniLed("WIN")
                }
                IconButton(onClick = { onDismiss?.invoke() }, modifier = Modifier.size(24.dp)) {
                    Text("✕", fontSize = 12.sp, color = Color(0xFFEF9A9A))
                }
            }
            HorizontalDivider(color = Color.White.copy(0.04f), thickness = 1.dp)
        }

        // ── Combo preview ──────────────────────────────────────────────────
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
                Surface(color = Color(0xFF0A1828), shape = RoundedCornerShape(5.dp),
                        modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            st.anyMod && st.lastKey.isEmpty() ->
                                "▶ ${st.modPrefix().trimEnd('+')}+ …waiting"
                            combo.isEmpty() -> "Ready…"
                            else -> "⌨ $combo"
                        },
                        fontSize = 11.sp,
                        color = if (combo.isEmpty()) Color(0xFF546E7A) else Color(0xFF90CAF9),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
                if (st.anyMod && onClearMods != null) {
                    TextButton(onClick = onClearMods,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("✕ Clear", fontSize = 10.sp, color = Color(0xFFEF9A9A), maxLines = 1)
                    }
                }
            }
            HorizontalDivider(color = Color.White.copy(0.04f), thickness = 1.dp)
        }

        // ── Optional rows (Media / Nav) ────────────────────────────────────
        AnimatedVisibility(visible = showOptionalRows) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                optionalRowOrder.forEach { rowType ->
                    when (rowType) {
                        LandscapeKeyboardOptionalRow.MEDIA_ROW -> if (showMediaRow) {
                            val mediaKeys = buildLandscapeMediaRow(settings)
                            if (mediaKeys.isNotEmpty()) {
                                LandscapeStyledKeyRow(mediaKeys, fnH, settings, st, ::handleKeyClick)
                                HorizontalDivider(color = Color.White.copy(0.04f), thickness = 1.dp)
                            }
                        }
                        LandscapeKeyboardOptionalRow.NAV_ROW -> if (showNavRow) {
                            LandscapeStyledKeyRow(L_SHARED_ROW_NAV, fnH, settings, st, ::handleKeyClick)
                            HorizontalDivider(color = Color.White.copy(0.04f), thickness = 1.dp)
                        }
                    }
                }
            }
        }

        // ── Body: pick layout ──────────────────────────────────────────────
        when (layoutMode) {
            LandscapeLayoutMode.SINGLE_COLUMN ->
                SingleColumnBody(st, settings, rowH, fnH, ::handleKeyClick)
            LandscapeLayoutMode.TWO_COLUMN ->
                TwoColumnBody(st, settings, rowH, fnH, ::handleKeyClick)
        }
    }
}

// ─── Layout A: Single column (original) ──────────────────────────────────────

@Composable
private fun SingleColumnBody(
    st: LandscapeKbState,
    settings: LandscapeKeyboardSettings,
    rowH: Dp,
    fnH: Dp,
    onClick: (LandscapeKey) -> Unit,
) {
    LandscapeStyledKeyRow(LANDSCAPE_ROW_FN, fnH, settings, st, onClick)
    HorizontalDivider(color = Color.White.copy(0.04f), thickness = 1.dp)
    LandscapeStyledKeyRow(LANDSCAPE_ROW_NUM, rowH, settings, st, onClick)
    LandscapeStyledKeyRow(LANDSCAPE_ROW_QWERTY, rowH, settings, st, onClick)
    LandscapeStyledKeyRow(LANDSCAPE_ROW_HOME, rowH, settings, st, onClick)
    LandscapeStyledKeyRow(LANDSCAPE_ROW_ALPHA, rowH, settings, st, onClick)
    LandscapeStyledKeyRow(
        if (settings.compactModifiers) LANDSCAPE_ROW_MODS_COMPACT else LANDSCAPE_ROW_MODS,
        rowH, settings, st, onClick
    )
}

// ─── Layout B: Two column (responsive) ───────────────────────────────────────

@Composable
private fun TwoColumnBody(
    st: LandscapeKbState,
    settings: LandscapeKeyboardSettings,
    rowH: Dp,
    fnH: Dp,
    onClick: (LandscapeKey) -> Unit,
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val (mainW, rightW) = when {
        screenWidthDp < 600  -> 0.82f to 0.18f
        screenWidthDp < 840  -> 0.80f to 0.20f
        screenWidthDp < 1080 -> 0.78f to 0.22f
        else                 -> 0.76f to 0.24f
    }
    val gap = 3.dp

    // Function row + PrtSc/ScrLk/Pause
    TwoColRow(mainW, rightW, gap,
        left  = { LandscapeStyledKeyRow(L_ROW_FN_NO_DEL, fnH, settings, st, onClick) },
        right = { LandscapeStyledKeyRow(L_RIGHT_FN,      fnH, settings, st, onClick) })

    HorizontalDivider(color = Color.White.copy(0.04f), thickness = 1.dp)

    // Number row + Ins/Home/PgUp
    TwoColRow(mainW, rightW, gap,
        left  = { LandscapeStyledKeyRow(LANDSCAPE_ROW_NUM, rowH, settings, st, onClick) },
        right = { LandscapeStyledKeyRow(L_RIGHT_NUM,       rowH, settings, st, onClick) })

    // QWERTY row + Del/End/PgDn
    TwoColRow(mainW, rightW, gap,
        left  = { LandscapeStyledKeyRow(LANDSCAPE_ROW_QWERTY, rowH, settings, st, onClick) },
        right = { LandscapeStyledKeyRow(L_RIGHT_QWERTY,       rowH, settings, st, onClick) })

    // Home row + empty right
    TwoColRow(mainW, rightW, gap,
        left  = { LandscapeStyledKeyRow(LANDSCAPE_ROW_HOME, rowH, settings, st, onClick) },
        right = { Spacer(Modifier.fillMaxWidth().height(rowH)) })

    // Alpha row + centered Up arrow
    TwoColRow(mainW, rightW, gap,
        left = { LandscapeStyledKeyRow(LANDSCAPE_ROW_ALPHA, rowH, settings, st, onClick) },
        right = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Spacer(Modifier.weight(1f))
                Box(Modifier.weight(1f)) {
                    LandscapeStyledKeyRow(L_RIGHT_ALPHA, rowH, settings, st, onClick)
                }
                Spacer(Modifier.weight(1f))
            }
        })

    // Modifier row + ← ↓ →
    TwoColRow(mainW, rightW, gap,
        left = {
            LandscapeStyledKeyRow(
                if (settings.compactModifiers) LANDSCAPE_ROW_MODS_COMPACT else LANDSCAPE_ROW_MODS,
                rowH, settings, st, onClick
            )
        },
        right = { LandscapeStyledKeyRow(L_RIGHT_MODS, rowH, settings, st, onClick) })
}

@Composable
private fun TwoColRow(
    mainWeight: Float,
    rightWeight: Float,
    gap: Dp,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        Box(Modifier.weight(mainWeight))  { left() }
        Box(Modifier.weight(rightWeight)) { right() }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

@Composable
private fun LandscapeSharedMiniLed(label: String) {
    Text(
        label, fontSize = 6.sp, color = Color(0xFF90CAF9),
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(Color(0xFF1565C0).copy(0.3f), RoundedCornerShape(2.dp))
            .padding(horizontal = 3.dp, vertical = 1.dp),
    )
}

@Composable
private fun LandscapeStyledKeyRow(
    keys: List<LandscapeKey>,
    height: Dp,
    settings: LandscapeKeyboardSettings,
    st: LandscapeKbState,
    onClick: (LandscapeKey) -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        keys.forEach { k ->
            LandscapeKBtn(
                key = k,
                modifier = Modifier.weight(k.w),
                h = height,
                settings = settings,
                active = landscapeIsKeyActive(k, st),
                topLabel = landscapeDisplayTop(k, st, settings.showKeyHints),
                mainLabel = landscapeDisplayMain(k, st),
                onPress = { onClick(k) },
            )
        }
    }
}

private val L_SHARED_ROW_NAV = listOf(
    LandscapeKey("PgUp", code = 0x4B, color = LandscapeKC.SPECIAL),
    LandscapeKey("PgDn", code = 0x4E, color = LandscapeKC.SPECIAL),
    LandscapeKey("Ins",  code = 0x49, color = LandscapeKC.SPECIAL),
    LandscapeKey("Home", code = 0x4A, color = LandscapeKC.SPECIAL),
    LandscapeKey("End",  code = 0x4D, color = LandscapeKC.SPECIAL),
    LandscapeKey("←",    code = 0x50, color = LandscapeKC.SPECIAL),
    LandscapeKey("↑",    code = 0x52, color = LandscapeKC.SPECIAL),
    LandscapeKey("↓",    code = 0x51, color = LandscapeKC.SPECIAL),
    LandscapeKey("→",    code = 0x4F, color = LandscapeKC.SPECIAL),
)

private fun buildLandscapeMediaRow(settings: LandscapeKeyboardSettings): List<LandscapeKey> {
    val list = mutableListOf<LandscapeKey>()
    if (settings.mediaRowShowTransport) {
        list.add(LandscapeKey("⏮", consumerCode = 0xB6, isConsumer = true, color = LandscapeKC.MEDIA))
        list.add(LandscapeKey("⏯", consumerCode = 0xCD, isConsumer = true, color = LandscapeKC.MEDIA))
        list.add(LandscapeKey("⏭", consumerCode = 0xB5, isConsumer = true, color = LandscapeKC.MEDIA))
    }
    if (settings.mediaRowShowVolume) {
        list.add(LandscapeKey("🔉", consumerCode = 0xEA, isConsumer = true, color = LandscapeKC.MEDIA))
        list.add(LandscapeKey("🔊", consumerCode = 0xE9, isConsumer = true, color = LandscapeKC.MEDIA))
    }
    if (settings.mediaRowShowBrightness) {
        list.add(LandscapeKey("🔅", consumerCode = 0x0070, isConsumer = true, color = LandscapeKC.MEDIA))
        list.add(LandscapeKey("🔆", consumerCode = 0x006F, isConsumer = true, color = LandscapeKC.MEDIA))
    }
    return list.map { it.copy(w = 1f) }
}