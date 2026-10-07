# Changelog

## Unreleased

Changed:

- Space Grotesk is no longer lighter than the other fonts.
- Numbers (the score on Today and Insights, the amount and time pickers, the stats in habit detail) use the font you picked in More instead of always Outfit.
- Haptics: tapping a habit's circle, a swipe crossing the point where it logs, each step of − / + and the slider, Save in the amount sheet, and every switch, chip, segmented button, day and checkbox now give a light tap.

## 1.3.1

Changed:

- Insights, Habit breakdown: each habit's score is a ring on the right, like a partly done habit on Today, in place of the colour ribbon. The habits sit together in one group.
- Insights, Habit breakdown: a Score | Streak switch sorts the habits by score or by current streak. Sorted by streak, the ring shows each habit's streak within the chosen dates (a 7-day range shows at most 7 days).
- Insights, Patterns: "Prime time" and "Needs focus" are labels with an icon at the top of each card, and the text below matches the habit rows.
- Insights: the summary card shows just your score and the done and partial counts. The streak and best-week line is gone; each habit's streak is in Habit breakdown.
- Insights, Patterns: Prime time shows just the score ("92% completion") on one line.
- Today: the sample habits banner has a proper card title ("These are sample habits").

## 1.3.0

Fixed:

- The Journal opened on "This week" and hid older notes. It now opens on all notes.
- The app no longer opens on an empty "0% of today" card before the habits appear.

Changed:

- Journal and Insights have range chips under the title: All, 7 days and 30 days on the Journal; This week, 7 days and 30 days on Insights. A range picked from the calendar shows as its own chip.
- When no notes match, the Journal says which filter is hiding them and has a "Show all notes" button.
- Insights has a new layout. The summary card adds your longest streak and the habit having its best week. A "Patterns" card shows your best weekday and the habit that needs focus. "Weekly rhythm" is a row of rounded bars, one per weekday (one per week for ranges over two weeks). "Habit breakdown" lists each habit with its score, its streak and a colour ribbon that is wider and stronger the more consistent the habit was. Tap a bar to see its numbers, and tap a habit to open it.
- Today: the ring on a partly done habit is slimmer, with rounded ends.
- The dot on the calendar and filter buttons is smaller and has no ring.

## 1.2.2

Changed:

- Text is consistent across screens: nine text styles, used the same way everywhere. Section labels ("Today", "By habit", "Notes", "Backup & restore", "Icon", "Days") are small and grey, and the text on the right of a card is always the same size.
- Journal: the day headings are grey, and the note count and note time are a little larger.
- Insights: the line under each habit's name is a little larger.
- Habit detail: the labels under the three numbers are a little larger.
- More: the values on the right ("System default", "Monday") are smaller, and the section labels are grey instead of the accent color.
- Date range sheet: the selected range is regular weight and the weekday letters are smaller.
- Only two text weights are used now, Regular and SemiBold.
- Gaps and sizes are consistent too: cards sit the same distance apart on every tab, every card has the same padding, section labels have the same space above and below, and the habit icon tile is the same size in the filter and habit pickers as on Today.

## 1.2.1

New:

- The Journal opens on this week's notes. The date chip under the title shows what you're looking at; tap it or the calendar button to pick other dates or "All dates".
- Journal notes are grouped by day, with the number of notes next to each date.
- Insights lists each habit with how many days you kept it, its current streak and its score.
- Habits → Overall shows each habit's best streak under its name and the current streak on the right.

Changed:

- Active filters show as chips under the title on Journal and Insights: the dates first, then the habits. The calendar and filter buttons get a small dot while their filter is on.
- The sample habits banner has a round delete button, like the buttons in the headers.
- Habit cards look the same on every screen: the same icon tile as on Today, and plain text on the right.
- The reminder card in New habit has a bell in the habit's color.
- The Figtree font is replaced by Space Grotesk. If you had picked Figtree, the app goes back to Lexend; choose Space Grotesk in More → Appearance.

Fixed:

- The title, its buttons and the filter chips stay at the top of every tab while you scroll. Before, you had to scroll back to the top to change or clear a filter.
- In New note, picking a day that already has a note for that habit now shows that note instead of an empty page. Anything you had typed is added under it.

## 1.2.0

New:

- Journal and Insights show which filters are on: the calendar and filter buttons turn tinted, the filter button shows how many habits are picked, and a chip under the title lists them. Tap ✕ after the dates to clear the range, ✕ on the chip to clear the habits, or the chip to change them.
- Insights has the same card style as Today and Habits: a score card with one bar per habit, a "Habits per day" card with a column for each weekday and the best day highlighted, and one "By habit" card with each habit's icon and score.
- Insights groups ranges longer than two weeks by week ("Habits per week").
- Swipe the Undo message left or right to dismiss it.

Changed:

- The score now counts a past day you never logged as 0. Only a day you marked Skip is left out. Before, one Done in an otherwise empty week showed as 100%.
- A reminder that is still in your notifications goes away once you mark that habit done or skipped.
- New installs start with the amber accent and Dynamic color off; turn it on in More → Appearance. If you already had Sprout, your colors stay as they were.
- The accent colors Gold and Pink are now called Amber and Plum.
- About says "Made by Nisab Mohd & Contributors".

## 1.1.0

New:

- Check for updates (GitHub build): About has a "Check for updates" button, Sprout looks once a day on its own, and "Update" downloads the new version and opens Android's installer. More → About Sprout shows "Update available" when there is one. The F-Droid and Play builds are updated by their stores.
- A simpler time picker for reminders (New habit and More → Default reminder): hour and minute tiles with arrows above and below, and an AM/PM switch. Tap an arrow, hold it to move faster, or drag a number up or down. Phones set to the 24-hour clock get 00–23 and no AM/PM.
- Amount and Duration are now one type, **Measure**: a target in any unit (20 pages, 8000 steps, 45 min, 7 h) and a **Step** that − / + and the slider move by (1 page, 500 steps, 0.25 h). Your Duration habits become Measure habits in min (step 5) or h (step 0.25), with their history converted; old backups import the same way.
- New installs start with sample habits, two weeks of history and notes on several days, so every screen has something to show. A card on Today and More → Remove sample data delete them in one tap; your own habits stay. Updates don't get sample data.
- Press and hold a note in the Journal to edit, share or delete it. Deleting shows Undo.
- The widget picker shows a preview of each widget instead of the Sprout logo.

Changed:

- One header on every tab: the title on top, the date or date range under it, round buttons on the right, all in the same place.
- Round buttons in every screen header: back or close on the left, and on New habit, Edit habit and notes, Save is a round ✓ button. A habit's page has Edit, Archive and Delete buttons, and a note has a Delete button, instead of a ⋮ menu.

Fixed:

- On New habit, the icon next to the name sat higher than the name box.

## 1.0.0

New:

- Sprout speaks Hindi, Spanish, German, French, Portuguese (Brazil) and Japanese. Pick one in More → General → Language, or follow the phone's language. On Android 13 and later it's also in Settings → App languages.
- Dates follow the language too: "Montag, 28. September", "9月28日月曜日". English keeps "Monday, 28 September".
- The More tab has a tune icon, and "Week starts on" uses the same sheet style as Language.

Fixed:

- Scrolling Today could mark a habit done or skipped. A swipe now has to drag the card past half its width.
- Insights' "This week" ran to the end of the week and selected days that haven't happened yet. It now ends today.

## 0.3.1

New:

- Choose from 29 habit icons (Material Symbols) in a searchable "Choose icon" sheet; New habit shows 7 suggestions and a More tile. Existing habits keep a matching icon.
- Journal notes are grouped under "Today", "Yesterday" and dated headers, and each card shows the habit, that day's outcome ("Skipped", "8 of 20 pages", "Done · 45 min") and the time.
- The note editor is one clean card: the habit row on top and a plain text area that opens ready to type.
- Habits → Overall shows "X-day streak · best Y", and a habit's page shows "N days this month", instead of percentages. Notes on a habit's page use the Journal card style.

Fixed:

- Sheets could reach the top of the screen with a long list, and sat above the bottom edge. They now sit on the bottom edge and leave the top of the screen visible.
- On Android 8 and 9, opening a sheet with a text field popped up the keyboard by itself.
- The date sheet opened on next month when this week ends in it, with every day disabled.
- The "Add note" buttons had no label for TalkBack.

## 0.3.0

New:

- Edit any past day: tap a day in Habits → Week or in a habit's calendar to set Done, Partial or Skip, change the amount, add a note or clear it. Future days can't be edited.
- Done cards say when: "Done at 6:52 AM".
- Skipping a habit with "Ask for a note when I skip" on opens the note right away.
- Duration habits can be set in minutes or hours ("1.5 / 2 h", steps of a quarter hour).
- The note date, reminder times and week start are picked in bottom sheets instead of dialogs.

Fixed:

- On Android 8 and 9, the keyboard jumped back to the Name field after tapping an option in New habit.

## 0.2.0

New:

- Journal filters: pick a date range or one or more habits from the two buttons at the top. Filters always open in a bottom sheet.
- Insights shows any date range (this week by default) for all habits or the ones you pick.
- Swiping a done or skipped habit the other way undoes it. The press-and-hold sheet has an Undo button too.
- A Sponsor link in About, and in the Play build a Rate on Play Store link. After a week of use Sprout asks once whether you'd like to support it.
- Refreshed look from the new design: Today without the week strip, a clearer Undo snackbar, and the habit picker in a sheet when writing a note.

Fixed:

- Widgets showed old data after changes in the app. They now update right away.
- Marking a habit done from the Today widget sometimes did nothing.
- Widgets took a long time to appear when first added. They show a placeholder while loading.
- The Streak widget was cut off at small sizes.
- Widgets used the app's theme. They now follow the phone's colors and light or dark mode.
- Sunday was squeezed in the New habit day picker on narrow phones.
- The name field took focus back after changing a toggle or the reminder time.
- Color swatches looked different in New habit and in the accent picker; they now share one style.
- The smallest text size snapped back to Default.
- On Android 15 the keyboard pushed the New habit screen under the status bar.
- Floating buttons showed up in long (scrolling) screenshots.

## 0.1.0

First release.
