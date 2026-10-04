package app.sprout.habits.ui.insights

import app.sprout.habits.ui.theme.SproutType
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
import app.sprout.habits.ui.components.HabitFilterChip
import app.sprout.habits.ui.components.DateFilterChip
import app.sprout.habits.ui.components.FilterChipRow
import app.sprout.habits.ui.components.groupedShape
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
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
    // The header stays put while the list scrolls under it, so its buttons are always in reach.
    Column(Modifier.fillMaxSize()) {
    Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)) {
            Column {
                TabHeader(stringResource(R.string.insights_title), listSpacing = 16.dp) {
                    HeaderIconButton(R.drawable.ic_calendar, stringResource(R.string.change_date_range), active = ui.customRange) { pickingRange = true }
                    HeaderIconButton(R.drawable.ic_filter, stringResource(R.string.filter_by_habit), active = ui.filter.isNotEmpty()) { filtering = true }
                }
                // Insights always has a range, so the date chip is always there.
                FilterChipRow(Modifier.padding(top = 12.dp)) {
                    DateFilterChip(
                        label = ui.rangeLabel,
                        openLabel = stringResource(R.string.change_date_range),
                        clearLabel = stringResource(R.string.clear_date_filter),
                        onOpen = { pickingRange = true },
                        // ✕ goes back to this week.
                        onClear = if (ui.customRange) ({ viewModel.setRange(null, null) }) else null,
                    )
                    if (ui.filter.isNotEmpty()) {
                        HabitFilterChip(
                            selected = ui.options.filter { it.habitId in ui.filter },
                            label = pluralStringResource(R.plurals.habit_count, ui.filter.size, ui.filter.size),
                            removeLabel = stringResource(R.string.remove_habit_filter),
                            onOpen = { filtering = true },
                            onClear = { viewModel.setFilter(emptySet()) },
                        )
                    }
                }
            }
        
    }
    LazyColumn(
        Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "score") { ScoreCard(ui) }
        item(key = "bars") { DayBars(ui) }
        if (ui.rates.isNotEmpty()) item(key = "rates") { ByHabit(ui.rates) }
    }
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
    val summary = buildList {
        add(pluralStringResource(R.plurals.count_done, ui.doneCount, ui.doneCount))
        add(pluralStringResource(R.plurals.count_partial, ui.partialCount, ui.partialCount))
    }.joinToString(" · ")
    Column(
        Modifier.fillMaxWidth().background(colors.surfaceContainerLowest, RoundedCornerShape(24.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.percent, ui.scorePercent), style = SproutType.screenTitle, color = colors.onSurface, softWrap = false)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.insights_average), style = SproutType.supporting, color = colors.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            Spacer(Modifier.width(12.dp))
            // Takes the rest of the row and wraps at large text sizes.
            Text(summary, style = SproutType.label, color = colors.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.weight(1f).padding(bottom = 4.dp))
        }
        // One segment per habit, filled up to that habit's score for the range.
        SegmentBar(ui.rates.map { BarSegment(it.hue, it.percent / 100f) }, Modifier.clearAndSetSemantics {})
    }
}

/** Laid out like a Habits → Week card: a header row, then one column per weekday. */
@Composable
private fun DayBars(ui: InsightsUi) {
    val colors = MaterialTheme.colorScheme
    val res = LocalContext.current.resources
    val done = colors.primary
    val partial = colors.primaryContainer
    Column(
        Modifier.fillMaxWidth().background(colors.surfaceContainerLowest, RoundedCornerShape(24.dp)).padding(start = 12.dp, top = 16.dp, end = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(colors.primaryContainer, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_nav_insights), contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(18.dp))
            }
            Text(
                stringResource(
                    when {
                        ui.byWeek -> R.string.insights_per_week
                        ui.averaged -> R.string.insights_per_day_avg
                        else -> R.string.insights_per_day
                    },
                ),
                style = SproutType.cardTitle,
                color = colors.onSurface,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            )
            ui.bestDay?.let { Text(stringResource(R.string.insights_best_day, it), style = SproutType.supporting, color = colors.onSurfaceVariant) }
        }
        // The tallest column fills about 96 dp; a column with only a few habits stops at 24 dp each.
        val max = ui.bars.maxOf { it.done + it.partial }.coerceAtLeast(1f)
        val unit = minOf(24f, 96f / max)
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
                                val name = if (ui.byWeek) res.getString(R.string.insights_week_of, bar.name) else bar.name
                                contentDescription = res.getString(R.string.insights_bar_description, name, bar.done.fmt(), bar.partial.fmt())
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(bar.label, style = SproutType.tiny, color = if (bar.isBest) colors.onSurface else colors.onSurfaceVariant, maxLines = 1, softWrap = false)
                        Canvas(Modifier.width(20.dp).height(BAR_AREA.dp)) {
                            val gap = 2.dp.toPx()
                            val dh = bar.done * unit.dp.toPx()
                            val ph = bar.partial * unit.dp.toPx()
                            // A short segment gets a smaller radius so it stays a pill, not a lens.
                            fun radius(h: Float) = CornerRadius(minOf(size.width, h) / 2)
                            if (dh > 0f) drawRoundRect(done, Offset(0f, size.height - dh), Size(size.width, dh), radius(dh))
                            if (ph > 0f) {
                                val top = size.height - dh - (if (dh > 0f) gap else 0f) - ph
                                drawRoundRect(partial, Offset(0f, top.coerceAtLeast(0f)), Size(size.width, ph), radius(ph))
                            }
                        }
                        Text(value, style = SproutType.label, color = colors.onSurface, maxLines = 1, softWrap = false)
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
        Text(label, style = SproutType.caption, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
    }
}

/**
 * A grouped list, one row per habit: the habit tile, name, "6 of 7 days" and the streak, a
 * progress bar in the habit's color, and the score as plain text on the right.
 */
@Composable
private fun ByHabit(rates: List<HabitRateUi>) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(R.string.insights_by_habit), style = SproutType.label, color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp))
        rates.forEachIndexed { index, r ->
            val hc = habitColors(r.hue)
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceContainerLowest, groupedShape(index, rates.size))
                    .padding(16.dp)
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(44.dp).background(hc.soft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(r.icon), contentDescription = null, tint = hc.ink, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(r.name, style = SproutType.cardTitle, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    // One plain muted line: "6 of 7 days · 5-day streak" (the streak only when over 1).
                    Text(
                        listOfNotNull(
                            pluralStringResource(R.plurals.days_kept, r.scheduledDays, r.keptDays, r.scheduledDays),
                            if (r.streak > 1) pluralStringResource(R.plurals.streak_days, r.streak, r.streak) else null,
                        ).joinToString(" · "),
                        style = SproutType.supporting,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    // The M3 linear indicator: the bar, a 4 dp gap, the rest of the track and a stop
                    // dot at its end. At 100% the bar fills it all.
                    Canvas(Modifier.padding(top = 8.dp).fillMaxWidth().height(6.dp)) {
                        val radius = CornerRadius(size.height / 2)
                        val gap = 4.dp.toPx()
                        val fraction = r.percent.coerceIn(0, 100) / 100f
                        if (fraction >= 1f) {
                            drawRoundRect(hc.solid, cornerRadius = radius)
                        } else {
                            // The bar is at least a dot, and leaves room for a piece of track.
                            val bar = if (fraction > 0f) (size.width * fraction).coerceIn(size.height, size.width - gap - 2 * size.height) else 0f
                            val start = if (bar > 0f) bar + gap else 0f
                            val rtl = layoutDirection == LayoutDirection.Rtl
                            fun x(left: Float, width: Float) = if (rtl) size.width - left - width else left
                            if (bar > 0f) drawRoundRect(hc.solid, Offset(x(0f, bar), 0f), Size(bar, size.height), radius)
                            drawRoundRect(hc.soft, Offset(x(start, size.width - start), 0f), Size(size.width - start, size.height), radius)
                            val dot = 4.dp.toPx()
                            val inset = (size.height - dot) / 2
                            drawCircle(hc.solid, dot / 2, Offset(x(size.width - inset - dot, dot) + dot / 2, size.height / 2))
                        }
                    }
                }
                // The row's main value: plain text, right-aligned in a slot wide enough for "100%".
                Text(
                    stringResource(R.string.percent, r.percent),
                    style = SproutType.label,
                    color = colors.onSurface,
                    textAlign = TextAlign.End,
                    softWrap = false,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp),
                )
            }
        }
    }
}
