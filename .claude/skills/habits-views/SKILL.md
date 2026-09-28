---
name: habits-views
description: The Habits tab (Week marks, Overall heatmap) and Habit detail (stats, month calendar, notes). Use when changing those screens, day marks or streak display.
---

# Habits tab and habit detail

**Code**
- `ui/habits/HabitsViewModel.kt` + `HabitsScreen.kt`: Week view queries only the visible week; Overall queries 26 weeks. Best streak in Overall is within that window.
- `ui/detail/HabitDetailViewModel.kt` + `HabitDetailScreen.kt`: all entries of one habit (all-time best streak), month calendar, notes, round header buttons for edit, archive and delete.
- `ui/components/DayMark.kt`: the shared mark shapes (check, pie, dash, dashed ring, dotted ring, dot) also used by widgets.
- `domain/Stats.kt`: `HabitHistory`, `score`, `currentStreak`, `bestStreak`, `counts`.

**Verify:** `python3 tools/verify/smoke.py habits detail`. Unit tests: `StatsTest`.

**Pitfalls**
- The 7 day columns and heatmap month labels are in `CappedFontScale`; month labels drop the first (partial) month if the next starts within 3 columns.
- Days before a habit was created don't count (`Habit.firstDay()`).
