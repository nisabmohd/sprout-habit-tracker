package app.sprout.habits

import android.content.Context
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.SettingsRepository
import app.sprout.habits.notify.ReminderNotifier
import app.sprout.habits.notify.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import app.sprout.habits.data.SproutDatabase

/** Holds the app's long-lived objects. The only dependency "graph" in the app. */
class AppContainer(private val context: Context) {
    private val database by lazy { SproutDatabase.create(context) }
    val repository by lazy { HabitRepository(database) }
    val settings by lazy { SettingsRepository(context) }
    val reminders by lazy { ReminderScheduler(context, repository) }
    val reminderNotifier by lazy { ReminderNotifier(context, repository) }

    /** Lives as long as the process, for work that isn't tied to a screen. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
