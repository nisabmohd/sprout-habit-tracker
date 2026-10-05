package app.sprout.habits.ui.insights

import app.sprout.habits.ui.components.groupedShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.LayoutDirection
import app.sprout.habits.ui.components.CustomRangeChip
import app.sprout.habits.ui.components.RangeChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sprout.habits.R
import app.sprout.habits.ui.components.TabHeader
import app.sprout.habits.ui.components.DateRangeSheet
import app.sprout.habits.ui.components.HabitFilterChip
import app.sprout.habits.ui.components.FilterChipRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import kotlinx.coroutines.delay
import app.sprout.habits.ui.components.HabitFilterSheet
import app.sprout.habits.ui.components.HeaderIconButton
import app.sprout.habits.ui.components.BarSegment
import app.sprout.habits.ui.components.CappedFontScale
import app.sprout.habits.ui.components.SegmentBar
import app.sprout.habits.ui.theme.habitColors

@Composable
fun InsightsScreen(viewModel: InsightsViewModel, weekStart: java.time.DayOfWeek, onOpenHabit: (Long) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ui = state ?: return
    var pickingRange by remember { mutableStateOf(false) }
    var filtering by remember { mutableStateOf(false) }
    val res = LocalContext.current.resources
    // The header stays put while the list scrolls under it, so its buttons are always in reach.
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)) {
            TabHeader(stringResource(R.string.insights_title), listSpacing = 16.dp) {
                HeaderIconButton(R.drawable.ic_calendar, stringResource(R.string.change_date_range), active = ui.preset == null) { pickingRange = true }
                HeaderIconButton(R.drawable.ic_filter, stringResource(R.string.filter_by_habit), active = ui.filter.isNotEmpty()) { filtering = true }
            }
            FilterChipRow(Modifier.padding(top = 12.dp)) {
                // A picked range takes the place of "This week"; its ✕ goes back to it.
                if (ui.preset == null) {
                    CustomRangeChip(
                        label = ui.rangeLabel,
                        openLabel = stringResource(R.string.change_date_range),
                        clearLabel = stringResource(R.string.clear_date_filter),
                        onOpen = { pickingRange = true },
                        onClear = { viewModel.setPreset(InsightsRange.THIS_WEEK) },
                    )
                } else {
                    RangeChip(stringResource(R.string.this_week), ui.preset == InsightsRange.THIS_WEEK) { viewModel.setPreset(InsightsRange.THIS_WEEK) }
                }
                RangeChip(stringResource(R.string.range_7_days), ui.preset == InsightsRange.DAYS_7) { viewModel.setPreset(InsightsRange.DAYS_7) }
                RangeChip(stringResource(R.string.range_30_days), ui.preset == InsightsRange.DAYS_30) { viewModel.setPreset(InsightsRange.DAYS_30) }
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
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "score") { SummaryCard(ui) }
            if (ui.primeDay != null || ui.focusName != null) item(key = "patterns") { Patterns(ui) }
            item(key = "bars") { WeeklyRhythm(ui) }
            if (ui.rates.isNotEmpty()) item(key = "rates") { HabitBreakdown(ui.rates, onOpenHabit) }
        }
    }

    if (pickingRange) {
        DateRangeSheet(
            from = ui.from,
            to = ui.to,
            weekStart = weekStart,
            shortcutLabel = stringResource(R.string.this_week),
            onApply = { a, b -> viewModel.setRange(a, b); pickingRange = false },
            onShortcut = { viewModel.setPreset(InsightsRange.THIS_WEEK); pickingRange = false },
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

/** Today's score card for the range, with one line of highlights under a divider. */
@Composable
private fun SummaryCard(ui: InsightsUi) {
    val colors = MaterialTheme.colorScheme
    val summary = buildList {
        add(pluralStringResource(R.plurals.count_done, ui.doneCount, ui.doneCount))
        add(pluralStringResource(R.plurals.count_partial, ui.partialCount, ui.partialCount))
    }.joinToString(" · ")
    // "12-day streak · Best week for Drink water", whichever of the two there is.
    val highlights = listOfNotNull(
        if (ui.streak > 1) pluralStringResource(R.plurals.streak_days, ui.streak, ui.streak) else null,
        ui.bestHabit?.let { stringResource(if (ui.weekLong) R.string.insights_best_week_for else R.string.insights_best_stretch_for, it) },
    ).joinToString(" · ")
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
        if (highlights.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.surfaceContainerHigh))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_flame), contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text(highlights, style = SproutType.supporting, color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/**
 * A section: its header sits on the page background (like a Journal day header), then the
 * section's own card or cards. No card inside a card. With [card], the content is one white card.
 */
@Composable
private fun Section(title: String, trailing: String?, card: Boolean = false, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = SproutType.cardTitle, color = colors.onSurface, modifier = Modifier.weight(1f).padding(end = 12.dp))
            if (trailing != null) Text(trailing, style = SproutType.supporting, color = colors.onSurfaceVariant)
        }
        if (card) {
            Column(
                Modifier.fillMaxWidth().background(colors.surfaceContainerLowest, RoundedCornerShape(24.dp)).padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) { content() }
        } else {
            content()
        }
    }
}

/**
 * Two white cards joined as a pair: the weekday that goes best, and the habit that dropped most against the
 * range before this one (or else the lowest one).
 */
@Composable
private fun Patterns(ui: InsightsUi) {
    val colors = MaterialTheme.colorScheme
    Section(stringResource(R.string.insights_patterns), ui.rangeLabel) {
        // Same height for both, whichever has more text.
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            val both = ui.primeDay != null && ui.focusName != null
            if (ui.primeDay != null) {
                PatternTile(
                    shape = if (both) RoundedCornerShape(topStart = 24.dp, topEnd = 6.dp, bottomEnd = 6.dp, bottomStart = 24.dp) else RoundedCornerShape(24.dp),
                    icon = R.drawable.ic_trophy,
                    iconContainer = colors.secondaryContainer,
                    iconTint = colors.onSecondaryContainer,
                    label = stringResource(R.string.insights_prime_time),
                    value = ui.primeDay,
                    detail = stringResource(R.string.insights_completion, ui.primePercent),
                )
            }
            if (ui.focusName != null) {
                PatternTile(
                    shape = if (both) RoundedCornerShape(topStart = 6.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 6.dp) else RoundedCornerShape(24.dp),
                    icon = R.drawable.ic_trending_down,
                    iconContainer = colors.surfaceContainerHigh,
                    iconTint = colors.onSurface,
                    label = stringResource(R.string.insights_needs_focus),
                    value = ui.focusName,
                    detail = ui.focusDrop?.let { stringResource(R.string.insights_down, it) } ?: stringResource(R.string.insights_lowest),
                )
            }
        }
    }
}

@Composable
private fun RowScope.PatternTile(shape: RoundedCornerShape, icon: Int, iconContainer: Color, iconTint: Color, label: String, value: String, detail: String) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.weight(1f).fillMaxHeight().background(colors.surfaceContainerLowest, shape).padding(16.dp).semantics(mergeDescendants = true) {},
    ) {
        Box(Modifier.size(36.dp).background(iconContainer, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Text(label, style = SproutType.caption, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
        Text(value, style = SproutType.cardTitle, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(detail, style = SproutType.supporting, color = colors.onSurfaceVariant)
    }
}

/** The light primary tone, hsl(H 45% 78%) in the design: the primary's own hue, so it follows the accent and dynamic color. */
@Composable
private fun partialTone(): Color {
    val colors = MaterialTheme.colorScheme
    return remember(colors.primary, colors.background) {
        val hsl = FloatArray(3).also { ColorUtils.colorToHSL(colors.primary.toArgb(), it) }
        if (colors.background.luminance() < 0.5f) Color.hsl(hsl[0], 0.4f, 0.42f) else Color.hsl(hsl[0], 0.45f, 0.78f)
    }
}

/**
 * A small label above a tapped mark; it goes away on its own. [at] is the point inside the
 * parent to sit above, or null for the parent's top centre.
 */
@Composable
private fun ChartTooltip(text: String, at: IntOffset?, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val margin = with(LocalDensity.current) { 6.dp.roundToPx() }
    LaunchedEffect(text, at) {
        delay(2500)
        onDismiss()
    }
    val position = remember(at, margin) {
        object : PopupPositionProvider {
            override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
                val x = anchorBounds.left + (at?.x ?: (anchorBounds.width / 2)) - popupContentSize.width / 2
                val y = anchorBounds.top + (at?.y ?: 0) - popupContentSize.height - margin
                return IntOffset(x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)), y.coerceAtLeast(0))
            }
        }
    }
    Popup(popupPositionProvider = position, onDismissRequest = onDismiss) {
        Text(
            text,
            style = SproutType.caption,
            color = colors.inverseOnSurface,
            modifier = Modifier.background(colors.inverseSurface, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/**
 * One pill bar per weekday (or per week for a long range), with no track behind it: done at the
 * bottom, partial on top of that. The full height stands for every scheduled habit.
 */
@Composable
private fun WeeklyRhythm(ui: InsightsUi) {
    val colors = MaterialTheme.colorScheme
    val done = colors.primary
    val partial = partialTone()
    val scale = ui.columns.maxOf { it.total }.coerceAtLeast(1)
    var tip by remember { mutableStateOf<Int?>(null) }
    Section(
        stringResource(if (ui.byWeek) R.string.insights_per_week else R.string.insights_weekly_rhythm),
        ui.bestDay?.let { stringResource(R.string.insights_best_day, it) },
        card = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().height(BAR_HEIGHT.dp)) {
                ui.columns.forEachIndexed { index, column ->
                    val description = stringResource(R.string.insights_bar_description, column.name, column.done.toString(), column.partial.toString())
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(interactionSource = null, indication = null) { tip = index }
                            .clearAndSetSemantics { contentDescription = description },
                        contentAlignment = Alignment.Center,
                    ) {
                        // 24 dp wide, or narrower when a long range has many weeks.
                        Canvas(Modifier.padding(horizontal = 1.dp).widthIn(max = 24.dp).fillMaxSize()) {
                            val unit = size.height / scale
                            val gap = 4.dp.toPx()
                            // Every segment is a full pill; a short one gets a smaller radius so it
                            // stays a pill, not a lens.
                            fun corner(height: Float) = CornerRadius(minOf(size.width, height) / 2)
                            val doneHeight = column.done * unit
                            if (doneHeight > 0f) drawRoundRect(done, Offset(0f, size.height - doneHeight), Size(size.width, doneHeight), corner(doneHeight))
                            if (column.partial > 0) {
                                // One partial is still visible on a tall scale.
                                val below = if (doneHeight > 0f) doneHeight + gap else 0f
                                val partialHeight = (column.partial * unit - (below - doneHeight)).coerceAtLeast(16.dp.toPx()).coerceAtMost(size.height - below)
                                drawRoundRect(partial, Offset(0f, size.height - below - partialHeight), Size(size.width, partialHeight), corner(partialHeight))
                            }
                        }
                        if (tip == index) ChartTooltip(description, null) { tip = null }
                    }
                }
            }
            // The labels stop scaling at 1.3x so they never wrap.
            CappedFontScale {
                Row(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
                    ui.columns.forEach { column ->
                        Text(
                            column.label,
                            style = SproutType.tiny,
                            color = if (column.isBest) colors.onSurface else colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)) {
            LegendSquare(stringResource(R.string.outcome_done), done)
            LegendSquare(stringResource(R.string.outcome_partial), partial)
        }
    }
}

/** Height of the bar tracks in dp. */
private const val BAR_HEIGHT = 128

@Composable
private fun LegendSquare(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
        Text(label, style = SproutType.supporting, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
    }
}

/**
 * Grouped like a Journal day: one white card per habit, sorted by score. An accent ribbon at the
 * start shows how consistent the habit was: wider toward 100%, and in the solid colour from 85%.
 * Then the habit tile, the name, a summary line with the streak, and the score. Tapping a card
 * opens the habit.
 */
@Composable
private fun HabitBreakdown(rates: List<HabitRateUi>, onOpenHabit: (Long) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Section(stringResource(R.string.insights_breakdown), pluralStringResource(R.plurals.habit_count, rates.size, rates.size)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            rates.forEachIndexed { index, r ->
                val hc = habitColors(r.hue)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .clip(groupedShape(index, rates.size))
                        .background(colors.surfaceContainerLowest)
                        .clickable(onClickLabel = stringResource(R.string.open_habit, r.name)) { onOpenHabit(r.id) }
                        .padding(start = 12.dp, top = 14.dp, end = 16.dp, bottom = 14.dp)
                        .semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // 4 dp wide up to 60%, growing to 8 dp at 100%, in an 8 dp slot.
                    val ribbon = (4f + 4f * (r.percent - 60) / 40f).coerceIn(4f, 8f)
                    Box(Modifier.width(8.dp).fillMaxHeight()) {
                        Box(Modifier.width(ribbon.dp).fillMaxHeight().background(if (r.percent >= 85) hc.solid else hc.mid, RoundedCornerShape(4.dp)))
                    }
                    Box(Modifier.size(44.dp).background(hc.soft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(r.icon), contentDescription = null, tint = hc.ink, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(r.name, style = SproutType.cardTitle, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // "5 done · 1 partial", or "7 of 7 days" when nothing was partial.
                            Text(
                                (if (r.partialDays > 0) {
                                    pluralStringResource(R.plurals.count_done, r.doneDays, r.doneDays) + " · " +
                                        pluralStringResource(R.plurals.count_partial, r.partialDays, r.partialDays)
                                } else {
                                    pluralStringResource(R.plurals.days_kept, r.scheduledDays, r.doneDays, r.scheduledDays)
                                }) + if (r.streak > 1) " · " else "",
                                style = SproutType.supporting,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            // The streak, with a muted flame: "5d".
                            if (r.streak > 1) {
                                val streak = pluralStringResource(R.plurals.streak_days, r.streak, r.streak)
                                Icon(painterResource(R.drawable.ic_flame), contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.padding(end = 2.dp).size(14.dp))
                                Text(
                                    stringResource(R.string.insights_streak_short, r.streak),
                                    style = SproutType.supporting,
                                    color = colors.onSurfaceVariant,
                                    softWrap = false,
                                    modifier = Modifier.semantics { contentDescription = streak },
                                )
                            }
                        }
                    }
                    // The row's main value: plain text.
                    Text(stringResource(R.string.percent, r.percent), style = SproutType.label, color = colors.onSurface, softWrap = false)
                }
            }
        }
    }
}
