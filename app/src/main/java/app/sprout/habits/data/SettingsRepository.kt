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
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
    /** False until the welcome screen has been passed once. */
    val onboarded: Boolean = false,
    /** Epoch millis of the first launch; null until then. */
    val firstOpenAt: Long? = null,
    /** Days logged as done or partly done, counted for the support prompt. */
    val checkInCount: Int = 0,
    val supportPromptShownAt: Long? = null,
    val supportPromptDismissCount: Int = 0,
    /** A newer release found on GitHub (github build), for the "Update available" pill. */
    val availableUpdate: String? = null,
    /** Epoch millis of the last automatic update check. */
    val lastUpdateCheckAt: Long? = null,
    /** The release the user answered "Later" to; its dialog doesn't come back on its own. */
    val dismissedUpdate: String? = null,
    /** Ids of the sample habits a fresh install starts with; empty once removed or never added. */
    val sampleHabitIds: Set<Long> = emptySet(),
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
    suspend fun setOnboarded() = store.edit { it[ONBOARDED] = true }
    /** Remembers the newest release found (or null when up to date) and when the check ran. */
    suspend fun setUpdateCheck(available: String?, at: Long) = store.edit {
        if (available != null) it[AVAILABLE_UPDATE] = available else it.remove(AVAILABLE_UPDATE)
        it[LAST_UPDATE_CHECK] = at
    }
    suspend fun dismissUpdate(version: String) = store.edit { it[DISMISSED_UPDATE] = version }
    suspend fun setSampleHabitIds(ids: Set<Long>) = store.edit {
        if (ids.isEmpty()) it.remove(SAMPLE_HABITS) else it[SAMPLE_HABITS] = ids.mapTo(HashSet()) { id -> id.toString() }
    }
    suspend fun markFirstOpen(now: Long = System.currentTimeMillis()) = store.edit { if (it[FIRST_OPEN_AT] == null) it[FIRST_OPEN_AT] = now }
    suspend fun addCheckIn() = store.edit { it[CHECK_INS] = (it[CHECK_INS] ?: 0) + 1 }
    suspend fun supportPromptShown(dismissed: Boolean, now: Long = System.currentTimeMillis()) = store.edit {
        it[PROMPT_SHOWN_AT] = now
        if (dismissed) it[PROMPT_DISMISSED] = (it[PROMPT_DISMISSED] ?: 0) + 1
    }

    /** Replaces the user's preferences in one write, e.g. when restoring a backup. */
    suspend fun restore(settings: Settings) = store.edit {
        it[THEME] = settings.theme.mode.name
        it[DYNAMIC_COLOR] = settings.theme.dynamicColor
        it[ACCENT_HUE] = settings.theme.accentHue
        it[FONT] = settings.theme.font.name
        it[TEXT_SCALE] = settings.theme.textScale
        it[WEEK_START] = settings.weekStart.name
        it[DEFAULT_REMINDER] = settings.defaultReminderMinutes
    }

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
            onboarded = this[ONBOARDED] ?: false,
            firstOpenAt = this[FIRST_OPEN_AT],
            checkInCount = this[CHECK_INS] ?: 0,
            supportPromptShownAt = this[PROMPT_SHOWN_AT],
            supportPromptDismissCount = this[PROMPT_DISMISSED] ?: 0,
            availableUpdate = this[AVAILABLE_UPDATE],
            lastUpdateCheckAt = this[LAST_UPDATE_CHECK],
            dismissedUpdate = this[DISMISSED_UPDATE],
            sampleHabitIds = this[SAMPLE_HABITS].orEmpty().mapNotNullTo(HashSet()) { it.toLongOrNull() },
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
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val FIRST_OPEN_AT = longPreferencesKey("firstOpenAt")
        val CHECK_INS = intPreferencesKey("checkInCount")
        val PROMPT_SHOWN_AT = longPreferencesKey("supportPromptShownAt")
        val PROMPT_DISMISSED = intPreferencesKey("supportPromptDismissCount")
        val AVAILABLE_UPDATE = stringPreferencesKey("availableUpdateVersion")
        val LAST_UPDATE_CHECK = longPreferencesKey("lastUpdateCheckAt")
        val DISMISSED_UPDATE = stringPreferencesKey("dismissedUpdateVersion")
        val SAMPLE_HABITS = stringSetPreferencesKey("sampleHabitIds")

        inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
            name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default
    }
}
