package app.sprout.habits.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import app.sprout.habits.MainActivity
import app.sprout.habits.SproutApp
import app.sprout.habits.ui.theme.DEFAULT_ACCENT_HUE
import app.sprout.habits.ui.theme.seedColorScheme

/**
 * Widgets live on the home screen, so they follow the system: wallpaper colors on Android 12+
 * and Sprout's default palette below that. The in-app theme and accent only change the app.
 */
@Composable
fun SproutGlanceTheme(content: @Composable () -> Unit) {
    GlanceTheme(
        colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            GlanceTheme.colors
        } else {
            ColorProviders(light = seedColorScheme(DEFAULT_ACCENT_HUE, false), dark = seedColorScheme(DEFAULT_ACCENT_HUE, true))
        },
        content = content,
    )
}

val Context.container get() = (applicationContext as SproutApp).container

fun Context.openAppIntent(): Intent = Intent(this, MainActivity::class.java)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

fun Context.openHabitIntent(habitId: Long): Intent = openAppIntent()
    .setAction(MainActivity.ACTION_OPEN_HABIT)
    .putExtra(MainActivity.EXTRA_HABIT_ID, habitId)
    // Distinct data so each habit gets its own PendingIntent.
    .setData(android.net.Uri.parse("sprout://habit/$habitId"))
