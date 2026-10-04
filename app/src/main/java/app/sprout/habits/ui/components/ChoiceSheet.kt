package app.sprout.habits.ui.components

import app.sprout.habits.ui.theme.SproutType
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * A bottom sheet for picking one option (Language, Week starts on): the title, then full-width
 * [ChoiceRow]s that scroll, then an optional [footer] that stays below them.
 */
@Composable
fun ChoiceSheet(
    title: String,
    onDismiss: () -> Unit,
    footer: (@Composable () -> Unit)? = null,
    rows: @Composable ColumnScope.() -> Unit,
) {
    SproutSheet(onDismissRequest = onDismiss) {
        Text(
            title,
            style = SproutType.title,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
        )
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).selectableGroup(), content = rows)
        if (footer != null) footer() else Column(Modifier.padding(bottom = 20.dp).navigationBarsPadding()) {}
    }
}

/** One option: title with an optional line under it, the radio on the right; the chosen one is tinted. */
@Composable
fun ChoiceRow(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(if (selected) colors.surfaceContainerHigh else Color.Transparent)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = SproutType.cardTitle, color = colors.onSurface)
            subtitle?.let { Text(it, style = SproutType.supporting, color = colors.onSurfaceVariant) }
        }
        RadioButton(selected = selected, onClick = null)
    }
}
