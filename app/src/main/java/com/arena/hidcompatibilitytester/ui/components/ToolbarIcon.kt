package com.arena.hidcompatibilitytester.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ToolbarIcon(
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