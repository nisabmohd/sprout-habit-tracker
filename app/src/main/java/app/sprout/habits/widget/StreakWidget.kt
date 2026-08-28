package app.sprout.habits.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.sprout.habits.ui.theme.habitColors

/** One habit's current streak and its last 7 days. The habit is picked when the widget is added. */
class StreakWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val habitId = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[HabitKey]
        val data = WidgetDataSource(context.container.repository, context.container.settings).streak(habitId)
        val theme = context.themeSettings()
        val bitmaps = WidgetBitmaps(context)
        provideContent { SproutGlanceTheme(theme) { StreakContent(data, bitmaps) } }
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
            Text("Add a habit to see its streak.", style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant))
        }
        return
    }
    val habit = data.habit
    val hc = habitColors(habit.hue, bitmaps.dark)
    val ink = ColorProvider(hc.ink, hc.ink)
    val width = (LocalSize.current.width.value - 36).coerceAtLeast(100f)
    Column(
        GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(hc.soft, hc.soft))
            .cornerRadius(28.dp)
            .padding(18.dp)
            .clickable(actionStartActivity(context.openHabitIntent(habit.id))),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Image(ImageProvider(habit.icon), contentDescription = null, colorFilter = ColorFilter.tint(ink), modifier = GlanceModifier.size(24.dp))
            Spacer(GlanceModifier.defaultWeight())
            Text(habit.name, maxLines = 1, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ink))
        }
        Spacer(GlanceModifier.defaultWeight())
        Text("${data.streak}", style = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Medium, color = ink))
        Text(if (data.streak == 1) "day streak" else "day streak", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ink))
        Spacer(GlanceModifier.height(10.dp))
        Image(
            ImageProvider(bitmaps.marks(data.last7, habit.hue, width, 18f)),
            contentDescription = "Last 7 days",
            modifier = GlanceModifier.fillMaxWidth().height(18.dp),
        )
    }
}

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StreakWidget()
}
