package com.tvplayer.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppDarkColorScheme = darkColorScheme(
    background = AppColors.background,
    onBackground = AppColors.onBackground,
    surface = AppColors.surface,
    onSurface = AppColors.onSurface,
    surfaceVariant = AppColors.surfaceVariant,
    onSurfaceVariant = AppColors.onSurfaceVariant,
    primary = AppColors.primary,
    onPrimary = AppColors.onPrimary,
    primaryContainer = AppColors.primaryContainer,
    onPrimaryContainer = AppColors.onPrimaryContainer,
    secondary = AppColors.secondary,
    onSecondary = AppColors.onSecondary,
    secondaryContainer = AppColors.secondaryContainer,
    onSecondaryContainer = AppColors.onSecondaryContainer,
    tertiary = AppColors.tertiary,
    onTertiary = AppColors.onTertiary,
    tertiaryContainer = AppColors.tertiaryContainer,
    onTertiaryContainer = AppColors.onTertiaryContainer,
    error = AppColors.error,
    onError = AppColors.onError,
    errorContainer = AppColors.errorContainer,
    onErrorContainer = AppColors.onErrorContainer,
    outline = AppColors.outline,
    outlineVariant = AppColors.outlineVariant,
    inverseSurface = AppColors.inverseSurface,
    inverseOnSurface = AppColors.inverseOnSurface,
    surfaceTint = AppColors.surfaceTint,
    surfaceBright = AppColors.surfaceBright,
    surfaceDim = AppColors.surfaceDim,
    surfaceContainer = AppColors.surfaceContainer,
    surfaceContainerLow = AppColors.surfaceContainerLow,
    surfaceContainerHigh = AppColors.surfaceContainerHigh,
    surfaceContainerHighest = AppColors.surfaceContainerHighest,
    surfaceContainerLowest = AppColors.surfaceContainerLowest,
    scrim = AppColors.scrim,
)

@Composable
fun TVPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppDarkColorScheme,
        typography = AppTypography,
        shapes = AppM3Shapes,
        content = content,
    )
}
