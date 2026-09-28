# Changelog

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
