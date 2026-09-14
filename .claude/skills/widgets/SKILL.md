---
name: widgets
description: The four Glance home-screen widgets (This week, Today, Today strip, Streak) and how to add and test them. Use when changing widgets or their refresh.
---

# Widgets

**Code:** `widget/` — `WidgetData.kt` (data from the repository; habits with "Show on widget" off are excluded), `WidgetBitmaps.kt` (Glance has no Canvas, so rings, marks and strips are drawn into bitmaps with the app's own drawing code), `WeekWidget`, `TodayWidget` (circle = `MarkDoneAction`), `StripWidget`, `StreakWidget` (+ `StreakWidgetConfigActivity`; with no choice it follows the longest current streak), `WidgetUpdater` (refreshes all widgets after any change), `WidgetTheme.kt`.

**Verify:** debug builds include `PinWidgetActivity`:
```sh
adb shell am start -n app.sprout.habits/.PinWidgetActivity --es widget today   # week | today | strip | streak
```
then tap "Add to home screen", screenshot the home screen, tap a circle in the Today widget and check every widget updates (also after `am force-stop`).

**Pitfalls:** an action can start the process, so `MarkDoneAction` refreshes widgets itself; the launcher doesn't run the configure step for pinned widgets.
