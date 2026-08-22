package app.sprout.habits.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sprout.habits.R
import app.sprout.habits.ui.components.ProgressRing
import app.sprout.habits.ui.theme.habitColors
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun InsightsScreen(viewModel: InsightsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ui = state ?: return
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Insights", style = type.headlineMedium, color = colors.onBackground, modifier = Modifier.weight(1f))
                PeriodMenu(ui.period, viewModel::setPeriod)
            }
        }
        item(key = "chips") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("All habits", ui.filterId == null) { viewModel.setFilter(null) }
                ui.habits.forEach { (id, name) -> Chip(name, ui.filterId == id) { viewModel.setFilter(id) } }
            }
        }
        item(key = "score") { ScoreCard(ui) }
        item(key = "bars") { DayBars(ui) }
        if (ui.filterId == null && ui.rates.isNotEmpty()) item(key = "rates") { ByHabit(ui.rates) }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.titleSmall) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

@Composable
private fun PeriodMenu(period: InsightsPeriod, onSelect: (InsightsPeriod) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
            Text(period.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Icon(painterResource(R.drawable.ic_chevron_down), contentDescription = null, modifier = Modifier.padding(start = 6.dp).size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            InsightsPeriod.entries.forEach { p ->
                DropdownMenuItem(text = { Text(p.label) }, onClick = { onSelect(p); open = false })
            }
        }
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
                Text("${ui.scorePercent}%", style = type.headlineMedium, color = colors.onSurface)
            }
            Column(Modifier.padding(start = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Average score", style = type.titleSmall, color = colors.onSurfaceVariant)
                val habits = if (ui.habitCount == 1) "1 habit" else "${ui.habitCount} habits"
                Text("${ui.doneCount} check-ins done, ${ui.partialCount} partial across $habits.", style = type.bodyLarge, color = colors.onSurface)
                ui.bestDay?.let { Text("Best day: $it", style = type.titleSmall, color = colors.primary) }
            }
        }
    }
}

@Composable
private fun DayBars(ui: InsightsUi) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val done = colors.primary
    val partial = colors.primaryContainer
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (ui.averaged) "Habits per day, average" else "Habits per day", style = type.titleMedium, color = colors.onSurface, modifier = Modifier.weight(1f))
                LegendDot("Done", done)
                LegendDot("Partial", partial)
            }
            val max = ui.bars.maxOf { it.done + it.partial }.coerceAtLeast(1f)
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                ui.bars.forEach { bar ->
                    val total = bar.done + bar.partial
                    val value = if (ui.averaged) "%.1f".format(total) else total.roundToInt().toString()
                    Column(
                        Modifier.width(30.dp).semantics(mergeDescendants = true) {
                            contentDescription = "${bar.label}: ${bar.done.fmt()} done, ${bar.partial.fmt()} partial"
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
            Text("By habit", style = type.titleMedium, color = colors.onSurface)
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
                    Text("${r.percent}%", style = type.titleSmall, color = colors.onSurface, textAlign = TextAlign.End, modifier = Modifier.width(44.dp))
                }
            }
        }
    }
}
