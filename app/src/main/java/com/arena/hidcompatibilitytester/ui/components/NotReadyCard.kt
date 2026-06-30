package com.arena.hidcompatibilitytester.ui.components

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

@Composable
fun NotReadyCard() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors   = CardDefaults.cardColors(containerColor = Color(0xFFF57F17).copy(alpha = 0.1f))
        ) {
            Column(
                modifier            = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⏳", fontSize = 32.sp)
                Text("Not Connected", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "Start BLE HID → pair from host Bluetooth settings → come back here.",
                    fontSize  = 13.sp, color = Color.Gray, textAlign = TextAlign.Center
                )
            }
        }
    }
}