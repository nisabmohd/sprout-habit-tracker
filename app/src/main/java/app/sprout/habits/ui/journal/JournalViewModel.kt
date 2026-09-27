package app.sprout.habits.ui.journal

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.ui.detail.HabitDetailViewModel
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

/** A habit you can filter the Journal by, with how many notes it has. */
@Immutable
data class JournalFilterOption(val habitId: Long, val name: String, val icon: Int, val hue: Float, val noteCount: Int)

@Immutable
data class JournalUi(
    /** Newest first, already filtered. */
    val notes: List<JournalNoteUi>,
    val filter: JournalFilterOption?,
    val options: List<JournalFilterOption>,
)

class JournalViewModel(repository: HabitRepository) : ViewModel() {
    private val filterId = MutableStateFlow<Long?>(null)

    /** Null while loading. */
    val state: StateFlow<JournalUi?> =
        combine(repository.observeNotes(), repository.observeAllHabits(), filterId) { notes, habits, filter ->
            val byId = habits.associateBy { it.id }
            val counts = notes.groupingBy { it.habitId }.eachCount()
            val today = LocalDate.now()
            val options = habits
                .filter { !it.archived || (counts[it.id] ?: 0) > 0 }
                .map { JournalFilterOption(it.id, it.name, HabitIcon.fromKey(it.icon).drawable, it.colorHue.toFloat(), counts[it.id] ?: 0) }
            JournalUi(
                notes = notes
                    .filter { filter == null || it.habitId == filter }
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
                filter = options.firstOrNull { it.habitId == filter },
                options = options,
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Null shows every habit's notes. */
    fun setFilter(habitId: Long?) {
        filterId.value = habitId
    }
}
