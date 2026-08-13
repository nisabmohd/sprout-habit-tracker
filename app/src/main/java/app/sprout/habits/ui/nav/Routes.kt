package app.sprout.habits.ui.nav

import kotlinx.serialization.Serializable

@Serializable data object TodayRoute
@Serializable data object HabitsRoute
@Serializable data object JournalRoute
@Serializable data object InsightsRoute
@Serializable data object MoreRoute

/** New habit when [habitId] is 0, otherwise edit. */
@Serializable data class EditHabitRoute(val habitId: Long = 0L)

@Serializable data object ManageHabitsRoute
