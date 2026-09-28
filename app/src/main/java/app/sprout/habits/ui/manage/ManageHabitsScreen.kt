package app.sprout.habits.ui.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import app.sprout.habits.R
import app.sprout.habits.ui.components.HeaderIconButton
import app.sprout.habits.data.Habit
import app.sprout.habits.data.HabitIcon
import app.sprout.habits.ui.theme.habitColors

@Composable
fun ManageHabitsScreen(
    viewModel: ManageHabitsViewModel,
    onEdit: (Long) -> Unit,
    onClose: () -> Unit,
) {
    val active by viewModel.active.collectAsStateWithLifecycle()
    val archived by viewModel.archived.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<Habit?>(null) }

    // Local copy so a drag can reorder instantly; replaced whenever the database changes.
    val order = remember { mutableStateListOf<Habit>() }
    LaunchedEffect(active) {
        active?.let {
            order.clear()
            order.addAll(it)
        }
    }
    val listState = rememberLazyListState()
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val haptics = LocalHapticFeedback.current

    fun move(from: Int, to: Int) {
        if (to !in order.indices || from == to) return
        order.add(to, order.removeAt(from))
    }

    fun commit() = viewModel.reorder(order.map { it.id })

    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(start = 16.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderIconButton(R.drawable.ic_close, stringResource(R.string.action_close), onClick = onClose)
            Text(stringResource(R.string.manage_habits), style = MaterialTheme.typography.titleLarge, color = colors.onBackground, modifier = Modifier.padding(start = 14.dp))
        }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "hint") {
                Text(
                    stringResource(R.string.reorder_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            itemsIndexed(order, key = { _, h -> h.id }) { index, habit ->
                val dragging = draggingId == habit.id
                val moveUp = stringResource(R.string.move_up)
                val moveDown = stringResource(R.string.move_down)
                HabitRow(
                    habit = habit,
                    modifier = Modifier
                        .then(if (dragging) Modifier.zIndex(1f).graphicsLayer { translationY = dragOffset } else Modifier.animateItem())
                        .semantics {
                            customActions = buildList {
                                if (index > 0) add(CustomAccessibilityAction(moveUp) { move(index, index - 1); commit(); true })
                                if (index < order.lastIndex) add(CustomAccessibilityAction(moveDown) { move(index, index + 1); commit(); true })
                            }
                        },
                    elevated = dragging,
                    handleModifier = Modifier.pointerInput(habit.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                draggingId = habit.id
                                dragOffset = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                                val from = order.indexOfFirst { it.id == draggingId }
                                val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggingId } ?: return@detectDragGesturesAfterLongPress
                                val center = info.offset + info.size / 2 + dragOffset
                                val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                    item.key is Long && item.key != draggingId && center.toInt() in item.offset..(item.offset + item.size)
                                } ?: return@detectDragGesturesAfterLongPress
                                val to = order.indexOfFirst { it.id == target.key }
                                if (to >= 0) {
                                    move(from, to)
                                    // Keep the dragged row under the finger after it moves slots.
                                    dragOffset -= (target.offset - info.offset)
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                }
                            },
                            onDragEnd = {
                                draggingId = null
                                dragOffset = 0f
                                commit()
                            },
                            onDragCancel = {
                                draggingId = null
                                dragOffset = 0f
                            },
                        )
                    },
                ) {
                    TextButton(onClick = { onEdit(habit.id) }) { Text(stringResource(R.string.edit)) }
                    TextButton(onClick = { viewModel.setArchived(habit, true) }) { Text(stringResource(R.string.archive)) }
                }
            }
            if (archived.isNotEmpty()) {
                item(key = "archived") {
                    Text(
                        stringResource(R.string.archived),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onBackground,
                        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp),
                    )
                }
                items(archived, key = { "a${it.id}" }) { habit ->
                    HabitRow(habit, Modifier.animateItem(), elevated = false, handleModifier = null) {
                        TextButton(onClick = { viewModel.setArchived(habit, false) }) { Text(stringResource(R.string.restore)) }
                        TextButton(onClick = { deleting = habit }) { Text(stringResource(R.string.action_delete), color = colors.error) }
                    }
                }
            }
        }
    }

    deleting?.let { habit ->
        DeleteHabitDialog(habit.name, onConfirm = { viewModel.delete(habit); deleting = null }, onDismiss = { deleting = null })
    }
}

@Composable
private fun HabitRow(
    habit: Habit,
    modifier: Modifier,
    elevated: Boolean,
    handleModifier: Modifier?,
    actions: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val hc = habitColors(habit.colorHue.toFloat())
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(68.dp)
            .then(if (elevated) Modifier.shadow(8.dp, shape) else Modifier)
            .background(colors.surfaceContainerLowest, shape)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).background(hc.soft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(HabitIcon.fromKey(habit.icon).drawable), contentDescription = null, tint = hc.ink, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            habit.name,
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions()
        if (handleModifier != null) {
            Box(handleModifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_drag), contentDescription = stringResource(R.string.drag_to_reorder), tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
        }
    }
}
