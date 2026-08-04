package app.sprout.habits.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

/** The single entry point to habits, entries and notes. */
class HabitRepository(private val db: SproutDatabase) {
    private val habits = db.habitDao()
    private val entries = db.entryDao()
    private val notes = db.noteDao()

    // Habits

    fun observeHabits(): Flow<List<Habit>> = habits.observeActive()
    fun observeArchivedHabits(): Flow<List<Habit>> = habits.observeArchived()
    fun observeHabit(id: Long): Flow<Habit?> = habits.observe(id)
    suspend fun getHabit(id: Long): Habit? = habits.get(id)

    /** Inserts a new habit at the end of the list, or updates an existing one. Returns its id. */
    suspend fun saveHabit(habit: Habit): Long = if (habit.id == 0L) {
        db.withTransaction { habits.insert(habit.copy(sortOrder = habits.nextSortOrder())) }
    } else {
        habits.update(habit)
        habit.id
    }

    /** Persists a new order; [orderedIds] is the full list of habit ids top to bottom. */
    suspend fun reorderHabits(orderedIds: List<Long>) = db.withTransaction {
        orderedIds.forEachIndexed { index, id -> habits.setSortOrder(id, index) }
    }

    suspend fun setArchived(id: Long, archived: Boolean) = habits.setArchived(id, archived)

    /** Deletes the habit together with its entries and notes (cascade). */
    suspend fun deleteHabit(habit: Habit) = habits.delete(habit)

    // Entries

    fun observeEntries(fromDay: Long, toDay: Long): Flow<List<Entry>> = entries.observeRange(fromDay, toDay)
    fun observeEntries(habitId: Long, fromDay: Long, toDay: Long): Flow<List<Entry>> =
        entries.observeRange(habitId, fromDay, toDay)
    fun observeAllEntries(habitId: Long): Flow<List<Entry>> = entries.observeAll(habitId)
    suspend fun getEntry(habitId: Long, day: Long): Entry? = entries.get(habitId, day)
    suspend fun setEntry(entry: Entry) = entries.upsert(entry)

    /** Clears the day back to "not logged". */
    suspend fun clearEntry(habitId: Long, day: Long) = entries.delete(habitId, day)

    // Notes

    fun observeNotes(): Flow<List<Note>> = notes.observeAll()
    fun observeNotes(habitId: Long): Flow<List<Note>> = notes.observeForHabit(habitId)
    fun observeNotes(fromDay: Long, toDay: Long): Flow<List<Note>> = notes.observeRange(fromDay, toDay)
    suspend fun getNote(id: Long): Note? = notes.get(id)
    suspend fun saveNote(note: Note): Long {
        val id = notes.upsert(note.copy(updatedAt = System.currentTimeMillis()))
        // Upsert returns -1 when it updated an existing row.
        return if (id == -1L) note.id else id
    }
    suspend fun deleteNote(id: Long) = notes.delete(id)
}
