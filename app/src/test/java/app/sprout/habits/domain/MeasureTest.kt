package app.sprout.habits.domain

import app.sprout.habits.data.DurationUnit
import app.sprout.habits.data.Habit
import app.sprout.habits.data.TrackType
import org.junit.Assert.assertEquals
import org.junit.Test

class MeasureTest {
    private val minutes = Habit(name = "Workout", icon = "dumbbell", colorHue = 12, trackType = TrackType.DURATION, target = 45.0)
    private val hours = minutes.copy(name = "Deep work", target = 120.0, durationUnit = DurationUnit.HOURS)
    private val pages = Habit(name = "Read", icon = "book", colorHue = 215, trackType = TrackType.AMOUNT, target = 20.0, unit = "pages")

    @Test fun minutesHabitsReadInMinutes() {
        assertEquals("45 min", minutes.measure(minutes.target))
        assertEquals(5.0, minutes.step(), 0.0)
        assertEquals(1.0, minutes.storedPerShown(), 0.0)
    }

    @Test fun hoursHabitsStoreMinutesButReadInHours() {
        assertEquals("2 h", hours.measure(hours.target))
        assertEquals("1.5 h", hours.measure(90.0))
        assertEquals(1.25, hours.toShown(75.0), 0.0)
        assertEquals(0.25, hours.step(), 0.0)
        assertEquals(60.0, hours.storedPerShown(), 0.0)
    }

    @Test fun amountsUseTheirOwnUnit() {
        assertEquals("20 pages", pages.measure(20.0))
        assertEquals("20", pages.copy(unit = "").measure(20.0))
        assertEquals(1.0, pages.step(), 0.0)
    }

    @Test fun numbersDropTrailingZeros() {
        assertEquals("15", formatNumber(15.0))
        assertEquals("1.5", formatNumber(1.5))
        assertEquals("1.25", formatNumber(1.25))
        assertEquals("0.83", formatNumber(50.0 / 60))
    }

    @Test fun switchingToHoursConvertsOnlyWholeQuarterHours() {
        val form = app.sprout.habits.ui.edit.HabitForm(trackType = TrackType.DURATION, target = "90")
        assertEquals("1.5", form.withDurationUnit(DurationUnit.HOURS).target)
        // Typed "2", then picked Hours: they meant 2 hours.
        assertEquals("2", form.copy(target = "2").withDurationUnit(DurationUnit.HOURS).target)
        assertEquals("120", form.copy(target = "2", durationUnit = DurationUnit.HOURS).withDurationUnit(DurationUnit.MINUTES).target)
        // Typed "45" with Hours still selected, then picked Minutes: they meant 45 minutes.
        assertEquals("45", form.copy(target = "45", durationUnit = DurationUnit.HOURS).withDurationUnit(DurationUnit.MINUTES).target)
    }
}
