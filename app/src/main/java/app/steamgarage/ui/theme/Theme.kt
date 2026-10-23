package app.steamgarage.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.steamgarage.data.ThemeMode

val LocalSteamPalette = staticCompositionLocalOf { DarkPalette }
val LocalSteamText = staticCompositionLocalOf { SteamTextStyles() }

/** true — в системе отключены анимации: стрелки не дрожат, пар не идёт, шестерни стоят. */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Короткий доступ к токенам темы из любого компонента. */
object Steam {
    val palette: SteamPalette
        @Composable @ReadOnlyComposable get() = LocalSteamPalette.current
    val text: SteamTextStyles
        @Composable @ReadOnlyComposable get() = LocalSteamText.current
    val reduceMotion: Boolean
        @Composable @ReadOnlyComposable get() = LocalReduceMotion.current
}

/** Скошенные углы («фаска») у полей и карточек — как у латунных табличек. */
val SteamShapes = Shapes(
    extraSmall = CutCornerShape(4.dp),
    small = CutCornerShape(6.dp),
    medium = CutCornerShape(8.dp),
    large = CutCornerShape(12.dp),
    extraLarge = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
)

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun SteamTheme(
    mode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val palette = if (isDarkTheme(mode)) DarkPalette else LightPalette
    val context = LocalContext.current
    val reduceMotion = remember { animationsDisabled(context) }
    val colorScheme = remember(palette) { palette.toColorScheme() }
    CompositionLocalProvider(
        LocalSteamPalette provides palette,
        LocalSteamText provides SteamTextStyles(),
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SteamTypography,
            shapes = SteamShapes,
            content = content,
        )
    }
}

/** Системный переключатель «Отключить анимации» выставляет масштаб длительности анимаций в 0. */
fun animationsDisabled(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

private fun SteamPalette.toColorScheme(): ColorScheme =
    if (isDark) {
        darkColorScheme(
            primary = brassLight,
            onPrimary = engraved,
            primaryContainer = brassDark,
            onPrimaryContainer = engraved,
            secondary = copperLight,
            onSecondary = onOk,
            secondaryContainer = surfaceDeep,
            onSecondaryContainer = text,
            tertiary = ok,
            onTertiary = onOk,
            background = background,
            onBackground = text,
            surface = surface,
            onSurface = text,
            surfaceVariant = surfaceDeep,
            onSurfaceVariant = textSecondary,
            surfaceTint = surface,
            surfaceBright = surface,
            surfaceDim = background,
            surfaceContainerLowest = surfaceDeep,
            surfaceContainerLow = surface,
            surfaceContainer = surface,
            surfaceContainerHigh = surface,
            surfaceContainerHighest = surface,
            inverseSurface = text,
            inverseOnSurface = background,
            error = overdue,
            onError = onOverdue,
            outline = brassDark,
            outlineVariant = brassShadow,
        )
    } else {
        lightColorScheme(
            primary = brassShadow,
            onPrimary = background,
            primaryContainer = brassLight,
            onPrimaryContainer = engraved,
            secondary = copper,
            onSecondary = background,
            secondaryContainer = surfaceDeep,
            onSecondaryContainer = text,
            tertiary = ok,
            onTertiary = onOk,
            background = background,
            onBackground = text,
            surface = surface,
            onSurface = text,
            surfaceVariant = surfaceDeep,
            onSurfaceVariant = textSecondary,
            surfaceTint = surface,
            surfaceBright = background,
            surfaceDim = surfaceDeep,
            surfaceContainerLowest = background,
            surfaceContainerLow = surface,
            surfaceContainer = surface,
            surfaceContainerHigh = surface,
            surfaceContainerHighest = surfaceDeep,
            inverseSurface = text,
            inverseOnSurface = background,
            error = overdue,
            onError = onOverdue,
            outline = brassShadow,
            outlineVariant = brassDark,
        )
    }
