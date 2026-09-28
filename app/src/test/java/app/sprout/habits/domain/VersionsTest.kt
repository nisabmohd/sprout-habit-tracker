package app.sprout.habits.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionsTest {
    @Test fun comparesNumbersNotText() {
        assertTrue(isNewerVersion("v1.10.0", "1.9.3"))
        assertTrue(isNewerVersion("v1.0.1", "1.0.0-github"))
        assertTrue(isNewerVersion("2", "1.9.9"))
        assertFalse(isNewerVersion("v1.0.0", "1.0.0-github"))
        assertFalse(isNewerVersion("v0.9.0", "1.0.0"))
        assertFalse(isNewerVersion("1.0", "1.0.0"))
    }

    @Test fun highlightsTakeFiveBulletsWithoutMarkdown() {
        val body = """
            # Sprout 1.1.0

            New:

            - **Language** picker in More
            * Edit any past day
            - `Faster` swipes
            Some paragraph
            - four
            - five
            - six
        """.trimIndent()
        assertEquals(listOf("Language picker in More", "Edit any past day", "Faster swipes", "four", "five"), releaseHighlights(body))
        assertEquals(emptyList<String>(), releaseHighlights(null))
    }
}
