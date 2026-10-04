---
name: journal-notes
description: Notes and the Journal tab (write, edit, delete, list). Use when changing notes, the Journal or Add note entry points. Wording rule: Note / Journal, never "reflect".
---

# Journal and notes

**Rules:** one note = one habit + one date + free text. Buttons say "Add note"; the tab is "Journal"; never use "reflect" or "reflection".

**Code:** `ui/journal/` (list, newest first, includes archived habits' notes), `ui/note/WriteNoteScreen.kt` + `WriteNoteViewModel.kt` (route `WriteNoteRoute(noteId, habitId, epochDay)`; habit picker, date picker capped at today, delete with confirmation). Add note is reachable from Today, Journal and Habit detail; the log sheet also saves a note.

**Dates:** the Journal opens on this week (`DateFilter.ThisWeek` in `JournalViewModel`); the sheet's "All dates" shortcut is the only way to see everything, and the chip's ✕ returns to this week. Notes of a day form one group (`groupedShape`, 2 dp gaps) under a header with the note count. A note card uses the Today card pieces (44 dp tile, plain time text, note text straight on the card): no pills or tinted boxes.

**One note per habit and day in the form:** `WriteNoteViewModel.moveTo` looks up the note for the habit and day picked. If there is one, the form becomes that note ("Edit note"), with typed text added under it by `domain/joinNoteText`; moving on to an empty day takes only the typed text along. A note opened by its id is not swapped: changing its date moves it.

**Pitfall:** in adb-driven checks, wait for a sheet to finish opening before `uiautomator dump`; a dump during the animation comes back without the Compose nodes.

**Verify:** `python3 tools/verify/smoke.py journal`.
