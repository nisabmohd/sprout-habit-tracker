package app.sprout.habits.ui.insights

import app.sprout.habits.ui.englishDates
import app.sprout.habits.ui.datePattern
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sprout.habits.R
import app.sprout.habits.Strings
import app.sprout.habits.data.Entry
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.data.SettingsRepository
import app.sprout.habits.ui.components.HabitFilterOption
import app.sprout.habits.domain.DayOutcome
import app.sprout.habits.domain.dayCredit
import app.sprout.habits.domain.currentStreak
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


/** The range chips under the title. Insights opens on [THIS_WEEK]. */
enum class InsightsRange { THIS_WEEK, DAYS_7, DAYS_30 }

@Immutable
data class DayColumnUi(
    /** "Mon", or the day a week starts on: "1 Sep" ("1/9" once there are many weeks). */
    val label: String,
    /** "Monday", or "Week of 14 Sep". */
    val name: String,
    val done: Int,
    val partial: Int,
    /** Scheduled habit-days in this column; 0 for a day still to come. */
    val total: Int,
    val isBest: Boolean,
)

@Immutable
data class HabitRateUi(
    val id: Long,
    val name: String,
    val icon: Int,
    val hue: Float,
    val percent: Int,
    /** Out of [scheduledDays] in the range. */
    val doneDays: Int,
    val partialDays: Int,
    /** The part of the score that comes from done days, 0..1; the rest of it is partial days. */
    val doneShare: Float,
    val scheduledDays: Int,
    /** The habit's streak at the end of the range. */
    val streak: Int,
)

@Immutable
data class InsightsUi(
    /** The selected range chip, or null while a range from the date sheet is on. */
    val preset: InsightsRange?,
    /** "This week", "7 days", "30 days" or "21 – 27 Sep". */
    val rangeLabel: String,
    val from: LocalDate,
    val to: LocalDate,
    /** Empty = every habit. */
    val filter: Set<Long>,
    val options: List<HabitFilterOption>,
    val scorePercent: Int,
    val doneCount: Int,
    val partialCount: Int,
    /** The longest streak any of the habits has as of today. */
    val streak: Int,
    /** The habit for which this range is at least as good as the eight ranges before it, if any. */
    val bestHabit: String?,
    /** True when the range is a week or shorter, so it can be called a week. */
    val weekLong: Boolean,
    /** The weekday with the highest score in the range, "Tuesday", and that score. */
    val primeDay: String?,
    val primePercent: Int,
    /** The habit that dropped most against the range before, or else the lowest one. */
    val focusName: String?,
    /** How many points it dropped; null when [focusName] is only the lowest. */
    val focusDrop: Int?,
    val bestDay: String?,
    /** True when the range is longer than two weeks: one column per week instead of per weekday. */
    val byWeek: Boolean,
    val columns: List<DayColumnUi>,
    val rates: List<HabitRateUi>,
)

/** One of the chips, or a range from the date sheet. */
private sealed interface Selection {
    data class Preset(val range: InsightsRange) : Selection
    data class Custom(val from: LocalDate, val to: LocalDate) : Selection
}

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(
    private val repository: HabitRepository,
    private val settings: SettingsRepository,
    private val strings: Strings,
) : ViewModel() {
    private val selection = MutableStateFlow<Selection>(Selection.Preset(InsightsRange.THIS_WEEK))
    private val filter = MutableStateFlow<Set<Long>>(emptySet())

    val state: StateFlow<InsightsUi?> = combine(selection, settings.settings) { r, s -> r to s.weekStart }
        .flatMapLatest { (picked, weekStart) ->
            val today = LocalDate.now()
            // Days still to come are never part of a range.
            val (from, to) = when (picked) {
                is Selection.Custom -> picked.from to picked.to
                is Selection.Preset -> when (picked.range) {
                    InsightsRange.THIS_WEEK -> weekOf(today, weekStart).first() to today
                    InsightsRange.DAYS_7 -> today.minusDays(6) to today
                    InsightsRange.DAYS_30 -> today.minusDays(29) to today
                }
            }
            // Every entry: streaks and the ranges before this one reach back past it.
            combine(repository.observeHabits(), repository.observeEntries(0, Long.MAX_VALUE), filter) { habits, entries, f ->
                build(today, from, to, (picked as? Selection.Preset)?.range, weekStart, habits, entries, f)
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setPreset(range: InsightsRange) {
        selection.value = Selection.Preset(range)
    }

    fun setRange(from: LocalDate, to: LocalDate) {
        selection.value = Selection.Custom(minOf(from, to), maxOf(from, to))
    }

    fun setFilter(habitIds: Set<Long>) {
        filter.value = habitIds
    }

    private fun build(
        today: LocalDate,
        from: LocalDate,
        to: LocalDate,
        preset: InsightsRange?,
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
        val length = last - first + 1

        // Up to two weeks: one column per weekday (8 to 14 days add both weeks together).
        // Longer: one column per week.
        val byWeek = to.toEpochDay() - first > 13
        val weekdays = List(7) { weekStart.plus(it.toLong()) }
        val firstWeek = weekOf(from, weekStart).first().toEpochDay()
        val columns = if (byWeek) ((last - firstWeek) / 7).toInt() + 1 else 7
        fun weekdayOf(day: Long) = weekdays.indexOf(LocalDate.ofEpochDay(day).dayOfWeek)
        fun columnOf(day: Long) = if (byWeek) ((day - firstWeek) / 7).toInt() else weekdayOf(day)

        val done = IntArray(columns)
        val partial = IntArray(columns)
        val total = IntArray(columns)
        val credits = List(columns) { mutableListOf<Double?>() }
        val weekdayCredits = List(7) { mutableListOf<Double?>() }

        val allCredits = mutableListOf<Double?>()
        var streak = 0
        // The habit that fell most against the range before this one, and by how many points.
        var dropped: Pair<String, Int>? = null
        // The best-scoring habit for which no earlier range was better.
        var best: Pair<String, Double>? = null
        val rates = habits.map { habit ->
            val history = habit.history(byHabit[habit.id].orEmpty())
            val habitCredits = mutableListOf<Double?>()
            var habitDone = 0
            var habitPartial = 0
            var scheduled = 0
            for (day in first..last) {
                if (!history.isScheduled(day)) continue
                val entry = history.entries[day]
                val c = columnOf(day)
                scheduled++
                total[c]++
                when (outcomeOf(entry, day, todayDay)) {
                    DayOutcome.DONE -> { done[c]++; habitDone++ }
                    DayOutcome.PARTIAL -> { partial[c]++; habitPartial++ }
                    else -> Unit
                }
                val credit = dayCredit(entry, habit.target, day, todayDay)
                habitCredits += credit
                credits[c] += credit
                weekdayCredits[weekdayOf(day)] += credit
            }
            allCredits += habitCredits
            val current = score(habitCredits)
            val percent = (current * 100).roundToInt()
            streak = maxOf(streak, history.currentStreak(todayDay))
            // Only a habit that already existed before the range has something to compare with.
            if (length > 0 && history.firstDay < first) {
                val before = (1..8).map { k -> first - k * length }.filter { it + length - 1 >= history.firstDay }
                    .map { start -> history.score(start, start + length - 1, todayDay) }
                val fall = (before.first() * 100).roundToInt() - percent
                if (fall > 0 && fall > (dropped?.second ?: 0)) dropped = habit.name to fall
                if (current > 0.0 && before.all { it <= current } && current > (best?.second ?: 0.0)) best = habit.name to current
            }
            HabitRateUi(
                id = habit.id,
                name = habit.name,
                icon = HabitIcon.fromKey(habit.icon).drawable,
                hue = habit.colorHue.toFloat(),
                percent = percent,
                doneDays = habitDone,
                partialDays = habitPartial,
                // Days that count toward the score: a logged Skip is left out.
                doneShare = habitCredits.count { it != null }.let { if (it == 0) 0f else habitDone.toFloat() / it },
                scheduledDays = scheduled,
                streak = history.currentStreak(last),
            )
        }.sortedByDescending { it.percent }

        fun bestOf(lists: List<List<Double?>>) = lists.withIndex()
            .filter { (_, list) -> list.any { it != null } }
            .maxByOrNull { (_, list) -> score(list) }
            ?.index
        val bestIndex = bestOf(credits)
        val primeIndex = bestOf(weekdayCredits)
        val columnUis = List(columns) { i ->
            if (byWeek) {
                // A week is named by the day it starts on (or the range's first day).
                val start = LocalDate.ofEpochDay(maxOf(firstWeek + i * 7L, first))
                DayColumnUi(start.format(datePattern(if (columns > 6) "d/M" else "d MMM")), strings(R.string.insights_week_of, start.format(datePattern("d MMM"))), done[i], partial[i], total[i], i == bestIndex)
            } else {
                DayColumnUi(
                    label = weekdays[i].getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    name = weekdays[i].getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    done = done[i],
                    partial = partial[i],
                    total = total[i],
                    isBest = i == bestIndex,
                )
            }
        }

        return InsightsUi(
            preset = preset,
            rangeLabel = when (preset) {
                InsightsRange.THIS_WEEK -> strings(R.string.this_week)
                InsightsRange.DAYS_7 -> strings(R.string.range_7_days)
                InsightsRange.DAYS_30 -> strings(R.string.range_30_days)
                null -> chipRangeLabel(from, to, today)
            },
            from = from,
            to = to,
            filter = filterIds,
            options = allHabits.map { HabitFilterOption(it.id, it.name, HabitIcon.fromKey(it.icon).drawable, it.colorHue.toFloat()) },
            scorePercent = (score(allCredits) * 100).roundToInt(),
            doneCount = done.sum(),
            partialCount = partial.sum(),
            streak = streak,
            bestHabit = best?.first,
            weekLong = to.toEpochDay() - first <= 6,
            primeDay = primeIndex?.let { weekdays[it].getDisplayName(TextStyle.FULL, Locale.getDefault()) },
            primePercent = primeIndex?.let { (score(weekdayCredits[it]) * 100).roundToInt() } ?: 0,
            // With nothing down, the lowest habit, as long as there is more than one to pick from.
            focusName = dropped?.first ?: rates.takeIf { it.size > 1 }?.last()?.name,
            focusDrop = dropped?.second,
            bestDay = bestIndex?.let { columnUis[it].name },
            byWeek = byWeek,
            columns = columnUis,
            rates = rates,
        )
    }

    companion object {
        /** The range as a filter chip shows it: without the year while it is this year's. */
        fun chipRangeLabel(from: LocalDate, to: LocalDate, today: LocalDate): String {
            if (from.year != today.year || to.year != today.year) return rangeLabel(from, to)
            val dm = datePattern("d MMM")
            return when {
                from == to -> from.format(dm)
                englishDates() && from.month == to.month -> "${from.dayOfMonth} – ${to.format(dm)}"
                else -> "${from.format(dm)} – ${to.format(dm)}"
            }
        }

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
