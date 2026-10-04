package app.sprout.habits.ui.theme

import androidx.annotation.FontRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
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
private val SpaceGroteskFamily = variableFamily(R.font.space_grotesk)
private val LexendFamily = variableFamily(R.font.lexend)
private val AtkinsonFamily = variableFamily(R.font.atkinson_hyperlegible)

fun BodyFont.family(): FontFamily = when (this) {
    BodyFont.SYSTEM -> FontFamily.Default
    BodyFont.SPACE_GROTESK -> SpaceGroteskFamily
    BodyFont.OUTFIT -> OutfitFamily
    BodyFont.LEXEND -> LexendFamily
    BodyFont.ATKINSON -> AtkinsonFamily
}

/**
 * The type scale from TYPOGRAPHY.md, as Material slots so Material components pick it up too.
 * Screens never read these slots or set a size or weight themselves: they use [SproutType].
 * Headings (display, headline, titleLarge) use Outfit; everything else uses [body].
 */
fun sproutTypography(body: BodyFont, scale: Float): Typography {
    val bodyFamily = body.family()
    fun style(family: FontFamily, size: Int, weight: FontWeight, lineHeight: Float = 1.3f) = TextStyle(
        fontFamily = family,
        fontWeight = weight,
        fontSize = (size * scale).sp,
        lineHeight = (size * lineHeight * scale).sp,
    )
    val display = style(OutfitFamily, 40, FontWeight.SemiBold)
    val screenTitle = style(OutfitFamily, 28, FontWeight.SemiBold)
    val title = style(OutfitFamily, 22, FontWeight.SemiBold)
    val label = style(bodyFamily, 14, FontWeight.SemiBold)
    return Typography(
        // display
        displayLarge = display,
        displayMedium = display,
        displaySmall = display,
        // screenTitle
        headlineLarge = screenTitle,
        headlineMedium = screenTitle,
        // title
        headlineSmall = title,
        titleLarge = title,
        // cardTitle and body (body reads at 1.5)
        titleMedium = style(bodyFamily, 16, FontWeight.SemiBold),
        bodyLarge = style(bodyFamily, 16, FontWeight.Normal, lineHeight = 1.5f),
        // supporting and label
        bodyMedium = style(bodyFamily, 14, FontWeight.Normal),
        titleSmall = label,
        labelLarge = label,
        // caption (Regular) and its SemiBold form
        bodySmall = style(bodyFamily, 12, FontWeight.Normal),
        labelMedium = style(bodyFamily, 12, FontWeight.SemiBold),
        // tiny
        labelSmall = style(bodyFamily, 11, FontWeight.SemiBold),
    )
}

/**
 * The only text styles screens use; TYPOGRAPHY.md says what each one is for. Colour is not part
 * of a style: set it where the text is drawn.
 */
object SproutType {
    /** Outfit 40 SemiBold: big input numbers, the Welcome headline. */
    val display: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.displayLarge

    /** Outfit 28 SemiBold: tab titles, the habit name in habit detail, hero numbers. */
    val screenTitle: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.headlineMedium

    /** Outfit 22 SemiBold: top-bar, sheet and dialog titles, stat values. */
    val title: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.titleLarge

    /** 16 SemiBold: habit names, card titles, list row titles. */
    val cardTitle: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.titleMedium

    /** 16 Regular, 1.5 line height: note text, paragraphs, text fields. */
    val body: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.bodyLarge

    /** 14 Regular: the line under a cardTitle, plain trailing text, descriptions. */
    val supporting: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.bodyMedium

    /** 14 SemiBold: buttons, chips, section labels, main trailing values, calendar day numbers. */
    val label: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.labelLarge

    /** 12 Regular: helper text, legends, font descriptions. */
    val caption: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.bodySmall

    /** 12 SemiBold: field labels, nav bar labels, small labels inside rings. */
    val captionStrong: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.labelMedium

    /** 11 SemiBold: weekday names, heatmap month labels. */
    val tiny: TextStyle @Composable @ReadOnlyComposable get() = MaterialTheme.typography.labelSmall

    /** Exception: the New note editor, 18 Regular at 1.55, for comfortable writing. */
    val noteEditor: TextStyle
        @Composable @ReadOnlyComposable get() = body.let { it.copy(fontSize = it.fontSize * (18f / 16f), lineHeight = it.fontSize * (18f / 16f) * 1.55f) }
}

/** The smallest a nav bar label may shrink to when a translation is too long for its tab. */
val NavLabelMinSize = 9.sp

/** Sizes for the Glance widgets, which can't use Compose text styles. Same scale as [SproutType]. */
object WidgetType {
    val display = 40.sp
    val screenTitle = 28.sp
    val title = 22.sp
    val cardTitle = 16.sp
    val supporting = 14.sp
    val caption = 12.sp
    val tiny = 11.sp
}
