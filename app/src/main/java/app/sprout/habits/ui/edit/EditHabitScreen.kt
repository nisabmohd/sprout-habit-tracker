package app.sprout.habits.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import app.sprout.habits.R
import app.sprout.habits.ui.components.HeaderIconButton
import app.sprout.habits.ui.components.CappedFontScale
import app.sprout.habits.ui.components.ColorSwatchRow
import app.sprout.habits.ui.components.Swatch
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
import app.sprout.habits.ui.components.TimePickerSheet

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
        archiveLabel = stringResource(if (viewModel.isArchived) R.string.restore else R.string.archive),
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
    // Tapping anything that isn't a text field ends typing, so the keyboard doesn't come back
    // on the Name field when a dialog closes or a switch flips. Focus moves to the screen itself
    // rather than being cleared: before Android 9, clearing focus hands it straight back to the
    // first text field and the keyboard pops up again.
    val focusSink = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    fun endTyping() {
        focusSink.requestFocus()
        keyboard?.hide()
    }
    fun edit(change: (HabitForm) -> HabitForm) {
        endTyping()
        onEdit(change)
    }
    val notifications = rememberNotificationPermission()
    val context = LocalContext.current
    var exactAlarms by remember { mutableStateOf(canScheduleExactAlarms(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { exactAlarms = canScheduleExactAlarms(context) }
    /** Turning on anything that notifies asks for the permission first, if needed. */
    fun needsNotifications(on: Boolean) {
        if (on && !notifications.granted) notifications.request()
    }

    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().imePadding().focusRequester(focusSink).focusable()) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(start = 16.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderIconButton(R.drawable.ic_close, stringResource(R.string.action_close), onClick = onClose)
            Text(
                stringResource(if (form.isNew) R.string.new_habit else R.string.edit_habit),
                style = type.titleLarge,
                color = colors.onBackground,
                modifier = Modifier.weight(1f).padding(start = 14.dp),
            )
            HeaderIconButton(R.drawable.ic_check, stringResource(R.string.action_save), primary = true, enabled = form.canSave, onClick = onSave)
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
                // The text field keeps 8 dp above its outline for the floating label; match it so the
                // tile lines up with the outline, not with the label space.
                Box(Modifier.padding(top = 8.dp).size(56.dp).background(hc.soft, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(form.icon.drawable), contentDescription = null, tint = hc.ink, modifier = Modifier.size(28.dp))
                }
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { v -> onEdit { it.copy(name = v) } },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    textStyle = type.titleMedium,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
            }

            val moreIcons = stringResource(R.string.more_icons)
            Section(stringResource(R.string.icon)) {
                // Seven suggestions and a More tile that opens every icon in a sheet.
                var pickingIcon by rememberSaveable { mutableStateOf(false) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HabitIcon.suggestedFor(form.icon).forEach { icon ->
                        IconTile(icon, selected = icon == form.icon, colors = hc, modifier = Modifier.weight(1f).aspectRatio(1f)) {
                            edit { it.copy(icon = icon) }
                        }
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surfaceContainerHigh)
                            .clickable(onClickLabel = moreIcons) { endTyping(); pickingIcon = true }
                            .semantics { contentDescription = moreIcons },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.ic_more_horiz), contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(24.dp))
                    }
                }
                if (pickingIcon) {
                    IconPickerSheet(
                        current = form.icon,
                        colors = hc,
                        onPick = { icon -> edit { it.copy(icon = icon) }; pickingIcon = false },
                        onDismiss = { pickingIcon = false },
                    )
                }
            }

            Section(stringResource(R.string.color)) {
                ColorSwatchRow(
                    swatches = HABIT_HUES.map { hue ->
                        val c = habitColors(hue.toFloat())
                        Swatch(c.solid, c.on, colorName(hue))
                    },
                    selected = HABIT_HUES.indexOf(form.hue),
                    onSelect = { i -> edit { it.copy(hue = HABIT_HUES[i]) } },
                )
            }

            Section(stringResource(R.string.how_track)) {
                val options = listOf(TrackType.CHECK to stringResource(R.string.track_check), TrackType.AMOUNT to stringResource(R.string.track_amount))
                // Each segment is a fixed share of the row, so labels stop scaling at 1.3x.
                CappedFontScale {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = form.trackType == value,
                            onClick = { edit { it.copy(trackType = value) } },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                            icon = {},
                        ) { Text(label, style = type.labelLarge) }
                    }
                }
                }
                if (form.trackType == TrackType.AMOUNT) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(stringResource(R.string.target), form.target, Modifier.weight(1f)) { v -> onEdit { it.copy(target = v) } }
                        OutlinedTextField(
                            value = form.unit,
                            onValueChange = { v -> onEdit { it.copy(unit = v) } },
                            label = { Text(stringResource(R.string.unit)) },
                            placeholder = { Text(stringResource(R.string.unit_placeholder)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f),
                        )
                        NumberField(stringResource(R.string.step), form.step, Modifier.weight(0.9f)) { v -> onEdit { it.copy(step = v) } }
                    }
                }
                Text(
                    stringResource(if (form.trackType == TrackType.AMOUNT) R.string.step_hint else R.string.partial_hint),
                    style = type.labelMedium,
                    fontWeight = FontWeight.Normal,
                    color = colors.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Text(stringResource(R.string.days), style = type.titleSmall, color = colors.onBackground, modifier = Modifier.weight(1f))
                    Text(daysLabel(form.daysMask), style = type.bodyMedium, color = colors.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth()) {
                    orderedDays(form.weekStart).forEach { day ->
                        val on = form.daysMask and (1 shl (day.value - 1)) != 0
                        // Seven equal cells, so the row fits narrow phones; each is a 48 dp target.
                        Box(
                            Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(CircleShape)
                                .semantics { contentDescription = day.getDisplayName(TextStyle.FULL, Locale.getDefault()) }
                                .toggleable(value = on, role = Role.Checkbox) { endTyping(); onToggleDay(day) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .padding(2.dp)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(if (on) hc.solid else colors.surfaceContainerLowest)
                                    .border(1.dp, if (on) hc.solid else colors.outlineVariant, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                CappedFontScale {
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
            }

            Section(stringResource(R.string.reminder)) {
                Card {
                    Row(
                        Modifier.fillMaxWidth().clickable { endTyping(); pickingTime = true }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(painterResource(R.drawable.ic_bell), contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(22.dp))
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(TodayViewModel.formatTime(form.reminderMinutes), style = type.titleLarge, color = colors.onSurface)
                            Text(stringResource(R.string.reminder_hint), style = type.bodyMedium, color = colors.onSurfaceVariant)
                        }
                        Switch(checked = form.reminderOn, onCheckedChange = { v -> needsNotifications(v); edit { it.copy(reminderOn = v) } })
                    }
                }
            }

            if (form.reminderOn && notifications.granted && !exactAlarms) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.exact_alarm_hint),
                        style = type.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { Notifications.openExactAlarmSettings(context) }) { Text(stringResource(R.string.allow)) }
                }
            }
            if ((form.reminderOn || form.askForNote) && !notifications.granted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.notifications_off_hint),
                        style = type.bodyMedium,
                        color = colors.error,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { Notifications.openSettings(context) }) { Text(stringResource(R.string.turn_on)) }
                }
            }

            Card {
                ToggleRow(stringResource(R.string.ask_note), stringResource(R.string.ask_note_desc), form.askForNote) { v -> needsNotifications(v); edit { it.copy(askForNote = v) } }
                HorizontalDivider(color = colors.surfaceContainerHigh)
                ToggleRow(stringResource(R.string.show_widget), stringResource(R.string.show_widget_desc), form.showOnWidget) { v -> edit { it.copy(showOnWidget = v) } }
            }

            if (!form.isNew) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onArchive, modifier = Modifier.weight(1f).height(48.dp)) {
                        Text(archiveLabel, style = type.labelLarge)
                    }
                    OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f).height(48.dp)) {
                        Text(stringResource(R.string.action_delete), style = type.labelLarge, color = colors.error)
                    }
                }
            }
        }
    }

    if (pickingTime) {
        TimePickerSheet(
            title = stringResource(R.string.reminder_time),
            initialMinutes = form.reminderMinutes,
            onApply = { minutes ->
                needsNotifications(true)
                edit { it.copy(reminderMinutes = minutes, reminderOn = true) }
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


private fun orderedDays(weekStart: DayOfWeek): List<DayOfWeek> = List(7) { weekStart.plus(it.toLong()) }

@Composable
private fun daysLabel(mask: Int): String = when (mask) {
    0b111_1111 -> stringResource(R.string.every_day)
    0b001_1111 -> stringResource(R.string.weekdays)
    0b110_0000 -> stringResource(R.string.weekends)
    0 -> stringResource(R.string.pick_one_day)
    else -> Integer.bitCount(mask).let { pluralStringResource(R.plurals.days_a_week, it, it) }
}

private fun canScheduleExactAlarms(context: android.content.Context): Boolean =
    android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S ||
        context.getSystemService(android.app.AlarmManager::class.java).canScheduleExactAlarms()

/** Names TalkBack reads for the habit colors. */
@Composable
private fun colorName(hue: Int) = stringResource(
    when (hue) {
        275 -> R.string.color_purple
        215 -> R.string.color_blue
        192 -> R.string.color_teal
        150 -> R.string.color_green
        95 -> R.string.color_lime
        38 -> R.string.color_gold
        12 -> R.string.color_rust
        330 -> R.string.color_pink
        else -> R.string.color_color
    },
)
