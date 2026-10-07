package app.sprout.habits.ui.components

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.sprout.habits.R

@Immutable
data class Swatch(val color: Color, val onColor: Color, val label: String)

/**
 * A row of color circles that share the width evenly, so they fit narrow screens. The selected
 * one gets a check and a thin ring in its own color, set slightly apart. Used for habit colors
 * and the app accent so selection looks the same everywhere.
 */
@Composable
fun ColorSwatchRow(swatches: List<Swatch>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        swatches.forEachIndexed { i, swatch ->
            val isSelected = i == selected
            // Each cell is at least a 48 dp touch target; the circle stays smaller inside it.
            Box(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(CircleShape)
                    .selectable(isSelected, role = Role.RadioButton) { if (!isSelected) haptics.tick(); onSelect(i) }
                    .semantics { contentDescription = swatch.label },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .then(if (isSelected) Modifier.border(2.dp, swatch.color, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(if (isSelected) 28.dp else 32.dp).background(swatch.color, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = swatch.onColor, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
