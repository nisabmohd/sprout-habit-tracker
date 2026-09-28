package app.sprout.habits.ui.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sprout.habits.R
import app.sprout.habits.Strings
import app.sprout.habits.data.Entry
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
import app.sprout.habits.ui.components.DayMark
import app.sprout.habits.ui.components.NoteCardUi
import app.sprout.habits.ui.components.markFor
import app.sprout.habits.ui.habits.HabitsViewModel
import app.sprout.habits.ui.today.TodayViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class CalendarDayUi(val date: LocalDate, val mark: DayMark, val isToday: Boolean) {
    val day: Int get() = date.dayOfMonth
}

/** A note on the habit's page, with "Today" / "Yesterday" / "Thu 24 Sep" for its day. */
@Immutable
data class DetailNoteUi(val card: NoteCardUi, val dateLabel: String)

@Immutable
data class HabitDetailUi(
    val habit: Habit,
    val icon: Int,
    val subtitle: String,
    /** Done or Partial days in the month shown. */
    val monthDone: Int,
    /** "this month", or "August" when looking at another month. */
    val monthDoneLabel: String,
    val currentStreak: Int,
    val bestStreak: Int,
    val noteCount: Int,
    val monthLabel: String,
    val canGoForward: Boolean,
    val weekdayLabels: List<String>,
    /** Leading blanks before day 1, so the grid starts on the week-start day. */
    val leadingBlanks: Int,
    val days: List<CalendarDayUi>,
    val notes: List<DetailNoteUi>,
)

class HabitDetailViewModel(
    private val repository: HabitRepository,
    settings: app.sprout.habits.data.SettingsRepository,
    private val strings: Strings,
    private val habitId: Long,
) : ViewModel() {
    /** Tapping a past day in the calendar opens the log sheet for that day. */
    val logger = app.sprout.habits.ui.today.DayLogger(repository, settings, viewModelScope, strings)

    fun editDay(date: LocalDate) = logger.open(habitId, date)

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
                date = LocalDate.ofEpochDay(day),
                mark = markFor(scheduled, outcomeOf(entry, day, todayDay), credit.toFloat(), day > todayDay),
                isToday = day == todayDay,
            )
        }
        val subtitle = buildList {
            add(HabitsViewModel.daysLabel(habit.daysMask, strings))
            if (habit.trackType != app.sprout.habits.data.TrackType.CHECK) add(HabitsViewModel.goalLabel(habit, strings))
            habit.reminderMinutes?.let { add(strings(R.string.reminder_at_lower, TodayViewModel.formatTime(it))) }
        }.joinToString(" · ")
        return HabitDetailUi(
            habit = habit,
            icon = HabitIcon.fromKey(habit.icon).drawable,
            subtitle = subtitle,
            monthDone = (first..minOf(last, todayDay)).count { history.isScheduled(it) && history.isKept(it, todayDay) },
            monthDoneLabel = if (ym == YearMonth.now()) strings(R.string.this_month) else ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
            currentStreak = history.currentStreak(todayDay),
            bestStreak = history.bestStreak(todayDay),
            noteCount = notes.size,
            monthLabel = ym.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            canGoForward = ym < YearMonth.now(),
            weekdayLabels = List(7) { weekStart.plus(it.toLong()).getDisplayName(TextStyle.NARROW, Locale.getDefault()) },
            leadingBlanks = (ym.atDay(1).dayOfWeek.value - weekStart.value + 7) % 7,
            days = days,
            notes = notes.map { n ->
                DetailNoteUi(NoteCardUi.of(n, habit, history.entries[n.date], today, strings), relativeDate(LocalDate.ofEpochDay(n.date), today, strings))
            },
        )
    }

    companion object {
        fun relativeDate(date: LocalDate, today: LocalDate, strings: Strings): String = when (date) {
            today -> strings(R.string.today)
            today.minusDays(1) -> strings(R.string.yesterday)
            else -> date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "EEE d MMM" else "d MMM yyyy"))
        }
    }
}
