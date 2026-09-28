package app.sprout.habits.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text

/**
 * The header of every tab (Today, Habits, Journal, Insights, More), so the title sits at the same
 * place on each: the 28sp title centred on the 48 dp round [actions] ([HeaderIconButton]), and an
 * optional [subtitle] (date, date range) on the line below it.
 *
 * Tabs space their lists differently, so [listSpacing] is the list's own gap; the header adds the
 * rest so there are always 16 dp between the header and the first card.
 */
@Composable
fun TabHeader(
    title: String,
    subtitle: String? = null,
    listSpacing: Dp = 0.dp,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, bottom = (16.dp - listSpacing).coerceAtLeast(0.dp)),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Box(Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
                Text(title, style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
            }
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}
