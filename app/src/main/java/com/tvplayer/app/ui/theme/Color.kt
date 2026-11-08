package com.tvplayer.app.ui.theme

import androidx.compose.ui.graphics.Color

object AppColors {
    private val Grey975 = Color(0xFF080808)
    private val Grey950 = Color(0xFF121212)
    private val Grey850 = Color(0xFF2A2A2A)
    private val Grey500 = Color(0xFF808080)
    private val Grey200 = Color(0xFFCFCFCF)
    private val Grey50 = Color(0xFFF5F5F5)

    private val BlueGrey900 = Color(0xFF1C2026)
    private val BlueGrey850 = Color(0xFF272A30)
    private val BlueGrey800 = Color(0xFF36393F)
    private val BlueGrey700 = Color(0xFF474A52)
    private val BlueGrey300 = Color(0xFFB5B9BF)
    private val BlueGrey100 = Color(0xFFE8EAED)
    private val BlueGrey25 = Color(0xFFFCFCFD)

    private val Blue500 = Color(0xFF3B82F6)
    private val Blue100 = Color(0xFFDBEAFE)
    private val Blue800 = Color(0xFF1E3A8A)

    private val Red500 = Color(0xFFDF2222)

    // 原型设计 token：深蓝背景 + 半透明白卡片 + 白色聚焦态
    val pageTop = Color(0xFF1A2236)
    val pageMid = Color(0xFF0D111A)
    val pageBottom = Color(0xFF07090D)
    val card = Color(0x0FFFFFFF)
    val cardBorder = Color(0x1AFFFFFF)
    val cardHover = Color(0x1FFFFFFF)
    val focusRing = Color(0xFFFFFFFF)
    val focusShadow = Color(0x4DFFFFFF)
    val accent = Color(0xFF2563EB)
    val accentSoft = Color(0xFF60A5FA)
    val textPrimary = Color(0xFFF8FAFC)
    val textMuted = Color(0xFF94A3B8)
    val inset = Color(0x40000000)
    val navBackground = Color(0x4D000000)
    val divider = Color(0x0FFFFFFF)
    val danger = Color(0xFFF87171)
    val dangerFocused = Color(0xFFEF4444)

    val background = Color(0xFF0D111A)
    val onBackground = textPrimary
    val surface = BlueGrey900
    val onSurface = textPrimary
    val surfaceVariant = BlueGrey850
    val onSurfaceVariant = BlueGrey300
    val primary = Blue500
    val onPrimary = Color.White
    val primaryContainer = Blue800
    val onPrimaryContainer = Blue100
    val secondary = BlueGrey800
    val onSecondary = BlueGrey100
    val secondaryContainer = BlueGrey700
    val onSecondaryContainer = BlueGrey100
    val tertiary = Color(0xFF5407A6)
    val onTertiary = Color(0xFFEDDBFF)
    val tertiaryContainer = Color(0xFF300367)
    val onTertiaryContainer = Color(0xFFEDDBFF)
    val error = Red500
    val onError = Color.White
    val errorContainer = Color(0xFF93000A)
    val onErrorContainer = Color(0xFFFFDAD6)
    val outline = BlueGrey700
    val outlineVariant = BlueGrey800
    val inverseSurface = BlueGrey850
    val inverseOnSurface = BlueGrey25
    val surfaceTint = Color.Transparent
    val surfaceBright = BlueGrey850
    val surfaceDim = Grey975
    val surfaceContainer = BlueGrey900
    val surfaceContainerLow = BlueGrey900
    val surfaceContainerHigh = BlueGrey900
    val surfaceContainerHighest = BlueGrey900
    val surfaceContainerLowest = Grey975
    val scrim = Color.Black.copy(alpha = 0.67f)
    val playerScrim = Color(0xCC000000)

    val button = Color(0x14FFFFFF)
    val onButton = textPrimary
    val buttonFocused = Color(0xFFFFFFFF)
    val onButtonFocused = Color(0xFF000000)
    val buttonDisabled = Color(0x0AFFFFFF)
    val onButtonDisabled = Color(0x66FFFFFF)

    val listHeader = textPrimary
    val listHeadline = textPrimary
    val listCaption = textMuted
    val listOverline = Color(0xFF64748B)
    val listButton = Color.Transparent
    val listButtonFocused = cardHover

    val badge = Blue500
    val onBadge = Color.White

    val success = Color(0xFF4CAF50)
}
