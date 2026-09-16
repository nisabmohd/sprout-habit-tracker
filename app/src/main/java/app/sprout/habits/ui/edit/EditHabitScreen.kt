package app.sprout.habits.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sprout.habits.R
import app.sprout.habits.ui.components.CappedFontScale
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.data.TrackType
import app.sprout.habits.notify.Notifications
import app.sprout.habits.notify.rememberNotificationPermission
import app.sprout.habits.ui.manage.DeleteHabitDialog
import app.sprout.habits.ui.theme.habitColors
import app.sprout.habits.ui.today.TodayViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun EditHabitScreen(viewModel: EditHabitViewModel, onClose: () -> Unit) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.saved.collect { onClose() } }
    if (!form.loaded) return
    var deleting by rememberSaveable { mutableStateOf(false) }
    EditHabitContent(
        form = form,
        onEdit = viewModel::edit,
        onToggleDay = viewModel::toggleDay,
        onSave = viewModel::save,
        onClose = onClose,
        archiveLabel = if (viewModel.isArchived) "Restore" else "Archive",
        onArchive = viewModel::archive,
        onDelete = { deleting = true },
    )
    if (deleting) {
        DeleteHabitDialog(form.name, onConfirm = { deleting = false; viewModel.delete() }, onDismiss = { deleting = false })
    }
}

@Composable
private fun EditHabitContent(
    form: HabitForm,
    onEdit: ((HabitForm) -> HabitForm) -> Unit,
    onToggleDay: (DayOfWeek) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    archiveLabel: String,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val hc = habitColors(form.hue.toFloat())
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    val notifications = rememberNotificationPermission()
    val context = LocalContext.current
    var exactAlarms by remember { mutableStateOf(canScheduleExactAlarms(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { exactAlarms = canScheduleExactAlarms(context) }
    /** Turning on anything that notifies asks for the permission first, if needed. */
    fun needsNotifications(on: Boolean) {
        if (on && !notifications.granted) notifications.request()
    }

    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(start = 8.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = "Close", modifier = Modifier.size(22.dp))
            }
            Text(
                if (form.isNew) "New habit" else "Edit habit",
                style = type.titleLarge,
                color = colors.onBackground,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            Button(onClick = onSave, enabled = form.canSave, modifier = Modifier.height(40.dp)) {
                Text("Save", style = type.labelLarge)
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).background(hc.soft, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(form.icon.drawable), contentDescription = null, tint = hc.ink, modifier = Modifier.size(28.dp))
                }
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { v -> onEdit { it.copy(name = v) } },
                    label = { Text("Name") },
                    singleLine = true,
                    textStyle = type.titleMedium,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
            }

            Section("Icon") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HabitIcon.entries.forEach { icon ->
                        val selected = icon == form.icon
                        Box(
                            Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) hc.soft else colors.surfaceContainerLowest)
                                .then(if (selected) Modifier.border(2.dp, hc.solid, RoundedCornerShape(12.dp)) else Modifier)
                                .clickable(role = Role.RadioButton) { onEdit { it.copy(icon = icon) } }
                                .semantics { contentDescription = icon.key; this.selected = selected },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painterResource(icon.drawable),
                                contentDescription = null,
                                tint = if (selected) hc.ink else colors.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            Section("Color") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    HABIT_HUES.forEach { hue ->
                        val selected = hue == form.hue
                        val swatch = habitColors(hue.toFloat())
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(swatch.solid)
                                .border(3.dp, if (selected) colors.onBackground else Color.Transparent, CircleShape)
                                .clickable(role = Role.RadioButton) { onEdit { it.copy(hue = hue) } }
                                .semantics { contentDescription = "Color $hue"; this.selected = selected },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = swatch.on, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Section("How do you track it?") {
                val options = listOf(TrackType.CHECK to "Check off", TrackType.AMOUNT to "Amount", TrackType.DURATION to "Duration")
                // Each segment is a fixed share of the row, so labels stop scaling at 1.3x.
                CappedFontScale {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = form.trackType == value,
                            onClick = { onEdit { it.copy(trackType = value) } },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                            icon = {},
                        ) { Text(label, style = type.labelLarge) }
                    }
                }
                }
                when (form.trackType) {
                    TrackType.AMOUNT -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("Target", form.target, Modifier.weight(1f)) { v -> onEdit { it.copy(target = v) } }
                        OutlinedTextField(
                            value = form.unit,
                            onValueChange = { v -> onEdit { it.copy(unit = v) } },
                            label = { Text("Unit") },
                            placeholder = { Text("pages") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(2f),
                        )
                    }
                    TrackType.DURATION -> NumberField("Minutes", form.target, Modifier.fillMaxWidth()) { v -> onEdit { it.copy(target = v) } }
                    TrackType.CHECK -> Unit
                }
                Text("You can always log part of it with a long press.", style = type.labelMedium, fontWeight = FontWeight.Normal, color = colors.onSurfaceVariant)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Text("Days", style = type.titleSmall, color = colors.onBackground, modifier = Modifier.weight(1f))
                    Text(daysLabel(form.daysMask), style = type.bodyMedium, color = colors.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    orderedDays(form.weekStart).forEach { day ->
                        val on = form.daysMask and (1 shl (day.value - 1)) != 0
                        // 48 dp touch target around the 44 dp circle from the design.
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .semantics { contentDescription = day.getDisplayName(TextStyle.FULL, Locale.getDefault()) }
                                .toggleable(value = on, role = Role.Checkbox) { onToggleDay(day) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (on) hc.solid else colors.surfaceContainerLowest)
                                    .border(1.dp, if (on) hc.solid else colors.outlineVariant, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                    style = type.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (on) hc.on else colors.onSurface,
                                    // The full day name is on the toggle; don't also read "M".
                                    modifier = Modifier.clearAndSetSemantics {},
                                )
                            }
                        }
                    }
                }
            }

            Section("Reminder") {
                Card {
                    Row(
                        Modifier.fillMaxWidth().clickable { pickingTime = true }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(painterResource(R.drawable.ic_bell), contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(22.dp))
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(TodayViewModel.formatTime(form.reminderMinutes), style = type.titleLarge, color = colors.onSurface)
                            Text("On the days above · tap to change", style = type.bodyMedium, color = colors.onSurfaceVariant)
                        }
                        Switch(checked = form.reminderOn, onCheckedChange = { v -> needsNotifications(v); onEdit { it.copy(reminderOn = v) } })
                    }
                }
            }

            if (form.reminderOn && notifications.granted && !exactAlarms) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Reminders can be up to 10 minutes late unless you allow exact alarms.",
                        style = type.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { Notifications.openExactAlarmSettings(context) }) { Text("Allow") }
                }
            }
            if ((form.reminderOn || form.askForNote) && !notifications.granted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Notifications are off, so reminders won't show.",
                        style = type.bodyMedium,
                        color = colors.error,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { Notifications.openSettings(context) }) { Text("Turn on") }
                }
            }

            Card {
                ToggleRow("Ask for a note when I skip", "A reminder to write why", form.askForNote) { v -> needsNotifications(v); onEdit { it.copy(askForNote = v) } }
                HorizontalDivider(color = colors.surfaceContainerHigh)
                ToggleRow("Show on home screen widget", "Week view and Today widget", form.showOnWidget) { v -> onEdit { it.copy(showOnWidget = v) } }
            }

            if (!form.isNew) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onArchive, modifier = Modifier.weight(1f).height(48.dp)) {
                        Text(archiveLabel, style = type.labelLarge)
                    }
                    OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f).height(48.dp)) {
                        Text("Delete", style = type.labelLarge, color = colors.error)
                    }
                }
            }
        }
    }

    if (pickingTime) {
        TimeDialog(
            initialMinutes = form.reminderMinutes,
            onConfirm = { minutes ->
                needsNotifications(true)
                onEdit { it.copy(reminderMinutes = minutes, reminderOn = true) }
                pickingTime = false
            },
            onDismiss = { pickingTime = false },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
        content()
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest),
        content = content,
    )
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || it == '.' || it == ',' }) },
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(initialMinutes: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = initialMinutes / 60, initialMinute = initialMinutes % 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state) },
    )
}

private fun orderedDays(weekStart: DayOfWeek): List<DayOfWeek> = List(7) { weekStart.plus(it.toLong()) }

private fun daysLabel(mask: Int): String = when (mask) {
    0b111_1111 -> "Every day"
    0b001_1111 -> "Weekdays"
    0b110_0000 -> "Weekends"
    0 -> "Pick at least one day"
    else -> "${Integer.bitCount(mask)} days a week"
}

private fun canScheduleExactAlarms(context: android.content.Context): Boolean =
    android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S ||
        context.getSystemService(android.app.AlarmManager::class.java).canScheduleExactAlarms()
