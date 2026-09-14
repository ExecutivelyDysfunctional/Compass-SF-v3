package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.*

class MainActivity : ComponentActivity() {
    private val viewModel: CompassViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val bottomNavigationItems = listOf(
                Screen.Now,
                Screen.Find,
                Screen.Ask,
                Screen.Ebt,
                Screen.Add,
                Screen.Day,
                Screen.Info,
                Screen.Settings
            )

            Scaffold(
                containerColor = Ink950,
                bottomBar = {
                    // Only show bottom bar for primary screen routes (not resource detail)
                    val showBottomBar = currentRoute?.startsWith("detail") == false
                    if (showBottomBar) {
                        NavigationBar(
                            containerColor = Ink900,
                            tonalElevation = 8.dp,
                            modifier = Modifier
                                .navigationBarsPadding()
                                .height(64.dp)
                        ) {
                            bottomNavigationItems.forEach { screen ->
                                val isSelected = currentRoute == screen.route || 
                                        (screen == Screen.Find && currentRoute?.startsWith("find") == true)
                                
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Text(
                                            text = screen.icon,
                                            fontSize = 20.sp
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = screen.title,
                                            fontSize = 9.sp,
                                            color = if (isSelected) Beacon500 else Mist400
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Beacon500,
                                        unselectedIconColor = Mist400,
                                        indicatorColor = Ink800
                                    )
                                )
                            }
                        }
                    }
                },
                contentWindowInsets = WindowInsets.safeDrawing
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Ink950)
                        .padding(paddingValues)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Now.route
                    ) {
                        composable(Screen.Now.route) {
                            NowScreen(
                                viewModel = viewModel,
                                onNavigateToFind = { cat ->
                                    navController.navigate("find?category=$cat")
                                },
                                onNavigateToDetail = { id ->
                                    navController.navigate("detail/$id")
                                }
                            )
                        }

                        composable(
                            route = "find?category={category}",
                            arguments = listOf(
                                navArgument("category") {
                                    type = NavType.StringType
                                    defaultValue = "all"
                                }
                            )
                        ) { backStackEntry ->
                            val category = backStackEntry.arguments?.getString("category") ?: "all"
                            FindScreen(
                                viewModel = viewModel,
                                initialCategory = category,
                                onNavigateToDetail = { id ->
                                    navController.navigate("detail/$id")
                                }
                            )
                        }

                        // Fallback route without arguments
                        composable(Screen.Find.route) {
                            FindScreen(
                                viewModel = viewModel,
                                initialCategory = "all",
                                onNavigateToDetail = { id ->
                                    navController.navigate("detail/$id")
                                }
                            )
                        }

                        composable(Screen.Ask.route) {
                            AskScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { id ->
                                    navController.navigate("detail/$id")
                                }
                            )
                        }

                        composable(Screen.Ebt.route) {
                            EbtScreen(viewModel = viewModel)
                        }

                        composable(Screen.Add.route) {
                            AddScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { id ->
                                    navController.navigate("detail/$id") {
                                        popUpTo(Screen.Now.route)
                                    }
                                }
                            )
                        }

                        composable(Screen.Day.route) {
                            DayScreen(
                                viewModel = viewModel,
                                onNavigateToFind = {
                                    navController.navigate(Screen.Find.route)
                                }
                            )
                        }

                        composable(Screen.Info.route) {
                            InfoScreen()
                        }

                        composable(Screen.Settings.route) {
                            SettingsScreen(viewModel = viewModel)
                        }

                        composable(
                            route = "detail/{id}",
                            arguments = listOf(
                                navArgument("id") {
                                    type = NavType.IntType
                                }
                            )
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getInt("id") ?: 0
                            DetailScreen(
                                resourceId = id,
                                viewModel = viewModel,
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
