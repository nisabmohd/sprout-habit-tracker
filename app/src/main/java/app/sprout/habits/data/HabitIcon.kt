package app.sprout.habits.data

import androidx.annotation.DrawableRes
import app.sprout.habits.R

/** The bundled habit icons. [key] is what `habit.icon` stores. */
enum class HabitIcon(val key: String, @DrawableRes val drawable: Int, val defaultHue: Int) {
    SUN("sun", R.drawable.ic_habit_sun, 38),
    BOOK("book", R.drawable.ic_habit_book, 215),
    DROP("drop", R.drawable.ic_habit_drop, 192),
    CALM("calm", R.drawable.ic_habit_calm, 275),
    DUMBBELL("dumbbell", R.drawable.ic_habit_dumbbell, 12),
    PILL("pill", R.drawable.ic_habit_pill, 95),
    LEAF("leaf", R.drawable.ic_habit_leaf, 150),
    PEN("pen", R.drawable.ic_habit_pen, 330);

    companion object {
        fun fromKey(key: String): HabitIcon = entries.firstOrNull { it.key == key } ?: LEAF
    }
}
