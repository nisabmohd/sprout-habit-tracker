package app.sprout.habits.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habit WHERE archived = 0 ORDER BY sortOrder, id")
    fun observeActive(): Flow<List<Habit>>

    @Query("SELECT * FROM habit WHERE archived = 1 ORDER BY sortOrder, id")
    fun observeArchived(): Flow<List<Habit>>

    @Query("SELECT * FROM habit WHERE id = :id")
    fun observe(id: Long): Flow<Habit?>

    @Query("SELECT * FROM habit WHERE id = :id")
    suspend fun get(id: Long): Habit?

    @Query("SELECT * FROM habit ORDER BY sortOrder, id")
    suspend fun getAll(): List<Habit>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM habit")
    suspend fun nextSortOrder(): Int

    @Insert
    suspend fun insert(habit: Habit): Long

    @Update
    suspend fun update(habit: Habit)

    @Query("UPDATE habit SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setSortOrder(id: Long, sortOrder: Int)

    @Query("UPDATE habit SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Delete
    suspend fun delete(habit: Habit)
}

@Dao
interface EntryDao {
    /** All entries for dates in [from, to], both inclusive. */
    @Query("SELECT * FROM entry WHERE date BETWEEN :from AND :to")
    fun observeRange(from: Long, to: Long): Flow<List<Entry>>

    @Query("SELECT * FROM entry WHERE habitId = :habitId AND date BETWEEN :from AND :to ORDER BY date")
    fun observeRange(habitId: Long, from: Long, to: Long): Flow<List<Entry>>

    @Query("SELECT * FROM entry WHERE habitId = :habitId ORDER BY date")
    fun observeAll(habitId: Long): Flow<List<Entry>>

    @Query("SELECT * FROM entry WHERE habitId = :habitId AND date = :date")
    suspend fun get(habitId: Long, date: Long): Entry?

    @Query("SELECT * FROM entry")
    suspend fun getAll(): List<Entry>

    @Upsert
    suspend fun upsert(entry: Entry)

    @Query("DELETE FROM entry WHERE habitId = :habitId AND date = :date")
    suspend fun delete(habitId: Long, date: Long)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM note ORDER BY date DESC, updatedAt DESC")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM note WHERE habitId = :habitId ORDER BY date DESC, updatedAt DESC")
    fun observeForHabit(habitId: Long): Flow<List<Note>>

    @Query("SELECT * FROM note WHERE date BETWEEN :from AND :to")
    fun observeRange(from: Long, to: Long): Flow<List<Note>>

    @Query("SELECT * FROM note WHERE id = :id")
    suspend fun get(id: Long): Note?

    @Query("SELECT * FROM note")
    suspend fun getAll(): List<Note>

    @Upsert
    suspend fun upsert(note: Note): Long

    @Query("DELETE FROM note WHERE id = :id")
    suspend fun delete(id: Long)
}
