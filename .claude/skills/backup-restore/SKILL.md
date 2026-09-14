---
name: backup-restore
description: JSON export/import (habits, entries, notes and preferences) and CSV export through the system file picker. Use when changing the backup format, import or export.
---

# Backup and restore

**Code:** `data/backup/Backup.kt` — `BackupFile` (versioned DTOs, separate from Room entities; `settings` is optional so older files still import), `BackupManager.parse` (validation, clear errors), `restore` (one transaction, then preferences), `csv`. UI: `ui/more/BackupSection.kt` (`CreateDocument` / `OpenDocument`, no storage permission). Unit tests: `BackupFormatTest`.

**Verify:** More → Export to file → JSON backup → Save; read it with `adb exec-out cat /sdcard/Download/<file>`; change data or settings; More → Import from file → Replace; confirm everything is back. A non-backup JSON must show "This isn't a Sprout backup file." and change nothing.

**Pitfalls:** bump `FORMAT_VERSION` only for incompatible changes; add new fields with defaults. Check under R8 with a release build (kotlinx.serialization).
