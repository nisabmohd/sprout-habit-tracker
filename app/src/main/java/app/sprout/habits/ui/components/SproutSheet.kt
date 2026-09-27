package app.sprout.habits.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/** The share of the screen a sheet may take; the rest stays visible above it. */
private const val MAX_SHEET_FRACTION = 0.9f

/** The sheet's drag handle area, which sits above the content. */
private val DRAG_HANDLE = 48.dp

/**
 * Every bottom sheet in the app: opens fully, sits on the bottom edge, and leaves the top 10% of
 * the screen visible however much it holds (just the status bar while the keyboard is up). Long
 * content scrolls inside it.
 *
 * The limit is on the content, not the sheet: a height limit on the sheet itself lifts it off
 * the bottom edge. It uses the room the sheet really has, because before Android 9 the sheet's
 * window shrinks to fit above the keyboard.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SproutSheet(onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val density = LocalDensity.current
    val screen = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    val navBar = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    val statusBar = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    // While typing (e.g. searching icons) the keyboard takes half the screen, so the sheet may
    // grow to just under the status bar; otherwise it leaves the top 10% free.
    val topGap = if (WindowInsets.isImeVisible) statusBar + 8.dp else screen * (1 - MAX_SHEET_FRACTION)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        BoxWithConstraints {
            val byScreen = screen - topGap - DRAG_HANDLE - navBar
            val max = if (constraints.hasBoundedHeight) minOf(byScreen, maxHeight - topGap - DRAG_HANDLE) else byScreen
            // The sheet takes focus itself, so opening one never pops up the keyboard: before
            // Android 9 the first text field in it would get focus on its own.
            Column(Modifier.heightIn(max = max).focusable(), content = content)
        }
    }
}
