package app.hermes.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.hermes.onboarding.OnboardingScreen
import app.hermes.ui.DocumentsScreen
import app.hermes.ui.HabitsScreen
import app.hermes.ui.InboxScreen
import app.hermes.ui.MemoryScreen
import app.hermes.ui.MoneyScreen
import app.hermes.ui.MoreScreen
import app.hermes.ui.PeopleScreen
import app.hermes.ui.PlaceholderScreen
import app.hermes.ui.ProjectsScreen
import app.hermes.ui.SettingsScreen
import app.hermes.ui.TodayScreen

/**
 * Single-activity root: one [Scaffold] owns the top bar (title + back for non-tab
 * screens) and the bottom bar (visible only on the tab roots), wrapping one NavHost.
 * Onboarding renders with no chrome at all.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HermesAppRoot(startOnboarded: Boolean) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    val isOnboarding = route == Routes.ONBOARDING
    val showBottomBar = route in BOTTOM_BAR_ROUTES

    Scaffold(
        topBar = {
            if (!isOnboarding && route != null) {
                TopAppBar(
                    title = { Text(stringResource(titleForRoute(route))) },
                    navigationIcon = {
                        if (route !in BOTTOM_BAR_ROUTES) {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                HermesBottomBar(currentRoute = route, onSelect = { navigateTopLevel(navController, it) })
            }
        },
    ) { innerPadding ->
        HermesNavHost(
            navController = navController,
            startDestination = if (startOnboarded) Routes.TODAY else Routes.ONBOARDING,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun HermesBottomBar(currentRoute: String?, onSelect: (TopLevelDestination) -> Unit) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onSelect(destination) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.labelRes)) },
            )
        }
    }
}

private fun navigateTopLevel(navController: NavHostController, destination: TopLevelDestination) {
    if (destination == TopLevelDestination.CAPTURE) {
        // Capture is a standalone destination, not a tab — a plain navigate so the future
        // external share entry (M2) reaches the same place and back returns cleanly (D33).
        navController.navigate(Routes.CAPTURE) { launchSingleTop = true }
        return
    }
    navController.navigate(destination.route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun HermesNavHost(navController: NavHostController, startDestination: String, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    if (navController.previousBackStackEntry != null) {
                        // Re-entered from Settings → return where we came from.
                        navController.popBackStack()
                    } else {
                        // First run → enter the app, dropping onboarding from the back stack.
                        navController.navigate(Routes.TODAY) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                },
            )
        }
        // Screens with a seeded read model in Demo (empty state otherwise).
        composable(Routes.TODAY) { TodayScreen() }
        composable(Routes.INBOX) { InboxScreen() }
        composable(Routes.MEMORY) { MemoryScreen() }
        composable(Routes.DOCUMENTS) { DocumentsScreen() }
        composable(Routes.MONEY) { MoneyScreen() }
        composable(Routes.HABITS) { HabitsScreen() }
        composable(Routes.PROJECTS) { ProjectsScreen() }
        composable(Routes.PEOPLE) { PeopleScreen() }
        // Still stubs — their features arrive later (Capture M2, Chat M5, Briefs M4).
        composable(Routes.CAPTURE) { PlaceholderScreen(route = Routes.CAPTURE) }
        composable(Routes.CHAT) { PlaceholderScreen(route = Routes.CHAT) }
        composable(Routes.BRIEFS) { PlaceholderScreen(route = Routes.BRIEFS) }
        composable(Routes.MORE) { MoreScreen(onOpen = { navController.navigate(it) }) }
        composable(Routes.SETTINGS) {
            SettingsScreen(onChangeProvider = { navController.navigate(Routes.ONBOARDING) })
        }
    }
}
