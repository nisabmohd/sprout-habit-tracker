package app.sprout.habits.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteTextTest {
    @Test
    fun `a day without a note keeps what was typed`() {
        assertEquals("Felt good", joinNoteText(null, "Felt good"))
        assertEquals("", joinNoteText(null, ""))
    }

    @Test
    fun `a day with a note shows that note`() {
        assertEquals("Only 8 pages", joinNoteText("Only 8 pages", ""))
        assertEquals("Only 8 pages", joinNoteText("Only 8 pages", "  "))
    }

    @Test
    fun `typed text goes under the saved note`() {
        assertEquals("Only 8 pages\n\nFelt good", joinNoteText("Only 8 pages", "Felt good"))
    }

    @Test
    fun `the same text is not repeated`() {
        assertEquals("Only 8 pages", joinNoteText("Only 8 pages", "Only 8 pages "))
    }
}
