package app.sprout.habits

import android.content.Context
import android.app.Application
import app.sprout.habits.data.SampleData
import app.sprout.habits.notify.Notifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SproutApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLanguage.wrap(base))
    }

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        container.appScope.launch(Dispatchers.IO) {
            // Sample data first: it only goes in while the first-open time isn't set yet.
            SampleData.seedOnFirstLaunch(container.repository, container.settings, container.strings)
            container.settings.markFirstOpen()
        }
        container.reminders.start(container.appScope)
        container.widgets.start(container.appScope)
    }
}
