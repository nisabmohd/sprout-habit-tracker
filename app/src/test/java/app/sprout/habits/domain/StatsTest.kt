package app.sprout.habits.domain

import app.sprout.habits.data.Entry
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.data.EntryStatus.DONE
import app.sprout.habits.data.EntryStatus.PARTIAL
import app.sprout.habits.data.EntryStatus.SKIP
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsTest {
    // Monday 21 Sep 2026 … Sunday 27 Sep 2026.
    private val mon = LocalDate.of(2026, 9, 21).toEpochDay()
    private val everyDay = 0b111_1111
    private val monWedFri = 0b001_0101

    private fun history(
        vararg entries: Pair<Long, EntryStatus>,
        mask: Int = everyDay,
        target: Double = 1.0,
        firstDay: Long = mon - 30,
        amounts: Map<Long, Double> = emptyMap(),
    ) = HabitHistory(
        daysMask = mask,
        target = target,
        entries = entries.associate { (day, status) -> day to Entry(1, day, status, amounts[day] ?: if (status == DONE) target else 0.0) },
        firstDay = firstDay,
    )

    // Score

    @Test fun scoreCountsDoneAndPartialOverScheduledDays() {
        val h = history(mon to DONE, mon + 1 to PARTIAL, mon + 2 to DONE, target = 20.0, amounts = mapOf(mon + 1 to 10.0))
        // (1 + 0.5 + 1) / 3
        assertEquals(2.5 / 3, h.score(mon, mon + 2, today = mon + 2), 1e-9)
    }

    @Test fun scoreExcludesLoggedSkipsButCountsUnloggedPastDaysAsZero() {
        val h = history(mon to DONE, mon + 1 to SKIP) // mon+2 has no entry and is in the past
        assertEquals(0.5, h.score(mon, mon + 2, today = mon + 3), 1e-9)
    }

    @Test fun scoreIgnoresNonScheduledDays() {
        // Tue and Thu have no entries but are not scheduled, so they don't count as skips or zeros.
        val h = history(mon to DONE, mon + 2 to DONE, mon + 4 to PARTIAL, mask = monWedFri, target = 4.0, amounts = mapOf(mon + 4 to 2.0))
        assertEquals(2.5 / 3, h.score(mon, mon + 6, today = mon + 6), 1e-9)
    }

    @Test fun scoreCountsTodayNotYetLoggedAsZero() {
        val h = history(mon to DONE)
        assertEquals(0.5, h.score(mon, mon + 1, today = mon + 1), 1e-9)
    }

    @Test fun scoreIgnoresFutureDaysAndDaysBeforeCreation() {
        val h = history(mon + 2 to DONE, firstDay = mon + 2)
        assertEquals(1.0, h.score(mon, mon + 6, today = mon + 2), 1e-9)
    }

    // Current streak

    @Test fun streakCountsDoneAndPartialDays() {
        val h = history(mon to DONE, mon + 1 to PARTIAL, mon + 2 to DONE)
        assertEquals(3, h.currentStreak(today = mon + 2))
    }

    @Test fun skipEndsStreak() {
        val h = history(mon to DONE, mon + 1 to SKIP, mon + 2 to DONE, mon + 3 to DONE)
        assertEquals(2, h.currentStreak(today = mon + 3))
    }

    @Test fun unloggedPastDayEndsStreak() {
        val h = history(mon to DONE, mon + 2 to DONE)
        assertEquals(1, h.currentStreak(today = mon + 2))
    }

    @Test fun todayNotYetLoggedKeepsStreak() {
        val h = history(mon to DONE, mon + 1 to DONE)
        assertEquals(2, h.currentStreak(today = mon + 2))
    }

    @Test fun todaySkippedEndsStreak() {
        val h = history(mon to DONE, mon + 1 to DONE, mon + 2 to SKIP)
        assertEquals(0, h.currentStreak(today = mon + 2))
    }

    @Test fun nonScheduledDaysAreIgnoredByStreak() {
        // Mon, Wed, Fri done; Tue and Thu not scheduled.
        val h = history(mon to DONE, mon + 2 to DONE, mon + 4 to DONE, mask = monWedFri)
        assertEquals(3, h.currentStreak(today = mon + 6))
    }

    @Test fun streakStopsAtCreationDay() {
        val h = history(mon to DONE, mon + 1 to DONE, firstDay = mon)
        assertEquals(2, h.currentStreak(today = mon + 1))
    }

    @Test fun streakStopsAtRangeStart() {
        // Insights: a 7-day streak counted inside a range that starts on Thursday is 4 days.
        val h = history(*(0..6).map { mon + it to DONE }.toTypedArray(), firstDay = mon)
        assertEquals(4, h.currentStreak(today = mon + 6, from = mon + 3))
    }

    @Test fun noEntriesMeansNoStreak() = assertEquals(0, history(firstDay = mon).currentStreak(today = mon + 3))

    // Best streak and counts

    @Test fun bestStreakFindsLongestRun() {
        val h = history(
            mon to DONE, mon + 1 to DONE, mon + 2 to DONE, mon + 3 to SKIP, mon + 4 to DONE, mon + 5 to PARTIAL,
            firstDay = mon,
        )
        assertEquals(3, h.bestStreak(today = mon + 6))
    }

    @Test fun bestStreakIncludesRunEndingBeforeOpenToday() {
        val h = history(mon to DONE, mon + 1 to DONE, firstDay = mon)
        assertEquals(2, h.bestStreak(today = mon + 2))
    }

    @Test fun countsSplitOutcomes() {
        val h = history(mon to DONE, mon + 1 to PARTIAL, mon + 2 to SKIP, firstDay = mon)
        // mon+3 unlogged in the past = skip; mon+4 is today and open.
        assertEquals(OutcomeCounts(done = 1, partial = 1, skipped = 2), h.counts(mon, mon + 6, today = mon + 4))
    }
}
