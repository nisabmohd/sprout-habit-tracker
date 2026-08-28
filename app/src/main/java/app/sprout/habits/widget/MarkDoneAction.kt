package app.sprout.habits.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import app.sprout.habits.data.Entry
import app.sprout.habits.data.EntryStatus
import java.time.LocalDate

/** The circle in the Today widget: marks the habit done for today. */
class MarkDoneAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val habitId = parameters[HabitIdKey] ?: return
        val repository = context.container.repository
        val habit = repository.getHabit(habitId) ?: return
        repository.setEntry(Entry(habit.id, LocalDate.now().toEpochDay(), EntryStatus.DONE, habit.target))
        // Refresh every widget now: this process may have just started for the tap, so the
        // app-wide updater can't be relied on to see the change as new.
        context.container.widgets.updateAll()
    }

    companion object {
        val HabitIdKey = ActionParameters.Key<Long>("habitId")
    }
}
