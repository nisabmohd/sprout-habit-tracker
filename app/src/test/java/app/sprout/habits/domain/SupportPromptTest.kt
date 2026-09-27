package app.sprout.habits.domain

import app.sprout.habits.data.Settings
import app.sprout.habits.support.SupportPrompt
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportPromptTest {
    private val day = 24 * 60 * 60 * 1000L
    private val now = 1_000 * day
    private val eligible = Settings(firstOpenAt = now - 8 * day, checkInCount = 30)

    @Test fun showsWhenEligible() = assertTrue(SupportPrompt.shouldShow(eligible, now))

    @Test fun notBeforeSevenDays() =
        assertFalse(SupportPrompt.shouldShow(eligible.copy(firstOpenAt = now - 6 * day), now))

    @Test fun notBeforeThirtyCheckIns() =
        assertFalse(SupportPrompt.shouldShow(eligible.copy(checkInCount = 29), now))

    @Test fun notOnFirstOpen() = assertFalse(SupportPrompt.shouldShow(eligible.copy(firstOpenAt = null), now))

    @Test fun atMostEverySixtyDays() {
        assertFalse(SupportPrompt.shouldShow(eligible.copy(supportPromptShownAt = now - 59 * day), now))
        assertTrue(SupportPrompt.shouldShow(eligible.copy(supportPromptShownAt = now - 60 * day), now))
    }

    @Test fun neverAfterTwoMaybeLaters() =
        assertFalse(SupportPrompt.shouldShow(eligible.copy(supportPromptDismissCount = 2), now))
}
