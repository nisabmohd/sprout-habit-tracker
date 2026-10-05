package app.sprout.habits.ui.today

import app.sprout.habits.ui.theme.SproutType
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalScrollCaptureInProgress
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sprout.habits.BuildConfig
import androidx.compose.runtime.getValue
import app.sprout.habits.R
import app.sprout.habits.ui.components.BarSegment
import app.sprout.habits.ui.components.SegmentBar
import app.sprout.habits.ui.components.HeaderIconButton
import app.sprout.habits.ui.components.TabHeader
import app.sprout.habits.support.SupportPromptDialog
import app.sprout.habits.ui.openUrl
import app.sprout.habits.domain.DayOutcome
import app.sprout.habits.ui.theme.habitColors
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onAddHabit: () -> Unit,
    onOpenHabit: (Long) -> Unit,
    /** Habit to preselect (0 = none) and the epoch day shown. */
    onAddNote: (Long, Long) -> Unit,
    /** True while the sample habits a fresh install starts with are still there. */
    hasSampleData: Boolean = false,
    onRemoveSampleData: () -> Unit = {},
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
            showRate = BuildConfig.PLAY_LISTED,
            showSponsor = !BuildConfig.PLAY_STORE,
            onRate = { openUrl(context, BuildConfig.PLAY_STORE_URL); viewModel.supportPromptClosed(dismissed = false) },
            onStar = { openUrl(context, BuildConfig.REPO_URL); viewModel.supportPromptClosed(dismissed = false) },
            onSponsor = { openUrl(context, BuildConfig.SPONSOR_URL); viewModel.supportPromptClosed(dismissed = false) },
            onLater = { viewModel.supportPromptClosed(dismissed = true) },
        )
    }
    DayLogHost(viewModel.logger, snackbar)
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
            sampleCard = if (hasSampleData) {
                { SampleDataCard(onRemove = onRemoveSampleData) }
            } else {
                null
            },
        )
        val addNote = stringResource(R.string.add_note)
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
                text = { Text(stringResource(R.string.add_note), style = SproutType.label) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = fabLift).semantics { contentDescription = addNote },
            )
        }
        UndoSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(horizontal = 8.dp))
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
    sampleCard: (@Composable () -> Unit)?,
) {
    // The header stays put while the list scrolls under it, so its buttons are always in reach.
    Column(Modifier.fillMaxSize()) {
    Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)) {
 Header(state, onAddHabit) 
    }
    LazyColumn(
        Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Nothing until the first data is in, or the app opens on a "0% of today" card that
        // is replaced a moment later.
        if (state.loading) return@LazyColumn
        if (sampleCard != null) item(key = "sample") { sampleCard() }
        item(key = "score") { ScoreCard(state) }
        if (state.habits.isEmpty()) {
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
}

@Composable
private fun Header(state: TodayUiState, onAddHabit: () -> Unit) {
    TabHeader(
        title = if (state.isToday) stringResource(R.string.today) else state.selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
        subtitle = state.dateLabel,
        listSpacing = 8.dp,
    ) {
        HeaderIconButton(R.drawable.ic_plus, stringResource(R.string.new_habit), onClick = onAddHabit)
    }
}

@Composable
private fun ScoreCard(state: TodayUiState) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(colors.surfaceContainerLowest, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.percent, state.scorePercent), style = SproutType.screenTitle, color = colors.onSurface)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(if (state.isToday) R.string.of_today else R.string.of_the_day),
                style = SproutType.supporting,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Spacer(Modifier.width(12.dp))
            // Takes the rest of the row and wraps at large text sizes.
            Text(
                summary(state),
                style = SproutType.label,
                color = colors.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier.weight(1f).padding(bottom = 4.dp),
            )
        }
        val summary = summary(state)
        Segments(state.habits, Modifier.semantics { contentDescription = summary })
    }
}

/** "1 done · 2 partial · 3 left"; each count stays on one line with its word when text wraps. */
@Composable
private fun summary(state: TodayUiState): String = buildList {
    add(pluralStringResource(R.plurals.count_done, state.doneCount, state.doneCount))
    if (state.partialCount > 0) add(pluralStringResource(R.plurals.count_partial, state.partialCount, state.partialCount))
    if (state.openCount > 0) add(pluralStringResource(R.plurals.count_left, state.openCount, state.openCount))
}.joinToString(" · ")

/** One bar per habit: solid when done, part-filled when partial, faint when skipped. */
@Composable
private fun Segments(habits: List<HabitRowUi>, modifier: Modifier = Modifier) {
    SegmentBar(
        habits.map {
            val fill = when (it.outcome) {
                DayOutcome.DONE -> 1f
                DayOutcome.PARTIAL -> it.progress
                else -> 0f
            }
            BarSegment(it.hue, fill, skipped = it.outcome == DayOutcome.SKIP)
        },
        modifier,
    )
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.today_empty), style = SproutType.cardTitle, color = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.today_empty_hint), style = SproutType.supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Shown while the sample habits are there: what they are, and one tap to remove them. */
@Composable
private fun SampleDataCard(onRemove: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.primaryContainer, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(stringResource(R.string.sample_card_title), style = SproutType.label, color = colors.onPrimaryContainer)
            Text(
                stringResource(R.string.sample_card_body),
                style = SproutType.supporting,
                color = colors.onPrimaryContainer,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        // The same round delete button as in the screen headers.
        HeaderIconButton(R.drawable.ic_delete, stringResource(R.string.sample_remove), danger = true, onClick = onRemove)
    }
}
