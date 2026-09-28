package app.sprout.habits.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.sprout.habits.R
import app.sprout.habits.ui.theme.habitColors

/** Today's ring and the next habits still to do, each with a circle to mark it done. */
class TodayWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataSource(context.container.repository, context.container.settings).today()
        val bitmaps = WidgetBitmaps(context)
        provideContent {
            val live by liveWidgetData(context, data) { it.today() }
            SproutGlanceTheme { TodayContent(live, bitmaps) }
        }
    }
}

@Composable
private fun TodayContent(data: TodayWidgetData, bitmaps: WidgetBitmaps) {
    val context = LocalContext.current
    val colors = GlanceTheme.colors
    val ring = bitmaps.ring(
        progress = if (data.total == 0) 0f else data.done.toFloat() / data.total,
        color = colors.primary.getColor(context),
        track = colors.surfaceVariant.getColor(context),
        sizeDp = 104f,
        strokeDp = 10f,
    )
    Row(
        GlanceModifier
            .fillMaxSize()
            .background(colors.widgetBackground)
            .cornerRadius(28.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(GlanceModifier.size(104.dp).clickable(actionStartActivity(context.openAppIntent())), contentAlignment = Alignment.Center) {
            Image(ImageProvider(ring), contentDescription = LocalContext.current.getString(R.string.widget_done_today, data.done, data.total), modifier = GlanceModifier.size(104.dp))
            Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                Text(LocalContext.current.getString(R.string.widget_ratio, data.done, data.total), style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Medium, color = colors.onSurface))
                Text(LocalContext.current.getString(R.string.widget_today_lower), style = TextStyle(fontSize = 12.sp, color = colors.onSurfaceVariant))
            }
        }
        Spacer(GlanceModifier.width(16.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(
                LocalContext.current.getString(if (data.upNext.isEmpty()) R.string.widget_all_done_today else R.string.widget_up_next),
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant),
            )
            data.upNext.take(2).forEach { habit ->
                val hc = habitColors(habit.hue, bitmaps.dark)
                Spacer(GlanceModifier.height(8.dp))
                // Two sibling tap areas, never nested: the name opens the habit, the circle marks it
                // done. Nested clickables let the outer one win on some launchers.
                Row(
                    GlanceModifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(ColorProvider(hc.soft, hc.soft))
                        .cornerRadius(16.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Box(
                        GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .padding(start = 14.dp)
                            .clickable(actionStartActivity(context.openHabitIntent(habit.id))),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            habit.name,
                            maxLines = 1,
                            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = ColorProvider(hc.ink, hc.ink)),
                        )
                    }
                    Box(
                        GlanceModifier
                            .size(48.dp)
                            .clickable(actionRunCallback<MarkDoneAction>(actionParametersOf(MarkDoneAction.HabitIdKey to habit.id))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            ImageProvider(R.drawable.widget_circle),
                            contentDescription = LocalContext.current.getString(R.string.mark_done, habit.name),
                            colorFilter = ColorFilter.tint(ColorProvider(hc.solid, hc.solid)),
                            modifier = GlanceModifier.size(34.dp),
                        )
                    }
                }
            }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TodayWidget()
}
