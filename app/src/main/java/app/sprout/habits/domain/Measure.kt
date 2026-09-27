package app.sprout.habits.domain

import app.sprout.habits.data.DurationUnit
import app.sprout.habits.data.Habit
import app.sprout.habits.data.TrackType

/*
 * How a habit's amounts read. Durations are stored in minutes and shown in the unit the user
 * picked for that habit, so "90" in the database reads "1.5 h" for an hours habit.
 */

private fun Habit.inHours() = trackType == TrackType.DURATION && durationUnit == DurationUnit.HOURS

/** "min", "h", the habit's own unit for AMOUNT, or "" for CHECK. */
fun Habit.displayUnit(): String = when (trackType) {
    TrackType.DURATION -> if (durationUnit == DurationUnit.HOURS) "h" else "min"
    TrackType.AMOUNT -> unit
    TrackType.CHECK -> ""
}

/** Multiply a shown value by this to get the stored one. */
fun Habit.storedPerShown(): Double = if (inHours()) 60.0 else 1.0

fun Habit.toShown(stored: Double): Double = stored / storedPerShown()

/** The amount sheet's step: 5 min, a quarter hour, or 1. */
fun Habit.step(): Double = when {
    inHours() -> 0.25
    trackType == TrackType.DURATION -> 5.0
    else -> 1.0
}

/** "20 pages", "45 min", "1.5 h", or just "20" when there's no unit. */
fun Habit.measure(stored: Double): String = "${formatNumber(toShown(stored))} ${displayUnit()}".trim()

/** "15", "1.5", "1.25": up to two decimals, without trailing zeros. */
fun formatNumber(value: Double): String {
    val rounded = Math.round(value * 100) / 100.0
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString()
    else rounded.toBigDecimal().stripTrailingZeros().toPlainString()
}
