package app.sprout.habits.notify

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.sprout.habits.MainActivity
import app.sprout.habits.R
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.data.Habit
import app.sprout.habits.domain.measure
import app.sprout.habits.domain.toShown
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.TrackType
import app.sprout.habits.ui.today.TodayViewModel.Companion.formatNumber
import app.sprout.habits.domain.isScheduled
import java.time.LocalDate

/** Simple notifications: a title and one line, no action buttons; a tap opens the habit. */
class ReminderNotifier(
    private val context: Context,
    private val repository: HabitRepository,
) {
    /** The habit's reminder, unless today is already done or skipped. */
    @SuppressLint("MissingPermission") // checked by canPost()
    suspend fun showReminder(habit: Habit) {
        if (habit.archived || !Notifications.canPost(context)) return
        val entry = repository.getEntry(habit.id, LocalDate.now().toEpochDay())
        if (entry?.status == EntryStatus.DONE || entry?.status == EntryStatus.SKIP) return
        val (title, text) = reminderText(habit, entry?.amount ?: 0.0)
        post(habit.id, Notifications.CHANNEL_REMINDERS, title, text, reminderId(habit.id))
    }

    /** For each habit that asks for a note: if it was skipped today and has no note yet, nudge. */
    suspend fun showNoteNudges(habits: List<Habit>) {
        val today = LocalDate.now()
        val day = today.toEpochDay()
        for (habit in habits) {
            if (!habit.askForNote || habit.archived || !isScheduled(habit.daysMask, today)) continue
            if (repository.getEntry(habit.id, day)?.status != EntryStatus.SKIP) continue
            if (repository.getNote(habit.id, day) != null) continue
            post(habit.id, Notifications.CHANNEL_NOTES, "${habit.name} · how did today go?", "Add a note about today.", noteReminderId(habit.id))
        }
    }

    @SuppressLint("MissingPermission")
    fun post(habitId: Long, channel: String, title: String, text: String, id: Int) {
        if (!Notifications.canPost(context)) return
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openHabit(habitId, id))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun openHabit(habitId: Long, requestCode: Int): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_OPEN_HABIT)
            .putExtra(MainActivity.EXTRA_HABIT_ID, habitId)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        fun reminderId(habitId: Long) = habitId.toInt() * 2
        fun noteReminderId(habitId: Long) = habitId.toInt() * 2 + 1

        /** Title and line for a reminder, given how much is already logged today. */
        fun reminderText(habit: Habit, amount: Double): Pair<String, String> {
            return when {
                habit.trackType == TrackType.CHECK -> habit.name to "Time to check it off."
                amount > 0 -> "${habit.name} · ${habit.measure((habit.target - amount).coerceAtLeast(0.0))} to go" to
                    "You're at ${formatNumber(habit.toShown(amount))} of ${habit.measure(habit.target)} today."
                else -> habit.name to "Goal today: ${habit.measure(habit.target)}."
            }
        }
    }
}
