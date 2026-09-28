package app.sprout.habits.support

/** At most one prompt per launch: the update dialog and the support prompt never follow each other. */
object LaunchPrompts {
    @Volatile
    private var shown = false

    /** Claims this launch's prompt; false if another one already showed. */
    fun claim(): Boolean = synchronized(this) { if (shown) false else { shown = true; true } }

    fun available(): Boolean = !shown
}
