package app.sprout.habits.ui.today

import app.sprout.habits.ui.datePattern
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
import app.sprout.habits.data.TrackType
import app.sprout.habits.domain.DayOutcome
import app.sprout.habits.domain.dayCredit
import app.sprout.habits.domain.firstDay
import app.sprout.habits.domain.isScheduled
import app.sprout.habits.domain.measure
import app.sprout.habits.domain.toShown
import app.sprout.habits.domain.outcomeOf
import app.sprout.habits.domain.score
import app.sprout.habits.domain.weekOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import app.sprout.habits.support.SupportPrompt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Immutable
data class WeekDayUi(
    val date: LocalDate,
    val letter: String,
    val dayOfMonth: Int,
    /** 0..1, or null for a future day. */
    val progress: Float?,
    val isSelected: Boolean,
)

@Immutable
data class HabitRowUi(
    val id: Long,
    val name: String,
    val icon: Int,
    val hue: Float,
    val outcome: DayOutcome,
    /** Fraction of the target reached, 0..1. */
    val progress: Float,
    val subtitle: String,
    val hasNote: Boolean,
)

@Immutable
data class TodayUiState(
    val loading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val isToday: Boolean = true,
    val dateLabel: String = "",
    val week: List<WeekDayUi> = emptyList(),
    val scorePercent: Int = 0,
    val doneCount: Int = 0,
    val partialCount: Int = 0,
    val openCount: Int = 0,
    val habits: List<HabitRowUi> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val repository: HabitRepository,
    private val settings: SettingsRepository,
    private val strings: Strings,
) : ViewModel() {
    private val today = MutableStateFlow(LocalDate.now())
    private val selected = MutableStateFlow(today.value)

    private val weekStart = settings.settings.map { it.weekStart }.distinctUntilChanged()

    val state: StateFlow<TodayUiState> =
        combine(today, selected, weekStart) { t, s, ws -> Triple(t, s, weekOf(s, ws)) }
            .flatMapLatest { (t, s, week) ->
                val from = week.first().toEpochDay()
                val to = week.last().toEpochDay()
                combine(
                    repository.observeHabits(),
                    repository.observeEntries(from, to),
                    repository.observeNotes(from, to),
                ) { habits, entries, notes -> buildState(t, s, week, habits, entries, notes) }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    private val _supportPrompt = MutableStateFlow(false)

    /** True while the "Enjoying Sprout?" dialog should show. */
    val supportPrompt: StateFlow<Boolean> = _supportPrompt

    /** Quick actions, the log sheet and Undo; shared with Habits and the habit detail. */
    val logger = DayLogger(repository, settings, viewModelScope, strings)
    private var promptCheckedThisSession = false

    /** Called when Today resumes; shows the support prompt at most once per app session. */
    fun maybeShowSupportPrompt() {
        if (promptCheckedThisSession || logger.skipped) return
        promptCheckedThisSession = true
        viewModelScope.launch {
            if (SupportPrompt.shouldShow(settings.settings.first(), System.currentTimeMillis())) _supportPrompt.value = true
        }
    }

    fun supportPromptClosed(dismissed: Boolean) {
        _supportPrompt.value = false
        viewModelScope.launch { settings.supportPromptShown(dismissed) }
    }

    fun markDone(habitId: Long) = logger.markDone(habitId, selected.value)

    fun markSkipped(habitId: Long) = logger.markSkipped(habitId, selected.value)

    /** Clears the selected day back to not logged (the opposite swipe on a done or skipped card). */
    fun resetDay(habitId: Long) = logger.reset(habitId, selected.value)

    /** The circle on the card: DONE becomes not logged; anything else becomes DONE. */
    fun toggleDone(habitId: Long) {
        val done = state.value.habits.firstOrNull { it.id == habitId }?.outcome == DayOutcome.DONE
        if (done) logger.reset(habitId, selected.value, R.string.log_marked_not_done) else markDone(habitId)
    }

    fun openLogSheet(habitId: Long) = logger.open(habitId, selected.value)

    fun select(date: LocalDate) {
        if (!date.isAfter(today.value)) selected.value = date
    }

    /** Called when the screen resumes, so the app rolls over to a new day after midnight. */
    fun refreshToday() {
        val now = LocalDate.now()
        if (now != today.value) {
            if (selected.value == today.value) selected.value = now
            today.value = now
        }
    }

    private fun buildState(
        today: LocalDate,
        selected: LocalDate,
        week: List<LocalDate>,
        habits: List<Habit>,
        entries: List<Entry>,
        notes: List<Note>,
    ): TodayUiState {
        val byKey = entries.associateBy { it.habitId to it.date }
        val noted = notes.mapTo(HashSet()) { it.habitId to it.date }
        val todayDay = today.toEpochDay()

        fun scheduledOn(date: LocalDate) = habits.filter { isScheduled(it.daysMask, date) && !createdAfter(it, date) }

        val weekUi = week.map { date ->
            val day = date.toEpochDay()
            val progress = if (date.isAfter(today)) {
                null
            } else {
                score(scheduledOn(date).map { dayCredit(byKey[it.id to day], it.target, day, todayDay) }).toFloat()
            }
            WeekDayUi(
                date = date,
                letter = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                dayOfMonth = date.dayOfMonth,
                progress = progress,
                isSelected = date == selected,
            )
        }

        val day = selected.toEpochDay()
        val rows = scheduledOn(selected).map { habit ->
            val entry = byKey[habit.id to day]
            val outcome = outcomeOf(entry, day, todayDay)
            val credit = dayCredit(entry, habit.target, day, todayDay) ?: 0.0
            val hasNote = (habit.id to day) in noted
            HabitRowUi(
                id = habit.id,
                name = habit.name,
                icon = HabitIcon.fromKey(habit.icon).drawable,
                hue = habit.colorHue.toFloat(),
                outcome = outcome,
                progress = credit.toFloat(),
                subtitle = subtitle(habit, entry, outcome).let { if (hasNote) strings(R.string.note_added, it) else it },
                hasNote = hasNote,
            )
        }

        return TodayUiState(
            loading = false,
            selectedDate = selected,
            isToday = selected == today,
            dateLabel = selected.format(DATE_FORMAT),
            week = weekUi,
            scorePercent = (score(rows.map { if (it.outcome == DayOutcome.SKIP) null else it.progress.toDouble() }) * 100).roundToInt(),
            doneCount = rows.count { it.outcome == DayOutcome.DONE },
            partialCount = rows.count { it.outcome == DayOutcome.PARTIAL },
            openCount = rows.count { it.outcome == DayOutcome.OPEN },
            habits = rows,
        )
    }

    private fun createdAfter(habit: Habit, date: LocalDate): Boolean = habit.firstDay() > date.toEpochDay()

    private fun subtitle(habit: Habit, entry: Entry?, outcome: DayOutcome): String = when (outcome) {
        DayOutcome.DONE -> if (habit.trackType == TrackType.CHECK) doneAt(entry) else amountOf(habit, entry?.amount ?: habit.target, strings)
        DayOutcome.PARTIAL -> amountOf(habit, entry?.amount ?: 0.0, strings)
        DayOutcome.SKIP -> strings(R.string.outcome_skipped)
        DayOutcome.OPEN -> goalOf(habit, strings)
    }

    /** "Done at 6:52 AM" when it was logged on that same day; plain "Done" otherwise. */
    private fun doneAt(entry: Entry?): String {
        val at = entry?.loggedAt?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()) }
        return if (at != null && at.toLocalDate().toEpochDay() == entry.date) strings(R.string.done_at, at.toLocalTime().format(TIME)) else strings(R.string.outcome_done)
    }

    companion object {
        // Built on each use so a change of language shows up.
        private val TIME get() = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        private val DATE_FORMAT get() = datePattern("EEEE, d MMMM")

        fun formatNumber(value: Double): String = app.sprout.habits.domain.formatNumber(value)

        /** "15 / 20 pages", "30 / 45 min", "1.5 / 2 h". */
        fun amountOf(habit: Habit, amount: Double, strings: Strings): String =
            strings(R.string.amount_slash_goal, formatNumber(habit.toShown(amount)), goalOf(habit, strings))

        /** "20 pages", "45 min", or the reminder time for a check habit. */
        fun goalOf(habit: Habit, strings: Strings): String = when (habit.trackType) {
            TrackType.AMOUNT, TrackType.DURATION -> habit.measure(habit.target)
            TrackType.CHECK -> habit.reminderMinutes?.let { strings(R.string.reminder_at, formatTime(it)) } ?: strings(R.string.once_a_day)
        }

        fun formatTime(minutes: Int): String =
            java.time.LocalTime.of(minutes / 60, minutes % 60).format(TIME)
    }
}
