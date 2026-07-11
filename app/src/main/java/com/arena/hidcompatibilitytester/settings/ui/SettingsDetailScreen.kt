package com.arena.hidcompatibilitytester.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.settings.AppSettings
import com.arena.hidcompatibilitytester.settings.ui.content.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDetailScreen(
    destination: SettingsDestination,
    settings: AppSettings,
    isLandscape: Boolean,
    hasChanges: Boolean,
    onSettingsChange: (AppSettings) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    val subSections = getSubSections(destination)
    var selectedSubSectionId by remember(destination) {
        mutableStateOf(subSections.firstOrNull()?.id ?: "")
    }
    var showResetDialog by remember { mutableStateOf(false) }

    val currentSubSection = subSections.find { it.id == selectedSubSectionId }

    if (isLandscape) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SettingsColors.Background),
        ) {
            CompactSettingsHeader(
                title = "${destination.category.label} › ${destination.label}",
                onBack = onBack,
                actions = {
                    when (destination) {
                        SettingsDestination.PORTRAIT_KEYBOARD,
                        SettingsDestination.LANDSCAPE_KEYBOARD -> {
                            SyncToggleButton(
                                checked = settings.syncFlags.syncKeyboardPortraitLandscape,
                                onToggle = { enabled ->
                                    val syncManager = com.arena.hidcompatibilitytester.settings.SettingsSyncManager
                                    val newSettings = if (enabled) {
                                        syncManager.enableKeyboardSync(settings)
                                    } else {
                                        syncManager.disableKeyboardSync(settings)
                                    }
                                    onSettingsChange(newSettings)
                                },
                            )
                        }
                        SettingsDestination.PORTRAIT_TRACKPAD,
                        SettingsDestination.LANDSCAPE_TRACKPAD -> {
                            SyncToggleButton(
                                checked = settings.syncFlags.syncTrackpadPortraitLandscape,
                                onToggle = { enabled ->
                                    val syncManager = com.arena.hidcompatibilitytester.settings.SettingsSyncManager
                                    val newSettings = if (enabled) {
                                        syncManager.enableTrackpadSync(settings)
                                    } else {
                                        syncManager.disableTrackpadSync(settings)
                                    }
                                    onSettingsChange(newSettings)
                                },
                            )
                        }
                        else -> {}
                    }

                    SettingsIconAction(
                        icon = "↺",
                        tint = SettingsColors.Warning,
                        onClick = { showResetDialog = true },
                    )

                    SaveButton(
                        hasChanges = hasChanges,
                        onClick = onSave,
                    )
                },
            )

            if (subSections.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SettingsDetailBody(
                        destination = destination,
                        subSectionId = "",
                        settings = settings,
                        isLandscape = true,
                        onSettingsChange = onSettingsChange,
                    )
                    Spacer(Modifier.height(20.dp))
                }
            } else {
                LandscapeDetailLayout(
                    destination = destination,
                    subSections = subSections,
                    selectedSubSectionId = selectedSubSectionId,
                    onSelectSubSection = { selectedSubSectionId = it },
                    settings = settings,
                    onSettingsChange = onSettingsChange,
                )
            }
        }

        if (showResetDialog) {
            ResetScopeDialog(
                destination = destination,
                currentSubSectionLabel = currentSubSection?.label,
                onDismiss = { showResetDialog = false },
                onReset = { scope ->
                    onSettingsChange(applyReset(settings, destination, currentSubSection?.id, scope))
                    showResetDialog = false
                },
            )
        }

        return
    }

    Scaffold(
        containerColor = SettingsColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${destination.category.label} › ${destination.label}",
                        color = SettingsColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = SettingsColors.TextPrimary,
                        )
                    }
                },
                actions = {
                    // Sync toggle
                    when (destination) {
                        SettingsDestination.PORTRAIT_KEYBOARD,
                        SettingsDestination.LANDSCAPE_KEYBOARD -> {
                            SyncToggleButton(
                                checked = settings.syncFlags.syncKeyboardPortraitLandscape,
                                onToggle = { enabled ->
                                    val syncManager = com.arena.hidcompatibilitytester.settings.SettingsSyncManager
                                    val newSettings = if (enabled) {
                                        syncManager.enableKeyboardSync(settings)
                                    } else {
                                        syncManager.disableKeyboardSync(settings)
                                    }
                                    onSettingsChange(newSettings)
                                },
                            )
                        }
                        SettingsDestination.PORTRAIT_TRACKPAD,
                        SettingsDestination.LANDSCAPE_TRACKPAD -> {
                            SyncToggleButton(
                                checked = settings.syncFlags.syncTrackpadPortraitLandscape,
                                onToggle = { enabled ->
                                    val syncManager = com.arena.hidcompatibilitytester.settings.SettingsSyncManager
                                    val newSettings = if (enabled) {
                                        syncManager.enableTrackpadSync(settings)
                                    } else {
                                        syncManager.disableTrackpadSync(settings)
                                    }
                                    onSettingsChange(newSettings)
                                },
                            )
                        }
                        else -> {}
                    }

                    // Reset button
                    SettingsIconAction(
                        icon = "↺",
                        tint = SettingsColors.Warning,
                        onClick = { showResetDialog = true },
                    )

                    // Save button
                    SaveButton(
                        hasChanges = hasChanges,
                        onClick = onSave,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SettingsColors.Background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (subSections.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SettingsDetailBody(
                        destination = destination,
                        subSectionId = "",
                        settings = settings,
                        isLandscape = isLandscape,
                        onSettingsChange = onSettingsChange,
                    )
                    Spacer(Modifier.height(20.dp))
                }
            } else if (isLandscape) {
                LandscapeDetailLayout(
                    destination = destination,
                    subSections = subSections,
                    selectedSubSectionId = selectedSubSectionId,
                    onSelectSubSection = { selectedSubSectionId = it },
                    settings = settings,
                    onSettingsChange = onSettingsChange,
                )
            } else {
                PortraitDetailLayout(
                    destination = destination,
                    subSections = subSections,
                    selectedSubSectionId = selectedSubSectionId,
                    onSelectSubSection = { selectedSubSectionId = it },
                    settings = settings,
                    onSettingsChange = onSettingsChange,
                )
            }
        }
    }

    if (showResetDialog) {
        ResetScopeDialog(
            destination = destination,
            currentSubSectionLabel = currentSubSection?.label,
            onDismiss = { showResetDialog = false },
            onReset = { scope ->
                onSettingsChange(applyReset(settings, destination, currentSubSection?.id, scope))
                showResetDialog = false
            },
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SAVE BUTTON
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun SaveButton(
    hasChanges: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (hasChanges) SettingsColors.Success.copy(0.85f) else SettingsColors.TextMuted.copy(0.3f)
    val fg = if (hasChanges) Color.White else SettingsColors.TextMuted

    Surface(
        modifier = Modifier
            .padding(end = 4.dp)
            .clickable(enabled = hasChanges) { onClick() },
        color = bg,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            "💾 Save",
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// PORTRAIT LAYOUT
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun PortraitDetailLayout(
    destination: SettingsDestination,
    subSections: List<SettingsSubSection>,
    selectedSubSectionId: String,
    onSelectSubSection: (String) -> Unit,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = subSections.indexOfFirst { it.id == selectedSubSectionId }
                .coerceAtLeast(0),
            containerColor = SettingsColors.Background,
            contentColor = SettingsColors.TextPrimary,
            edgePadding = 8.dp,
            divider = { HorizontalDivider(color = SettingsColors.Divider) },
        ) {
            subSections.forEachIndexed { i, sub ->
                val selected = sub.id == selectedSubSectionId
                Tab(
                    selected = selected,
                    onClick = { onSelectSubSection(sub.id) },
                    selectedContentColor = SettingsColors.Accent,
                    unselectedContentColor = SettingsColors.TextMuted,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(sub.icon, fontSize = 12.sp)
                        Text(
                            sub.label,
                            fontSize = if (selected) 12.sp else 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SettingsDetailBody(
                destination = destination,
                subSectionId = selectedSubSectionId,
                settings = settings,
                isLandscape = false,
                onSettingsChange = onSettingsChange,
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// LANDSCAPE LAYOUT
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun LandscapeDetailLayout(
    destination: SettingsDestination,
    subSections: List<SettingsSubSection>,
    selectedSubSectionId: String,
    onSelectSubSection: (String) -> Unit,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left rail
        LazyColumn(
            modifier = Modifier
                .width(140.dp)
                .fillMaxHeight()
                .background(SettingsColors.Surface),
            contentPadding = PaddingValues(top = 0.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            items(subSections.size) { index ->
                val sub = subSections[index]
                LandscapeRailItem(
                    icon = sub.icon,
                    label = sub.label,
                    selected = sub.id == selectedSubSectionId,
                    onClick = { onSelectSubSection(sub.id) },
                )
            }
        }

        // Right pane
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(
                start = 10.dp,
                top = 0.dp,
                end = 10.dp,
                bottom = 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SettingsDetailBody(
                    destination = destination,
                    subSectionId = selectedSubSectionId,
                    settings = settings,
                    isLandscape = true,
                    onSettingsChange = onSettingsChange,
                )
            }

            item {
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun LandscapeRailItem(
    icon: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val selectedColor = SettingsColors.AccentSoft
    val bg = if (selected) SettingsColors.Accent.copy(0.12f) else Color.Transparent
    val fg = if (selected) selectedColor else SettingsColors.TextMuted
    val iconColor = if (selected) Color.White else Color(0xFF90CAF9)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (selected) {
                    Modifier.drawBehind {
                        drawLine(
                            color = selectedColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                        drawLine(
                            color = selectedColor,
                            start = Offset(size.width, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                } else {
                    Modifier
                }
            ),
        color = bg,
        shape = RectangleShape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                icon,
                fontSize = 14.sp,
                color = iconColor,
            )
            Text(
                label,
                color = fg,
                fontSize = 9.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// SYNC TOGGLE
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun SyncToggleButton(
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val icon = if (checked) "🔗" else "⛓"
    val tint = if (checked) SettingsColors.Success else SettingsColors.TextMuted

    SettingsIconAction(
        icon = icon,
        tint = tint,
        onClick = { onToggle(!checked) },
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// RESET DIALOG
// ═════════════════════════════════════════════════════════════════════════════

enum class ResetScope { SUB_SECTION, CATEGORY }

@Composable
private fun ResetScopeDialog(
    destination: SettingsDestination,
    currentSubSectionLabel: String?,
    onDismiss: () -> Unit,
    onReset: (ResetScope) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SettingsColors.SurfaceElevated,
        title = {
            Text(
                "Reset Settings",
                color = SettingsColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Choose what to reset to defaults:",
                    color = SettingsColors.TextSecondary,
                    fontSize = 13.sp,
                )
                if (currentSubSectionLabel != null) {
                    Text(
                        "• Only \"$currentSubSectionLabel\" section",
                        color = SettingsColors.TextMuted,
                        fontSize = 11.sp,
                    )
                }
                Text(
                    "• Entire ${destination.label} (all sub-sections)",
                    color = SettingsColors.TextMuted,
                    fontSize = 11.sp,
                )
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (currentSubSectionLabel != null) {
                    TextButton(onClick = { onReset(ResetScope.SUB_SECTION) }) {
                        Text(
                            "Reset $currentSubSectionLabel",
                            color = SettingsColors.Warning,
                            fontSize = 12.sp,
                        )
                    }
                }
                TextButton(onClick = { onReset(ResetScope.CATEGORY) }) {
                    Text(
                        "Reset Entire ${destination.label}",
                        color = SettingsColors.Danger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SettingsColors.TextSecondary, fontSize = 12.sp)
            }
        },
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// RESET LOGIC
// ═════════════════════════════════════════════════════════════════════════════

private fun applyReset(
    settings: AppSettings,
    destination: SettingsDestination,
    subSectionId: String?,
    scope: ResetScope,
): AppSettings {
    return when (destination) {
        SettingsDestination.PORTRAIT_KEYBOARD -> {
            settings.copy(portraitKeyboard = com.arena.hidcompatibilitytester.settings.PortraitKeyboardSettings())
        }
        SettingsDestination.LANDSCAPE_KEYBOARD -> {
            settings.copy(landscapeKeyboard = com.arena.hidcompatibilitytester.settings.LandscapeKeyboardSettings())
        }
        SettingsDestination.PORTRAIT_TRACKPAD -> {
            settings.copy(portraitTrackpad = com.arena.hidcompatibilitytester.settings.PortraitTrackpadSettings())
        }
        SettingsDestination.LANDSCAPE_TRACKPAD -> {
            settings.copy(landscapeTrackpad = com.arena.hidcompatibilitytester.settings.LandscapeTrackpadSettings())
        }
        SettingsDestination.GENERAL_APPEARANCE,
        SettingsDestination.GENERAL_BEHAVIOR,
        SettingsDestination.GENERAL_LANGUAGE -> {
            settings.copy(general = com.arena.hidcompatibilitytester.settings.GeneralSettings())
        }
        SettingsDestination.ABOUT_RESET -> {
            com.arena.hidcompatibilitytester.settings.AppSettings()
        }
        else -> settings
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// BODY DISPATCHER
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsDetailBody(
    destination: SettingsDestination,
    subSectionId: String,
    settings: AppSettings,
    isLandscape: Boolean,
    onSettingsChange: (AppSettings) -> Unit,
) {
    when (destination) {
        SettingsDestination.PORTRAIT_KEYBOARD -> {
            PortraitKeyboardContent(
                subSectionId = subSectionId,
                settings = settings,
                isLandscape = isLandscape,
                onSettingsChange = onSettingsChange,
            )
        }
        SettingsDestination.LANDSCAPE_KEYBOARD -> {
            LandscapeKeyboardContent(
                subSectionId = subSectionId,
                settings = settings,
                isLandscape = isLandscape,
                onSettingsChange = onSettingsChange,
            )
        }
        SettingsDestination.PORTRAIT_TRACKPAD -> {
            PortraitTrackpadContent(
                subSectionId = subSectionId,
                settings = settings,
                isLandscape = isLandscape,
                onSettingsChange = onSettingsChange,
            )
        }
        SettingsDestination.LANDSCAPE_TRACKPAD -> {
            LandscapeTrackpadContent(
                subSectionId = subSectionId,
                settings = settings,
                isLandscape = isLandscape,
                onSettingsChange = onSettingsChange,
            )
        }
        SettingsDestination.GENERAL_APPEARANCE -> {
            GeneralAppearanceContent(
                subSectionId = subSectionId,
                settings = settings,
                isLandscape = isLandscape,
                onSettingsChange = onSettingsChange,
            )
        }
        SettingsDestination.GENERAL_BEHAVIOR -> {
            GeneralBehaviorContent(
                subSectionId = subSectionId,
                settings = settings,
                isLandscape = isLandscape,
                onSettingsChange = onSettingsChange,
            )
        }
        SettingsDestination.GENERAL_LANGUAGE -> {
            GeneralLanguageContent(
                subSectionId = subSectionId,
                settings = settings,
                isLandscape = isLandscape,
                onSettingsChange = onSettingsChange,
            )
        }
        SettingsDestination.ABOUT_VERSION -> AboutVersionContent()
        SettingsDestination.ABOUT_LICENSES -> AboutLicensesContent()
        SettingsDestination.ABOUT_RESET -> AboutResetContent(onSettingsChange = onSettingsChange)
        SettingsDestination.FUTURE_GAMEPAD,
        SettingsDestination.FUTURE_PRESENTER,
        SettingsDestination.FUTURE_SHORTCUTS -> FutureStubContent(destination.label)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// ABOUT / FUTURE CONTENT (kept here for simplicity)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun AboutVersionContent() {
    SettingsGroupCard(title = "App Information", collapsible = false) {
        Text("HID Compatibility Tester", color = SettingsColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text("Version 1.0", color = SettingsColors.TextSecondary, fontSize = 12.sp)
        Text("Build 1", color = SettingsColors.TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun AboutLicensesContent() {
    SettingsGroupCard(title = "Open Source Licenses", collapsible = false) {
        Text(
            "Includes libraries under Apache License 2.0, MIT License, and BSD licenses.",
            color = SettingsColors.TextSecondary,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun AboutResetContent(onSettingsChange: (AppSettings) -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    SettingsGroupCard(title = "Reset Everything", collapsible = false) {
        Text(
            "This will reset all settings to their default values. This cannot be undone.",
            color = SettingsColors.TextSecondary,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(6.dp))
        Button(
            onClick = { showConfirm = true },
            colors = ButtonDefaults.buttonColors(containerColor = SettingsColors.Danger.copy(0.8f)),
            shape = RoundedCornerShape(6.dp),
        ) {
            Text("Reset All Settings", color = Color.White, fontSize = 12.sp)
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = SettingsColors.SurfaceElevated,
            title = {
                Text("Reset All Settings?", color = SettingsColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("All settings will be reset to defaults.", color = SettingsColors.TextSecondary, fontSize = 13.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    onSettingsChange(AppSettings())
                    showConfirm = false
                }) {
                    Text("Reset All", color = SettingsColors.Danger, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel", color = SettingsColors.TextSecondary, fontSize = 13.sp)
                }
            },
        )
    }
}

@Composable
private fun FutureStubContent(label: String) {
    SettingsGroupCard(title = label, collapsible = false) {
        Text("🔮", fontSize = 24.sp)
        Text("This feature is planned for a future release.", color = SettingsColors.TextSecondary, fontSize = 12.sp)
        Text("Stay tuned!", color = SettingsColors.TextMuted, fontSize = 11.sp)
    }
}

@Composable
fun CompactSettingsHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SettingsColors.Background,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(start = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF90CAF9),
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = title,
                color = SettingsColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = actions,
            )
        }
    }
}