package app.sprout.habits

import app.sprout.habits.data.Entry
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.Note
import app.sprout.habits.data.TrackType
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random
import kotlinx.coroutines.flow.first

/** Debug builds only: fills an empty database with the habits from the designs and two weeks of history. */
object DevData {
    suspend fun seedIfEmpty(repository: HabitRepository) {
        if (repository.observeHabits().first().isNotEmpty()) return
        val today = LocalDate.now()
        val createdAt = today.minusDays(60).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val habits = listOf(
            Habit(name = "Wake up at 7", icon = "sun", colorHue = 38, reminderMinutes = 7 * 60),
            Habit(name = "Read", icon = "book", colorHue = 215, trackType = TrackType.AMOUNT, target = 20.0, unit = "pages"),
            Habit(name = "Drink water", icon = "drop", colorHue = 192, trackType = TrackType.AMOUNT, target = 8.0, unit = "glasses"),
            Habit(name = "Stay calm", icon = "calm", colorHue = 275, askForNote = true),
            Habit(name = "Workout", icon = "dumbbell", colorHue = 12, trackType = TrackType.DURATION, target = 45.0),
            Habit(name = "Vitamins", icon = "pill", colorHue = 95),
            Habit(name = "Walk outside", icon = "leaf", colorHue = 150, daysMask = 0b0011111),
            Habit(name = "Evening journal", icon = "pen", colorHue = 330, reminderMinutes = 21 * 60 + 30),
        ).map { it.copy(createdAt = createdAt) }
        val ids = habits.map { repository.saveHabit(it) }
        val random = Random(7)
        for (back in 1..14L) {
            val day = today.minusDays(back).toEpochDay()
            habits.forEachIndexed { i, h ->
                val roll = random.nextInt(10)
                val entry = when {
                    roll < 6 -> Entry(ids[i], day, EntryStatus.DONE, h.target)
                    roll < 8 && h.trackType != TrackType.CHECK -> Entry(ids[i], day, EntryStatus.PARTIAL, h.target / 2)
                    roll < 9 -> Entry(ids[i], day, EntryStatus.SKIP)
                    else -> null
                }
                entry?.let { repository.setEntry(it) }
            }
        }
        val d = today.toEpochDay()
        repository.setEntry(Entry(ids[0], d, EntryStatus.DONE, 1.0))
        repository.setEntry(Entry(ids[1], d, EntryStatus.PARTIAL, 15.0))
        repository.setEntry(Entry(ids[2], d, EntryStatus.PARTIAL, 5.0))
        repository.setEntry(Entry(ids[3], d, EntryStatus.SKIP))
        repository.saveNote(Note(habitId = ids[3], date = d, text = "Rough morning, but I noticed it sooner than usual."))
    }
}
