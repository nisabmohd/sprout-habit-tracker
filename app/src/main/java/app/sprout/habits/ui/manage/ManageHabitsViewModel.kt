package app.sprout.habits.ui.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ManageHabitsViewModel(private val repository: HabitRepository) : ViewModel() {
    val active: StateFlow<List<Habit>?> =
        repository.observeHabits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val archived: StateFlow<List<Habit>> =
        repository.observeArchivedHabits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** [orderedIds] is the active list, top to bottom. Archived habits keep their place after them. */
    fun reorder(orderedIds: List<Long>) {
        viewModelScope.launch {
            repository.reorderHabits(orderedIds + archived.value.map { it.id })
        }
    }

    fun setArchived(habit: Habit, archived: Boolean) {
        viewModelScope.launch { repository.setArchived(habit.id, archived) }
    }

    fun delete(habit: Habit) {
        viewModelScope.launch { repository.deleteHabit(habit) }
    }
}
