package com.arena.hidcompatibilitytester.settings.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.arena.hidcompatibilitytester.settings.AppSettings

/**
 * Entry point for the settings module.
 *
 * Save-on-close behavior:
 * - Works on a local copy of settings.
 * - Changes are NOT applied until user explicitly saves.
 * - Save button appears on detail pages.
 * - Navigating back without saving triggers a discard confirmation dialog.
 */
@Composable
fun SettingsRootScreen(
    isLandscape: Boolean,
    settings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    // Local working copy — changes only committed on save
    var workingSettings by remember(settings) { mutableStateOf(settings) }
    var hasChanges by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf<SettingsPage>(SettingsPage.Home) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var pendingBackAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun onWorkingSettingsChange(newSettings: AppSettings) {
        workingSettings = newSettings
        hasChanges = true
    }

    fun saveAndDismiss() {
        if (hasChanges) {
            onSave(workingSettings)
            hasChanges = false
        }
    }

    fun tryNavigateBack(backAction: () -> Unit) {
        if (hasChanges) {
            pendingBackAction = backAction
            showDiscardDialog = true
        } else {
            backAction()
        }
    }

    when (val page = currentPage) {
        is SettingsPage.Home -> {
            SettingsHomeScreen(
                isLandscape = isLandscape,
                onDismiss = {
                    tryNavigateBack { onDismiss() }
                },
                onNavigate = { currentPage = it },
            )
        }
        is SettingsPage.Category -> {
            SettingsCategoryScreen(
                category = page.category,
                onBack = {
                    tryNavigateBack { currentPage = SettingsPage.Home }
                },
                onNavigate = { currentPage = it },
            )
        }
        is SettingsPage.Detail -> {
            SettingsDetailScreen(
                destination = page.destination,
                settings = workingSettings,
                isLandscape = isLandscape,
                hasChanges = hasChanges,
                onSettingsChange = ::onWorkingSettingsChange,
                onSave = {
                    saveAndDismiss()
                },
                onBack = {
                    tryNavigateBack {
                        currentPage = if (isLandscape) {
                            SettingsPage.Home
                        } else {
                            SettingsPage.Category(page.destination.category)
                        }
                    }
                },
            )
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = {
                showDiscardDialog = false
                pendingBackAction = null
            },
            containerColor = SettingsColors.SurfaceElevated,
            title = {
                Text(
                    "Unsaved Changes",
                    color = SettingsColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    "You have unsaved changes. What would you like to do?",
                    color = SettingsColors.TextSecondary,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    saveAndDismiss()
                    showDiscardDialog = false
                    pendingBackAction?.invoke()
                    pendingBackAction = null
                }) {
                    Text(
                        "Save & Go Back",
                        color = SettingsColors.Success,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    // Discard changes — reset working copy
                    workingSettings = settings
                    hasChanges = false
                    showDiscardDialog = false
                    pendingBackAction?.invoke()
                    pendingBackAction = null
                }) {
                    Text(
                        "Discard",
                        color = SettingsColors.Danger,
                        fontSize = 12.sp,
                    )
                }
            },
        )
    }
}