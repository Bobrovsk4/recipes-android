package com.example.recipes.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.outlined.Casino

private enum class Tab(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val iconSelected: ImageVector
) {
    Home("home", "Главная", Icons.Outlined.Home, Icons.Filled.Home),
    Random("random", "Случайный", Icons.Outlined.Casino, Icons.Filled.Casino),
    Settings("settings", "Настройки", Icons.Outlined.Settings, Icons.Filled.Settings)
}

private const val RECIPE_ROUTE = "recipe/{id}?drink={drink}"
private const val EDIT_ROUTE = "edit?id={id}&typeId={typeId}&daytimeId={daytimeId}&drink={drink}"

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    val showBottomBar = Tab.entries.any { tab ->
        destination?.hierarchy?.any { it.route == tab.route } == true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Tab.entries.forEach { tab ->
                        val selected = destination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) tab.iconSelected else tab.icon,
                                    contentDescription = tab.title
                                )
                            },
                            label = { Text(tab.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Home.route,
            // На экране рецепта нижней панели нет, поэтому отступ не нужен:
            // там Scaffold экрана сам учитывает системные панели.
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() }
        ) {
            composable(Tab.Home.route) { entry ->
                val refresh by entry.savedStateHandle
                    .getStateFlow("refresh", false).collectAsState()
                HomePager(
                    onRecipeClick = { id -> navController.navigate("recipe/$id") },
                    onDrinkClick = { id -> navController.navigate("recipe/$id?drink=true") },
                    onCreate = { t, d -> navController.navigate("edit?id=-1&typeId=$t&daytimeId=$d") },
                    onCreateDrink = { t -> navController.navigate("edit?id=-1&typeId=$t&daytimeId=-1&drink=true") },
                    refreshSignal = refresh,
                    onRefreshHandled = { entry.savedStateHandle["refresh"] = false }
                )
            }
            composable(Tab.Random.route) {
                RandomScreen(onRecipeClick = { id -> navController.navigate("recipe/$id") })
            }
            composable(
                route = EDIT_ROUTE,
                arguments = listOf(
                    navArgument("id") { type = NavType.IntType; defaultValue = -1 },
                    navArgument("typeId") { type = NavType.IntType; defaultValue = -1 },
                    navArgument("daytimeId") { type = NavType.IntType; defaultValue = -1 },
                    navArgument("drink") { type = NavType.BoolType; defaultValue = false }
                ),
                enterTransition = { slideInHorizontally(initialOffsetX = { it / 4 }) + fadeIn() },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 4 }) + fadeOut() }
            ) { entry ->
                val args = entry.arguments
                RecipeEditScreen(
                    recipeId = args?.getInt("id") ?: -1,
                    typeId = args?.getInt("typeId") ?: -1,
                    daytimeId = args?.getInt("daytimeId") ?: -1,
                    drink = args?.getBoolean("drink") ?: false,
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("refresh", true)
                        runCatching {
                            navController.getBackStackEntry(Tab.Home.route).savedStateHandle["refresh"] = true
                        }
                        navController.popBackStack()
                    }
                )
            }
            composable(Tab.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = RECIPE_ROUTE,
                arguments = listOf(
                    navArgument("id") { type = NavType.IntType },
                    navArgument("drink") { type = NavType.BoolType; defaultValue = false }
                ),
                enterTransition = {
                    slideInHorizontally(initialOffsetX = { it / 4 }) + fadeIn()
                },
                popExitTransition = {
                    slideOutHorizontally(targetOffsetX = { it / 4 }) + fadeOut()
                }
            ) { entry ->
                val id = entry.arguments?.getInt("id") ?: 0
                val drink = entry.arguments?.getBoolean("drink") ?: false
                val refresh by entry.savedStateHandle
                    .getStateFlow("refresh", false).collectAsState()
                RecipeScreen(
                    recipeId = id,
                    onBack = { navController.popBackStack() },
                    onEdit = { t, d -> navController.navigate("edit?id=$id&typeId=$t&daytimeId=$d&drink=$drink") },
                    refreshSignal = refresh,
                    onRefreshHandled = { entry.savedStateHandle["refresh"] = false },
                    drink = drink
                )
            }
        }
    }
}