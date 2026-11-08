package com.tvplayer.app.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tvplayer.app.ui.theme.AppColors

@Immutable
data class AppButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val focusedContainerColor: Color,
    val focusedContentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
)

object AppButtonDefaults {
    val Shape: Shape = RoundedCornerShape(10.dp)
    val ContentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
    val IconContentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 10.dp)

    fun colors(
        containerColor: Color = AppColors.button,
        contentColor: Color = AppColors.onButton,
        focusedContainerColor: Color = AppColors.buttonFocused,
        focusedContentColor: Color = AppColors.onButtonFocused,
        disabledContainerColor: Color = AppColors.buttonDisabled,
        disabledContentColor: Color = AppColors.onButtonDisabled,
    ) = AppButtonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        focusedContainerColor = focusedContainerColor,
        focusedContentColor = focusedContentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    )

    fun dangerColors() = colors(
        contentColor = AppColors.danger,
        focusedContainerColor = AppColors.dangerFocused,
        focusedContentColor = Color.White,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = AppButtonDefaults.Shape,
    colors: AppButtonColors = AppButtonDefaults.colors(),
    borderColor: Color = Color(0x26FFFFFF),
    contentPadding: PaddingValues = AppButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val src = interactionSource ?: remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val pressed by src.collectIsPressedAsState()
    val active = focused || pressed
    val scale by animateFloatAsState(
        targetValue = if (active) 1.05f else 1f,
        animationSpec = tween(150),
        label = "buttonScale",
    )

    val (bg, fg) = when {
        !enabled -> colors.disabledContainerColor to colors.disabledContentColor
        active -> colors.focusedContainerColor to colors.focusedContentColor
        else -> colors.containerColor to colors.contentColor
    }

    Row(
        modifier = modifier
            .zIndex(if (active) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .defaultMinSize(minWidth = 58.dp, minHeight = 40.dp)
            .combinedClickable(
                interactionSource = src,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .background(bg, shape)
            .then(
                if (active) Modifier else Modifier.border(1.dp, borderColor, shape)
            )
            .clip(shape)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        androidx.compose.material3.ProvideTextStyle(
            value = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(color = fg)
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    colors: AppButtonColors = AppButtonDefaults.colors(),
    borderColor: Color = Color(0x26FFFFFF),
    contentPadding: PaddingValues = AppButtonDefaults.IconContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val src = interactionSource ?: remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val pressed by src.collectIsPressedAsState()
    val active = focused || pressed
    val scale by animateFloatAsState(
        targetValue = if (active) 1.08f else 1f,
        animationSpec = tween(150),
        label = "iconButtonScale",
    )

    val (bg, fg) = when {
        !enabled -> colors.disabledContainerColor to colors.disabledContentColor
        active -> colors.focusedContainerColor to colors.focusedContentColor
        else -> colors.containerColor to colors.contentColor
    }

    Box(
        modifier = modifier
            .zIndex(if (active) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                interactionSource = src,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .background(bg, shape)
            .then(
                if (active) Modifier else Modifier.border(1.dp, borderColor, shape)
            )
            .clip(shape)
            .padding(contentPadding),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides fg
        ) {
            androidx.compose.material3.ProvideTextStyle(
                value = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(color = fg)
            ) {
                content()
            }
        }
    }
}
