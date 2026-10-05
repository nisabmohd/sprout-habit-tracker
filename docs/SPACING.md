# Sprout spacing and size rule (applies to every screen)

Everything sits on a **4dp grid**. Use only the values below, so no screen feels tighter or looser than another.
When in doubt, copy the same element on the **Today** screen.

## Gaps

| Gap | Value | Where |
| --- | --- | --- |
| Screen side padding | 16dp | Every tab and full screen (Today, Habits, Journal, Insights, More, habit detail, New habit, New note) |
| Sheet side padding | 20dp (24dp for a sheet's title row) | Bottom sheets only |
| Header to content | 8dp under the fixed tab header, then the list starts | All five tabs |
| Title to filter chips | 12dp | Journal, Insights |
| Between cards | 8dp | Habit cards on Today, Week, Overall; settings cards in More |
| Between rows of a grouped list | 2dp | Journal notes of one day, Insights → By habit |
| Between sections | 16dp | Score card → habit cards, card → next section label, Insights cards |
| Between sections on a form | 20dp | New habit |
| Section label → its cards | 8dp | "Today", "By habit", "Notes", "Backup & restore", "Icon", "Days" |
| Section label inset | 4dp from the cards' left edge | Every section label |
| Inside a card | 16dp padding on all sides | Every card and grouped-list row |
| Tile → text in a card | 12dp | Every habit card and row |
| Name → line under it | 2dp | Every habit card and row |
| Card header → card content | 12dp | Week marks, heatmap, note text, score bar |
| Text → switch or value on its right | at least 12dp | Every settings and form row |
| Header buttons | 8dp apart | Every header |

So a section label always has **16dp above it and 8dp below it**. Where the list adds its own gap between items (8dp on More, 2dp in Journal), the label's padding makes up the rest; it never adds to it.

## Sizes

| Element | Size |
| --- | --- |
| Habit tile | 44dp, 14dp corners, 22dp icon. Everywhere a habit is listed: Today, Week, Overall, Journal, Insights, the filter sheet and the habit picker |
| Habit tile in the habit detail header | 56dp, 18dp corners |
| Header icon button | 48dp circle, 22dp icon |
| Touch target | at least 48dp |
| Card corners | 24dp (habit cards on Today 22dp); grouped rows 24dp outside, 6dp between |
| Sheet and dialog corners | 28dp |
| Filter chip | 32dp tall, 8dp corners |
| Day mark | 32dp in Week, 16dp next to an outcome line |
| Progress bar and score segments | 6dp tall |

## Exceptions (only these)
- The month calendar cards (habit detail, date sheets) use 12dp side padding for the same reason.
- Today's habit cards use 12dp horizontal and 8dp vertical padding with a 68dp minimum height, because the fill and the ring reach the card's edge.
- The three stat cards in habit detail use 12dp vertical padding.
