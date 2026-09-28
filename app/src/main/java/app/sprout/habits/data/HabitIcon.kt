package app.sprout.habits.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.sprout.habits.R

/** The icon picker's groups, in the order the sheet shows them. */
enum class IconGroup(@StringRes val label: Int) {
    HEALTH(R.string.icon_group_health),
    FITNESS(R.string.icon_group_fitness),
    MIND(R.string.icon_group_mind),
    WORK_AND_HOME(R.string.icon_group_work_and_home),
}

/**
 * The bundled habit icons: Material Symbols Rounded, one vector drawable each. [key] is what
 * `habit.icon` stores (the symbol's name), so backups keep working when drawables change.
 */
enum class HabitIcon(val key: String, @StringRes val label: Int, val group: IconGroup, @DrawableRes val drawable: Int) {
    WATER("water_drop", R.string.icon_water, IconGroup.HEALTH, R.drawable.ic_sym_water_drop),
    FOOD("restaurant", R.string.icon_food, IconGroup.HEALTH, R.drawable.ic_sym_restaurant),
    FRUIT("nutrition", R.string.icon_fruit, IconGroup.HEALTH, R.drawable.ic_sym_nutrition),
    MEDICINE("medication", R.string.icon_medicine, IconGroup.HEALTH, R.drawable.ic_sym_medication),
    SLEEP("bed", R.string.icon_sleep, IconGroup.HEALTH, R.drawable.ic_sym_bed),
    TEETH("dentistry", R.string.icon_teeth, IconGroup.HEALTH, R.drawable.ic_sym_dentistry),
    HEALTH("favorite", R.string.icon_health, IconGroup.HEALTH, R.drawable.ic_sym_favorite),
    NO_SMOKING("smoke_free", R.string.icon_no_smoking, IconGroup.HEALTH, R.drawable.ic_sym_smoke_free),

    RUN("directions_run", R.string.icon_run, IconGroup.FITNESS, R.drawable.ic_sym_directions_run),
    WALK("directions_walk", R.string.icon_walk, IconGroup.FITNESS, R.drawable.ic_sym_directions_walk),
    CYCLE("directions_bike", R.string.icon_cycle, IconGroup.FITNESS, R.drawable.ic_sym_directions_bike),
    GYM("fitness_center", R.string.icon_gym, IconGroup.FITNESS, R.drawable.ic_sym_fitness_center),
    MEDITATE("self_improvement", R.string.icon_meditate, IconGroup.FITNESS, R.drawable.ic_sym_self_improvement),
    OUTDOORS("park", R.string.icon_outdoors, IconGroup.FITNESS, R.drawable.ic_sym_park),

    READ("menu_book", R.string.icon_read, IconGroup.MIND, R.drawable.ic_sym_menu_book),
    WRITE("edit_note", R.string.icon_write, IconGroup.MIND, R.drawable.ic_sym_edit_note),
    LEARN("psychology", R.string.icon_learn, IconGroup.MIND, R.drawable.ic_sym_psychology),
    MUSIC("music_note", R.string.icon_music, IconGroup.MIND, R.drawable.ic_sym_music_note),
    MORNING("wb_sunny", R.string.icon_morning, IconGroup.MIND, R.drawable.ic_sym_wb_sunny),
    WAKE_UP("alarm", R.string.icon_wake_up, IconGroup.MIND, R.drawable.ic_sym_alarm),

    WORK("laptop_mac", R.string.icon_work, IconGroup.WORK_AND_HOME, R.drawable.ic_sym_laptop_mac),
    CODE("code", R.string.icon_code, IconGroup.WORK_AND_HOME, R.drawable.ic_sym_code),
    MONEY("savings", R.string.icon_money, IconGroup.WORK_AND_HOME, R.drawable.ic_sym_savings),
    LESS_PHONE("mobile_off", R.string.icon_less_phone, IconGroup.WORK_AND_HOME, R.drawable.ic_sym_mobile_off),
    CLEAN("cleaning_services", R.string.icon_clean, IconGroup.WORK_AND_HOME, R.drawable.ic_sym_cleaning_services),
    COOK("skillet", R.string.icon_cook, IconGroup.WORK_AND_HOME, R.drawable.ic_sym_skillet),
    TEA_OR_COFFEE("local_cafe", R.string.icon_tea_or_coffee, IconGroup.WORK_AND_HOME, R.drawable.ic_sym_local_cafe),
    PLANTS("potted_plant", R.string.icon_plants, IconGroup.WORK_AND_HOME, R.drawable.ic_sym_potted_plant),
    FAMILY_AND_FRIENDS("group", R.string.icon_family_and_friends, IconGroup.WORK_AND_HOME, R.drawable.ic_sym_group);

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
