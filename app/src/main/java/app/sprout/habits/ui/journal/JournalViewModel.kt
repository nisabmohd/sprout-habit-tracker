package app.sprout.habits.ui.journal

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.ui.detail.HabitDetailViewModel
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
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

class JournalViewModel(repository: HabitRepository) : ViewModel() {
    /** Newest first; null while loading. */
    val notes: StateFlow<List<JournalNoteUi>?> =
        combine(repository.observeNotes(), repository.observeAllHabits()) { notes, habits ->
            val byId = habits.associateBy { it.id }
            val today = LocalDate.now()
            notes.mapNotNull { note ->
                val habit = byId[note.habitId] ?: return@mapNotNull null
                JournalNoteUi(
                    id = note.id,
                    habitName = habit.name,
                    icon = HabitIcon.fromKey(habit.icon).drawable,
                    hue = habit.colorHue.toFloat(),
                    dateLabel = HabitDetailViewModel.relativeDate(LocalDate.ofEpochDay(note.date), today),
                    text = note.text,
                )
            }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
