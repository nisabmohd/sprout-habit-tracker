package app.sprout.habits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import app.sprout.habits.R
import app.sprout.habits.Strings
import app.sprout.habits.data.Entry
import app.sprout.habits.data.EntryStatus
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.data.Note
import app.sprout.habits.data.TrackType
import app.sprout.habits.domain.formatNumber
import app.sprout.habits.domain.measure
import app.sprout.habits.ui.theme.habitColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** One note as the Journal and a habit's detail show it. */
@Immutable
data class NoteCardUi(
    val id: Long,
    /** Epoch day the note is about. */
    val day: Long,
    val habitName: String,
    val icon: Int,
    val hue: Float,
    /** That day's outcome for the habit, drawn like the Week view marks. */
    val mark: DayMark,
    /** "Skipped", "8 of 20 pages", "Done · 45 min". */
    val outcome: String,
    /** When the note was last written, "9:12 PM". */
    val time: String,
    val text: String,
) {
    companion object {
        // Built on each use so a change of language shows up.
        private val TIME get() = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

        fun of(note: Note, habit: Habit, entry: Entry?, today: LocalDate, strings: Strings): NoteCardUi {
            val (mark, outcome) = dayOutcome(habit, entry, note.date, today.toEpochDay(), strings)
            return NoteCardUi(
                id = note.id,
                day = note.date,
                habitName = habit.name,
                icon = HabitIcon.fromKey(habit.icon).drawable,
                hue = habit.colorHue.toFloat(),
                mark = mark,
                outcome = outcome,
                time = Instant.ofEpochMilli(note.updatedAt).atZone(ZoneId.systemDefault()).toLocalTime().format(TIME),
                text = note.text,
            )
        }

        /** The day's mark and its words. A past day with nothing logged counts as skipped. */
        fun dayOutcome(habit: Habit, entry: Entry?, day: Long, today: Long, strings: Strings): Pair<DayMark, String> = when (entry?.status) {
            EntryStatus.DONE -> DayMark(MarkKind.DONE, 1f) to
                if (habit.trackType == TrackType.CHECK) strings(R.string.outcome_done) else strings(R.string.outcome_done_amount, habit.measure(entry.amount))
            EntryStatus.PARTIAL -> DayMark(MarkKind.PARTIAL, (entry.amount / habit.target).toFloat().coerceIn(0f, 1f)) to
                strings(R.string.amount_of_goal, formatNumber(entry.amount), habit.measure(habit.target))
            EntryStatus.SKIP -> DayMark(MarkKind.SKIP) to strings(R.string.outcome_skipped)
            null -> if (day < today) DayMark(MarkKind.SKIP) to strings(R.string.outcome_skipped) else DayMark(MarkKind.OPEN_TODAY) to strings(R.string.not_logged_yet)
        }
    }
}

/**
 * A note card in the Today card style: habit tile, name, the day's mark and outcome, the time on
 * the right, then the note. Without [showHabit] (on a habit's own page) the first line is the mark
 * and "[outcome] · [dateLabel]".
 */
@Composable
fun NoteCard(note: NoteCardUi, onOpen: ((Long) -> Unit)?, showHabit: Boolean = true, dateLabel: String? = null, onLongPress: ((NoteCardUi) -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val hc = habitColors(note.hue)
    val marks = MarkColors(skip = colors.outlineVariant, skipInk = colors.onSurfaceVariant, outline = colors.outline)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceContainerLowest)
            .then(
                if (onOpen != null || onLongPress != null) {
                    Modifier.combinedClickable(
                        onClickLabel = stringResource(R.string.edit_note),
                        onLongClickLabel = stringResource(R.string.action_more_options),
                        onLongClick = onLongPress?.let { { it(note) } },
                        onClick = { onOpen?.invoke(note.id) },
                    )
                } else {
                    Modifier
                },
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = if (showHabit) Alignment.Top else Alignment.CenterVertically) {
            if (showHabit) {
                Box(Modifier.size(44.dp).background(hc.soft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(note.icon), contentDescription = null, tint = hc.ink, modifier = Modifier.size(22.dp))
                }
            }
            Column(Modifier.weight(1f).padding(start = if (showHabit) 12.dp else 0.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (showHabit) {
                    Text(note.habitName, style = type.titleMedium, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DayMarkView(note.mark, hc, marks, Modifier.size(16.dp))
                    Text(
                        listOfNotNull(note.outcome, dateLabel).joinToString(" · "),
                        style = type.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            Text(note.time, style = type.labelMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
        }
        Text(note.text, style = type.bodyLarge.copy(lineHeight = 1.5.em), color = colors.onSurface)
    }
}
