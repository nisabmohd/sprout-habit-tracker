package app.sprout.habits.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** True if [date] falls on one of the weekdays in [daysMask] (Monday = bit 0). */
fun isScheduled(daysMask: Int, date: LocalDate): Boolean =
    daysMask and (1 shl (date.dayOfWeek.value - 1)) != 0

/** The 7 days of the week containing [date], starting on [weekStart]. */
fun weekOf(date: LocalDate, weekStart: DayOfWeek): List<LocalDate> {
    val first = date.with(TemporalAdjusters.previousOrSame(weekStart))
    return List(7) { first.plusDays(it.toLong()) }
}
