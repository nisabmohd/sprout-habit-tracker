package app.sprout.habits.ui.note

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sprout.habits.data.Entry
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.Note
import app.sprout.habits.domain.joinNoteText
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
    /** True when the screen was opened to write, not to edit a note picked from a list. */
    val opensTyping: Boolean = noteId == 0L

    private val _form = MutableStateFlow(NoteForm())
    val form: StateFlow<NoteForm> = _form

    /** Habits to choose from. An archived habit is included only when the note already belongs to it. */
    val habits: StateFlow<List<Habit>> = repository.observeAllHabits()
        .combine(_form) { all, f -> all.filter { !it.archived || it.id == f.habitId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** What was logged for the chosen habit on the chosen day, shown next to the date. */
    val entry: StateFlow<Entry?> = _form
        .map { it.habitId to it.date.toEpochDay() }
        .flatMapLatest { (habit, day) ->
            if (habit == 0L) flowOf(null) else repository.observeEntries(habit, day, day).map { it.firstOrNull() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * The saved note the form shows because the habit and day chosen already have one. Null for a
     * blank note, and for a note opened by its id (changing its date moves that note).
     */
    private var existing: Note? = null

    /** What the user had typed before [existing] was put above it. */
    private var typedBefore = ""

    private val _done = Channel<Unit>(Channel.CONFLATED)
    val done = _done.receiveAsFlow()

    init {
        viewModelScope.launch {
            val note = if (noteId != 0L) repository.getNote(noteId) else null
            _form.value = if (note != null) {
                NoteForm(true, note.id, note.habitId, LocalDate.ofEpochDay(note.date), note.text)
            } else {
                val fallback = if (habitId != 0L) habitId else repository.observeHabits().first().firstOrNull()?.id ?: 0L
                val date = epochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now()
                formFor(fallback, date, typed = "")
            }
        }
    }

    fun setText(text: String) = _form.update { it.copy(text = text) }
    fun setHabit(id: Long) = moveTo(id, _form.value.date)
    fun setDate(date: LocalDate) = moveTo(_form.value.habitId, minOf(date, LocalDate.now()))

    /**
     * Points the form at another habit or day. If that one already has a note, the form shows it,
     * so saving edits it instead of writing a second note without the first ever being seen.
     */
    private fun moveTo(habitId: Long, date: LocalDate) {
        val f = _form.value
        if (!f.isNew && existing == null) {
            _form.update { it.copy(habitId = habitId, date = date) }
            return
        }
        viewModelScope.launch {
            // Only what the user wrote moves on; a saved note left as shown stays on its own day.
            val typed = if (existing != null && f.text == joinNoteText(existing?.text, typedBefore)) typedBefore else f.text
            val next = formFor(habitId, date, typed)
            // Keep anything typed while the lookup ran.
            _form.update { if (it.text == f.text) next else next.copy(text = it.text) }
        }
    }

    private suspend fun formFor(habitId: Long, date: LocalDate, typed: String): NoteForm {
        val saved = if (habitId != 0L) repository.getNote(habitId, date.toEpochDay()) else null
        existing = saved
        typedBefore = typed
        return NoteForm(
            loaded = true,
            noteId = saved?.id ?: 0,
            habitId = habitId,
            date = date,
            text = joinNoteText(saved?.text, typed),
        )
    }

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
