---
name: today-logging
description: How the Today screen logs habits (swipe, tap, hold, undo, score) and how to verify it. Use when changing Today, habit cards, the amount sheet or scoring.
---

# Today and logging

**Rules (CLAUDE.md):** outcomes are DONE, PARTIAL (with amount) or SKIP. Swipe right = DONE, swipe left = SKIP, tap circle = toggle DONE, press and hold = amount sheet. Undo snackbar after each change. A past scheduled day with no entry counts as SKIP; today not logged is OPEN (counts as 0).

**Code**
- `ui/today/TodayViewModel.kt`: state for the selected day, `markDone`, `markSkipped`, `toggleDone`, `saveLog`, `undo` (restores the exact previous entry).
- `ui/today/HabitCard.kt` (4 states), `SwipeableHabitCard.kt` (swipe state lives in the card), `LogSheet.kt`, `TodayScreen.kt` (week strip, score card, list, Add note button that lifts above the snackbar).
- `domain/Scoring.kt`: `outcomeOf`, `dayCredit`, `score`.

**Verify:** `python3 tools/verify/smoke.py today` (score, circle toggle, swipe + undo, hold sheet). Unit tests: `ScoringTest`.

**Pitfalls**
- Check habits have no Partial in the sheet; a partial amount that reaches the target saves as DONE.
- The week strip's numbers are in `CappedFontScale`; the cards grow (`heightIn(min = 68.dp)`).
