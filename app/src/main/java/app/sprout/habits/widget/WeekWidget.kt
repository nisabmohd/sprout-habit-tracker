package app.sprout.habits.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import app.sprout.habits.ui.theme.habitColors

/** "This week": every habit with its 7 day marks and the week's score. */
class WeekWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetDataSource(context.container.repository, context.container.settings).week()
        val theme = context.themeSettings()
        val bitmaps = WidgetBitmaps(context)
        provideContent {
            SproutGlanceTheme(theme) { WeekContent(context, data, bitmaps) }
        }
    }
}

@Composable
private fun WeekContent(context: Context, data: WeekWidgetData, bitmaps: WidgetBitmaps) {
    val width = LocalSize.current.width.value
    val marksWidth = (width - 32 - 130).coerceAtLeast(140f)
    Column(
        GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(28.dp)
            .padding(16.dp)
            .clickable(actionStartActivity(context.openAppIntent())),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Column(GlanceModifier.defaultWeight()) {
                Text("This week", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium, color = GlanceTheme.colors.onSurface))
                Text(data.range, style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant))
            }
            Text("${data.percent}%", style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Medium, color = GlanceTheme.colors.primary))
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            Spacer(GlanceModifier.defaultWeight())
            Row(GlanceModifier.width(marksWidth.dp)) {
                data.dayLetters.forEachIndexed { i, letter ->
                    Text(
                        letter,
                        modifier = GlanceModifier.defaultWeight(),
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (i == data.todayIndex) GlanceTheme.colors.onSurface else GlanceTheme.colors.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
        if (data.habits.isEmpty()) {
            Text("No habits to show yet.", modifier = GlanceModifier.padding(top = 12.dp), style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant))
        }
        LazyColumn(GlanceModifier.fillMaxWidth()) {
            items(data.habits, itemId = { it.id }) { habit ->
                val hc = habitColors(habit.hue, bitmaps.dark)
                Row(
                    GlanceModifier.fillMaxWidth().padding(vertical = 5.dp).clickable(actionStartActivity(context.openHabitIntent(habit.id))),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Box(
                        GlanceModifier.size(28.dp).background(ColorProvider(hc.soft, hc.soft)).cornerRadius(9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(ImageProvider(habit.icon), contentDescription = null, modifier = GlanceModifier.size(16.dp), colorFilter = androidx.glance.ColorFilter.tint(ColorProvider(hc.ink, hc.ink)))
                    }
                    Text(
                        habit.name,
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight().padding(start = 10.dp),
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = GlanceTheme.colors.onSurface),
                    )
                    Image(
                        ImageProvider(bitmaps.marks(habit.marks, habit.hue, marksWidth, 20f)),
                        contentDescription = "${habit.name} this week",
                        modifier = GlanceModifier.width(marksWidth.dp).height(20.dp),
                    )
                }
            }
        }
    }
}

class WeekWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = WeekWidget()
}
