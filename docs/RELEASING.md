# Releasing

Releases are built by GitHub Actions (`.github/workflows/release.yml`) when a tag starting with `v` is pushed. The workflow runs the unit tests, builds signed `github` and `play` APKs, and attaches them to a GitHub release with their SHA-256 checksums.

## One-time setup

1. Create an upload keystore (keep it and its passwords somewhere safe; losing it means users can't update):

   ```sh
   keytool -genkeypair -v -keystore sprout-release.jks -alias sprout \
     -keyalg RSA -keysize 4096 -validity 10000
   ```

2. Add four repository secrets under **Settings → Secrets and variables → Actions**:

   | Secret | Value |
   | --- | --- |
   | `SPROUT_KEYSTORE_BASE64` | `base64 -i sprout-release.jks` (one line) |
   | `SPROUT_KEYSTORE_PASSWORD` | the keystore password |
   | `SPROUT_KEY_ALIAS` | `sprout` (or the alias you chose) |
   | `SPROUT_KEY_PASSWORD` | the key password |

   Never commit the keystore; `*.jks` and `*.keystore` are in `.gitignore`.

## Each release

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts` and commit.
2. Tag and push:

   ```sh
   git tag v1.0.0
   git push origin v1.0.0
   ```

3. The release appears on GitHub with `sprout-v1.0.0-github.apk`, `sprout-v1.0.0-play.apk` and `SHA256SUMS.txt`. Its notes are that version's section of `CHANGELOG.md`, so rename `## Unreleased` to `## 1.0.0` before tagging.

The app's update check reads the latest release: its tag is the version, its `- ` lines are the What's new list (first five), it downloads the asset ending in `-github.apk`, and it checks it against `SHA256SUMS.txt`. Uploading a release by hand? Attach the `github` APK (`./gradlew :app:assembleGithubRelease`) and `SHA256SUMS.txt` with the same names, or installs from GitHub stop getting update checks.

## Signing locally

The maintainer's key lives in `keystore/` at the repo root (`sprout-release.jks` and `keystore.properties` with its alias and passwords). The folder is gitignored; keep a backup of both files outside the repo. When `keystore/keystore.properties` exists, `./gradlew :app:assembleFossRelease` signs with it automatically.


The build reads the same values from environment variables, so a local signed build is:

```sh
SPROUT_KEYSTORE_PATH=/path/to/sprout-release.jks \
SPROUT_KEYSTORE_PASSWORD=... SPROUT_KEY_ALIAS=sprout SPROUT_KEY_PASSWORD=... \
./gradlew :app:assembleFossRelease :app:assemblePlayRelease
```

Without these variables, release builds are signed with the debug key so they can still be installed for testing.
