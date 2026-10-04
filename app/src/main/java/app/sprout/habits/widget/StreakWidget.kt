package app.sprout.habits.widget

import app.sprout.habits.ui.theme.WidgetType
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.ColorFilter
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
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.TextAlign
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.sprout.habits.R
import app.sprout.habits.ui.theme.habitColors

/** One habit's current streak and its last 7 days. The habit is picked when the widget is added. */
class StreakWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val habitId = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[HabitKey]
        val data = WidgetDataSource(context.container.repository, context.container.settings).streak(habitId)
        val bitmaps = WidgetBitmaps(context)
        provideContent {
            val live by liveWidgetData(context, data) { it.streak(habitId) }
            SproutGlanceTheme { StreakContent(live, bitmaps) }
        }
    }

    companion object {
        val HabitKey = longPreferencesKey("habitId")
    }
}

@Composable
private fun StreakContent(data: StreakWidgetData?, bitmaps: WidgetBitmaps) {
    val context = LocalContext.current
    if (data == null) {
        Column(
            GlanceModifier.fillMaxSize().background(GlanceTheme.colors.widgetBackground).cornerRadius(28.dp).padding(16.dp)
                .clickable(actionStartActivity(context.openAppIntent())),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(LocalContext.current.getString(R.string.streak_empty), style = TextStyle(fontSize = WidgetType.supporting, color = GlanceTheme.colors.onSurfaceVariant))
        }
        return
    }
    val habit = data.habit
    val hc = habitColors(habit.hue, bitmaps.dark)
    val ink = ColorProvider(hc.ink, hc.ink)
    val size = LocalSize.current
    // Launchers give a 2x2 cell anywhere from about 100 to 200 dp; shrink instead of cutting off.
    val compact = size.height.value < 170f
    val tiny = size.height.value < 120f
    val pad = if (compact) 12.dp else 18.dp
    val marksWidth = (size.width.value - 2 * pad.value).coerceAtLeast(80f)
    val markSize = if (compact) 14f else 18f
    Column(
        GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(hc.soft, hc.soft))
            .cornerRadius(28.dp)
            .padding(pad)
            .clickable(actionStartActivity(context.openHabitIntent(habit.id))),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Image(ImageProvider(habit.icon), contentDescription = null, colorFilter = ColorFilter.tint(ink), modifier = GlanceModifier.size(if (compact) 18.dp else 24.dp))
            Spacer(GlanceModifier.width(8.dp))
            Text(
                habit.name,
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(fontSize = if (compact) WidgetType.caption else WidgetType.supporting, fontWeight = FontWeight.Bold, color = ink, textAlign = TextAlign.End),
            )
        }
        Spacer(GlanceModifier.defaultWeight())
        Text("${data.streak}", maxLines = 1, style = TextStyle(fontSize = if (compact) WidgetType.screenTitle else WidgetType.display, fontWeight = FontWeight.Medium, color = ink))
        Text(LocalContext.current.getString(R.string.day_streak), maxLines = 1, style = TextStyle(fontSize = if (compact) WidgetType.caption else WidgetType.supporting, fontWeight = FontWeight.Medium, color = ink))
        if (!tiny) {
            Spacer(GlanceModifier.height(if (compact) 6.dp else 10.dp))
            Image(
                ImageProvider(bitmaps.marks(data.last7, habit.hue, marksWidth, markSize)),
                contentDescription = LocalContext.current.getString(R.string.last_7_days),
                modifier = GlanceModifier.fillMaxWidth().height(markSize.dp),
            )
        }
    }
}

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StreakWidget()
}
