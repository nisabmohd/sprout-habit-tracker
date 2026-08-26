package app.sprout.habits.notify

import app.sprout.habits.data.Habit
import app.sprout.habits.data.TrackType
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTextTest {
    private val read = Habit(name = "Read", icon = "book", colorHue = 215, trackType = TrackType.AMOUNT, target = 20.0, unit = "pages")

    @Test fun partialAmountSaysHowMuchIsLeft() = assertEquals(
        "Read · 5 pages to go" to "You're at 15 of 20 pages today.",
        ReminderNotifier.reminderText(read, 15.0),
    )

    @Test fun nothingLoggedStatesTheGoal() =
        assertEquals("Read" to "Goal today: 20 pages.", ReminderNotifier.reminderText(read, 0.0))

    @Test fun durationUsesMinutes() {
        val workout = Habit(name = "Workout", icon = "dumbbell", colorHue = 12, trackType = TrackType.DURATION, target = 45.0)
        assertEquals("Workout · 15 min to go" to "You're at 30 of 45 min today.", ReminderNotifier.reminderText(workout, 30.0))
    }

    @Test fun checkHabitIsJustTheName() {
        val vitamins = Habit(name = "Vitamins", icon = "pill", colorHue = 95)
        assertEquals("Vitamins" to "Time to check it off.", ReminderNotifier.reminderText(vitamins, 0.0))
    }

    @Test fun amountWithoutUnitReadsCleanly() {
        val pushups = Habit(name = "Push-ups", icon = "dumbbell", colorHue = 12, trackType = TrackType.AMOUNT, target = 30.0)
        assertEquals("Push-ups · 10 to go" to "You're at 20 of 30 today.", ReminderNotifier.reminderText(pushups, 20.0))
        assertEquals("Push-ups" to "Goal today: 30.", ReminderNotifier.reminderText(pushups, 0.0))
    }
}
