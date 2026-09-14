---
name: habit-editing
description: Creating, editing, reordering, archiving and deleting habits, and how to verify them. Use when changing the habit editor or Manage habits.
---

# Habit editing

**Code**
- `ui/edit/EditHabitScreen.kt`, `EditHabitViewModel.kt` (route `EditHabitRoute(habitId)`, 0 = new). Saves keep fields the form doesn't show.
- `ui/manage/ManageHabitsScreen.kt`: drag to reorder (with Move up/down accessibility actions), archive/restore, delete with confirmation (`DeleteHabitDialog`).
- Data: `data/Entities.kt` (`Habit`, `daysMask` Monday = bit 0), `HabitRepository.saveHabit/reorderHabits/setArchived/deleteHabit` (delete cascades to entries and notes).

**Verify:** `python3 tools/verify/smoke.py new_habit`. Manually: edit a habit, change nothing, save, and confirm its days are unchanged (pull the DB: `adb exec-out run-as app.sprout.habits cat databases/sprout.db > /tmp/s.db` plus the `-wal`/`-shm` files, then `sqlite3`).

**Pitfalls**
- Day toggles are 44 dp circles inside 48 dp touch boxes; the day name is the TalkBack label, the single letter is hidden.
- One reminder per habit (`reminderMinutes`); turning it on asks for the notification permission.
