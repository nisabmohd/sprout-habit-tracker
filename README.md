# Sprout

An open-source, local-first habit tracker for Android. Small (under 5 MB), fast, and built with Kotlin, Jetpack Compose and Material 3.

- Track each habit per day as **Done**, **Partial** (with an amount) or **Skip**
- Swipe right to complete, swipe left to skip, press and hold to log an amount
- Week view, overall heatmap, streaks and scores
- A **Journal** of short notes, one per habit per day
- Reminders, home-screen widgets, JSON backup and restore
- Dynamic color on Android 12+, light and dark themes, a choice of bundled fonts

Status: early development. See [`docs/PLAN.md`](docs/PLAN.md) for the build plan and [`design/`](design/) for the screen designs.

## Build

Requirements: JDK 17+ and the Android SDK (compileSdk 36).

```sh
./gradlew assembleFossDebug     # no Google code, for F-Droid
./gradlew assemblePlayDebug     # adds Google sign-in and Drive backup
./gradlew assemblePlayRelease   # R8-minified release build
```

### Flavors

| Flavor | What it includes |
| --- | --- |
| `foss` | No Google or Play Services code. Backup is file export/import only. |
| `play` | Adds Google sign-in (Credential Manager) and backup to Drive's hidden app folder. |

## License

Sprout is licensed under the [GNU General Public License v3.0](LICENSE).

The bundled fonts (Figtree, Outfit, Lexend, Atkinson Hyperlegible Next) are licensed under the SIL Open Font License 1.1; see [`licenses/fonts`](licenses/fonts).
