package app.sprout.habits.ui.components

import app.sprout.habits.ui.theme.SproutType
import app.sprout.habits.ui.englishDates
import app.sprout.habits.ui.datePattern
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sprout.habits.R
import app.sprout.habits.domain.weekOf
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/**
 * Pick a date range, shared by Insights and Journal: tap a first day, then a last day (or apply
 * a single day). Future days are disabled. The shortcut is "This week" on Insights and
 * "All dates" on Journal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSheet(
    from: LocalDate?,
    to: LocalDate?,
    weekStart: DayOfWeek,
    shortcutLabel: String,
    onApply: (LocalDate, LocalDate) -> Unit,
    onShortcut: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val today = LocalDate.now()
    // Future days can't be picked, so a range passed in is cut off at today.
    var start by remember { mutableStateOf(from?.let { minOf(it, today) }) }
    var end by remember { mutableStateOf(to?.let { minOf(it, today) }) }
    var month by remember { mutableStateOf(YearMonth.from(end ?: today)) }
    val dm = datePattern("d MMM")

    SproutSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.date_range), style = SproutType.title, color = colors.onSurface, modifier = Modifier.weight(1f))
                TextButton(onClick = onShortcut) { Text(shortcutLabel, style = SproutType.label) }
            }
            val s = start
            val e = end ?: start
            // Both months spelled out ("21 Sep – 27 Sep 2026") so it reads as a range.
            Text(
                when {
                    s == null || e == null -> stringResource(R.string.pick_first_day)
                    s == e -> s.format(datePattern("d MMM yyyy"))
                    else -> stringResource(R.string.range_same_year, s.format(dm), e.format(dm), e.year)
                },
                style = SproutType.supporting,
                color = colors.onSurfaceVariant,
            )
            MonthHeader(month, today) { month = it }
            MonthGrid(month, weekStart, today, start, end ?: start) { day ->
                val a = start
                if (a == null || end != null) {
                    start = day
                    end = null
                } else if (day < a) {
                    start = day
                } else {
                    end = day
                }
            }
            Button(
                onClick = { if (s != null && e != null) onApply(s, e) },
                enabled = s != null,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(48.dp),
            ) {
                Text(
                    when {
                        s == null || e == null -> stringResource(R.string.show)
                        s == e -> stringResource(R.string.show_value, s.format(dm))
                        s.month == e.month && englishDates() -> stringResource(R.string.show_value, stringResource(R.string.range_short, s.dayOfMonth.toString(), e.format(dm)))
                        else -> stringResource(R.string.show_value, stringResource(R.string.range_short, s.format(dm), e.format(dm)))
                    },
                    style = SproutType.label,
                )
            }
        }
    }
}

/** Pick one day, for a note's date. Same month grid as [DateRangeSheet]; future days are disabled. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(
    date: LocalDate,
    weekStart: DayOfWeek,
    onApply: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val today = LocalDate.now()
    var picked by remember { mutableStateOf(date) }
    var month by remember { mutableStateOf(YearMonth.from(date)) }

    SproutSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.date), style = SproutType.title, color = colors.onSurface, modifier = Modifier.weight(1f))
                TextButton(onClick = { picked = today; month = YearMonth.from(today) }) { Text(stringResource(R.string.today), style = SproutType.label) }
            }
            Text(picked.format(datePattern("EEEE, d MMM yyyy")), style = SproutType.supporting, color = colors.onSurfaceVariant)
            MonthHeader(month, today) { month = it }
            MonthGrid(month, weekStart, today, picked, picked) { picked = it }
            Button(
                onClick = { onApply(picked) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(48.dp),
            ) {
                Text(stringResource(R.string.use_value, picked.format(datePattern("d MMM"))), style = SproutType.label)
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, today: LocalDate, onMonth: (YearMonth) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onMonth(month.minusMonths(1)) }) {
            Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = stringResource(R.string.previous_month), modifier = Modifier.size(20.dp))
        }
        Text(
            month.format(datePattern("MMMM yyyy")),
            style = SproutType.cardTitle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onMonth(month.plusMonths(1)) }, enabled = month < YearMonth.from(today)) {
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = stringResource(R.string.next_month), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, weekStart: DayOfWeek, today: LocalDate, start: LocalDate?, end: LocalDate?, onPick: (LocalDate) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val band = colors.primaryContainer
    val first = month.atDay(1)
    val lead = (first.dayOfWeek.value - weekStart.value + 7) % 7
    val cells: List<LocalDate?> = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    CappedFontScale {
        Column {
            Row(Modifier.fillMaxWidth()) {
                weekOf(first, weekStart).forEach { d ->
                    Text(
                        d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = SproutType.tiny,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                    )
                }
            }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    for (i in 0 until 7) {
                        val day = week.getOrNull(i)
                        Box(Modifier.weight(1f).aspectRatio(1.15f), contentAlignment = Alignment.Center) {
                            if (day != null) {
                                val future = day.isAfter(today)
                                val isEdge = day == start || day == end
                                val inRange = start != null && end != null && day > start && day < end
                                // The band runs behind the days in the range, including half behind each end.
                                if (start != null && end != null && start != end && (inRange || isEdge)) {
                                    Row(Modifier.fillMaxWidth().height(40.dp)) {
                                        Box(Modifier.weight(1f).fillMaxSize().background(if (day == start) Color.Transparent else band))
                                        Box(Modifier.weight(1f).fillMaxSize().background(if (day == end) Color.Transparent else band))
                                    }
                                }
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isEdge) colors.primary else Color.Transparent)
                                        .clickable(enabled = !future, role = Role.Button) { onPick(day) }
                                        .semantics {
                                            contentDescription = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
                                            selected = isEdge || inRange
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "${day.dayOfMonth}",
                                        style = SproutType.label,
                                        color = when {
                                            isEdge -> colors.onPrimary
                                            future -> colors.outline
                                            inRange -> colors.onPrimaryContainer
                                            else -> colors.onSurface
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
