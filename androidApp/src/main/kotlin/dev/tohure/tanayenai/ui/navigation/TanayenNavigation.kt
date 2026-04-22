package dev.tohure.tanayenai.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.tohure.tanayenai.R
import dev.tohure.tanayenai.ui.chat.ChatScreen
import dev.tohure.tanayenai.ui.clinical.ClinicalProfileScreen
import dev.tohure.tanayenai.ui.dashboard.DashboardScreen
import dev.tohure.tanayenai.ui.notification.NotificationSettingsContent
import dev.tohure.tanayenai.ui.pantry.PantryScreen
import dev.tohure.tanayenai.ui.theme.BackgroundColor
import dev.tohure.tanayenai.ui.theme.PrimaryGreen
import dev.tohure.tanayenai.ui.theme.SurfaceColor
import dev.tohure.tanayenai.ui.theme.TextMutedColor

sealed class Screen(
    val route: String,
    val label: String,
    val iconRes: Int,
) {
    data object Dashboard : Screen("dashboard", "Inicio", R.drawable.ic_home)

    data object Chat : Screen("chat", "Asistente", R.drawable.ic_chat)

    data object Pantry : Screen("pantry", "Alacena", R.drawable.ic_pantry)

    data object Profile : Screen("profile", "Perfil", R.drawable.ic_profile)
}

private val bottomNavItems = listOf(Screen.Dashboard, Screen.Chat, Screen.Pantry, Screen.Profile)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TanayenNavigation() {
    var showNotificationSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        containerColor = BackgroundColor,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceColor,
                tonalElevation = 2f.dp,
            ) {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(screen.iconRes),
                                contentDescription = screen.label,
                            )
                        },
                        label = {
                            Text(
                                text = screen.label,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        colors =
                            NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryGreen,
                                selectedTextColor = PrimaryGreen,
                                indicatorColor = Color(0xFFE8F5EE),
                                unselectedIconColor = TextMutedColor,
                                unselectedTextColor = TextMutedColor,
                            ),
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(250),
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(250),
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(250),
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(250),
                )
            },
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToChat = {
                        navController.navigate(Screen.Chat.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSettings = { showNotificationSheet = true },
                )
            }
            composable(Screen.Chat.route) { ChatScreen() }
            composable(Screen.Pantry.route) { PantryScreen() }
            composable(Screen.Profile.route) { ClinicalProfileScreen() }
        }
    }

    // ── Modal de notificaciones (igual que iOS .sheet) ──────────────────────
    if (showNotificationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationSheet = false },
            sheetState = sheetState,
            containerColor = BackgroundColor,
        ) {
            NotificationSettingsContent(onDismiss = { showNotificationSheet = false })
        }
    }
}
