package app.sprout.habits.ui.nav

import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.BasicText
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.sprout.habits.ui.detail.HabitDetailScreen
import app.sprout.habits.ui.detail.HabitDetailViewModel
import app.sprout.habits.ui.edit.EditHabitScreen
import app.sprout.habits.ui.insights.InsightsScreen
import app.sprout.habits.ui.insights.InsightsViewModel
import app.sprout.habits.ui.note.WriteNoteScreen
import app.sprout.habits.ui.note.WriteNoteViewModel
import app.sprout.habits.ui.journal.JournalScreen
import app.sprout.habits.ui.journal.JournalViewModel
import app.sprout.habits.ui.habits.HabitsScreen
import app.sprout.habits.ui.habits.HabitsViewModel
import app.sprout.habits.ui.manage.ManageHabitsScreen
import app.sprout.habits.ui.manage.ManageHabitsViewModel
import app.sprout.habits.ui.edit.EditHabitViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import app.sprout.habits.AppContainer
import app.sprout.habits.data.Settings
import app.sprout.habits.ui.more.AboutScreen
import app.sprout.habits.ui.more.LicencesScreen
import app.sprout.habits.ui.more.MoreScreen
import app.sprout.habits.R
import kotlinx.coroutines.launch
import app.sprout.habits.data.SampleData
import app.sprout.habits.ui.components.CappedFontScale
import app.sprout.habits.ui.today.TodayScreen
import app.sprout.habits.ui.today.TodayViewModel
import kotlin.reflect.KClass

private enum class Tab(val route: Any, val routeClass: KClass<*>, @StringRes val label: Int, @DrawableRes val icon: Int) {
    TODAY(TodayRoute, TodayRoute::class, R.string.today, R.drawable.ic_nav_today),
    HABITS(HabitsRoute, HabitsRoute::class, R.string.nav_habits, R.drawable.ic_nav_habits),
    JOURNAL(JournalRoute, JournalRoute::class, R.string.journal_title, R.drawable.ic_nav_journal),
    INSIGHTS(InsightsRoute, InsightsRoute::class, R.string.nav_insights, R.drawable.ic_nav_insights),
    MORE(MoreRoute, MoreRoute::class, R.string.tab_more, R.drawable.ic_nav_more),
}

@Composable
fun SproutNavHost(
    container: AppContainer,
    settings: Settings,
    habitToOpen: Long?,
    onHabitOpened: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    LaunchedEffect(habitToOpen) {
        habitToOpen?.let { id ->
            navController.navigate(HabitDetailRoute(id)) { launchSingleTop = true }
            onHabitOpened()
        }
    }
    val currentTab = Tab.entries.firstOrNull { tab -> destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true }

    // The bar overlays the content so it can slide away on full-screen pages without making the
    // screen underneath jump; tab screens reserve room for it themselves (TabFrame).
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController,
            startDestination = TodayRoute,
            modifier = Modifier.fillMaxSize(),
            // Between tabs: a quick crossfade. Into and out of a page opened from a tab: Material's
            // shared-axis motion, a short slide with a fade, reversed on back.
            enterTransition = { if (initialState.isTab() && targetState.isTab()) fadeIn(tween(220)) else forwardIn() },
            exitTransition = { if (initialState.isTab() && targetState.isTab()) fadeOut(tween(160)) else forwardOut() },
            popEnterTransition = { if (initialState.isTab() && targetState.isTab()) fadeIn(tween(220)) else backIn() },
            popExitTransition = { if (initialState.isTab() && targetState.isTab()) fadeOut(tween(160)) else backOut() },
        ) {
            composable<TodayRoute> {
                TabFrame {
                val vm = viewModel { TodayViewModel(container.repository, container.settings, container.strings) }
                TodayScreen(
                    vm,
                    onAddHabit = { navController.navigate(EditHabitRoute()) },
                    onOpenHabit = { id -> navController.navigate(HabitDetailRoute(id)) },
                    onAddNote = { habitId, day -> navController.navigate(WriteNoteRoute(habitId = habitId, epochDay = day)) },
                    hasSampleData = settings.sampleHabitIds.isNotEmpty(),
                    onRemoveSampleData = { container.appScope.launch { SampleData.remove(container.repository, container.settings) } },
                )
                }
            }
            composable<WriteNoteRoute> { entry ->
                val route = entry.toRoute<WriteNoteRoute>()
                val vm = viewModel {
                    WriteNoteViewModel(container.repository, route.noteId, route.habitId, route.epochDay.takeIf { it >= 0 })
                }
                WriteNoteScreen(vm, settings.weekStart, onClose = { navController.popBackStack() })
            }
            composable<EditHabitRoute> { entry ->
                val id = entry.toRoute<EditHabitRoute>().habitId
                val vm = viewModel { EditHabitViewModel(container.repository, container.settings, id) }
                EditHabitScreen(vm, onClose = { navController.popBackStack() })
            }
            composable<HabitsRoute> {
                TabFrame {
                val vm = viewModel { HabitsViewModel(container.repository, container.settings, container.strings) }
                HabitsScreen(
                    vm,
                    onAddHabit = { navController.navigate(EditHabitRoute()) },
                    onManage = { navController.navigate(ManageHabitsRoute) },
                    onOpenHabit = { id -> navController.navigate(HabitDetailRoute(id)) },
                )
                }
            }
            composable<HabitDetailRoute> { entry ->
                val id = entry.toRoute<HabitDetailRoute>().habitId
                val vm = viewModel { HabitDetailViewModel(container.repository, container.settings, container.strings, id) }
                HabitDetailScreen(
                    vm,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(EditHabitRoute(id)) },
                    onAddNote = { navController.navigate(WriteNoteRoute(habitId = id)) },
                    onOpenNote = { noteId -> navController.navigate(WriteNoteRoute(noteId = noteId)) },
                    onSeeAllNotes = { navController.navigate(JournalRoute) { launchSingleTop = true } },
                )
            }
            composable<ManageHabitsRoute> {
                val vm = viewModel { ManageHabitsViewModel(container.repository) }
                ManageHabitsScreen(
                    vm,
                    onEdit = { id -> navController.navigate(EditHabitRoute(id)) },
                    onClose = { navController.popBackStack() },
                )
            }
            composable<JournalRoute> {
                TabFrame {
                val vm = viewModel { JournalViewModel(container.repository, container.settings, container.strings) }
                JournalScreen(
                    vm,
                    weekStart = settings.weekStart,
                    onAddNote = { navController.navigate(WriteNoteRoute()) },
                    onOpenNote = { id -> navController.navigate(WriteNoteRoute(noteId = id)) },
                )
                }
            }
            composable<InsightsRoute> {
                TabFrame {
                val vm = viewModel { InsightsViewModel(container.repository, container.settings, container.strings) }
                InsightsScreen(vm, settings.weekStart)
                }
            }
            composable<MoreRoute> {
                TabFrame {
                MoreScreen(
                    settings,
                    container.settings,
                    container.backup,
                    onOpenAbout = { navController.navigate(AboutRoute) },
                    onRemoveSampleData = { container.appScope.launch { SampleData.remove(container.repository, container.settings) } },
                )
                }
            }
            composable<AboutRoute> {
                AboutScreen(container.updates, onBack = { navController.popBackStack() }, onOpenLicences = { navController.navigate(LicencesRoute) })
            }
            composable<LicencesRoute> { LicencesScreen(onBack = { navController.popBackStack() }) }
        }
        AnimatedVisibility(
            visible = currentTab != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(250)) { it } + fadeIn(tween(250)),
            exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(200)),
        ) {
            // Keeps showing the last tab while the bar slides out.
            var lastTab by remember { mutableStateOf(Tab.TODAY) }
            if (currentTab != null) lastTab = currentTab
            SproutNavigationBar(lastTab) { tab ->
                navController.navigate(tab.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }
    }
}

/** Tab screens: opaque, below the status bar and above the navigation bar. */
@Composable
private fun TabFrame(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = NAV_BAR_HEIGHT),
    ) { content() }
}

/** Material 3 navigation bar height, not counting the system navigation inset. */
private val NAV_BAR_HEIGHT = 80.dp

private fun NavBackStackEntry.isTab() = Tab.entries.any { tab -> destination.hierarchy.any { it.hasRoute(tab.routeClass) } }

private const val MOTION_MS = 300

private fun forwardIn() = slideInHorizontally(tween(MOTION_MS)) { it / 8 } + fadeIn(tween(MOTION_MS))
private fun forwardOut() = slideOutHorizontally(tween(MOTION_MS)) { -it / 8 } + fadeOut(tween(MOTION_MS / 2))
private fun backIn() = slideInHorizontally(tween(MOTION_MS)) { -it / 8 } + fadeIn(tween(MOTION_MS))
private fun backOut() = slideOutHorizontally(tween(MOTION_MS)) { it / 8 } + fadeOut(tween(MOTION_MS / 2))

@Composable
private fun SproutNavigationBar(current: Tab, onSelect: (Tab) -> Unit) {
    val colors = MaterialTheme.colorScheme
    // Five labels have to fit in one row; past 1.3x they would wrap mid-word.
    CappedFontScale {
    NavigationBar(containerColor = colors.surfaceContainer) {
        Tab.entries.forEach { tab ->
            val selected = tab == current
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab) },
                icon = { Icon(painterResource(tab.icon), contentDescription = null, Modifier.size(22.dp)) },
                label = {
                    // Long translations ("Gewohnheiten", "Statistiques") shrink to fit instead of being cut off.
                    val style = MaterialTheme.typography.labelMedium
                    BasicText(
                        stringResource(tab.label),
                        maxLines = 1,
                        style = style.copy(
                            color = LocalContentColor.current,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = style.fontSize, stepSize = 0.5.sp),
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.onPrimaryContainer,
                    selectedTextColor = colors.onSurface,
                    indicatorColor = colors.primaryContainer,
                    unselectedIconColor = colors.onSurfaceVariant,
                    unselectedTextColor = colors.onSurface,
                ),
            )
        }
    }
    }
}
