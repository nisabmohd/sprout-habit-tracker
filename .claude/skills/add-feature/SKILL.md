---
name: add-feature
description: The checklist for adding or changing a Sprout feature so it stays documented and verifiable. Use whenever starting a new feature, screen or behaviour change.
---

# Add a feature

1. Read `CLAUDE.md` (hard rules, type scale, behaviour rules) and the matching design in `design/` (`design/TOKENS.md` has exact colors and icon paths).
2. No new dependency without asking the user first. The approved list is the Stack section of `CLAUDE.md`.
3. Put pure logic in `domain/` with unit tests in `app/src/test`. UI state classes are immutable; lists have stable keys; stats run off the main thread.
4. Text only uses the Material 3 typography slots (sizes 40/28/22/16/14/12/11). Anything inside a fixed-size shape (rings, segmented buttons, the nav bar) goes in `CappedFontScale`; everything else must grow with the font size.
5. Every clickable is at least 48 dp and has a label TalkBack can read; gestures (swipe, hold) also get a `CustomAccessibilityAction`.
6. Add one check to `tools/verify/smoke.py` (find by visible text or label, never coordinates) and run the **verify-app** skill.
7. Write or update the feature's skill in `.claude/skills/<feature>/SKILL.md`: what it does, where the code is, how to verify it, pitfalls you hit.
8. Tick the item in `docs/PLAN.md` and commit (one commit per checklist item, message ending with the Co-Authored-By line).
