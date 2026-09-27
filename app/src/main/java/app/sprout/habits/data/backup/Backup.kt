package app.sprout.habits.data.backup

import app.sprout.habits.data.DurationUnit
import app.sprout.habits.data.Entry
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.Note
import app.sprout.habits.data.SettingsRepository
import app.sprout.habits.ui.theme.ThemeSettings
import kotlinx.coroutines.flow.first
import app.sprout.habits.data.TrackType
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

/**
 * The backup file. Kept separate from the Room entities so the file format stays stable when the
 * database changes; bump [FORMAT_VERSION] on incompatible changes.
 */
@Serializable
data class BackupFile(
    val app: String = APP_ID,
    val version: Int = FORMAT_VERSION,
    val exportedAt: Long,
    val habits: List<HabitDto>,
    val entries: List<EntryDto>,
    val notes: List<NoteDto>,
    /** Absent in backups made before preferences were included; restoring then keeps current ones. */
    val settings: SettingsDto? = null,
) {
    companion object {
        const val APP_ID = "sprout"
        const val FORMAT_VERSION = 1
    }
}

@Serializable
data class HabitDto(
    val id: Long,
    val name: String,
    val icon: String,
    val colorHue: Int,
    val trackType: String,
    val target: Double,
    val unit: String = "",
    /** MINUTES or HOURS; absent in backups from 0.1.0. */
    val durationUnit: String = "MINUTES",
    val daysMask: Int,
    val reminderMinutes: Int? = null,
    val askForNote: Boolean = false,
    val showOnWidget: Boolean = true,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
    val createdAt: Long,
)

@Serializable
data class EntryDto(
    val habitId: Long,
    /** ISO date, readable in the file ("2026-09-27"). */
    @SerialName("date") val date: String,
    val status: String,
    val amount: Double = 0.0,
    /** Epoch millis; absent in backups from 0.1.0. */
    val loggedAt: Long? = null,
)

@Serializable
data class NoteDto(val id: Long, val habitId: Long, val date: String, val text: String, val updatedAt: Long)

/** Appearance and general preferences from the More tab. Names are enum names; unknown ones fall back. */
@Serializable
data class SettingsDto(
    val theme: String,
    val dynamicColor: Boolean,
    val accentHue: Float,
    val font: String,
    val textScale: Float,
    val weekStart: String,
    val defaultReminderMinutes: Int,
)

class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Converts between the database and a [BackupFile], and reads and writes JSON and CSV. */
class BackupManager(
    private val repository: HabitRepository,
    private val settings: SettingsRepository,
) {

    suspend fun createBackup(now: Long = System.currentTimeMillis()): BackupFile {
        val (habits, entries, notes) = repository.snapshot()
        return BackupFile(
            exportedAt = now,
            habits = habits.map {
                HabitDto(
                    it.id, it.name, it.icon, it.colorHue, it.trackType.name, it.target, it.unit, it.durationUnit.name, it.daysMask,
                    it.reminderMinutes, it.askForNote, it.showOnWidget, it.sortOrder, it.archived, it.createdAt,
                )
            },
            entries = entries.map { EntryDto(it.habitId, LocalDate.ofEpochDay(it.date).toString(), it.status.name, it.amount, it.loggedAt) },
            notes = notes.map { NoteDto(it.id, it.habitId, LocalDate.ofEpochDay(it.date).toString(), it.text, it.updatedAt) },
            settings = settings.settings.first().let {
                SettingsDto(
                    theme = it.theme.mode.name,
                    dynamicColor = it.theme.dynamicColor,
                    accentHue = it.theme.accentHue,
                    font = it.theme.font.name,
                    textScale = it.theme.textScale,
                    weekStart = it.weekStart.name,
                    defaultReminderMinutes = it.defaultReminderMinutes,
                )
            },
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun exportJson(out: OutputStream) = json.encodeToStream(createBackup(), out)

    fun encode(backup: BackupFile): String = json.encodeToString(BackupFile.serializer(), backup)

    /** Replaces everything in the database with [backup]. Validates first; on error nothing changes. */
    suspend fun restore(backup: BackupFile) {
        val habits = backup.habits.map {
            Habit(
                id = it.id, name = it.name, icon = it.icon, colorHue = it.colorHue,
                trackType = enumOr(it.trackType, TrackType.CHECK), target = it.target, unit = it.unit,
                durationUnit = enumOr(it.durationUnit, DurationUnit.MINUTES),
                daysMask = it.daysMask and 0b111_1111, reminderMinutes = it.reminderMinutes?.takeIf { m -> m in 0 until 24 * 60 },
                askForNote = it.askForNote, showOnWidget = it.showOnWidget, sortOrder = it.sortOrder,
                archived = it.archived, createdAt = it.createdAt,
            )
        }
        val ids = habits.mapTo(HashSet()) { it.id }
        if (ids.size != habits.size) throw BackupException("This backup is damaged and can't be imported.")
        val entries = backup.entries
            .filter { it.habitId in ids }
            .map { Entry(it.habitId, parseDate(it.date), enumOr(it.status, EntryStatus.DONE), it.amount, it.loggedAt) }
            .distinctBy { it.habitId to it.date }
        val notes = backup.notes
            .filter { it.habitId in ids }
            .map { Note(it.id, it.habitId, parseDate(it.date), it.text, it.updatedAt) }
            .distinctBy { it.id }
        repository.replaceAll(habits, entries, notes)
        backup.settings?.let { dto ->
            val current = settings.settings.first()
            settings.restore(
                current.copy(
                    theme = ThemeSettings(
                        mode = enumOr(dto.theme, current.theme.mode),
                        dynamicColor = dto.dynamicColor,
                        accentHue = dto.accentHue,
                        font = enumOr(dto.font, current.theme.font),
                        textScale = dto.textScale.coerceIn(0.85f, 1.3f),
                    ),
                    weekStart = enumOr(dto.weekStart, current.weekStart),
                    defaultReminderMinutes = dto.defaultReminderMinutes.coerceIn(0, 24 * 60 - 1),
                ),
            )
        }
    }

    /** One row per logged day, with its note if there is one. Dates are ISO; opens in any spreadsheet. */
    suspend fun exportCsv(out: OutputStream) {
        val (habits, entries, notes) = repository.snapshot()
        out.bufferedWriter().use { w -> w.write(csv(habits, entries, notes)) }
    }

    companion object {
        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        /** Reads and checks a backup file without touching the database. */
        @OptIn(ExperimentalSerializationApi::class)
        fun parse(input: InputStream): BackupFile {
            val backup = try {
                json.decodeFromStream(BackupFile.serializer(), input)
            } catch (e: SerializationException) {
                throw BackupException("This isn't a Sprout backup file.", e)
            } catch (e: IllegalArgumentException) {
                throw BackupException("This isn't a Sprout backup file.", e)
            }
            if (backup.app != BackupFile.APP_ID) throw BackupException("This isn't a Sprout backup file.")
            if (backup.version > BackupFile.FORMAT_VERSION) {
                throw BackupException("This backup was made by a newer version of Sprout. Update the app, then try again.")
            }
            return backup
        }

        fun csv(habits: List<Habit>, entries: List<Entry>, notes: List<Note>): String {
            val byId = habits.associateBy { it.id }
            val noteFor = notes.groupBy { it.habitId to it.date }.mapValues { (_, n) -> n.joinToString(" / ") { it.text } }
            val rows = entries.sortedWith(compareBy({ it.date }, { byId[it.habitId]?.sortOrder ?: 0 }))
            return buildString {
                appendLine("date,habit,status,amount,target,unit,note")
                for (e in rows) {
                    val h = byId[e.habitId] ?: continue
                    val unit = if (h.trackType == TrackType.DURATION) "min" else h.unit
                    appendLine(
                        listOf(
                            LocalDate.ofEpochDay(e.date).toString(), h.name, e.status.name.lowercase(),
                            number(e.amount), number(h.target), unit, noteFor[h.id to e.date].orEmpty(),
                        ).joinToString(",") { field(it) },
                    )
                }
            }
        }

        /** Quotes a CSV field when it holds a comma, quote or line break. */
        fun field(value: String): String =
            if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

        private fun number(v: Double) = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

        private fun parseDate(s: String): Long = try {
            LocalDate.parse(s).toEpochDay()
        } catch (e: java.time.format.DateTimeParseException) {
            throw BackupException("The backup has a date that can't be read: $s", e)
        }

        private inline fun <reified E : Enum<E>> enumOr(name: String, default: E): E =
            enumValues<E>().firstOrNull { it.name == name } ?: default
    }
}
