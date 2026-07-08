package com.arena.hidcompatibilitytester.ui.screen.landscape.trackpad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arena.hidcompatibilitytester.ui.screen.trackpad.TrackpadClickButtons
import com.arena.hidcompatibilitytester.ui.screen.trackpad.TrackpadSettings
import com.arena.hidcompatibilitytester.ui.screen.trackpad.TrackpadSurface

@Composable
fun LandscapeTrackpad(
    settings: TrackpadSettings,
    holdEnabled: Boolean,
    onSendMouse: (dx: Int, dy: Int, buttons: Int, wheel: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val physHoldActive = remember { mutableStateOf(holdEnabled) }

    // Keep physHoldActive in sync with holdEnabled from parent
    LaunchedEffect(holdEnabled) {
        physHoldActive.value = holdEnabled
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080F18))
    ) {
        // Reuse the exact same TrackpadSurface from portrait
        TrackpadSurface(
            modifier       = Modifier.weight(1f),
            settings       = settings,
            onSendMouse    = onSendMouse,
            physHoldActive = physHoldActive
        )

        // Reuse the exact same click buttons from portrait
        TrackpadClickButtons(
            onSendMouse    = onSendMouse,
            physHoldActive = physHoldActive
        )
    }
}