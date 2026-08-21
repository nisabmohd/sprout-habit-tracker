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

@Serializable data class HabitDetailRoute(val habitId: Long)

/** Edit note [noteId], or write a new one (0) for [habitId] on [epochDay] (−1 = today). */
@Serializable data class WriteNoteRoute(val noteId: Long = 0L, val habitId: Long = 0L, val epochDay: Long = -1L)
