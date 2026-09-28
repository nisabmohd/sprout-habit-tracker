package app.sprout.habits

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * UI text for code outside Compose (view models, notifications, widgets). Reads the resources on
 * every call, so a change of app language shows up without restarting the process.
 */
interface Strings {
    operator fun invoke(@StringRes id: Int, vararg args: Any): String
    fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String
}

class ResourceStrings(private val context: Context) : Strings {
    override fun invoke(id: Int, vararg args: Any): String = context.getString(id, *args)
    override fun plural(id: Int, count: Int, vararg args: Any): String = context.resources.getQuantityString(id, count, *args)
}

/** [Strings] for Compose code that calls a helper shared with the view models. */
@Composable
fun rememberStrings(): Strings {
    val context = LocalContext.current
    return remember(context) { ResourceStrings(context) }
}
