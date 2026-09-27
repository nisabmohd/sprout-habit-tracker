package app.sprout.habits

import android.app.Application
import app.sprout.habits.notify.Notifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SproutApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        container.appScope.launch(Dispatchers.IO) { DevData.seedIfEmpty(container.repository) }
        container.appScope.launch { container.settings.markFirstOpen() }
        container.reminders.start(container.appScope)
        container.widgets.start(container.appScope)
    }
}
