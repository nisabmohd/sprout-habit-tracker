package app.sprout.habits.ui.insights

import app.sprout.habits.ui.components.tick
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.LayoutDirection
import app.sprout.habits.ui.components.CustomRangeChip
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
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
                HeaderIconButton(R.drawable.ic_calendar, stringResource(R.string.change_date_range), active = !ui.thisWeek) { pickingRange = true }
                HeaderIconButton(R.drawable.ic_filter, stringResource(R.string.filter_by_habit), active = ui.filter.isNotEmpty()) { filtering = true }
            }
            // No preset ranges: the row only shows the filters that are on, each with its ✕.
            if (!ui.thisWeek || ui.filter.isNotEmpty()) {
                FilterChipRow(Modifier.padding(top = 12.dp)) {
                    if (!ui.thisWeek) {
                        CustomRangeChip(
                            label = ui.rangeLabel,
                            openLabel = stringResource(R.string.change_date_range),
                            clearLabel = stringResource(R.string.clear_date_filter),
                            onOpen = { pickingRange = true },
                            onClear = viewModel::clearRange,
                        )
                    }
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
            item(key = "score") { SummaryCard(ui) }
            if (ui.primeDay != null || ui.focusName != null) item(key = "patterns") { Patterns(ui) }
            item(key = "bars") { WeeklyRhythm(ui) }
            if (ui.rates.isNotEmpty()) item(key = "rates") { HabitBreakdown(ui.rates, viewModel.breakdownByStreak, { viewModel.breakdownByStreak = it }, onOpenHabit) }
        }
    }

    if (pickingRange) {
        DateRangeSheet(
            from = ui.from,
            to = ui.to,
            weekStart = weekStart,
            shortcutLabel = stringResource(R.string.this_week),
            onApply = { a, b -> viewModel.setRange(a, b); pickingRange = false },
            onShortcut = { viewModel.clearRange(); pickingRange = false },
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

/** Today's score card for the range. */
@Composable
private fun SummaryCard(ui: InsightsUi) {
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
            Text(stringResource(R.string.percent, ui.scorePercent), style = SproutType.screenTitleNumber, color = colors.onSurface, softWrap = false)
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

/**
 * A section: its header sits on the page background (like a Journal day header), then the
 * section's own card or cards. No card inside a card. With [card], the content is one white card.
 */
@Composable
private fun Section(
    title: String,
    trailing: String?,
    card: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = SproutType.cardTitle, color = colors.onSurface, modifier = Modifier.weight(1f).padding(end = 12.dp))
            if (trailingContent != null) trailingContent()
            else if (trailing != null) Text(trailing, style = SproutType.supporting, color = colors.onSurfaceVariant)
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
 * Two white cards joined as a pair: the weekday that goes best, and the habit that
 * dropped most against the range before this one (or else the lowest one). Each card leads with a
 * labelled pill.
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
                    pill = colors.secondaryContainer,
                    pillContent = colors.onSecondaryContainer,
                    label = stringResource(R.string.insights_prime_time),
                    value = ui.primeDay,
                    detail = stringResource(R.string.insights_completion, ui.primePercent),
                )
            }
            if (ui.focusName != null) {
                PatternTile(
                    shape = if (both) RoundedCornerShape(topStart = 6.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 6.dp) else RoundedCornerShape(24.dp),
                    icon = R.drawable.ic_trending_down,
                    pill = colors.surfaceContainerHigh,
                    pillContent = colors.onSurface,
                    label = stringResource(R.string.insights_needs_focus),
                    value = ui.focusName,
                    detail = ui.focusDrop?.let { stringResource(R.string.insights_down, it) } ?: stringResource(R.string.insights_lowest),
                )
            }
        }
    }
}

@Composable
private fun RowScope.PatternTile(shape: RoundedCornerShape, icon: Int, pill: Color, pillContent: Color, label: String, value: String, detail: String) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.weight(1f).fillMaxHeight().background(colors.surfaceContainerLowest, shape).padding(16.dp).semantics(mergeDescendants = true) {},
    ) {
        // The label as a pill: icon + "Prime time".
        Row(
            Modifier.background(pill, RoundedCornerShape(12.dp)).padding(start = 10.dp, top = 6.dp, end = 12.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = pillContent, modifier = Modifier.size(18.dp))
            Text(label, style = SproutType.label, color = pillContent, modifier = Modifier.padding(start = 6.dp))
        }
        Text(value, style = SproutType.cardTitle, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 12.dp))
        // One line at normal text sizes; with large text the label and detail wrap rather than
        // clip, and the row's IntrinsicSize.Min keeps both cards the same height.
        Text(detail, style = SproutType.supporting, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
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
 * One group of white rows, 28 dp at the outer corners and 4 dp between rows, sorted by score or by
 * streak (the toggle in the header). Each row: the habit tile, the name, a done / partial summary
 * line, and the score as Today's partial ring on the right. Tapping a row opens the habit.
 */
@Composable
private fun HabitBreakdown(rates: List<HabitRateUi>, byStreak: Boolean, onSort: (Boolean) -> Unit, onOpenHabit: (Long) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val sorted = remember(rates, byStreak) {
        if (byStreak) rates.sortedWith(compareByDescending<HabitRateUi> { it.streak }.thenByDescending { it.percent }) else rates
    }
    Section(stringResource(R.string.insights_breakdown), null, trailingContent = { SortToggle(byStreak, onSort) }) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            sorted.forEachIndexed { index, r ->
                key(r.id) {
                val hc = habitColors(r.hue)
                val top = if (index == 0) 28.dp else 4.dp
                val bottom = if (index == sorted.size - 1) 28.dp else 4.dp
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(top, top, bottom, bottom))
                        .background(colors.surfaceContainerLowest)
                        .clickable(onClickLabel = stringResource(R.string.open_habit, r.name)) { onOpenHabit(r.id) }
                        .padding(start = 12.dp, top = 12.dp, end = 16.dp, bottom = 12.dp)
                        .semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(44.dp).background(hc.soft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(r.icon), contentDescription = null, tint = hc.ink, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(r.name, style = SproutType.cardTitle, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        // "5 done · 1 partial", or "7 of 7 days" when nothing was partial. The streak has its own toggle.
                        Text(
                            if (r.partialDays > 0) {
                                pluralStringResource(R.plurals.count_done, r.doneDays, r.doneDays) + " · " +
                                    pluralStringResource(R.plurals.count_partial, r.partialDays, r.partialDays)
                            } else {
                                pluralStringResource(R.plurals.days_kept, r.scheduledDays, r.doneDays, r.scheduledDays)
                            },
                            style = SproutType.supporting,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    // Today's partial ring: 44 dp, 4 dp stroke, round cap, from 12 o'clock, in the habit's
                    // solid colour on its mid tone. It shows the score, or sorted by streak the streak
                    // ("5d", filled to the streak's share of the range's scheduled days).
                    val progress = if (byStreak) r.streak.toFloat() / r.scheduledDays.coerceAtLeast(1) else r.percent / 100f
                    val ringText = if (byStreak) stringResource(R.string.insights_streak_short, r.streak) else stringResource(R.string.percent, r.percent)
                    val ringDescription = if (byStreak) pluralStringResource(R.plurals.streak_days, r.streak, r.streak) else ringText
                    Box(Modifier.size(44.dp).semantics { contentDescription = ringDescription }, contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            val stroke = 4.dp.toPx()
                            val radius = (size.minDimension - stroke) / 2
                            drawCircle(hc.mid, radius, style = Stroke(stroke))
                            if (progress > 0f) drawArc(
                                hc.solid,
                                -90f,
                                360f * progress.coerceAtMost(1f),
                                false,
                                Offset(center.x - radius, center.y - radius),
                                Size(radius * 2, radius * 2),
                                style = Stroke(stroke, cap = StrokeCap.Round),
                            )
                        }
                        // Fixed-size ring, so the text inside stops scaling at 1.3x.
                        CappedFontScale {
                            Text(ringText, style = SproutType.tiny, color = colors.onSurface, softWrap = false, modifier = Modifier.clearAndSetSemantics {})
                        }
                    }
                }
                }
            }
        }
    }
}

/**
 * "Score | Streak": a small two-way toggle for the breakdown's order. A soft pill track with the
 * chosen half as a rounded thumb; both halves are as wide as the wider label.
 */
@Composable
private fun SortToggle(byStreak: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    CappedFontScale {
        Row(
            Modifier
                .height(36.dp)
                .width(IntrinsicSize.Max)
                .background(colors.surfaceContainerHigh, CircleShape)
                .padding(3.dp)
                .selectableGroup(),
        ) {
            val haptics = LocalHapticFeedback.current
            listOf(
                false to stringResource(R.string.insights_sort_score),
                true to stringResource(R.string.insights_sort_streak),
            ).forEach { (value, label) ->
                val selected = byStreak == value
                val description = stringResource(if (value) R.string.insights_sort_by_streak else R.string.insights_sort_by_score)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(if (selected) colors.surfaceContainerLowest else Color.Transparent)
                        .selectable(selected = selected, role = Role.RadioButton) { if (!selected) haptics.tick(); onChange(value) }
                        .semantics { contentDescription = description }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = SproutType.label,
                        color = if (selected) colors.onSurface else colors.onSurfaceVariant,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
