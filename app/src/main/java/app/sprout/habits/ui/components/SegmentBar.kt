package app.sprout.habits.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import app.sprout.habits.ui.theme.habitColors

/** One habit's part of a [SegmentBar]: filled in the habit's color up to [fill] (0..1). */
@Immutable
data class BarSegment(val hue: Float, val fill: Float, val skipped: Boolean = false)

/** One 8 dp bar per habit: solid when full, part-filled otherwise, faint when skipped. */
@Composable
fun SegmentBar(segments: List<BarSegment>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val line = colors.outlineVariant
    val skip = colors.surfaceContainerHigh
    val solids = segments.map { habitColors(it.hue).solid }
    Canvas(modifier.fillMaxWidth().height(8.dp)) {
        if (segments.isEmpty()) {
            drawRoundRect(line, cornerRadius = CornerRadius(size.height / 2))
            return@Canvas
        }
        val gap = 4.dp.toPx()
        val w = (size.width - gap * (segments.size - 1)) / segments.size
        val r = CornerRadius(size.height / 2)
        segments.forEachIndexed { i, s ->
            val x = i * (w + gap)
            drawRoundRect(if (s.skipped) skip else line, Offset(x, 0f), Size(w, size.height), r)
            if (s.fill > 0f) {
                clipRect(left = x, right = x + w * s.fill.coerceAtMost(1f)) { drawRoundRect(solids[i], Offset(x, 0f), Size(w, size.height), r) }
            }
        }
    }
}
