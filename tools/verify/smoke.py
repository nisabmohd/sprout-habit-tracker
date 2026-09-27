#!/usr/bin/env python3
"""End-to-end smoke check of every Sprout feature on a connected emulator or phone.

Installs a fresh debug build (which seeds the sample habits from DevData), then runs one short
check per feature and prints PASS/FAIL. Exit code is the number of failures.

    ./gradlew :app:assemblePlayDebug
    python3 tools/verify/smoke.py            # all checks
    python3 tools/verify/smoke.py today notes  # only checks whose name contains these words

Checks use visible text and TalkBack labels, so they also catch missing labels.
"""
import os
import re
import sys
import time
import traceback

sys.path.insert(0, os.path.dirname(__file__))
import device as d  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
APK = os.path.join(ROOT, "app/build/outputs/apk/play/debug/app-play-debug.apk")

CHECKS = []


def check(fn):
    CHECKS.append(fn)
    return fn


def fresh_install():
    d.adb("uninstall", d.PACKAGE)
    out = d.adb("install", APK)
    assert "Success" in out, f"install failed: {out}"
    d.shell(f"pm grant {d.PACKAGE} android.permission.POST_NOTIFICATIONS")
    d.shell(f"am start -W -n {d.PACKAGE}/.MainActivity")
    time.sleep(3)


# --- checks, in the order a new user meets the features -----------------------------------


@check
def welcome_shows_once():
    assert d.exists("Get started"), "welcome screen missing on first launch"
    d.tap("Get started")
    assert d.exists("Today"), "Today not shown after Get started"
    d.shell(f"am force-stop {d.PACKAGE}")
    d.shell(f"am start -W -n {d.PACKAGE}/.MainActivity")
    time.sleep(2.5)
    assert not d.exists("Get started", timeout=1.5), "welcome shown again after restart"


@check
def today_shows_score_and_habits():
    assert d.exists("of today", timeout=5), "score card missing"
    assert d.exists("Wake up at 7"), "seeded habit missing"
    assert d.exists("done", contains=True), "summary line missing"


@check
def today_circle_toggles_done():
    assert d.scroll_to("Mark Vitamins done"), "Vitamins not on Today"
    d.tap("Mark Vitamins done")
    assert d.exists("Mark Vitamins not done"), "circle did not mark done"
    d.tap("Mark Vitamins not done")
    assert d.exists("Mark Vitamins done"), "circle did not unmark"


@check
def today_swipe_skip_and_undo():
    n = d.scroll_to("Workout")
    assert n, "Workout not on Today"
    w, _ = d.screen_size()
    y = n.center[1]
    d.swipe(int(w * 0.85), y, int(w * 0.12), y, 250)
    # One dump for both: the snackbar only stays about 4 seconds.
    nodes = d.dump()
    assert any(n.label == "Workout skipped" for n in nodes), "no Undo snackbar after swipe left"
    undo = next(n for n in nodes if n.label == "Undo")
    d.tap_xy(*undo.center)
    assert d.exists("Mark Workout done"), "undo did not restore Workout"


@check
def today_opposite_swipe_undoes():
    w, _ = d.screen_size()
    def swipe(label, right):
        n = d.scroll_to(label)
        assert n, f"{label} not on Today"
        y = n.center[1]
        a, b = (int(w * 0.12), int(w * 0.88)) if right else (int(w * 0.88), int(w * 0.12))
        d.swipe(a, y, b, y, 250)
        time.sleep(1)
    # Done card: swipe left undoes instead of skipping.
    swipe("Wake up at 7", right=False)
    assert d.exists("Mark Wake up at 7 done"), "swipe left on a done card didn't undo it"
    swipe("Wake up at 7", right=True)
    assert d.exists("Mark Wake up at 7 not done"), "swipe right didn't mark it done again"
    # Skipped card: swipe right undoes instead of marking done.
    swipe("Workout", right=False)
    assert d.exists("Skipped", timeout=2), "swipe left didn't skip Workout"
    swipe("Workout", right=True)
    assert d.exists("Mark Workout done"), "swipe right on a skipped card didn't undo it"


@check
def today_long_press_sheet():
    d.nav("Today")
    d.long_press("Read")
    assert d.exists("Goal 20 pages", contains=True), "amount sheet missing its goal"
    assert d.exists("Partial"), "outcome buttons missing"
    d.tap("Cancel")


@check
def new_habit_is_created():
    d.nav("Today")
    d.scroll_to_top()
    d.tap("New habit")
    d.tap("Name")
    d.type_text("Smoke test habit")
    d.back()  # hide the keyboard
    d.tap("Save")
    time.sleep(1)
    assert d.scroll_to("Smoke test habit"), "new habit not on Today"


@check
def habits_week_and_overall():
    d.nav("Habits")
    assert d.exists("complete this week", contains=True), "week view missing"
    d.tap("Overall")
    assert d.exists("Last 26 weeks"), "overall heatmap missing"
    d.tap("Week")


@check
def habit_detail_opens():
    d.tap("Read", timeout=5)
    assert d.exists("best streak"), "detail stats missing"
    assert d.exists("Edit habit"), "edit action missing"
    d.back()


@check
def journal_lists_and_adds_notes():
    d.nav("Journal")
    assert d.exists("Rough morning", contains=True), "seeded note missing"
    d.tap("Add note")
    d.type_text("Smoke test note")
    d.tap("Save")
    assert d.exists("Smoke test note", timeout=4), "new note not in Journal"


@check
def journal_filter_by_habit():
    d.nav("Journal")
    d.tap("Filter by habit")
    assert d.exists("Show all notes"), "filter sheet didn't open"
    d.tap("Stay calm")
    d.tap("Show notes for 1 habit")
    time.sleep(1)
    assert d.exists("Rough morning", contains=True), "Stay calm's note missing when filtered"
    assert not d.exists("Smoke test note", timeout=1), "another habit's note shown under the filter"
    d.tap("Filter by habit")
    d.tap("Clear")
    d.tap("Vitamins")
    d.tap("Show notes for 1 habit")
    time.sleep(1)
    assert d.exists("No notes for this habit yet"), "empty filter message missing"
    d.tap("Show all notes")
    assert d.exists("Rough morning", contains=True), "filter didn't reset"


@check
def journal_filter_by_date():
    import datetime
    d.nav("Journal")
    d.tap("Filter by date")
    assert d.exists("All dates"), "date sheet didn't open with the All dates shortcut"
    today = datetime.date.today()
    d.tap(str(today.day))
    d.tap("Show", contains=True)
    time.sleep(1)
    assert d.exists("Rough morning", contains=True), "today's note missing under a today-only range"
    assert d.exists(f"{today.day} ", contains=True), "range label missing above the title"
    d.tap("Filter by date")
    d.tap("All dates")
    assert d.exists("Journal"), "Journal missing after clearing dates"


@check
def insights_render():
    d.nav("Insights")
    assert d.exists("Average score"), "score card missing"
    assert d.exists("Habits per day", contains=True), "bar chart missing"
    d.tap("Change date range")
    assert d.exists("Date range"), "date range sheet didn't open"
    d.tap("1")  # the 1st of this month starts a new range
    d.tap("Show 1", contains=True)
    time.sleep(1)
    assert d.exists("Average score"), "insights missing after picking a range"
    d.tap("Filter by habit")
    d.tap("Read")
    d.tap("Show 1 habit")
    assert d.exists("across 1 habit", contains=True), "habit filter not applied"
    d.tap("Change date range")
    d.tap("This week")


@check
def more_settings_and_about():
    d.nav("More")
    assert d.exists("Export to file"), "backup rows missing"
    assert d.exists("Theme"), "appearance section missing"
    d.tap("Dark")
    assert d.exists("Dark"), "theme option missing"
    d.tap("System")
    w, h = d.screen_size()
    for _ in range(3):
        d.swipe(w // 2, int(h * 0.8), w // 2, int(h * 0.3))
    d.tap("About Sprout")
    assert d.exists("Open-source licences"), "About screen incomplete"
    d.back()


@check
def reminders_are_scheduled():
    alarms = d.shell("dumpsys alarm")
    assert "app.sprout.habits.action.REMINDER" in alarms, "no habit reminder alarms scheduled"


@check
def touch_targets_48dp():
    problems = []
    for tab in ["Today", "Habits", "Journal", "Insights", "More"]:
        d.nav(tab)
        problems += [f"{tab}: {p}" for p in d.small_targets()]
    assert not problems, "small touch targets: " + "; ".join(problems)


def main(filters):
    selected = [c for c in CHECKS if not filters or any(f in c.__name__ for f in filters)]
    if not os.path.exists(APK):
        sys.exit(f"Build first: ./gradlew :app:assemblePlayDebug (missing {APK})")
    fresh_install()
    if welcome_shows_once not in selected and d.exists("Get started", timeout=3):
        d.tap("Get started")
    failures = 0
    for c in selected:
        try:
            c()
            print(f"PASS  {c.__name__}")
        except Exception as e:  # noqa: BLE001 - report and continue
            failures += 1
            print(f"FAIL  {c.__name__}: {e}")
            if os.environ.get("VERBOSE"):
                traceback.print_exc()
            d.shell(f"am start -W -n {d.PACKAGE}/.MainActivity")
    print(f"\n{len(selected) - failures}/{len(selected)} passed")
    return failures


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
