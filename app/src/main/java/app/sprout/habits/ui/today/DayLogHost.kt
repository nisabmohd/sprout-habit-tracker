package app.sprout.habits.ui.today

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

/**
 * Wires a [DayLogger] into a screen: shows its log sheet when one is open, and an Undo snackbar
 * in [snackbar] for every change.
 */
@Composable
fun DayLogHost(logger: DayLogger, snackbar: SnackbarHostState) {
    LaunchedEffect(logger) {
        logger.changes.collectLatest { change ->
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(change.message, actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) logger.undo(change)
        }
    }
    val sheet by logger.sheet.collectAsStateWithLifecycle()
    sheet?.let { s ->
        LogSheet(
            s,
            onSave = { status, amount, note -> logger.save(s, status, amount, note) },
            onUndo = { logger.clear(s) },
            onDismiss = logger::dismiss,
        )
    }
}

/** The Undo snackbar: dark inverse surface with 14 dp corners, as in the design. */
@Composable
fun UndoSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data ->
        Snackbar(
            data,
            shape = RoundedCornerShape(14.dp),
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            actionColor = MaterialTheme.colorScheme.inversePrimary,
        )
    }
}
