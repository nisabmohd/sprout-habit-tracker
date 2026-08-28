package app.sprout.habits.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
        val theme = context.themeSettings()
        val bitmaps = WidgetBitmaps(context)
        provideContent { SproutGlanceTheme(theme) { TodayContent(data, bitmaps) } }
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
            .padding(16.dp)
            .clickable(actionStartActivity(context.openAppIntent())),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(GlanceModifier.size(104.dp), contentAlignment = Alignment.Center) {
            Image(ImageProvider(ring), contentDescription = "${data.done} of ${data.total} done today", modifier = GlanceModifier.size(104.dp))
            Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                Text("${data.done}/${data.total}", style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Medium, color = colors.onSurface))
                Text("today", style = TextStyle(fontSize = 12.sp, color = colors.onSurfaceVariant))
            }
        }
        Spacer(GlanceModifier.width(16.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(
                if (data.upNext.isEmpty()) "All done for today" else "Up next",
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant),
            )
            data.upNext.take(2).forEach { habit ->
                val hc = habitColors(habit.hue, bitmaps.dark)
                Spacer(GlanceModifier.height(8.dp))
                Row(
                    GlanceModifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(ColorProvider(hc.soft, hc.soft))
                        .cornerRadius(16.dp)
                        .padding(start = 14.dp, end = 6.dp)
                        .clickable(actionStartActivity(context.openHabitIntent(habit.id))),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Text(
                        habit.name,
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight(),
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = ColorProvider(hc.ink, hc.ink)),
                    )
                    Image(
                        ImageProvider(R.drawable.widget_circle),
                        contentDescription = "Mark ${habit.name} done",
                        colorFilter = ColorFilter.tint(ColorProvider(hc.solid, hc.solid)),
                        modifier = GlanceModifier
                            .size(36.dp)
                            .clickable(actionRunCallback<MarkDoneAction>(actionParametersOf(MarkDoneAction.HabitIdKey to habit.id))),
                    )
                }
            }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TodayWidget()
}
