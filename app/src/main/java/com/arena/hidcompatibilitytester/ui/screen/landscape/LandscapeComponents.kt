package com.arena.hidcompatibilitytester.ui.screen.landscape

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp

// ═════════════════════════════════════════════════════════════════
// NotReadyCard — landscape copy
// ═════════════════════════════════════════════════════════════════

@Composable
fun LandscapeNotReadyCard() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF57F17).copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⏳", fontSize = 32.sp)
                Text("Not Connected", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "Start BLE HID → pair from host Bluetooth settings → come back here.",
                    fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// ToolbarIcon — landscape copy
// ═════════════════════════════════════════════════════════════════

@Composable
fun LandscapeToolbarIcon(
    icon: String,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(5.dp))
            .border(
                width = if (active) 1.dp else 0.5.dp,
                color = if (active) Color(0xFF4A90D9) else Color(0xFF607D8B).copy(0.3f),
                shape = RoundedCornerShape(5.dp)
            )
            .background(
                if (active) Color(0xFF1565C0).copy(0.25f) else Color(0xFF1A2332)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            icon,
            fontSize = 14.sp,
            color = if (active) Color.White else Color(0xFF90CAF9)
        )
    }
}

// ═════════════════════════════════════════════════════════════════
// AdaptiveSpacing — landscape copy
// ═════════════════════════════════════════════════════════════════

data class LandscapeAdaptiveSpacing(
    val horizontal: Dp,
    val vertical: Dp,
    val cardSpacing: Dp,
    val innerCardPadding: Dp,
    val itemSpacing: Dp,
    val chipMinWidth: Dp,
    val chipSpacing: Dp,
)

@Composable
fun landscapeRememberAdaptiveSpacing(): LandscapeAdaptiveSpacing {
    val screenWidth = LocalConfiguration.current.screenWidthDp

    return when {
        screenWidth < 360 -> LandscapeAdaptiveSpacing(
            horizontal = 8.dp, vertical = 8.dp, cardSpacing = 8.dp,
            innerCardPadding = 10.dp, itemSpacing = 10.dp,
            chipMinWidth = 72.dp, chipSpacing = 4.dp,
        )
        screenWidth < 420 -> LandscapeAdaptiveSpacing(
            horizontal = 12.dp, vertical = 12.dp, cardSpacing = 10.dp,
            innerCardPadding = 12.dp, itemSpacing = 12.dp,
            chipMinWidth = 82.dp, chipSpacing = 6.dp,
        )
        screenWidth < 600 -> LandscapeAdaptiveSpacing(
            horizontal = 16.dp, vertical = 14.dp, cardSpacing = 12.dp,
            innerCardPadding = 14.dp, itemSpacing = 14.dp,
            chipMinWidth = 88.dp, chipSpacing = 8.dp,
        )
        else -> LandscapeAdaptiveSpacing(
            horizontal = 24.dp, vertical = 16.dp, cardSpacing = 14.dp,
            innerCardPadding = 16.dp, itemSpacing = 16.dp,
            chipMinWidth = 100.dp, chipSpacing = 10.dp,
        )
    }
}