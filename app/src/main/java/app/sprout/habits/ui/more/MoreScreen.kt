package app.sprout.habits.ui.more

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.sprout.habits.BuildConfig
import app.sprout.habits.R
import app.sprout.habits.ui.components.CappedFontScale
import app.sprout.habits.data.Settings
import app.sprout.habits.data.SettingsRepository
import app.sprout.habits.data.backup.BackupManager
import app.sprout.habits.notify.Notifications
import app.sprout.habits.ui.theme.BodyFont
import app.sprout.habits.ui.theme.ThemeMode
import app.sprout.habits.ui.theme.family
import app.sprout.habits.ui.theme.seedColorScheme
import app.sprout.habits.ui.today.TodayViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Accent choices when dynamic color is off: the app's green first, then the habit hues. */
private val ACCENT_HUES = listOf(150f, 192f, 215f, 275f, 330f, 12f, 38f)

private val TEXT_SCALES = listOf(0.85f to "Small", 1f to "Default", 1.15f to "Large", 1.3f to "Largest")

private val FONT_INFO = mapOf(
    BodyFont.SYSTEM to ("System default" to "Your phone's font"),
    BodyFont.FIGTREE to ("Figtree" to "Friendly and clear"),
    BodyFont.OUTFIT to ("Outfit" to "Rounded, geometric"),
    BodyFont.LEXEND to ("Lexend" to "Default · built for easy reading"),
    BodyFont.ATKINSON to ("Atkinson Hyperlegible" to "Designed for low vision"),
)

@Composable
fun MoreScreen(
    settings: Settings,
    repository: SettingsRepository,
    backup: BackupManager,
    onOpenAbout: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val theme = settings.theme
    var pickingWeekStart by remember { mutableStateOf(false) }
    var pickingReminder by remember { mutableStateOf(false) }
    fun save(block: suspend SettingsRepository.() -> Unit) { scope.launch { repository.block() } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "title") {
                Text("More", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp))
            }

            item(key = "backup") {
                SectionLabel("Backup & restore")
                SettingsCard { BackupRows(backup) { msg -> scope.launch { snackbar.showSnackbar(msg) } } }
            }

            item(key = "appearance") {
                SectionLabel("Appearance")
                SettingsCard {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Theme", style = MaterialTheme.typography.titleMedium)
                        // Each segment is a fixed share of the row, so labels stop scaling at 1.3x.
                        CappedFontScale {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            val modes = listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")
                            modes.forEachIndexed { i, (mode, label) ->
                                SegmentedButton(
                                    selected = theme.mode == mode,
                                    onClick = { save { setThemeMode(mode) } },
                                    shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                                    icon = {},
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                ) { Text(label, style = MaterialTheme.typography.labelLarge) }
                            }
                        }
                        }
                    }
                    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    if (dynamicSupported) {
                        SwitchRow("Dynamic color", "Match your wallpaper", theme.dynamicColor) { on -> save { setDynamicColor(on) } }
                    }
                    if (!dynamicSupported || !theme.dynamicColor) {
                        AccentPicker(theme.accentHue) { hue -> save { setAccentHue(hue) } }
                    }
                    Text("Font", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp))
                    BodyFont.entries.forEach { font ->
                        FontRow(font, selected = theme.font == font) { save { setFont(font) } }
                    }
                    TextSizeRow(theme.textScale) { scale -> save { setTextScale(scale) } }
                }
            }

            item(key = "general") {
                SectionLabel("General")
                SettingsCard {
                    SettingsRow("Week starts on", onClick = { pickingWeekStart = true }) {
                        TrailingValue(settings.weekStart.getDisplayName(TextStyle.FULL, Locale.getDefault()))
                    }
                    CardDivider()
                    SettingsRow("Default reminder", "Suggested time for new reminders", onClick = { pickingReminder = true }) {
                        TrailingValue(TodayViewModel.formatTime(settings.defaultReminderMinutes))
                    }
                    CardDivider()
                    SettingsRow("Notifications", onClick = { Notifications.openSettings(context) }) { TrailingValue("System settings") }
                }
            }

            item(key = "about") {
                Box(Modifier.padding(top = 12.dp)) {
                    SettingsCard {
                        SettingsRow("About Sprout", "Version ${BuildConfig.VERSION_NAME} · open source", icon = R.drawable.ic_seedling, onClick = onOpenAbout) {
                            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (pickingWeekStart) {
        AlertDialog(
            onDismissRequest = { pickingWeekStart = false },
            title = { Text("Week starts on") },
            text = {
                Column {
                    listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY, DayOfWeek.SATURDAY).forEach { day ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(settings.weekStart == day, role = Role.RadioButton) {
                                    save { setWeekStart(day) }
                                    pickingWeekStart = false
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = settings.weekStart == day, onClick = null)
                            Text(day.getDisplayName(TextStyle.FULL, Locale.getDefault()), modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickingWeekStart = false }) { Text("Close") } },
        )
    }
    if (pickingReminder) {
        ReminderTimeDialog(
            settings.defaultReminderMinutes,
            onConfirm = { minutes -> save { setDefaultReminderMinutes(minutes) }; pickingReminder = false },
            onDismiss = { pickingReminder = false },
        )
    }
}

@Composable
private fun AccentPicker(selectedHue: Float, onSelect: (Float) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Accent", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ACCENT_HUES.forEach { hue ->
                val selected = hue == selectedHue
                val color = seedColorScheme(hue, false).primary
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(3.dp, if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent, CircleShape)
                        .selectable(selected, role = Role.RadioButton) { onSelect(hue) }
                        .semantics { contentDescription = "Accent color" },
                )
            }
        }
    }
}

@Composable
private fun FontRow(font: BodyFont, selected: Boolean, onClick: () -> Unit) {
    val (name, description) = FONT_INFO.getValue(font)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(name, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = font.family()))
            Text(description, style = MaterialTheme.typography.bodySmall.copy(fontFamily = font.family()), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("Aa", style = MaterialTheme.typography.titleLarge.copy(fontFamily = font.family()), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TextSizeRow(scale: Float, onChange: (Float) -> Unit) {
    // Unknown saved value → show "Default"; index 0 (Small) is a real choice.
    val index = TEXT_SCALES.indexOfFirst { it.first == scale }.takeIf { it >= 0 } ?: 1
    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Text size", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(TEXT_SCALES[index].second, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = index.toFloat(),
            onValueChange = { onChange(TEXT_SCALES[it.roundToInt()].first) },
            valueRange = 0f..(TEXT_SCALES.size - 1).toFloat(),
            steps = TEXT_SCALES.size - 2,
            modifier = Modifier.semantics { stateDescription = TEXT_SCALES[index].second },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(initialMinutes: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = initialMinutes / 60, initialMinute = initialMinutes % 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state) },
    )
}
