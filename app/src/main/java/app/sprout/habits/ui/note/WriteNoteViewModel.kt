package app.sprout.habits.ui.note

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.Note
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class NoteForm(
    val loaded: Boolean = false,
    val noteId: Long = 0,
    val habitId: Long = 0,
    val date: LocalDate = LocalDate.now(),
    val text: String = "",
) {
    val isNew: Boolean get() = noteId == 0L
    val canSave: Boolean get() = habitId != 0L && text.isNotBlank()
}

@OptIn(ExperimentalCoroutinesApi::class)
class WriteNoteViewModel(
    private val repository: HabitRepository,
    noteId: Long,
    habitId: Long,
    epochDay: Long?,
) : ViewModel() {
    private val _form = MutableStateFlow(NoteForm())
    val form: StateFlow<NoteForm> = _form

    /** Habits to choose from. An archived habit is included only when the note already belongs to it. */
    val habits: StateFlow<List<Habit>> = repository.observeAllHabits()
        .combine(_form) { all, f -> all.filter { !it.archived || it.id == f.habitId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The outcome logged for the chosen habit on the chosen day, shown next to the date. */
    val status: StateFlow<EntryStatus?> = _form
        .map { it.habitId to it.date.toEpochDay() }
        .flatMapLatest { (habit, day) ->
            if (habit == 0L) flowOf(null) else repository.observeEntries(habit, day, day).map { it.firstOrNull()?.status }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _done = Channel<Unit>(Channel.CONFLATED)
    val done = _done.receiveAsFlow()

    init {
        viewModelScope.launch {
            val note = if (noteId != 0L) repository.getNote(noteId) else null
            _form.value = if (note != null) {
                NoteForm(true, note.id, note.habitId, LocalDate.ofEpochDay(note.date), note.text)
            } else {
                val fallback = if (habitId != 0L) habitId else repository.observeHabits().first().firstOrNull()?.id ?: 0L
                NoteForm(loaded = true, habitId = fallback, date = epochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now())
            }
        }
    }

    fun setText(text: String) = _form.update { it.copy(text = text) }
    fun setHabit(id: Long) = _form.update { it.copy(habitId = id) }
    fun setDate(date: LocalDate) = _form.update { it.copy(date = minOf(date, LocalDate.now())) }

    fun save() {
        val f = _form.value
        if (!f.canSave) return
        viewModelScope.launch {
            repository.saveNote(Note(id = f.noteId, habitId = f.habitId, date = f.date.toEpochDay(), text = f.text.trim()))
            _done.send(Unit)
        }
    }

    fun delete() {
        val id = _form.value.noteId
        if (id == 0L) return
        viewModelScope.launch {
            repository.deleteNote(id)
            _done.send(Unit)
        }
    }
}
