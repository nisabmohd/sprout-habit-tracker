package app.sprout.habits.ui.journal

import androidx.compose.foundation.background
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.material3.FloatingActionButton
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
import app.sprout.habits.ui.theme.habitColors

@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onAddNote: (() -> Unit)?,
    onOpenNote: ((Long) -> Unit)?,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var choosing by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "title") {
                Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Journal", style = type.headlineMedium, color = colors.onBackground, modifier = Modifier.weight(1f))
                    state?.let { ui -> if (ui.options.isNotEmpty()) FilterButton(ui.filter) { choosing = true } }
                }
            }
            val list = state?.notes
            val filter = state?.filter
            if (list != null && list.isEmpty() && filter != null) {
                item(key = "empty-filter") {
                    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("No notes for ${filter.name} yet", style = type.titleMedium, color = colors.onSurface)
                        TextButton(onClick = { viewModel.setFilter(null) }) { Text("Show all habits") }
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
            items(list.orEmpty(), key = { it.id }) { note -> JournalCard(note, onOpenNote) }
        }
        // Hidden during a long screenshot, or it would be stamped into every captured frame.
        if (onAddNote != null && !LocalScrollCaptureInProgress.current) {
            FloatingActionButton(
                onClick = onAddNote,
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Icon(painterResource(R.drawable.ic_habit_pen), contentDescription = "Add note", modifier = Modifier.size(22.dp))
            }
        }
    }
    val ui = state
    if (choosing && ui != null) {
        HabitFilterSheet(ui, onPick = { id -> viewModel.setFilter(id); choosing = false }, onDismiss = { choosing = false })
    }
}

/** Top-right button showing the current filter; opens [HabitFilterSheet]. */
@Composable
private fun FilterButton(filter: JournalFilterOption?, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
        if (filter != null) {
            val hc = habitColors(filter.hue)
            Icon(painterResource(filter.icon), contentDescription = null, tint = hc.solid, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            filter?.name ?: "All habits",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 140.dp),
        )
        Icon(painterResource(R.drawable.ic_chevron_down), contentDescription = null, modifier = Modifier.padding(start = 4.dp).size(18.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitFilterSheet(ui: JournalUi, onPick: (Long?) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    // Fully open right away so every habit is visible; the list scrolls if there are many.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surfaceContainerLowest,
    ) {
        Text("Show notes for", style = MaterialTheme.typography.titleLarge, color = colors.onSurface, modifier = Modifier.padding(start = 24.dp, bottom = 8.dp))
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp)) {
            item(key = "all") {
                FilterRow("All habits", null, null, ui.options.sumOf { it.noteCount }, selected = ui.filter == null) { onPick(null) }
            }
            items(ui.options, key = { it.habitId }) { o ->
                FilterRow(o.name, o.icon, o.hue, o.noteCount, selected = ui.filter?.habitId == o.habitId) { onPick(o.habitId) }
            }
        }
    }
}

@Composable
private fun FilterRow(name: String, icon: Int?, hue: Float?, count: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val hc = hue?.let { habitColors(it) }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) colors.surfaceContainerHigh else colors.surfaceContainerLowest)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).background(hc?.soft ?: colors.surfaceContainerHigh, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon ?: R.drawable.ic_nav_journal), contentDescription = null, tint = hc?.ink ?: colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        Text(name, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, modifier = Modifier.weight(1f).padding(start = 14.dp))
        Text(if (count == 1) "1 note" else "$count notes", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun JournalCard(note: JournalNoteUi, onOpen: ((Long) -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val hc = habitColors(note.hue)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceContainerLowest)
            .then(if (onOpen != null) Modifier.clickable(onClickLabel = "Edit note") { onOpen(note.id) } else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(24.dp).background(hc.soft, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(note.icon), contentDescription = null, tint = hc.ink, modifier = Modifier.size(14.dp))
            }
            Text(
                note.habitName,
                style = type.titleSmall,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 10.dp),
            )
            Text(note.dateLabel, style = type.bodyMedium, color = colors.onSurfaceVariant)
        }
        Text(note.text, style = type.bodyLarge, color = colors.onSurface)
    }
}
