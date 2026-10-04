package app.sprout.habits.widget

import app.sprout.habits.ui.theme.SproutType
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import app.sprout.habits.R
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.ui.theme.SproutTheme
import app.sprout.habits.ui.theme.habitColors
import kotlinx.coroutines.launch

/** Shown when a streak widget is added: pick which habit it follows. */
class StreakWidgetConfigActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(app.sprout.habits.AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Backing out without picking cancels adding the widget.
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            SproutTheme {
                val habits by produceState<List<Habit>?>(null) {
                    value = WidgetDataSource(container.repository, container.settings).habitsForPicker()
                }
                LazyColumn(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text(stringResource(R.string.pick_a_habit), style = SproutType.title, modifier = Modifier.padding(4.dp, 8.dp, 4.dp, 12.dp))
                    }
                    items(habits.orEmpty(), key = { it.id }) { habit -> HabitRow(habit) { pick(appWidgetId, habit.id) } }
                }
            }
        }
    }

    private fun pick(appWidgetId: Int, habitId: Long) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@StreakWidgetConfigActivity).getGlanceIdBy(appWidgetId)
            updateAppWidgetState(this@StreakWidgetConfigActivity, PreferencesGlanceStateDefinition, glanceId) {
                it.toMutablePreferences().apply { this[StreakWidget.HabitKey] = habitId }
            }
            StreakWidget().update(this@StreakWidgetConfigActivity, glanceId)
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }
}

@androidx.compose.runtime.Composable
private fun HabitRow(habit: Habit, onClick: () -> Unit) {
    val hc = habitColors(habit.colorHue.toFloat())
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).background(hc.soft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(HabitIcon.fromKey(habit.icon).drawable), contentDescription = null, tint = hc.ink, modifier = Modifier.size(20.dp))
        }
        Text(habit.name, style = SproutType.cardTitle, modifier = Modifier.padding(start = 14.dp))
    }
}
