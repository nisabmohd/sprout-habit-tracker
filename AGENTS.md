# Rules for contributors and AI agents

These rules apply to every change, whether a person or an AI agent makes it. They keep Sprout small, fast and consistent. If a rule gets in your way, open an issue before breaking it.

## Hard rules

- Ask before adding any dependency. Most of the app's size comes from libraries, so the default answer is to use the platform or write the few lines yourself. The libraries already in use are listed in `gradle/libs.versions.toml`.
- Never add `material-icons-extended`. Icons are vector drawables in `app/src/main/res/drawable`, drawn with the same 24 × 24 grid and 1.9 stroke as the rest.
- No dependency injection framework (use `AppContainer`), no chart library (use Compose `Canvas`), no image library, no networking library.
- The `foss` flavor must never contain Google or Play Services code. Put anything Google-specific in the `play` source set.
- Release builds stay minified: R8 full mode, `isMinifyEnabled = true`, `isShrinkResources = true`. Check the APK size after changes that could grow it (target: under 5 MB; today it is about 2.8 MB).
- Build, test and run the app after every change, and commit each working step on its own.

## Behaviour rules

- Each habit has one outcome per scheduled day: DONE, PARTIAL (with an amount) or SKIP. A past scheduled day with no entry counts as SKIP. Today without an entry is open and counts as 0.
- Score = (DONE + PARTIAL amount ÷ target) ÷ scheduled days, leaving logged skips out of the total; a past day with no entry counts as 0.
- A streak is consecutive scheduled days that are DONE or PARTIAL. A SKIP ends it. Days the habit isn't scheduled are ignored, and today not being logged yet doesn't break it.
- Swipe right marks DONE, swipe left marks SKIP, tapping the circle toggles DONE, and press and hold opens the amount sheet. Every change shows an Undo snackbar.
- One entry is a Note (buttons say "Add note") and the tab listing them is Journal. Don't use the words "reflect" or "reflection" anywhere in the app.
- A note is one habit, one date and free text.
- Notifications are a title and one line, with no action buttons. Tapping one opens the habit.
- Each habit keeps its own fixed hue. App chrome follows dynamic color on Android 12 and later, and the green seed palette below that.

## Design and text

- Screens follow the designs in `design/` (one PNG per screen, e.g. `today.png`, `journal.png`, `icon-picker.png`). Exact colors and icon paths are in `design/TOKENS.md`.
- Gaps and sizes follow [`docs/SPACING.md`](docs/SPACING.md): 16 dp side padding, cards 8 dp apart, sections 16 dp apart, 16 dp inside a card, section labels with 16 dp above and 8 dp below, a 44 dp habit tile wherever a habit is listed.
- Charts (Insights) are drawn with Compose `Canvas` and use only the app colors: primary for done, the light primary tone for partial, outline grey for the rest. Every chart mark that can be tapped shows its numbers and has a content description.
- When a screen changes, retake its screenshot in `docs/screenshots/` (see `release/README.md` for how they are staged) in the same commit. Don't invent a new value; copy the same element on Today.
- Text uses only the nine styles in [`docs/TYPOGRAPHY.md`](docs/TYPOGRAPHY.md), available as `SproutType.*` from `ui/theme/Type.kt`. Never set a font size or weight anywhere else, and never read `MaterialTheme.typography` in a screen. Headings use Outfit; body text uses the chosen font (Lexend by default).
- Text must grow with the user's font size. Only text inside a fixed-size shape (rings, segmented buttons, the navigation bar, the week columns) may cap its scaling, using `CappedFontScale`.
- Filters, pickers and choices (dates, times, habits, amounts, week start) open in a bottom sheet, never a dialog, inline chips or menus. Use `DateRangeSheet`, `DatePickerSheet`, `TimePickerSheet` and `HabitFilterSheet` in `ui/components`. Dialogs are only for confirming something (delete, import). Every sheet is a `SproutSheet` (ui/components): it opens fully and stops at 90% of the screen, so put long content in a scrolling list with `Modifier.weight(1f, fill = false)` and keep the main button below it. Full screens are only for creating or editing (New habit, New note).
- Tab headers (`TabHeader`, plus the filter chips on Journal and Insights) sit above the list, not inside it, so they stay in place while the list scrolls.
- Widgets follow the system: wallpaper colors on Android 12+ and the system light or dark mode. Never apply the in-app theme, accent or dynamic-color setting to them.
- Every clickable element is at least 48 dp and has a label TalkBack can read. Gestures such as swipe and press and hold also get an accessibility action.
- Write UI text the way a person would say it: short, specific and plain. No marketing words, no exclamation marks.

## Code

- Pure logic goes in `domain/` with unit tests. Scoring and streak rules are easy to get subtly wrong, so change the tests first.
- UI state classes are immutable, lazy lists use stable keys, swipe state lives inside the card, and statistics run off the main thread.
- Query only what the screen shows (the visible week or month).

## Workflow

1. Read the skill for the feature you're changing in `.claude/skills/`. It explains how the feature works, where the code is and how to check it.
2. Make the change, then run the unit tests and the smoke suite (`python3 tools/verify/smoke.py`, see `CONTRIBUTING.md`).
3. For UI changes, also check dark mode and the largest font size.
4. Add a check to `tools/verify/smoke.py` for new behaviour, update or add the feature's skill, and add anything left undone to `TODO.md`.
5. Commit with a message that says what changed and why.
