---
name: insights
description: The Insights tab (score ring, habits per day bars, by-habit bars, period and habit filters). Use when changing Insights or its statistics.
---

# Insights

**Code:** `ui/insights/InsightsViewModel.kt` (period This week / This month / Last 3 months, optional habit filter, computed off the main thread) and `InsightsScreen.kt` (all charts drawn with Compose `Canvas`, no chart library). For month and 3-month periods the per-day bars are averages per weekday.

**Verify:** `python3 tools/verify/smoke.py insights`.

**Pitfalls:** percentages use `softWrap = false` so "100%" never breaks at large text sizes.
