package app.sprout.habits.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.sprout.habits.ui.theme.BodyFont
import app.sprout.habits.ui.theme.DEFAULT_ACCENT_HUE
import app.sprout.habits.ui.theme.ThemeMode
import app.sprout.habits.ui.theme.ThemeSettings
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Immutable
data class Settings(
    val theme: ThemeSettings = ThemeSettings(),
    val weekStart: DayOfWeek = DayOfWeek.MONDAY,
    /** Minutes after midnight used as the default for new habit reminders. */
    val defaultReminderMinutes: Int = 9 * 60,
    val backupEnabled: Boolean = false,
    /** Epoch millis of the last successful backup, or null if never. */
    val lastBackupAt: Long? = null,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store: DataStore<Preferences> = context.dataStore

    val settings: Flow<Settings> = store.data.map { it.toSettings() }.distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[THEME] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = store.edit { it[DYNAMIC_COLOR] = enabled }
    suspend fun setAccentHue(hue: Float) = store.edit { it[ACCENT_HUE] = hue }
    suspend fun setFont(font: BodyFont) = store.edit { it[FONT] = font.name }
    suspend fun setTextScale(scale: Float) = store.edit { it[TEXT_SCALE] = scale }
    suspend fun setWeekStart(day: DayOfWeek) = store.edit { it[WEEK_START] = day.name }
    suspend fun setDefaultReminderMinutes(minutes: Int) = store.edit { it[DEFAULT_REMINDER] = minutes }
    suspend fun setBackupEnabled(enabled: Boolean) = store.edit { it[BACKUP_ENABLED] = enabled }
    suspend fun setLastBackupAt(epochMillis: Long) = store.edit { it[LAST_BACKUP_AT] = epochMillis }

    private fun Preferences.toSettings(): Settings {
        val defaults = Settings()
        val theme = defaults.theme
        return Settings(
            theme = ThemeSettings(
                mode = enumOrDefault(this[THEME], theme.mode),
                dynamicColor = this[DYNAMIC_COLOR] ?: theme.dynamicColor,
                accentHue = this[ACCENT_HUE] ?: DEFAULT_ACCENT_HUE,
                font = enumOrDefault(this[FONT], theme.font),
                textScale = this[TEXT_SCALE] ?: theme.textScale,
            ),
            weekStart = enumOrDefault(this[WEEK_START], defaults.weekStart),
            defaultReminderMinutes = this[DEFAULT_REMINDER] ?: defaults.defaultReminderMinutes,
            backupEnabled = this[BACKUP_ENABLED] ?: defaults.backupEnabled,
            lastBackupAt = this[LAST_BACKUP_AT],
        )
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamicColor")
        val ACCENT_HUE = floatPreferencesKey("accentHue")
        val FONT = stringPreferencesKey("font")
        val TEXT_SCALE = floatPreferencesKey("textScale")
        val WEEK_START = stringPreferencesKey("weekStart")
        val DEFAULT_REMINDER = intPreferencesKey("defaultReminderMinutes")
        val BACKUP_ENABLED = booleanPreferencesKey("backupEnabled")
        val LAST_BACKUP_AT = longPreferencesKey("lastBackupAt")

        inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
            name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default
    }
}
