package app.sprout.habits

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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import app.sprout.habits.ui.theme.BodyFont
import app.sprout.habits.ui.theme.SproutTheme
import app.sprout.habits.ui.theme.ThemeMode
import app.sprout.habits.ui.theme.ThemeSettings
import app.sprout.habits.ui.theme.habitColors
import app.sprout.habits.ui.theme.isDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Temporary: in-memory settings until DataStore and the More tab exist.
            var settings by remember { mutableStateOf(ThemeSettings()) }
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
                ThemePreview(settings, onChange = { settings = it })
            }
        }
    }
}

@Composable
private fun ThemePreview(settings: ThemeSettings, onChange: (ThemeSettings) -> Unit) {
    val t = MaterialTheme.typography
    val c = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().background(c.background).safeDrawingPadding().padding(16.dp),
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
            listOf(38f, 215f, 192f, 275f, 12f, 95f, 150f, 330f).forEach { hue ->
                val h = habitColors(hue)
                Box(Modifier.size(32.dp).background(h.solid, RoundedCornerShape(10.dp)))
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
