package app.sprout.habits

import android.app.Application
import app.sprout.habits.notify.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SproutApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { DevData.seedIfEmpty(container.repository) }
    }
}
