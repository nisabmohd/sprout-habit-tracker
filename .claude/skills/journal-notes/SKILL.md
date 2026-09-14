---
name: journal-notes
description: Notes and the Journal tab (write, edit, delete, list). Use when changing notes, the Journal or Add note entry points. Wording rule: Note / Journal, never "reflect".
---

# Journal and notes

**Rules:** one note = one habit + one date + free text. Buttons say "Add note"; the tab is "Journal"; never use "reflect" or "reflection".

**Code:** `ui/journal/` (list, newest first, includes archived habits' notes), `ui/note/WriteNoteScreen.kt` + `WriteNoteViewModel.kt` (route `WriteNoteRoute(noteId, habitId, epochDay)`; habit picker, date picker capped at today, delete with confirmation). Add note is reachable from Today, Journal and Habit detail; the log sheet also saves a note.

**Verify:** `python3 tools/verify/smoke.py journal`.
