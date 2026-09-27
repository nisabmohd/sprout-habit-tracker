package app.sprout.habits.ui.today

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalScrollCaptureInProgress
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sprout.habits.BuildConfig
import app.sprout.habits.R
import app.sprout.habits.support.SupportPromptDialog
import app.sprout.habits.ui.openUrl
import app.sprout.habits.domain.DayOutcome
import app.sprout.habits.ui.components.CappedFontScale
import app.sprout.habits.ui.components.ProgressRing
import app.sprout.habits.ui.theme.habitColors
import java.time.LocalDate
import kotlinx.coroutines.flow.collectLatest
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onAddHabit: () -> Unit,
    onOpenHabit: (Long) -> Unit,
    /** Habit to preselect (0 = none) and the epoch day shown. */
    onAddNote: (Long, Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshToday()
        viewModel.maybeShowSupportPrompt()
    }
    val showSupport by viewModel.supportPrompt.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    if (showSupport) {
        SupportPromptDialog(
            showRate = BuildConfig.PLAY_STORE,
            onRate = { openUrl(context, BuildConfig.PLAY_STORE_URL); viewModel.supportPromptClosed(dismissed = false) },
            onStar = { openUrl(context, BuildConfig.REPO_URL); viewModel.supportPromptClosed(dismissed = false) },
            onSponsor = { openUrl(context, BuildConfig.SPONSOR_URL); viewModel.supportPromptClosed(dismissed = false) },
            onLater = { viewModel.supportPromptClosed(dismissed = true) },
        )
    }
    LaunchedEffect(viewModel) {
        viewModel.changes.collectLatest { change ->
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(change.message, actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) viewModel.undo(change)
        }
    }
    Box(Modifier.fillMaxSize()) {
        TodayContent(
            state,
            onSelectDay = viewModel::select,
            onAddHabit = onAddHabit,
            onDone = viewModel::markDone,
            onSkip = viewModel::markSkipped,
            onUndo = viewModel::resetDay,
            onToggle = viewModel::toggleDone,
            onLongPress = viewModel::openLogSheet,
            onOpen = onOpenHabit,
        )
        // Lift the button above the Undo snackbar while it shows, so the two never overlap.
        val fabLift by animateDpAsState(if (snackbar.currentSnackbarData != null) 72.dp else 0.dp, label = "fabLift")
        // Hidden during a long screenshot, or it would be stamped into every captured frame.
        if (state.habits.isNotEmpty() && !LocalScrollCaptureInProgress.current) {
            ExtendedFloatingActionButton(
                // Preselect a skipped habit that has no note yet, if there is one.
                onClick = {
                    val suggested = state.habits.firstOrNull { it.outcome == DayOutcome.SKIP && !it.hasNote } ?: state.habits.first()
                    onAddNote(suggested.id, state.selectedDate.toEpochDay())
                },
                icon = { Icon(painterResource(R.drawable.ic_habit_pen), contentDescription = null, modifier = Modifier.size(22.dp)) },
                text = { Text("Add note", style = MaterialTheme.typography.labelLarge) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = fabLift),
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(horizontal = 8.dp)) { data ->
            // Dark inverse surface with 14 dp corners, as in the design.
            Snackbar(
                data,
                shape = RoundedCornerShape(14.dp),
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                actionColor = MaterialTheme.colorScheme.inversePrimary,
            )
        }
    }
    val logSheet by viewModel.logSheet.collectAsStateWithLifecycle()
    logSheet?.let { sheet ->
        LogSheet(
            sheet,
            onSave = { status, amount, note -> viewModel.saveLog(sheet, status, amount, note) },
            onUndo = { viewModel.clearFromSheet(sheet) },
            onDismiss = viewModel::dismissLogSheet,
        )
    }
}

@Composable
private fun TodayContent(
    state: TodayUiState,
    onSelectDay: (LocalDate) -> Unit,
    onAddHabit: () -> Unit,
    onDone: (Long) -> Unit,
    onSkip: (Long) -> Unit,
    onUndo: (Long) -> Unit,
    onToggle: (Long) -> Unit,
    onLongPress: (Long) -> Unit,
    onOpen: (Long) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") { Header(state, onAddHabit) }
        item(key = "score") { ScoreCard(state) }
        if (!state.loading && state.habits.isEmpty()) {
            item(key = "empty") { EmptyState() }
        }
        items(state.habits, key = { it.id }) { habit ->
            SwipeableHabitCard(
                habit,
                onDone = { onDone(habit.id) },
                onSkip = { onSkip(habit.id) },
                onUndo = { onUndo(habit.id) },
                onToggle = { onToggle(habit.id) },
                onLongPress = { onLongPress(habit.id) },
                onClick = { onOpen(habit.id) },
            )
        }
    }
}

@Composable
private fun Header(state: TodayUiState, onAddHabit: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp).padding(bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(state.dateLabel, style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant)
            Text(
                if (state.isToday) "Today" else state.selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onBackground,
            )
        }
        IconButton(
            onClick = onAddHabit,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.iconButtonColors(containerColor = colors.surfaceContainerHigh, contentColor = colors.onSurface),
        ) {
            Icon(painterResource(R.drawable.ic_plus), contentDescription = "New habit", modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ScoreCard(state: TodayUiState) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .background(colors.surfaceContainerLowest, RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${state.scorePercent}%", style = type.headlineMedium, color = colors.onSurface)
            Spacer(Modifier.width(8.dp))
            Text(
                if (state.isToday) "of today" else "of the day",
                style = type.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Spacer(Modifier.width(12.dp))
            // Takes the rest of the row and wraps at large text sizes.
            Text(
                summary(state),
                style = type.titleSmall,
                color = colors.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier.weight(1f).padding(bottom = 4.dp),
            )
        }
        Segments(state.habits, Modifier.semantics { contentDescription = summary(state) })
    }
}

/** "1 done · 2 partial · 3 left"; each count stays on one line with its word when text wraps. */
private fun summary(state: TodayUiState): String = buildList {
    add("${state.doneCount}\u00A0done")
    if (state.partialCount > 0) add("${state.partialCount}\u00A0partial")
    if (state.openCount > 0) add("${state.openCount}\u00A0left")
}.joinToString(" · ")

/** One bar per habit: solid when done, part-filled when partial, faint when skipped. */
@Composable
private fun Segments(habits: List<HabitRowUi>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val line = colors.outlineVariant
    val skip = colors.surfaceContainerHigh
    val solids = habits.map { habitColors(it.hue).solid }
    Canvas(modifier.fillMaxWidth().height(8.dp)) {
        if (habits.isEmpty()) {
            drawRoundRect(line, cornerRadius = CornerRadius(size.height / 2))
            return@Canvas
        }
        val gap = 4.dp.toPx()
        val w = (size.width - gap * (habits.size - 1)) / habits.size
        val r = CornerRadius(size.height / 2)
        habits.forEachIndexed { i, h ->
            val x = i * (w + gap)
            val base = if (h.outcome == DayOutcome.SKIP) skip else line
            drawRoundRect(base, Offset(x, 0f), Size(w, size.height), r)
            val fill = when (h.outcome) {
                DayOutcome.DONE -> 1f
                DayOutcome.PARTIAL -> h.progress
                else -> 0f
            }
            if (fill > 0f) {
                clipRect(left = x, right = x + w * fill) { drawRoundRect(solids[i], Offset(x, 0f), Size(w, size.height), r) }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Nothing scheduled for this day", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text("Tap + to add a habit.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
