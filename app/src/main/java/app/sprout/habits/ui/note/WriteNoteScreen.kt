package app.sprout.habits.ui.note

import androidx.compose.foundation.background
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.ui.theme.habitColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteNoteScreen(viewModel: WriteNoteViewModel, onClose: () -> Unit) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.done.collect { onClose() } }
    if (!form.loaded) return

    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    var pickingHabit by remember { mutableStateOf(false) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (form.isNew) focus.requestFocus() }

    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(start = 8.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = "Close", modifier = Modifier.size(22.dp))
            }
            Text(
                if (form.isNew) "New note" else "Edit note",
                style = type.titleLarge,
                color = colors.onBackground,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            if (!form.isNew) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(painterResource(R.drawable.ic_more_vert), contentDescription = "More options", modifier = Modifier.size(22.dp))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Delete", color = colors.error) }, onClick = { menuOpen = false; confirmDelete = true })
                    }
                }
            }
            Button(onClick = viewModel::save, enabled = form.canSave, modifier = Modifier.height(40.dp)) {
                Text("Save", style = type.labelLarge)
            }
        }

        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val habit = habits.firstOrNull { it.id == form.habitId }
            if (habit == null) {
                Text("Add a habit first, then write a note about it.", style = type.bodyLarge, color = colors.onSurfaceVariant)
            } else {
                val hc = habitColors(habit.colorHue.toFloat())
                Box {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(hc.soft)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(hc.mid)
                                .clickable(onClickLabel = "Choose habit") { pickingHabit = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(HabitIcon.fromKey(habit.icon).drawable), contentDescription = null, tint = hc.ink, modifier = Modifier.size(24.dp))
                        }
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(
                                habit.name,
                                style = type.titleMedium,
                                color = hc.ink,
                                modifier = Modifier.clickable(onClickLabel = "Choose habit") { pickingHabit = true },
                            )
                            Text(
                                listOfNotNull(form.date.format(DateTimeFormatter.ofPattern("EEEE, d MMM")), status?.label()).joinToString(" · "),
                                style = type.titleSmall,
                                color = hc.ink.copy(alpha = 0.8f),
                                modifier = Modifier.clickable(onClickLabel = "Change date") { pickingDate = true },
                            )
                        }
                        IconButton(onClick = { pickingHabit = true }) {
                            Icon(painterResource(R.drawable.ic_chevron_down), contentDescription = "Choose habit", tint = hc.ink, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            OutlinedTextField(
                value = form.text,
                onValueChange = viewModel::setText,
                placeholder = { Text("How did it go?", style = type.bodyLarge) },
                textStyle = type.bodyLarge,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = colors.surfaceContainerLowest,
                    focusedContainerColor = colors.surfaceContainerLowest,
                    unfocusedBorderColor = colors.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false).height(420.dp).focusRequester(focus),
            )
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
        val today = LocalDate.now()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = form.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { viewModel.setDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    pickingDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this note?") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text("Delete", color = colors.error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

private fun EntryStatus.label() = when (this) {
    EntryStatus.DONE -> "Done"
    EntryStatus.PARTIAL -> "Partial"
    EntryStatus.SKIP -> "Skipped"
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
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surfaceContainerLowest) {
        Text(
            "Choose habit",
            style = MaterialTheme.typography.titleLarge,
            color = colors.onSurface,
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
        )
        LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp)) {
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
