package app.sprout.habits.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderTimeTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val everyDay = 0b111_1111
    private val longAgo = LocalDate.of(2020, 1, 1).toEpochDay()

    // Sunday 27 Sep 2026
    private fun at(day: Int, hour: Int, minute: Int = 0, month: Int = 9) = ZonedDateTime.of(2026, month, day, hour, minute, 0, 0, zone)

    @Test fun laterTodayWhenTimeHasNotPassed() =
        assertEquals(at(27, 21), nextReminderTime(at(27, 9), 21 * 60, everyDay, longAgo))

    @Test fun tomorrowWhenTimeHasPassed() =
        assertEquals(at(28, 9), nextReminderTime(at(27, 10), 9 * 60, everyDay, longAgo))

    @Test fun exactlyNowMovesToNextDay() =
        assertEquals(at(28, 9), nextReminderTime(at(27, 9), 9 * 60, everyDay, longAgo))

    @Test fun skipsUnscheduledDays() {
        val monWedFri = 0b001_0101
        // Sunday evening → Monday
        assertEquals(at(28, 8), nextReminderTime(at(27, 20), 8 * 60, monWedFri, longAgo))
        // Monday after the time → Wednesday
        assertEquals(at(30, 8), nextReminderTime(at(28, 9), 8 * 60, monWedFri, longAgo))
    }

    @Test fun waitsForFirstDay() {
        val first = LocalDate.of(2026, 10, 1).toEpochDay()
        assertEquals(at(1, 7, month = 10), nextReminderTime(at(27, 6), 7 * 60, everyDay, first))
    }

    @Test fun noDaysMeansNoReminder() = assertNull(nextReminderTime(at(27, 6), 7 * 60, 0, longAgo))

    @Test fun keepsWallClockTimeAcrossDstChange() {
        // Clocks go back on Sunday 25 Oct 2026 in Berlin; 9:00 stays 9:00 local.
        val next = nextReminderTime(ZonedDateTime.of(2026, 10, 24, 22, 0, 0, 0, zone), 9 * 60, everyDay, longAgo)!!
        assertEquals(9, next.hour)
        assertEquals(25, next.dayOfMonth)
    }
}
