package app.sprout.habits.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * CHECK is done or not; AMOUNT counts toward a target in the habit's own unit (pages, min, h).
 * Up to 1.0 there was also DURATION; database migration 3→4 and backup import turn it into AMOUNT.
 */
enum class TrackType { CHECK, AMOUNT }

enum class EntryStatus { DONE, PARTIAL, SKIP }

/** Legacy (before 1.1): how a Duration habit was shown. Only kept so the habit table keeps its column. */
enum class DurationUnit { MINUTES, HOURS }

@Entity(tableName = "habit")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Key of one of the bundled habit icons, see [HabitIcon]. */
    val icon: String,
    val colorHue: Int,
    val trackType: TrackType = TrackType.CHECK,
    /** Daily goal for AMOUNT, in [unit]; 1 for CHECK. Entry amounts are in the same unit. */
    val target: Double = 1.0,
    val unit: String = "",
    /** Legacy, unused since 1.1 (see [DurationUnit]). */
    @ColumnInfo(defaultValue = "MINUTES") val durationUnit: DurationUnit = DurationUnit.MINUTES,
    /** How much the amount sheet's − / + and slider move for AMOUNT, e.g. 500 steps or 0.25 h. */
    @ColumnInfo(defaultValue = "1") val step: Double = 1.0,
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
    /** Epoch millis when the outcome was last set, or null for entries from before 0.3.0. */
    val loggedAt: Long? = null,
) {
    /** Same outcome and amount; the time it was logged doesn't count. */
    fun sameOutcomeAs(other: Entry?): Boolean =
        other != null && habitId == other.habitId && date == other.date && status == other.status && amount == other.amount
}

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
