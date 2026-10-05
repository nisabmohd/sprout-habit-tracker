package app.sprout.habits.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** A ring that fills clockwise from 12 o'clock (the Today widget). Stroke sits inside the bounds. */
fun DrawScope.drawRing(progress: Float, color: Color, trackColor: Color, stroke: Float) {
    val inset = stroke / 2
    val topLeft = Offset(inset, inset)
    val arcSize = Size(size.width - stroke, size.height - stroke)
    drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
    val sweep = 360f * progress.coerceIn(0f, 1f)
    if (sweep > 0f) drawArc(color, -90f, sweep, false, topLeft, arcSize, style = Stroke(stroke))
}
