package app.sprout.habits.ui.insights

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sprout.habits.data.Entry
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.SettingsRepository
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

enum class InsightsPeriod(val label: String) { WEEK("This week"), MONTH("This month"), QUARTER("Last 3 months") }

@Immutable
data class DayBarUi(val label: String, val done: Float, val partial: Float, val isToday: Boolean)

@Immutable
data class HabitRateUi(val id: Long, val name: String, val hue: Float, val percent: Int)

@Immutable
data class InsightsUi(
    val period: InsightsPeriod,
    val filterId: Long?,
    val habits: List<Pair<Long, String>>,
    val scorePercent: Int,
    val doneCount: Int,
    val partialCount: Int,
    val habitCount: Int,
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
    private val period = MutableStateFlow(InsightsPeriod.WEEK)
    private val filter = MutableStateFlow<Long?>(null)

    val state: StateFlow<InsightsUi?> = combine(period, settings.settings) { p, s -> p to s.weekStart }
        .flatMapLatest { (p, weekStart) ->
            val today = LocalDate.now()
            val (from, to) = range(p, today, weekStart)
            combine(
                repository.observeHabits(),
                repository.observeEntries(from.toEpochDay(), to.toEpochDay()),
                filter,
            ) { habits, entries, f -> build(p, today, from, to, weekStart, habits, entries, f) }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setPeriod(value: InsightsPeriod) {
        period.value = value
    }

    fun setFilter(habitId: Long?) {
        filter.value = habitId
    }

    private fun range(p: InsightsPeriod, today: LocalDate, weekStart: DayOfWeek): Pair<LocalDate, LocalDate> = when (p) {
        InsightsPeriod.WEEK -> weekOf(today, weekStart).let { it.first() to it.last() }
        InsightsPeriod.MONTH -> today.withDayOfMonth(1) to today
        InsightsPeriod.QUARTER -> today.minusMonths(3).plusDays(1) to today
    }

    private fun build(
        p: InsightsPeriod,
        today: LocalDate,
        from: LocalDate,
        to: LocalDate,
        weekStart: DayOfWeek,
        allHabits: List<Habit>,
        entries: List<Entry>,
        filterId: Long?,
    ): InsightsUi {
        val habits = allHabits.filter { filterId == null || it.id == filterId }
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
            HabitRateUi(habit.id, habit.name, habit.colorHue.toFloat(), (score(habitCredits) * 100).roundToInt())
        }.sortedByDescending { it.percent }

        val averaged = p != InsightsPeriod.WEEK
        val bars = weekdays.mapIndexed { i, d ->
            val n = if (averaged) occurrences[i].coerceAtLeast(1) else 1
            DayBarUi(
                label = d.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                done = done[i].toFloat() / n,
                partial = partial[i].toFloat() / n,
                isToday = d == today.dayOfWeek,
            )
        }
        val best = credits.withIndex()
            .filter { (_, list) -> list.any { it != null } }
            .maxByOrNull { (_, list) -> score(list) }
            ?.let { weekdays[it.index].getDisplayName(TextStyle.FULL, Locale.getDefault()) }

        return InsightsUi(
            period = p,
            filterId = filterId,
            habits = allHabits.map { it.id to it.name },
            scorePercent = (score(allCredits) * 100).roundToInt(),
            doneCount = done.sum(),
            partialCount = partial.sum(),
            habitCount = habits.size,
            bestDay = best,
            averaged = averaged,
            bars = bars,
            rates = rates,
        )
    }
}
