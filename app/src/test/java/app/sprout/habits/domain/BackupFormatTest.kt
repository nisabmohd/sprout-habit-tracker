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

    @Test fun readsSettingsWhenPresent() {
        val file = BackupManager.parse(
            """{"app":"sprout","version":1,"exportedAt":0,"habits":[],"entries":[],"notes":[],
               "settings":{"theme":"DARK","dynamicColor":false,"accentHue":215,"font":"LEXEND","textScale":1.15,
               "weekStart":"SUNDAY","defaultReminderMinutes":1260}}""".byteInputStream(),
        )
        assertEquals("DARK", file.settings!!.theme)
        assertEquals(1260, file.settings!!.defaultReminderMinutes)
    }

    @Test fun olderBackupWithoutSettingsStillReads() {
        val file = BackupManager.parse("""{"app":"sprout","version":1,"exportedAt":0,"habits":[],"entries":[],"notes":[]}""".byteInputStream())
        assertEquals(null, file.settings)
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

    @Test fun entriesKeepTheTimeTheyWereLogged() {
        val file = BackupManager.parse(
            """{"app":"sprout","version":1,"exportedAt":0,"habits":[],"notes":[],
               "entries":[{"habitId":2,"date":"2026-09-27","status":"DONE","amount":20,"loggedAt":1790000000000},
                          {"habitId":2,"date":"2026-09-26","status":"SKIP"}]}""".byteInputStream(),
        )
        assertEquals(1790000000000L, file.entries[0].loggedAt)
        // Backups from 0.1.0 have no time; the entry still reads.
        assertEquals(null, file.entries[1].loggedAt)
    }

    @Test fun sameOutcomeIgnoresWhenItWasLogged() {
        val a = Entry(2, day, EntryStatus.DONE, 20.0, loggedAt = 1)
        assertEquals(true, a.sameOutcomeAs(a.copy(loggedAt = 2)))
        assertEquals(false, a.sameOutcomeAs(a.copy(status = EntryStatus.PARTIAL)))
        assertEquals(false, a.sameOutcomeAs(null))
    }

    @Test fun oldDurationHabitsImportAsAmounts() {
        val file = BackupManager.parse(
            """{"app":"sprout","version":1,"exportedAt":5,
               "habits":[
                 {"id":1,"name":"Workout","icon":"dumbbell","colorHue":12,"trackType":"DURATION","target":45,"durationUnit":"MINUTES","daysMask":127,"createdAt":0},
                 {"id":2,"name":"Sleep","icon":"bed","colorHue":215,"trackType":"DURATION","target":420,"durationUnit":"HOURS","daysMask":127,"createdAt":0},
                 {"id":3,"name":"Walk","icon":"leaf","colorHue":150,"trackType":"AMOUNT","target":8000,"unit":"steps","step":500,"daysMask":127,"createdAt":0}],
               "entries":[
                 {"habitId":1,"date":"2026-09-27","status":"PARTIAL","amount":30},
                 {"habitId":2,"date":"2026-09-27","status":"PARTIAL","amount":390}],"notes":[]}""".byteInputStream(),
        )
        val (habits, entries, _) = BackupManager.toDatabase(file)
        val (workout, sleep, walk) = habits
        assertEquals(TrackType.AMOUNT, workout.trackType)
        assertEquals("45 min", workout.measure(workout.target))
        assertEquals(5.0, workout.step, 0.0)
        assertEquals("7 h", sleep.measure(sleep.target))
        assertEquals(0.25, sleep.step, 0.0)
        assertEquals(500.0, walk.step, 0.0)
        assertEquals(30.0, entries.first { it.habitId == 1L }.amount, 0.0)
        assertEquals(6.5, entries.first { it.habitId == 2L }.amount, 0.0)
    }
}
