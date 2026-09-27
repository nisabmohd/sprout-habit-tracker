package app.sprout.habits.ui.journal

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.ui.components.HabitFilterOption
import app.sprout.habits.ui.detail.HabitDetailViewModel
import app.sprout.habits.ui.insights.InsightsViewModel
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

@Immutable
data class JournalNoteUi(
    val id: Long,
    val habitName: String,
    val icon: Int,
    val hue: Float,
    val dateLabel: String,
    val text: String,
)

@Immutable
data class JournalUi(
    /** Newest first, already filtered. */
    val notes: List<JournalNoteUi>,
    /** Empty = every habit. */
    val filter: Set<Long>,
    val options: List<HabitFilterOption>,
    /** Null = all dates. */
    val from: LocalDate?,
    val to: LocalDate?,
    /** "21 – 27 Sep 2026" when a range is set. */
    val rangeLabel: String?,
)

class JournalViewModel(repository: HabitRepository) : ViewModel() {
    private val filterIds = MutableStateFlow<Set<Long>>(emptySet())
    private val range = MutableStateFlow<Pair<LocalDate, LocalDate>?>(null)

    /** Null while loading. */
    val state: StateFlow<JournalUi?> =
        combine(repository.observeNotes(), repository.observeAllHabits(), filterIds, range) { notes, habits, filter, r ->
            val byId = habits.associateBy { it.id }
            val withNotes = notes.mapTo(HashSet()) { it.habitId }
            val today = LocalDate.now()
            JournalUi(
                notes = notes
                    .filter { filter.isEmpty() || it.habitId in filter }
                    .filter { r == null || it.date in r.first.toEpochDay()..r.second.toEpochDay() }
                    .mapNotNull { note ->
                        val habit = byId[note.habitId] ?: return@mapNotNull null
                        JournalNoteUi(
                            id = note.id,
                            habitName = habit.name,
                            icon = HabitIcon.fromKey(habit.icon).drawable,
                            hue = habit.colorHue.toFloat(),
                            dateLabel = HabitDetailViewModel.relativeDate(LocalDate.ofEpochDay(note.date), today),
                            text = note.text,
                        )
                    },
                filter = filter,
                // Active habits, plus archived ones that still have notes.
                options = habits.filter { !it.archived || it.id in withNotes }
                    .map { HabitFilterOption(it.id, it.name, HabitIcon.fromKey(it.icon).drawable, it.colorHue.toFloat()) },
                from = r?.first,
                to = r?.second,
                rangeLabel = r?.let { InsightsViewModel.rangeLabel(it.first, it.second) },
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Nulls show notes from every date. */
    fun setRange(from: LocalDate?, to: LocalDate?) {
        range.value = if (from == null || to == null) null else minOf(from, to) to maxOf(from, to)
    }

    fun setFilter(habitIds: Set<Long>) {
        filterIds.value = habitIds
    }
}
