package app.sprout.habits.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sprout.habits.R
import app.sprout.habits.ui.theme.habitColors

@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onAddNote: (() -> Unit)?,
    onOpenNote: ((Long) -> Unit)?,
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "title") {
                Text("Journal", style = type.headlineMedium, color = colors.onBackground, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 14.dp))
            }
            val list = notes
            if (list != null && list.isEmpty()) {
                item(key = "empty") {
                    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("No notes yet", style = type.titleMedium, color = colors.onSurface)
                        Text("Add a note to remember how a day went.", style = type.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
            }
            items(list.orEmpty(), key = { it.id }) { note -> JournalCard(note, onOpenNote) }
        }
        if (onAddNote != null) {
            FloatingActionButton(
                onClick = onAddNote,
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Icon(painterResource(R.drawable.ic_habit_pen), contentDescription = "Add note", modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun JournalCard(note: JournalNoteUi, onOpen: ((Long) -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val hc = habitColors(note.hue)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceContainerLowest)
            .then(if (onOpen != null) Modifier.clickable(onClickLabel = "Edit note") { onOpen(note.id) } else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(24.dp).background(hc.soft, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(note.icon), contentDescription = null, tint = hc.ink, modifier = Modifier.size(14.dp))
            }
            Text(
                note.habitName,
                style = type.titleSmall,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 10.dp),
            )
            Text(note.dateLabel, style = type.bodyMedium, color = colors.onSurfaceVariant)
        }
        Text(note.text, style = type.bodyLarge, color = colors.onSurface)
    }
}
