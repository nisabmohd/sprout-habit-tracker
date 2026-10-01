# Play Store release assets

Graphics and listing text for the Play Store.

## Graphics

| Asset | Play requirement | File | Status |
| --- | --- | --- | --- |
| App icon | 512 × 512 PNG, 32-bit, up to 1 MB. Full square, Play applies the rounded mask | `icon-512.png` | Ready |
| Feature graphic | 1024 × 500 JPEG or 24-bit PNG, no transparency | `feature-graphic-1024x500.png` | Ready |
| Phone screenshots | 2–8 images, PNG or JPEG, 16:9 or 9:16, each side 320–3840 px, long side at most 2× the short side. At least 4 at 1080 px or wider to be eligible for promotion | `phone-screenshots/01–08` (1080 × 1920) | Ready |
| 7-inch tablet screenshots | Optional (needed only to show as tablet-ready) | – | Not made |
| 10-inch tablet screenshots | Optional | – | Not made |
| Promo video | Optional YouTube link | – | Not made |

The raw emulator screenshots are 1080 × 2400 (20:9). Play rejects those because the long side is more than twice the short side, so the store shots put each screenshot in a 9:16 frame with a caption.

How they were taken: Pixel-size emulator (API 36), gesture navigation, status bar demo mode (9:30, full battery, Wi-Fi only), date set to Sunday 27 September 2026, fresh install with the sample habits, Dynamic color off, brown accent, Lexend font. Widgets follow the system colours, so they use the wallpaper's colours rather than the in-app accent.

| # | Screen | Caption |
| --- | --- | --- |
| 1 | Today | Swipe to check off your day |
| 2 | Amount sheet | Log part of a habit |
| 3 | Habits → Week | Your week at a glance |
| 4 | Habit detail | Streaks and a calendar |
| 5 | Journal | A note for each day |
| 6 | Insights | See what's working |
| 7 | Home screen widgets | Widgets for your home screen |
| 8 | Today, dark | Light or dark, your colors |

## Listing text (draft)

**App name** (30 chars max): `Sprout: Habit Tracker`

**Short description** (80 chars max):

> Track habits, log part of a day, and keep a journal. Free, open source, no ads.

**Full description** (4000 chars max):

> Sprout is a small habit tracker that keeps your habits and notes on your phone.
>
> Each day, every habit is Done, Partial or Skipped. Swipe right to mark it done, swipe left to skip it, or press and hold to log an amount, like 15 of 20 pages or 30 of 45 minutes. Every change can be undone.
>
> • Today: your score for the day and a card for each habit
> • Habits: the week at a glance, or the last 26 weeks as a heatmap, with streaks
> • Habit detail: a month calendar where you can fix any past day
> • Journal: short notes tied to a habit and a day, filtered by habit or date
> • Insights: your score, habits per day and each habit's score for any date range
> • Reminders at the time you choose, and an evening nudge to add a note when you skip
> • Four home-screen widgets: This week, Today, Today strip and Streak
> • Export and import a JSON backup, or export CSV for spreadsheets
> • Light and dark theme, dynamic color or an accent color, five fonts and a text size
> • English, Hindi, Spanish, German, French, Portuguese (Brazil) and Japanese
>
> Skipped days are left out of your score, so one bad day doesn't sink the week.
>
> Sprout is free and open source under the GNU GPL v3. No ads, no tracking, and it works without an account.

**Category:** Productivity. **Tags:** Habit tracker, Journal.

## Play Console

The privacy policy is [`PRIVACY.md`](../PRIVACY.md) at the repo root.
