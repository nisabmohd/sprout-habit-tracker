package app.sprout.habits.domain

import app.sprout.habits.data.Entry
import app.sprout.habits.data.EntryStatus

/** What a scheduled day looks like for one habit. */
enum class DayOutcome { DONE, PARTIAL, SKIP, OPEN }

/**
 * The outcome of a scheduled day. A day with no entry is SKIP if it is in the past, and OPEN
 * (still to do) if it is [today] or later.
 */
fun outcomeOf(entry: Entry?, day: Long, today: Long): DayOutcome = when (entry?.status) {
    EntryStatus.DONE -> DayOutcome.DONE
    EntryStatus.PARTIAL -> DayOutcome.PARTIAL
    EntryStatus.SKIP -> DayOutcome.SKIP
    null -> if (day < today) DayOutcome.SKIP else DayOutcome.OPEN
}

/**
 * How much of one scheduled day counts toward the score: 1 for DONE, amount / target for PARTIAL
 * (capped at 1), 0 for OPEN and for a past day that was never logged, and null for a logged SKIP,
 * which is left out of the score entirely.
 */
fun dayCredit(entry: Entry?, target: Double, day: Long, today: Long): Double? =
    when (outcomeOf(entry, day, today)) {
        DayOutcome.DONE -> 1.0
        DayOutcome.PARTIAL -> if (target > 0) (entry!!.amount / target).coerceIn(0.0, 1.0) else 0.0
        DayOutcome.OPEN -> 0.0
        // Only a skip the user chose is excused; a day that was just left empty counts as 0.
        DayOutcome.SKIP -> if (entry == null) 0.0 else null
    }

/**
 * Score = (DONE + PARTIAL amount / target) ÷ scheduled days, logged skips excluded.
 * Takes the credits of each scheduled day (see [dayCredit]). Returns 0 when nothing counts.
 */
fun score(credits: Iterable<Double?>): Double {
    var sum = 0.0
    var count = 0
    for (c in credits) {
        if (c == null) continue
        sum += c
        count++
    }
    return if (count == 0) 0.0 else sum / count
}
