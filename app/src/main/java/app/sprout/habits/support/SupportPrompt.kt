package app.sprout.habits.support

import app.sprout.habits.data.Settings

/** When the "Enjoying Sprout?" prompt may show (rules from the design spec). */
object SupportPrompt {
    private const val DAY = 24 * 60 * 60 * 1000L
    const val MIN_DAYS_OF_USE = 7
    const val MIN_CHECK_INS = 30
    const val MIN_DAYS_BETWEEN = 60
    const val MAX_DISMISSALS = 2

    /**
     * After 7+ days of use and 30+ check-ins, at most once every 60 days, never again after
     * "Maybe later" twice. The caller also never shows it right after a skip.
     */
    fun shouldShow(s: Settings, now: Long): Boolean {
        val firstOpen = s.firstOpenAt ?: return false
        if (now - firstOpen < MIN_DAYS_OF_USE * DAY) return false
        if (s.checkInCount < MIN_CHECK_INS) return false
        if (s.supportPromptDismissCount >= MAX_DISMISSALS) return false
        val lastShown = s.supportPromptShownAt
        return lastShown == null || now - lastShown >= MIN_DAYS_BETWEEN * DAY
    }
}
