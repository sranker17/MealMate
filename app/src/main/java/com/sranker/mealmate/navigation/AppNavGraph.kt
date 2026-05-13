package com.sranker.mealmate.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sranker.mealmate.ui.screens.MealDetailScreen
import com.sranker.mealmate.ui.screens.MealEditScreen
import com.sranker.mealmate.ui.screens.MealListScreen
import com.sranker.mealmate.ui.screens.MenuHistoryDetailScreen
import com.sranker.mealmate.ui.screens.MenuHistoryScreen
import com.sranker.mealmate.ui.screens.PlannerScreen
import com.sranker.mealmate.ui.screens.SettingsScreen
import com.sranker.mealmate.ui.screens.TagManageScreen
import com.sranker.mealmate.ui.viewmodel.MealDetailViewModel
import com.sranker.mealmate.ui.viewmodel.MealEditViewModel
import com.sranker.mealmate.ui.viewmodel.MealListViewModel
import com.sranker.mealmate.ui.viewmodel.MenuHistoryDetailViewModel
import com.sranker.mealmate.ui.viewmodel.MenuHistoryViewModel
import com.sranker.mealmate.ui.viewmodel.PlannerViewModel
import com.sranker.mealmate.ui.viewmodel.SettingsViewModel
import com.sranker.mealmate.ui.viewmodel.TagManageViewModel
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

/**
 * Route constants for the MealMate navigation graph.
 */
object Routes {
    const val PLANNER = "planner"
    const val MEAL_LIST = "meal_list"
    const val MEAL_DETAIL = "meal_detail/{mealId}"
    const val MEAL_EDIT = "meal_edit/{mealId}"
    const val SETTINGS = "settings"
    const val MENU_HISTORY = "menu_history"
    const val MENU_HISTORY_DETAIL = "menu_history_detail/{menuId}"
    const val TAG_MANAGE = "tag_manage"

    fun mealDetail(mealId: Long) = "meal_detail/$mealId"
    fun mealEdit(mealId: Long) = "meal_edit/$mealId"
    fun menuHistoryDetail(menuId: Long) = "menu_history_detail/$menuId"
}

/**
 * Main navigation graph for the MealMate app.
 *
 * @param navController The [NavHostController] for navigation.
 * @param windowWidthSizeClass The current window width class for responsive layouts.
 * @param modifier Optional [Modifier] applied to the [NavHost].
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    windowWidthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.PLANNER,
        modifier = modifier
    ) {
        // region Planner
        composable(
            route = Routes.PLANNER,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            val viewModel: PlannerViewModel = hiltViewModel()
            PlannerScreen(
                viewModel = viewModel,
                windowWidthSizeClass = windowWidthSizeClass,
                onNavigateToMeals = { navController.navigate(Routes.MEAL_LIST) }
            )
        }

        // region Meal List
        composable(
            route = Routes.MEAL_LIST,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            val viewModel: MealListViewModel = hiltViewModel()
            MealListScreen(
                viewModel = viewModel,
                onMealClick = { mealId -> navController.navigate(Routes.mealDetail(mealId)) },
                onAddMealClick = { navController.navigate(Routes.mealEdit(0L)) },
                onManageTagsClick = { navController.navigate(Routes.TAG_MANAGE) },
                onDeleteMeal = { viewModel.deleteMeal(it) }
            )
        }

        // region Meal Detail
        composable(
            route = Routes.MEAL_DETAIL,
            arguments = listOf(navArgument("mealId") { type = NavType.LongType }),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            val viewModel: MealDetailViewModel = hiltViewModel()
            MealDetailScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onEditClick = { mealId -> navController.navigate(Routes.mealEdit(mealId)) },
                onDeleteClick = {
                    navController.popBackStack()
                }
            )
        }

        // region Meal Edit
        composable(
            route = Routes.MEAL_EDIT,
            arguments = listOf(navArgument("mealId") { type = NavType.LongType; defaultValue = 0L }),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            val viewModel: MealEditViewModel = hiltViewModel()
            MealEditScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onMealSaved = { navController.popBackStack() }
            )
        }

        // region Settings
        composable(
            route = Routes.SETTINGS,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // region Menu History
        composable(
            route = Routes.MENU_HISTORY,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            val viewModel: MenuHistoryViewModel = hiltViewModel()
            MenuHistoryScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onMenuClick = { menuId -> navController.navigate(Routes.menuHistoryDetail(menuId)) }
            )
        }

        // region Menu History Detail
        composable(
            route = Routes.MENU_HISTORY_DETAIL,
            arguments = listOf(navArgument("menuId") { type = NavType.LongType }),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            val viewModel: MenuHistoryDetailViewModel = hiltViewModel()
            MenuHistoryDetailScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // region Tag Manage
        composable(
            route = Routes.TAG_MANAGE,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            val viewModel: TagManageViewModel = hiltViewModel()
            TagManageScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
