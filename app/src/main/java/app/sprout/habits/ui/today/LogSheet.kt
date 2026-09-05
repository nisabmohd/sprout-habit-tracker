package app.sprout.habits.ui.today

import androidx.compose.foundation.background
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

/** Press-and-hold sheet: outcome, amount (stepper, slider, quick chips) and an optional note. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSheet(
    sheet: LogSheetUi,
    onSave: (EntryStatus, Double, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val hc = habitColors(sheet.hue)
    val hasAmount = sheet.trackType != TrackType.CHECK
    val step = if (sheet.trackType == TrackType.DURATION) 5.0 else 1.0

    var status by rememberSaveable(sheet.habitId) { mutableStateOf(sheet.status) }
    var amount by rememberSaveable(sheet.habitId) { mutableDoubleStateOf(sheet.amount) }
    var note by rememberSaveable(sheet.habitId) { mutableStateOf(sheet.noteText) }

    fun setAmount(value: Double) {
        amount = value.coerceIn(0.0, sheet.target)
        status = if (amount >= sheet.target) EntryStatus.DONE else EntryStatus.PARTIAL
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surfaceContainerLowest,
    ) {
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
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(sheet.name, style = type.titleLarge, color = colors.onSurface)
                    val goal = if (hasAmount) "Goal ${TodayViewModel.formatNumber(sheet.target)} ${sheet.unit} · " else ""
                    Text("$goal${sheet.dayLabel}", style = type.bodyMedium, color = colors.onSurfaceVariant)
                }
            }

            val options = if (hasAmount) {
                listOf(EntryStatus.DONE to "Done", EntryStatus.PARTIAL to "Partial", EntryStatus.SKIP to "Skip")
            } else {
                listOf(EntryStatus.DONE to "Done", EntryStatus.SKIP to "Skip")
            }
            // Each segment is a fixed share of the row, so labels stop scaling at 1.3x.
            CappedFontScale {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = status == value,
                        onClick = {
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
                    ) { Text(label, style = type.labelLarge) }
                }
            }
            }

            if (hasAmount && status != EntryStatus.SKIP) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StepButton(R.drawable.ic_minus, "Less") { setAmount(amount - step) }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                buildAnnotatedString {
                                    append(TodayViewModel.formatNumber(amount))
                                    withStyle(SpanStyle(fontSize = type.titleLarge.fontSize, color = colors.onSurfaceVariant)) {
                                        append(" / ${TodayViewModel.formatNumber(sheet.target)}")
                                    }
                                },
                                style = type.displayLarge,
                                color = colors.onSurface,
                            )
                            val pct = if (sheet.target > 0) (amount / sheet.target * 100).roundToInt() else 0
                            Text("${sheet.unit} · $pct%".trimStart(' ', '·'), style = type.titleSmall, color = colors.onSurfaceVariant)
                        }
                        StepButton(R.drawable.ic_plus, "More") { setAmount(amount + step) }
                    }
                    val sliderSteps = (sheet.target / step).roundToInt() - 1
                    Slider(
                        value = amount.toFloat(),
                        onValueChange = { setAmount((it / step).roundToInt() * step) },
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.25, 0.5, 0.75, 1.0).forEach { fraction ->
                            val value = (sheet.target * fraction / step).roundToInt() * step
                            FilterChip(
                                selected = amount == value,
                                onClick = { setAmount(value) },
                                label = { Text(TodayViewModel.formatNumber(value), style = type.titleSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = hc.mid,
                                    selectedLabelColor = hc.ink,
                                ),
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Add a note", style = type.bodyLarge) },
                textStyle = type.bodyLarge,
                minLines = 2,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = colors.background,
                    focusedContainerColor = colors.background,
                    unfocusedBorderColor = colors.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss, modifier = Modifier.height(44.dp)) {
                    Text("Cancel", style = type.labelLarge)
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = { onSave(status, amount, note) },
                    modifier = Modifier.height(44.dp),
                    contentPadding = ButtonDefaults.ContentPadding,
                ) {
                    Text("Save", style = type.labelLarge)
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
