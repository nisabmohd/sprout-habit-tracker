"""Small helpers for driving the Sprout app on an emulator or phone over adb.

Only the Python standard library; finds elements by their visible text or TalkBack label
(content description), so checks don't depend on screen coordinates.
"""
import os
import re
import subprocess
import time

PACKAGE = "app.sprout.habits"
ADB = os.environ.get("ADB") or os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")


def adb(*args, check=False):
    return subprocess.run([ADB, *args], capture_output=True, text=True, check=check).stdout


def shell(cmd):
    return adb("shell", cmd)


class Node:
    def __init__(self, raw):
        def attr(key):
            m = re.search(key + r'="([^"]*)"', raw)
            return m.group(1) if m else ""
        self.text = attr("text").replace("&amp;", "&")
        self.desc = attr("content-desc").replace("&amp;", "&")
        self.cls = attr("class")
        self.package = attr("package")
        self.clickable = attr("clickable") == "true"
        self.checkable = attr("checkable") == "true"
        self.checked = attr("checked") == "true"
        self.scrollable = attr("scrollable") == "true"
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", attr("bounds")))
        self.bounds = (x1, y1, x2, y2)

    @property
    def label(self):
        return self.text or self.desc

    @property
    def center(self):
        x1, y1, x2, y2 = self.bounds
        return (x1 + x2) // 2, (y1 + y2) // 2

    def __repr__(self):
        return f"Node({self.label!r} {self.bounds})"


def dump():
    """The current screen as a list of nodes."""
    shell("uiautomator dump /sdcard/ui.xml")
    xml = shell("cat /sdcard/ui.xml")
    return [Node(m.group(0)) for m in re.finditer(r"<node [^>]*>", xml)]


def find(label, contains=False, timeout=5.0):
    """Waits for a node whose text or description is [label] (or contains it)."""
    end = time.time() + timeout
    while True:
        for n in dump():
            if n.label == label or (contains and label in n.label):
                return n
        if time.time() > end:
            return None
        time.sleep(0.4)


def exists(label, contains=False, timeout=3.0):
    return find(label, contains, timeout) is not None


def tap(label, contains=False, timeout=5.0):
    n = find(label, contains, timeout)
    if n is None:
        raise AssertionError(f"not on screen: {label!r}")
    shell(f"input tap {n.center[0]} {n.center[1]}")
    time.sleep(0.8)
    return n


def tap_xy(x, y):
    shell(f"input tap {x} {y}")
    time.sleep(0.8)


def swipe(x1, y1, x2, y2, ms=300):
    shell(f"input swipe {x1} {y1} {x2} {y2} {ms}")
    time.sleep(0.8)


def long_press(label, contains=False):
    n = find(label, contains)
    if n is None:
        raise AssertionError(f"not on screen: {label!r}")
    x, y = n.center
    swipe(x, y, x, y, 900)


def scroll_to(label, contains=False, max_swipes=6):
    """Scrolls down the current list until [label] is on screen; returns its node or None."""
    w, h = screen_size()
    for _ in range(max_swipes + 1):
        n = find(label, contains, timeout=0.5)
        if n:
            return n
        swipe(w // 2, int(h * 0.75), w // 2, int(h * 0.35), 300)
    return None


def type_text(text):
    shell("input text " + text.replace(" ", "%s").replace("'", "\\'"))
    time.sleep(0.4)


def back():
    shell("input keyevent KEYCODE_BACK")
    time.sleep(0.8)


def screen_size():
    m = re.search(r"(\d+)x(\d+)", shell("wm size"))
    return int(m.group(1)), int(m.group(2))


def density():
    m = re.search(r"(\d+)", shell("wm density").splitlines()[-1])
    return int(m.group(1)) / 160


def nav(tab):
    """Taps a bottom-bar tab by its label (the bar's labels are always the last matches)."""
    nodes = [n for n in dump() if n.label == tab and n.package == PACKAGE]
    if not nodes:
        raise AssertionError(f"tab not found: {tab}")
    n = max(nodes, key=lambda n: n.bounds[1])
    shell(f"input tap {n.center[0]} {n.center[1]}")
    time.sleep(1.0)


def small_targets(min_dp=48):
    """Interactive app nodes smaller than [min_dp], ignoring ones clipped by a scrolling list."""
    d = density()
    nodes = dump()
    # A node touching the top or bottom edge of a scrolling container is cut off, not small.
    edges = {y for n in nodes if n.scrollable for y in (n.bounds[1], n.bounds[3])}
    out = []
    for n in nodes:
        if n.package != PACKAGE or not (n.clickable or n.checkable):
            continue
        x1, y1, x2, y2 = n.bounds
        if y1 in edges or y2 in edges:
            continue
        w, h = (x2 - x1) / d, (y2 - y1) / d
        if w < min_dp - 1.5 or h < min_dp - 1.5:
            out.append(f"{n.label or n.cls} {w:.0f}x{h:.0f}dp")
    return out
