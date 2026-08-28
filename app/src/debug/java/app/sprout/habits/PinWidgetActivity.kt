package app.sprout.habits

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import app.sprout.habits.widget.StreakWidgetReceiver
import app.sprout.habits.widget.StripWidgetReceiver
import app.sprout.habits.widget.TodayWidgetReceiver
import app.sprout.habits.widget.WeekWidgetReceiver

/**
 * Debug builds only: asks the launcher to pin a widget, for testing without dragging from the
 * widget picker. `adb shell am start -n app.sprout.habits/.PinWidgetActivity --es widget today`
 */
class PinWidgetActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val receiver = when (intent.getStringExtra("widget")) {
            "week" -> WeekWidgetReceiver::class.java
            "today" -> TodayWidgetReceiver::class.java
            "strip" -> StripWidgetReceiver::class.java
            "streak" -> StreakWidgetReceiver::class.java
            else -> null
        }
        if (receiver != null) {
            getSystemService(AppWidgetManager::class.java).requestPinAppWidget(ComponentName(this, receiver), null, null)
        }
        finish()
    }
}
