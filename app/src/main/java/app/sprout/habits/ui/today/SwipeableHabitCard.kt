package app.sprout.habits.ui.today

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
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.sprout.habits.R
import app.sprout.habits.domain.DayOutcome
import app.sprout.habits.ui.theme.habitColors

/**
 * [HabitCard] with swipe right = Done and swipe left = Skip. The card springs back after the
 * swipe; the new state comes from the database. Swipe state lives here, per card, so a swipe
 * never recomposes the list.
 */
@Composable
fun SwipeableHabitCard(
    habit: HabitRowUi,
    onDone: () -> Unit,
    onSkip: () -> Unit,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val currentOnDone by rememberUpdatedState(onDone)
    val currentOnSkip by rememberUpdatedState(onSkip)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    currentOnDone()
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    currentOnSkip()
                }
                SwipeToDismissBoxValue.Settled -> Unit
            }
            // Never stay dismissed: the card returns to rest showing its new state.
            false
        },
        positionalThreshold = { distance -> distance * 0.35f },
    )
    val canDone = habit.outcome != DayOutcome.DONE
    val canSkip = habit.outcome != DayOutcome.SKIP
    SwipeToDismissBox(
        state = state,
        modifier = modifier.semantics {
            customActions = buildList {
                if (canDone) add(CustomAccessibilityAction("Mark done") { onDone(); true })
                if (canSkip) add(CustomAccessibilityAction("Skip") { onSkip(); true })
                add(CustomAccessibilityAction("Log amount") { onLongPress(); true })
            }
        },
        enableDismissFromStartToEnd = canDone,
        enableDismissFromEndToStart = canSkip,
        backgroundContent = { SwipeBackground(habit, state.dismissDirection) },
    ) {
        HabitCard(habit, onToggle = onToggle, onLongPress = onLongPress, onClick = onClick)
    }
}

@Composable
private fun SwipeBackground(habit: HabitRowUi, direction: SwipeToDismissBoxValue) {
    val colors = MaterialTheme.colorScheme
    val hc = habitColors(habit.hue)
    when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Row(
            Modifier.fillMaxSize().background(hc.solid, HabitCardShape).padding(start = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = hc.on, modifier = Modifier.size(20.dp))
            Text("Done", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = hc.on)
        }

        SwipeToDismissBoxValue.EndToStart -> Row(
            Modifier.fillMaxSize().background(colors.surfaceContainerHigh, HabitCardShape).padding(end = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        ) {
            Text("Skip", style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant)
            Icon(painterResource(R.drawable.ic_skip), contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }

        SwipeToDismissBoxValue.Settled -> Box(Modifier)
    }
}
