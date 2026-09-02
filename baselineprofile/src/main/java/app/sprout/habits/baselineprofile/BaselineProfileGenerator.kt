package app.sprout.habits.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the code paths of a typical first session: welcome, adding a habit, logging it on
 * Today, and visiting every tab. Run with `./gradlew :app:generateBaselineProfile`.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        device.startSession()
    }

    private fun UiDevice.startSession() {
        waitAndClick("Get started")
        // Add a habit so Today, Habits and Insights have something to draw.
        if (!hasObject(By.text("Walk"))) {
            waitAndClick(By.desc("New habit"))
            wait(Until.findObject(By.text("Name")), TIMEOUT)?.click()
            findObject(By.focused(true))?.text = "Walk"
            waitAndClick("Save")
        }
        wait(Until.hasObject(By.text("Walk")), TIMEOUT)
        findObject(By.desc("Mark Walk done"))?.click()
        waitForIdle()
        findObject(By.desc("Mark Walk not done"))?.click()
        waitForIdle()
        for (tab in listOf("Habits", "Journal", "Insights", "More", "Today")) {
            waitAndClick(tab)
            waitForIdle()
            if (tab == "Habits") {
                waitAndClick("Overall")
                waitForIdle()
            }
            if (tab == "More") {
                findObject(By.scrollable(true))?.fling(Direction.DOWN)
                waitForIdle()
            }
        }
    }

    private fun UiDevice.waitAndClick(text: String) = waitAndClick(By.text(text))

    private fun UiDevice.waitAndClick(selector: androidx.test.uiautomator.BySelector) {
        wait(Until.findObject(selector), TIMEOUT)?.click()
    }

    private companion object {
        const val PACKAGE = "app.sprout.habits"
        const val TIMEOUT = 5_000L
    }
}
