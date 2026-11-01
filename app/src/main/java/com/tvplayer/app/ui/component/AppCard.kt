package com.tvplayer.app.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.zIndex
import com.tvplayer.app.ui.theme.AppColors
import com.tvplayer.app.ui.theme.AppShapes

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    shape: Shape = AppShapes.medium,
    containerColor: Color = AppColors.surface,
    focusedColor: Color = AppColors.listButtonFocused,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val bg by animateColorAsState(
        targetValue = if (focused) focusedColor else containerColor,
        animationSpec = tween(durationMillis = 150),
        label = "cardFocus",
    )

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .zIndex(if (focused) 1f else 0f)
            .background(bg, shape)
            .clip(shape),
    ) {
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    shape: Shape = AppShapes.medium,
    containerColor: Color = AppColors.surface,
    focusedColor: Color = AppColors.listButtonFocused,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val bg by animateColorAsState(
        targetValue = if (focused) focusedColor else containerColor,
        animationSpec = tween(durationMillis = 150),
        label = "rowFocus",
    )

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .combinedClickable(
                interactionSource = src,
                indication = null,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .background(bg, shape)
            .clip(shape),
    ) {
        content()
    }
}
