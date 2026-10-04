package app.sprout.habits.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.sprout.habits.R

/** Everything bundled into the app, with its licence. Keep in step with the version catalog and res/font. */
private val LICENCES = listOf(
    "AndroidX (Compose, Material 3, Room, DataStore, Navigation, Lifecycle, Activity, Core, Glance)" to "Apache License 2.0",
    "Kotlin standard library and kotlinx.coroutines" to "Apache License 2.0",
    "kotlinx.serialization" to "Apache License 2.0",
    "Material Symbols (habit icons)" to "Apache License 2.0",
    "Space Grotesk font" to "SIL Open Font License 1.1",
    "Outfit font" to "SIL Open Font License 1.1",
    "Lexend font" to "SIL Open Font License 1.1",
    "Atkinson Hyperlegible Next font" to "SIL Open Font License 1.1",
)

@Composable
fun LicencesScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        TopBar(stringResource(R.string.licences), onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LICENCES, key = { it.first }) { (name, licence) ->
                SettingsCard { SettingsRow(name, licence) }
            }
        }
    }
}
