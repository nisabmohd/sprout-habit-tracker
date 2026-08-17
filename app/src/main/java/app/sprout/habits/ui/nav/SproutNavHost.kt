package app.sprout.habits.ui.nav

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import app.sprout.habits.ui.habits.HabitsScreen
import app.sprout.habits.ui.habits.HabitsViewModel
import app.sprout.habits.ui.manage.ManageHabitsScreen
import app.sprout.habits.ui.manage.ManageHabitsViewModel
import app.sprout.habits.ui.edit.EditHabitViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import app.sprout.habits.AppContainer
import app.sprout.habits.R
import app.sprout.habits.ui.today.TodayScreen
import app.sprout.habits.ui.today.TodayViewModel
import kotlin.reflect.KClass

private enum class Tab(val route: Any, val routeClass: KClass<*>, val label: String, @DrawableRes val icon: Int) {
    TODAY(TodayRoute, TodayRoute::class, "Today", R.drawable.ic_nav_today),
    HABITS(HabitsRoute, HabitsRoute::class, "Habits", R.drawable.ic_nav_habits),
    JOURNAL(JournalRoute, JournalRoute::class, "Journal", R.drawable.ic_nav_journal),
    INSIGHTS(InsightsRoute, InsightsRoute::class, "Insights", R.drawable.ic_nav_insights),
    MORE(MoreRoute, MoreRoute::class, "More", R.drawable.ic_nav_more),
}

@Composable
fun SproutNavHost(container: AppContainer, moreContent: @Composable () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val currentTab = Tab.entries.firstOrNull { tab -> destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true }

    Scaffold(
        bottomBar = {
            // Only the five tab roots show the bar; detail screens are full screen.
            if (currentTab != null) {
                SproutNavigationBar(currentTab) { tab ->
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController,
            startDestination = TodayRoute,
            modifier = Modifier.padding(if (currentTab != null) padding else PaddingValues()),
        ) {
            composable<TodayRoute> {
                val vm = viewModel { TodayViewModel(container.repository, container.settings) }
                TodayScreen(
                    vm,
                    onAddHabit = { navController.navigate(EditHabitRoute()) },
                    onOpenHabit = { id -> navController.navigate(HabitDetailRoute(id)) },
                )
            }
            composable<EditHabitRoute> { entry ->
                val id = entry.toRoute<EditHabitRoute>().habitId
                val vm = viewModel { EditHabitViewModel(container.repository, container.settings, id) }
                EditHabitScreen(vm, onClose = { navController.popBackStack() })
            }
            composable<HabitsRoute> {
                val vm = viewModel { HabitsViewModel(container.repository, container.settings) }
                HabitsScreen(
                    vm,
                    onAddHabit = { navController.navigate(EditHabitRoute()) },
                    onManage = { navController.navigate(ManageHabitsRoute) },
                    onOpenHabit = { id -> navController.navigate(HabitDetailRoute(id)) },
                )
            }
            composable<HabitDetailRoute> { entry ->
                val id = entry.toRoute<HabitDetailRoute>().habitId
                val vm = viewModel { HabitDetailViewModel(container.repository, container.settings, id) }
                HabitDetailScreen(
                    vm,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(EditHabitRoute(id)) },
                    // Wired up with the Write note screen.
                    onAddNote = null,
                    onOpenNote = null,
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
            composable<JournalRoute> { Placeholder("Journal") }
            composable<InsightsRoute> { Placeholder("Insights") }
            composable<MoreRoute> { moreContent() }
        }
    }
}

@Composable
private fun SproutNavigationBar(current: Tab, onSelect: (Tab) -> Unit) {
    val colors = MaterialTheme.colorScheme
    NavigationBar(containerColor = colors.surfaceContainer) {
        Tab.entries.forEach { tab ->
            val selected = tab == current
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab) },
                icon = { Icon(painterResource(tab.icon), contentDescription = null, Modifier.size(22.dp)) },
                label = {
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
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

@Composable
private fun Placeholder(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
    }
}
