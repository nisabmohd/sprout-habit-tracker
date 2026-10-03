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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import app.sprout.habits.ui.components.BarSegment
import app.sprout.habits.ui.components.CappedFontScale
import app.sprout.habits.ui.components.SegmentBar
import app.sprout.habits.ui.theme.habitColors
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(viewModel: InsightsViewModel, weekStart: java.time.DayOfWeek) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ui = state ?: return
    var pickingRange by remember { mutableStateOf(false) }
    var filtering by remember { mutableStateOf(false) }
    val res = LocalContext.current.resources
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") {
            TabHeader(stringResource(R.string.insights_title), subtitle = ui.rangeLabel, listSpacing = 16.dp) {
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
private fun ScoreCard(ui: InsightsUi) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val summary = buildList {
        add(pluralStringResource(R.plurals.count_done, ui.doneCount, ui.doneCount))
        add(pluralStringResource(R.plurals.count_partial, ui.partialCount, ui.partialCount))
    }.joinToString(" · ")
    Column(
        Modifier.fillMaxWidth().background(colors.surfaceContainerLowest, RoundedCornerShape(24.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.percent, ui.scorePercent), style = type.headlineMedium, color = colors.onSurface, softWrap = false)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.insights_average), style = type.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            Spacer(Modifier.width(12.dp))
            // Takes the rest of the row and wraps at large text sizes.
            Text(summary, style = type.titleSmall, color = colors.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.weight(1f).padding(bottom = 4.dp))
        }
        // One segment per habit, filled up to that habit's score for the range.
        SegmentBar(ui.rates.map { BarSegment(it.hue, it.percent / 100f) }, Modifier.clearAndSetSemantics {})
    }
}

/** Laid out like a Habits → Week card: a header row, then one column per weekday. */
@Composable
private fun DayBars(ui: InsightsUi) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val res = LocalContext.current.resources
    val done = colors.primary
    val partial = colors.primaryContainer
    Column(
        Modifier.fillMaxWidth().background(colors.surfaceContainerLowest, RoundedCornerShape(24.dp)).padding(start = 12.dp, top = 16.dp, end = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(colors.primaryContainer, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_nav_insights), contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(18.dp))
            }
            Text(
                stringResource(if (ui.averaged) R.string.insights_per_day_avg else R.string.insights_per_day),
                style = type.titleMedium,
                color = colors.onSurface,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            )
            ui.bestDay?.let { Text(stringResource(R.string.insights_best_day, it), style = type.bodyMedium, color = colors.onSurfaceVariant) }
        }
        // 13 dp per habit, shrinking only when a day has more than fits the bar area.
        val max = ui.bars.maxOf { it.done + it.partial }
        val unit = if (max * 13f > BAR_AREA) BAR_AREA / max else 13f
        // Seven equal columns that share the card's width; the labels stop scaling at 1.3x so
        // they never wrap.
        CappedFontScale {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                ui.bars.forEach { bar ->
                    val total = bar.done + bar.partial
                    val value = if (ui.averaged) "%.1f".format(total) else total.roundToInt().toString()
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (bar.isBest) colors.surfaceContainerHigh else colors.surfaceContainerLowest)
                            .padding(vertical = 8.dp)
                            .clearAndSetSemantics {
                                contentDescription = res.getString(R.string.insights_bar_description, bar.name, bar.done.fmt(), bar.partial.fmt())
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(bar.label, style = type.labelSmall, color = if (bar.isBest) colors.onSurface else colors.onSurfaceVariant, maxLines = 1, softWrap = false)
                        Canvas(Modifier.width(20.dp).height(BAR_AREA.dp)) {
                            val gap = 2.dp.toPx()
                            val r = CornerRadius(size.width / 2)
                            val dh = bar.done * unit.dp.toPx()
                            val ph = bar.partial * unit.dp.toPx()
                            if (dh > 0f) drawRoundRect(done, Offset(0f, size.height - dh), Size(size.width, dh), r)
                            if (ph > 0f) {
                                val top = size.height - dh - (if (dh > 0f) gap else 0f) - ph
                                drawRoundRect(partial, Offset(0f, top.coerceAtLeast(0f)), Size(size.width, ph), r)
                            }
                        }
                        Text(value, style = type.titleSmall, color = colors.onSurface, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
            LegendDot(stringResource(R.string.outcome_done), done)
            LegendDot(stringResource(R.string.outcome_partial), partial)
        }
    }
}

/** Height of the bar area in dp. */
private const val BAR_AREA = 100f

private fun Float.fmt() = if (this == ceil(this)) toInt().toString() else "%.1f".format(this)

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
    }
}

/** One card, a row per habit: tile, name and score, with a thin bar in the habit's color. */
@Composable
private fun ByHabit(rates: List<HabitRateUi>) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val track = colors.surfaceContainerHigh
    Column(
        Modifier.fillMaxWidth().background(colors.surfaceContainerLowest, RoundedCornerShape(24.dp)).padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
    ) {
        Text(stringResource(R.string.insights_by_habit), style = type.titleMedium, color = colors.onSurface, modifier = Modifier.padding(bottom = 4.dp))
        rates.forEach { r ->
            val hc = habitColors(r.hue)
            Row(Modifier.padding(vertical = 10.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).background(hc.soft, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(r.icon), contentDescription = null, tint = hc.ink, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(r.name, style = type.titleSmall, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = 8.dp))
                        Text(stringResource(R.string.percent, r.percent), style = type.bodyMedium, color = colors.onSurfaceVariant, softWrap = false)
                    }
                    Canvas(Modifier.fillMaxWidth().height(6.dp)) {
                        val radius = CornerRadius(size.height / 2)
                        drawRoundRect(track, cornerRadius = radius)
                        val w = size.width * r.percent / 100f
                        if (w > 0f) drawRoundRect(hc.solid, size = Size(w, size.height), cornerRadius = radius)
                    }
                }
            }
        }
    }
}
