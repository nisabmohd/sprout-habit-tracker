package app.sprout.habits.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.sprout.habits.R
import app.sprout.habits.ui.theme.habitColors

@Immutable
data class HabitFilterOption(val habitId: Long, val name: String, val icon: Int, val hue: Float)

/**
 * "Filter by habit", shared by Journal and Insights: tick any number of habits, Clear to
 * untick all, then apply. An empty selection means every habit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitFilterSheet(
    options: List<HabitFilterOption>,
    selected: Set<Long>,
    applyLabel: (count: Int) -> String,
    onApply: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var picked by remember { mutableStateOf(selected) }
    SproutSheet(onDismissRequest = onDismiss) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.filter_by_habit), style = MaterialTheme.typography.titleLarge, color = colors.onSurface, modifier = Modifier.weight(1f))
            TextButton(onClick = { picked = emptySet() }, enabled = picked.isNotEmpty()) { Text(stringResource(R.string.clear), style = MaterialTheme.typography.labelLarge) }
        }
        LazyColumn(Modifier.weight(1f, fill = false), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            items(options, key = { it.habitId }) { o ->
                val hc = habitColors(o.hue)
                val checked = o.habitId in picked
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .toggleable(checked, role = Role.Checkbox) { on -> picked = if (on) picked + o.habitId else picked - o.habitId }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(36.dp).background(hc.soft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(o.icon), contentDescription = null, tint = hc.ink, modifier = Modifier.size(20.dp))
                    }
                    Text(o.name, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, modifier = Modifier.weight(1f).padding(start = 14.dp))
                    Checkbox(checked = checked, onCheckedChange = null)
                }
            }
        }
        Button(
            onClick = { onApply(picked) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).navigationBarsPadding().height(48.dp),
        ) { Text(applyLabel(picked.size), style = MaterialTheme.typography.labelLarge) }
    }
}

/**
 * A 48 dp round header button (back, close, save, edit, delete, calendar, filter), filled in when
 * its filter is [active], which also puts a small dot in its corner. [primary] is the screen's main action (Save): filled with the primary color.
 * Every screen header uses it so they all look the same.
 */
@Composable
fun HeaderIconButton(
    icon: Int,
    label: String,
    active: Boolean = false,
    danger: Boolean = false,
    primary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = when {
                primary -> colors.primary
                active -> colors.primaryContainer
                else -> colors.surfaceContainerHigh
            },
            contentColor = when {
                primary -> colors.onPrimary
                active -> colors.onPrimaryContainer
                danger -> colors.error
                else -> colors.onSurface
            },
            disabledContainerColor = colors.surfaceContainerHigh,
            disabledContentColor = colors.onSurface.copy(alpha = 0.38f),
        ),
    ) {
        Icon(painterResource(icon), contentDescription = label, modifier = Modifier.size(22.dp))
    }
    // 10 dp across with its 2 dp ring in the page color, 6 dp in from the top right. No number:
    // the chip under the header says what is applied.
    if (active) {
        Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(10.dp).background(colors.background, CircleShape).padding(2.dp).background(colors.primary, CircleShape))
    }
    }
}

/** The applied filters under a tab header: the date chip first, then the habit chip. */
@Composable
fun FilterChipRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = content)
}

/**
 * The date range in use. With [onClear] it is a range the user picked: filled primaryContainer
 * with a ✕ that goes back to the default. Without it, it is the default ("This week").
 * Tapping the chip opens the date range sheet.
 */
@Composable
fun DateFilterChip(label: String, openLabel: String, clearLabel: String, onOpen: () -> Unit, onClear: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val content = if (onClear != null) colors.onPrimaryContainer else colors.onSurface
    Row(
        Modifier
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (onClear != null) colors.primaryContainer else colors.surfaceContainerHigh)
            .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onOpen)
            .padding(start = 8.dp, end = if (onClear != null) 4.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_calendar), contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = content, maxLines = 1, modifier = Modifier.padding(start = 6.dp, end = if (onClear != null) 6.dp else 0.dp))
        if (onClear != null) SmallClearButton(clearLabel, 24.dp, Color.Transparent, content, onClear)
    }
}

/**
 * The habit filter that is on: overlapping dots in the habits' colors, [label] ("3 habits", or
 * their names) and a ✕ that clears it. Tapping the chip opens the filter sheet again.
 */
@Composable
fun RowScope.HabitFilterChip(selected: List<HabitFilterOption>, label: String, removeLabel: String, onOpen: () -> Unit, onClear: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .weight(1f, fill = false)
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.primaryContainer)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(start = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-5).dp)) {
            selected.take(5).forEach { option ->
                Box(Modifier.size(18.dp).background(colors.primaryContainer, CircleShape).padding(2.dp).background(habitColors(option.hue).solid, CircleShape))
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = colors.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false).padding(horizontal = 6.dp),
        )
        SmallClearButton(removeLabel, 24.dp, Color.Transparent, colors.onPrimaryContainer, onClear)
    }
}
