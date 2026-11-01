package com.tvplayer.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.W400),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.W500),
    headlineSmall = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.W400),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.W600),
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.W700, lineHeight = 20.sp),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W600, lineHeight = 28.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.W400, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W400, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.W500, letterSpacing = 0.1.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W700),
    labelMedium = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.W700),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.W600, letterSpacing = 0.65.sp),
)
