package app.sprout.habits.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Habit::class, Entry::class, Note::class], version = 3, exportSchema = true)
abstract class SproutDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun entryDao(): EntryDao
    abstract fun noteDao(): NoteDao

    companion object {
        fun create(context: Context): SproutDatabase =
            Room.databaseBuilder(context, SproutDatabase::class.java, "sprout.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()

        /** 0.3.0: when each entry was logged, for "Done at 6:52 AM". Old entries stay null. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE entry ADD COLUMN loggedAt INTEGER")
            }
        }

        /** 0.3.0: duration habits can be shown in minutes or hours. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habit ADD COLUMN durationUnit TEXT NOT NULL DEFAULT 'MINUTES'")
            }
        }
    }
}
