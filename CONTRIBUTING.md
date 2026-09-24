# Contributing to Sprout

Thanks for helping. Bug reports, fixes, design work and translations are all welcome.

Before you start, read [`AGENTS.md`](AGENTS.md). It lists the rules every change follows, whoever writes it. [`docs/FEATURES.md`](docs/FEATURES.md) explains what each part of the app does and where its code is, and [`TODO.md`](TODO.md) lists open work.

## Set up

You need JDK 17 or later (Android Studio's bundled JDK works) and the Android SDK with API 36.

```sh
git clone https://github.com/nisabmohd/sprout-habit-tracker.git
cd sprout-habit-tracker
./gradlew :app:assemblePlayDebug
```

Debug builds fill an empty database with sample habits and two weeks of history, so every screen has something to show. Release builds never do this.

## Test

Unit tests cover scoring, streaks, reminder timing, notification text and the backup format:

```sh
./gradlew :app:testPlayDebugUnitTest
```

The smoke suite checks every feature on a connected emulator or phone. It installs a fresh debug build, runs one short check per feature and fails on touch targets smaller than 48 dp:

```sh
./gradlew :app:assemblePlayDebug
python3 tools/verify/smoke.py          # all checks
python3 tools/verify/smoke.py journal  # only the matching checks
```

For UI changes, also look at the screen in dark mode and at the largest font size:

```sh
adb shell cmd uimode night yes
adb shell settings put system font_scale 2.0
```

## Adding a feature

1. Open an issue first if the change is large or adds a dependency.
2. Put logic in `domain/` with unit tests, and UI in the matching `ui/` package.
3. Add a check to `tools/verify/smoke.py` and describe the feature in `.claude/skills/<feature>/SKILL.md` (what it does, where the code is, how to verify it, anything that tripped you up). The existing skills are good examples.
4. Update `docs/FEATURES.md` if users will notice the change.

## Pull requests

Keep each pull request to one change, explain what it does and why, and include a screenshot for anything visual. CI runs the unit tests and builds both flavors on every pull request.

## Releases

See [`docs/RELEASING.md`](docs/RELEASING.md).
