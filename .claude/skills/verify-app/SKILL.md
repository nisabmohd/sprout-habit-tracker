---
name: verify-app
description: Build Sprout and check every feature on the emulator with the smoke suite. Use after any change, before committing, or when asked to verify, test or check the app.
---

# Verify the app

1. Build with Android Studio's JDK (system JDK 25 breaks AGP):
   ```sh
   JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testPlayDebugUnitTest :app:assemblePlayDebug
   ```
2. Make sure an emulator or phone is connected (`adb devices`). If none: `~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36.1` in the background, then `adb wait-for-device`.
3. Run the smoke suite. It uninstalls, installs a fresh debug build (sample habits from `data/SampleData.kt`, added on first launch), and runs one check per feature:
   ```sh
   python3 tools/verify/smoke.py            # everything
   python3 tools/verify/smoke.py today      # only checks whose name contains "today"
   VERBOSE=1 python3 tools/verify/smoke.py  # stack traces on failure
   ```
4. For a failure, look at the screen before guessing: `adb exec-out screencap -p > /tmp/s.png` and read it, or list labels with `python3 -c "import sys; sys.path.insert(0,'tools/verify'); import device as d; print([n.label for n in d.dump() if n.label])"`.
5. For UI changes also check dark mode (`adb shell cmd uimode night yes`) and the largest text (`adb shell settings put system font_scale 2.0`, plus More → Text size → Largest). Reset both afterwards.

## Pitfalls
- Snackbars last about 4 s and a UI dump takes 1–2 s: read the snackbar and tap Undo from one `dump()`.
- New items appear at the end of lists; use `d.scroll_to(label)`, not `d.exists`.
- Items clipped by a list edge look "small"; `small_targets()` already skips them.
- Scripted taps by coordinates break when the layout shifts (e.g. a switch reveals a row). Tap by label.
- For anything time-based (alarms), schedule it for the next minute rather than waiting.
