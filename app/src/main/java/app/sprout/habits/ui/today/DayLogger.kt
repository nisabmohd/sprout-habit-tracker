package app.sprout.habits.ui.today

import app.sprout.habits.ui.datePattern
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import app.sprout.habits.R
import app.sprout.habits.Strings
import app.sprout.habits.data.Entry
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.Note
import app.sprout.habits.data.SettingsRepository
import app.sprout.habits.data.TrackType
import app.sprout.habits.domain.displayUnit
import app.sprout.habits.domain.measure
import app.sprout.habits.domain.step
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** What the log sheet needs for one habit on one day. */
@Immutable
data class LogSheetUi(
    val habitId: Long,
    val day: Long,
    val name: String,
    val icon: Int,
    val hue: Float,
    val trackType: TrackType,
    /** Goal and amounts in the unit shown ("h" habits in hours); saving converts back. */
    val target: Double,
    val unit: String,
    /** Stepper step in the shown unit: 1, 5 min or 0.25 h. */
    val step: Double,
    val isToday: Boolean,
    /** "Today", or the date when logging a past day ("Friday, 25 Sep"). */
    val dayLabel: String,
    /** "Goal 20 pages · today", or "Friday, 25 Sep · goal 20 pages" for a past day. */
    val subtitle: String,
    val status: EntryStatus,
    val amount: Double,
    val noteId: Long?,
    val noteText: String,
    /** True when the day already has an entry the sheet's Undo can clear. */
    val hasEntry: Boolean,
    /** Put the cursor in the note field, when the sheet opens right after a skip. */
    val focusNote: Boolean = false,
)

/** A change the user can undo from the snackbar. */
data class UndoableChange(
    val message: String,
    val habitId: Long,
    val day: Long,
    /** The entry before the change, or null if the day was not logged. */
    val previous: Entry?,
)

/**
 * Logs outcomes for any habit on any day up to today, for Today, Habits → Week and the habit
 * detail calendar alike: quick actions, the log sheet, and Undo. Every change is sent to
 * [changes] for the Undo snackbar.
 */
class DayLogger(
    private val repository: HabitRepository,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
    private val strings: Strings,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val _sheet = MutableStateFlow<LogSheetUi?>(null)

    /** The open log sheet, or null. */
    val sheet: StateFlow<LogSheetUi?> = _sheet

    private val _changes = Channel<UndoableChange>(Channel.BUFFERED)
    val changes = _changes.receiveAsFlow()

    /** True once anything was skipped, so the support prompt never follows a skip. */
    var skipped = false
        private set

    /**
     * Writes a new entry for [date]; [build] returns null to clear the day instead. A skip on a
     * habit that asks for a note opens the sheet so the note can be written right away.
     */
    fun log(habitId: Long, date: LocalDate, @StringRes verb: Int, build: (Habit, Long) -> Entry?) {
        if (date.isAfter(LocalDate.now())) return
        val day = date.toEpochDay()
        scope.launch {
            val habit = repository.getHabit(habitId) ?: return@launch
            val previous = repository.getEntry(habitId, day)
            val next = build(habit, day)?.copy(loggedAt = now())
            if (next == null && previous == null) return@launch
            if (next != null && next.sameOutcomeAs(previous)) return@launch
            if (next == null) repository.clearEntry(habitId, day) else repository.setEntry(next)
            countCheckIn(next)
            _changes.send(UndoableChange(message(strings(verb, habit.name), date), habitId, day, previous))
            if (next?.status == EntryStatus.SKIP && habit.askForNote && repository.getNote(habitId, day) == null) {
                open(habitId, date, focusNote = true)
            }
        }
    }

    fun markDone(habitId: Long, date: LocalDate) = log(habitId, date, R.string.log_marked_done) { habit, day ->
        Entry(habit.id, day, EntryStatus.DONE, habit.target)
    }

    fun markSkipped(habitId: Long, date: LocalDate) = log(habitId, date, R.string.log_skipped) { habit, day ->
        Entry(habit.id, day, EntryStatus.SKIP)
    }

    /** Clears the day back to not logged. */
    fun reset(habitId: Long, date: LocalDate, @StringRes verb: Int = R.string.log_undone) = log(habitId, date, verb) { _, _ -> null }

    /** Opens the log sheet for [habitId] on [date]; future days can't be logged. */
    fun open(habitId: Long, date: LocalDate, focusNote: Boolean = false) {
        val today = LocalDate.now()
        if (date.isAfter(today)) return
        val day = date.toEpochDay()
        scope.launch {
            val habit = repository.getHabit(habitId) ?: return@launch
            val entry = repository.getEntry(habitId, day)
            val note = repository.getNote(habitId, day)
            val isToday = date == today
            val goal = if (habit.trackType == TrackType.CHECK) null else habit.measure(habit.target)
            val dayLabel = if (isToday) strings(R.string.today) else date.format(LONG_DATE)
            _sheet.value = LogSheetUi(
                habitId = habit.id,
                day = day,
                name = habit.name,
                icon = HabitIcon.fromKey(habit.icon).drawable,
                hue = habit.colorHue.toFloat(),
                trackType = habit.trackType,
                target = habit.target,
                unit = habit.displayUnit(),
                step = habit.step(),
                isToday = isToday,
                dayLabel = dayLabel,
                subtitle = when {
                    goal == null -> dayLabel
                    isToday -> strings(R.string.log_goal_today, goal)
                    else -> strings(R.string.log_goal_on_day, dayLabel, goal)
                },
                status = entry?.status ?: if (habit.trackType == TrackType.CHECK) EntryStatus.DONE else EntryStatus.PARTIAL,
                amount = entry?.amount ?: 0.0,
                noteId = note?.id,
                noteText = note?.text.orEmpty(),
                hasEntry = entry != null,
                focusNote = focusNote,
            )
        }
    }

    fun dismiss() {
        _sheet.value = null
    }

    /** Saves the sheet. A partial amount that reaches the goal is saved as DONE. */
    fun save(sheet: LogSheetUi, status: EntryStatus, amount: Double, noteText: String) {
        _sheet.value = null
        scope.launch {
            val finalStatus = if (status == EntryStatus.PARTIAL && amount >= sheet.target) EntryStatus.DONE else status
            val finalAmount = when (finalStatus) {
                EntryStatus.DONE -> maxOf(amount, sheet.target)
                EntryStatus.PARTIAL -> amount
                EntryStatus.SKIP -> 0.0
            }
            val previous = repository.getEntry(sheet.habitId, sheet.day)
            val next = Entry(sheet.habitId, sheet.day, finalStatus, finalAmount, loggedAt = now())
            if (!next.sameOutcomeAs(previous)) {
                repository.setEntry(next)
                countCheckIn(next)
                val text = when (finalStatus) {
                    EntryStatus.DONE -> strings(R.string.log_marked_done, sheet.name)
                    // "Read · 15 of 20 pages"
                    EntryStatus.PARTIAL -> strings(R.string.log_partial, sheet.name, TodayViewModel.formatNumber(finalAmount), "${TodayViewModel.formatNumber(sheet.target)} ${sheet.unit}".trimEnd())
                    EntryStatus.SKIP -> strings(R.string.log_skipped, sheet.name)
                }
                _changes.send(UndoableChange(message(text, LocalDate.ofEpochDay(sheet.day)), sheet.habitId, sheet.day, previous))
            }
            val text = noteText.trim()
            when {
                text.isNotEmpty() && text != sheet.noteText ->
                    repository.saveNote(Note(id = sheet.noteId ?: 0, habitId = sheet.habitId, date = sheet.day, text = text))
                text.isEmpty() && sheet.noteId != null -> repository.deleteNote(sheet.noteId)
            }
        }
    }

    /** The sheet's Undo pill: closes the sheet and clears that day back to not logged. */
    fun clear(sheet: LogSheetUi) {
        _sheet.value = null
        scope.launch {
            val previous = repository.getEntry(sheet.habitId, sheet.day) ?: return@launch
            repository.clearEntry(sheet.habitId, sheet.day)
            _changes.send(UndoableChange(message(strings(R.string.log_cleared, sheet.name), LocalDate.ofEpochDay(sheet.day)), sheet.habitId, sheet.day, previous))
        }
    }

    /** Puts back exactly what was there before, including deleting an entry that didn't exist. */
    fun undo(change: UndoableChange) {
        scope.launch {
            val previous = change.previous
            if (previous == null) repository.clearEntry(change.habitId, change.day) else repository.setEntry(previous)
        }
    }

    /** Done or partly done counts toward the support prompt. */
    private suspend fun countCheckIn(entry: Entry?) {
        when (entry?.status) {
            EntryStatus.DONE, EntryStatus.PARTIAL -> settings.addCheckIn()
            EntryStatus.SKIP -> skipped = true
            null -> Unit
        }
    }

    /** Past days say which day changed: "Read marked done · Fri 25 Sep". */
    private fun message(text: String, date: LocalDate): String =
        if (date == LocalDate.now()) text else strings(R.string.on_date, text, date.format(SHORT_DATE))

    companion object {
        // Built on each use so a change of language shows up.
        private val LONG_DATE get() = datePattern("EEEE, d MMM")
        private val SHORT_DATE get() = datePattern("EEE d MMM")
    }
}
