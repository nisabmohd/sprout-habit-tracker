package app.sprout.habits.ui.today

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
import app.sprout.habits.data.TrackType
import app.sprout.habits.domain.DayOutcome
import app.sprout.habits.domain.dayCredit
import app.sprout.habits.domain.isScheduled
import app.sprout.habits.domain.outcomeOf
import app.sprout.habits.domain.score
import app.sprout.habits.domain.weekOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
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

/** A change the user can undo from the snackbar. */
data class UndoableChange(
    val message: String,
    val habitId: Long,
    val day: Long,
    /** The entry before the change, or null if the day was not logged. */
    val previous: Entry?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val repository: HabitRepository,
    settings: SettingsRepository,
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

    private val _changes = Channel<UndoableChange>(Channel.BUFFERED)

    /** One event per logged change, for the Undo snackbar. */
    val changes = _changes.receiveAsFlow()

    fun markDone(habitId: Long) = log(habitId, "marked done") { habit, day ->
        Entry(habit.id, day, EntryStatus.DONE, habit.target)
    }

    fun markSkipped(habitId: Long) = log(habitId, "skipped") { habit, day ->
        Entry(habit.id, day, EntryStatus.SKIP)
    }

    /** The circle on the card: DONE becomes not logged; anything else becomes DONE. */
    fun toggleDone(habitId: Long) {
        val done = state.value.habits.firstOrNull { it.id == habitId }?.outcome == DayOutcome.DONE
        if (done) {
            log(habitId, "marked not done") { _, _ -> null }
        } else {
            markDone(habitId)
        }
    }

    fun undo(change: UndoableChange) {
        viewModelScope.launch {
            val previous = change.previous
            if (previous == null) repository.clearEntry(change.habitId, change.day) else repository.setEntry(previous)
        }
    }

    /** Writes a new entry for the selected day. [build] returns null to clear the day instead. */
    private fun log(habitId: Long, verb: String, build: (Habit, Long) -> Entry?) {
        val day = selected.value.toEpochDay()
        viewModelScope.launch {
            val habit = repository.getHabit(habitId) ?: return@launch
            val previous = repository.getEntry(habitId, day)
            val next = build(habit, day)
            if (next == previous) return@launch
            if (next == null) repository.clearEntry(habitId, day) else repository.setEntry(next)
            _changes.send(UndoableChange("${habit.name} $verb", habitId, day, previous))
        }
    }

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
                subtitle = subtitle(habit, entry, outcome).let { if (hasNote) "$it · note added" else it },
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

    private fun createdAfter(habit: Habit, date: LocalDate): Boolean {
        val created = java.time.Instant.ofEpochMilli(habit.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        return created.isAfter(date)
    }

    private fun subtitle(habit: Habit, entry: Entry?, outcome: DayOutcome): String = when (outcome) {
        DayOutcome.DONE -> if (habit.trackType == TrackType.CHECK) "Done" else amountOf(habit, entry?.amount ?: habit.target)
        DayOutcome.PARTIAL -> amountOf(habit, entry?.amount ?: 0.0)
        DayOutcome.SKIP -> "Skipped"
        DayOutcome.OPEN -> goalOf(habit)
    }

    companion object {
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, d MMMM")

        fun formatNumber(value: Double): String =
            if (value == value.toLong().toDouble()) value.toLong().toString() else "%.1f".format(value)

        /** "15 / 20 pages", "30 / 45 min". */
        fun amountOf(habit: Habit, amount: Double): String =
            "${formatNumber(amount)} / ${goalOf(habit)}"

        /** "20 pages", "45 min", or the reminder time for a check habit. */
        fun goalOf(habit: Habit): String = when (habit.trackType) {
            TrackType.AMOUNT -> "${formatNumber(habit.target)} ${habit.unit}".trim()
            TrackType.DURATION -> "${formatNumber(habit.target)} min"
            TrackType.CHECK -> habit.reminderMinutes?.let { "Reminder at ${formatTime(it)}" } ?: "Once a day"
        }

        fun formatTime(minutes: Int): String =
            java.time.LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofPattern("h:mm a"))
    }
}
