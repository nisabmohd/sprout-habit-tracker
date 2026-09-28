package app.sprout.habits.domain

import app.sprout.habits.data.Habit
import app.sprout.habits.data.TrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasureTest {
    private val minutes = Habit(name = "Workout", icon = "dumbbell", colorHue = 12, trackType = TrackType.AMOUNT, target = 45.0, unit = "min", step = 5.0)
    private val hours = Habit(name = "Sleep", icon = "bed", colorHue = 12, trackType = TrackType.AMOUNT, target = 7.0, unit = "h", step = 0.25)
    private val pages = Habit(name = "Read", icon = "book", colorHue = 215, trackType = TrackType.AMOUNT, target = 20.0, unit = "pages")

    @Test fun amountsReadInTheirOwnUnit() {
        assertEquals("45 min", minutes.measure(minutes.target))
        assertEquals("7 h", hours.measure(hours.target))
        assertEquals("1.5 h", hours.measure(1.5))
        assertEquals("20 pages", pages.measure(20.0))
        assertEquals("20", pages.copy(unit = "").measure(20.0))
    }

    @Test fun stepComesFromTheHabit() {
        assertEquals(5.0, minutes.step(), 0.0)
        assertEquals(0.25, hours.step(), 0.0)
        assertEquals(1.0, pages.step(), 0.0)
        assertEquals(500.0, pages.copy(step = 500.0).step(), 0.0)
        // A zero or negative step (bad import) falls back to 1; check-off habits always step by 1.
        assertEquals(1.0, pages.copy(step = 0.0).step(), 0.0)
        assertEquals(1.0, Habit(name = "Vitamins", icon = "pill", colorHue = 95, step = 5.0).step(), 0.0)
    }

    @Test fun numbersDropTrailingZeros() {
        assertEquals("15", formatNumber(15.0))
        assertEquals("1.5", formatNumber(1.5))
        assertEquals("1.25", formatNumber(1.25))
        assertEquals("0.83", formatNumber(50.0 / 60))
    }

    @Test fun formNeedsAPositiveTargetAndStep() {
        val form = app.sprout.habits.ui.edit.HabitForm(name = "Walk", trackType = TrackType.AMOUNT, target = "8000", unit = "steps", step = "500")
        assertTrue(form.canSave)
        assertEquals(500.0, form.stepValue!!, 0.0)
        // An empty step means 1; a comma works as the decimal point.
        assertEquals(1.0, form.copy(step = "").stepValue!!, 0.0)
        assertEquals(0.25, form.copy(step = "0,25").stepValue!!, 0.0)
        assertFalse(form.copy(step = "0").canSave)
        assertFalse(form.copy(target = "").canSave)
    }
}
