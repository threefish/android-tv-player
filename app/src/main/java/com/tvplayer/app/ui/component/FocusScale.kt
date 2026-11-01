package com.tvplayer.app.ui.component

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.zIndex

fun Modifier.tvFocusScale(
    scale: Float = 1.15f,
    durationMs: Int = 150,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    this
        .focusable(interactionSource = interactionSource)
        .zIndex(if (focused) 1f else 0f)
}
