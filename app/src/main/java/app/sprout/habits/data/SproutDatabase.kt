package app.sprout.habits.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Habit::class, Entry::class, Note::class], version = 1, exportSchema = true)
abstract class SproutDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun entryDao(): EntryDao
    abstract fun noteDao(): NoteDao

    companion object {
        fun create(context: Context): SproutDatabase =
            Room.databaseBuilder(context, SproutDatabase::class.java, "sprout.db").build()
    }
}
