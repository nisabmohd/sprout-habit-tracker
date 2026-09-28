package app.sprout.habits.domain

import app.sprout.habits.data.Habit
import app.sprout.habits.data.TrackType

/*
 * How a habit's amounts read. AMOUNT habits store and show amounts in their own unit, so a
 * sleep habit with unit "h" stores 7.5 and reads "7.5 h".
 */

/** The habit's own unit for AMOUNT, or "" for CHECK. */
fun Habit.displayUnit(): String = if (trackType == TrackType.AMOUNT) unit else ""

/** The amount sheet's step: the habit's step for AMOUNT (1 if unset), else 1. */
fun Habit.step(): Double = if (trackType == TrackType.AMOUNT && step > 0) step else 1.0

/** "20 pages", "45 min", "1.5 h", or just "20" when there's no unit. */
fun Habit.measure(value: Double): String = "${formatNumber(value)} ${displayUnit()}".trim()

/** "15", "1.5", "1.25": up to two decimals, without trailing zeros. */
fun formatNumber(value: Double): String {
    val rounded = Math.round(value * 100) / 100.0
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString()
    else rounded.toBigDecimal().stripTrailingZeros().toPlainString()
}
