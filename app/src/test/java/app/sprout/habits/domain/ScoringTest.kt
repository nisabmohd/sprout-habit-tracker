package app.sprout.habits.domain

import app.sprout.habits.data.Entry
import app.sprout.habits.data.EntryStatus
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringTest {
    private val today = 100L

    private fun entry(status: EntryStatus, amount: Double = 0.0, day: Long = today) = Entry(1, day, status, amount)

    @Test fun doneCountsAsOne() = assertEquals(1.0, dayCredit(entry(EntryStatus.DONE), 1.0, today, today)!!, 0.0)

    @Test fun partialCountsAmountOverTarget() =
        assertEquals(0.75, dayCredit(entry(EntryStatus.PARTIAL, 15.0), 20.0, today, today)!!, 1e-9)

    @Test fun partialOverTargetIsCapped() =
        assertEquals(1.0, dayCredit(entry(EntryStatus.PARTIAL, 30.0), 20.0, today, today)!!, 0.0)

    @Test fun skipIsExcluded() = assertNull(dayCredit(entry(EntryStatus.SKIP), 1.0, today, today))

    @Test fun pastDayWithNoEntryIsSkip() {
        assertEquals(DayOutcome.SKIP, outcomeOf(null, today - 1, today))
        assertNull(dayCredit(null, 1.0, today - 1, today))
    }

    @Test fun todayNotYetLoggedIsOpenAndCountsZero() {
        assertEquals(DayOutcome.OPEN, outcomeOf(null, today, today))
        assertEquals(0.0, dayCredit(null, 1.0, today, today)!!, 0.0)
    }

    @Test fun scoreExcludesSkips() {
        // Design example: done, 75%, 63%, skip, and three open habits ≈ 40%.
        val credits = listOf(1.0, 0.75, 0.625, null, 0.0, 0.0, null, 0.0)
        assertEquals(2.375 / 6, score(credits), 1e-9)
    }

    @Test fun scoreOfOnlySkipsIsZero() = assertEquals(0.0, score(listOf(null, null)), 0.0)

    @Test fun scheduleMaskUsesMondayAsBitZero() {
        val monday = LocalDate.of(2026, 9, 21)
        val monWedFri = 0b0010101
        assertTrue(isScheduled(monWedFri, monday))
        assertFalse(isScheduled(monWedFri, monday.plusDays(1)))
        assertTrue(isScheduled(monWedFri, monday.plusDays(4)))
        assertFalse(isScheduled(monWedFri, monday.plusDays(6)))
    }

    @Test fun weekOfRespectsWeekStart() {
        val sunday = LocalDate.of(2026, 9, 27)
        assertEquals(LocalDate.of(2026, 9, 21), weekOf(sunday, DayOfWeek.MONDAY).first())
        assertEquals(sunday, weekOf(sunday, DayOfWeek.SUNDAY).first())
    }
}
