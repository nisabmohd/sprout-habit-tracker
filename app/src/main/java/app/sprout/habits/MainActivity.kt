package app.sprout.habits

import app.sprout.habits.ui.update.UpdatePrompt
import app.sprout.habits.notify.Notifications
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
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

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // After a language change the activity is recreated but view models survive, holding
        // text formatted in the old language ("Monday, 28 September"). Start them afresh.
        if (savedInstanceState != null && savedInstanceState.getString(KEY_LANGUAGE) != language()) viewModelStore.clear()
        // Channel names follow the app language, which may have just changed.
        Notifications.createChannels(this)
        if (savedInstanceState == null) handleIntent(intent)
        enableEdgeToEdge()
        val container = (application as SproutApp).container
        setContent {
            // Null until DataStore has loaded, so the first real frame already has the right theme.
            // Until then draw an empty frame over the window background (light or night), so the
            // launch still produces a frame for the system and startup tooling.
            val settings by container.settings.settings.collectAsStateWithLifecycle(null)
            val current = settings ?: run {
                Spacer(Modifier.fillMaxSize())
                return@setContent
            }
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
                    UpdatePrompt(container.updates)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_LANGUAGE, language())
    }

    private fun language() = resources.configuration.locales.get(0).toLanguageTag()

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
        private const val KEY_LANGUAGE = "language"
    }
}
