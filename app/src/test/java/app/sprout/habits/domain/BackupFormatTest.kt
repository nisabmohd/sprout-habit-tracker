package app.sprout.habits.domain

import app.sprout.habits.data.Entry
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.data.Habit
import app.sprout.habits.data.Note
import app.sprout.habits.data.TrackType
import app.sprout.habits.data.backup.BackupException
import app.sprout.habits.data.backup.BackupManager
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupFormatTest {
    private val day = LocalDate.of(2026, 9, 27).toEpochDay()
    private val read = Habit(id = 2, name = "Read, daily", icon = "book", colorHue = 215, trackType = TrackType.AMOUNT, target = 20.0, unit = "pages", createdAt = 0)

    @Test fun csvHasHeaderAndQuotesFields() {
        val csv = BackupManager.csv(
            listOf(read),
            listOf(Entry(2, day, EntryStatus.PARTIAL, 15.0)),
            listOf(Note(1, 2, day, "Said \"later\", then read", 0)),
        )
        assertEquals(
            "date,habit,status,amount,target,unit,note\n" +
                "2026-09-27,\"Read, daily\",partial,15,20,pages,\"Said \"\"later\"\", then read\"\n",
            csv,
        )
    }

    @Test fun rejectsFilesThatAreNotBackups() {
        assertThrows(BackupException::class.java) { BackupManager.parse("""{"hello":"world"}""".byteInputStream()) }
        assertThrows(BackupException::class.java) { BackupManager.parse("not json".byteInputStream()) }
        assertThrows(BackupException::class.java) {
            BackupManager.parse("""{"app":"other","version":1,"exportedAt":0,"habits":[],"entries":[],"notes":[]}""".byteInputStream())
        }
    }

    @Test fun rejectsNewerFormat() {
        val e = assertThrows(BackupException::class.java) {
            BackupManager.parse("""{"app":"sprout","version":99,"exportedAt":0,"habits":[],"entries":[],"notes":[]}""".byteInputStream())
        }
        assertEquals(true, e.message!!.contains("newer version"))
    }

    @Test fun readsValidFileAndIgnoresUnknownKeys() {
        val file = BackupManager.parse(
            """{"app":"sprout","version":1,"exportedAt":5,"future":true,
               "habits":[{"id":1,"name":"Walk","icon":"leaf","colorHue":150,"trackType":"CHECK","target":1,"daysMask":127,"createdAt":0}],
               "entries":[{"habitId":1,"date":"2026-09-27","status":"DONE","amount":1}],"notes":[]}""".byteInputStream(),
        )
        assertEquals("Walk", file.habits.single().name)
        assertEquals("2026-09-27", file.entries.single().date)
    }
}
