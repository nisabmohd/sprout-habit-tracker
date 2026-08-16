package app.sprout.habits.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import app.sprout.habits.domain.DayOutcome
import app.sprout.habits.ui.theme.HabitColors

/** How one habit looks on one day in the Week view, calendars and widgets. */
enum class MarkKind { DONE, PARTIAL, SKIP, OPEN_TODAY, FUTURE, NOT_SCHEDULED }

@Immutable
data class DayMark(val kind: MarkKind, val fraction: Float = 0f)

fun markFor(scheduled: Boolean, outcome: DayOutcome, fraction: Float, isFuture: Boolean): DayMark = when {
    !scheduled -> DayMark(MarkKind.NOT_SCHEDULED)
    isFuture -> DayMark(MarkKind.FUTURE)
    outcome == DayOutcome.DONE -> DayMark(MarkKind.DONE, 1f)
    outcome == DayOutcome.PARTIAL -> DayMark(MarkKind.PARTIAL, fraction)
    outcome == DayOutcome.SKIP -> DayMark(MarkKind.SKIP)
    else -> DayMark(MarkKind.OPEN_TODAY)
}

/** Colors a mark needs, besides the habit's own. */
@Immutable
data class MarkColors(val skip: Color, val skipInk: Color, val outline: Color)

/**
 * A circular day mark. Each state differs in shape, not only color: check, pie, dash, dashed
 * ring, dotted ring, and a small dot for days the habit isn't scheduled.
 */
@Composable
fun DayMarkView(mark: DayMark, habit: HabitColors, colors: MarkColors, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawDayMark(mark, habit, colors) }
}

fun DrawScope.drawDayMark(mark: DayMark, habit: HabitColors, colors: MarkColors) {
    val r = size.minDimension / 2
    val c = center
    val glyph = r * 0.42f
    val glyphStroke = r * 0.13f
    when (mark.kind) {
        MarkKind.DONE -> {
            drawCircle(habit.solid, r, c)
            drawCheck(c, glyph, glyphStroke, habit.on)
        }
        MarkKind.PARTIAL -> {
            drawCircle(habit.mid, r, c)
            drawArc(habit.solid, -90f, 360f * mark.fraction.coerceIn(0f, 1f), true, topLeft = Offset(c.x - r, c.y - r), size = androidx.compose.ui.geometry.Size(2 * r, 2 * r))
        }
        MarkKind.SKIP -> {
            drawCircle(colors.skip, r, c)
            drawLine(colors.skipInk, Offset(c.x - glyph * 0.6f, c.y), Offset(c.x + glyph * 0.6f, c.y), glyphStroke, StrokeCap.Round)
        }
        MarkKind.OPEN_TODAY -> {
            val w = r * 0.1f
            drawCircle(habit.solid, r - w / 2, c, style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(r * 0.28f, r * 0.2f))))
        }
        MarkKind.FUTURE -> {
            val w = r * 0.06f
            drawCircle(colors.outline, r - w / 2, c, style = Stroke(w, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.01f, r * 0.22f))))
        }
        MarkKind.NOT_SCHEDULED -> drawCircle(colors.outline, r * 0.1f, c)
    }
}

private fun DrawScope.drawCheck(c: Offset, s: Float, stroke: Float, color: Color) {
    // Same shape as ic_check: M5 12.5 l4.5 4.5 L19 7.5 in a 24 box, centred on c.
    val unit = s / 7f
    fun p(x: Float, y: Float) = Offset(c.x + (x - 12f) * unit, c.y + (y - 12.25f) * unit)
    drawLine(color, p(5f, 12.5f), p(9.5f, 17f), stroke, StrokeCap.Round)
    drawLine(color, p(9.5f, 17f), p(19f, 7.5f), stroke, StrokeCap.Round)
}
