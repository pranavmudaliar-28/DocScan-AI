package com.example.docscanai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Applies a premium shimmering effect to any component.
 * Use this for skeleton loading states to replace static spinners.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    var size = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.value.width.toFloat(),
        targetValue = 2 * size.value.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFE2E8F0), // slate-200
                Color(0xFFF1F5F9), // slate-100
                Color(0xFFE2E8F0), // slate-200
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.value.width.toFloat(), size.value.height.toFloat())
        )
    ).onGloballyPositioned {
        size.value = it.size
    }
}
