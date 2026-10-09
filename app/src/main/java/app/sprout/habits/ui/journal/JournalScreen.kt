package app.sprout.habits.ui.journal

import app.sprout.habits.ui.theme.SproutType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.sprout.habits.ui.components.NoteCard
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextAlign
import app.sprout.habits.ui.components.CustomRangeChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalScrollCaptureInProgress
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import app.sprout.habits.R
import app.sprout.habits.ui.components.TabHeader
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.graphics.Color
import app.sprout.habits.ui.today.UndoSnackbarHost
import app.sprout.habits.ui.components.SproutSheet
import app.sprout.habits.ui.components.NoteCardUi
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import android.content.Intent
import app.sprout.habits.ui.components.DateRangeSheet
import app.sprout.habits.ui.components.HabitFilterChip
import app.sprout.habits.ui.components.FilterChipRow
import app.sprout.habits.ui.components.groupedShape
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import app.sprout.habits.ui.components.HabitFilterSheet
import app.sprout.habits.ui.components.HeaderIconButton

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
    var actionsFor by remember { mutableStateOf<NoteCardUi?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val deletedText = stringResource(R.string.note_deleted)
    val undo = stringResource(R.string.action_undo)
    LaunchedEffect(viewModel) {
        viewModel.deleted.collectLatest { note ->
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(deletedText, actionLabel = undo, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) viewModel.restoreNote(note)
        }
    }
    val colors = MaterialTheme.colorScheme
    val res = LocalContext.current.resources
    val addNote = stringResource(R.string.add_note)
    Box(Modifier.fillMaxSize()) {
        // The header stays put while the list scrolls under it, so its buttons are always in reach.
        Column(Modifier.fillMaxSize()) {
        Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 2.dp)) {
                // Every filter lives in a sheet; the chips under the title show what is applied.
                Column(Modifier.padding(bottom = 14.dp)) {
                    TabHeader(stringResource(R.string.journal_title), listSpacing = 16.dp) {
                        state?.let { ui ->
                            HeaderIconButton(R.drawable.ic_calendar, stringResource(R.string.filter_by_date), active = ui.from != null) { pickingRange = true }
                            if (ui.options.isNotEmpty()) {
                                HeaderIconButton(R.drawable.ic_filter, stringResource(R.string.filter_by_habit), active = ui.filter.isNotEmpty()) { choosing = true }
                            }
                        }
                    }
                    // No preset ranges: the row only shows the filters that are on, each with its ✕.
                    val ui = state
                    if (ui != null && (ui.from != null || ui.filter.isNotEmpty())) {
                        FilterChipRow(Modifier.padding(top = 12.dp)) {
                            if (ui.from != null) {
                                CustomRangeChip(
                                    label = ui.dateLabel,
                                    openLabel = stringResource(R.string.filter_by_date),
                                    clearLabel = stringResource(R.string.clear_date_filter),
                                    onOpen = { pickingRange = true },
                                    onClear = viewModel::clearRange,
                                )
                            }
                            if (ui.filter.isNotEmpty()) {
                                val picked = ui.options.filter { it.habitId in ui.filter }
                                HabitFilterChip(
                                    selected = picked,
                                    // Names while there is room for them; a count next to a picked range.
                                    label = if (ui.from == null) picked.joinToString(", ") { it.name } else pluralStringResource(R.plurals.habit_count, picked.size, picked.size),
                                    removeLabel = stringResource(R.string.remove_habit_filter),
                                    onOpen = { choosing = true },
                                    onClear = { viewModel.setFilter(emptySet()) },
                                )
                            }
                        }
                    }
                }
            
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val list = state?.days
            val shown = state
            if (shown != null && shown.days.isEmpty() && shown.hasNotes) {
                // The filters hide every note: say which ones, and offer the way back.
                item(key = "empty-filter") {
                    EmptyJournal(
                        title = if (shown.from != null) {
                            stringResource(R.string.journal_empty_range, shown.dateLabel)
                        } else {
                            stringResource(R.string.journal_empty_habits)
                        },
                        text = stringResource(R.string.journal_empty_earlier),
                        action = stringResource(R.string.journal_show_all),
                        onAction = viewModel::showAllNotes,
                    )
                }
            } else if (shown != null && shown.days.isEmpty()) {
                item(key = "empty") {
                    EmptyJournal(stringResource(R.string.journal_empty), stringResource(R.string.journal_empty_hint), action = null, onAction = {})
                }
            }
            list.orEmpty().forEachIndexed { index, day ->
                item(key = "d${day.day}") {
                    // The day on the left, how many notes it has on the right, like the Insights
                    // section headers.
                    Row(
                        // With the list's 2 dp gaps: 20 dp above the header, 8 dp below it.
                        Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = if (index == 0) 0.dp else 18.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(day.label, style = SproutType.cardTitle, color = colors.onSurface, modifier = Modifier.weight(1f))
                        Text(
                            pluralStringResource(R.plurals.note_count, day.notes.size, day.notes.size),
                            style = SproutType.supporting,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                // One group per day: 2 dp between its notes, big corners only on the outside.
                itemsIndexed(day.notes, key = { _, note -> note.id }) { i, note ->
                    NoteCard(note, onOpenNote, shape = groupedShape(i, day.notes.size), onLongPress = { actionsFor = it })
                }
            }
        }
        }
        // Hidden during a long screenshot, or it would be stamped into every captured frame.
        if (onAddNote != null && !LocalScrollCaptureInProgress.current) {
            ExtendedFloatingActionButton(
                onClick = onAddNote,
                icon = { Icon(painterResource(R.drawable.ic_habit_pen), contentDescription = null, modifier = Modifier.size(22.dp)) },
                text = { Text(stringResource(R.string.add_note), style = SproutType.label) },
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    // Moves up while the Undo snackbar shows, like on Today.
                    .padding(bottom = if (snackbar.currentSnackbarData != null) 64.dp else 0.dp)
                    .semantics { contentDescription = addNote },
            )
        }
        UndoSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(horizontal = 8.dp))
    }
    val ui = state
    if (pickingRange && ui != null) {
        DateRangeSheet(
            from = ui.from,
            to = ui.to,
            weekStart = weekStart,
            shortcutLabel = stringResource(R.string.all_dates),
            onApply = { a, b -> viewModel.setRange(a, b); pickingRange = false },
            onShortcut = { viewModel.clearRange(); pickingRange = false },
            onDismiss = { pickingRange = false },
        )
    }
    actionsFor?.let { note ->
        NoteActionsSheet(
            note,
            onEdit = onOpenNote?.let { open -> { actionsFor = null; open(note.id) } },
            onDelete = { actionsFor = null; viewModel.deleteNote(note.id) },
            onDismiss = { actionsFor = null },
        )
    }
    if (choosing && ui != null) {
        HabitFilterSheet(
            options = ui.options,
            selected = ui.filter,
            applyLabel = { n -> if (n == 0) res.getString(R.string.journal_show_all) else res.getQuantityString(R.plurals.journal_show_for_habits, n, n) },
            onApply = { ids -> viewModel.setFilter(ids); choosing = false },
            onDismiss = { choosing = false },
        )
    }
}

/**
 * Nothing to list: a note tile, what is missing and why, and with [action] a tonal button that
 * brings every note back.
 */
@Composable
private fun EmptyJournal(title: String, text: String, action: String?, onAction: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(top = 72.dp, start = 16.dp, end = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp).background(colors.surfaceContainerHigh, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_note), contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(32.dp))
        }
        Text(title, style = SproutType.title, color = colors.onSurface, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 20.dp))
        Text(text, style = SproutType.supporting, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        if (action != null) {
            FilledTonalButton(
                onClick = onAction,
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.secondaryContainer, contentColor = colors.onSecondaryContainer),
                modifier = Modifier.padding(top = 20.dp).heightIn(min = 40.dp),
            ) {
                Text(action, style = SproutType.label)
            }
        }
    }
}

/** Long-press on a note: its habit and text on top, then Edit, Share and Delete. */
@Composable
private fun NoteActionsSheet(note: NoteCardUi, onEdit: (() -> Unit)?, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.action_share)
    SproutSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(note.habitName, style = SproutType.title, color = colors.onSurface)
            Text(note.text, style = SproutType.supporting, color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (onEdit != null) ActionRow(R.drawable.ic_habit_pen, stringResource(R.string.edit_note), colors.onSurface, onEdit)
        ActionRow(R.drawable.ic_share, shareTitle, colors.onSurface) {
            onDismiss()
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "${note.habitName}\n${note.text}")
            context.startActivity(Intent.createChooser(send, shareTitle))
        }
        ActionRow(R.drawable.ic_delete, stringResource(R.string.delete_note), colors.error, onDelete)
        Spacer(Modifier.padding(bottom = 12.dp).navigationBarsPadding())
    }
}

@Composable
private fun ActionRow(icon: Int, label: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(label, style = SproutType.body, color = color, modifier = Modifier.padding(start = 16.dp))
    }
}
