package app.sprout.habits.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The next time a reminder at [minutes] after midnight should fire, strictly after [now], on a
 * day in [daysMask] that is not before [firstDay] (epoch day). Null when no day is scheduled.
 * Uses the zone of [now], so DST changes and time-zone moves are handled by the caller
 * rescheduling on TIME_SET / TIMEZONE_CHANGED.
 */
fun nextReminderTime(now: ZonedDateTime, minutes: Int, daysMask: Int, firstDay: Long): ZonedDateTime? {
    if (daysMask and 0b111_1111 == 0) return null
    val time = LocalTime.of(minutes / 60, minutes % 60)
    var date = maxOf(now.toLocalDate(), LocalDate.ofEpochDay(firstDay))
    repeat(8) {
        if (isScheduled(daysMask, date)) {
            val at = ZonedDateTime.of(date, time, now.zone)
            if (at.isAfter(now)) return at
        }
        date = date.plusDays(1)
    }
    return null
}

fun nextReminderTime(nowMillis: Long, zone: ZoneId, minutes: Int, daysMask: Int, firstDay: Long): Long? =
    nextReminderTime(java.time.Instant.ofEpochMilli(nowMillis).atZone(zone), minutes, daysMask, firstDay)
        ?.toInstant()?.toEpochMilli()
