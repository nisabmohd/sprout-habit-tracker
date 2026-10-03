package app.sprout.habits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.sprout.habits.R
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
 * [onClearSubtitle] puts a small ✕ after the subtitle while a date filter is on.
 *
 * Tabs space their lists differently, so [listSpacing] is the list's own gap; the header adds the
 * rest so there are always 16 dp between the header and the first card.
 */
@Composable
fun TabHeader(
    title: String,
    subtitle: String? = null,
    listSpacing: Dp = 0.dp,
    clearLabel: String? = null,
    onClearSubtitle: (() -> Unit)? = null,
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
            if (subtitle != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(subtitle, style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant)
                    if (onClearSubtitle != null && clearLabel != null) {
                        SmallClearButton(clearLabel, 22.dp, colors.surfaceContainerHigh, colors.onSurfaceVariant, onClearSubtitle)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

/**
 * A small round ✕ ([size] wide) whose touch target is still 48 dp: the target overflows the
 * button's own bounds, so the line it sits in keeps its height.
 */
@Composable
fun SmallClearButton(label: String, size: Dp, container: Color, content: Color, onClick: () -> Unit) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier.requiredSize(48.dp).clip(CircleShape).clickable(onClickLabel = label, role = Role.Button, onClick = onClick).semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(size).background(container, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
            }
        }
    }
}
