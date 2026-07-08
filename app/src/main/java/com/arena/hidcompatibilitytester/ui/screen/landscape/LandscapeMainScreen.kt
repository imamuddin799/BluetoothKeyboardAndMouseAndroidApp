package com.arena.hidcompatibilitytester.ui.screen.landscape

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.arena.hidcompatibilitytester.bluetooth.BleHidManager
import com.arena.hidcompatibilitytester.bluetooth.BleHidState
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.LandscapeKeyboardScreen
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.LandscapeKeyboardSettings
import com.arena.hidcompatibilitytester.ui.screen.landscape.keyboard.LandscapeKeyboardSettingsSheet
import com.arena.hidcompatibilitytester.ui.screen.landscape.trackpad.LandscapeTrackpadScreen
import com.arena.hidcompatibilitytester.ui.screen.landscape.trackpad.LandscapeTrackpadSettings
import com.arena.hidcompatibilitytester.ui.screen.landscape.trackpad.LandscapeTrackpadSettingsSheet
import kotlin.math.roundToInt

private enum class LandscapeTab(val icon: String, val label: String) {
    STATUS("📶", "Status"),
    MOUSE("🖱", "Mouse"),
    KEYBOARD("⌨", "Keyboard"),
    DEVICES("📱", "Devices"),
}

@SuppressLint("MissingPermission")
@Composable
fun LandscapeMainScreen(
    bleHidState: BleHidState,
    bleSupported: Boolean,
    connectedHostList: List<BleHidManager.DeviceInfo>,
    pairedList: List<BluetoothDevice>,
    nearbyList: List<BluetoothDevice>,
    isScanningState: Boolean,
    trackpadSettings: LandscapeTrackpadSettings,
    keyboardSettings: LandscapeKeyboardSettings,
    targetMode: BleHidManager.TargetMode,
    targetAddress: String?,
    onSelectAllTargets: () -> Unit,
    onSelectTargetDevice: (String) -> Unit,
    onToggleBleHid: () -> Unit,
    onSendMouse: (Int, Int, Int, Int) -> Unit,
    onSendKey: (Int, List<Int>) -> Unit,
    onReleaseKeys: () -> Unit,
    onConsumerKey: (Int) -> Unit,
    onTypeText: (String) -> Unit,
    onDisconnectHost: (String) -> Unit,
    onReconnectHost: (BluetoothDevice) -> Unit,
    onScanDevices: () -> Unit,
    onPairClick: (BluetoothDevice) -> Unit,
    onUnpairClick: (BluetoothDevice) -> Unit,
    onTrackpadSettingsChange: (LandscapeTrackpadSettings) -> Unit,
    onKeyboardSettingsChange: (LandscapeKeyboardSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReady = connectedHostList.any { it.isSubscribed }

    var selectedTab by remember { mutableStateOf(LandscapeTab.MOUSE) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showTrackpadSettings by remember { mutableStateOf(false) }
    var showKeyboardSettings by remember { mutableStateOf(false) }

    var fabOffsetX by remember { mutableFloatStateOf(0f) }
    var fabOffsetY by remember { mutableFloatStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val fabSizePx = with(density) { 48.dp.toPx() }
    val marginPx = with(density) { 12.dp.toPx() }

    val statusColor = when (bleHidState) {
        BleHidState.IDLE -> Color.Gray
        BleHidState.STARTING -> Color(0xFFF57F17)
        BleHidState.ADVERTISING -> Color(0xFF1565C0)
        BleHidState.CONNECTED -> Color(0xFF2E7D32)
        is BleHidState.ERROR -> Color.Red
        BleHidState.PAIR_REQUIRED -> Color(0xFFF57F17)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080F18))
            .onGloballyPositioned { containerSize = it.size }
    ) {
        when (selectedTab) {
            LandscapeTab.STATUS -> {
                LandscapeStatusScreen(
                    bleHidState = bleHidState,
                    bleSupported = bleSupported,
                    connectedHostList = connectedHostList,
                    onToggleBleHid = onToggleBleHid,
                    onDisconnectHost = onDisconnectHost,
                    onReconnectHost = onReconnectHost,
                )
            }

            LandscapeTab.MOUSE -> {
                LandscapeTrackpadScreen(
                    isReady = isReady,
                    settings = trackpadSettings,
                    keyboardSettings = keyboardSettings,
                    onSendMouse = onSendMouse,
                    onShowSettings = { showTrackpadSettings = true },
                    onSendKey = onSendKey,
                    onReleaseKeys = onReleaseKeys,
                    onConsumerKey = onConsumerKey,
                    onTypeText = onTypeText,
                )
            }

            LandscapeTab.KEYBOARD -> {
                LandscapeKeyboardScreen(
                    isReady = isReady,
                    settings = keyboardSettings,
                    onSendKey = onSendKey,
                    onReleaseKeys = onReleaseKeys,
                    onConsumerKey = onConsumerKey,
                    onTypeText = onTypeText,
                    onShowSettings = { showKeyboardSettings = true },
                    onSettingsChange = onKeyboardSettingsChange,
                )
            }

            LandscapeTab.DEVICES -> {
                LandscapeDevicesScreen(
                    nearbyList = nearbyList,
                    pairedList = pairedList,
                    isScanningState = isScanningState,
                    onToggleScan = onScanDevices,
                    onPairClick = onPairClick,
                    onUnpairClick = onUnpairClick,
                    onReconnect = onReconnectHost,
                )
            }
        }

        AnimatedVisibility(
            visible = menuExpanded,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(90f),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { menuExpanded = false }
            )
        }

        AnimatedVisibility(
            visible = menuExpanded,
            enter = scaleIn(
                initialScale = 0.7f,
                transformOrigin = gridTransformOrigin(fabOffsetX, fabOffsetY, containerSize),
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
            ) + fadeIn(tween(120)),
            exit = scaleOut(
                targetScale = 0.7f,
                transformOrigin = gridTransformOrigin(fabOffsetX, fabOffsetY, containerSize),
                animationSpec = tween(150),
            ) + fadeOut(tween(100)),
            modifier = Modifier
                .zIndex(95f)
                .offset {
                    gridMenuOffset(
                        fabOffsetX, fabOffsetY, containerSize,
                        menuWidthPx = with(density) { 220.dp.toPx() },
                        menuHeightPx = with(density) { 260.dp.toPx() },
                        fabSizePx = fabSizePx,
                        marginPx = marginPx,
                    )
                },
        ) {
            LandscapeGridMenu(
                selectedTab = selectedTab,
                statusColor = statusColor,
                onSelectTab = { tab ->
                    selectedTab = tab
                    menuExpanded = false
                },
                onClose = { menuExpanded = false },
            )
        }

        Box(
            modifier = Modifier
                .zIndex(100f)
                .offset { IntOffset(fabOffsetX.roundToInt(), fabOffsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val maxX = containerSize.width - fabSizePx - marginPx
                        val maxY = containerSize.height - fabSizePx - marginPx
                        fabOffsetX = (fabOffsetX + dragAmount.x).coerceIn(marginPx, maxX)
                        fabOffsetY = (fabOffsetY + dragAmount.y).coerceIn(marginPx, maxY)
                    }
                }
        ) {
            LandscapeFloatingMenuButton(
                expanded = menuExpanded,
                statusColor = statusColor,
                onClick = { menuExpanded = !menuExpanded },
            )
        }

        if (showTrackpadSettings) {
            LandscapeTrackpadSettingsSheet(
                settings = trackpadSettings,
                onDismiss = { showTrackpadSettings = false },
                onSave = { onTrackpadSettingsChange(it) },
            )
        }

        if (showKeyboardSettings) {
            LandscapeKeyboardSettingsSheet(
                settings = keyboardSettings,
                onDismiss = { showKeyboardSettings = false },
                onSave = { onKeyboardSettingsChange(it) },
            )
        }
    }
}

@Composable
private fun LandscapeFloatingMenuButton(
    expanded: Boolean,
    statusColor: Color,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (expanded) Color(0xFFD32F2F).copy(0.90f)
        else Color(0xFF1565C0).copy(0.90f),
        animationSpec = tween(200),
        label = "fabBg",
    )

    Surface(
        color = bg,
        shape = CircleShape,
        shadowElevation = 8.dp,
        modifier = Modifier.size(48.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (expanded) "✕" else "☰",
                fontSize = if (expanded) 18.sp else 20.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )

            if (!expanded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(10.dp)
                        .background(statusColor, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun LandscapeGridMenu(
    selectedTab: LandscapeTab,
    statusColor: Color,
    onSelectTab: (LandscapeTab) -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        color = Color(0xFF0D1B2A).copy(alpha = 0.97f),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 16.dp,
        modifier = Modifier.width(220.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape)
                    )
                    Text(
                        "Navigation",
                        color = Color(0xFF90CAF9),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Surface(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = CircleShape,
                    modifier = Modifier.size(28.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✕", fontSize = 12.sp, color = Color(0xFFEF9A9A), fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LandscapeGridMenuItem(LandscapeTab.STATUS, selectedTab == LandscapeTab.STATUS, Modifier.weight(1f)) { onSelectTab(LandscapeTab.STATUS) }
                    LandscapeGridMenuItem(LandscapeTab.MOUSE, selectedTab == LandscapeTab.MOUSE, Modifier.weight(1f)) { onSelectTab(LandscapeTab.MOUSE) }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LandscapeGridMenuItem(LandscapeTab.KEYBOARD, selectedTab == LandscapeTab.KEYBOARD, Modifier.weight(1f)) { onSelectTab(LandscapeTab.KEYBOARD) }
                    LandscapeGridMenuItem(LandscapeTab.DEVICES, selectedTab == LandscapeTab.DEVICES, Modifier.weight(1f)) { onSelectTab(LandscapeTab.DEVICES) }
                }
            }

            Surface(
                color = Color(0xFF1565C0).copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Active: ${selectedTab.label}",
                    color = Color(0xFF90CAF9),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun LandscapeGridMenuItem(
    tab: LandscapeTab,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) Color(0xFF1565C0).copy(alpha = 0.28f) else Color.White.copy(alpha = 0.04f),
        animationSpec = tween(150),
        label = "gridBg",
    )

    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.aspectRatio(1.2f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(tab.icon, fontSize = 24.sp)
                Text(
                    tab.label,
                    color = if (selected) Color(0xFF90CAF9) else Color.White.copy(0.7f),
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-6).dp)
                        .size(5.dp)
                        .background(Color(0xFF4A90D9), CircleShape)
                )
            }
        }
    }
}

private fun gridTransformOrigin(
    fabX: Float, fabY: Float, containerSize: IntSize,
): androidx.compose.ui.graphics.TransformOrigin {
    if (containerSize.width == 0 || containerSize.height == 0)
        return androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
    return androidx.compose.ui.graphics.TransformOrigin(
        pivotFractionX = (fabX / containerSize.width).coerceIn(0f, 1f),
        pivotFractionY = (fabY / containerSize.height).coerceIn(0f, 1f),
    )
}

private fun gridMenuOffset(
    fabX: Float, fabY: Float, containerSize: IntSize,
    menuWidthPx: Float, menuHeightPx: Float, fabSizePx: Float, marginPx: Float,
): IntOffset {
    var menuX = fabX + fabSizePx + marginPx
    if (menuX + menuWidthPx > containerSize.width - marginPx)
        menuX = fabX - menuWidthPx - marginPx
    menuX = menuX.coerceIn(marginPx, (containerSize.width - menuWidthPx - marginPx).coerceAtLeast(marginPx))

    val fabCenterY = fabY + fabSizePx / 2f
    var menuY = fabCenterY - menuHeightPx / 2f
    menuY = menuY.coerceIn(marginPx, (containerSize.height - menuHeightPx - marginPx).coerceAtLeast(marginPx))

    return IntOffset(menuX.roundToInt(), menuY.roundToInt())
}