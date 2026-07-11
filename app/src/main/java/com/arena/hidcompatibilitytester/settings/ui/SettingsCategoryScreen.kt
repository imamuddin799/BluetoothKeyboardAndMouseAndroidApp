package com.arena.hidcompatibilitytester.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Page 2 (PORTRAIT ONLY) — Category detail. Shows all destinations in one category.
 * Tapping a destination navigates to Page 3 (SettingsDetailScreen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCategoryScreen(
    category: SettingsCategory,
    onBack: () -> Unit,
    onNavigate: (SettingsPage) -> Unit,
) {
    val destinations = SettingsDestination.entries.filter { it.category == category }

    Scaffold(
        containerColor = SettingsColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(category.icon, fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            category.label,
                            color = SettingsColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SettingsColors.Background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            destinations.forEach { dest ->
                SettingsNavRow(
                    icon = dest.icon,
                    label = dest.label,
                    subtitle = if (!dest.enabled) "Coming soon" else null,
                    enabled = dest.enabled,
                    onClick = { onNavigate(SettingsPage.Detail(dest)) },
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}