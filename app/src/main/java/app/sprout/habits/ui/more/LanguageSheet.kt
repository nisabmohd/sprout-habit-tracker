package app.sprout.habits.ui.more

import app.sprout.habits.ui.theme.SproutType
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.sprout.habits.AppLanguage
import app.sprout.habits.BuildConfig
import app.sprout.habits.R
import app.sprout.habits.ui.components.ChoiceRow
import app.sprout.habits.ui.components.ChoiceSheet
import app.sprout.habits.ui.openUrl

/**
 * "Language": the phone's language first, then every shipped language in its own script with the
 * English name under it. Picking one applies it at once; the card at the bottom links to
 * translating Sprout.
 */
@Composable
fun LanguageSheet(current: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val system = AppLanguage.systemLocale(context).let { it.getDisplayName(it) }.replaceFirstChar { it.uppercase() }
    ChoiceSheet(
        title = stringResource(R.string.language),
        onDismiss = onDismiss,
        footer = { HelpTranslateCard { openUrl(context, "${BuildConfig.REPO_URL}/blob/main/CONTRIBUTING.md#translations") } },
    ) {
        ChoiceRow(stringResource(R.string.language_system), system, current == null) { onPick(null) }
        AppLanguage.LANGUAGES.forEach { lang ->
            // English needs no second line; the others show their English name under them.
            ChoiceRow(lang.nativeName, lang.englishName.takeIf { it != lang.nativeName }, current == lang.tag) { onPick(lang.tag) }
        }
    }
}

@Composable
private fun HelpTranslateCard(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp)
            .navigationBarsPadding()
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_translate), contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.help_translate), style = SproutType.cardTitle, color = colors.onSurface)
            Text(stringResource(R.string.help_translate_desc), style = SproutType.supporting, color = colors.onSurfaceVariant)
        }
        Icon(painterResource(R.drawable.ic_open_external), contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(18.dp))
    }
}
