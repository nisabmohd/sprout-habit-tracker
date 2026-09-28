# Sprout

Sprout is a small, open-source habit tracker for Android. It keeps your habits and notes on your phone, works without an account, and the APK is under 3 MB.

| Today | Log an amount | Week | Overall |
| --- | --- | --- | --- |
| ![Today](docs/screenshots/02-today.png) | ![Log an amount](docs/screenshots/03-log-amount.png) | ![Week view](docs/screenshots/04-habits-week.png) | ![Overall heatmap](docs/screenshots/05-habits-overall.png) |
| **Habit detail** | **Journal** | **Insights** | **Dark mode** |
| ![Habit detail](docs/screenshots/06-habit-detail.png) | ![Journal](docs/screenshots/07-journal.png) | ![Insights](docs/screenshots/09-insights.png) | ![Dark mode](docs/screenshots/13-today-dark.png) |

More: [welcome](docs/screenshots/01-welcome.png), [filter by habit](docs/screenshots/08-filter-by-habit.png), [date range](docs/screenshots/10-date-range.png), [new habit](docs/screenshots/12-new-habit.png), [settings](docs/screenshots/11-more.png).

### Widgets

| Today | This week | Today strip | Streak |
| --- | --- | --- | --- |
| ![Today widget](docs/screenshots/14-widget-today.png) | ![This week widget](docs/screenshots/15-widget-week.png) | ![Today strip widget](docs/screenshots/16-widget-strip.png) | ![Streak widget](docs/screenshots/17-widget-streak.png) |

## What it does

Each day, every habit is done, partly done (with an amount such as 15 of 20 pages) or skipped. Swipe right to mark a habit done, swipe left to skip it, tap its circle to toggle it, or press and hold to log an amount and add a note. Swiping the other way undoes a swipe. Days you skip are left out of your score, so one bad day doesn't sink the week.

You can also:

- see each week at a glance, or the last 26 weeks as a heatmap, with scores and streaks
- keep a Journal of short notes tied to a habit and a day, filtered by habit or by dates
- look back at any date range in Insights, for all habits or a few
- get a reminder at the time you pick, and a nudge in the evening to add a note when you skip a habit that asks for one
- add four home-screen widgets that stay in sync with the app; the Today widget marks habits done from the home screen
- export everything, including your settings, to a JSON file and import it again, or export a CSV for spreadsheets
- choose light or dark, dynamic color from your wallpaper or an accent color, one of five fonts and a text size
- use it in English, Hindi, Spanish, German, French, Portuguese (Brazil) or Japanese, separately from the phone's language

[`docs/FEATURES.md`](docs/FEATURES.md) describes each feature in detail.

## Download

Signed APKs are attached to each [GitHub release](https://github.com/nisabmohd/sprout-habit-tracker/releases). There are two builds:

- `foss` has no Google or Play Services code and backs up to files only. This is the one for F-Droid.
- `play` will add Google Drive backup (not built yet, see [`TODO.md`](TODO.md)).

Sprout runs on Android 8.0 and later.

## Privacy

Sprout has no ads, no analytics and no trackers. It asks for two permissions: notifications, for reminders, and "Alarms & reminders" if you want them to arrive on the minute. Your data only leaves the phone when you export it.

## Build

You need JDK 17 or later and the Android SDK (API 36).

```sh
./gradlew :app:assembleFossDebug
./gradlew :app:assemblePlayRelease
```

Kotlin, Jetpack Compose and Material 3, with Room, DataStore, Navigation Compose and Glance. Charts are drawn with Compose `Canvas`. No dependency injection, chart or image libraries.

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for setup and testing, and [`AGENTS.md`](AGENTS.md) for the rules every change follows. Open tasks are in [`TODO.md`](TODO.md).

## Support

Sprout is free and has no ads. I build it because I enjoy it. If it helps you, you can [sponsor me on GitHub](https://github.com/sponsors/nisabmohd) or star the repo.

## License

Sprout is licensed under the [GNU General Public License v3.0](LICENSE). The bundled fonts (Figtree, Outfit, Lexend and Atkinson Hyperlegible Next) use the SIL Open Font License 1.1; see [`licenses/fonts`](licenses/fonts).
