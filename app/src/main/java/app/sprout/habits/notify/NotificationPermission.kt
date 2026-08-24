package app.sprout.habits.notify

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** Whether notifications are allowed, plus a way to ask on Android 13+. */
class NotificationPermissionState(val granted: Boolean, val request: () -> Unit)

@Composable
fun rememberNotificationPermission(): NotificationPermissionState {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(Notifications.canPost(context)) }
    // The user may change it in system settings while the app is in the background.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = Notifications.canPost(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = Notifications.canPost(context)
    }
    return NotificationPermissionState(granted) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
