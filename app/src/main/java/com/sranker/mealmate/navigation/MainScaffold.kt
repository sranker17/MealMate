package com.sranker.mealmate.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sranker.mealmate.R

/**
 * Represents a single destination in the bottom navigation bar / navigation rail.
 */
data class BottomNavItem(
    val route: String,
    val labelResId: Int,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.PLANNER, R.string.nav_planner, Icons.Default.ViewTimeline),
    BottomNavItem(Routes.MEAL_LIST, R.string.nav_meals, Icons.Default.Restaurant),
    BottomNavItem(Routes.MENU_HISTORY, R.string.nav_history, Icons.Default.CalendarMonth),
    BottomNavItem(Routes.SETTINGS, R.string.nav_settings, Icons.Default.Settings)
)

/**
 * Main scaffold with responsive navigation:
 * - Phone: Bottom [NavigationBar].
 * - Tablet: [NavigationRail] on the left.
 *
 * @param windowWidthSizeClass The current window width class (for deciding phone vs tablet layout).
 * @param navController The [NavHostController] for navigation.
 * @param content The [AppNavGraph] content composable.
 */
@Composable
fun MainScaffold(
    windowWidthSizeClass: WindowWidthSizeClass,
    navController: NavHostController = rememberNavController(),
    content: @Composable (NavHostController, WindowWidthSizeClass) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Determine if the current screen is a "top-level" tab
    val isTopLevel = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }

    if (windowWidthSizeClass == WindowWidthSizeClass.Expanded) {
        // Tablet layout: NavigationRail
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                header = {
                    Text(
                        text = "MM",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            ) {
                bottomNavItems.forEach { item ->
                    val selected =
                        currentDestination?.hierarchy?.any { it.route == item.route } == true
                    val label = stringResource(item.labelResId)
                    NavigationRailItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = label
                            )
                        },
                        label = { Text(label) }
                    )
                }
            }
            content(navController, windowWidthSizeClass)
        }
    } else {
        // Phone layout: Scaffold with BottomNavigationBar
        Scaffold(
            bottomBar = {
                // Only show bottom bar on top-level destinations
                if (isTopLevel) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            val selected =
                                currentDestination?.hierarchy?.any { it.route == item.route } == true
                            val label = stringResource(item.labelResId)
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)) {
                content(navController, windowWidthSizeClass)
            }
        }
    }
}