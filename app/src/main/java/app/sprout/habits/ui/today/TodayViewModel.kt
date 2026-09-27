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
import app.sprout.habits.domain.firstDay
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

/** What the press-and-hold sheet needs for one habit on the selected day. */
@Immutable
data class LogSheetUi(
    val habitId: Long,
    val day: Long,
    val name: String,
    val icon: Int,
    val hue: Float,
    val trackType: TrackType,
    val target: Double,
    val unit: String,
    /** "today", or the date when logging a past day. */
    val dayLabel: String,
    val status: EntryStatus,
    val amount: Double,
    val noteId: Long?,
    val noteText: String,
    /** True when the day already has an entry the sheet's Undo can clear. */
    val hasEntry: Boolean,
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

    /** The sheet's Undo pill: closes the sheet and clears that day back to not logged. */
    fun clearFromSheet(sheet: LogSheetUi) {
        _logSheet.value = null
        viewModelScope.launch {
            val previous = repository.getEntry(sheet.habitId, sheet.day) ?: return@launch
            repository.clearEntry(sheet.habitId, sheet.day)
            _changes.send(UndoableChange("${sheet.name} cleared", sheet.habitId, sheet.day, previous))
        }
    }

    /** Clears the selected day back to not logged (the opposite swipe on a done or skipped card). */
    fun resetDay(habitId: Long) = log(habitId, "undone") { _, _ -> null }

    /** The circle on the card: DONE becomes not logged; anything else becomes DONE. */
    fun toggleDone(habitId: Long) {
        val done = state.value.habits.firstOrNull { it.id == habitId }?.outcome == DayOutcome.DONE
        if (done) {
            log(habitId, "marked not done") { _, _ -> null }
        } else {
            markDone(habitId)
        }
    }

    private val _logSheet = MutableStateFlow<LogSheetUi?>(null)

    /** The habit whose press-and-hold sheet is open, or null. */
    val logSheet: StateFlow<LogSheetUi?> = _logSheet

    fun openLogSheet(habitId: Long) {
        val date = selected.value
        val day = date.toEpochDay()
        viewModelScope.launch {
            val habit = repository.getHabit(habitId) ?: return@launch
            val entry = repository.getEntry(habitId, day)
            val note = repository.getNote(habitId, day)
            _logSheet.value = LogSheetUi(
                habitId = habit.id,
                day = day,
                name = habit.name,
                icon = HabitIcon.fromKey(habit.icon).drawable,
                hue = habit.colorHue.toFloat(),
                trackType = habit.trackType,
                target = habit.target,
                unit = if (habit.trackType == TrackType.DURATION) "min" else habit.unit,
                dayLabel = if (date == today.value) "today" else date.format(SHORT_DATE),
                status = entry?.status ?: if (habit.trackType == TrackType.CHECK) EntryStatus.DONE else EntryStatus.PARTIAL,
                amount = entry?.amount ?: 0.0,
                noteId = note?.id,
                noteText = note?.text.orEmpty(),
                hasEntry = entry != null,
            )
        }
    }

    fun dismissLogSheet() {
        _logSheet.value = null
    }

    /** Saves the sheet. A partial amount that reaches the goal is saved as DONE. */
    fun saveLog(sheet: LogSheetUi, status: EntryStatus, amount: Double, noteText: String) {
        _logSheet.value = null
        viewModelScope.launch {
            val finalStatus = if (status == EntryStatus.PARTIAL && amount >= sheet.target) EntryStatus.DONE else status
            val finalAmount = when (finalStatus) {
                EntryStatus.DONE -> maxOf(amount, sheet.target)
                EntryStatus.PARTIAL -> amount
                EntryStatus.SKIP -> 0.0
            }
            val previous = repository.getEntry(sheet.habitId, sheet.day)
            val next = Entry(sheet.habitId, sheet.day, finalStatus, finalAmount)
            if (next != previous) {
                repository.setEntry(next)
                val message = when (finalStatus) {
                    EntryStatus.DONE -> "${sheet.name} marked done"
                    // "Read · 15 of 20 pages"
                    EntryStatus.PARTIAL -> "${sheet.name} · ${formatNumber(finalAmount)} of ${formatNumber(sheet.target)} ${sheet.unit}".trimEnd()
                    EntryStatus.SKIP -> "${sheet.name} skipped"
                }
                _changes.send(UndoableChange(message, sheet.habitId, sheet.day, previous))
            }
            val text = noteText.trim()
            when {
                text.isNotEmpty() && text != sheet.noteText ->
                    repository.saveNote(Note(id = sheet.noteId ?: 0, habitId = sheet.habitId, date = sheet.day, text = text))
                text.isEmpty() && sheet.noteId != null -> repository.deleteNote(sheet.noteId)
            }
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

    private fun createdAfter(habit: Habit, date: LocalDate): Boolean = habit.firstDay() > date.toEpochDay()

    private fun subtitle(habit: Habit, entry: Entry?, outcome: DayOutcome): String = when (outcome) {
        DayOutcome.DONE -> if (habit.trackType == TrackType.CHECK) "Done" else amountOf(habit, entry?.amount ?: habit.target)
        DayOutcome.PARTIAL -> amountOf(habit, entry?.amount ?: 0.0)
        DayOutcome.SKIP -> "Skipped"
        DayOutcome.OPEN -> goalOf(habit)
    }

    companion object {
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, d MMMM")
        private val SHORT_DATE = DateTimeFormatter.ofPattern("EEE d MMM")

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
