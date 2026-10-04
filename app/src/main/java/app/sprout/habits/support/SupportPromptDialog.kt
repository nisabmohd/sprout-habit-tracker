package app.sprout.habits.support

import app.sprout.habits.ui.theme.SproutType
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.sprout.habits.R
import app.sprout.habits.ui.theme.habitColors

/**
 * "Enjoying Sprout?": rate (play build, once listed), star on GitHub, sponsor (not in the play
 * build: Google Play doesn't allow asking for money outside its own billing), or maybe later.
 */
@Composable
fun SupportPromptDialog(
    showRate: Boolean,
    showSponsor: Boolean,
    onRate: () -> Unit,
    onStar: () -> Unit,
    onSponsor: () -> Unit,
    onLater: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val pink = habitColors(330f)
    Dialog(onDismissRequest = onLater) {
        Surface(shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerLowest) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(64.dp).background(colors.primaryContainer, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_seedling), contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(32.dp))
                }
                Text(stringResource(R.string.support_title), style = SproutType.title, color = colors.onSurface)
                Text(
                    stringResource(
                        when {
                            showRate -> R.string.support_text_play
                            showSponsor -> R.string.support_text
                            else -> R.string.support_text_star
                        },
                    ),
                    style = SproutType.supporting,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                if (showRate) {
                    Button(onClick = onRate, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Icon(painterResource(R.drawable.ic_thumb_up), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.rate_on_play), style = SproutType.label)
                    }
                }
                FilledTonalButton(onClick = onStar, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(painterResource(R.drawable.ic_star), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.star_github), style = SproutType.label)
                }
                if (showSponsor) {
                    FilledTonalButton(
                        onClick = onSponsor,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = pink.soft, contentColor = pink.ink),
                    ) {
                        Icon(painterResource(R.drawable.ic_heart), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.sponsor), style = SproutType.label)
                    }
                }
                TextButton(onClick = onLater) { Text(stringResource(R.string.maybe_later), style = SproutType.label) }
            }
        }
    }
}
