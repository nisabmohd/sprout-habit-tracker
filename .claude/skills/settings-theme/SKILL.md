---
name: settings-theme
description: The More tab settings (language, theme, dynamic color, accent, font, text size, week start, default reminder) and the theme system. Use when changing settings, fonts, colors or text scaling.
---

# Settings and theme

**Code:** `data/SettingsRepository.kt` (DataStore; every preference persists), `ui/theme/` (`ThemeSettings` defaults: System theme, dynamic color on, Lexend body font, text scale 1.0; `Color.kt` seed palette from `design/TOKENS.md`; `Type.kt` type scale), `ui/more/MoreScreen.kt`, `ui/components/CappedFontScale.kt`.

**Verify:** `python3 tools/verify/smoke.py more`. Change settings, `adb shell am force-stop app.sprout.habits`, relaunch and confirm they stuck (`adb shell "run-as app.sprout.habits cat files/datastore/settings.preferences_pb" | strings`).

**Pitfalls:** the text-size slider maps to `TEXT_SCALES`; index 0 (Small) is valid, only an unknown value falls back to Default. Accent swatches show only when dynamic color is off or unsupported.

**Language and translations:** all UI text is in `res/values/strings.xml`, one `values-xx/` per language (en, hi, es, de, fr, pt-rBR, ja). Screens use `stringResource`/`pluralStringResource`; view models, notifications and widgets use `Strings` (`AppContainer.strings`, `rememberStrings()` in Compose). `AppLanguage` switches language with no AppCompat: the platform `LocaleManager` on Android 13+ (the same setting as Settings → App languages), and a saved tag applied in `attachBaseContext` of `SproutApp`, `MainActivity` and `StreakWidgetConfigActivity` below that. More → General → Language opens `LanguageSheet`; one-choice sheets use `ChoiceSheet`/`ChoiceRow` in `ui/components`.

**Verify a language:** `adb shell cmd locale set-app-locales app.sprout.habits --locales ja` (13+), relaunch and read the screens; `--locales ""` resets it. On Android 8 use the Language sheet, then force-stop and relaunch to check it sticks. Unit tests read the English `strings.xml` through `TestStrings`.

**Pitfalls:** a new language must also go into `localeFilters` in `app/build.gradle.kts`, or R8 strips it from the APK; and into `AppLanguage.LANGUAGES` to show in the sheet. Counts need `<plurals>` ("1 hecho", not "1 hechos"). Date formatters are built on each use with `datePattern()` (`ui/Dates.kt`), never cached in a `val`: English keeps the pattern as written, other languages get their own order from `DateFormat.getBestDateTimePattern`. View models outlive the activity recreate that a language change triggers, so `MainActivity` clears the `viewModelStore` when the saved language differs; otherwise text a view model formatted earlier stays in the old language. Tab labels auto-size and have their own short strings (`nav_habits`, `nav_insights`) for long languages.
