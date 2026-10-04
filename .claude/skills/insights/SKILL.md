---
name: insights
description: The Insights tab (score ring, habits per day bars, by-habit bars, period and habit filters). Use when changing Insights or its statistics.
---

# Insights

**Code:** `ui/insights/InsightsViewModel.kt` (period This week / This month / Last 3 months, optional habit filter, computed off the main thread) and `InsightsScreen.kt` (all charts drawn with Compose `Canvas`, no chart library; design in `design/insights.png`: a score card with Today's segment bar from `ui/components/SegmentBar.kt`, a Week-style "Habits per day" card with the best weekday highlighted, and a "By habit" grouped list: 2 dp between rows, `groupedShape` corners, the 44 dp habit tile, "6 of 7 days", "· 5-day streak" on the same plain muted line when the streak is over 1 (no icon, no colour), an M3-style bar drawn on `Canvas`, the % as plain text). Streaks need every entry, so the view model also observes all entries. For month and 3-month periods the per-day bars are averages per weekday.

**Verify:** `python3 tools/verify/smoke.py insights`.

**Filters (shared with Journal):** no line under the title. `FilterChipRow` under the header holds `DateFilterChip` ("This week" by default; a picked range is primaryContainer with a ✕ back to this week) and `HabitFilterChip` (dots + "3 habits", or the names on Journal while the dates are the default; tap = reopen sheet, ✕ = clear). `HeaderIconButton(active)` tints the button and adds a 10 dp dot, with no number. Both tabs open on this week. Up to 7 days the chart has a column per weekday, 8–14 days each weekday's average, longer one column per week ("Habits per week").

**Score:** `domain/Scoring.kt` `dayCredit`: a logged Skip is left out, a past scheduled day with no entry counts as 0 (it still shows as a Skip mark). Otherwise one Done in an otherwise empty week reads 100%.

**Pitfalls:** percentages use `softWrap = false` so "100%" never breaks at large text sizes.
