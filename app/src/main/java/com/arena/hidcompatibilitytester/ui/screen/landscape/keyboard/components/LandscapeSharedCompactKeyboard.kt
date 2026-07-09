package com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
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

// Total rows in a full layout: media/nav (up to 2) + fn + num + qwerty + home + alpha + mods
// We compute the shrink factor based on the actual visible row count.
private fun computeShrinkFactor(
    availableHeightDp: Float,
    rowH: Float,
    fnH: Float,
    normalRows: Int,
    fnRows: Int,
    verticalGapDp: Float,
): Float {
    val totalGaps = (normalRows + fnRows - 1).coerceAtLeast(0) * verticalGapDp
    val required = normalRows * rowH + fnRows * fnH + totalGaps
    return if (required <= availableHeightDp) 1f
    else (availableHeightDp / required).coerceIn(0.5f, 1f)
}

// ─── Two-column layout row definitions ───────────────────────────────────────

// Numpad cell size (drives auto-width column)
private val NUMPAD_CELL_WIDTH = 48.dp
private val NUMPAD_GAP = 0.dp
private const val NUMPAD_COLUMNS = 4  // NumLk ÷ × −
private val NUMPAD_TOTAL_WIDTH =
    NUMPAD_CELL_WIDTH * NUMPAD_COLUMNS +
    NUMPAD_GAP * (NUMPAD_COLUMNS - 1) +
    8.dp   // small breathing room

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
    layoutMode: LandscapeLayoutMode = LandscapeLayoutMode.SINGLE_COLUMN,
    rightColumnMode: LandscapeRightColumnMode = LandscapeRightColumnMode.NAV_CLUSTER,   // NEW
    onKeyPress: (LandscapeKey) -> Unit,
    onConsumerKey: ((Int) -> Unit)? = null,
    onClearMods: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    onNumpadKey: ((Int, String) -> Unit)? = null,   // NEW
    onNumLockToggle: (() -> Unit)? = null,          // NEW
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
            .fillMaxHeight()
            .background(Color(0xFF080F18))
            .padding(horizontal = 3.dp),   // slightly wider outer breathing
        verticalArrangement = Arrangement.Bottom,
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

        // ── Body: pick layout ──────────────────────────────────────────────
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF080F18))
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.BottomStart,
        ) {
            val availableDp = maxHeight.value
            val gapDp = 2f

            // Count visible optional rows
            val mediaKeysBuilt = buildLandscapeMediaRow(settings)
            val optionalCount = if (showOptionalRows) {
                var c = 0
                optionalRowOrder.forEach {
                    when (it) {
                        LandscapeKeyboardOptionalRow.MEDIA_ROW -> if (showMediaRow && mediaKeysBuilt.isNotEmpty()) c++
                        LandscapeKeyboardOptionalRow.NAV_ROW   -> if (showNavRow) c++
                    }
                }
                c
            } else 0

            // Rows: 5 normal + (1 fn + optionalCount fn-height rows)
            // Reserve space for dismiss bar (~26dp) and combo bar (~24dp) if visible
            var reserved = 0f
            if (showDismissBar) reserved += 26f
            if (showComboPreview && (st.anyMod || st.lastKey.isNotEmpty())) reserved += 24f

            val usableDp = (availableDp - reserved).coerceAtLeast(50f)

            val normalRows = 5
            val fnRows = 1 + optionalCount

            val rowHf = settings.keyHeight.mainDp.toFloat()
            val fnHf  = settings.keyHeight.fnDp.toFloat()

            val shrink = computeShrinkFactor(usableDp, rowHf, fnHf, normalRows, fnRows, gapDp)

            val effRowH = (rowHf * shrink).dp
            val effFnH  = (fnHf  * shrink).dp

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Bottom,
            ) {
                // Dismiss bar
                if (showDismissBar) {
                    // … your existing dismiss-bar Row …
                }

                // Combo preview bar
                if (showComboPreview && (st.anyMod || st.lastKey.isNotEmpty())) {
                    // … your existing combo preview Row …
                }

                when (layoutMode) {
                    LandscapeLayoutMode.SINGLE_COLUMN ->
                        SingleColumnBody(
                            st, settings, effRowH, effFnH, ::handleKeyClick,
                            showMediaRow, showNavRow, showOptionalRows, optionalRowOrder,
                        )
                    LandscapeLayoutMode.TWO_COLUMN ->
                        TwoColumnBody(
                            st, settings, effRowH, effFnH, ::handleKeyClick,
                            rightColumnMode = rightColumnMode,
                            onNumpadKey = onNumpadKey,
                            onNumLockToggle = onNumLockToggle,
                            showMediaRow = showMediaRow,
                            showNavRow = showNavRow,
                            showOptionalRows = showOptionalRows,
                            optionalRowOrder = optionalRowOrder,
                        )
                }
            }
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
    showMediaRow: Boolean,
    showNavRow: Boolean,
    showOptionalRows: Boolean,
    optionalRowOrder: List<LandscapeKeyboardOptionalRow>,
) {
    // Optional rows — attached directly above FN
    if (showOptionalRows) {
        optionalRowOrder.forEach { rowType ->
            when (rowType) {
                LandscapeKeyboardOptionalRow.MEDIA_ROW -> if (showMediaRow) {
                    val mediaKeys = buildLandscapeMediaRow(settings)
                    if (mediaKeys.isNotEmpty()) {
                        LandscapeStyledKeyRow(mediaKeys, fnH, settings, st, onClick)
                    }
                }
                LandscapeKeyboardOptionalRow.NAV_ROW -> if (showNavRow) {
                    LandscapeStyledKeyRow(L_SHARED_ROW_NAV, fnH, settings, st, onClick)
                }
            }
        }
    }

    LandscapeStyledKeyRow(LANDSCAPE_ROW_FN, fnH, settings, st, onClick)
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
    rightColumnMode: LandscapeRightColumnMode,
    onNumpadKey: ((Int, String) -> Unit)?,
    onNumLockToggle: (() -> Unit)?,
    showMediaRow: Boolean,
    showNavRow: Boolean,
    showOptionalRows: Boolean,
    optionalRowOrder: List<LandscapeKeyboardOptionalRow>,
) {
    when (rightColumnMode) {
        LandscapeRightColumnMode.NAV_CLUSTER -> TwoColumnNavBody(
            st, settings, rowH, fnH, onClick,
            showMediaRow, showNavRow, showOptionalRows, optionalRowOrder,
        )
        LandscapeRightColumnMode.NUMPAD -> TwoColumnNumpadBody(
            st, settings, rowH, fnH, onClick, onNumpadKey, onNumLockToggle,
            showMediaRow, showNavRow, showOptionalRows, optionalRowOrder,
        )
    }
}

// ── Right column = existing nav cluster ─────────────────────────────────────
@Composable
private fun TwoColumnNavBody(
    st: LandscapeKbState,
    settings: LandscapeKeyboardSettings,
    rowH: Dp,
    fnH: Dp,
    onClick: (LandscapeKey) -> Unit,
    showMediaRow: Boolean,
    showNavRow: Boolean,
    showOptionalRows: Boolean,
    optionalRowOrder: List<LandscapeKeyboardOptionalRow>,
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val (mainW, rightW) = when {
        screenWidthDp < 600  -> 0.82f to 0.18f
        screenWidthDp < 840  -> 0.80f to 0.20f
        screenWidthDp < 1080 -> 0.78f to 0.22f
        else                 -> 0.76f to 0.24f
    }
    val gap = 3.dp

    // Optional rows — only above main keyboard (column 1)
    if (showOptionalRows) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            Column(Modifier.weight(mainW)) {
                optionalRowOrder.forEach { rowType ->
                    when (rowType) {
                        LandscapeKeyboardOptionalRow.MEDIA_ROW -> if (showMediaRow) {
                            val mediaKeys = buildLandscapeMediaRow(settings)
                            if (mediaKeys.isNotEmpty()) {
                                LandscapeStyledKeyRow(mediaKeys, fnH, settings, st, onClick)
                            }
                        }
                        LandscapeKeyboardOptionalRow.NAV_ROW -> if (showNavRow) {
                            LandscapeStyledKeyRow(L_SHARED_ROW_NAV, fnH, settings, st, onClick)
                        }
                    }
                }
            }
            // Right column empty above FN
            Spacer(Modifier.weight(rightW))
        }
    }

    TwoColRowWeighted(mainW, rightW, gap,
        left  = { LandscapeStyledKeyRow(L_ROW_FN_NO_DEL, fnH, settings, st, onClick) },
        right = { LandscapeStyledKeyRow(L_RIGHT_FN,      fnH, settings, st, onClick) })
    TwoColRowWeighted(mainW, rightW, gap,
        left  = { LandscapeStyledKeyRow(LANDSCAPE_ROW_NUM, rowH, settings, st, onClick) },
        right = { LandscapeStyledKeyRow(L_RIGHT_NUM,       rowH, settings, st, onClick) })
    TwoColRowWeighted(mainW, rightW, gap,
        left  = { LandscapeStyledKeyRow(LANDSCAPE_ROW_QWERTY, rowH, settings, st, onClick) },
        right = { LandscapeStyledKeyRow(L_RIGHT_QWERTY,       rowH, settings, st, onClick) })
    TwoColRowWeighted(mainW, rightW, gap,
        left  = { LandscapeStyledKeyRow(LANDSCAPE_ROW_HOME, rowH, settings, st, onClick) },
        right = { Spacer(Modifier.fillMaxWidth().height(rowH)) })
    TwoColRowWeighted(mainW, rightW, gap,
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
    TwoColRowWeighted(mainW, rightW, gap,
        left = {
            LandscapeStyledKeyRow(
                if (settings.compactModifiers) LANDSCAPE_ROW_MODS_COMPACT else LANDSCAPE_ROW_MODS,
                rowH, settings, st, onClick
            )
        },
        right = { LandscapeStyledKeyRow(L_RIGHT_MODS, rowH, settings, st, onClick) })
}

// ── Right column = auto-width numpad (bottom-aligned, top empty) ────────────
@Composable
private fun TwoColumnNumpadBody(
    st: LandscapeKbState,
    settings: LandscapeKeyboardSettings,
    rowH: Dp,
    fnH: Dp,
    onClick: (LandscapeKey) -> Unit,
    onNumpadKey: ((Int, String) -> Unit)?,
    onNumLockToggle: (() -> Unit)?,
    showMediaRow: Boolean,
    showNavRow: Boolean,
    showOptionalRows: Boolean,
    optionalRowOrder: List<LandscapeKeyboardOptionalRow>,
) {
    val gap = 3.dp

    Row(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.Bottom,
    ) {
        // Main keyboard column — bottom-aligned
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.Bottom,
        ) {
            // Optional rows attached above FN — only in this column
            if (showOptionalRows) {
                optionalRowOrder.forEach { rowType ->
                    when (rowType) {
                        LandscapeKeyboardOptionalRow.MEDIA_ROW -> if (showMediaRow) {
                            val mediaKeys = buildLandscapeMediaRow(settings)
                            if (mediaKeys.isNotEmpty()) {
                                LandscapeStyledKeyRow(mediaKeys, fnH, settings, st, onClick)
                            }
                        }
                        LandscapeKeyboardOptionalRow.NAV_ROW -> if (showNavRow) {
                            LandscapeStyledKeyRow(L_SHARED_ROW_NAV, fnH, settings, st, onClick)
                        }
                    }
                }
            }

            LandscapeStyledKeyRow(L_ROW_FN_NO_DEL, fnH, settings, st, onClick)
            LandscapeStyledKeyRow(LANDSCAPE_ROW_NUM, rowH, settings, st, onClick)
            LandscapeStyledKeyRow(LANDSCAPE_ROW_QWERTY, rowH, settings, st, onClick)
            LandscapeStyledKeyRow(LANDSCAPE_ROW_HOME, rowH, settings, st, onClick)
            LandscapeStyledKeyRow(LANDSCAPE_ROW_ALPHA, rowH, settings, st, onClick)
            LandscapeStyledKeyRow(
                if (settings.compactModifiers) LANDSCAPE_ROW_MODS_COMPACT else LANDSCAPE_ROW_MODS,
                rowH, settings, st, onClick
            )
        }

        // Numpad column — bottom-aligned, top empty
        // Numpad column — bottom-aligned to match main keyboard bottom
        Column(
            modifier = Modifier.width(NUMPAD_TOTAL_WIDTH).fillMaxHeight(),
        ) {
            Spacer(Modifier.weight(1f))   // pushes numpad to the bottom

            InlineNumpad(
                cellW = NUMPAD_CELL_WIDTH,
                cellH = rowH,
                gap = NUMPAD_GAP,
                numLock = st.numLock,
                shift = st.shift,
                settings = settings,
                highContrast = settings.highContrastMode,
                onKey = { code, label -> onNumpadKey?.invoke(code, label) },
                onNumLock = { onNumLockToggle?.invoke() },
            )
        }
    }
}

@Composable
private fun TwoColRowWeighted(
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

    settings.mediaRowGroupOrder.forEach { group ->
        when (group) {
            LandscapeMediaRowGroup.TRANSPORT -> if (settings.mediaRowShowTransport) {
                list.add(LandscapeKey("⏮", consumerCode = 0xB6, isConsumer = true,
                    color = LandscapeKC.MEDIA, noRepeat = true,
                    mediaGroup = LandscapeMediaRowGroup.TRANSPORT))
                list.add(LandscapeKey("⏯", consumerCode = 0xCD, isConsumer = true,
                    color = LandscapeKC.MEDIA, noRepeat = true,
                    mediaGroup = LandscapeMediaRowGroup.TRANSPORT))
                list.add(LandscapeKey("⏹", consumerCode = 0xB7, isConsumer = true,
                    color = LandscapeKC.MEDIA, noRepeat = true,
                    mediaGroup = LandscapeMediaRowGroup.TRANSPORT))
                list.add(LandscapeKey("⏭", consumerCode = 0xB5, isConsumer = true,
                    color = LandscapeKC.MEDIA, noRepeat = true,
                    mediaGroup = LandscapeMediaRowGroup.TRANSPORT))
            }
            LandscapeMediaRowGroup.VOLUME -> if (settings.mediaRowShowVolume) {
                list.add(LandscapeKey("🔇", consumerCode = 0xE2, isConsumer = true,
                    color = LandscapeKC.MEDIA,
                    mediaGroup = LandscapeMediaRowGroup.VOLUME))
                list.add(LandscapeKey("🔉", consumerCode = 0xEA, isConsumer = true,
                    color = LandscapeKC.MEDIA,
                    mediaGroup = LandscapeMediaRowGroup.VOLUME))
                list.add(LandscapeKey("🔊", consumerCode = 0xE9, isConsumer = true,
                    color = LandscapeKC.MEDIA,
                    mediaGroup = LandscapeMediaRowGroup.VOLUME))
            }
            LandscapeMediaRowGroup.BRIGHTNESS -> if (settings.mediaRowShowBrightness) {
                list.add(LandscapeKey("🔅", consumerCode = 0x0070, isConsumer = true,
                    color = LandscapeKC.MEDIA,
                    mediaGroup = LandscapeMediaRowGroup.BRIGHTNESS))
                list.add(LandscapeKey("🔆", consumerCode = 0x006F, isConsumer = true,
                    color = LandscapeKC.MEDIA,
                    mediaGroup = LandscapeMediaRowGroup.BRIGHTNESS))
            }
        }
    }

    return list.map { it.copy(w = 1f) }
}

@Composable
private fun InlineNumpad(
    cellW: Dp,
    cellH: Dp,
    gap: Dp,
    numLock: Boolean,
    shift: Boolean,
    settings: LandscapeKeyboardSettings,
    highContrast: Boolean,
    onKey: (Int, String) -> Unit,
    onNumLock: () -> Unit,
) {
    fun lbl(on: String, off: String): String {
        val eff = if (shift) !numLock else numLock
        return if (eff) on else off
    }
    fun top(on: String, off: String): String {
        if (on == off) return ""
        val eff = if (shift) !numLock else numLock
        return if (eff) off else on
    }

    val tallH = cellH * 2 + gap

    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        // Row: NumLk ÷ × −
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            NumCell("NumLk", w = cellW, h = cellH, active = numLock,
                color = LandscapeKC.MOD, settings = settings, onTap = onNumLock)
            NumCell("÷", w = cellW, h = cellH, color = LandscapeKC.SPECIAL,
                settings = settings) { onKey(0x54, "/") }
            NumCell("×", w = cellW, h = cellH, color = LandscapeKC.SPECIAL,
                settings = settings) { onKey(0x55, "*") }
            NumCell("−", w = cellW, h = cellH, color = LandscapeKC.SPECIAL,
                settings = settings) { onKey(0x56, "-") }
        }

        // Rows 7/8/9 + tall +
        Row(
            modifier = Modifier.height(tallH),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    NumCell(lbl("7","Home"), top("7","Home"), w = cellW, h = cellH,
                        settings = settings) { onKey(0x5F, lbl("7","Home")) }
                    NumCell(lbl("8","↑"), top("8","↑"), w = cellW, h = cellH,
                        settings = settings) { onKey(0x60, lbl("8","↑")) }
                    NumCell(lbl("9","PgUp"), top("9","PgUp"), w = cellW, h = cellH,
                        settings = settings) { onKey(0x61, lbl("9","PgUp")) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    NumCell(lbl("4","←"), top("4","←"), w = cellW, h = cellH,
                        settings = settings) { onKey(0x5C, lbl("4","←")) }
                    NumCell(lbl("5","·"), top("5","·"), w = cellW, h = cellH,
                        settings = settings) { onKey(0x5D, lbl("5","·")) }
                    NumCell(lbl("6","→"), top("6","→"), w = cellW, h = cellH,
                        settings = settings) { onKey(0x5E, lbl("6","→")) }
                }
            }
            NumCell("+", w = cellW, h = tallH, color = LandscapeKC.ACCENT,
                settings = settings) { onKey(0x57, "+") }
        }

        // Rows 1/2/3/0/. + tall Enter
        Row(
            modifier = Modifier.height(tallH),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    NumCell(lbl("1","End"), top("1","End"), w = cellW, h = cellH,
                        settings = settings) { onKey(0x59, lbl("1","End")) }
                    NumCell(lbl("2","↓"), top("2","↓"), w = cellW, h = cellH,
                        settings = settings) { onKey(0x5A, lbl("2","↓")) }
                    NumCell(lbl("3","PgDn"), top("3","PgDn"), w = cellW, h = cellH,
                        settings = settings) { onKey(0x5B, lbl("3","PgDn")) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    NumCell(lbl("0","Ins"), top("0","Ins"), w = cellW * 2 + gap, h = cellH,
                        settings = settings) { onKey(0x62, lbl("0","Ins")) }
                    NumCell(lbl(".","Del"), top(".","Del"), w = cellW, h = cellH,
                        settings = settings) { onKey(0x63, lbl(".","Del")) }
                }
            }
            NumCell("↵", w = cellW, h = tallH, color = LandscapeKC.ACCENT,
                settings = settings) { onKey(0x58, "↵") }
        }
    }
}

@Composable
private fun NumCell(
    mainLabel: String,
    topLabel: String = "",
    w: Dp,
    h: Dp,
    color: LandscapeKC = LandscapeKC.NORMAL,
    active: Boolean = false,
    settings: LandscapeKeyboardSettings,
    onTap: () -> Unit,
) {
    Box(modifier = Modifier.width(w)) {
        com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.components
            .LandscapeNumpadCell(
                mainLabel = mainLabel,
                topLabel  = topLabel,
                color     = color,
                isActive  = active,
                modifier  = Modifier.fillMaxWidth(),
                h         = h,
                settings  = settings,
                doRepeat  = true,
                onTap     = onTap,
            )
    }
}