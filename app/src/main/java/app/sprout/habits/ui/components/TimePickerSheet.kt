package app.sprout.habits.ui.components

import app.sprout.habits.ui.theme.SproutType
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.sprout.habits.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols

/**
 * Pick a time of day (minutes after midnight), for reminders. A sheet, like every other picker.
 * Hour and minute are steppers: tap the chevrons (hold to repeat) or drag a number up or down.
 * AM/PM shows only when the phone uses the 12-hour clock.
 */
@Composable
fun TimePickerSheet(title: String, initialMinutes: Int, onApply: (Int) -> Unit, onDismiss: () -> Unit) {
    var hour by rememberSaveable { mutableIntStateOf(initialMinutes / 60) }
    var minute by rememberSaveable { mutableIntStateOf(initialMinutes % 60) }
    var editingMinute by rememberSaveable { mutableStateOf(false) }
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val locale = LocalConfiguration.current.locales[0]

    SproutSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                style = SproutType.title,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val shownHour = if (is24Hour) hour else (hour + 11) % 12 + 1
                TimeField(
                    value = String.format(locale, "%02d", shownHour),
                    label = stringResource(R.string.time_hour),
                    selected = !editingMinute,
                    onSelect = { editingMinute = false },
                    onStep = { delta ->
                        // In 12-hour mode the hour cycles 1–12 and keeps AM/PM, like a clock face.
                        hour = if (is24Hour) (hour + delta).mod(24) else (hour - hour % 12) + (hour % 12 + delta).mod(12)
                    },
                )
                Text(":", style = SproutType.display, color = MaterialTheme.colorScheme.onSurface)
                TimeField(
                    value = String.format(locale, "%02d", minute),
                    label = stringResource(R.string.time_minute),
                    selected = editingMinute,
                    onSelect = { editingMinute = true },
                    onStep = { delta -> minute = (minute + delta).mod(60) },
                )
                if (!is24Hour) {
                    val (am, pm) = DateFormatSymbols.getInstance(locale).amPmStrings.let { it[0] to it[1] }
                    AmPmToggle(am, pm, isPm = hour >= 12, onChange = { pm -> hour = hour % 12 + if (pm) 12 else 0 })
                }
            }
            Button(
                onClick = { onApply(hour * 60 + minute) },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp).height(48.dp),
            ) {
                Text(stringResource(R.string.set_time), style = SproutType.label)
            }
        }
    }
}

/** Chevron up, the number in a tinted tile, chevron down. The selected field uses the primary container. */
@Composable
private fun TimeField(value: String, label: String, selected: Boolean, onSelect: () -> Unit, onStep: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val step by rememberUpdatedState(onStep)
    val stepPx = with(LocalDensity.current) { 24.dp.toPx() }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StepButton(R.drawable.ic_chevron_up, "$label, ${stringResource(R.string.time_later)}") { onSelect(); step(1) }
        Box(
            Modifier
                .size(width = 96.dp, height = 80.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (selected) colors.primaryContainer else colors.surfaceContainerHigh)
                .clickable(onClickLabel = label, onClick = onSelect)
                .pointerInput(Unit) {
                    // Drag up for later, down for earlier: one step per 24dp.
                    var travelled = 0f
                    detectVerticalDragGestures(
                        onDragStart = { onSelect(); travelled = 0f },
                        onVerticalDrag = { change, dy ->
                            change.consume()
                            travelled -= dy
                            while (travelled >= stepPx) { step(1); travelled -= stepPx }
                            while (travelled <= -stepPx) { step(-1); travelled += stepPx }
                        },
                    )
                }
                .semantics { contentDescription = "$label, $value"; this.selected = selected },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                value,
                style = SproutType.display,
                color = if (selected) colors.onPrimaryContainer else colors.onSurface,
            )
        }
        StepButton(R.drawable.ic_chevron_down, "$label, ${stringResource(R.string.time_earlier)}") { onSelect(); step(-1) }
    }
}

/** 48dp chevron that steps once on tap and keeps stepping while held. */
@Composable
private fun StepButton(icon: Int, description: String, onStep: () -> Unit) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val step by rememberUpdatedState(onStep)
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    step()
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val repeat = scope.launch {
                        delay(400)
                        while (true) { step(); delay(70) }
                    }
                    tryAwaitRelease()
                    repeat.cancel()
                })
            }
            .semantics {
                role = Role.Button
                contentDescription = description
                onClick { step(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Outlined vertical pill with AM on top and PM below; the chosen half is tinted. */
@Composable
private fun AmPmToggle(am: String, pm: String, isPm: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .width(56.dp)
            .clip(shape)
            .border(BorderStroke(1.dp, colors.outline), shape),
    ) {
        listOf(am to false, pm to true).forEachIndexed { index, (text, value) ->
            if (index == 1) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outline))
            val chosen = isPm == value
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(if (chosen) colors.primaryContainer else colors.surface)
                    .clickable(role = Role.RadioButton) { onChange(value) }
                    .semantics { selected = chosen },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text,
                    style = SproutType.label,
                    color = if (chosen) colors.onPrimaryContainer else colors.onSurface,
                )
            }
        }
    }
}
