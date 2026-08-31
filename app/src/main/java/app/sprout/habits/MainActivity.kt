package app.sprout.habits

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sprout.habits.ui.WelcomeScreen
import app.sprout.habits.ui.nav.SproutNavHost
import app.sprout.habits.ui.theme.SproutTheme
import app.sprout.habits.ui.theme.isDark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    /** A habit to open, from a tapped notification or widget; consumed by the nav host. */
    private val openHabit = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        enableEdgeToEdge()
        val container = (application as SproutApp).container
        setContent {
            // Null until DataStore has loaded, so the first frame already has the right theme
            // (the window background, light or night, shows until then).
            val settings by container.settings.settings.collectAsStateWithLifecycle(null)
            val current = settings ?: return@setContent
            val dark = current.theme.isDark()
            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.Transparent.toArgb())
                } else {
                    SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb())
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            val scope = rememberCoroutineScope()
            SproutTheme(current.theme) {
                if (!current.onboarded) {
                    WelcomeScreen(onStart = { scope.launch { container.settings.setOnboarded() } })
                } else {
                    val habitToOpen by openHabit.collectAsStateWithLifecycle()
                    SproutNavHost(container, current, habitToOpen = habitToOpen, onHabitOpened = { openHabit.value = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_OPEN_HABIT) {
            intent.getLongExtra(EXTRA_HABIT_ID, 0L).takeIf { it != 0L }?.let { openHabit.value = it }
        }
    }

    companion object {
        const val ACTION_OPEN_HABIT = "app.sprout.habits.action.OPEN_HABIT"
        const val EXTRA_HABIT_ID = "habitId"
    }
}
