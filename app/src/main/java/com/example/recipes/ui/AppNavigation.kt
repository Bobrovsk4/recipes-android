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

private enum class Tab(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val iconSelected: ImageVector
) {
    Home("home", "Главная", Icons.Outlined.Home, Icons.Filled.Home),
    Settings("settings", "Настройки", Icons.Outlined.Settings, Icons.Filled.Settings)
}

private const val RECIPE_ROUTE = "recipe/{id}"

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    // Нижняя панель видна только на экранах вкладок, на экране рецепта скрыта
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
            composable(Tab.Home.route) {
                HomeScreen(onRecipeClick = { id -> navController.navigate("recipe/$id") })
            }
            composable(Tab.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = RECIPE_ROUTE,
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
                enterTransition = {
                    slideInHorizontally(initialOffsetX = { it / 4 }) + fadeIn()
                },
                popExitTransition = {
                    slideOutHorizontally(targetOffsetX = { it / 4 }) + fadeOut()
                }
            ) { entry ->
                val id = entry.arguments?.getInt("id") ?: 0
                RecipeScreen(recipeId = id, onBack = { navController.popBackStack() })
            }
        }
    }
}