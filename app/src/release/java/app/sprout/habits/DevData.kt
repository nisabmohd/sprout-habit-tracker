package app.sprout.habits

import app.sprout.habits.data.HabitRepository

/** Sample data exists only in debug builds. */
object DevData {
    suspend fun seedIfEmpty(repository: HabitRepository) = Unit
}
