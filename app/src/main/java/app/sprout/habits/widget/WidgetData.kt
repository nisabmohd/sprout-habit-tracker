package app.sprout.habits.widget

import app.sprout.habits.data.Entry
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.SettingsRepository
import app.sprout.habits.domain.DayOutcome
import app.sprout.habits.domain.currentStreak
import app.sprout.habits.domain.dayCredit
import app.sprout.habits.domain.history
import app.sprout.habits.domain.outcomeOf
import app.sprout.habits.domain.score
import app.sprout.habits.domain.weekOf
import app.sprout.habits.ui.components.DayMark
import app.sprout.habits.ui.components.markFor
import app.sprout.habits.ui.today.TodayViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first

/** One habit's row in a widget. */
data class WidgetHabit(
    val id: Long,
    val name: String,
    val icon: Int,
    val hue: Float,
    val marks: List<DayMark>,
    val todayOutcome: DayOutcome?,
    val todayProgress: Float,
    val reminderMinutes: Int?,
)

data class WeekWidgetData(
    val range: String,
    val percent: Int,
    val dayLetters: List<String>,
    val todayIndex: Int,
    val habits: List<WidgetHabit>,
)

data class TodayWidgetData(
    val done: Int,
    val total: Int,
    /** Scheduled today and not finished (not logged or partly done), soonest reminder first. */
    val upNext: List<WidgetHabit>,
    /** All of today's habits, for the progress strip. */
    val today: List<WidgetHabit>,
    val nextLabel: String?,
)

data class StreakWidgetData(val habit: WidgetHabit, val streak: Int, val last7: List<DayMark>)

/** Reads what the widgets show, straight from the repository (widgets have no ViewModels). */
class WidgetDataSource(private val repository: HabitRepository, private val settings: SettingsRepository) {

    suspend fun week(): WeekWidgetData {
        val today = LocalDate.now()
        val days = weekOf(today, settings.settings.first().weekStart)
        val (habits, entries) = load(days.first(), days.last())
        val todayDay = today.toEpochDay()
        val credits = mutableListOf<Double?>()
        val rows = habits.map { habit ->
            val h = habit.history(entries[habit.id].orEmpty())
            days.forEach { d ->
                val day = d.toEpochDay()
                if (h.isScheduled(day) && day <= todayDay) credits += dayCredit(h.entries[day], habit.target, day, todayDay)
            }
            row(habit, h.entries, days, todayDay)
        }
        return WeekWidgetData(
            range = "${days.first().dayOfMonth} – ${days.last().format(DateTimeFormatter.ofPattern("d MMM"))}",
            percent = (score(credits) * 100).roundToInt(),
            dayLetters = days.map { it.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()) },
            todayIndex = days.indexOf(today),
            habits = rows,
        )
    }

    suspend fun today(): TodayWidgetData {
        val today = LocalDate.now()
        val (habits, entries) = load(today, today)
        val todayDay = today.toEpochDay()
        val rows = habits
            .map { row(it, entries[it.id].orEmpty(), listOf(today), todayDay) }
            .filter { it.todayOutcome != null }
        // Still to do: not logged yet, or only partly done (the circle can finish it).
        val open = rows.filter { it.todayOutcome == DayOutcome.OPEN || it.todayOutcome == DayOutcome.PARTIAL }
            .sortedWith(compareBy(nullsLast()) { it.reminderMinutes })
        val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }
        val next = open.firstOrNull { (it.reminderMinutes ?: -1) >= nowMinutes } ?: open.firstOrNull()
        return TodayWidgetData(
            done = rows.count { it.todayOutcome == DayOutcome.DONE },
            total = rows.size,
            upNext = open,
            today = rows,
            nextLabel = next?.let { n -> n.reminderMinutes?.let { "${n.name}, ${TodayViewModel.formatTime(it)}" } ?: n.name },
        )
    }

    /**
     * The chosen habit's streak. When none is chosen yet, or it was deleted or archived, follows
     * the habit with the longest current streak. Null only when there are no habits.
     */
    suspend fun streak(habitId: Long?): StreakWidgetData? {
        val chosen = habitId?.let { repository.getHabit(it) }?.takeIf { !it.archived }
        if (chosen != null) return streakOf(chosen)
        return repository.observeHabits().first()
            .map { streakOf(it) }
            .maxByOrNull { it.streak }
    }

    private suspend fun streakOf(habit: Habit): StreakWidgetData {
        val habitId = habit.id
        val today = LocalDate.now()
        val todayDay = today.toEpochDay()
        val entries = repository.observeAllEntries(habitId).first().associateBy { it.date }
        val last7 = (6 downTo 0).map { today.minusDays(it.toLong()) }
        val row = row(habit, entries, last7, todayDay)
        return StreakWidgetData(row, habit.history(entries).currentStreak(todayDay), row.marks)
    }

    suspend fun habitsForPicker(): List<Habit> = repository.observeHabits().first()

    private suspend fun load(from: LocalDate, to: LocalDate): Pair<List<Habit>, Map<Long, Map<Long, Entry>>> {
        val habits = repository.observeHabits().first().filter { it.showOnWidget }
        val entries = repository.observeEntries(from.toEpochDay(), to.toEpochDay()).first()
            .groupBy { it.habitId }.mapValues { (_, l) -> l.associateBy { it.date } }
        return habits to entries
    }

    private fun row(habit: Habit, entries: Map<Long, Entry>, days: List<LocalDate>, todayDay: Long): WidgetHabit {
        val h = habit.history(entries)
        val marks = days.map { d ->
            val day = d.toEpochDay()
            val credit = dayCredit(entries[day], habit.target, day, todayDay) ?: 0.0
            markFor(h.isScheduled(day), outcomeOf(entries[day], day, todayDay), credit.toFloat(), day > todayDay)
        }
        val scheduledToday = h.isScheduled(todayDay)
        return WidgetHabit(
            id = habit.id,
            name = habit.name,
            icon = HabitIcon.fromKey(habit.icon).drawable,
            hue = habit.colorHue.toFloat(),
            marks = marks,
            todayOutcome = if (scheduledToday) outcomeOf(entries[todayDay], todayDay, todayDay) else null,
            todayProgress = (dayCredit(entries[todayDay], habit.target, todayDay, todayDay) ?: 0.0).toFloat(),
            reminderMinutes = habit.reminderMinutes,
        )
    }
}
