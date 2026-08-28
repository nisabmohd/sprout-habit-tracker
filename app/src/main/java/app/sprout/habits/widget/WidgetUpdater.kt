package app.sprout.habits.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.SettingsRepository
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/** Refreshes every widget after any change to habits, entries, notes or settings. */
@OptIn(FlowPreview::class)
class WidgetUpdater(
    private val context: Context,
    private val repository: HabitRepository,
    private val settings: SettingsRepository,
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            // A 7-day look-back covers the week and streak widgets; older edits are rare and the
            // periodic update picks them up.
            val today = LocalDate.now().toEpochDay()
            combine(
                repository.observeAllHabits(),
                repository.observeEntries(today - 7, today + 7),
                settings.settings,
            ) { _, _, _ -> Unit }
                .debounce(300)
                .collect { updateAll() }
        }
    }

    suspend fun updateAll() {
        val manager = GlanceAppWidgetManager(context)
        if (manager.getGlanceIds(WeekWidget::class.java).isNotEmpty()) WeekWidget().updateAll(context)
        if (manager.getGlanceIds(TodayWidget::class.java).isNotEmpty()) TodayWidget().updateAll(context)
        if (manager.getGlanceIds(StripWidget::class.java).isNotEmpty()) StripWidget().updateAll(context)
        if (manager.getGlanceIds(StreakWidget::class.java).isNotEmpty()) StreakWidget().updateAll(context)
    }
}
