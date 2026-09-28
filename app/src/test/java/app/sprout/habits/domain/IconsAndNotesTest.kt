package app.sprout.habits.domain

import app.sprout.habits.data.Entry
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.data.Habit
import app.sprout.habits.TestStrings
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.data.TrackType
import app.sprout.habits.ui.components.MarkKind
import app.sprout.habits.ui.components.NoteCardUi
import app.sprout.habits.ui.journal.JournalViewModel
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class IconsAndNotesTest {
    @Test fun iconKeysFromBefore031StillWork() {
        assertEquals(HabitIcon.MORNING, HabitIcon.fromKey("sun"))
        assertEquals(HabitIcon.READ, HabitIcon.fromKey("book"))
        assertEquals(HabitIcon.WATER, HabitIcon.fromKey("drop"))
        assertEquals(HabitIcon.MEDITATE, HabitIcon.fromKey("calm"))
        assertEquals(HabitIcon.GYM, HabitIcon.fromKey("dumbbell"))
        assertEquals(HabitIcon.MEDICINE, HabitIcon.fromKey("pill"))
        assertEquals(HabitIcon.OUTDOORS, HabitIcon.fromKey("leaf"))
        assertEquals(HabitIcon.WRITE, HabitIcon.fromKey("pen"))
        assertEquals(HabitIcon.SLEEP, HabitIcon.fromKey("bed"))
        assertEquals(HabitIcon.OUTDOORS, HabitIcon.fromKey("no-such-icon"))
    }

    @Test fun everyIconHasAUniqueKeyAndThereAre29() {
        assertEquals(29, HabitIcon.entries.size)
        assertEquals(29, HabitIcon.entries.map { it.key }.toSet().size)
    }

    @Test fun suggestionsPutTheCurrentIconFirstAndStaySeven() {
        assertEquals(HabitIcon.SUGGESTED, HabitIcon.suggestedFor(HabitIcon.SLEEP))
        val withMoney = HabitIcon.suggestedFor(HabitIcon.MONEY)
        assertEquals(7, withMoney.size)
        assertEquals(HabitIcon.MONEY, withMoney.first())
    }

    private val today = LocalDate.of(2026, 9, 27).toEpochDay()
    private val read = Habit(name = "Read", icon = "menu_book", colorHue = 215, trackType = TrackType.AMOUNT, target = 20.0, unit = "pages")
    private val workout = Habit(name = "Workout", icon = "fitness_center", colorHue = 12, trackType = TrackType.DURATION, target = 45.0)
    private val calm = Habit(name = "Stay calm", icon = "self_improvement", colorHue = 275)

    @Test fun noteOutcomesReadLikeTheDesign() {
        val (partial, partialText) = NoteCardUi.dayOutcome(read, Entry(1, today - 3, EntryStatus.PARTIAL, 8.0), today - 3, today, TestStrings)
        assertEquals(MarkKind.PARTIAL, partial.kind)
        assertEquals(0.4f, partial.fraction, 0.001f)
        assertEquals("8 of 20 pages", partialText)
        assertEquals("Done · 45 min", NoteCardUi.dayOutcome(workout, Entry(1, today, EntryStatus.DONE, 45.0), today, today, TestStrings).second)
        assertEquals("Done", NoteCardUi.dayOutcome(calm, Entry(1, today, EntryStatus.DONE, 1.0), today, today, TestStrings).second)
        assertEquals("Skipped", NoteCardUi.dayOutcome(calm, Entry(1, today, EntryStatus.SKIP), today, today, TestStrings).second)
        // Nothing logged: skipped on a past day, still open today.
        assertEquals(MarkKind.SKIP, NoteCardUi.dayOutcome(calm, null, today - 1, today, TestStrings).first.kind)
        assertEquals(MarkKind.OPEN_TODAY, NoteCardUi.dayOutcome(calm, null, today, today, TestStrings).first.kind)
    }

    @Test fun journalHeadersSayTodayYesterdayThenTheDate() {
        // Month names follow the phone's language; pin one so "Sep" doesn't become "Sept".
        java.util.Locale.setDefault(java.util.Locale.US)
        val t = LocalDate.of(2026, 9, 27)
        assertEquals("Today", JournalViewModel.dayHeader(t, t, TestStrings))
        assertEquals("Yesterday", JournalViewModel.dayHeader(t.minusDays(1), t, TestStrings))
        assertEquals("Thursday, 24 Sep", JournalViewModel.dayHeader(LocalDate.of(2026, 9, 24), t, TestStrings))
        assertEquals("Wednesday, 24 Sep 2025", JournalViewModel.dayHeader(LocalDate.of(2025, 9, 24), t, TestStrings))
    }
}
