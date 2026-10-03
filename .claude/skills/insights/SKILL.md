---
name: insights
description: The Insights tab (score ring, habits per day bars, by-habit bars, period and habit filters). Use when changing Insights or its statistics.
---

# Insights

**Code:** `ui/insights/InsightsViewModel.kt` (period This week / This month / Last 3 months, optional habit filter, computed off the main thread) and `InsightsScreen.kt` (all charts drawn with Compose `Canvas`, no chart library; design in `design/insights.html`: a score card with Today's segment bar from `ui/components/SegmentBar.kt`, a Week-style "Habits per day" card with the best weekday highlighted, and one "By habit" card). For month and 3-month periods the per-day bars are averages per weekday.

**Verify:** `python3 tools/verify/smoke.py insights`.

**Score:** `domain/Scoring.kt` `dayCredit`: a logged Skip is left out, a past scheduled day with no entry counts as 0 (it still shows as a Skip mark). Otherwise one Done in an otherwise empty week reads 100%.

**Pitfalls:** percentages use `softWrap = false` so "100%" never breaks at large text sizes.
