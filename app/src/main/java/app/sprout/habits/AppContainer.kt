package app.sprout.habits

import android.content.Context
import app.sprout.habits.data.HabitRepository
import app.sprout.habits.data.SproutDatabase

/** Holds the app's long-lived objects. The only dependency "graph" in the app. */
class AppContainer(context: Context) {
    private val database by lazy { SproutDatabase.create(context) }
    val repository by lazy { HabitRepository(database) }
}
