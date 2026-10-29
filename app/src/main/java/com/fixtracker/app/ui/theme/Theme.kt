package com.fixtracker.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.fixtracker.app.data.ThemeMode

// Core brand tones
private val Forest = Color(0xFF1E4A3D)
private val ForestDeep = Color(0xFF14352B)
private val ForestSoft = Color(0xFFDDE8E1)
private val Copper = Color(0xFFB8733D)
private val CopperDeep = Color(0xFF8A5228)
private val Bone = Color(0xFFF5F3EE)
private val BoneDeep = Color(0xFFECE8DF)
private val Ink = Color(0xFF171B1A)
private val InkSoft = Color(0xFF5E6662)

private val DarkGreen = Color(0xFF152D26)
private val DarkBg = Color(0xFF0F1312)
private val DarkSurface = Color(0xFF171C1A)
private val DarkInk = Color(0xFFECEFEA)
private val DarkInkSoft = Color(0xFF9CA6A1)

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = ForestSoft,
    onPrimaryContainer = ForestDeep,
    secondary = Copper,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4E5D8),
    onSecondaryContainer = CopperDeep,
    tertiary = Color(0xFF536F7C),
    onTertiary = Color.White,
    background = Bone,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = BoneDeep,
    onSurfaceVariant = InkSoft,
    outline = Color(0xFFD9D4C9),
    outlineVariant = Color(0xFFE6E1D8),
    error = Color(0xFFA94B3F),
    onError = Color.White,
    errorContainer = Color(0xFFF5E1DD),
    onErrorContainer = Color(0xFF6E2A22),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9AC7B1),
    onPrimary = DarkGreen,
    primaryContainer = Color(0xFF24473B),
    onPrimaryContainer = Color(0xFFD6EADF),
    secondary = Color(0xFFD79B6C),
    onSecondary = Color(0xFF38220F),
    secondaryContainer = Color(0xFF3A2A1D),
    onSecondaryContainer = Color(0xFFF4DFC9),
    tertiary = Color(0xFFA5C1CD),
    onTertiary = Color(0xFF112A33),
    background = DarkBg,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = Color(0xFF222926),
    onSurfaceVariant = DarkInkSoft,
    outline = Color(0xFF39423E),
    outlineVariant = Color(0xFF2A322E),
    error = Color(0xFFE39A8E),
    onError = Color(0xFF3D0F08),
    errorContainer = Color(0xFF3A2320),
    onErrorContainer = Color(0xFFF6DAD5),
)

@Immutable
data class FixPalette(
    val copper: Color,
    val copperSoft: Color,
    val copperDeep: Color,
    val gold: Color,
    val blue: Color,
    val positive: Color,
    val positiveSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val warningSoft: Color,
    val hairline: Color,
    val surfaceAlt: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val chartBar: Color,
    val chartBarMuted: Color,
    val dark: Boolean,
)

private val LightPalette = FixPalette(
    copper = Copper,
    copperSoft = Color(0xFFF4E5D8),
    copperDeep = CopperDeep,
    gold = Color(0xFFA9803A),
    blue = Color(0xFF4E6C82),
    positive = Color(0xFF4E7A5E),
    positiveSoft = Color(0xFFE3ECE6),
    danger = Color(0xFFA94B3F),
    dangerSoft = Color(0xFFF5E1DD),
    warningSoft = Color(0xFFF7EDDD),
    hairline = Color(0xFFE3DED4),
    surfaceAlt = Color(0xFFEFECE5),
    heroStart = Color(0xFF245245),
    heroEnd = Color(0xFF15352B),
    chartBar = Copper,
    chartBarMuted = Color(0xFFD9E4DE),
    dark = false,
)

private val DarkPalette = FixPalette(
    copper = Color(0xFFD79B6C),
    copperSoft = Color(0xFF36271B),
    copperDeep = Color(0xFFF0C9A6),
    gold = Color(0xFFD5B273),
    blue = Color(0xFF9DBDD1),
    positive = Color(0xFF93C2A7),
    positiveSoft = Color(0xFF1E3329),
    danger = Color(0xFFE39A8E),
    dangerSoft = Color(0xFF3A2320),
    warningSoft = Color(0xFF35291A),
    hairline = Color(0xFF2A322E),
    surfaceAlt = Color(0xFF202724),
    heroStart = Color(0xFF265043),
    heroEnd = Color(0xFF142D25),
    chartBar = Color(0xFFD79B6C),
    chartBarMuted = Color(0xFF2B3B34),
    dark = true,
)

val LocalFixPalette = staticCompositionLocalOf { LightPalette }

object FixTheme {
    val palette: FixPalette
        @Composable get() = LocalFixPalette.current
}

@Composable
fun FixTrackerTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalFixPalette provides if (dark) DarkPalette else LightPalette,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = FixTypography,
            shapes = FixShapes,
            content = content,
        )
    }
}
