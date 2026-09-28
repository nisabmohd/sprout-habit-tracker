---
name: app-updates
description: The GitHub build's update check (Check for updates on About, daily auto check, Update dialog, in-app download and install) and the github flavor. Use when changing updates, releases or the github/foss/play flavors.
---

# App updates

**What:** only the `github` flavor (`BuildConfig.GITHUB_UPDATES`) checks `api.github.com/repos/<repo>/releases/latest`, compares `tag_name` with `VERSION_NAME` (`domain/Versions.kt`, unit-tested), and shows `UpdateDialog` with up to five `- ` lines from the release body. Update downloads the `-github.apk` asset into `cacheDir/updates`, checks it against `SHA256SUMS.txt` if the release has one, and opens the installer through the FileProvider `${applicationId}.updates`. `foss` (F-Droid) has no INTERNET permission and R8 strips the checker; `play` says updates come from Google Play.

**Code:** `data/update/UpdateManager.kt`, `ui/update/UpdateUi.kt` (dialog, About pill, `UpdatePrompt`, `UpdateBadge`), `support/LaunchPrompts.kt` (one prompt per launch), `src/github/AndroidManifest.xml` + `res/xml/update_paths.xml`. Settings: `availableUpdateVersion`, `lastUpdateCheckAt`, `dismissedUpdateVersion`.

**Verify:** build a copy that thinks it's older: set `versionName = "0.9.0"`, `./gradlew :app:assembleGithubRelease`, copy the APK somewhere, set the version back. Install it (release-signed, so the real release installs over it), open it: the daily check shows the dialog; Update downloads with progress, Install asks once for "Install unknown apps", then Android's "Update this app?". Check `dumpsys package app.sprout.habits | grep versionName` afterwards. Offline: About → Check for updates must end in "Couldn't check. Try again" after 10 s. Emulator without internet? Cold boot with `-dns-server 8.8.8.8`.

**Pitfalls:** blocking network calls (DNS) ignore coroutine cancellation, so the request runs in its own scope and `withTimeoutOrNull` only awaits it. The install permission can change while the dialog is open; it's re-read on resume. A release that ships only the `foss` APK strands GitHub users without update checks.
