package com.tvplayer.app.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tvplayer.app.ui.theme.AppColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(20.dp),
    containerColor: Color = AppColors.card,
    focusedColor: Color = AppColors.cardHover,
    focusedScale: Float = 1.06f,
    focusedLift: Dp = 4.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val bg by animateColorAsState(
        targetValue = if (focused) focusedColor else containerColor,
        animationSpec = tween(durationMillis = 150),
        label = "cardFocus",
    )
    val scale by animateFloatAsState(
        targetValue = if (focused) focusedScale else 1f,
        animationSpec = tween(150),
        label = "cardScale",
    )
    val lift by animateDpAsState(
        targetValue = if (focused) focusedLift else 0.dp,
        animationSpec = tween(150),
        label = "cardLift",
    )

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = -lift.toPx()
            }
            .shadow(
                elevation = if (focused) 18.dp else 0.dp,
                shape = shape,
                ambientColor = AppColors.focusShadow,
                spotColor = AppColors.focusShadow,
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .background(bg, shape)
            .border(
                width = 1.dp,
                color = if (focused) AppColors.focusRing else AppColors.cardBorder,
                shape = shape,
            )
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
    shape: Shape = RoundedCornerShape(16.dp),
    containerColor: Color = AppColors.card,
    focusedColor: Color = AppColors.cardHover,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val bg by animateColorAsState(
        targetValue = if (focused) focusedColor else containerColor,
        animationSpec = tween(durationMillis = 150),
        label = "rowFocus",
    )
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.02f else 1f,
        animationSpec = tween(150),
        label = "rowScale",
    )

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (focused) 12.dp else 0.dp,
                shape = shape,
                ambientColor = AppColors.focusShadow,
                spotColor = AppColors.focusShadow,
            )
            .combinedClickable(
                interactionSource = src,
                indication = null,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .background(bg, shape)
            .border(
                width = 1.dp,
                color = if (focused) AppColors.focusRing else AppColors.cardBorder,
                shape = shape,
            )
            .clip(shape),
    ) {
        content()
    }
}
