package app.sprout.habits.ui.theme

import androidx.compose.runtime.Immutable

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class BodyFont { SYSTEM, FIGTREE, OUTFIT, LEXEND, ATKINSON }

/** Everything the theme needs from user settings. */
@Immutable
data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    /** Seed hue for the app chrome when dynamic color is off or unavailable. */
    val accentHue: Float = DEFAULT_ACCENT_HUE,
    val font: BodyFont = BodyFont.FIGTREE,
    /** Multiplies every type-scale size, on top of the system font scale. */
    val textScale: Float = 1f,
)

const val DEFAULT_ACCENT_HUE = 150f
