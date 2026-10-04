#!/usr/bin/env python3
"""End-to-end smoke check of every Sprout feature on a connected emulator or phone.

Installs a fresh debug build (which seeds the sample habits from SampleData on first launch), then runs one short
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
def sample_data_on_first_launch():
    # A fresh install starts with sample habits, a card to remove them, and notes on several days.
    d.nav("Today")
    d.scroll_to_top()
    assert d.exists("These are sample habits"), "sample data card missing on Today"
    d.nav("Journal")
    assert d.exists("Yesterday"), "sample notes don't cover earlier days"


@check
def today_circle_toggles_done():
    d.nav("Today")  # the check before this one ends on Journal
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
    d.swipe(int(w * 0.62), y, int(w * 0.02), y, 250)
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
        d.scroll_to_top()
        n = d.scroll_to(label)
        assert n, f"{label} not on Today"
        y = n.center[1]
        # Left swipes start mid-card: on short screens the Add note button covers the card's right end.
        a, b = (int(w * 0.12), int(w * 0.88)) if right else (int(w * 0.62), int(w * 0.02))
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
    d.scroll_to_top()  # the swipe checks before this one leave a short screen scrolled past Read
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
def measure_with_step():
    # Measure habit in hours with a quarter-hour step: − / + and the slider move by the step.
    d.nav("Today")
    d.scroll_to_top()
    d.tap("New habit")
    d.tap("Name")
    d.type_text("Deep work")
    d.back()
    d.tap("Measure")
    d.tap_xy(*d.scroll_to("Target").center)
    d.type_text("1.5")
    d.back()
    d.tap("Unit")
    d.type_text("h")
    d.back()
    d.tap_xy(*d.find("Step").center)
    d.shell("input keyevent KEYCODE_MOVE_END")
    d.shell("input keyevent KEYCODE_DEL")
    d.type_text("0.25")
    d.back()
    d.tap("Save")
    time.sleep(1)
    n = d.scroll_to("Deep work")
    assert n and d.exists("1.5 h"), "measure habit doesn't show its goal"
    d.long_press("Deep work")
    assert d.exists("Goal 1.5 h · today"), "sheet header missing the goal"
    d.tap("More")
    d.tap("More")
    assert d.exists("0.5", contains=True), "stepper doesn't move by the habit's step"
    d.tap("Save")
    assert d.exists("0.5 / 1.5 h"), "partial amount not shown on the card"


@check
def today_done_shows_time():
    d.nav("Today")
    d.scroll_to_top()
    # Earlier checks toggle Wake up at 7, so its time is whenever that ran.
    assert d.exists("Done at ", contains=True), "done card doesn't say when it was logged"
    n = d.scroll_to("Mark Workout done")  # below the fold on a small screen
    assert n, "Workout not on Today"
    d.tap_xy(*n.center)
    assert d.exists("45 / 45 min"), "Workout not done"
    d.tap("Mark Workout not done")


@check
def skip_opens_note_when_asked():
    import datetime
    d.nav("Today")
    d.tap_xy(*d.scroll_to("Vitamins").center)
    d.tap("Edit habit")
    d.scroll_to("Ask for a note when I skip")
    d.tap("Ask for a note when I skip")
    d.tap("Save")
    time.sleep(1)
    d.back()
    n = d.scroll_to("Vitamins")
    w, _ = d.screen_size()
    d.swipe(int(w * 0.62), n.center[1], int(w * 0.02), n.center[1], 250)
    assert d.exists("Add a note", timeout=4), "skipping a habit that asks for a note didn't open the note sheet"
    d.type_text("Left them at home")
    d.tap("Save")
    assert d.exists("Skipped · note added", contains=True), "note from the skip sheet wasn't saved"


@check
def edit_past_day_from_week():
    import datetime
    day = datetime.date.today() - datetime.timedelta(days=1)
    name = day.strftime("%A %-d %B")
    d.nav("Habits")
    d.tap("Week")
    n = d.find("Read, " + name, contains=True)
    if n is None:  # yesterday was last week (today is the first day of the week)
        d.tap("Previous week")
        n = d.find("Read, " + name, contains=True)
    assert n, "no day mark for yesterday"
    before = n.label
    target, want, message = ("Done", "done", "Read marked done · ") if before.endswith("skipped") else ("Skip", "skipped", "Read skipped · ")
    d.tap_xy(*n.center)
    assert d.exists(day.strftime("%A, %-d %b") + " · goal 20 pages"), "edit sheet header missing the date"
    d.tap(target)
    d.tap("Save")
    nodes = d.dump()
    assert any(x.label.startswith(message) for x in nodes), "no Undo snackbar after editing a past day"
    assert any(x.label == f"Read, {name}: {want}" for x in nodes), "day mark didn't change"
    d.tap_xy(*next(x for x in nodes if x.label == "Undo").center)
    assert d.exists(before), "undo didn't restore the past day"


@check
def edit_past_day_from_detail_calendar():
    import datetime
    day = datetime.date.today() - datetime.timedelta(days=1)
    name = day.strftime("%A %-d %B")
    d.nav("Habits")
    d.tap("Week")
    d.tap("Wake up at 7")
    assert d.exists("best streak"), "habit detail didn't open"
    time.sleep(0.8)  # let the screen transition finish before tapping a day
    if day.month != datetime.date.today().month:
        d.tap("Previous month")
    n = d.find(name + ":", contains=True)
    assert n, "calendar day missing"
    was = n.label.split(": ")[1]
    target, want = ("Done", "done") if was == "skipped" else ("Skip", "skipped")
    d.tap_xy(*n.center)
    assert d.exists("Save"), "calendar day didn't open the log sheet"
    d.tap(target)
    d.tap("Save")
    time.sleep(0.5)
    nodes = d.dump()  # one dump: the snackbar only stays about 4 seconds
    try:
        assert any(x.label == f"{name}: {want}" for x in nodes), "calendar day didn't change"
        d.tap_xy(*next(x for x in nodes if x.label == "Undo").center)
        assert d.exists(f"{name}: {was}"), "undo didn't restore the day"
    finally:
        d.back()


@check
def habits_week_and_overall():
    d.nav("Habits")
    assert d.exists("complete this week", contains=True), "week view missing"
    d.tap("Overall")
    assert d.exists("Last 26 weeks"), "overall heatmap missing"
    assert d.exists("Best streak · ", contains=True), "overall card missing its best streak line"
    assert d.exists("-day streak", contains=True), "overall card missing its current streak"
    assert not any(n.label.endswith("%") for n in d.dump()), "overall still shows a percentage"
    d.tap("Week")


@check
def habit_detail_opens():
    d.tap("Read", timeout=5)
    assert d.exists("best streak"), "detail stats missing"
    assert d.exists("this month"), "detail's first stat isn't \"this month\""
    assert not any(n.label.endswith("%") for n in d.dump()), "detail still shows a percentage"
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
def journal_cards_and_new_note():
    d.nav("Journal")
    assert d.exists("Today"), "no date header over today's notes"
    assert d.exists("Skipped"), "note card doesn't show the day's outcome"
    d.tap("Add note")
    # The check before this one wrote today's note for the first habit, so Add note shows it.
    assert d.exists("Smoke test note"), "today's saved note isn't shown when adding a note for the same habit and day"
    time.sleep(1)
    assert "mInputShown=true" in d.shell("dumpsys input_method"), "new note didn't open with the keyboard"
    d.back()
    d.tap("Close")


@check
def icon_picker():
    d.nav("Today")
    d.scroll_to_top()
    d.tap("New habit")
    d.tap("More icons")
    assert d.exists("Choose icon"), "icon picker didn't open"
    _, h = d.screen_size()
    title = d.find("Choose icon")
    assert title.bounds[1] > h * 0.08, "sheet reaches the top of the screen"
    use = d.find("Use this icon")
    assert use.bounds[3] > h * 0.85, "sheet doesn't sit on the bottom edge"
    d.tap("Search icons")
    d.type_text("pho")
    assert d.exists("Less phone") and not d.exists("Code", timeout=1), "search doesn't filter icons"
    d.back()  # keyboard
    d.tap("Less phone")
    d.tap("Use this icon")
    n = d.find("Less phone")
    assert n and (n.selected or n.checked), "picked icon isn't selected in the row"
    d.tap("Close")


@check
def journal_long_press_actions():
    d.nav("Journal")
    d.long_press("Rough morning, but I noticed it sooner than usual.")
    for label in ["Edit note", "Share", "Delete note"]:
        assert d.exists(label), f"long-press sheet missing {label}"
    d.back()


@check
def detail_header_buttons():
    d.nav("Habits")
    d.tap("Wake up at 7")
    for label in ["Back", "Edit habit", "Archive", "Delete"]:
        assert d.find(label), f"habit page header missing {label}"
    d.tap("Back")


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
    # Made by measure_with_step; no sample note and no check writes one. Last in the sheet's list.
    n = d.scroll_to("Deep work")
    assert n, "Deep work not in the filter sheet"
    d.tap_xy(*n.center)
    d.tap("Show notes for 1 habit")
    time.sleep(1)
    assert d.exists("No notes for these filters"), "empty filter message missing"
    d.tap("Clear filters")
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
    assert d.exists(f"{today.day} ", contains=True), "date chip doesn't show the range"
    d.tap("Clear date filter")
    assert d.exists("This week"), "the chip's ✕ didn't go back to this week"
    d.tap("Filter by date")
    d.tap("All dates")
    assert d.exists("Clear date filter"), "All dates chip has no ✕"
    d.tap("Clear date filter")
    assert d.exists("This week"), "Journal didn't go back to this week"


@check
def note_date_in_a_sheet():
    import datetime
    today = datetime.date.today()
    if today.day == 1:
        return  # no earlier day in this month to pick
    d.nav("Journal")
    d.tap("Add note")
    time.sleep(1)
    # Hide the keyboard, but only once it's up: Back without it would close the screen.
    if "mInputShown=true" in d.shell("dumpsys input_method"):
        d.back()
    d.tap(today.strftime("%A, ") + str(today.day), contains=True)
    assert d.exists("Use ", contains=True), "note date didn't open the date sheet"
    d.tap(str(today.day - 1))
    d.tap("Use ", contains=True)
    yesterday = today - datetime.timedelta(days=1)
    assert d.exists(yesterday.strftime("%A, ") + str(yesterday.day), contains=True), "note date didn't change"
    d.tap("Close")


@check
def note_shows_the_saved_one_for_that_day():
    # The sample data has a Stay calm note for today.
    d.nav("Journal")
    d.tap("Add note")
    time.sleep(1)
    if "mInputShown=true" in d.shell("dumpsys input_method"):
        d.back()
    d.tap("Choose habit")
    time.sleep(1)
    d.tap("Stay calm")
    assert d.exists("Rough morning", contains=True), "the day's saved note isn't shown in the form"
    assert d.exists("Edit note"), "the form didn't switch to editing the saved note"
    d.tap("Close")


@check
def insights_render():
    d.nav("Insights")
    assert d.exists("average"), "score card missing"
    assert d.exists("Habits per day", contains=True), "bar chart missing"
    d.tap("Change date range")
    assert d.exists("Date range"), "date range sheet didn't open"
    d.tap("1")  # the 1st of this month starts a new range
    d.tap("Show 1", contains=True)
    time.sleep(1)
    assert d.exists("average"), "insights missing after picking a range"
    d.tap("Filter by habit")
    d.tap("Read")
    d.tap("Show 1 habit")
    assert d.exists("Remove habit filter"), "habit filter chip missing"
    d.tap("Remove habit filter")
    assert not d.exists("Remove habit filter", timeout=1.0), "chip's ✕ didn't clear the habit filter"
    assert d.exists("Clear date filter"), "date chip has no ✕ for a custom range"
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
    assert d.scroll_to("Open-source licences"), "About screen incomplete"
    d.back()


@check
def more_language_sheet():
    # Pick Spanish, see the app switch at once, then go back to the phone's language.
    d.nav("More")
    assert d.scroll_to("Language"), "Language row missing"
    d.tap("Language")
    assert d.exists("Help translate Sprout"), "Language sheet missing"
    assert d.scroll_to("日本語"), "Japanese not listed"
    d.tap("Español")
    assert d.exists("Más", timeout=8), "app did not switch to Spanish"
    assert d.scroll_to("Idioma"), "Language row not translated"
    d.tap("Idioma")
    d.tap("Predeterminado del sistema")
    assert d.exists("More", timeout=8), "app did not return to the phone's language"


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


def recover():
    """After a failure, back out of sheets and inner screens to the tabs so later checks run."""
    d.shell(f"am start -W -n {d.PACKAGE}/.MainActivity")
    for _ in range(4):
        if d.exists("Today", timeout=1) and d.exists("Insights", timeout=1):
            return
        d.back()


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
            recover()
    print(f"\n{len(selected) - failures}/{len(selected)} passed")
    return failures


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
