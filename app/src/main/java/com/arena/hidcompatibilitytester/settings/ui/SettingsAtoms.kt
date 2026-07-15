package com.arena.hidcompatibilitytester.settings.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ═════════════════════════════════════════════════════════════════════════════
// COLORS (shared across settings UI)
// ═════════════════════════════════════════════════════════════════════════════

object SettingsColors {
    val Background = Color(0xFF111C28)
    val Surface = Color(0xFF0A1520)
    val SurfaceElevated = Color(0xFF162334)
    val Divider = Color.White.copy(alpha = 0.08f)
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFB0BEC5)
    val TextMuted = Color(0xFF607D8B)
    val Accent = Color(0xFF4A90D9)
    val AccentSoft = Color(0xFF90CAF9)
    val Success = Color(0xFF81C784)
    val Warning = Color(0xFFF57F17)
    val Danger = Color(0xFFEF9A9A)
}

// ═════════════════════════════════════════════════════════════════════════════
// GROUP CARD (collapsible container for related settings)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsGroupCard(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = true,
    collapsible: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SettingsColors.Surface,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (collapsible) Modifier.clickable { expanded = !expanded } else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (collapsible) {
                        Text(
                            text = if (expanded) "▾" else "▸",
                            fontSize = 10.sp,
                            color = SettingsColors.Accent,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                    Text(
                        title,
                        color = SettingsColors.Accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                trailingIcon?.invoke()
            }

            if (expanded) {
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    content()
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// COMPACT TOGGLE
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsToggle(
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    onToggle: (Boolean) -> Unit,
) {
    val alpha = if (enabled) 1f else 0.4f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable { onToggle(!checked) } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        ) {
            Text(
                label,
                color = SettingsColors.TextPrimary.copy(alpha = alpha),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = SettingsColors.TextMuted.copy(alpha = alpha),
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = { if (enabled) onToggle(it) },
            enabled = enabled,
            modifier = Modifier.scale(0.75f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = SettingsColors.Accent,
                checkedTrackColor = SettingsColors.Accent.copy(0.5f),
                uncheckedThumbColor = SettingsColors.TextMuted,
                uncheckedTrackColor = Color.White.copy(0.15f),
                disabledCheckedThumbColor = SettingsColors.Accent.copy(0.4f),
                disabledCheckedTrackColor = SettingsColors.Accent.copy(0.2f),
                disabledUncheckedThumbColor = SettingsColors.TextMuted.copy(0.4f),
                disabledUncheckedTrackColor = Color.White.copy(0.08f),
            ),
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// COMPACT SLIDER
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    display: (Float) -> String = { it.toString() },
    minLabel: String? = null,
    maxLabel: String? = null,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
) {
    val alpha = if (enabled) 1f else 0.4f

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                label,
                color = SettingsColors.TextPrimary.copy(alpha = alpha),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                display(value),
                color = SettingsColors.AccentSoft.copy(alpha = alpha),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = SettingsColors.Accent,
                activeTrackColor = SettingsColors.Accent,
                inactiveTrackColor = Color.White.copy(0.15f),
            ),
        )
        if (minLabel != null || maxLabel != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    minLabel ?: "",
                    color = SettingsColors.TextMuted.copy(alpha = alpha),
                    fontSize = 9.sp,
                )
                Text(
                    maxLabel ?: "",
                    color = SettingsColors.TextMuted.copy(alpha = alpha),
                    fontSize = 9.sp,
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// CHIP SELECTOR (horizontal segmented buttons)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsChipSelector(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    onSelect: (Int) -> Unit,
) {
    val alpha = if (enabled) 1f else 0.4f

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            label,
            color = SettingsColors.TextPrimary.copy(alpha = alpha),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        if (subtitle != null) {
            Text(
                subtitle,
                color = SettingsColors.TextMuted.copy(alpha = alpha),
                fontSize = 10.sp,
            )
        }
        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEachIndexed { i, opt ->
                val isSelected = selectedIndex == i
                SettingsChip(
                    label = opt,
                    selected = isSelected,
                    enabled = enabled,
                    onClick = { if (enabled) onSelect(i) },
                )
            }
        }
    }
}

@Composable
fun SettingsChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.4f
    val bg = if (selected) SettingsColors.Accent.copy(0.2f * alpha) else Color.Transparent
    val border = if (selected) SettingsColors.Accent.copy(alpha) else Color.White.copy(0.15f * alpha)

    Surface(
        modifier = modifier
            .clickable(enabled = enabled) { onClick() },
        color = bg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, border),
    ) {
        Text(
            label,
            color = SettingsColors.TextPrimary.copy(alpha = alpha),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// DROPDOWN SELECTOR
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsDropdown(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    onSelect: (Int) -> Unit,
) {
    val alpha = if (enabled) 1f else 0.4f
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                label,
                color = SettingsColors.TextPrimary.copy(alpha = alpha),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = SettingsColors.TextMuted.copy(alpha = alpha),
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Box {
            Surface(
                modifier = Modifier.clickable(enabled = enabled) { expanded = true },
                color = SettingsColors.SurfaceElevated,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, Color.White.copy(0.1f * alpha)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        options.getOrNull(selectedIndex) ?: "?",
                        color = SettingsColors.TextPrimary.copy(alpha = alpha),
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                    Text(
                        "▼",
                        color = SettingsColors.TextMuted.copy(alpha = alpha),
                        fontSize = 8.sp,
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(SettingsColors.SurfaceElevated),
            ) {
                options.forEachIndexed { i, opt ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                opt,
                                color = SettingsColors.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (i == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            onSelect(i)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// TWO-COLUMN ROW (for pairing two settings side-by-side)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsTwoColRow(
    modifier: Modifier = Modifier,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.weight(1f)) { left() }
        Box(modifier = Modifier.weight(1f)) { right() }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// INFO HINT CARD
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsHint(
    text: String,
    icon: String = "ℹ",
    modifier: Modifier = Modifier,
    variant: SettingsHintVariant = SettingsHintVariant.INFO,
) {
    val bg = when (variant) {
        SettingsHintVariant.INFO -> Color(0xFF1565C0).copy(0.12f)
        SettingsHintVariant.WARNING -> Color(0xFFF57F17).copy(0.12f)
        SettingsHintVariant.SUCCESS -> Color(0xFF2E7D32).copy(0.12f)
    }
    val fg = when (variant) {
        SettingsHintVariant.INFO -> SettingsColors.AccentSoft
        SettingsHintVariant.WARNING -> Color(0xFFFFB74D)
        SettingsHintVariant.SUCCESS -> SettingsColors.Success
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bg,
        shape = RoundedCornerShape(6.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(icon, fontSize = 11.sp)
            Text(
                text,
                color = fg,
                fontSize = 10.sp,
                lineHeight = 13.sp,
            )
        }
    }
}

enum class SettingsHintVariant { INFO, WARNING, SUCCESS }

// ═════════════════════════════════════════════════════════════════════════════
// SYNC BANNER (for keyboard/trackpad sync toggles)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsSyncBanner(
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onToggle: (Boolean) -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (checked) SettingsColors.Accent.copy(0.15f) else Color.White.copy(0.04f),
        animationSpec = tween(200),
        label = "syncBg",
    )
    val icon = if (checked) "🔗" else "⛓"
    val subtitle = if (checked)
        "Common settings mirror between Portrait & Landscape"
    else
        "Portrait & Landscape settings stay independent"

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bg,
        shape = RoundedCornerShape(6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle(!checked) }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            ) {
                Text(icon, fontSize = 14.sp)
                Column {
                    Text(
                        label,
                        color = SettingsColors.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        subtitle,
                        color = SettingsColors.TextMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                modifier = Modifier.scale(0.75f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SettingsColors.Accent,
                    checkedTrackColor = SettingsColors.Accent.copy(0.5f),
                    uncheckedThumbColor = SettingsColors.TextMuted,
                    uncheckedTrackColor = Color.White.copy(0.15f),
                ),
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SECTION HEADER (for navigation lists on Page 1 and Page 2)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsNavRow(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: String = "›",
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.4f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        color = SettingsColors.Surface,
        shape = RoundedCornerShape(6.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            ) {
                Text(
                    icon,
                    fontSize = 16.sp,
                    color = Color(0xFF90CAF9),
                )
                Column {
                    Text(
                        label,
                        color = SettingsColors.TextPrimary.copy(alpha = alpha),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            color = SettingsColors.TextMuted.copy(alpha = alpha),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Text(
                trailing,
                color = SettingsColors.TextMuted.copy(alpha = alpha),
                fontSize = 14.sp,
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// COLLAPSIBLE CATEGORY (for Page 1 top-level categories)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsCollapsibleCategory(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onToggle: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Header — only the header is clickable for expand/collapse
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() },
            color = SettingsColors.Surface,
            shape = RoundedCornerShape(6.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        if (expanded) "▾" else "▸",
                        color = SettingsColors.Accent,
                        fontSize = 12.sp,
                    )
                    Text(
                        icon,
                        fontSize = 16.sp,
                        color = Color(0xFF90CAF9),
                    )
                    Text(
                        label,
                        color = SettingsColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Children — rendered separately, not inside the header's clickable area
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                content()
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// DRAG-TO-REORDER LIST
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun <T> SettingsDragReorderList(
    items: List<T>,
    modifier: Modifier = Modifier,
    labelProvider: (T) -> String,
    iconProvider: (T) -> String = { "⠿" },
    onReorder: (List<T>) -> Unit,
) {
    var orderList by remember(items) { mutableStateOf(items.toList()) }
    var draggingIdx by remember { mutableIntStateOf(-1) }
    var dragYAccum by remember { mutableFloatStateOf(0f) }
    var lastTargetIdx by remember { mutableIntStateOf(-1) }
    var isCommitting by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val itemHeightDp = 40.dp
    val spacingDp = 3.dp
    val slotPx = with(density) { (itemHeightDp + spacingDp).toPx() }
    val scope = rememberCoroutineScope()

    val targetIdx = if (draggingIdx >= 0) {
        (draggingIdx + (dragYAccum / slotPx).roundToInt())
            .coerceIn(0, orderList.size - 1)
    } else -1

    LaunchedEffect(targetIdx) { if (targetIdx >= 0) lastTargetIdx = targetIdx }

    val offsetAnimatables = remember { mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>() }
    orderList.indices.forEach { idx ->
        if (!offsetAnimatables.containsKey(idx)) {
            offsetAnimatables[idx] = Animatable(0f)
        }
    }

    orderList.indices.forEach { index ->
        val isDragged = draggingIdx == index
        val targetOffsetPx = when {
            isDragged -> dragYAccum
            draggingIdx < 0 -> 0f
            targetIdx > draggingIdx && index in (draggingIdx + 1)..targetIdx -> -slotPx
            targetIdx < draggingIdx && index in targetIdx until draggingIdx -> slotPx
            else -> 0f
        }
        LaunchedEffect(index, targetOffsetPx, isDragged) {
            if (isCommitting) return@LaunchedEffect
            val anim = offsetAnimatables[index] ?: return@LaunchedEffect
            if (isDragged) anim.snapTo(targetOffsetPx)
            else anim.animateTo(targetOffsetPx, spring(dampingRatio = 0.8f, stiffness = 300f))
        }
    }

    fun commitReorder() {
        val from = draggingIdx; val to = lastTargetIdx
        draggingIdx = -1; dragYAccum = 0f; lastTargetIdx = -1
        if (from < 0 || to < 0 || to == from) return
        val newList = orderList.toMutableList()
        val item = newList.removeAt(from)
        newList.add(to, item)
        scope.launch {
            isCommitting = true
            orderList.indices.forEach { offsetAnimatables[it]?.snapTo(0f) }
            orderList = newList
            onReorder(newList)
            isCommitting = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(orderList) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        if (isCommitting) return@detectDragGesturesAfterLongPress
                        val idx = (offset.y / slotPx).toInt().coerceIn(orderList.indices)
                        draggingIdx = idx; dragYAccum = 0f; lastTargetIdx = idx
                    },
                    onDrag = { change, amount ->
                        if (draggingIdx >= 0 && !isCommitting) {
                            change.consume(); dragYAccum += amount.y
                        }
                    },
                    onDragEnd = { if (!isCommitting) commitReorder() },
                    onDragCancel = { draggingIdx = -1; dragYAccum = 0f; lastTargetIdx = -1 },
                )
            },
        verticalArrangement = Arrangement.spacedBy(spacingDp),
    ) {
        orderList.forEachIndexed { index, item ->
            val isDragged = draggingIdx == index
            val offsetDp = with(density) { (offsetAnimatables[index]?.value ?: 0f).toDp() }

            // Dynamic display index — matches old DragToReorderList behavior
            val displayIndex = when {
                isDragged -> (targetIdx + 1).coerceIn(1, orderList.size)
                draggingIdx >= 0 -> {
                    val wouldBe = when {
                        targetIdx > draggingIdx && index in (draggingIdx + 1)..targetIdx -> index - 1
                        targetIdx < draggingIdx && index in targetIdx until draggingIdx -> index + 1
                        else -> index
                    }
                    wouldBe + 1
                }
                else -> index + 1
            }

            Surface(
                color = if (isDragged) SettingsColors.Accent.copy(0.25f) else SettingsColors.SurfaceElevated,
                shape = RoundedCornerShape(6.dp),
                shadowElevation = if (isDragged) 6.dp else 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeightDp)
                    .zIndex(if (isDragged) 10f else 0f)
                    .offset(y = offsetDp)
                    .scale(if (isDragged) 1.02f else 1f)
                    .border(
                        if (isDragged) 1.5.dp else 0.5.dp,
                        if (isDragged) SettingsColors.Accent else Color.White.copy(0.08f),
                        RoundedCornerShape(6.dp),
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Number badge — centered text
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    if (isDragged) SettingsColors.Accent.copy(0.3f)
                                    else SettingsColors.Accent.copy(0.15f),
                                    RoundedCornerShape(5.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$displayIndex",
                                color = if (isDragged) Color.White else SettingsColors.AccentSoft,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 11.sp,
                                modifier = Modifier.wrapContentSize(Alignment.Center),
                            )
                        }
                        // Icon — use AccentSoft color instead of default black
                        Text(
                            iconProvider(item),
                            fontSize = 12.sp,
                            color = SettingsColors.AccentSoft,
                        )
                        Text(
                            labelProvider(item),
                            color = SettingsColors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                    Text(
                        "⠿",
                        fontSize = 14.sp,
                        color = if (isDragged) SettingsColors.AccentSoft else SettingsColors.TextMuted,
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// TOP APP BAR ACTIONS (compact icon buttons)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsIconAction(
    icon: String,
    modifier: Modifier = Modifier,
    tint: Color = SettingsColors.AccentSoft,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .size(32.dp)
            .clickable(enabled = enabled) { onClick() },
        color = Color.Transparent,
        shape = RoundedCornerShape(6.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                icon,
                fontSize = 15.sp,
                color = tint.copy(alpha = if (enabled) 1f else 0.4f),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}