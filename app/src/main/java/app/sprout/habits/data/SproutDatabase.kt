package app.sprout.habits.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Habit::class, Entry::class, Note::class], version = 4, exportSchema = true)
abstract class SproutDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun entryDao(): EntryDao
    abstract fun noteDao(): NoteDao

    companion object {
        fun create(context: Context): SproutDatabase =
            Room.databaseBuilder(context, SproutDatabase::class.java, "sprout.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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

        /**
         * 1.1.0: Duration merges into Amount, and Amount habits get a step. Duration habits become
         * Amount habits in "min" (step 5) or "h" (step 0.25); hours habits stored minutes, so their
         * target and entry amounts are divided by 60.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habit ADD COLUMN step REAL NOT NULL DEFAULT 1")
                db.execSQL(
                    "UPDATE entry SET amount = amount / 60.0 WHERE habitId IN " +
                        "(SELECT id FROM habit WHERE trackType = 'DURATION' AND durationUnit = 'HOURS')",
                )
                db.execSQL(
                    "UPDATE habit SET trackType = 'AMOUNT', unit = 'h', target = target / 60.0, step = 0.25 " +
                        "WHERE trackType = 'DURATION' AND durationUnit = 'HOURS'",
                )
                db.execSQL("UPDATE habit SET trackType = 'AMOUNT', unit = 'min', step = 5 WHERE trackType = 'DURATION'")
                db.execSQL("UPDATE habit SET durationUnit = 'MINUTES'")
            }
        }
    }
}
