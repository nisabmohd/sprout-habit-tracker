package app.sprout.habits.ui.detail

import app.sprout.habits.ui.datePattern
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import app.sprout.habits.ui.components.NoteCard
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
import androidx.compose.ui.platform.LocalScrollCaptureInProgress
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material3.SnackbarHostState
import app.sprout.habits.ui.today.DayLogHost
import app.sprout.habits.ui.today.UndoSnackbarHost
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sprout.habits.R
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
    val snackbar = remember { SnackbarHostState() }
    DayLogHost(viewModel.logger, snackbar)

    Box(Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(key = "bar") {
                Row(Modifier.fillMaxWidth().height(64.dp).padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.padding(start = 0.dp)) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back), modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onEdit) {
                        Icon(painterResource(R.drawable.ic_habit_pen), contentDescription = stringResource(R.string.edit_habit), modifier = Modifier.size(22.dp))
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.action_more_options), modifier = Modifier.size(22.dp))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(if (ui.habit.archived) R.string.restore else R.string.archive)) },
                                onClick = {
                                    menuOpen = false
                                    viewModel.setArchived(!ui.habit.archived)
                                    if (!ui.habit.archived) onBack()
                                },
                            )
                            DropdownMenuItem(text = { Text(stringResource(R.string.action_delete), color = colors.error) }, onClick = { menuOpen = false; deleting = true })
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
                    StatTile(days(ui.monthDone), ui.monthDoneLabel, hc, Modifier.weight(1f))
                    StatTile(days(ui.bestStreak), stringResource(R.string.best_streak), hc, Modifier.weight(1f))
                    StatTile("${ui.noteCount}", pluralStringResource(R.plurals.notes_count_label, ui.noteCount), hc, Modifier.weight(1f))
                }
            }
            item(key = "calendar") { MonthCalendar(ui, hc, viewModel::previousMonth, viewModel::nextMonth, viewModel::editDay) }
            item(key = "notes-header") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.notes_title), style = type.titleLarge, color = colors.onBackground, modifier = Modifier.weight(1f))
                    if (ui.noteCount > 0) TextButton(onClick = onSeeAllNotes) { Text(stringResource(R.string.see_all), style = type.labelLarge) }
                }
            }
            if (ui.notes.isEmpty()) {
                item(key = "no-notes") {
                    Text(stringResource(R.string.no_notes_yet), style = type.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
            items(ui.notes.take(5), key = { it.card.id }) { note -> NoteCard(note.card, onOpenNote, showHabit = false, dateLabel = note.dateLabel) }
        }
        // Hidden during a long screenshot, or it would be stamped into every captured frame.
        // Lift the button above the Undo snackbar while it shows, so the two never overlap.
        val addNote = stringResource(R.string.add_note)
        val fabLift by animateDpAsState(if (snackbar.currentSnackbarData != null) 72.dp else 0.dp, label = "fabLift")
        if (onAddNote != null && !LocalScrollCaptureInProgress.current) {
            ExtendedFloatingActionButton(
                onClick = onAddNote,
                icon = { Icon(painterResource(R.drawable.ic_habit_pen), contentDescription = null, modifier = Modifier.size(22.dp)) },
                text = { Text(stringResource(R.string.add_note), style = type.labelLarge) },
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(16.dp).padding(bottom = fabLift).semantics { contentDescription = addNote },
            )
        }
        UndoSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 8.dp))
    }

    if (deleting) {
        DeleteHabitDialog(ui.habit.name, onConfirm = { deleting = false; viewModel.delete(onBack) }, onDismiss = { deleting = false })
    }
}

@Composable
private fun days(n: Int) = pluralStringResource(R.plurals.day_count, n, n)

@Composable
private fun StatTile(value: String, label: String, hc: HabitColors, modifier: Modifier) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = hc.ink, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
    }
}

@Composable
private fun MonthCalendar(ui: HabitDetailUi, hc: HabitColors, onPrevious: () -> Unit, onNext: () -> Unit, onEditDay: (java.time.LocalDate) -> Unit) {
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
                Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = stringResource(R.string.previous_month), modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onNext, enabled = ui.canGoForward) {
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = stringResource(R.string.next_month), modifier = Modifier.size(18.dp))
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
                        val day = week.getOrNull(i)
                        // Any scheduled day up to today can be edited; future and unscheduled days can't.
                        val editable = day != null && day.mark.kind != MarkKind.FUTURE && day.mark.kind != MarkKind.NOT_SCHEDULED
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .then(if (editable) Modifier.clickable(onClickLabel = stringResource(R.string.edit_this_day)) { onEditDay(day!!.date) } else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            day?.let { CalendarDay(it, hc, editable) { onEditDay(it.date) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(day: CalendarDayUi, hc: HabitColors, editable: Boolean, onEdit: () -> Unit) {
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
        MarkKind.DONE -> stringResource(R.string.mark_done_lower)
        MarkKind.PARTIAL -> stringResource(R.string.mark_partial_plain)
        MarkKind.SKIP -> stringResource(R.string.mark_skipped_lower)
        MarkKind.OPEN_TODAY -> stringResource(R.string.mark_open_lower)
        MarkKind.FUTURE -> stringResource(R.string.mark_future_lower)
        MarkKind.NOT_SCHEDULED -> stringResource(R.string.mark_not_scheduled_lower)
    }
    val fullDescription = stringResource(R.string.date_description, day.date.format(datePattern("EEEE d MMMM")), description)
    val editLabel = stringResource(R.string.edit_this_day)
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bg)
            .then(if (day.isToday) Modifier.border(2.dp, colors.primary, CircleShape) else Modifier)
            .clearAndSetSemantics {
                contentDescription = fullDescription
                if (editable) onClick(editLabel) { onEdit(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("${day.day}", style = MaterialTheme.typography.titleSmall, color = fg)
    }
}
