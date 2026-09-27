package app.sprout.habits.data

import androidx.annotation.DrawableRes
import app.sprout.habits.R

/** The icon picker's groups, in the order the sheet shows them. */
enum class IconGroup(val label: String) {
    HEALTH("Health"),
    FITNESS("Fitness"),
    MIND("Mind"),
    WORK_AND_HOME("Work and home"),
}

/**
 * The bundled habit icons: Material Symbols Rounded, one vector drawable each. [key] is what
 * `habit.icon` stores (the symbol's name), so backups keep working when drawables change.
 */
enum class HabitIcon(val key: String, val label: String, val group: IconGroup, @DrawableRes val drawable: Int) {
    WATER("water_drop", "Water", IconGroup.HEALTH, R.drawable.ic_sym_water_drop),
    FOOD("restaurant", "Food", IconGroup.HEALTH, R.drawable.ic_sym_restaurant),
    FRUIT("nutrition", "Fruit", IconGroup.HEALTH, R.drawable.ic_sym_nutrition),
    MEDICINE("medication", "Medicine", IconGroup.HEALTH, R.drawable.ic_sym_medication),
    SLEEP("bed", "Sleep", IconGroup.HEALTH, R.drawable.ic_sym_bed),
    TEETH("dentistry", "Teeth", IconGroup.HEALTH, R.drawable.ic_sym_dentistry),
    HEALTH("favorite", "Health", IconGroup.HEALTH, R.drawable.ic_sym_favorite),
    NO_SMOKING("smoke_free", "No smoking", IconGroup.HEALTH, R.drawable.ic_sym_smoke_free),

    RUN("directions_run", "Run", IconGroup.FITNESS, R.drawable.ic_sym_directions_run),
    WALK("directions_walk", "Walk", IconGroup.FITNESS, R.drawable.ic_sym_directions_walk),
    CYCLE("directions_bike", "Cycle", IconGroup.FITNESS, R.drawable.ic_sym_directions_bike),
    GYM("fitness_center", "Gym", IconGroup.FITNESS, R.drawable.ic_sym_fitness_center),
    MEDITATE("self_improvement", "Meditate", IconGroup.FITNESS, R.drawable.ic_sym_self_improvement),
    OUTDOORS("park", "Outdoors", IconGroup.FITNESS, R.drawable.ic_sym_park),

    READ("menu_book", "Read", IconGroup.MIND, R.drawable.ic_sym_menu_book),
    WRITE("edit_note", "Write", IconGroup.MIND, R.drawable.ic_sym_edit_note),
    LEARN("psychology", "Learn", IconGroup.MIND, R.drawable.ic_sym_psychology),
    MUSIC("music_note", "Music", IconGroup.MIND, R.drawable.ic_sym_music_note),
    MORNING("wb_sunny", "Morning", IconGroup.MIND, R.drawable.ic_sym_wb_sunny),
    WAKE_UP("alarm", "Wake up", IconGroup.MIND, R.drawable.ic_sym_alarm),

    WORK("laptop_mac", "Work", IconGroup.WORK_AND_HOME, R.drawable.ic_sym_laptop_mac),
    CODE("code", "Code", IconGroup.WORK_AND_HOME, R.drawable.ic_sym_code),
    MONEY("savings", "Money", IconGroup.WORK_AND_HOME, R.drawable.ic_sym_savings),
    LESS_PHONE("mobile_off", "Less phone", IconGroup.WORK_AND_HOME, R.drawable.ic_sym_mobile_off),
    CLEAN("cleaning_services", "Clean", IconGroup.WORK_AND_HOME, R.drawable.ic_sym_cleaning_services),
    COOK("skillet", "Cook", IconGroup.WORK_AND_HOME, R.drawable.ic_sym_skillet),
    TEA_OR_COFFEE("local_cafe", "Tea or coffee", IconGroup.WORK_AND_HOME, R.drawable.ic_sym_local_cafe),
    PLANTS("potted_plant", "Plants", IconGroup.WORK_AND_HOME, R.drawable.ic_sym_potted_plant),
    FAMILY_AND_FRIENDS("group", "Family and friends", IconGroup.WORK_AND_HOME, R.drawable.ic_sym_group);

    companion object {
        /** Keys from 0.3.0 and earlier, when Sprout had 8 icons of its own. */
        private val LEGACY = mapOf(
            "sun" to MORNING,
            "book" to READ,
            "drop" to WATER,
            "calm" to MEDITATE,
            "dumbbell" to GYM,
            "pill" to MEDICINE,
            "leaf" to OUTDOORS,
            "pen" to WRITE,
        )

        /** The New habit row before "More": water, food, sleep, run, read, medicine, meditate. */
        val SUGGESTED = listOf(WATER, FOOD, SLEEP, RUN, READ, MEDICINE, MEDITATE)

        fun fromKey(key: String): HabitIcon = entries.firstOrNull { it.key == key } ?: LEGACY[key] ?: OUTDOORS

        /** The suggestions, with [current] first when it isn't one of them, still 7 in all. */
        fun suggestedFor(current: HabitIcon): List<HabitIcon> =
            if (current in SUGGESTED) SUGGESTED else listOf(current) + SUGGESTED.dropLast(1)
    }
}
