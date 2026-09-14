---
name: settings-theme
description: The More tab settings (theme, dynamic color, accent, font, text size, week start, default reminder) and the theme system. Use when changing settings, fonts, colors or text scaling.
---

# Settings and theme

**Code:** `data/SettingsRepository.kt` (DataStore; every preference persists), `ui/theme/` (`ThemeSettings` defaults: System theme, dynamic color on, Lexend body font, text scale 1.0; `Color.kt` seed palette from `design/TOKENS.md`; `Type.kt` type scale), `ui/more/MoreScreen.kt`, `ui/components/CappedFontScale.kt`.

**Verify:** `python3 tools/verify/smoke.py more`. Change settings, `adb shell am force-stop app.sprout.habits`, relaunch and confirm they stuck (`adb shell "run-as app.sprout.habits cat files/datastore/settings.preferences_pb" | strings`).

**Pitfalls:** the text-size slider maps to `TEXT_SCALES`; index 0 (Small) is valid, only an unknown value falls back to Default. Accent swatches show only when dynamic color is off or unsupported.
