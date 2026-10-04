package app.sprout.habits.domain

/**
 * The text a note form shows when the chosen habit and day already have a note: the [saved] note
 * first, then what the user [typed] before getting there, so neither is lost.
 */
fun joinNoteText(saved: String?, typed: String): String = when {
    saved.isNullOrBlank() -> typed
    typed.isBlank() || typed.trim() == saved.trim() -> saved
    else -> "$saved\n\n$typed"
}
