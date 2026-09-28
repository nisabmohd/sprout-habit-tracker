package app.sprout.habits.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sprout.habits.R
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.ui.theme.habitColors

/** First launch only. The play flavor will add "Continue with Google" here once Drive backup exists. */
@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Column(
        Modifier.fillMaxSize().background(colors.background).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Box(Modifier.size(72.dp).background(colors.primaryContainer, RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_seedling), contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.app_name), style = type.titleSmall, color = colors.primary)
        Text(stringResource(R.string.welcome_title), style = type.displayLarge, color = colors.onBackground)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.welcome_body),
            style = type.bodyLarge,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Feature(HabitIcon.MORNING.drawable, 38f, stringResource(R.string.welcome_feature_log))
            Feature(HabitIcon.WRITE.drawable, 330f, stringResource(R.string.welcome_feature_journal))
            Feature(HabitIcon.OUTDOORS.drawable, 150f, stringResource(R.string.welcome_feature_widget))
        }
        Spacer(Modifier.weight(1f).height(32.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(R.string.welcome_start), style = type.titleMedium)
        }
        Text(
            stringResource(R.string.welcome_footer),
            style = type.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
    }
}

@Composable
private fun Feature(@DrawableRes icon: Int, hue: Float, text: String) {
    val hc = habitColors(hue)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(hc.soft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = hc.ink, modifier = Modifier.size(20.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 16.dp))
    }
}
