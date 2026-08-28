package app.sprout.habits.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import app.sprout.habits.MainActivity
import app.sprout.habits.SproutApp
import app.sprout.habits.ui.theme.ThemeSettings
import app.sprout.habits.ui.theme.seedColorScheme
import kotlinx.coroutines.flow.first

/** Dynamic color on Android 12+ when it's on in settings, otherwise the seed palette. */
@Composable
fun SproutGlanceTheme(theme: ThemeSettings, content: @Composable () -> Unit) {
    val useDynamic = theme.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    GlanceTheme(
        colors = if (useDynamic) {
            GlanceTheme.colors
        } else {
            ColorProviders(light = seedColorScheme(theme.accentHue, false), dark = seedColorScheme(theme.accentHue, true))
        },
        content = content,
    )
}

val Context.container get() = (applicationContext as SproutApp).container

suspend fun Context.themeSettings(): ThemeSettings = container.settings.settings.first().theme

fun Context.openAppIntent(): Intent = Intent(this, MainActivity::class.java)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

fun Context.openHabitIntent(habitId: Long): Intent = openAppIntent()
    .setAction(MainActivity.ACTION_OPEN_HABIT)
    .putExtra(MainActivity.EXTRA_HABIT_ID, habitId)
    // Distinct data so each habit gets its own PendingIntent.
    .setData(android.net.Uri.parse("sprout://habit/$habitId"))
