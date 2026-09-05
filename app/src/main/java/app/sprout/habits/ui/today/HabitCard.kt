package app.sprout.habits.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.sprout.habits.R
import app.sprout.habits.domain.DayOutcome
import app.sprout.habits.ui.components.ProgressRing
import app.sprout.habits.ui.theme.HabitColors
import app.sprout.habits.ui.theme.habitColors
import kotlin.math.roundToInt

val HabitCardShape = RoundedCornerShape(22.dp)

/**
 * One habit on one day, in one of four states (see design/habit-card-states-and-gestures.png).
 * The tonal fill behind the content grows with progress: full when done, part-way when partial.
 */
@Composable
fun HabitCard(
    habit: HabitRowUi,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    val hc = habitColors(habit.hue)
    val skipped = habit.outcome == DayOutcome.SKIP
    val fillFraction = when (habit.outcome) {
        DayOutcome.DONE -> 1f
        DayOutcome.PARTIAL -> habit.progress
        else -> 0f
    }
    Box(
        modifier
            .fillMaxWidth()
            // Grows with large text instead of clipping it.
            .heightIn(min = 68.dp)
            .height(IntrinsicSize.Min)
            .clip(HabitCardShape)
            .background(if (skipped) colors.surfaceContainerHigh else colors.surfaceContainerLowest)
            .combinedClickable(
                onLongClickLabel = "Log amount",
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                },
                onClickLabel = "Open ${habit.name}",
                onClick = onClick,
            ),
    ) {
        if (fillFraction > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fillFraction)
                    .background(if (habit.outcome == DayOutcome.DONE) hc.mid else hc.soft),
            )
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(habit, hc)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (skipped) colors.onSurfaceVariant else colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    habit.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            TrailingControl(habit, hc, onToggle)
        }
    }
}

@Composable
private fun IconTile(habit: HabitRowUi, hc: HabitColors) {
    val colors = MaterialTheme.colorScheme
    val (tile, tint) = when (habit.outcome) {
        DayOutcome.DONE -> hc.solid to hc.on
        DayOutcome.PARTIAL -> hc.mid to hc.ink
        DayOutcome.SKIP -> colors.surfaceContainerLowest to colors.onSurfaceVariant
        DayOutcome.OPEN -> hc.soft to hc.ink
    }
    Box(Modifier.size(44.dp).background(tile, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        Icon(painterResource(habit.icon), contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun TrailingControl(habit: HabitRowUi, hc: HabitColors, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val label = if (habit.outcome == DayOutcome.DONE) "Mark ${habit.name} not done" else "Mark ${habit.name} done"
    when (habit.outcome) {
        DayOutcome.SKIP -> Box(
            Modifier
                .height(32.dp)
                .background(colors.surfaceContainerLowest, CircleShape)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Skipped", style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant)
        }

        DayOutcome.DONE -> CircleButton(label, onToggle) {
            Box(Modifier.size(40.dp).background(hc.solid, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = hc.on, modifier = Modifier.size(20.dp))
            }
        }

        DayOutcome.PARTIAL -> CircleButton(label, onToggle) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                ProgressRing(habit.progress, hc.solid, hc.mid, 4.dp, Modifier.fillMaxSize())
                Text(
                    "${(habit.progress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = hc.ink,
                )
            }
        }

        DayOutcome.OPEN -> CircleButton(label, onToggle) {
            Box(Modifier.size(40.dp).border(2.dp, hc.solid, CircleShape))
        }
    }
}

/** 48 dp touch target around the visible circle. */
@Composable
private fun CircleButton(label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp).semantics { contentDescription = label },
    ) {
        content()
    }
}
