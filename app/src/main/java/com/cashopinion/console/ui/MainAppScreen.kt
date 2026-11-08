package com.cashopinion.console.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cashopinion.console.R
import com.cashopinion.console.core.AppContainer
import com.cashopinion.console.ui.screens.analytics.AnalyticsScreen
import com.cashopinion.console.ui.screens.config.ConfigScreen
import com.cashopinion.console.ui.screens.home.HomeScreen
import com.cashopinion.console.ui.screens.more.MoreScreen
import com.cashopinion.console.ui.screens.profile.ProfileScreen
import com.cashopinion.console.ui.screens.reviews.ReviewQueueScreen
import com.cashopinion.console.ui.screens.season.SeasonScreen
import com.cashopinion.console.ui.screens.settings.SettingsScreen
import com.cashopinion.console.ui.screens.tasks.TasksScreen
import com.cashopinion.console.ui.screens.users.UsersScreen
import com.cashopinion.console.ui.screens.withdrawals.WithdrawalsScreen
import com.cashopinion.console.ui.theme.CashTheme
import kotlinx.coroutines.launch

enum class ConsoleTab(val label: String, val iconRes: Int) {
    Home("Home", R.drawable.ic_home),
    Users("Users", R.drawable.ic_user),
    Withdrawals("Payouts", R.drawable.ic_wallet),
    Tasks("Tasks", R.drawable.ic_document),
    More("More", R.drawable.ic_menu)
}

enum class SubScreen {
    None, Reviews, Season, Analytics, Config, Profile, Settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    container: AppContainer,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(ConsoleTab.Home) }
    var activeSubScreen by remember { mutableStateOf(SubScreen.None) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = colors.surface,
                drawerContentColor = colors.textPrimary
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Cash Opinion Console",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.primary,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Text(
                    text = "v2.0.0 Super Admin",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = colors.border)

                NavigationDrawerItem(
                    label = { Text("Dashboard") },
                    icon = { Icon(painterResource(id = R.drawable.ic_home), contentDescription = null) },
                    selected = selectedTab == ConsoleTab.Home && activeSubScreen == SubScreen.None,
                    onClick = {
                        selectedTab = ConsoleTab.Home
                        activeSubScreen = SubScreen.None
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Withdrawals & Payouts") },
                    icon = { Icon(painterResource(id = R.drawable.ic_wallet), contentDescription = null) },
                    selected = selectedTab == ConsoleTab.Withdrawals && activeSubScreen == SubScreen.None,
                    onClick = {
                        selectedTab = ConsoleTab.Withdrawals
                        activeSubScreen = SubScreen.None
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("User Directory") },
                    icon = { Icon(painterResource(id = R.drawable.ic_user), contentDescription = null) },
                    selected = selectedTab == ConsoleTab.Users && activeSubScreen == SubScreen.None,
                    onClick = {
                        selectedTab = ConsoleTab.Users
                        activeSubScreen = SubScreen.None
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Task Management") },
                    icon = { Icon(painterResource(id = R.drawable.ic_document), contentDescription = null) },
                    selected = selectedTab == ConsoleTab.Tasks && activeSubScreen == SubScreen.None,
                    onClick = {
                        selectedTab = ConsoleTab.Tasks
                        activeSubScreen = SubScreen.None
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Review Queue") },
                    icon = { Icon(painterResource(id = R.drawable.ic_check_circle), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.Reviews,
                    onClick = {
                        activeSubScreen = SubScreen.Reviews
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Season System") },
                    icon = { Icon(painterResource(id = R.drawable.ic_calendar_star), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.Season,
                    onClick = {
                        activeSubScreen = SubScreen.Season
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Analytics") },
                    icon = { Icon(painterResource(id = R.drawable.ic_earnings_history), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.Analytics,
                    onClick = {
                        activeSubScreen = SubScreen.Analytics
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("App Config") },
                    icon = { Icon(painterResource(id = R.drawable.ic_settings), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.Config,
                    onClick = {
                        activeSubScreen = SubScreen.Config
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Admin Profile") },
                    icon = { Icon(painterResource(id = R.drawable.ic_user), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.Profile,
                    onClick = {
                        activeSubScreen = SubScreen.Profile
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    icon = { Icon(painterResource(id = R.drawable.ic_shield), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.Settings,
                    onClick = {
                        activeSubScreen = SubScreen.Settings
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (activeSubScreen != SubScreen.None) {
                                activeSubScreen.name
                            } else {
                                selectedTab.label
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                    },
                    navigationIcon = {
                        if (activeSubScreen != SubScreen.None) {
                            IconButton(onClick = { activeSubScreen = SubScreen.None }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_back),
                                    contentDescription = "Back",
                                    tint = colors.textPrimary
                                )
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_menu),
                                    contentDescription = "Menu",
                                    tint = colors.textPrimary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = colors.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = colors.surface,
                    contentColor = colors.primary
                ) {
                    ConsoleTab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab && activeSubScreen == SubScreen.None,
                            onClick = {
                                selectedTab = tab
                                activeSubScreen = SubScreen.None
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(id = tab.iconRes),
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF140E00),
                                indicatorColor = colors.primary,
                                unselectedIconColor = colors.textSecondary,
                                unselectedTextColor = colors.textSecondary
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(colors.background)
            ) {
                Crossfade(targetState = activeSubScreen, label = "screenTransition") { subScreen ->
                    when (subScreen) {
                        SubScreen.Reviews -> ReviewQueueScreen(consoleRepo = container.consoleRepo)
                        SubScreen.Season -> SeasonScreen(consoleRepo = container.consoleRepo)
                        SubScreen.Analytics -> AnalyticsScreen(consoleRepo = container.consoleRepo)
                        SubScreen.Config -> ConfigScreen(consoleRepo = container.consoleRepo)
                        SubScreen.Profile -> ProfileScreen(
                            consoleRepo = container.consoleRepo,
                            authRepo = container.authRepo,
                            sessionManager = container.sessionManager
                        )
                        SubScreen.Settings -> SettingsScreen(
                            sessionManager = container.sessionManager,
                            serverClient = container.serverClient,
                            authRepo = container.authRepo,
                            onSignOut = onSignOut
                        )
                        SubScreen.None -> {
                            Crossfade(targetState = selectedTab, label = "tabTransition") { tab ->
                                when (tab) {
                                    ConsoleTab.Home -> HomeScreen(
                                        consoleRepo = container.consoleRepo,
                                        onNavigateToWithdrawals = { selectedTab = ConsoleTab.Withdrawals },
                                        onNavigateToReviews = { activeSubScreen = SubScreen.Reviews },
                                        onNavigateToTickets = { selectedTab = ConsoleTab.More }
                                    )
                                    ConsoleTab.Users -> UsersScreen(consoleRepo = container.consoleRepo)
                                    ConsoleTab.Withdrawals -> WithdrawalsScreen(consoleRepo = container.consoleRepo)
                                    ConsoleTab.Tasks -> TasksScreen(consoleRepo = container.consoleRepo)
                                    ConsoleTab.More -> MoreScreen(
                                        onNavigateToReviews = { activeSubScreen = SubScreen.Reviews },
                                        onNavigateToSeason = { activeSubScreen = SubScreen.Season },
                                        onNavigateToAnalytics = { activeSubScreen = SubScreen.Analytics },
                                        onNavigateToConfig = { activeSubScreen = SubScreen.Config },
                                        onNavigateToProfile = { activeSubScreen = SubScreen.Profile },
                                        onNavigateToSettings = { activeSubScreen = SubScreen.Settings }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
