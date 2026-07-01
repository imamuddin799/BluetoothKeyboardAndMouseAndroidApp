// ui/components/AdaptivePadding.kt
package com.arena.hidcompatibilitytester.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AdaptiveSpacing(
    val horizontal: Dp,
    val vertical: Dp,
    val cardSpacing: Dp,
    val innerCardPadding: Dp,
    val itemSpacing: Dp,
    val chipMinWidth: Dp,
    val chipSpacing: Dp,
)

@Composable
fun rememberAdaptiveSpacing(): AdaptiveSpacing {
    val screenWidth = LocalConfiguration.current.screenWidthDp

    return when {
        screenWidth < 360 -> AdaptiveSpacing(
            horizontal = 8.dp,
            vertical = 8.dp,
            cardSpacing = 8.dp,
            innerCardPadding = 10.dp,
            itemSpacing = 10.dp,
            chipMinWidth = 72.dp,
            chipSpacing = 4.dp,
        )
        screenWidth < 420 -> AdaptiveSpacing(
            horizontal = 12.dp,
            vertical = 12.dp,
            cardSpacing = 10.dp,
            innerCardPadding = 12.dp,
            itemSpacing = 12.dp,
            chipMinWidth = 82.dp,
            chipSpacing = 6.dp,
        )
        screenWidth < 600 -> AdaptiveSpacing(
            horizontal = 16.dp,
            vertical = 14.dp,
            cardSpacing = 12.dp,
            innerCardPadding = 14.dp,
            itemSpacing = 14.dp,
            chipMinWidth = 88.dp,
            chipSpacing = 8.dp,
        )
        else -> AdaptiveSpacing(
            horizontal = 24.dp,
            vertical = 16.dp,
            cardSpacing = 14.dp,
            innerCardPadding = 16.dp,
            itemSpacing = 16.dp,
            chipMinWidth = 100.dp,
            chipSpacing = 10.dp,
        )
    }
}