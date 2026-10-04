package app.sprout.habits.ui.update

import app.sprout.habits.ui.theme.SproutType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.sprout.habits.BuildConfig
import app.sprout.habits.R
import app.sprout.habits.data.update.AppUpdate
import app.sprout.habits.data.update.DownloadState
import app.sprout.habits.data.update.UpdateCheck
import app.sprout.habits.data.update.UpdateManager
import app.sprout.habits.support.LaunchPrompts
import app.sprout.habits.ui.openUrl
import kotlinx.coroutines.launch

/** The version without the build suffix: "1.0.0" rather than "1.0.0-github". */
private val currentVersion get() = BuildConfig.VERSION_NAME.substringBefore('-')

/**
 * "Update available": the new version, up to five What's new lines, then Update (download with
 * progress, then Android's installer) or Later.
 */
@Composable
fun UpdateDialog(update: AppUpdate, updates: UpdateManager, onLater: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    var state by remember { mutableStateOf<DownloadState?>(null) }
    // Checked again on return from "Install unknown apps", so the hint goes once it's allowed.
    var canInstall by remember { mutableStateOf(updates.canInstall()) }
    LifecycleResumeEffect(Unit) {
        canInstall = updates.canInstall()
        onPauseOrDispose {}
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = colors.surfaceContainerLowest) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(64.dp).background(colors.primaryContainer, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_seedling), contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.update_available), style = SproutType.title, color = colors.onSurface, textAlign = TextAlign.Center)
                Text(
                    stringResource(R.string.update_version_line, update.version, currentVersion),
                    style = SproutType.supporting,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (update.notes.isNotEmpty()) {
                    Column(
                        Modifier.padding(top = 20.dp).fillMaxWidth().background(colors.surfaceContainerHigh, RoundedCornerShape(16.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.whats_new), style = SproutType.label, color = colors.onSurface)
                        update.notes.forEach { line ->
                            Row(verticalAlignment = Alignment.Top) {
                                Box(Modifier.padding(top = 8.dp).size(6.dp).background(colors.primary, CircleShape))
                                Text(line, style = SproutType.supporting, color = colors.onSurface, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 10.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                when (val s = state) {
                    is DownloadState.Progress -> {
                        Column(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val fraction = s.fraction
                            if (fraction == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                            else LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                            Text(
                                if (fraction == null) stringResource(R.string.downloading) else stringResource(R.string.downloading_percent, (fraction * 100).toInt()),
                                style = SproutType.supporting,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                    is DownloadState.Ready -> {
                        if (!canInstall) {
                            Text(stringResource(R.string.allow_installs_hint), style = SproutType.supporting, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(12.dp))
                        }
                        PrimaryButton(stringResource(R.string.install)) {
                            if (updates.canInstall()) updates.install(s.apk) else updates.openInstallPermission()
                        }
                    }
                    DownloadState.Failed -> {
                        Text(stringResource(R.string.update_failed), style = SproutType.supporting, color = colors.error, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        PrimaryButton(stringResource(R.string.open_release_page)) { openUrl(context, update.pageUrl); onDismiss() }
                    }
                    null -> PrimaryButton(stringResource(R.string.update)) {
                        if (update.apkUrl == null) {
                            openUrl(context, update.pageUrl)
                            onDismiss()
                        } else {
                            scope.launch { updates.download(update).collect { state = it } }
                        }
                    }
                }
                TextButton(onClick = onLater, modifier = Modifier.padding(top = 4.dp).heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.later), style = SproutType.label)
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text(label, style = SproutType.label)
    }
}

private enum class CheckState { IDLE, CHECKING, LATEST, ERROR }

/**
 * About's "Check for updates" pill: Checking… → "You're on the latest version" or the Update
 * dialog. The F-Droid build says where its updates come from instead. The Play build's pill opens
 * the store listing, and shows nothing until Sprout is listed there.
 */
@Composable
fun CheckForUpdates(updates: UpdateManager) {
    val colors = MaterialTheme.colorScheme
    if (!updates.enabled) {
        when {
            BuildConfig.PLAY_LISTED -> PlayUpdatesPill()
            BuildConfig.PLAY_STORE -> Unit
            else -> Text(stringResource(R.string.updates_from_fdroid), style = SproutType.supporting, color = colors.onSurfaceVariant)
        }
        return
    }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(CheckState.IDLE) }
    var found by remember { mutableStateOf<AppUpdate?>(null) }
    val latest = state == CheckState.LATEST
    Button(
        onClick = {
            if (state == CheckState.LATEST) { state = CheckState.IDLE; return@Button }
            state = CheckState.CHECKING
            scope.launch {
                state = when (val r = updates.check()) {
                    is UpdateCheck.Available -> { found = r.update; CheckState.IDLE }
                    UpdateCheck.UpToDate -> CheckState.LATEST
                    UpdateCheck.Failed -> CheckState.ERROR
                }
            }
        },
        enabled = state != CheckState.CHECKING,
        modifier = Modifier.heightIn(min = 40.dp).semantics { liveRegion = LiveRegionMode.Polite },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (latest) colors.primaryContainer else colors.surfaceContainerHigh,
            contentColor = when {
                latest -> colors.onPrimaryContainer
                state == CheckState.ERROR -> colors.error
                else -> colors.onSurface
            },
            disabledContainerColor = colors.surfaceContainerHigh,
            disabledContentColor = colors.onSurface,
        ),
    ) {
        when (state) {
            CheckState.CHECKING -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.onSurface)
            CheckState.LATEST -> Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(18.dp))
            else -> Icon(painterResource(R.drawable.ic_refresh), contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.size(8.dp))
        Text(
            stringResource(
                when (state) {
                    CheckState.IDLE -> R.string.check_for_updates
                    CheckState.CHECKING -> R.string.checking_updates
                    CheckState.LATEST -> R.string.up_to_date
                    CheckState.ERROR -> R.string.update_check_failed
                },
            ),
            style = SproutType.label,
        )
    }
    found?.let { update ->
        UpdateDialog(
            update = update,
            updates = updates,
            onLater = { found = null; scope.launch { updates.later(update) } },
            onDismiss = { found = null },
        )
    }
}

/** The Play build's "Check for updates": Google Play's own page for Sprout, which offers the update. */
@Composable
private fun PlayUpdatesPill() {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    Button(
        onClick = { openUrl(context, BuildConfig.PLAY_STORE_URL) },
        modifier = Modifier.heightIn(min = 40.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceContainerHigh, contentColor = colors.onSurface),
    ) {
        Icon(painterResource(R.drawable.ic_refresh), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(stringResource(R.string.check_for_updates), style = SproutType.label)
    }
}

/** The automatic check on app start (github build): at most once a day, and only if no other prompt showed. */
@Composable
fun UpdatePrompt(updates: UpdateManager) {
    if (!updates.enabled) return
    val scope = rememberCoroutineScope()
    var found by remember { mutableStateOf<AppUpdate?>(null) }
    LaunchedEffect(Unit) {
        val update = updates.checkIfDue() ?: return@LaunchedEffect
        if (LaunchPrompts.claim()) found = update
    }
    found?.let { update ->
        UpdateDialog(
            update = update,
            updates = updates,
            onLater = { found = null; scope.launch { updates.later(update) } },
            onDismiss = { found = null },
        )
    }
}

/** A small "Update available" pill, for More → About Sprout. */
@Composable
fun UpdateBadge() {
    Text(
        stringResource(R.string.update_available),
        style = SproutType.captionStrong,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(11.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
