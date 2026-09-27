package app.sprout.habits.ui.edit

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sprout.habits.data.DurationUnit
import app.sprout.habits.data.Habit
import app.sprout.habits.domain.toShown
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.SettingsRepository
import app.sprout.habits.data.TrackType
import java.time.DayOfWeek
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The fixed habit color choices, in the order the design shows them. */
val HABIT_HUES = listOf(275, 215, 192, 150, 95, 38, 12, 330)

@Immutable
data class HabitForm(
    val loaded: Boolean = false,
    val isNew: Boolean = true,
    val name: String = "",
    val icon: HabitIcon = HabitIcon.CALM,
    val hue: Int = HABIT_HUES.first(),
    val trackType: TrackType = TrackType.CHECK,
    /** Text of the target field, kept as typed. */
    val target: String = "",
    val unit: String = "",
    /** For Duration: whether the target (and logging) is in minutes or hours. */
    val durationUnit: DurationUnit = DurationUnit.MINUTES,
    val daysMask: Int = Habit.EVERY_DAY,
    val reminderOn: Boolean = false,
    val reminderMinutes: Int = 9 * 60,
    val askForNote: Boolean = false,
    val showOnWidget: Boolean = true,
    val weekStart: DayOfWeek = DayOfWeek.MONDAY,
) {
    val targetValue: Double? get() = target.replace(',', '.').toDoubleOrNull()

    /**
     * Switching minutes and hours converts the target when it reads as a whole number of quarter
     * hours (90 min becomes 1.5 h), and to minutes when it could be hours in a day (2 h becomes
     * 120 min). Otherwise the number stays as typed: typing 2 and then tapping Hours means
     * 2 hours, not 0.03, and typing 45 and then tapping Minutes means 45 minutes.
     */
    fun withDurationUnit(unit: DurationUnit): HabitForm {
        if (unit == durationUnit) return this
        val v = targetValue ?: return copy(durationUnit = unit)
        val converted = if (unit == DurationUnit.HOURS) v / 60 else v * 60
        val keep = if (unit == DurationUnit.HOURS) (converted * 4) % 1.0 != 0.0 else v > 24
        return copy(durationUnit = unit, target = if (keep) target else app.sprout.habits.domain.formatNumber(converted))
    }

    val canSave: Boolean
        get() = name.isNotBlank() && daysMask != 0 &&
            (trackType == TrackType.CHECK || (targetValue ?: 0.0) > 0.0)
}

class EditHabitViewModel(
    private val repository: HabitRepository,
    private val settings: SettingsRepository,
    private val habitId: Long,
) : ViewModel() {
    private val _form = MutableStateFlow(HabitForm())
    val form: StateFlow<HabitForm> = _form

    private val _saved = Channel<Unit>(Channel.CONFLATED)

    /** Emits once the habit has been saved, so the screen can close. */
    val saved = _saved.receiveAsFlow()

    /** The habit as stored, kept so saving preserves fields the form doesn't show. */
    private var original: Habit? = null

    init {
        viewModelScope.launch {
            val s = settings.settings.first()
            val habit = if (habitId != 0L) repository.getHabit(habitId) else null
            original = habit
            _form.value = if (habit == null) {
                HabitForm(loaded = true, reminderMinutes = s.defaultReminderMinutes, weekStart = s.weekStart)
            } else {
                HabitForm(
                    loaded = true,
                    isNew = false,
                    name = habit.name,
                    icon = HabitIcon.fromKey(habit.icon),
                    hue = habit.colorHue,
                    trackType = habit.trackType,
                    target = if (habit.trackType == TrackType.CHECK) "" else formatTarget(habit.toShown(habit.target)),
                    unit = habit.unit,
                    durationUnit = habit.durationUnit,
                    daysMask = habit.daysMask,
                    reminderOn = habit.reminderMinutes != null,
                    reminderMinutes = habit.reminderMinutes ?: s.defaultReminderMinutes,
                    askForNote = habit.askForNote,
                    showOnWidget = habit.showOnWidget,
                    weekStart = s.weekStart,
                )
            }
        }
    }

    fun edit(change: (HabitForm) -> HabitForm) = _form.update(change)

    fun toggleDay(day: DayOfWeek) = _form.update { it.copy(daysMask = it.daysMask xor (1 shl (day.value - 1))) }

    fun save() {
        val f = _form.value
        if (!f.canSave) return
        val base = original ?: Habit(name = "", icon = "", colorHue = 0)
        val habit = base.copy(
            name = f.name.trim(),
            icon = f.icon.key,
            colorHue = f.hue,
            trackType = f.trackType,
            target = when {
                f.trackType == TrackType.CHECK -> 1.0
                f.trackType == TrackType.DURATION && f.durationUnit == DurationUnit.HOURS -> f.targetValue!! * 60
                else -> f.targetValue!!
            },
            unit = if (f.trackType == TrackType.AMOUNT) f.unit.trim() else "",
            durationUnit = f.durationUnit,
            daysMask = f.daysMask,
            reminderMinutes = if (f.reminderOn) f.reminderMinutes else null,
            askForNote = f.askForNote,
            showOnWidget = f.showOnWidget,
        )
        viewModelScope.launch {
            repository.saveHabit(habit)
            _saved.send(Unit)
        }
    }

    fun archive() {
        val habit = original ?: return
        viewModelScope.launch {
            repository.setArchived(habit.id, !habit.archived)
            _saved.send(Unit)
        }
    }

    fun delete() {
        val habit = original ?: return
        viewModelScope.launch {
            repository.deleteHabit(habit)
            _saved.send(Unit)
        }
    }

    val isArchived: Boolean get() = original?.archived == true

    private fun formatTarget(value: Double) = app.sprout.habits.domain.formatNumber(value)
}
