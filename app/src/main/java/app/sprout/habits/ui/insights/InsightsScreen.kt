package app.sprout.habits.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import app.sprout.habits.R
import app.sprout.habits.ui.components.TabHeader
import app.sprout.habits.ui.components.DateRangeSheet
import app.sprout.habits.ui.components.HabitFilterSheet
import app.sprout.habits.ui.components.HeaderIconButton
import app.sprout.habits.ui.components.ProgressRing
import app.sprout.habits.ui.theme.habitColors
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(viewModel: InsightsViewModel, weekStart: java.time.DayOfWeek) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ui = state ?: return
    var pickingRange by remember { mutableStateOf(false) }
    var filtering by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val res = LocalContext.current.resources
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") {
            TabHeader(stringResource(R.string.insights_title), subtitle = ui.rangeLabel, listSpacing = 14.dp) {
                HeaderIconButton(R.drawable.ic_calendar, stringResource(R.string.change_date_range)) { pickingRange = true }
                HeaderIconButton(R.drawable.ic_filter, stringResource(R.string.filter_by_habit), active = ui.filter.isNotEmpty()) { filtering = true }
            }
        }
        item(key = "score") { ScoreCard(ui) }
        item(key = "bars") { DayBars(ui) }
        if (ui.rates.size > 1) item(key = "rates") { ByHabit(ui.rates) }
    }

    if (pickingRange) {
        DateRangeSheet(
            from = ui.from,
            to = ui.to,
            weekStart = weekStart,
            shortcutLabel = stringResource(R.string.this_week),
            onApply = { a, b -> viewModel.setRange(a, b); pickingRange = false },
            onShortcut = { viewModel.setRange(null, null); pickingRange = false },
            onDismiss = { pickingRange = false },
        )
    }
    if (filtering) {
        HabitFilterSheet(
            options = ui.options,
            selected = ui.filter,
            applyLabel = { n -> if (n == 0) res.getString(R.string.show_all_habits) else res.getQuantityString(R.plurals.show_habits, n, n) },
            onApply = { ids -> viewModel.setFilter(ids); filtering = false },
            onDismiss = { filtering = false },
        )
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(24.dp)).padding(18.dp),
    ) { content() }
}

@Composable
private fun ScoreCard(ui: InsightsUi) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                ProgressRing(ui.scorePercent / 100f, colors.primary, colors.surfaceContainerHigh, 10.dp, Modifier.fillMaxSize())
                Text(stringResource(R.string.percent, ui.scorePercent), style = type.headlineMedium, color = colors.onSurface)
            }
            Column(Modifier.padding(start = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.insights_average), style = type.titleSmall, color = colors.onSurfaceVariant)
                val habits = pluralStringResource(R.plurals.habit_count, ui.habitCount, ui.habitCount)
                Text(stringResource(R.string.insights_summary, ui.doneCount, ui.partialCount, habits), style = type.bodyLarge, color = colors.onSurface)
                ui.bestDay?.let { Text(stringResource(R.string.insights_best_day, it), style = type.titleSmall, color = colors.primary) }
            }
        }
    }
}

@Composable
private fun DayBars(ui: InsightsUi) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val res = LocalContext.current.resources
    val done = colors.primary
    val partial = colors.primaryContainer
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (ui.averaged) R.string.insights_per_day_avg else R.string.insights_per_day), style = type.titleMedium, color = colors.onSurface, modifier = Modifier.weight(1f))
                LegendDot(stringResource(R.string.outcome_done), done)
                LegendDot(stringResource(R.string.outcome_partial), partial)
            }
            val max = ui.bars.maxOf { it.done + it.partial }.coerceAtLeast(1f)
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                ui.bars.forEach { bar ->
                    val total = bar.done + bar.partial
                    val value = if (ui.averaged) "%.1f".format(total) else total.roundToInt().toString()
                    Column(
                        Modifier.width(30.dp).semantics(mergeDescendants = true) {
                            contentDescription = res.getString(R.string.insights_bar_description, bar.label, bar.done.fmt(), bar.partial.fmt())
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(value, style = type.labelMedium, color = colors.onSurface)
                        Canvas(Modifier.width(26.dp).height(96.dp)) {
                            val gap = 2.dp.toPx()
                            val r = 6.dp.toPx()
                            val dh = size.height * bar.done / max
                            val ph = size.height * bar.partial / max
                            if (dh > 0f) drawRoundRect(done, Offset(0f, size.height - dh), Size(size.width, dh), CornerRadius(r))
                            if (ph > 0f) {
                                val top = size.height - dh - (if (dh > 0f) gap else 0f) - ph
                                drawRoundRect(partial, Offset(0f, top), Size(size.width, ph), CornerRadius(r))
                            }
                        }
                        Text(
                            bar.label,
                            style = type.labelMedium,
                            color = if (bar.isToday) colors.onSurface else colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

private fun Float.fmt() = if (this == ceil(this)) toInt().toString() else "%.1f".format(this)

@Composable
private fun LegendDot(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(10.dp)) { drawRoundRect(color, cornerRadius = CornerRadius(3.dp.toPx())) }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun ByHabit(rates: List<HabitRateUi>) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val track = colors.surfaceContainerHigh
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.insights_by_habit), style = type.titleMedium, color = colors.onSurface)
            rates.forEach { r ->
                val solid = habitColors(r.hue).solid
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) {}) {
                    Text(r.name, style = type.titleSmall, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(108.dp))
                    Canvas(Modifier.weight(1f).height(10.dp).padding(horizontal = 10.dp)) {
                        val radius = CornerRadius(size.height / 2)
                        drawRoundRect(track, cornerRadius = radius)
                        val w = size.width * r.percent / 100f
                        if (w > 0f) drawRoundRect(solid, size = Size(w, size.height), cornerRadius = radius)
                    }
                    Text(
                        stringResource(R.string.percent, r.percent),
                        style = type.titleSmall,
                        color = colors.onSurface,
                        textAlign = TextAlign.End,
                        softWrap = false,
                        modifier = Modifier.widthIn(min = 44.dp),
                    )
                }
            }
        }
    }
}
