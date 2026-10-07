package app.sprout.habits.ui.today

import app.sprout.habits.ui.components.confirm
import app.sprout.habits.ui.components.stepTick
import app.sprout.habits.ui.components.tick
import androidx.compose.ui.platform.LocalHapticFeedback
import app.sprout.habits.ui.theme.SproutType
import androidx.compose.ui.res.stringResource
import app.sprout.habits.ui.components.SproutSheet
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.sprout.habits.R
import app.sprout.habits.ui.components.CappedFontScale
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.data.TrackType
import app.sprout.habits.ui.theme.habitColors
import kotlin.math.roundToInt

/** Log sheet for one habit on one day: outcome, amount (stepper, slider) and an optional note. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSheet(
    sheet: LogSheetUi,
    onSave: (EntryStatus, Double, String) -> Unit,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val hc = habitColors(sheet.hue)
    val hasAmount = sheet.trackType != TrackType.CHECK
    val step = sheet.step

    var status by rememberSaveable(sheet.habitId, sheet.day) { mutableStateOf(sheet.status) }
    var amount by rememberSaveable(sheet.habitId, sheet.day) { mutableDoubleStateOf(sheet.amount) }
    var note by rememberSaveable(sheet.habitId, sheet.day) { mutableStateOf(sheet.noteText) }

    // Right after a skip on a habit that asks for a note, the cursor waits in the note field.
    val noteFocus = remember { FocusRequester() }
    LaunchedEffect(sheet.habitId, sheet.day) {
        if (sheet.focusNote) {
            delay(300)
            noteFocus.requestFocus()
        }
    }

    val haptics = LocalHapticFeedback.current

    fun setAmount(value: Double, slider: Boolean = false) {
        val next = value.coerceIn(0.0, sheet.target)
        // One tick per step the amount actually moves; none at either end.
        if (next != amount) { if (slider) haptics.stepTick() else haptics.tick() }
        amount = next
        status = if (amount >= sheet.target) EntryStatus.DONE else EntryStatus.PARTIAL
    }

    SproutSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(48.dp).background(hc.soft, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(sheet.icon), contentDescription = null, tint = hc.ink, modifier = Modifier.size(24.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(sheet.name, style = SproutType.title, color = colors.onSurface)
                    Text(sheet.subtitle, style = SproutType.supporting, color = colors.onSurfaceVariant)
                }
                // Clears the day back to not logged; only useful when something is logged.
                if (sheet.hasEntry) {
                    val clearLabel = if (sheet.isToday) stringResource(R.string.clear_today_entry, sheet.name) else stringResource(R.string.clear_entry_on, sheet.name, sheet.dayLabel)
                    FilledTonalButton(
                        onClick = onUndo,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(40.dp).semantics { contentDescription = clearLabel },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.surfaceContainerHigh, contentColor = colors.onSurface),
                    ) {
                        Icon(painterResource(R.drawable.ic_undo), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_undo), style = SproutType.label)
                    }
                }
            }

            val options = if (hasAmount) {
                listOf(EntryStatus.DONE to stringResource(R.string.outcome_done), EntryStatus.PARTIAL to stringResource(R.string.outcome_partial), EntryStatus.SKIP to stringResource(R.string.outcome_skip))
            } else {
                listOf(EntryStatus.DONE to stringResource(R.string.outcome_done), EntryStatus.SKIP to stringResource(R.string.outcome_skip))
            }
            // Each segment is a fixed share of the row, so labels stop scaling at 1.3x.
            CappedFontScale {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = status == value,
                        onClick = {
                            if (status != value) haptics.tick()
                            status = value
                            if (value == EntryStatus.DONE) amount = sheet.target
                            if (value == EntryStatus.PARTIAL && amount >= sheet.target) amount = (sheet.target - step).coerceAtLeast(0.0)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        icon = {},
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = hc.mid,
                            activeContentColor = hc.ink,
                            inactiveContainerColor = colors.surfaceContainerLowest,
                        ),
                    ) { Text(label, style = SproutType.label) }
                }
            }
            }

            if (hasAmount && status != EntryStatus.SKIP) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StepButton(R.drawable.ic_minus, stringResource(R.string.less)) { setAmount(amount - step) }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                buildAnnotatedString {
                                    append(TodayViewModel.formatNumber(amount))
                                    withStyle(SpanStyle(fontSize = SproutType.title.fontSize, color = colors.onSurfaceVariant)) {
                                        append(" / ${TodayViewModel.formatNumber(sheet.target)}")
                                    }
                                },
                                style = SproutType.displayNumber,
                                color = colors.onSurface,
                            )
                            val pct = if (sheet.target > 0) (amount / sheet.target * 100).roundToInt() else 0
                            Text(if (sheet.unit.isEmpty()) stringResource(R.string.percent, pct) else stringResource(R.string.unit_percent, sheet.unit, pct), style = SproutType.label, color = colors.onSurfaceVariant)
                        }
                        StepButton(R.drawable.ic_plus, stringResource(R.string.more)) { setAmount(amount + step) }
                    }
                    val sliderSteps = (sheet.target / step).roundToInt() - 1
                    Slider(
                        value = amount.toFloat(),
                        onValueChange = { setAmount((it / step).roundToInt() * step, slider = true) },
                        valueRange = 0f..sheet.target.toFloat(),
                        steps = if (sliderSteps in 1..100) sliderSteps else 0,
                        colors = SliderDefaults.colors(
                            thumbColor = hc.solid,
                            activeTrackColor = hc.solid,
                            inactiveTrackColor = hc.soft,
                            activeTickColor = hc.solid,
                            inactiveTickColor = hc.mid,
                        ),
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text(stringResource(R.string.add_a_note), style = SproutType.body) },
                textStyle = SproutType.body,
                minLines = 2,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = colors.background,
                    focusedContainerColor = colors.background,
                    unfocusedBorderColor = colors.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(noteFocus),
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss, modifier = Modifier.height(44.dp)) {
                    Text(stringResource(R.string.action_cancel), style = SproutType.label)
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        haptics.confirm()
                        onSave(status, amount, note)
                    },
                    modifier = Modifier.height(44.dp),
                    contentPadding = ButtonDefaults.ContentPadding,
                ) {
                    Text(stringResource(R.string.action_save), style = SproutType.label)
                }
            }
        }
    }
}

@Composable
private fun StepButton(icon: Int, label: String, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(painterResource(icon), contentDescription = label, modifier = Modifier.size(22.dp))
    }
}
