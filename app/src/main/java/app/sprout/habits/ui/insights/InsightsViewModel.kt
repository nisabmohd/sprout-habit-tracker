package app.sprout.habits.ui.insights

import app.sprout.habits.ui.englishDates
import app.sprout.habits.ui.datePattern
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sprout.habits.data.Entry
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.data.SettingsRepository
import app.sprout.habits.ui.components.HabitFilterOption
import app.sprout.habits.domain.DayOutcome
import app.sprout.habits.domain.dayCredit
import app.sprout.habits.domain.history
import app.sprout.habits.domain.outcomeOf
import app.sprout.habits.domain.score
import app.sprout.habits.domain.weekOf
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn


@Immutable
data class DayBarUi(val label: String, val name: String, val done: Float, val partial: Float, val isBest: Boolean)

@Immutable
data class HabitRateUi(val id: Long, val name: String, val icon: Int, val hue: Float, val percent: Int)

@Immutable
data class InsightsUi(
    /** "21 – 27 Sep 2026" */
    val rangeLabel: String,
    val from: LocalDate,
    val to: LocalDate,
    /** Empty = every habit. */
    val filter: Set<Long>,
    val options: List<HabitFilterOption>,
    val scorePercent: Int,
    val doneCount: Int,
    val partialCount: Int,
    val bestDay: String?,
    /** True when bars are averages per weekday rather than one week's counts. */
    val averaged: Boolean,
    val bars: List<DayBarUi>,
    val rates: List<HabitRateUi>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(
    private val repository: HabitRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    /** Null = this week up to today (follows the week-start setting and rolls over with the calendar). */
    private val range = MutableStateFlow<Pair<LocalDate, LocalDate>?>(null)
    private val filter = MutableStateFlow<Set<Long>>(emptySet())

    val state: StateFlow<InsightsUi?> = combine(range, settings.settings) { r, s -> r to s.weekStart }
        .flatMapLatest { (r, weekStart) ->
            val today = LocalDate.now()
            val (from, to) = r ?: thisWeek(today, weekStart)
            combine(
                repository.observeHabits(),
                repository.observeEntries(from.toEpochDay(), to.toEpochDay()),
                filter,
            ) { habits, entries, f -> build(today, from, to, weekStart, habits, entries, f) }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Null goes back to this week. */
    fun setRange(from: LocalDate?, to: LocalDate?) {
        range.value = if (from == null || to == null) null else minOf(from, to) to maxOf(from, to)
    }

    fun setFilter(habitIds: Set<Long>) {
        filter.value = habitIds
    }

    /** Start of this week through today; days still to come are never part of the range. */
    private fun thisWeek(today: LocalDate, weekStart: DayOfWeek) = weekOf(today, weekStart).first() to today

    private fun build(
        today: LocalDate,
        from: LocalDate,
        to: LocalDate,
        weekStart: DayOfWeek,
        allHabits: List<Habit>,
        entries: List<Entry>,
        filterIds: Set<Long>,
    ): InsightsUi {
        val habits = allHabits.filter { filterIds.isEmpty() || it.id in filterIds }
        val byHabit = entries.groupBy { it.habitId }.mapValues { (_, l) -> l.associateBy { it.date } }
        val todayDay = today.toEpochDay()
        val first = from.toEpochDay()
        val last = minOf(to.toEpochDay(), todayDay)

        val weekdays = List(7) { weekStart.plus(it.toLong()) }
        val done = IntArray(7)
        val partial = IntArray(7)
        val credits = List(7) { mutableListOf<Double?>() }
        val occurrences = IntArray(7)
        for (day in first..last) occurrences[weekdays.indexOf(LocalDate.ofEpochDay(day).dayOfWeek)]++

        val allCredits = mutableListOf<Double?>()
        val rates = habits.map { habit ->
            val history = habit.history(byHabit[habit.id].orEmpty())
            val habitCredits = mutableListOf<Double?>()
            for (day in first..last) {
                if (!history.isScheduled(day)) continue
                val entry = history.entries[day]
                val w = weekdays.indexOf(LocalDate.ofEpochDay(day).dayOfWeek)
                when (outcomeOf(entry, day, todayDay)) {
                    DayOutcome.DONE -> done[w]++
                    DayOutcome.PARTIAL -> partial[w]++
                    else -> Unit
                }
                val c = dayCredit(entry, habit.target, day, todayDay)
                habitCredits += c
                credits[w] += c
            }
            allCredits += habitCredits
            HabitRateUi(habit.id, habit.name, HabitIcon.fromKey(habit.icon).drawable, habit.colorHue.toFloat(), (score(habitCredits) * 100).roundToInt())
        }.sortedByDescending { it.percent }

        // Longer than a week: show each weekday's average instead of one week's counts.
        val averaged = to.toEpochDay() - from.toEpochDay() > 6
        val bestIndex = credits.withIndex()
            .filter { (_, list) -> list.any { it != null } }
            .maxByOrNull { (_, list) -> score(list) }
            ?.index
        val bars = weekdays.mapIndexed { i, d ->
            val n = if (averaged) occurrences[i].coerceAtLeast(1) else 1
            DayBarUi(
                label = d.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                name = d.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                done = done[i].toFloat() / n,
                partial = partial[i].toFloat() / n,
                isBest = i == bestIndex,
            )
        }

        return InsightsUi(
            rangeLabel = rangeLabel(from, to),
            from = from,
            to = to,
            filter = filterIds,
            options = allHabits.map { HabitFilterOption(it.id, it.name, HabitIcon.fromKey(it.icon).drawable, it.colorHue.toFloat()) },
            scorePercent = (score(allCredits) * 100).roundToInt(),
            doneCount = done.sum(),
            partialCount = partial.sum(),
            bestDay = bestIndex?.let { bars[it].name },
            averaged = averaged,
            bars = bars,
            rates = rates,
        )
    }

    companion object {
        /** "28 Sep 2026", "21 – 27 Sep 2026", "28 Sep – 4 Oct 2026", "29 Dec 2025 – 4 Jan 2026". */
        fun rangeLabel(from: LocalDate, to: LocalDate): String {
            val dm = datePattern("d MMM")
            val dmy = datePattern("d MMM yyyy")
            return when {
                from == to -> from.format(dmy)
                from.year != to.year -> "${from.format(dmy)} – ${to.format(dmy)}"
                !englishDates() -> "${from.format(dm)} – ${to.format(dmy)}"
                from.month == to.month -> "${from.dayOfMonth} – ${to.format(dm)} ${to.year}"
                else -> "${from.format(dm)} – ${to.format(dm)} ${to.year}"
            }
        }
    }
}
