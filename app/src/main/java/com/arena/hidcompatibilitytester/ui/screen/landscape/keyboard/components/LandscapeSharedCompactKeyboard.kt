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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.components.LandscapeKBtn

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
                    if (st.caps) LandscapeSharedMiniLed("CAP")
                    if (st.shift) LandscapeSharedMiniLed("SHF")
                    if (st.ctrl) LandscapeSharedMiniLed("CTL")
                    if (st.alt) LandscapeSharedMiniLed("ALT")
                    if (st.gui) LandscapeSharedMiniLed("WIN")
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
                Surface(
                    color = Color(0xFF0A1828),
                    shape = RoundedCornerShape(5.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = when {
                            st.anyMod && st.lastKey.isEmpty() ->
                                "▶ ${st.modPrefix().trimEnd('+')}+ …waiting"
                            combo.isEmpty() -> "Ready…"
                            else -> "⌨ $combo"
                        },
                        fontSize = 11.sp,
                        color = if (combo.isEmpty()) Color(0xFF546E7A)
                        else Color(0xFF90CAF9),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
                if (st.anyMod && onClearMods != null) {
                    TextButton(
                        onClick = onClearMods,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text("✕ Clear", fontSize = 10.sp, color = Color(0xFFEF9A9A), maxLines = 1)
                    }
                }
            }
            HorizontalDivider(color = Color.White.copy(0.04f), thickness = 1.dp)
        }

        // Optional Rows (Media/Nav)
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
                            LandscapeStyledKeyRow(LANDSCAPE_SHARED_ROW_NAV, fnH, settings, st, ::handleKeyClick)
                            HorizontalDivider(color = Color.White.copy(0.04f), thickness = 1.dp)
                        }
                    }
                }
            }
        }

        // Standard Rows
        LandscapeStyledKeyRow(LANDSCAPE_ROW_FN, fnH, settings, st, ::handleKeyClick)
        HorizontalDivider(color = Color.White.copy(0.04f), thickness = 1.dp)
        LandscapeStyledKeyRow(LANDSCAPE_ROW_NUM, rowH, settings, st, ::handleKeyClick)
        LandscapeStyledKeyRow(LANDSCAPE_ROW_QWERTY, rowH, settings, st, ::handleKeyClick)
        LandscapeStyledKeyRow(LANDSCAPE_ROW_HOME, rowH, settings, st, ::handleKeyClick)
        LandscapeStyledKeyRow(LANDSCAPE_ROW_ALPHA, rowH, settings, st, ::handleKeyClick)
        LandscapeStyledKeyRow(
            if (settings.compactModifiers) LANDSCAPE_ROW_MODS_COMPACT else LANDSCAPE_ROW_MODS,
            rowH, settings, st, ::handleKeyClick
        )
    }
}

@Composable
private fun LandscapeSharedMiniLed(label: String) {
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

private val LANDSCAPE_SHARED_ROW_NAV = listOf(
    LandscapeKey("PgUp", code = 0x4B, color = LandscapeKC.SPECIAL),
    LandscapeKey("PgDn", code = 0x4E, color = LandscapeKC.SPECIAL),
    LandscapeKey("Ins", code = 0x49, color = LandscapeKC.SPECIAL),
    LandscapeKey("Home", code = 0x4A, color = LandscapeKC.SPECIAL),
    LandscapeKey("End", code = 0x4D, color = LandscapeKC.SPECIAL),
    LandscapeKey("←", code = 0x50, color = LandscapeKC.SPECIAL),
    LandscapeKey("↑", code = 0x52, color = LandscapeKC.SPECIAL),
    LandscapeKey("↓", code = 0x51, color = LandscapeKC.SPECIAL),
    LandscapeKey("→", code = 0x4F, color = LandscapeKC.SPECIAL),
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