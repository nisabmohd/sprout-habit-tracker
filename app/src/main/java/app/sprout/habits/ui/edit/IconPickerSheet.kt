package app.sprout.habits.ui.edit

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.sprout.habits.ui.components.SproutSheet
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.sprout.habits.R
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.data.IconGroup
import app.sprout.habits.ui.theme.HabitColors

/**
 * Choose icon: every habit icon in a 6-column grid under group headers, with a search that
 * filters by label. The choice only applies on "Use this icon".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerSheet(
    current: HabitIcon,
    colors: HabitColors,
    onPick: (HabitIcon) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    var picked by rememberSaveable { mutableStateOf(current) }
    var query by rememberSaveable { mutableStateOf("") }
    // Matches the label in the app's language or the English symbol name.
    val res = LocalContext.current.resources
    val matches = HabitIcon.entries.filter { query.isBlank() || res.getString(it.label).contains(query.trim(), ignoreCase = true) || it.key.replace('_', ' ').contains(query.trim(), ignoreCase = true) }

    SproutSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().imePadding()) {
            Text(stringResource(R.string.choose_icon), style = type.titleLarge, color = scheme.onSurface, modifier = Modifier.padding(bottom = 16.dp))
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_icons), style = type.bodyLarge) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null, modifier = Modifier.size(20.dp)) },
                singleLine = true,
                textStyle = type.bodyLarge,
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = scheme.surfaceContainerHigh,
                    unfocusedContainerColor = scheme.surfaceContainerHigh,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconGroup.entries.forEach { group ->
                    val icons = matches.filter { it.group == group }
                    if (icons.isEmpty()) return@forEach
                    item(key = group.name, span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            stringResource(group.label),
                            style = type.labelMedium,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                        )
                    }
                    items(icons, key = { it.key }) { icon ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            IconTile(icon, selected = icon == picked, colors = colors) { picked = icon }
                        }
                    }
                }
                if (matches.isEmpty()) {
                    item(key = "none", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            stringResource(R.string.no_icons_match, query.trim()),
                            style = type.bodyMedium,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                }
            }
            Button(
                onClick = { onPick(picked) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(48.dp),
            ) {
                Text(stringResource(R.string.use_this_icon), style = type.labelLarge)
            }
        }
    }
}

/**
 * A rounded-square icon button, 48 dp unless [modifier] sizes it; the selected one takes the
 * habit's soft colour and border.
 */
@Composable
fun IconTile(icon: HabitIcon, selected: Boolean, colors: HabitColors, modifier: Modifier = Modifier.size(48.dp), onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    val label = stringResource(icon.label)
    Box(
        modifier
            .clip(shape)
            .background(if (selected) colors.soft else scheme.surfaceContainerLowest)
            .then(if (selected) Modifier.border(2.dp, colors.solid, shape) else Modifier)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label; this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon.drawable),
            contentDescription = null,
            tint = if (selected) colors.ink else scheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}
