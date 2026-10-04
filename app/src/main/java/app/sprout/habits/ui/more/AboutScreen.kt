package app.sprout.habits.ui.more

import app.sprout.habits.ui.theme.SproutType
import androidx.compose.foundation.layout.Spacer
import app.sprout.habits.ui.update.CheckForUpdates
import app.sprout.habits.data.update.UpdateManager
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sprout.habits.BuildConfig
import app.sprout.habits.R
import app.sprout.habits.ui.components.HeaderIconButton
import app.sprout.habits.ui.openUrl
import app.sprout.habits.ui.theme.habitColors

@Composable
fun AboutScreen(updates: UpdateManager, onBack: () -> Unit, onOpenLicences: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding()) {
        TopBar(stringResource(R.string.about), onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.padding(top = 16.dp).size(80.dp).background(colors.primaryContainer, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_seedling), contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(40.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.app_name), style = SproutType.screenTitle, color = colors.onBackground)
                Text(stringResource(R.string.version_build, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE), style = SproutType.supporting, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                CheckForUpdates(updates)
            }
            Column(
                Modifier.fillMaxWidth().background(colors.primaryContainer, RoundedCornerShape(24.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_code), contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(22.dp))
                    Text(stringResource(R.string.free_open_source), style = SproutType.cardTitle, color = colors.onPrimaryContainer, modifier = Modifier.padding(start = 12.dp))
                }
                Text(
                    stringResource(R.string.about_licence_text),
                    style = SproutType.supporting,
                    color = colors.onPrimaryContainer,
                )
            }
            // Not in the play build: Google Play doesn't allow asking for money outside its own billing.
            if (!BuildConfig.PLAY_STORE) SettingsCard { SupportRow { openUrl(context, BuildConfig.SPONSOR_URL) } }
            SettingsCard {
                val repo = BuildConfig.REPO_URL
                if (BuildConfig.PLAY_LISTED) {
                    LinkRow(stringResource(R.string.rate_on_play), stringResource(R.string.rate_desc)) { openUrl(context, BuildConfig.PLAY_STORE_URL) }
                    CardDivider()
                }
                LinkRow(stringResource(R.string.source_code), repo.removePrefix("https://")) { openUrl(context, repo) }
                CardDivider()
                LinkRow(stringResource(R.string.report_bug), stringResource(R.string.github_issues)) { openUrl(context, "$repo/issues") }
                CardDivider()
                LinkRow(stringResource(R.string.contribute), stringResource(R.string.contribute_desc)) { openUrl(context, "$repo/blob/main/CONTRIBUTING.md") }
                CardDivider()
                SettingsRow(stringResource(R.string.licences), stringResource(R.string.licences_desc), onClick = onOpenLicences) {
                    Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
            Text(stringResource(R.string.made_by), style = SproutType.supporting, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}

/** "Support Sprout" with a pink heart and a small Sponsor pill. */
@Composable
private fun SupportRow(onClick: () -> Unit) {
    val pink = habitColors(330f)
    SettingsRow(stringResource(R.string.support_sprout), stringResource(R.string.support_desc), onClick = onClick) {
        Text(
            stringResource(R.string.sponsor),
            style = SproutType.label,
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
    Row(Modifier.fillMaxWidth().height(64.dp).padding(start = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        HeaderIconButton(R.drawable.ic_arrow_back, stringResource(R.string.action_back), onClick = onBack)
        Text(title, style = SproutType.title, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 14.dp))
    }
}
