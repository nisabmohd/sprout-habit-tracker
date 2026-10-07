package app.sprout.habits.ui.components

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/*
 * The app's haptics, so the same kind of action always feels the same:
 * - toggle: something turns on or off (a switch, a day circle, a checkbox, tapping a habit's circle).
 * - tick: one choice in a row is picked (chips, segmented buttons, swatches), or − / + steps once.
 * - stepTick: each step a slider passes, lighter than tick because it repeats quickly.
 * - threshold: a swipe has gone far enough to count; letting go now logs it (once, going out).
 * - confirm: a change is saved (swipe released past the threshold, Save in a sheet).
 * Long-press keeps HapticFeedbackType.LongPress.
 */

fun HapticFeedback.toggle(on: Boolean) =
    performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)

fun HapticFeedback.tick() = performHapticFeedback(HapticFeedbackType.SegmentTick)

fun HapticFeedback.stepTick() = performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)

fun HapticFeedback.threshold() = performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)

fun HapticFeedback.confirm() = performHapticFeedback(HapticFeedbackType.Confirm)
