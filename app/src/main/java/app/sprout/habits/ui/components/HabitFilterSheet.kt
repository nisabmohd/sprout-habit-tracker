package app.sprout.habits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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

/** A 48 dp round header button (calendar, filter), filled in when its filter is active. */
@Composable
fun HeaderIconButton(icon: Int, label: String, active: Boolean = false, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (active) colors.primaryContainer else colors.surfaceContainerHigh,
            contentColor = if (active) colors.onPrimaryContainer else colors.onSurface,
        ),
    ) {
        Icon(painterResource(icon), contentDescription = label, modifier = Modifier.size(22.dp))
    }
}
