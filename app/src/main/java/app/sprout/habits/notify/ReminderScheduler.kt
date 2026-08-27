package app.sprout.habits.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.domain.firstDay
import app.sprout.habits.domain.nextReminderTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * One alarm per habit with a reminder. When the user allows exact alarms ("Alarms & reminders")
 * it fires on time, even in Doze; otherwise it uses a 10-minute window, the smallest Android
 * allows without that permission.
 */
class ReminderScheduler(
    private val context: Context,
    private val repository: HabitRepository,
) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    /** What decides a habit's alarm; a change to any of these reschedules it. */
    private data class Key(
        val id: Long,
        val minutes: Int?,
        val daysMask: Int,
        val archived: Boolean,
        val firstDay: Long,
        val askForNote: Boolean,
    )

    /**
     * Keeps alarms in step with the database while the app runs: any add, edit, archive,
     * delete or import reschedules the affected habits and cancels alarms of removed ones.
     */
    fun start(scope: CoroutineScope) {
        scope.launch {
            var previous = emptyMap<Long, Key>()
            repository.observeAllHabits()
                .map { habits -> habits.associate { it.id to it.key() } }
                .distinctUntilChanged()
                .collect { current ->
                    (previous.keys - current.keys).forEach(::cancel)
                    current.values.filter { previous[it.id] != it }.forEach(::apply)
                    scheduleNoteNudge(current.values.any { it.askForNote && !it.archived })
                    previous = current
                }
        }
    }

    /** Reschedules every habit, e.g. after a reboot or a clock or time-zone change. */
    suspend fun rescheduleAll() {
        val habits = repository.observeAllHabits().first()
        habits.forEach { schedule(it) }
        scheduleNoteNudge(habits.any { it.askForNote && !it.archived })
    }

    fun schedule(habit: Habit) = apply(habit.key())

    private fun Habit.key() = Key(id, reminderMinutes, daysMask, archived, firstDay(), askForNote)

    /** The evening check for skipped habits that ask for a note; one alarm for all of them. */
    fun scheduleNoteNudge(enabled: Boolean) {
        val intent = notePendingIntent()
        if (!enabled) {
            alarms.cancel(intent)
            return
        }
        val at = nextReminderTime(System.currentTimeMillis(), ZoneId.systemDefault(), NOTE_NUDGE_MINUTES, 0b111_1111, 0L) ?: return
        if (canScheduleExact()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        } else {
            alarms.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW_MILLIS, intent)
        }
    }

    private fun notePendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        NOTE_NUDGE_REQUEST_CODE,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_NOTE_NUDGE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun apply(key: Key) {
        val minutes = key.minutes
        if (minutes == null || key.archived) {
            cancel(key.id)
            return
        }
        val at = nextReminderTime(System.currentTimeMillis(), ZoneId.systemDefault(), minutes, key.daysMask, key.firstDay)
        when {
            at == null -> cancel(key.id)
            canScheduleExact() -> alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent(key.id))
            else -> alarms.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW_MILLIS, pendingIntent(key.id))
        }
    }

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    fun cancel(habitId: Long) = alarms.cancel(pendingIntent(habitId))

    private fun pendingIntent(habitId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        habitId.toInt(),
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMINDER).putExtra(EXTRA_HABIT_ID, habitId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val ACTION_REMINDER = "app.sprout.habits.action.REMINDER"
        const val ACTION_NOTE_NUDGE = "app.sprout.habits.action.NOTE_NUDGE"
        const val EXTRA_HABIT_ID = "habitId"

        /** 9:00 PM, as in the reminder design. */
        const val NOTE_NUDGE_MINUTES = 21 * 60

        /** Habit alarms use the habit id as request code; ids start at 1, so -1 is free. */
        private const val NOTE_NUDGE_REQUEST_CODE = -1
        private const val WINDOW_MILLIS = 10 * 60 * 1000L
    }
}
