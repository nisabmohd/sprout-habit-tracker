package app.sprout.habits

import android.content.Intent
import app.sprout.habits.ui.more.BackupSection
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sprout.habits.data.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import app.sprout.habits.data.HabitIcon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import app.sprout.habits.ui.nav.SproutNavHost
import app.sprout.habits.ui.theme.BodyFont
import app.sprout.habits.ui.theme.SproutTheme
import app.sprout.habits.ui.theme.ThemeMode
import app.sprout.habits.ui.theme.ThemeSettings
import app.sprout.habits.ui.theme.habitColors
import app.sprout.habits.ui.theme.isDark

class MainActivity : ComponentActivity() {
    /** A habit to open, from a tapped notification or widget; consumed by the nav host. */
    private val openHabit = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            val container = (application as SproutApp).container
            val settingsRepo = container.settings
            val settings = settingsRepo.settings.collectAsStateWithLifecycle(Settings()).value.theme
            val scope = rememberCoroutineScope()
            val dark = settings.isDark()
            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.Transparent.toArgb())
                } else {
                    SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb())
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            SproutTheme(settings) {
                val habitToOpen by openHabit.collectAsStateWithLifecycle()
                SproutNavHost(container, habitToOpen = habitToOpen, onHabitOpened = { openHabit.value = null }, moreContent = {
                    // Temporary theme check until the More tab exists.
                    val snackbar = remember { SnackbarHostState() }
                    Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    ThemePreview(settings, onChange = { new ->
                        scope.launch {
                            settingsRepo.setThemeMode(new.mode)
                            settingsRepo.setDynamicColor(new.dynamicColor)
                            settingsRepo.setFont(new.font)
                            settingsRepo.setTextScale(new.textScale)
                        }
                    })
                    Box(Modifier.padding(16.dp)) {
                        BackupSection(container.backup, onMessage = { msg -> scope.launch { snackbar.showSnackbar(msg) } })
                    }
                    }
                    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
                    }
                })
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

@Composable
private fun ThemePreview(settings: ThemeSettings, onChange: (ThemeSettings) -> Unit) {
    val t = MaterialTheme.typography
    val c = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().background(c.background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Today", style = t.headlineMedium, color = c.onBackground)
        Text("New habit", style = t.titleLarge, color = c.onBackground)
        Text("Wake up at 7", style = t.titleMedium, color = c.onBackground)
        Text("Done at 6:52 AM", style = t.bodyMedium, color = c.onSurfaceVariant)
        Text("Today · Habits · Journal", style = t.labelMedium, color = c.onSurfaceVariant)
        Text("M T W T F S S", style = t.labelSmall, color = c.onSurfaceVariant)
        Text("15", style = t.displayLarge, color = c.primary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HabitIcon.entries.forEach { icon ->
                val h = habitColors(icon.defaultHue.toFloat())
                Box(Modifier.size(40.dp).background(h.soft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(icon.drawable), contentDescription = icon.key, tint = h.ink, modifier = Modifier.size(22.dp))
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ThemeMode.entries.forEach { m ->
                FilterChip(settings.mode == m, { onChange(settings.copy(mode = m)) }, { Text(m.name) })
            }
            FilterChip(settings.dynamicColor, { onChange(settings.copy(dynamicColor = !settings.dynamicColor)) }, { Text("Dynamic") })
            BodyFont.entries.forEach { f ->
                FilterChip(settings.font == f, { onChange(settings.copy(font = f)) }, { Text(f.name) })
            }
            listOf(0.85f, 1f, 1.15f, 1.3f).forEach { s ->
                FilterChip(settings.textScale == s, { onChange(settings.copy(textScale = s)) }, { Text("${s}x") })
            }
        }
    }
}
