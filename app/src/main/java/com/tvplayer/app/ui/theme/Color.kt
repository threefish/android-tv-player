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

    private val Cyan500 = Color(0xFF00A4DD)
    private val Cyan100 = Color(0xFFD1F6FF)
    private val Cyan800 = Color(0xFF00435C)

    private val Red500 = Color(0xFFDF2222)

    val background = Grey975
    val onBackground = BlueGrey25
    val surface = BlueGrey900
    val onSurface = BlueGrey25
    val surfaceVariant = BlueGrey850
    val onSurfaceVariant = BlueGrey300
    val primary = Cyan500
    val onPrimary = BlueGrey100
    val primaryContainer = Cyan800
    val onPrimaryContainer = Cyan100
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

    val button = Color(0xB3747474)
    val onButton = Color(0xFFDDDDDD)
    val buttonFocused = Color(0xE6CCCCCC)
    val onButtonFocused = Color(0xFF444444)
    val buttonDisabled = Color(0x33747474)
    val onButtonDisabled = Color(0xFF686868)

    val listHeader = Grey50
    val listHeadline = Color(0xFFFCFCFC)
    val listCaption = Grey200
    val listOverline = Grey500
    val listButton = Color.Transparent
    val listButtonFocused = BlueGrey800

    val badge = Cyan500
    val onBadge = BlueGrey100

    val success = Color(0xFF4CAF50)
}
