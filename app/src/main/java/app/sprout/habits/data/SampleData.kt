package app.sprout.habits.data

import app.sprout.habits.BuildConfig
import app.sprout.habits.R
import app.sprout.habits.Strings
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random
import kotlinx.coroutines.flow.first

/**
 * The sample habits a fresh install starts with: the habits from the designs, two weeks of
 * history and notes on several days, so every screen has something to show. Names and notes come
 * from strings.xml, in the app language. Today and More offer to remove them in one tap.
 */
object SampleData {
    /**
     * Adds the sample data on the very first launch only (no first-open time yet and no habits),
     * so updates and restored backups never get it.
     */
    suspend fun seedOnFirstLaunch(repository: HabitRepository, settings: SettingsRepository, strings: Strings) {
        if (settings.settings.first().firstOpenAt != null) return
        if (repository.observeAllHabits().first().isNotEmpty()) return
        val ids = seed(repository, strings)
        settings.setSampleHabitIds(ids.toSet())
    }

    /** Deletes the sample habits (with their entries and notes); the user's own habits stay. */
    suspend fun remove(repository: HabitRepository, settings: SettingsRepository) {
        val ids = settings.settings.first().sampleHabitIds
        ids.forEach { id -> repository.getHabit(id)?.let { repository.deleteHabit(it) } }
        settings.setSampleHabitIds(emptySet())
    }

    private suspend fun seed(repository: HabitRepository, strings: Strings): List<Long> {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val createdAt = today.minusDays(60).atStartOfDay(zone).toInstant().toEpochMilli()
        // Reminders only in debug builds: real users shouldn't get notifications for sample habits.
        val remind = BuildConfig.DEBUG
        val habits = listOf(
            Habit(name = strings(R.string.sample_wake), icon = "alarm", colorHue = 38, reminderMinutes = if (remind) 7 * 60 else null),
            Habit(name = strings(R.string.sample_read), icon = "menu_book", colorHue = 215, trackType = TrackType.AMOUNT, target = 20.0, unit = strings(R.string.sample_unit_pages)),
            Habit(name = strings(R.string.sample_water), icon = "water_drop", colorHue = 192, trackType = TrackType.AMOUNT, target = 8.0, unit = strings(R.string.sample_unit_glasses)),
            Habit(name = strings(R.string.sample_calm), icon = "self_improvement", colorHue = 275, askForNote = true),
            Habit(name = strings(R.string.sample_workout), icon = "fitness_center", colorHue = 12, trackType = TrackType.AMOUNT, target = 45.0, unit = "min", step = 5.0),
            Habit(name = strings(R.string.sample_vitamins), icon = "medication", colorHue = 95),
            Habit(name = strings(R.string.sample_walk), icon = "directions_walk", colorHue = 150, daysMask = 0b0011111),
            Habit(name = strings(R.string.sample_journal), icon = "edit_note", colorHue = 330, reminderMinutes = if (remind) 21 * 60 + 30 else null),
        ).mapIndexed { i, h -> h.copy(createdAt = createdAt, sortOrder = i) }
        val ids = habits.map { repository.saveHabit(it) }
        val (wake, read, water, calm, workout) = ids
        val journal = ids[7]
        val walk = ids[6]

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

        fun at(back: Long, hour: Int, minute: Int) = today.minusDays(back).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        suspend fun day(back: Long, habit: Long, status: EntryStatus, amount: Double, note: Int? = null, time: Long = at(back, 21, 10)) {
            val d = today.minusDays(back).toEpochDay()
            repository.setEntry(Entry(habit, d, status, amount, loggedAt = time))
            // saveNote stamps the current time; notes from earlier days keep a time from that day.
            note?.let { repository.restoreNote(Note(habitId = habit, date = d, text = strings(it), updatedAt = time)) }
        }

        // Today: one done (at 6:52, so the card reads "Done at 6:52 AM"), two partial, one skipped with a note.
        day(0, wake, EntryStatus.DONE, 1.0, time = at(0, 6, 52))
        day(0, read, EntryStatus.PARTIAL, 15.0, time = at(0, 8, 5))
        day(0, water, EntryStatus.PARTIAL, 5.0, time = at(0, 13, 30))
        day(0, calm, EntryStatus.SKIP, 0.0, note = R.string.sample_note_calm_today, time = at(0, 9, 12))
        // Earlier notes, so the Journal has a few days to scroll through.
        day(1, read, EntryStatus.PARTIAL, 8.0, note = R.string.sample_note_read, time = at(1, 23, 40))
        day(1, workout, EntryStatus.DONE, 45.0, note = R.string.sample_note_workout, time = at(1, 19, 5))
        day(2, journal, EntryStatus.DONE, 1.0, note = R.string.sample_note_journal, time = at(2, 21, 45))
        day(3, calm, EntryStatus.SKIP, 0.0, note = R.string.sample_note_calm, time = at(3, 22, 5))
        // Walk outside is weekdays only, so its note goes on the most recent weekday at least 4 days back.
        val walkBack = (4L..7L).first { today.minusDays(it).dayOfWeek.value <= 5 }
        day(walkBack, walk, EntryStatus.DONE, 1.0, note = R.string.sample_note_walk, time = at(walkBack, 18, 20))
        day(6, water, EntryStatus.PARTIAL, 6.0, note = R.string.sample_note_water, time = at(6, 20, 15))
        return ids
    }
}
