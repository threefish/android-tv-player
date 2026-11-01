package com.tvplayer.app.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val Shape: Shape = CircleShape
    val ContentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
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
    contentPadding: PaddingValues = AppButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val src = interactionSource ?: remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val pressed by src.collectIsPressedAsState()

    val (bg, fg) = when {
        !enabled -> colors.disabledContainerColor to colors.disabledContentColor
        pressed -> colors.focusedContainerColor to colors.focusedContentColor
        focused -> colors.focusedContainerColor to colors.focusedContentColor
        else -> colors.containerColor to colors.contentColor
    }

    Row(
        modifier = modifier
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
    shape: Shape = AppButtonDefaults.Shape,
    colors: AppButtonColors = AppButtonDefaults.colors(),
    contentPadding: PaddingValues = AppButtonDefaults.IconContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val src = interactionSource ?: remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val pressed by src.collectIsPressedAsState()

    val (bg, fg) = when {
        !enabled -> colors.disabledContainerColor to colors.disabledContentColor
        pressed -> colors.focusedContainerColor to colors.focusedContentColor
        focused -> colors.focusedContainerColor to colors.focusedContentColor
        else -> colors.containerColor to colors.contentColor
    }

    Box(
        modifier = modifier
            .combinedClickable(
                interactionSource = src,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .background(bg, shape)
            .clip(shape)
            .padding(contentPadding),
    ) {
        androidx.compose.material3.ProvideTextStyle(
            value = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(color = fg)
        ) {
            content()
        }
    }
}
