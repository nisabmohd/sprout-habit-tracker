---
name: reminders
description: Habit reminders and the evening note nudge (AlarmManager, notification permission, rescheduling). Use when changing reminders or notifications, or when a reminder doesn't fire.
---

# Reminders

**Rules:** notifications are a title and one line, no action buttons; tapping opens the habit. Reschedule on boot, time change, time-zone change and every habit edit.

**Code**
- `notify/ReminderScheduler.kt`: one alarm per habit; exact (`setExactAndAllowWhileIdle`) when "Alarms & reminders" is allowed, otherwise a 10-minute window. `start()` watches the database, so any edit, archive, delete or import reschedules. Also the 9 PM note nudge (`NOTE_NUDGE_MINUTES`).
- `notify/ReminderReceiver.kt` (fires, posts, schedules next; `RescheduleReceiver` for boot/time/zone/package replaced/exact-alarm permission change).
- `notify/ReminderNotifier.kt` (text, skips days already done or skipped; `start()` watches today's entries and removes a reminder still in the shade once its habit is done or skipped), `notify/Notifications.kt` (channels, permission checks), `domain/ReminderTime.kt` (+ `ReminderTimeTest`).

**Verify**
- `adb shell dumpsys alarm | grep -A2 action.REMINDER` shows the next times.
- To see one fire, set a habit's reminder (or temporarily `NOTE_NUDGE_MINUTES`) to the next minute, wait, then `adb shell dumpsys notification --noredact | grep -A30 pkg=app.sprout.habits`. Revert any temporary constant.
- Exact alarms: `adb shell appops set app.sprout.habits SCHEDULE_EXACT_ALARM allow`.
