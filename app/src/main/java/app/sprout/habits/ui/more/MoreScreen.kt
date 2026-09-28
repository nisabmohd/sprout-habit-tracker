package app.sprout.habits.ui.more

import app.sprout.habits.ui.update.UpdateBadge
import app.sprout.habits.domain.isNewerVersion
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.res.stringResource
import app.sprout.habits.ui.components.SproutSheet
import android.app.Activity
import android.os.Build
import app.sprout.habits.AppLanguage
import app.sprout.habits.ui.components.ChoiceRow
import app.sprout.habits.ui.components.ChoiceSheet
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
import app.sprout.habits.ui.components.ColorSwatchRow
import app.sprout.habits.ui.components.Swatch
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
import app.sprout.habits.ui.components.TimePickerSheet
import androidx.compose.foundation.layout.navigationBarsPadding

/** Accent choices when dynamic color is off: the app's green first, then the habit hues. */
private val ACCENT_HUES = listOf(150f, 192f, 215f, 275f, 330f, 12f, 38f)
private val ACCENT_NAMES = mapOf(
    150f to R.string.color_green, 192f to R.string.color_teal, 215f to R.string.color_blue, 275f to R.string.color_purple,
    330f to R.string.color_pink, 12f to R.string.color_rust, 38f to R.string.color_gold,
)

private val TEXT_SCALES = listOf(0.85f to R.string.text_small, 1f to R.string.text_default, 1.15f to R.string.text_large, 1.3f to R.string.text_largest)

private val FONT_INFO = mapOf(
    // Font names stay as they are in every language; null = "System default".
    BodyFont.SYSTEM to (null to R.string.font_system_desc),
    BodyFont.FIGTREE to ("Figtree" to R.string.font_figtree_desc),
    BodyFont.OUTFIT to ("Outfit" to R.string.font_outfit_desc),
    BodyFont.LEXEND to ("Lexend" to R.string.font_lexend_desc),
    BodyFont.ATKINSON to ("Atkinson Hyperlegible" to R.string.font_atkinson_desc),
)

@OptIn(ExperimentalMaterial3Api::class)
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
    var pickingLanguage by remember { mutableStateOf(false) }
    fun save(block: suspend SettingsRepository.() -> Unit) { scope.launch { repository.block() } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "title") {
                Text(stringResource(R.string.tab_more), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp))
            }

            item(key = "backup") {
                SectionLabel(stringResource(R.string.backup_restore))
                SettingsCard { BackupRows(backup) { msg -> scope.launch { snackbar.showSnackbar(msg) } } }
            }

            item(key = "appearance") {
                SectionLabel(stringResource(R.string.appearance))
                SettingsCard {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleMedium)
                        // Each segment is a fixed share of the row, so labels stop scaling at 1.3x.
                        CappedFontScale {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            val modes = listOf(ThemeMode.SYSTEM to stringResource(R.string.theme_system), ThemeMode.LIGHT to stringResource(R.string.theme_light), ThemeMode.DARK to stringResource(R.string.theme_dark))
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
                        SwitchRow(stringResource(R.string.dynamic_color), stringResource(R.string.dynamic_color_desc), theme.dynamicColor) { on -> save { setDynamicColor(on) } }
                    }
                    if (!dynamicSupported || !theme.dynamicColor) {
                        AccentPicker(theme.accentHue) { hue -> save { setAccentHue(hue) } }
                    }
                    Text(stringResource(R.string.font), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp))
                    BodyFont.entries.forEach { font ->
                        FontRow(font, selected = theme.font == font) { save { setFont(font) } }
                    }
                    TextSizeRow(theme.textScale) { scale -> save { setTextScale(scale) } }
                }
            }

            item(key = "general") {
                SectionLabel(stringResource(R.string.general))
                SettingsCard {
                    SettingsRow(stringResource(R.string.language), onClick = { pickingLanguage = true }) {
                        val tag = AppLanguage.current(context)
                        TrailingValue(AppLanguage.LANGUAGES.firstOrNull { it.tag == tag }?.nativeName ?: stringResource(R.string.language_system))
                        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, modifier = Modifier.padding(start = 4.dp).size(20.dp))
                    }
                    CardDivider()
                    SettingsRow(stringResource(R.string.week_starts_on), onClick = { pickingWeekStart = true }) {
                        TrailingValue(settings.weekStart.getDisplayName(TextStyle.FULL, Locale.getDefault()))
                    }
                    CardDivider()
                    SettingsRow(stringResource(R.string.default_reminder), stringResource(R.string.default_reminder_desc), onClick = { pickingReminder = true }) {
                        TrailingValue(TodayViewModel.formatTime(settings.defaultReminderMinutes))
                    }
                    CardDivider()
                    SettingsRow(stringResource(R.string.notifications), onClick = { Notifications.openSettings(context) }) { TrailingValue(stringResource(R.string.system_settings)) }
                }
            }

            item(key = "about") {
                Box(Modifier.padding(top = 12.dp)) {
                    SettingsCard {
                        val update = settings.availableUpdate?.takeIf { isNewerVersion(it, BuildConfig.VERSION_NAME) }
                        SettingsRow(
                            stringResource(R.string.about_sprout),
                            stringResource(R.string.version_open_source, BuildConfig.VERSION_NAME),
                            icon = R.drawable.ic_seedling,
                            onClick = onOpenAbout,
                            badge = if (update != null) { { UpdateBadge() } } else null,
                        ) {
                            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (pickingLanguage) {
        LanguageSheet(
            current = AppLanguage.current(context),
            onPick = { tag ->
                pickingLanguage = false
                (context as? Activity)?.let { AppLanguage.set(it, tag) }
            },
            onDismiss = { pickingLanguage = false },
        )
    }

    if (pickingWeekStart) {
        ChoiceSheet(title = stringResource(R.string.week_starts_on), onDismiss = { pickingWeekStart = false }) {
            listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY, DayOfWeek.SATURDAY).forEach { day ->
                ChoiceRow(day.getDisplayName(TextStyle.FULL, Locale.getDefault()), null, settings.weekStart == day) {
                    save { setWeekStart(day) }
                    pickingWeekStart = false
                }
            }
        }
    }

    if (pickingReminder) {
        TimePickerSheet(
            title = stringResource(R.string.default_reminder),
            initialMinutes = settings.defaultReminderMinutes,
            onApply = { minutes -> save { setDefaultReminderMinutes(minutes) }; pickingReminder = false },
            onDismiss = { pickingReminder = false },
        )
    }
}

@Composable
private fun AccentPicker(selectedHue: Float, onSelect: (Float) -> Unit) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.accent), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
        ColorSwatchRow(
            swatches = ACCENT_HUES.map { hue ->
                val scheme = seedColorScheme(hue, false)
                Swatch(scheme.primary, scheme.onPrimary, stringResource(ACCENT_NAMES.getValue(hue)))
            },
            selected = ACCENT_HUES.indexOf(selectedHue),
            onSelect = { i -> onSelect(ACCENT_HUES[i]) },
        )
    }
}

@Composable
private fun FontRow(font: BodyFont, selected: Boolean, onClick: () -> Unit) {
    val (fontName, descriptionId) = FONT_INFO.getValue(font)
    val name = fontName ?: stringResource(R.string.font_system)
    val description = stringResource(descriptionId)
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
    val label = stringResource(TEXT_SCALES[index].second)
    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.text_size), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = index.toFloat(),
            onValueChange = { onChange(TEXT_SCALES[it.roundToInt()].first) },
            valueRange = 0f..(TEXT_SCALES.size - 1).toFloat(),
            steps = TEXT_SCALES.size - 2,
            modifier = Modifier.semantics { stateDescription = label },
        )
    }
}


