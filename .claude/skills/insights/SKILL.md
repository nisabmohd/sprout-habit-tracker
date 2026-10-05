---
name: insights
description: The Insights tab (summary card, Patterns, weekly rhythm bars, habit breakdown, range chips and habit filter). Use when changing Insights or its statistics.
---

# Insights

**Code:** `ui/insights/InsightsViewModel.kt` (range: `InsightsRange` preset or a custom range; optional habit filter; computed off the main thread from every entry, because the streak and the comparison with earlier ranges reach back past the range) and `InsightsScreen.kt` (charts drawn with Compose `Canvas`, no chart library; design in `design/insights.png`). Top to bottom:
- `SummaryCard`: Today's segment bar from `ui/components/SegmentBar.kt`, then a divider and one highlights line (longest current streak; the habit for which none of the eight ranges before was better).
- `Section` puts each section's header on the page background; there is never a card inside a card.
- `Patterns`: two white cards joined as a pair. Prime time is the weekday with the highest score; Needs focus is the habit that dropped most against the range before, or else the lowest one.
- `WeeklyRhythm`: one pill bar per `DayColumnUi` with no track; the full height is the largest column total. Done from the bottom, a 4 dp gap, partial on top (at least 16 dp). `ChartTooltip` is a `Popup` above the tapped bar that closes itself after 2.5 s.
- `HabitBreakdown`: one card per habit with `groupedShape` corners. The accent ribbon at the start is the visual (4 dp up to 60%, 8 dp at 100%, solid colour from 85%); the summary line ends with the streak at the end of the range ("5d" with a flame). No bar or ring.

**Verify:** `python3 tools/verify/smoke.py insights`.

**Filters (shared with Journal):** no line under the title. `FilterChipRow` (scrolls sideways) holds the single-select `RangeChip`s (Insights: This week, 7 days, 30 days; Journal: All, 7 days, 30 days), a `CustomRangeChip` in the default's place while a range from the date sheet is on, and `HabitFilterChip` last. `HeaderIconButton(active)` tints the button and adds a plain 8 dp dot. The Journal opens on All and each tab keeps its own range. Up to 14 days the chart has a bar per weekday (8–14 days add both weeks together), longer one bar per week ("Habits per week", labelled with the day the week starts).

**Score:** `domain/Scoring.kt` `dayCredit`: a logged Skip is left out, a past scheduled day with no entry counts as 0 (it still shows as a Skip mark). Otherwise one Done in an otherwise empty week reads 100%.

**Pitfalls:** percentages use `softWrap = false` so "100%" never breaks at large text sizes. The light primary tone (`partialTone`) takes the hue from `colorScheme.primary`, because a plain blend with the card colour comes out grey. On a Monday the week has one day, so check the charts with a longer range too.
