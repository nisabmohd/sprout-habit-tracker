package app.sprout.habits.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TrackType { CHECK, AMOUNT, DURATION }

enum class EntryStatus { DONE, PARTIAL, SKIP }

@Entity(tableName = "habit")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Key of one of the bundled habit icons, see [HabitIcon]. */
    val icon: String,
    val colorHue: Int,
    val trackType: TrackType = TrackType.CHECK,
    /** Daily goal for AMOUNT (in [unit]) and DURATION (in minutes); 1 for CHECK. */
    val target: Double = 1.0,
    val unit: String = "",
    /** Scheduled weekdays, Monday = bit 0 … Sunday = bit 6. */
    val daysMask: Int = EVERY_DAY,
    /** Minutes after midnight, or null for no reminder. */
    val reminderMinutes: Int? = null,
    val askForNote: Boolean = false,
    val showOnWidget: Boolean = true,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
    /** Epoch millis. */
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val EVERY_DAY = 0b111_1111
    }
}

/** One outcome for one habit on one day. A missing row on a past scheduled day counts as SKIP. */
@Entity(
    tableName = "entry",
    primaryKeys = ["habitId", "date"],
    indices = [Index("date")],
    foreignKeys = [
        ForeignKey(Habit::class, ["id"], ["habitId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class Entry(
    val habitId: Long,
    /** Epoch day (`LocalDate.toEpochDay()`). */
    val date: Long,
    val status: EntryStatus,
    val amount: Double = 0.0,
)

@Entity(
    tableName = "note",
    indices = [Index("habitId"), Index("date")],
    foreignKeys = [
        ForeignKey(Habit::class, ["id"], ["habitId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    /** Epoch day. */
    val date: Long,
    val text: String,
    /** Epoch millis. */
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
)
