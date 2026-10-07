package app.sprout.habits.ui.more

import app.sprout.habits.ui.components.toggle
import androidx.compose.ui.platform.LocalHapticFeedback
import app.sprout.habits.ui.theme.SproutType
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = SproutType.label,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        // With the list's 8 dp gaps: 16 dp above the label, 8 dp below it.
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
}

/** A rounded group of rows with thin dividers between them. */
@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest),
        content = content,
    )
}

@Composable
fun CardDivider() = HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

/** Title, optional subtitle and a trailing slot; the whole row is the touch target. */
@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    icon: Int? = null,
    onClick: (() -> Unit)? = null,
    /** Shown after the subtitle, e.g. the "Update available" pill. */
    badge: (@Composable () -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp).padding(end = 0.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = SproutType.cardTitle, color = MaterialTheme.colorScheme.onSurface)
            if (badge == null) {
                subtitle?.let { Text(it, style = SproutType.supporting, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    subtitle?.let { Text(it, style = SproutType.supporting, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f, fill = false)) }
                    badge()
                }
            }
        }
        trailing()
    }
}

/** A row that toggles as a whole and reads as a switch to TalkBack. */
@Composable
fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch) { haptics.toggle(it); onChange(it) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = SproutType.cardTitle, color = MaterialTheme.colorScheme.onSurface)
            subtitle?.let { Text(it, style = SproutType.supporting, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
fun TrailingValue(text: String) =
    Text(text, style = SproutType.supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
