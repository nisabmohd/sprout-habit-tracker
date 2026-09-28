package app.sprout.habits.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.sprout.habits.R

/** A slim strip: "5 of 8 today", the next habit, and one bar per habit. */
class StripWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataSource(context.container.repository, context.container.settings).today()
        val bitmaps = WidgetBitmaps(context)
        provideContent {
            val live by liveWidgetData(context, data) { it.today() }
            SproutGlanceTheme { StripContent(live, bitmaps) }
        }
    }
}

@Composable
private fun StripContent(data: TodayWidgetData, bitmaps: WidgetBitmaps) {
    val context = LocalContext.current
    val colors = GlanceTheme.colors
    val width = (LocalSize.current.width.value - 36).coerceAtLeast(100f)
    fun color(p: androidx.glance.unit.ColorProvider) = p.getColor(context)
    Column(
        GlanceModifier
            .fillMaxSize()
            .background(colors.widgetBackground)
            .cornerRadius(28.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .clickable(actionStartActivity(context.openAppIntent())),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Row(GlanceModifier.fillMaxWidth()) {
            Text(LocalContext.current.getString(R.string.widget_of_today, data.done, data.total), style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.onSurface))
            Spacer(GlanceModifier.defaultWeight())
            Text(
                data.nextLabel?.let { context.getString(R.string.widget_next, it) } ?: context.getString(R.string.widget_all_done),
                maxLines = 1,
                style = TextStyle(fontSize = 14.sp, color = colors.onSurfaceVariant),
            )
        }
        Spacer(GlanceModifier.height(10.dp))
        Image(
            ImageProvider(bitmaps.strip(data.today, width, 10f, track = color(colors.outline).copy(alpha = 0.35f), skip = color(colors.surfaceVariant))),
            contentDescription = LocalContext.current.getString(R.string.widget_habits_done_today, data.done, data.total),
            modifier = GlanceModifier.fillMaxWidth().height(10.dp),
        )
    }
}

class StripWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StripWidget()
}
