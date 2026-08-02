package app.sprout.habits.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.Composable

/** A ring that fills clockwise from 12 o'clock. Stroke sits inside the bounds. */
@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    trackColor: Color,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) { drawRing(progress, color, trackColor, strokeWidth.toPx()) }
}

fun DrawScope.drawRing(progress: Float, color: Color, trackColor: Color, stroke: Float) {
    val inset = stroke / 2
    val topLeft = Offset(inset, inset)
    val arcSize = Size(size.width - stroke, size.height - stroke)
    drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
    val sweep = 360f * progress.coerceIn(0f, 1f)
    if (sweep > 0f) drawArc(color, -90f, sweep, false, topLeft, arcSize, style = Stroke(stroke))
}
