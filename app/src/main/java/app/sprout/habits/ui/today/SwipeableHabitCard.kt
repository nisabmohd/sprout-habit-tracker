package app.sprout.habits.ui.today

import app.sprout.habits.ui.components.confirm
import app.sprout.habits.ui.components.tick
import app.sprout.habits.ui.components.threshold
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import app.sprout.habits.ui.theme.SproutType
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.sprout.habits.R
import app.sprout.habits.domain.DayOutcome
import app.sprout.habits.ui.theme.habitColors
import kotlin.math.abs

/**
 * [HabitCard] with swipe right = Done and swipe left = Skip; on a card that's already done or
 * skipped, the opposite swipe undoes it. The card springs back after the
 * swipe; the new state comes from the database. Swipe state lives here, per card, so a swipe
 * never recomposes the list.
 */
@Composable
fun SwipeableHabitCard(
    habit: HabitRowUi,
    onDone: () -> Unit,
    onSkip: () -> Unit,
    onUndo: () -> Unit,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val currentOnDone by rememberUpdatedState(onDone)
    val currentOnSkip by rememberUpdatedState(onSkip)
    val currentOnUndo by rememberUpdatedState(onUndo)
    val outcome by rememberUpdatedState(habit.outcome)
    var width by remember { mutableIntStateOf(0) }
    // Read inside confirmValueChange, which is passed before the state exists.
    val stateRef = remember { arrayOfNulls<SwipeToDismissBoxState>(1) }
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            // A quick flick counts as a swipe no matter how far the card moved, so a slightly
            // diagonal scroll could log a habit. Only a drag past half the card's width counts.
            val offset = stateRef[0]?.let { runCatching { it.requireOffset() }.getOrNull() } ?: 0f
            if (value != SwipeToDismissBoxValue.Settled && abs(offset) < width * SWIPE_FRACTION) return@rememberSwipeToDismissBoxState false
            when (value) {
                // The opposite swipe undoes: right on a skipped card, left on a done one.
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptics.confirm()
                    if (outcome == DayOutcome.SKIP) currentOnUndo() else currentOnDone()
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptics.confirm()
                    if (outcome == DayOutcome.DONE) currentOnUndo() else currentOnSkip()
                }
                SwipeToDismissBoxValue.Settled -> Unit
            }
            // Never stay dismissed: the card returns to rest showing its new state.
            false
        },
        positionalThreshold = { distance -> distance * SWIPE_FRACTION },
    )
    stateRef[0] = state
    // A tick as the drag crosses the point where letting go logs it, and again if it goes back.
    LaunchedEffect(state, width) {
        if (width == 0) return@LaunchedEffect
        snapshotFlow { abs(runCatching { state.requireOffset() }.getOrDefault(0f)) >= width * SWIPE_FRACTION }
            .distinctUntilChanged()
            .drop(1)
            .collect { past -> if (past) haptics.threshold() else haptics.tick() }
    }
    val done = habit.outcome == DayOutcome.DONE
    val markDone = stringResource(R.string.action_mark_done)
    val skip = stringResource(R.string.outcome_skip)
    val undo = stringResource(R.string.action_undo)
    val logAmount = stringResource(R.string.log_amount)
    val skipped = habit.outcome == DayOutcome.SKIP
    SwipeToDismissBox(
        state = state,
        modifier = modifier.onSizeChanged { width = it.width }.semantics {
            customActions = buildList {
                if (!done) add(CustomAccessibilityAction(markDone) { onDone(); true })
                if (!skipped) add(CustomAccessibilityAction(skip) { onSkip(); true })
                if (done || skipped) add(CustomAccessibilityAction(undo) { onUndo(); true })
                add(CustomAccessibilityAction(logAmount) { onLongPress(); true })
            }
        },
        backgroundContent = { SwipeBackground(habit, state.dismissDirection) },
    ) {
        HabitCard(habit, onToggle = onToggle, onLongPress = onLongPress, onClick = onClick)
    }
}

private const val SWIPE_FRACTION = 0.5f

@Composable
private fun SwipeBackground(habit: HabitRowUi, direction: SwipeToDismissBoxValue) {
    val colors = MaterialTheme.colorScheme
    val hc = habitColors(habit.hue)
    val undo = (direction == SwipeToDismissBoxValue.StartToEnd && habit.outcome == DayOutcome.SKIP) ||
        (direction == SwipeToDismissBoxValue.EndToStart && habit.outcome == DayOutcome.DONE)
    if (undo) {
        val start = direction == SwipeToDismissBoxValue.StartToEnd
        Row(
            Modifier.fillMaxSize().background(colors.surfaceContainerHigh, HabitCardShape).padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, if (start) Alignment.Start else Alignment.End),
        ) {
            Icon(painterResource(R.drawable.ic_undo), contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.action_undo), style = SproutType.label, color = colors.onSurfaceVariant)
        }
        return
    }
    when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Row(
            Modifier.fillMaxSize().background(hc.solid, HabitCardShape).padding(start = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = hc.on, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.outcome_done), style = SproutType.label, color = hc.on)
        }

        SwipeToDismissBoxValue.EndToStart -> Row(
            Modifier.fillMaxSize().background(colors.surfaceContainerHigh, HabitCardShape).padding(end = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        ) {
            Text(stringResource(R.string.outcome_skip), style = SproutType.label, color = colors.onSurfaceVariant)
            Icon(painterResource(R.drawable.ic_skip), contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }

        SwipeToDismissBoxValue.Settled -> Box(Modifier)
    }
}
