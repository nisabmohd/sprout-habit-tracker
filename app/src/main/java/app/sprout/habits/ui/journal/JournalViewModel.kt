package app.sprout.habits.ui.journal

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sprout.habits.R
import app.sprout.habits.Strings
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.Note
import app.sprout.habits.data.SettingsRepository
import app.sprout.habits.ui.components.HabitFilterOption
import app.sprout.habits.ui.components.NoteCardUi
import app.sprout.habits.ui.datePattern
import app.sprout.habits.ui.insights.InsightsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Notes of one day under its header ("Today", "Yesterday", "Thursday, 24 Sep"). */
@Immutable
data class JournalDayUi(val day: Long, val label: String, val notes: List<NoteCardUi>)

/** A range picked in the date sheet. The Journal has no preset ranges and opens on every note. */
private data class DateRange(val from: LocalDate, val to: LocalDate)

@Immutable
data class JournalUi(
    /** Newest day first, already filtered. */
    val days: List<JournalDayUi>,
    /** Empty = every habit. */
    val filter: Set<Long>,
    val options: List<HabitFilterOption>,
    /** Null = all dates. */
    val from: LocalDate?,
    val to: LocalDate?,
    /** That range as its chip shows it, "21 – 27 Sep"; empty for all dates. */
    val dateLabel: String,
    /** False when the journal has no notes at all, whatever the filters. */
    val hasNotes: Boolean,
)

class JournalViewModel(
    private val repository: HabitRepository,
    private val settings: SettingsRepository,
    private val strings: Strings,
) : ViewModel() {
    private val _deleted = Channel<Note>(Channel.CONFLATED)

    /** Each note deleted from the long-press sheet, for the Undo snackbar. */
    val deleted = _deleted.receiveAsFlow()

    private val filterIds = MutableStateFlow<Set<Long>>(emptySet())
    /** Null = all dates, which is where the Journal opens. */
    private val dates = MutableStateFlow<DateRange?>(null)

    /** Null while loading. */
    val state: StateFlow<JournalUi?> =
        combine(
            repository.observeNotes(),
            repository.observeAllHabits(),
            repository.observeEntries(0, Long.MAX_VALUE),
            filterIds,
            dates,
        ) { notes, habits, entries, filter, date ->
            val byId = habits.associateBy { it.id }
            val entryOf = entries.associateBy { it.habitId to it.date }
            val withNotes = notes.mapTo(HashSet()) { it.habitId }
            val today = LocalDate.now()
            val r = date?.let { it.from to it.to }
            val cards = notes
                .filter { filter.isEmpty() || it.habitId in filter }
                .filter { r == null || it.date in r.first.toEpochDay()..r.second.toEpochDay() }
                .mapNotNull { note ->
                    val habit = byId[note.habitId] ?: return@mapNotNull null
                    NoteCardUi.of(note, habit, entryOf[note.habitId to note.date], today, strings)
                }
            JournalUi(
                // Notes arrive newest day first, so grouping keeps that order.
                days = cards.groupBy { it.day }.map { (day, list) -> JournalDayUi(day, dayHeader(LocalDate.ofEpochDay(day), today, strings), list) },
                filter = filter,
                // Active habits, plus archived ones that still have notes.
                options = habits.filter { !it.archived || it.id in withNotes }
                    .map { HabitFilterOption(it.id, it.name, HabitIcon.fromKey(it.icon).drawable, it.colorHue.toFloat()) },
                from = r?.first,
                to = r?.second,
                dateLabel = date?.let { InsightsViewModel.chipRangeLabel(it.from, it.to, today) }.orEmpty(),
                hasNotes = notes.isNotEmpty(),
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setRange(from: LocalDate, to: LocalDate) {
        dates.value = DateRange(minOf(from, to), maxOf(from, to))
    }

    fun clearRange() {
        dates.value = null
    }

    /** Every note again: all dates and every habit. */
    fun showAllNotes() {
        dates.value = null
        filterIds.value = emptySet()
    }

    fun setFilter(habitIds: Set<Long>) {
        filterIds.value = habitIds
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            val note = repository.getNote(id) ?: return@launch
            repository.deleteNote(id)
            _deleted.send(note)
        }
    }

    fun restoreNote(note: Note) {
        viewModelScope.launch { repository.restoreNote(note) }
    }

    companion object {
        /** "Today", "Yesterday", "Thursday, 24 Sep", or with the year when it isn't this one. */
        fun dayHeader(date: LocalDate, today: LocalDate, strings: Strings): String = when (date) {
            today -> strings(R.string.today)
            today.minusDays(1) -> strings(R.string.yesterday)
            else -> date.format(datePattern(if (date.year == today.year) "EEEE, d MMM" else "EEEE, d MMM yyyy"))
        }
    }
}
