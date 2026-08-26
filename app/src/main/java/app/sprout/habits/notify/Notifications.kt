package app.sprout.habits.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object Notifications {
    /** One notification per habit reminder. */
    const val CHANNEL_REMINDERS = "reminders"

    /** The evening nudge to add a note to a skipped habit. */
    const val CHANNEL_NOTES = "notes"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_REMINDERS, "Habit reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "A reminder at the time you set for each habit."
                },
                NotificationChannel(CHANNEL_NOTES, "Note reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "An evening reminder to add a note when you skip a habit that asks for one."
                },
            ),
        )
    }

    /** True when the app may post notifications right now. */
    fun canPost(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    }

    /** Opens "Alarms & reminders" for this app (Android 12+), where exact alarms are allowed. */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /** Opens the system screen for this app's notifications. */
    fun openSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
