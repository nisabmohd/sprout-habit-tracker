package app.sprout.habits.ui.note

import app.sprout.habits.ui.datePattern
import androidx.compose.ui.res.stringResource
import app.sprout.habits.rememberStrings
import java.time.LocalDate
import app.sprout.habits.ui.components.MarkColors
import app.sprout.habits.ui.components.DayMarkView
import app.sprout.habits.ui.components.NoteCardUi
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import app.sprout.habits.ui.components.SproutSheet
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sprout.habits.R
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.ui.components.DatePickerSheet
import app.sprout.habits.ui.theme.habitColors
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteNoteScreen(viewModel: WriteNoteViewModel, weekStart: DayOfWeek, onClose: () -> Unit) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.done.collect { onClose() } }
    if (!form.loaded) return

    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    var pickingHabit by remember { mutableStateOf(false) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(start = 8.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.action_close), modifier = Modifier.size(22.dp))
            }
            Text(
                stringResource(if (form.isNew) R.string.new_note else R.string.edit_note),
                style = type.titleLarge,
                color = colors.onBackground,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            if (!form.isNew) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.action_more_options), modifier = Modifier.size(22.dp))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_delete), color = colors.error) }, onClick = { menuOpen = false; confirmDelete = true })
                    }
                }
            }
            Button(onClick = viewModel::save, enabled = form.canSave, modifier = Modifier.height(40.dp)) {
                Text(stringResource(R.string.action_save), style = type.labelLarge)
            }
        }

        // One card down to the keyboard: the habit row on top, then the note, with no outline.
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(colors.surfaceContainerLowest)
                .padding(8.dp),
        ) {
            val habit = habits.firstOrNull { it.id == form.habitId }
            if (habit == null) {
                Text(
                    stringResource(R.string.note_no_habits),
                    style = type.bodyLarge,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                val hc = habitColors(habit.colorHue.toFloat())
                val (mark, outcome) = NoteCardUi.dayOutcome(habit, entry, form.date.toEpochDay(), LocalDate.now().toEpochDay(), rememberStrings())
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(hc.soft)
                        .clickable(onClickLabel = stringResource(R.string.choose_habit)) { pickingHabit = true }
                        .padding(start = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(hc.mid), contentAlignment = Alignment.Center) {
                        Icon(painterResource(HabitIcon.fromKey(habit.icon).drawable), contentDescription = null, tint = hc.ink, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(habit.name, style = type.titleMedium, color = hc.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(
                            Modifier.clickable(onClickLabel = stringResource(R.string.change_date)) { pickingDate = true },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DayMarkView(mark, hc, MarkColors(colors.outlineVariant, colors.onSurfaceVariant, colors.outline), Modifier.size(16.dp))
                            Text(
                                "$outcome · ${form.date.format(datePattern("EEEE, d MMM"))}",
                                style = type.bodyMedium,
                                color = hc.ink.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                    IconButton(onClick = { pickingHabit = true }) {
                        Icon(painterResource(R.drawable.ic_chevron_down), contentDescription = stringResource(R.string.choose_habit), tint = hc.ink, modifier = Modifier.size(22.dp))
                    }
                }
                val noteLabel = stringResource(R.string.note)
                val textStyle = type.bodyLarge.copy(fontSize = 18.sp, lineHeight = 1.55.em, color = colors.onSurface)
                BasicTextField(
                    value = form.text,
                    onValueChange = viewModel::setText,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(hc.solid),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp)
                        .focusRequester(focus)
                        .semantics { contentDescription = noteLabel },
                    decorationBox = { field ->
                        Box {
                            if (form.text.isEmpty()) Text(stringResource(R.string.note_placeholder), style = textStyle.copy(color = colors.onSurfaceVariant))
                            field()
                        }
                    },
                )
                // A new note opens ready to type; the field only exists once the habit is known.
                LaunchedEffect(Unit) { if (form.isNew) focus.requestFocus() }
            }
        }
    }

    if (pickingHabit) {
        HabitPickerSheet(
            habits = habits,
            selectedId = form.habitId,
            onPick = { id -> viewModel.setHabit(id); pickingHabit = false },
            onDismiss = { pickingHabit = false },
        )
    }

    if (pickingDate) {
        DatePickerSheet(
            date = form.date,
            weekStart = weekStart,
            onApply = { viewModel.setDate(it); pickingDate = false },
            onDismiss = { pickingDate = false },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_note_title)) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text(stringResource(R.string.action_delete), color = colors.error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}


/** Every habit in a scrollable sheet; a dropdown got cut off with more than a handful. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitPickerSheet(
    habits: List<app.sprout.habits.data.Habit>,
    selectedId: Long,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    // Fully open right away so every habit is visible; the list scrolls if there are many.
    SproutSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.choose_habit),
            style = MaterialTheme.typography.titleLarge,
            color = colors.onSurface,
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
        )
        LazyColumn(Modifier.weight(1f, fill = false), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp)) {
            items(habits, key = { it.id }) { h ->
                val c = habitColors(h.colorHue.toFloat())
                val selected = h.id == selectedId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) c.soft else colors.surfaceContainerLowest)
                        .selectable(selected, role = Role.RadioButton) { onPick(h.id) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(36.dp).background(c.soft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(HabitIcon.fromKey(h.icon).drawable), contentDescription = null, tint = c.ink, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        h.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                        modifier = Modifier.weight(1f).padding(start = 14.dp),
                    )
                    if (selected) {
                        Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = c.solid, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
