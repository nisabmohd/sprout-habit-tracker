package app.sprout.habits.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import app.sprout.habits.ui.theme.ThemeSettings
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Widget data that stays current. Glance keeps a widget's session (and its composition) alive
 * between updates, so data read once in provideGlance would keep showing the old state; this
 * re-reads it whenever habits, entries or settings change, for as long as the session lives.
 */
@Composable
fun <T> liveWidgetData(context: Context, initial: T, load: suspend (WidgetDataSource) -> T): State<T> {
    val flow = remember {
        val c = context.container
        val source = WidgetDataSource(c.repository, c.settings)
        combine(c.repository.observeAllHabits(), c.repository.observeEntries(0, Long.MAX_VALUE), c.settings.settings) { _, _, _ -> }
            .map { load(source) }
    }
    return flow.collectAsState(initial)
}

@Composable
fun liveTheme(context: Context, initial: ThemeSettings): State<ThemeSettings> {
    val flow = remember { context.container.settings.settings.map { it.theme } }
    return flow.collectAsState(initial)
}
