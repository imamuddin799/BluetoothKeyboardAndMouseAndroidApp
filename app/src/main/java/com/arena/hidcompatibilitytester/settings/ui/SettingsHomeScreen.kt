package com.arena.hidcompatibilitytester.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items

@Composable
fun SettingsHomeScreen(
    isLandscape: Boolean,
    onDismiss: () -> Unit,
    onNavigate: (SettingsPage) -> Unit,
) {
    if (isLandscape) {
        LandscapeSettingsHomeScreen(
            onDismiss = onDismiss,
            onNavigate = onNavigate,
        )
    } else {
        PortraitSettingsHomeScreen(
            onDismiss = onDismiss,
            onNavigate = onNavigate,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortraitSettingsHomeScreen(
    onDismiss: () -> Unit,
    onNavigate: (SettingsPage) -> Unit,
) {
    val expandedCategories = remember {
        mutableStateMapOf<SettingsCategory, Boolean>().apply {
            put(SettingsCategory.GENERAL, true)
        }
    }

    Scaffold(
        containerColor = SettingsColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        color = SettingsColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
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
            SettingsCategory.entries.forEach { category ->
                val destinations = SettingsDestination.entries.filter { it.category == category }
                val isExpanded = expandedCategories[category] == true

                SettingsCollapsibleCategory(
                    icon = category.icon,
                    label = category.label,
                    expanded = isExpanded,
                    onToggle = {
                        expandedCategories[category] = !isExpanded
                    },
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
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun LandscapeSettingsHomeScreen(
    onDismiss: () -> Unit,
    onNavigate: (SettingsPage) -> Unit,
) {
    var selectedCategory by remember { mutableStateOf(SettingsCategory.GENERAL) }

    val destinations = remember(selectedCategory) {
        SettingsDestination.entries.filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsColors.Background),
    ) {
        CompactSettingsHeader(
            title = "Settings",
            onBack = onDismiss,
        )

        Row(modifier = Modifier.fillMaxSize()) {
            // Left rail
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .width(140.dp)
                    .fillMaxHeight()
                    .background(SettingsColors.Surface),
                contentPadding = PaddingValues(top = 0.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(SettingsCategory.entries.size) { index ->
                    val category = SettingsCategory.entries[index]
                    HomeCategoryRailItem(
                        icon = category.icon,
                        label = category.label,
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                    )
                }
            }

            // Right pane
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(
                    start = 10.dp,
                    top = 0.dp,
                    end = 10.dp,
                    bottom = 8.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(
                    count = destinations.size,
                    key = { index -> destinations[index].name },
                ) { index ->
                    val dest = destinations[index]
                    SettingsNavRow(
                        icon = dest.icon,
                        label = dest.label,
                        subtitle = if (!dest.enabled) "Coming soon" else null,
                        enabled = dest.enabled,
                        onClick = { onNavigate(SettingsPage.Detail(dest)) },
                    )
                }

                item(
                    span = {
                        androidx.compose.foundation.lazy.grid.GridItemSpan(2)
                    }
                ) {
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun HomeCategoryRailItem(
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
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                icon,
                fontSize = 15.sp,
                color = iconColor,
            )
            Text(
                label,
                color = fg,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}