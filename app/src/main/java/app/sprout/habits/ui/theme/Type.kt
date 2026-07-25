package app.sprout.habits.ui.theme

import androidx.annotation.FontRes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.sprout.habits.R

private val weights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

/** One bundled variable font, instanced at each weight the app uses. */
@OptIn(ExperimentalTextApi::class)
private fun variableFamily(@FontRes res: Int) = FontFamily(
    weights.map { w ->
        Font(res, weight = w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))
    },
)

val OutfitFamily = variableFamily(R.font.outfit)
private val FigtreeFamily = variableFamily(R.font.figtree)
private val LexendFamily = variableFamily(R.font.lexend)
private val AtkinsonFamily = variableFamily(R.font.atkinson_hyperlegible)

fun BodyFont.family(): FontFamily = when (this) {
    BodyFont.SYSTEM -> FontFamily.Default
    BodyFont.FIGTREE -> FigtreeFamily
    BodyFont.OUTFIT -> OutfitFamily
    BodyFont.LEXEND -> LexendFamily
    BodyFont.ATKINSON -> AtkinsonFamily
}

/**
 * The type scale from CLAUDE.md. Only these sizes exist: 40, 28, 22, 16, 14, 12, 11.
 * Headings (display, headline, titleLarge) use Outfit; everything else uses [body].
 */
fun sproutTypography(body: BodyFont, scale: Float): Typography {
    val bodyFamily = body.family()
    fun style(family: FontFamily, size: Int, weight: FontWeight) = TextStyle(
        fontFamily = family,
        fontWeight = weight,
        fontSize = (size * scale).sp,
        lineHeight = (size * 1.3f * scale).sp,
    )
    val display = style(OutfitFamily, 40, FontWeight.SemiBold)
    val screenTitle = style(OutfitFamily, 28, FontWeight.SemiBold)
    val title = style(OutfitFamily, 22, FontWeight.SemiBold)
    return Typography(
        // Display number: amount stepper, widget streak
        displayLarge = display,
        displayMedium = display,
        displaySmall = display,
        // Screen title: Today, Habits, Journal, Insights, More, big stat numbers
        headlineLarge = screenTitle,
        headlineMedium = screenTitle,
        // Title: top-bar titles, section headings, medium numbers
        headlineSmall = title,
        titleLarge = title,
        // Body / item title
        titleMedium = style(bodyFamily, 16, FontWeight.SemiBold),
        bodyLarge = style(bodyFamily, 16, FontWeight.Normal),
        // Supporting: subtitles, chips, buttons, segmented buttons
        titleSmall = style(bodyFamily, 14, FontWeight.SemiBold),
        bodyMedium = style(bodyFamily, 14, FontWeight.Normal),
        labelLarge = style(bodyFamily, 14, FontWeight.SemiBold),
        // Label: nav bar labels, legends, small tags
        labelMedium = style(bodyFamily, 12, FontWeight.SemiBold),
        bodySmall = style(bodyFamily, 12, FontWeight.SemiBold),
        // Tiny: weekday letters, heatmap month labels
        labelSmall = style(bodyFamily, 11, FontWeight.SemiBold),
    )
}
