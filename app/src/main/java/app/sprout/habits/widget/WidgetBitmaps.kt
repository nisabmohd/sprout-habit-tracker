package app.sprout.habits.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import app.sprout.habits.domain.DayOutcome
import app.sprout.habits.ui.components.DayMark
import app.sprout.habits.ui.components.MarkColors
import app.sprout.habits.ui.components.drawDayMark
import app.sprout.habits.ui.components.drawRing
import app.sprout.habits.ui.theme.habitColors
import app.sprout.habits.ui.theme.seedColorScheme

/**
 * Glance has no Canvas, so rings, day marks and strips are drawn into bitmaps with the same
 * drawing code the app uses.
 */
class WidgetBitmaps(private val context: Context) {
    private val density = context.resources.displayMetrics.density
    val dark: Boolean =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    private val neutral = seedColorScheme(150f, dark)
    private val markColors = MarkColors(skip = neutral.outlineVariant, skipInk = neutral.onSurfaceVariant, outline = neutral.outline)

    private fun draw(widthDp: Float, heightDp: Float, block: DrawScope.() -> Unit): Bitmap {
        val w = (widthDp * density).toInt().coerceAtLeast(1)
        val h = (heightDp * density).toInt().coerceAtLeast(1)
        val image = ImageBitmap(w, h)
        CanvasDrawScope().draw(Density(density), LayoutDirection.Ltr, Canvas(image), Size(w.toFloat(), h.toFloat()), block)
        return image.asAndroidBitmap()
    }

    /** A row of day marks, [markDp] each, spread evenly over [widthDp]. */
    fun marks(marks: List<DayMark>, hue: Float, widthDp: Float, markDp: Float): Bitmap {
        val hc = habitColors(hue, dark)
        return draw(widthDp, markDp) {
            val slot = size.width / marks.size
            val m = markDp * density
            marks.forEachIndexed { i, mark ->
                val left = i * slot + (slot - m) / 2
                translate(left, 0f) { drawIntoSize(m) { drawDayMark(mark, hc, markColors) } }
            }
        }
    }

    fun ring(progress: Float, color: Color, track: Color, sizeDp: Float, strokeDp: Float): Bitmap =
        draw(sizeDp, sizeDp) { drawRing(progress, color, track, strokeDp * density) }

    /** One bar per habit, like the Today score card. */
    fun strip(habits: List<WidgetHabit>, widthDp: Float, heightDp: Float, track: Color, skip: Color): Bitmap =
        draw(widthDp, heightDp) {
            if (habits.isEmpty()) {
                drawRoundRect(track, cornerRadius = CornerRadius(size.height / 2))
                return@draw
            }
            val gap = 4 * density
            val w = (size.width - gap * (habits.size - 1)) / habits.size
            val r = CornerRadius(size.height / 2)
            habits.forEachIndexed { i, h ->
                val x = i * (w + gap)
                drawRoundRect(if (h.todayOutcome == DayOutcome.SKIP) skip else track, Offset(x, 0f), Size(w, size.height), r)
                val fill = when (h.todayOutcome) {
                    DayOutcome.DONE -> 1f
                    DayOutcome.PARTIAL -> h.todayProgress
                    else -> 0f
                }
                if (fill > 0f) {
                    clipRect(left = x, right = x + w * fill) {
                        drawRoundRect(habitColors(h.hue, dark).solid, Offset(x, 0f), Size(w, size.height), r)
                    }
                }
            }
        }

    private inline fun DrawScope.translate(x: Float, y: Float, block: DrawScope.() -> Unit) {
        drawContext.transform.translate(x, y)
        block()
        drawContext.transform.translate(-x, -y)
    }

    /** Runs [block] with the draw size set to an [sizePx] square. */
    private inline fun DrawScope.drawIntoSize(sizePx: Float, block: DrawScope.() -> Unit) {
        val old = drawContext.size
        drawContext.size = Size(sizePx, sizePx)
        block()
        drawContext.size = old
    }
}
