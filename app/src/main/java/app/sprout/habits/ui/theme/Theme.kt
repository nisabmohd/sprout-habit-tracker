package app.sprout.habits.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

private val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun ThemeSettings.isDark(): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun SproutTheme(
    settings: ThemeSettings = ThemeSettings(),
    content: @Composable () -> Unit,
) {
    val dark = settings.isDark()
    val context = LocalContext.current
    val useDynamic = settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        useDynamic && dark -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        else -> remember(settings.accentHue, dark) { seedColorScheme(settings.accentHue, dark) }
    }
    val typography = remember(settings.font, settings.textScale) {
        sproutTypography(settings.font, settings.textScale)
    }
    CompositionLocalProvider(LocalDarkTheme provides dark) {
        MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
    }
}

/** Habit colors are fixed per habit hue and only follow light/dark, never dynamic color. */
@Composable
@ReadOnlyComposable
fun habitColors(hue: Float): HabitColors = habitColors(hue, LocalDarkTheme.current)
