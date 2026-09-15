package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CompassAppTheme(viewModel = viewModel) {
                val theme = LocalCompassTheme.current
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Streamlined 5 primary bottom navigation destinations (reduced from 8 to remove clutter)
                val primaryBottomNavItems = listOf(
                    Screen.Now,
                    Screen.Find,
                    Screen.Ask,
                    Screen.Day,
                    Screen.Ebt
                )

                val isPrimaryTab = currentRoute in listOf(
                    Screen.Now.route,
                    Screen.Find.route,
                    Screen.Ask.route,
                    Screen.Day.route,
                    Screen.Ebt.route
                ) || currentRoute?.startsWith("find") == true

                val isSubScreen = currentRoute in listOf(
                    Screen.Add.route,
                    Screen.Info.route,
                    Screen.Settings.route
                )

                val isDetailScreen = currentRoute?.startsWith("detail") == true

                Scaffold(
                    containerColor = theme.background,
                    topBar = {
                        // Show unified top app bar on primary screens and sub-screens (Detail screen has its own top header)
                        if (!isDetailScreen) {
                            TopAppBar(
                                title = {
                                    if (isSubScreen) {
                                        val title = when (currentRoute) {
                                            Screen.Add.route -> "Add New Resource"
                                            Screen.Info.route -> "City Info & Hotlines"
                                            Screen.Settings.route -> "Settings & Preferences"
                                            else -> "Compass SF"
                                        }
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = theme.textPrimary
                                        )
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Explore,
                                                contentDescription = null,
                                                tint = theme.accent,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Compass SF",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = theme.textPrimary
                                                )
                                                val subtitle = when {
                                                    currentRoute == Screen.Now.route -> "Today's Navigator"
                                                    currentRoute?.startsWith("find") == true -> "Directory Search"
                                                    currentRoute == Screen.Ask.route -> "AI Guide"
                                                    currentRoute == Screen.Day.route -> "Daily Checklist"
                                                    currentRoute == Screen.Ebt.route -> "EBT Restaurant Meals"
                                                    else -> "Street Resource Guide"
                                                }
                                                Text(
                                                    text = subtitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = theme.textSecondary
                                                )
                                            }
                                        }
                                    }
                                },
                                navigationIcon = {
                                    if (isSubScreen) {
                                        IconButton(
                                            onClick = { navController.popBackStack() },
                                            modifier = Modifier.testTag("top_bar_back_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back",
                                                tint = theme.textPrimary
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    if (!isSubScreen) {
                                        // Moved Info icon here to unclutter bottom bar
                                        IconButton(
                                            onClick = {
                                                if (currentRoute != Screen.Info.route) {
                                                    navController.navigate(Screen.Info.route)
                                                }
                                            },
                                            modifier = Modifier.testTag("top_nav_info_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Info,
                                                contentDescription = "Community Info & Hotlines",
                                                tint = theme.textSecondary
                                            )
                                        }
                                        // Moved Settings icon here to unclutter bottom bar
                                        IconButton(
                                            onClick = {
                                                if (currentRoute != Screen.Settings.route) {
                                                    navController.navigate(Screen.Settings.route)
                                                }
                                            },
                                            modifier = Modifier.testTag("top_nav_settings_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Settings,
                                                contentDescription = "Settings & Appearance",
                                                tint = theme.textSecondary
                                            )
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = theme.surface,
                                    titleContentColor = theme.textPrimary,
                                    actionIconContentColor = theme.textSecondary,
                                    navigationIconContentColor = theme.textPrimary
                                )
                            )
                        }
                    },
                    floatingActionButton = {
                        // Moved 'Add' action out of cramped bottom bar into a prominent Floating Action Button
                        val showFab = currentRoute in listOf(
                            Screen.Now.route,
                            Screen.Find.route,
                            Screen.Day.route
                        ) || currentRoute?.startsWith("find") == true

                        if (showFab) {
                            FloatingActionButton(
                                onClick = {
                                    if (currentRoute != Screen.Add.route) {
                                        navController.navigate(Screen.Add.route)
                                    }
                                },
                                containerColor = theme.accent,
                                contentColor = theme.onAccent,
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                                modifier = Modifier.testTag("fab_add_resource")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add New Resource",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    bottomBar = {
                        // Display clean 5-item bottom bar only on primary browsing destinations
                        if (isPrimaryTab) {
                            NavigationBar(
                                containerColor = theme.surface,
                                tonalElevation = 6.dp,
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .height(70.dp)
                            ) {
                                primaryBottomNavItems.forEach { screen ->
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
                                            Icon(
                                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                                contentDescription = screen.title,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = screen.title,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                maxLines = 1
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = theme.accent,
                                            selectedTextColor = theme.accent,
                                            unselectedIconColor = theme.textSecondary,
                                            unselectedTextColor = theme.textSecondary,
                                            indicatorColor = theme.surfaceVariant
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
                            .background(theme.background)
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
}
