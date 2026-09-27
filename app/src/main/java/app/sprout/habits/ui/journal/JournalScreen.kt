package app.sprout.habits.ui.journal

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.sprout.habits.ui.components.NoteCard
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalScrollCaptureInProgress
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sprout.habits.R
import app.sprout.habits.ui.components.DateRangeSheet
import app.sprout.habits.ui.components.HabitFilterSheet
import app.sprout.habits.ui.components.HeaderIconButton
import app.sprout.habits.ui.theme.habitColors

@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    weekStart: java.time.DayOfWeek,
    onAddNote: (() -> Unit)?,
    onOpenNote: ((Long) -> Unit)?,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var choosing by remember { mutableStateOf(false) }
    var pickingRange by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "title") {
                Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        state?.rangeLabel?.let { Text(it, style = type.titleSmall, color = colors.onSurfaceVariant) }
                        Text("Journal", style = type.headlineMedium, color = colors.onBackground)
                    }
                    // Every filter lives in a sheet: dates here, habits next to it.
                    state?.let { ui ->
                        HeaderIconButton(R.drawable.ic_calendar, "Filter by date", active = ui.from != null) { pickingRange = true }
                        if (ui.options.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            HeaderIconButton(R.drawable.ic_filter, "Filter by habit", active = ui.filter.isNotEmpty()) { choosing = true }
                        }
                    }
                }
            }
            val list = state?.days
            val filter = state?.filter.orEmpty()
            val dated = state?.from != null
            if (list != null && list.isEmpty() && (filter.isNotEmpty() || dated)) {
                item(key = "empty-filter") {
                    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            when {
                                dated && filter.isEmpty() -> "No notes in these dates"
                                filter.size == 1 -> "No notes for this habit yet"
                                else -> "No notes for these habits yet"
                            },
                            style = type.titleMedium,
                            color = colors.onSurface,
                        )
                        TextButton(onClick = { viewModel.setFilter(emptySet()); viewModel.setRange(null, null) }) { Text("Show all notes") }
                    }
                }
            } else if (list != null && list.isEmpty()) {
                item(key = "empty") {
                    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("No notes yet", style = type.titleMedium, color = colors.onSurface)
                        Text("Add a note to remember how a day went.", style = type.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
            }
            list.orEmpty().forEach { day ->
                item(key = "d${day.day}") {
                    Text(
                        day.label,
                        style = type.titleSmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                    )
                }
                items(day.notes, key = { it.id }) { note -> NoteCard(note, onOpenNote) }
            }
        }
        // Hidden during a long screenshot, or it would be stamped into every captured frame.
        if (onAddNote != null && !LocalScrollCaptureInProgress.current) {
            ExtendedFloatingActionButton(
                onClick = onAddNote,
                icon = { Icon(painterResource(R.drawable.ic_habit_pen), contentDescription = null, modifier = Modifier.size(22.dp)) },
                text = { Text("Add note", style = type.labelLarge) },
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).semantics { contentDescription = "Add note" },
            )
        }
    }
    val ui = state
    if (pickingRange && ui != null) {
        DateRangeSheet(
            from = ui.from,
            to = ui.to,
            weekStart = weekStart,
            shortcutLabel = "All dates",
            onApply = { a, b -> viewModel.setRange(a, b); pickingRange = false },
            onShortcut = { viewModel.setRange(null, null); pickingRange = false },
            onDismiss = { pickingRange = false },
        )
    }
    if (choosing && ui != null) {
        HabitFilterSheet(
            options = ui.options,
            selected = ui.filter,
            applyLabel = { n -> if (n == 0) "Show all notes" else if (n == 1) "Show notes for 1 habit" else "Show notes for $n habits" },
            onApply = { ids -> viewModel.setFilter(ids); choosing = false },
            onDismiss = { choosing = false },
        )
    }
}
