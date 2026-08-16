package app.sprout.habits.ui.habits

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sprout.habits.data.Entry
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.SettingsRepository
import app.sprout.habits.data.TrackType
import app.sprout.habits.domain.bestStreak
import app.sprout.habits.domain.dayCredit
import app.sprout.habits.domain.history
import app.sprout.habits.domain.outcomeOf
import app.sprout.habits.domain.score
import app.sprout.habits.domain.weekOf
import app.sprout.habits.ui.components.DayMark
import app.sprout.habits.ui.components.markFor
import app.sprout.habits.ui.today.TodayViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class HabitsMode { WEEK, OVERALL }

@Immutable
data class HabitWeekUi(
    val id: Long,
    val name: String,
    val icon: Int,
    val hue: Float,
    val goal: String,
    val marks: List<DayMark>,
)

@Immutable
data class WeekUi(
    val label: String,
    val percent: Int,
    val dayLabels: List<String>,
    /** Index of today in the week, or -1 when today is in another week. */
    val todayIndex: Int,
    val canGoForward: Boolean,
    val habits: List<HabitWeekUi>,
)

@Immutable
data class HabitOverallUi(
    val id: Long,
    val name: String,
    val icon: Int,
    val hue: Float,
    val percent: Int,
    val bestStreak: Int,
    /** Column-major: [OVERALL_WEEKS] columns of 7 days. */
    val cells: List<DayMark>,
)

@Immutable
data class OverallUi(
    /** Month label and the column it starts at. */
    val months: List<Pair<String, Int>>,
    val habits: List<HabitOverallUi>,
)

const val OVERALL_WEEKS = 26

@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModel(
    private val repository: HabitRepository,
    settings: SettingsRepository,
) : ViewModel() {
    val mode = MutableStateFlow(HabitsMode.WEEK)
    private val weekOffset = MutableStateFlow(0)
    private val today = MutableStateFlow(LocalDate.now())
    private val weekStart = settings.settings.map { it.weekStart }.distinctUntilChanged()

    val week: StateFlow<WeekUi?> =
        combine(today, weekOffset, weekStart) { t, offset, ws -> Triple(t, weekOf(t.plusWeeks(offset.toLong()), ws), offset) }
            .flatMapLatest { (t, days, offset) ->
                combine(
                    repository.observeHabits(),
                    repository.observeEntries(days.first().toEpochDay(), days.last().toEpochDay()),
                ) { habits, entries -> buildWeek(t, days, offset, habits, entries) }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val overall: StateFlow<OverallUi?> =
        combine(today, weekStart) { t, ws -> t to ws }
            .flatMapLatest { (t, ws) ->
                val lastWeek = weekOf(t, ws)
                val first = lastWeek.first().minusWeeks((OVERALL_WEEKS - 1).toLong())
                combine(
                    repository.observeHabits(),
                    repository.observeEntries(first.toEpochDay(), lastWeek.last().toEpochDay()),
                ) { habits, entries -> buildOverall(t, first, habits, entries) }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setMode(value: HabitsMode) {
        mode.value = value
    }

    fun previousWeek() = weekOffset.value--
    fun nextWeek() {
        if (weekOffset.value < 0) weekOffset.value++
    }

    fun refreshToday() {
        today.value = LocalDate.now()
    }

    private fun buildWeek(today: LocalDate, days: List<LocalDate>, offset: Int, habits: List<Habit>, entries: List<Entry>): WeekUi {
        val byHabit = entries.groupBy { it.habitId }.mapValues { (_, list) -> list.associateBy { it.date } }
        val todayDay = today.toEpochDay()
        val credits = mutableListOf<Double?>()
        val rows = habits.map { habit ->
            val history = habit.history(byHabit[habit.id].orEmpty())
            val marks = days.map { date ->
                val day = date.toEpochDay()
                val scheduled = history.isScheduled(day)
                val entry = history.entries[day]
                val credit = dayCredit(entry, habit.target, day, todayDay)
                if (scheduled && day <= todayDay) credits += credit
                markFor(scheduled, outcomeOf(entry, day, todayDay), (credit ?: 0.0).toFloat(), day > todayDay)
            }
            HabitWeekUi(habit.id, habit.name, HabitIcon.fromKey(habit.icon).drawable, habit.colorHue.toFloat(), goalLabel(habit), marks)
        }
        return WeekUi(
            label = rangeLabel(days.first(), days.last()),
            percent = (score(credits) * 100).roundToInt(),
            dayLabels = days.map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) },
            todayIndex = days.indexOf(today),
            canGoForward = offset < 0,
            habits = rows,
        )
    }

    private fun buildOverall(today: LocalDate, first: LocalDate, habits: List<Habit>, entries: List<Entry>): OverallUi {
        val byHabit = entries.groupBy { it.habitId }.mapValues { (_, list) -> list.associateBy { it.date } }
        val todayDay = today.toEpochDay()
        val firstDay = first.toEpochDay()
        val lastDay = firstDay + OVERALL_WEEKS * 7 - 1
        val rows = habits.map { habit ->
            val history = habit.history(byHabit[habit.id].orEmpty())
            val cells = (firstDay..lastDay).map { day ->
                val scheduled = history.isScheduled(day)
                val entry = history.entries[day]
                val credit = dayCredit(entry, habit.target, day, todayDay) ?: 0.0
                markFor(scheduled, outcomeOf(entry, day, todayDay), credit.toFloat(), day > todayDay)
            }
            // Best streak within the window shown; the habit detail shows the all-time one.
            val windowed = app.sprout.habits.domain.HabitHistory(habit.daysMask, habit.target, history.entries, maxOf(history.firstDay, firstDay))
            HabitOverallUi(
                id = habit.id,
                name = habit.name,
                icon = HabitIcon.fromKey(habit.icon).drawable,
                hue = habit.colorHue.toFloat(),
                percent = (windowed.score(firstDay, lastDay, todayDay) * 100).roundToInt(),
                bestStreak = windowed.bestStreak(todayDay),
                cells = cells,
            )
        }
        // A label at each column where a new month starts; the first (partial) month's label is
        // dropped when the next one starts within 3 columns, so labels never overlap.
        val months = (0 until OVERALL_WEEKS)
            .map { col -> first.plusWeeks(col.toLong()) }
            .withIndex()
            .filter { (col, date) -> col == 0 || date.monthValue != first.plusWeeks(col - 1L).monthValue }
            .map { (col, date) -> date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()) to col }
            .let { if (it.size > 1 && it[1].second - it[0].second < 3) it.drop(1) else it }
        return OverallUi(months, rows)
    }

    companion object {
        fun goalLabel(habit: Habit): String = when (habit.trackType) {
            TrackType.AMOUNT -> "${TodayViewModel.formatNumber(habit.target)} ${habit.unit}".trim()
            TrackType.DURATION -> "${TodayViewModel.formatNumber(habit.target)} min"
            TrackType.CHECK -> daysLabel(habit.daysMask)
        }

        fun daysLabel(mask: Int): String = when (mask) {
            0b111_1111 -> "Every day"
            0b001_1111 -> "Weekdays"
            0b110_0000 -> "Weekends"
            else -> DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }
                .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
        }

        private fun rangeLabel(from: LocalDate, to: LocalDate): String = if (from.month == to.month) {
            "${from.dayOfMonth} – ${to.format(DateTimeFormatter.ofPattern("d MMM"))}"
        } else {
            "${from.format(DateTimeFormatter.ofPattern("d MMM"))} – ${to.format(DateTimeFormatter.ofPattern("d MMM"))}"
        }
    }
}
