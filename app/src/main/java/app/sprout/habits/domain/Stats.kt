package app.sprout.habits.domain

import app.sprout.habits.data.Entry
import java.time.LocalDate

/**
 * The inputs every per-habit statistic needs. Days are epoch days.
 *
 * @param entries the habit's entries keyed by day.
 * @param firstDay the first day that counts (the day the habit was created).
 */
class HabitHistory(
    val daysMask: Int,
    val target: Double,
    val entries: Map<Long, Entry>,
    val firstDay: Long,
) {
    fun isScheduled(day: Long): Boolean = day >= firstDay && isScheduled(daysMask, LocalDate.ofEpochDay(day))

    /** True when the day counts toward a streak: DONE or PARTIAL. */
    fun isKept(day: Long, today: Long): Boolean =
        outcomeOf(entries[day], day, today).let { it == DayOutcome.DONE || it == DayOutcome.PARTIAL }
}

/**
 * Score over the days [from, to], both inclusive: (DONE + PARTIAL amount/target) ÷ scheduled days,
 * logged skips excluded. Days after [today] are not counted; a day with no entry counts as 0.
 */
fun HabitHistory.score(from: Long, to: Long, today: Long): Double {
    val start = maxOf(from, firstDay)
    val end = minOf(to, today)
    if (start > end) return 0.0
    return score((start..end).asSequence().filter { isScheduled(it) }.map { dayCredit(entries[it], target, it, today) }.asIterable())
}

/**
 * Consecutive scheduled days, ending today, that are DONE or PARTIAL. A SKIP (including a past
 * scheduled day with no entry) ends the streak; non-scheduled days are ignored. Today not yet
 * logged does not break it — the streak then counts back from the last scheduled day before today.
 */
fun HabitHistory.currentStreak(today: Long): Int {
    var day = today
    var streak = 0
    if (isScheduled(day) && outcomeOf(entries[day], day, today) == DayOutcome.OPEN) day--
    while (day >= firstDay) {
        if (isScheduled(day)) {
            if (!isKept(day, today)) break
            streak++
        }
        day--
    }
    return streak
}

/** The longest run of kept scheduled days between [firstDay] and [today]. */
fun HabitHistory.bestStreak(today: Long): Int {
    var best = 0
    var run = 0
    for (day in firstDay..today) {
        if (!isScheduled(day)) continue
        if (isKept(day, today)) {
            run++
            if (run > best) best = run
        } else if (day != today) {
            // An open today neither extends nor breaks the run.
            run = 0
        }
    }
    return best
}

/** Totals of each outcome over [from, to] for scheduled days up to [today]. */
data class OutcomeCounts(val done: Int, val partial: Int, val skipped: Int)

fun HabitHistory.counts(from: Long, to: Long, today: Long): OutcomeCounts {
    var done = 0
    var partial = 0
    var skipped = 0
    for (day in maxOf(from, firstDay)..minOf(to, today)) {
        if (!isScheduled(day)) continue
        when (outcomeOf(entries[day], day, today)) {
            DayOutcome.DONE -> done++
            DayOutcome.PARTIAL -> partial++
            DayOutcome.SKIP -> skipped++
            DayOutcome.OPEN -> Unit
        }
    }
    return OutcomeCounts(done, partial, skipped)
}

/** The epoch day the habit was created, in the device's time zone. Earlier days never count. */
fun app.sprout.habits.data.Habit.firstDay(): Long =
    java.time.Instant.ofEpochMilli(createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay()

fun app.sprout.habits.data.Habit.history(entries: Map<Long, Entry>) =
    HabitHistory(daysMask, target, entries, firstDay())
