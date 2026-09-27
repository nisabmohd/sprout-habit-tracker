package app.sprout.habits.ui.more

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sprout.habits.BuildConfig
import app.sprout.habits.R
import app.sprout.habits.ui.openUrl
import app.sprout.habits.ui.theme.habitColors

@Composable
fun AboutScreen(onBack: () -> Unit, onOpenLicences: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
        TopBar("About", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.padding(top = 16.dp).size(80.dp).background(colors.primaryContainer, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_seedling), contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(40.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Sprout", style = type.headlineMedium, color = colors.onBackground)
                Text("Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})", style = type.bodyMedium, color = colors.onSurfaceVariant)
            }
            Column(
                Modifier.fillMaxWidth().background(colors.primaryContainer, RoundedCornerShape(24.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_code), contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(22.dp))
                    Text("Free and open source", style = type.titleMedium, color = colors.onPrimaryContainer, modifier = Modifier.padding(start = 12.dp))
                }
                Text(
                    "Licensed under the GNU GPL v3. Sprout has no ads and doesn't track you. Your habits and notes stay on this device unless you export them.",
                    style = type.bodyMedium,
                    color = colors.onPrimaryContainer,
                )
            }
            SettingsCard { SupportRow { openUrl(context, BuildConfig.SPONSOR_URL) } }
            SettingsCard {
                val repo = BuildConfig.REPO_URL
                if (BuildConfig.PLAY_STORE) {
                    LinkRow("Rate on Play Store", "Takes 10 seconds, helps a lot") { openUrl(context, BuildConfig.PLAY_STORE_URL) }
                    CardDivider()
                }
                LinkRow("Source code", repo.removePrefix("https://")) { openUrl(context, repo) }
                CardDivider()
                LinkRow("Report a bug or request a feature", "GitHub Issues") { openUrl(context, "$repo/issues") }
                CardDivider()
                LinkRow("Contribute", "Code, design and translations welcome") { openUrl(context, "$repo/blob/main/CONTRIBUTING.md") }
                CardDivider()
                SettingsRow("Open-source licences", "Libraries and fonts this app uses", onClick = onOpenLicences) {
                    Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
            Text("Made by the Sprout contributors", style = type.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}

/** "Support Sprout" with a pink heart and a small Sponsor pill. */
@Composable
private fun SupportRow(onClick: () -> Unit) {
    val pink = habitColors(330f)
    SettingsRow("Support Sprout", "Free forever, kept alive by sponsors", onClick = onClick) {
        Text(
            "Sponsor",
            style = MaterialTheme.typography.labelLarge,
            color = pink.ink,
            modifier = Modifier.background(pink.soft, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun LinkRow(title: String, subtitle: String, onClick: () -> Unit) =
    SettingsRow(title, subtitle, onClick = onClick) {
        Icon(painterResource(R.drawable.ic_open_external), contentDescription = null, modifier = Modifier.size(20.dp))
    }

@Composable
fun TopBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(64.dp).padding(start = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back", modifier = Modifier.size(22.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 8.dp))
    }
}
