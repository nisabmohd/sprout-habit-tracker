# Features

What Sprout does today, and where each part lives in the code. Paths are under `app/src/main/java/app/sprout/habits/`.

## Today

The home screen shows the date, a score card for today, and every habit scheduled for today.

A habit card has four looks: open (an empty circle), done (filled with the habit's color and a check), partial (filled as far as the amount goes, with a percentage ring) and skipped (dimmed). Swipe right to mark it done, swipe left to skip it, tap the circle to toggle done, and press and hold to log an amount, pick an outcome and add a note. Each change can be undone from the snackbar. Swiping the opposite way also undoes: swipe left on a done habit or right on a skipped one to clear it. The press-and-hold sheet has an Undo button when the day already has an entry.

Code: `ui/today/`, scoring in `domain/Scoring.kt`.

## Habits

Habits are created and edited on one screen: name, icon, color, how it's tracked (check off, an amount with a unit, or minutes), the days it's scheduled, a reminder, whether to ask for a note after a skip, and whether it appears on widgets. Manage habits lets you drag to reorder, archive, restore and delete.

The Habits tab has a Week view (seven day marks per habit, with arrows to earlier weeks) and an Overall view (a 26-week heatmap per habit with its score and best streak). Tapping a habit opens its detail screen with this month's score, best streak, note count, a month calendar and its notes.

Code: `ui/edit/`, `ui/manage/`, `ui/habits/`, `ui/detail/`, statistics in `domain/Stats.kt`.

## Journal and notes

A note belongs to one habit and one day. The Journal tab lists every note, newest first. The two buttons at the top open bottom sheets to filter by date range and by one or more habits. Notes can be added from Today, the Journal, a habit's detail screen or the press-and-hold sheet, and edited or deleted later.

Code: `ui/journal/`, `ui/note/`.

## Insights

A summary for any date range (this week by default), for all habits or the ones you pick. The range and habits are chosen in bottom sheets from the buttons at the top. It shows the average score, how many days were done or partial, the best weekday, habits completed per day, and each habit's score.

Code: `ui/insights/`.

## Reminders

Each habit can have one daily reminder on its scheduled days. The notification shows what's left for today and opens the habit when tapped. Reminders are exact when the user allows "Alarms & reminders", and otherwise arrive within 10 minutes. At 9 PM, habits that were skipped and ask for a note get a short prompt to add one.

Code: `notify/`, timing in `domain/ReminderTime.kt`.

## Widgets

Four home-screen widgets: This week (every habit with its week), Today (a progress ring and the next habits, with a circle to mark each done), Today strip (a slim bar per habit), and Streak (one habit's current streak and last seven days). They follow the phone's colors (wallpaper colors on Android 12+) and its light or dark mode, whatever the app's own theme is set to. They update after every change made in the app or on a widget, and show a light placeholder while loading instead of a blank box.

| Today | This week | Today strip | Streak |
| --- | --- | --- | --- |
| ![](screenshots/14-widget-today.png) | ![](screenshots/15-widget-week.png) | ![](screenshots/16-widget-strip.png) | ![](screenshots/17-widget-streak.png) |

Code: `widget/`.

## Backup

Everything, including preferences, can be exported to a JSON file and imported again through the system file picker. The app needs no storage permission. A CSV export opens in any spreadsheet.

Code: `data/backup/`, `ui/more/BackupSection.kt`.

## Settings

The More tab covers backup, theme (system, light, dark), dynamic color or an accent color, font (system, Figtree, Outfit, Lexend, Atkinson Hyperlegible), text size, the first day of the week, the default reminder time, notification settings and About.

Code: `ui/more/`, `data/SettingsRepository.kt`, `ui/theme/`.

## Support

About has a Sponsor link and, in the `play` build, a Rate on Play Store link. After a week of use and 30 check-ins, Sprout asks once whether you'd like to support it; it asks again after 60 days at most, and stops after two "Maybe later"s.

Code: `support/`, `ui/about/`.
