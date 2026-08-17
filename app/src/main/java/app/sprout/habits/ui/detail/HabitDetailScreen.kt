package app.sprout.habits.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sprout.habits.R
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.ui.components.MarkKind
import app.sprout.habits.ui.manage.DeleteHabitDialog
import app.sprout.habits.ui.theme.HabitColors
import app.sprout.habits.ui.theme.habitColors

@Composable
fun HabitDetailScreen(
    viewModel: HabitDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddNote: (() -> Unit)?,
    onOpenNote: ((Long) -> Unit)?,
    onSeeAllNotes: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ui = state ?: return
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val hc = habitColors(ui.habit.colorHue.toFloat())
    var menuOpen by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(key = "bar") {
                Row(Modifier.fillMaxWidth().height(64.dp).padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.padding(start = 0.dp)) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back", modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onEdit) {
                        Icon(painterResource(R.drawable.ic_habit_pen), contentDescription = "Edit habit", modifier = Modifier.size(22.dp))
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(painterResource(R.drawable.ic_more_vert), contentDescription = "More options", modifier = Modifier.size(22.dp))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(if (ui.habit.archived) "Restore" else "Archive") },
                                onClick = {
                                    menuOpen = false
                                    viewModel.setArchived(!ui.habit.archived)
                                    if (!ui.habit.archived) onBack()
                                },
                            )
                            DropdownMenuItem(text = { Text("Delete", color = colors.error) }, onClick = { menuOpen = false; deleting = true })
                        }
                    }
                }
            }
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).background(hc.soft, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(ui.icon), contentDescription = null, tint = hc.ink, modifier = Modifier.size(28.dp))
                    }
                    Column(Modifier.padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(ui.habit.name, style = type.headlineMedium, color = colors.onBackground)
                        Text(ui.subtitle, style = type.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
            }
            item(key = "stats") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("${ui.monthPercent}%", "this month", hc, Modifier.weight(1f))
                    StatTile(days(ui.bestStreak), "best streak", hc, Modifier.weight(1f))
                    StatTile("${ui.noteCount}", if (ui.noteCount == 1) "note" else "notes", hc, Modifier.weight(1f))
                }
            }
            item(key = "calendar") { MonthCalendar(ui, hc, viewModel::previousMonth, viewModel::nextMonth) }
            item(key = "notes-header") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Notes", style = type.titleLarge, color = colors.onBackground, modifier = Modifier.weight(1f))
                    if (ui.noteCount > 0) TextButton(onClick = onSeeAllNotes) { Text("See all", style = type.labelLarge) }
                }
            }
            if (ui.notes.isEmpty()) {
                item(key = "no-notes") {
                    Text("No notes yet.", style = type.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
            items(ui.notes.take(5), key = { it.id }) { note -> NoteCard(note, onOpenNote) }
        }
        if (onAddNote != null) {
            ExtendedFloatingActionButton(
                onClick = onAddNote,
                icon = { Icon(painterResource(R.drawable.ic_habit_pen), contentDescription = null, modifier = Modifier.size(22.dp)) },
                text = { Text("Add note", style = type.labelLarge) },
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp),
            )
        }
    }

    if (deleting) {
        DeleteHabitDialog(ui.habit.name, onConfirm = { deleting = false; viewModel.delete(onBack) }, onDismiss = { deleting = false })
    }
}

private fun days(n: Int) = if (n == 1) "1 day" else "$n days"

@Composable
private fun StatTile(value: String, label: String, hc: HabitColors, modifier: Modifier) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = hc.ink, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun MonthCalendar(ui: HabitDetailUi, hc: HabitColors, onPrevious: () -> Unit, onNext: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLowest, RoundedCornerShape(24.dp))
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(ui.monthLabel, style = type.titleMedium, color = colors.onSurface, modifier = Modifier.weight(1f))
            IconButton(onClick = onPrevious) {
                Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "Previous month", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onNext, enabled = ui.canGoForward) {
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = "Next month", modifier = Modifier.size(18.dp))
            }
        }
        val cells: List<CalendarDayUi?> = List(ui.leadingBlanks) { null } + ui.days
        (listOf<List<CalendarDayUi?>?>(null) + cells.chunked(7)).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                if (week == null) {
                    ui.weekdayLabels.forEach { label ->
                        Text(
                            label,
                            style = type.labelSmall,
                            color = colors.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    for (i in 0 until 7) {
                        Box(Modifier.weight(1f).aspectRatio(1.1f), contentAlignment = Alignment.Center) {
                            week.getOrNull(i)?.let { CalendarDay(it, hc) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(day: CalendarDayUi, hc: HabitColors) {
    val colors = MaterialTheme.colorScheme
    val (bg, fg) = when (day.mark.kind) {
        MarkKind.DONE -> hc.solid to hc.on
        MarkKind.PARTIAL -> hc.mid to hc.ink
        MarkKind.SKIP -> colors.outlineVariant to colors.onSurfaceVariant
        MarkKind.OPEN_TODAY -> colors.surfaceContainerLowest to colors.onSurface
        MarkKind.FUTURE -> colors.surfaceContainerLowest to colors.onSurfaceVariant
        MarkKind.NOT_SCHEDULED -> colors.surfaceContainerLowest to colors.outline
    }
    val description = when (day.mark.kind) {
        MarkKind.DONE -> "done"
        MarkKind.PARTIAL -> "partial"
        MarkKind.SKIP -> "skipped"
        MarkKind.OPEN_TODAY -> "not logged yet"
        MarkKind.FUTURE -> "upcoming"
        MarkKind.NOT_SCHEDULED -> "not scheduled"
    }
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bg)
            .then(if (day.isToday) Modifier.border(2.dp, colors.primary, CircleShape) else Modifier)
            .clearAndSetSemantics { contentDescription = "${day.day}: $description" },
        contentAlignment = Alignment.Center,
    ) {
        Text("${day.day}", style = MaterialTheme.typography.titleSmall, color = fg)
    }
}

@Composable
fun NoteCard(note: NoteUi, onOpen: ((Long) -> Unit)?, title: String = note.dateLabel) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surfaceContainerLowest)
            .then(if (onOpen != null) Modifier.clickable(onClickLabel = "Edit note") { onOpen(note.id) } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = type.titleSmall, color = colors.onSurface)
            note.status?.let { status ->
                Spacer(Modifier.width(8.dp))
                Text(
                    when (status) {
                        EntryStatus.DONE -> "Done"
                        EntryStatus.PARTIAL -> "Partial"
                        EntryStatus.SKIP -> "Skipped"
                    },
                    style = type.labelMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.background(colors.surfaceContainerHigh, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Text(note.text, style = type.bodyLarge, color = colors.onSurface)
    }
}
