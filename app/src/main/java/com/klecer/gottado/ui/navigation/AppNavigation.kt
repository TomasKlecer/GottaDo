package com.klecer.gottado.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.klecer.gottado.ui.screen.categorylist.CategoryListScreen
import com.klecer.gottado.ui.screen.categorysettings.CategoryTabViewModel
import com.klecer.gottado.ui.screen.recordeditoptions.RecordEditOptionsScreen
import com.klecer.gottado.ui.screen.recordeditoptions.RecordEditOptionsViewModel
import com.klecer.gottado.ui.screen.routineedit.RoutineEditScreen
import com.klecer.gottado.ui.screen.routineedit.RoutineEditViewModel
import com.klecer.gottado.ui.screen.routinelist.RoutineListScreen
import com.klecer.gottado.ui.screen.routinelist.RoutineListViewModel
import com.klecer.gottado.ui.screen.settings.SettingsScreen
import com.klecer.gottado.ui.screen.settings.SettingsViewModel
import com.klecer.gottado.ui.screen.trash.TrashScreen
import com.klecer.gottado.ui.screen.trash.TrashViewModel
import com.klecer.gottado.ui.screen.widgetlist.WidgetListScreen
import com.klecer.gottado.ui.screen.widgetsettings.WidgetTabViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    context: Context,
    modifier: Modifier = Modifier
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = NavRoutes.WIDGET_LIST
    ) {
        composable(NavRoutes.WIDGET_LIST) {
            val viewModel: WidgetTabViewModel = hiltViewModel()
            WidgetListScreen(viewModel = viewModel)
        }
        composable(
            NavRoutes.WIDGET_SETTINGS,
            arguments = listOf(navArgument("widgetId") { defaultValue = "0" })
        ) {
            val viewModel: WidgetTabViewModel = hiltViewModel()
            WidgetListScreen(viewModel = viewModel)
        }
        composable(NavRoutes.CATEGORY_LIST) {
            val viewModel: CategoryTabViewModel = hiltViewModel()
            CategoryListScreen(
                viewModel = viewModel,
                onAddRoutine = { categoryId ->
                    navController.navigate(NavRoutes.routineEdit(categoryId, 0))
                },
                onEditRoutine = { categoryId, routineId ->
                    navController.navigate(NavRoutes.routineEdit(categoryId, routineId))
                }
            )
        }
        composable(NavRoutes.RECORD_EDIT_OPTIONS) {
            val viewModel: RecordEditOptionsViewModel = hiltViewModel()
            RecordEditOptionsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            NavRoutes.ROUTINE_LIST,
            arguments = listOf(navArgument("categoryId") { defaultValue = "0" })
        ) {
            val viewModel: RoutineListViewModel = hiltViewModel()
            RoutineListScreen(
                viewModel = viewModel,
                onAddRoutine = { categoryId ->
                    navController.navigate(NavRoutes.routineEdit(categoryId, 0))
                },
                onEditRoutine = { routineId ->
                    navController.navigate(NavRoutes.routineEdit(viewModel.categoryId, routineId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            NavRoutes.ROUTINE_EDIT,
            arguments = listOf(
                navArgument("categoryId") { defaultValue = "0" },
                navArgument("routineId") { defaultValue = "0" }
            )
        ) {
            val viewModel: RoutineEditViewModel = hiltViewModel()
            RoutineEditScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.TRASH) {
            val viewModel: TrashViewModel = hiltViewModel()
            TrashScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.SETTINGS) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(viewModel = viewModel)
        }
    }
}
