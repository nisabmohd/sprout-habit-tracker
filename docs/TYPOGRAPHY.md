# Sprout typography rule (applies to every screen)

Use **only** these 9 text styles. Define them once in `ui/theme/Type.kt` as `SproutType.*`, and never write a raw `fontSize` / `fontWeight` anywhere else.
Weights are only **Regular (400)** and **SemiBold (600)**. Line height: about 1.3 for titles and labels, 1.5 for body and notes.

| Style | Font | Size | Weight | Colour | Use it for (and nothing else) |
| --- | --- | --- | --- | --- | --- |
| `display` | Outfit | 40sp | SemiBold | text | Big input numbers: amount stepper "15", reminder time "09 : 00", the widget streak "11", the Welcome headline |
| `screenTitle` | Outfit | 28sp | SemiBold | text | Tab titles (Today, Habits, Journal, Insights, More), the habit name in habit detail, hero numbers "40%" / "87%" |
| `title` | Outfit | 22sp | SemiBold | text | Top-bar titles (New habit, New note, About), sheet and dialog titles, stat values ("13 days"), the reminder time "9:00 AM" |
| `cardTitle` | Lexend* | 16sp | SemiBold | text | Habit names in every card and row, card titles ("Habits per day", "Theme"), list row titles, the week range "21 – 27 Sep" |
| `body` | Lexend* | 16sp | Regular | text | Note text, paragraphs, text fields |
| `supporting` | Lexend* | 14sp | Regular | muted | The line under a cardTitle ("15 / 20 pages", "7 of 7 days · 2-day streak", "Best streak · 6 days"), **all trailing text on cards** ("Every day", "4-day streak", note time "9:12 AM", "2 notes", "Best: Tuesday"), descriptions |
| `label` | Lexend* | 14sp | SemiBold | depends | Buttons, chips, segmented buttons, **section labels above a group** (muted: "Today", "Yesterday", "By habit", "Notes", "Backup & restore", "Icon", "Days"), the date line under a tab title (muted), summary values ("31 done · 5 partial", muted), **main trailing value** ("100%", text colour), calendar day numbers |
| `caption` | Lexend* | 12sp | Regular / SemiBold | muted | Helper text under fields, legends, field labels ("Name", "Target"; SemiBold), nav bar labels, font descriptions, small labels inside rings ("75%"; SemiBold), "today" under "1/7" |
| `tiny` | Lexend* | 11sp | SemiBold | muted | Weekday names (Mon…Sun, M…S), heatmap month labels, the % inside the Today partial ring (text colour) |

\* The body font is the user's choice (Lexend by default). Outfit is only for `display`, `screenTitle` and `title`.

**Exceptions (only these):**
- The New note editor uses 18sp Regular with 1.55 line height, for comfortable writing.
- Font previews in More → Font show each name in its own font at 16sp, with "Aa" at 22sp.

## Rules that keep screens consistent
1. **Every habit card or row** (Today, Week, Overall, Journal, habit detail, Insights → By habit, filter sheet) uses: `cardTitle` for the name, `supporting` for the line under it, `supporting` for plain trailing text, or `label` (text colour) when the trailing text is the main value.
2. **Section labels** above a group of cards are always `label` in muted colour, 4dp from the left edge of the cards. Never 16sp, never 22sp, never primary colour.
3. **Inside a card**, its own title is `cardTitle`. A card never contains a `title` or `screenTitle`, except stat cards, whose value uses `title`.
4. **Never mix sizes on one line**, except a hero number followed by its unit ("87%" `screenTitle` + "average" `supporting`).
5. **No coloured text** except primary text buttons ("See all", "Clear", "This week") and the error colour (Delete). Habit colours are only for icons, fills and marks, never for text.
6. When in doubt, pick the style of the same element on the **Today** screen.

## What changed in this round (designs attached)
- **Insights → By habit:** the line under the habit name went from 12sp to 14sp (`supporting`). "By habit" became a muted 14sp section label (was 16sp).
- **Journal:** the day headers are muted 14sp SemiBold; "2 notes" and the note time are 14sp `supporting` (were 12sp).
- **Habit detail:** the stat labels are 14sp (were 12sp). "Notes" is a muted 14sp section label (was 22sp). The note time is 14sp.
- **More:** the section labels are muted (were primary colour).
- **New habit:** the form labels (Icon, Color, How do you track it?, Days, Reminder) are muted.
- **Habits → Overall:** "Last 26 weeks" is a 14sp section label.
- **Date range sheet:** the selected range line is 14sp `supporting`, the weekday letters are 11sp `tiny`, and the day numbers are 14sp SemiBold (same as the habit detail calendar).
- **Welcome:** the headline is 40sp `display` (was 36sp).
