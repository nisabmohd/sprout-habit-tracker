package app.sprout.habits.ui.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sprout.habits.data.Entry
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.Note
import app.sprout.habits.data.SettingsRepository
import app.sprout.habits.domain.bestStreak
import app.sprout.habits.domain.currentStreak
import app.sprout.habits.domain.dayCredit
import app.sprout.habits.domain.history
import app.sprout.habits.domain.outcomeOf
import app.sprout.habits.domain.score
import app.sprout.habits.ui.components.DayMark
import app.sprout.habits.ui.components.markFor
import app.sprout.habits.ui.habits.HabitsViewModel
import app.sprout.habits.ui.today.TodayViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class CalendarDayUi(val day: Int, val mark: DayMark, val isToday: Boolean)

@Immutable
data class NoteUi(val id: Long, val dateLabel: String, val status: EntryStatus?, val text: String)

@Immutable
data class HabitDetailUi(
    val habit: Habit,
    val icon: Int,
    val subtitle: String,
    val monthPercent: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val noteCount: Int,
    val monthLabel: String,
    val canGoForward: Boolean,
    val weekdayLabels: List<String>,
    /** Leading blanks before day 1, so the grid starts on the week-start day. */
    val leadingBlanks: Int,
    val days: List<CalendarDayUi>,
    val notes: List<NoteUi>,
)

class HabitDetailViewModel(
    private val repository: HabitRepository,
    settings: app.sprout.habits.data.SettingsRepository,
    private val habitId: Long,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())

    val state: StateFlow<HabitDetailUi?> = combine(
        repository.observeHabit(habitId),
        repository.observeAllEntries(habitId),
        repository.observeNotes(habitId),
        month,
        settings.settings,
    ) { habit, entries, notes, ym, s ->
        habit?.let { build(it, entries, notes, ym, s.weekStart) }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        if (month.value < YearMonth.now()) month.value = month.value.plusMonths(1)
    }

    fun setArchived(archived: Boolean) {
        viewModelScope.launch { repository.setArchived(habitId, archived) }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.getHabit(habitId)?.let { repository.deleteHabit(it) }
            onDone()
        }
    }

    private fun build(habit: Habit, entries: List<Entry>, notes: List<Note>, ym: YearMonth, weekStart: DayOfWeek): HabitDetailUi {
        val today = LocalDate.now()
        val todayDay = today.toEpochDay()
        val history = habit.history(entries.associateBy { it.date })
        val first = ym.atDay(1).toEpochDay()
        val last = ym.atEndOfMonth().toEpochDay()
        val days = (first..last).map { day ->
            val scheduled = history.isScheduled(day)
            val entry = history.entries[day]
            val credit = dayCredit(entry, habit.target, day, todayDay) ?: 0.0
            CalendarDayUi(
                day = LocalDate.ofEpochDay(day).dayOfMonth,
                mark = markFor(scheduled, outcomeOf(entry, day, todayDay), credit.toFloat(), day > todayDay),
                isToday = day == todayDay,
            )
        }
        val subtitle = buildList {
            add(HabitsViewModel.daysLabel(habit.daysMask))
            if (habit.trackType != app.sprout.habits.data.TrackType.CHECK) add(HabitsViewModel.goalLabel(habit))
            habit.reminderMinutes?.let { add("reminder at ${TodayViewModel.formatTime(it)}") }
        }.joinToString(" · ")
        return HabitDetailUi(
            habit = habit,
            icon = HabitIcon.fromKey(habit.icon).drawable,
            subtitle = subtitle,
            monthPercent = (history.score(first, last, todayDay) * 100).roundToInt(),
            currentStreak = history.currentStreak(todayDay),
            bestStreak = history.bestStreak(todayDay),
            noteCount = notes.size,
            monthLabel = ym.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            canGoForward = ym < YearMonth.now(),
            weekdayLabels = List(7) { weekStart.plus(it.toLong()).getDisplayName(TextStyle.NARROW, Locale.getDefault()) },
            leadingBlanks = (ym.atDay(1).dayOfWeek.value - weekStart.value + 7) % 7,
            days = days,
            notes = notes.map { n ->
                NoteUi(n.id, relativeDate(LocalDate.ofEpochDay(n.date), today), history.entries[n.date]?.status, n.text)
            },
        )
    }

    companion object {
        fun relativeDate(date: LocalDate, today: LocalDate): String = when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "EEE d MMM" else "d MMM yyyy"))
        }
    }
}
