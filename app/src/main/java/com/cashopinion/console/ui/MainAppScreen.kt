package com.cashopinion.console.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cashopinion.console.R
import com.cashopinion.console.core.AppContainer
import com.cashopinion.console.ui.components.SoundFx
import com.cashopinion.console.ui.screens.alerts.LiveAlertsScreen
import com.cashopinion.console.ui.screens.analytics.AnalyticsScreen
import com.cashopinion.console.ui.screens.backup.BackupExportScreen
import com.cashopinion.console.ui.screens.config.ConfigScreen
import com.cashopinion.console.ui.screens.gamification.GamificationScreen
import com.cashopinion.console.ui.screens.home.HomeScreen
import com.cashopinion.console.ui.screens.more.MoreScreen
import com.cashopinion.console.ui.screens.notifications.NotificationsScreen
import com.cashopinion.console.ui.screens.offers.OffersScreen
import com.cashopinion.console.ui.screens.pages.PagesFaqScreen
import com.cashopinion.console.ui.screens.payoutmethods.PayoutMethodsScreen
import com.cashopinion.console.ui.screens.profile.ProfileScreen
import com.cashopinion.console.ui.screens.promo.PromoReferralsScreen
import com.cashopinion.console.ui.screens.redeem.RedeemCodesScreen
import com.cashopinion.console.ui.screens.reports.ReportsScreen
import com.cashopinion.console.ui.screens.reviews.ReviewQueueScreen
import com.cashopinion.console.ui.screens.season.SeasonScreen
import com.cashopinion.console.ui.screens.settings.SettingsScreen
import com.cashopinion.console.ui.screens.support.SupportTicketsScreen
import com.cashopinion.console.ui.screens.tasks.TasksScreen
import com.cashopinion.console.ui.screens.transactions.TransactionsScreen
import com.cashopinion.console.ui.screens.users.UsersScreen
import com.cashopinion.console.ui.screens.withdrawals.WithdrawalsScreen
import com.cashopinion.console.ui.theme.CashColors
import com.cashopinion.console.ui.theme.CashTheme
import com.cashopinion.console.ui.theme.Shapes
import kotlinx.coroutines.launch

enum class ConsoleTab(val label: String, val iconRes: Int) {
    Home("Home", R.drawable.ic_home),
    Users("Users", R.drawable.ic_user),
    Withdrawals("Payouts", R.drawable.ic_wallet),
    Tasks("Tasks", R.drawable.ic_document),
    More("More", R.drawable.ic_menu)
}

enum class SubScreen(val title: String) {
    None(""),
    Reviews("Review Queue"),
    Reports("Problem Reports"),
    Offers("Sponsored Offers"),
    RedeemCodes("Redeem Codes"),
    Transactions("Global Ledger"),
    PayoutMethods("Payout Methods"),
    Season("Season System"),
    Analytics("Analytics & Reports"),
    Config("App Configuration"),
    Profile("Admin Profile"),
    Settings("Settings & Security"),
    Support("Customer Support"),
    Notifications("Notifications & Banners"),
    Promo("Promo Codes & Referrals"),
    Gamification("Gamification Studio"),
    PagesFaq("Content Pages & FAQs"),
    Alerts("Live System Alerts"),
    Backup("Database Backup & Export")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    container: AppContainer,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CashTheme.colors
    val context = LocalContext.current
    val activity = context as? Activity
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(ConsoleTab.Home) }
    var activeSubScreen by remember { mutableStateOf(SubScreen.None) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Section 2.1 & 2.2: Comprehensive Back Navigation & Exit Confirmation
    BackHandler {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (activeSubScreen != SubScreen.None) {
            SoundFx.playClick()
            activeSubScreen = SubScreen.None
        } else if (selectedTab != ConsoleTab.Home) {
            SoundFx.playClick()
            selectedTab = ConsoleTab.Home
        } else {
            showExitDialog = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = "Exit Cash Opinion Console?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Your session will stay signed in. Real-time updates will stop until you open the app again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        SoundFx.playClick()
                        showExitDialog = false
                        activity?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CashColors.GoldPrimary)
                ) {
                    Text("Exit", color = Color(0xFF140E00), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        SoundFx.playClick()
                        showExitDialog = false
                    }
                ) {
                    Text("Stay", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = Shapes.Dialog
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = colors.surface,
                drawerContentColor = colors.textPrimary
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Cash Opinion Console",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.primary,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Text(
                    text = "Full-Stack Enterprise Super Admin v2.0.1",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = colors.border)

                NavigationDrawerItem(
                    label = { Text("Dashboard") },
                    icon = { Icon(painterResource(id = R.drawable.ic_home), contentDescription = null) },
                    selected = selectedTab == ConsoleTab.Home && activeSubScreen == SubScreen.None,
                    onClick = {
                        SoundFx.playClick()
                        selectedTab = ConsoleTab.Home
                        activeSubScreen = SubScreen.None
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Payouts & Withdrawals") },
                    icon = { Icon(painterResource(id = R.drawable.ic_wallet), contentDescription = null) },
                    selected = selectedTab == ConsoleTab.Withdrawals && activeSubScreen == SubScreen.None,
                    onClick = {
                        SoundFx.playClick()
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
                        SoundFx.playClick()
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
                        SoundFx.playClick()
                        selectedTab = ConsoleTab.Tasks
                        activeSubScreen = SubScreen.None
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Proof Review Queue") },
                    icon = { Icon(painterResource(id = R.drawable.ic_check_circle), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.Reviews,
                    onClick = {
                        SoundFx.playClick()
                        activeSubScreen = SubScreen.Reviews
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Problem Reports") },
                    icon = { Icon(painterResource(id = R.drawable.ic_error), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.Reports,
                    onClick = {
                        SoundFx.playClick()
                        activeSubScreen = SubScreen.Reports
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Customer Support & Tickets") },
                    icon = { Icon(painterResource(id = R.drawable.ic_headset), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.Support,
                    onClick = {
                        SoundFx.playClick()
                        activeSubScreen = SubScreen.Support
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Sponsored Offers") },
                    icon = { Icon(painterResource(id = R.drawable.ic_sponsored_offers), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.Offers,
                    onClick = {
                        SoundFx.playClick()
                        activeSubScreen = SubScreen.Offers
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Redeem Codes Studio") },
                    icon = { Icon(painterResource(id = R.drawable.ic_gift), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.RedeemCodes,
                    onClick = {
                        SoundFx.playClick()
                        activeSubScreen = SubScreen.RedeemCodes
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Global Transaction Ledger") },
                    icon = { Icon(painterResource(id = R.drawable.ic_transaction_history), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.Transactions,
                    onClick = {
                        SoundFx.playClick()
                        activeSubScreen = SubScreen.Transactions
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Saved Payout Methods") },
                    icon = { Icon(painterResource(id = R.drawable.ic_bank), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.PayoutMethods,
                    onClick = {
                        SoundFx.playClick()
                        activeSubScreen = SubScreen.PayoutMethods
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Notifications & Banners") },
                    icon = { Icon(painterResource(id = R.drawable.ic_bell), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.Notifications,
                    onClick = {
                        SoundFx.playClick()
                        activeSubScreen = SubScreen.Notifications
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Promo Codes & Referrals") },
                    icon = { Icon(painterResource(id = R.drawable.ic_tag), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.Promo,
                    onClick = {
                        SoundFx.playClick()
                        activeSubScreen = SubScreen.Promo
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Gamification (Leaderboard, Spin, Scratch)") },
                    icon = { Icon(painterResource(id = R.drawable.ic_trophy), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.Gamification,
                    onClick = {
                        SoundFx.playClick()
                        activeSubScreen = SubScreen.Gamification
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Season System") },
                    icon = { Icon(painterResource(id = R.drawable.ic_calendar_star), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.Season,
                    onClick = {
                        SoundFx.playClick()
                        activeSubScreen = SubScreen.Season
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Database Backup & Export") },
                    icon = { Icon(painterResource(id = R.drawable.ic_document), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.Backup,
                    onClick = {
                        SoundFx.playClick()
                        activeSubScreen = SubScreen.Backup
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Analytics & Performance") },
                    icon = { Icon(painterResource(id = R.drawable.ic_earnings_history), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.Analytics,
                    onClick = {
                        SoundFx.playClick()
                        activeSubScreen = SubScreen.Analytics
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Pages & FAQs") },
                    icon = { Icon(painterResource(id = R.drawable.ic_info), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.PagesFaq,
                    onClick = {
                        SoundFx.playClick()
                        activeSubScreen = SubScreen.PagesFaq
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("App Configuration") },
                    icon = { Icon(painterResource(id = R.drawable.ic_settings), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.Config,
                    onClick = {
                        SoundFx.playClick()
                        activeSubScreen = SubScreen.Config
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Admin Profile") },
                    icon = { Icon(painterResource(id = R.drawable.ic_user), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.Profile,
                    onClick = {
                        SoundFx.playClick()
                        activeSubScreen = SubScreen.Profile
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    icon = { Icon(painterResource(id = R.drawable.ic_shield), contentDescription = null) },
                    selected = activeSubScreen == SubScreen.Settings,
                    onClick = {
                        SoundFx.playClick()
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
                                activeSubScreen.title
                            } else {
                                selectedTab.label
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                    },
                    navigationIcon = {
                        if (activeSubScreen != SubScreen.None) {
                            IconButton(onClick = {
                                SoundFx.playClick()
                                activeSubScreen = SubScreen.None
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_back),
                                    contentDescription = "Back",
                                    tint = colors.textPrimary
                                )
                            }
                        } else {
                            IconButton(onClick = {
                                SoundFx.playClick()
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_menu),
                                    contentDescription = "Menu",
                                    tint = colors.textPrimary
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            SoundFx.playClick()
                            activeSubScreen = SubScreen.Alerts
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_warning),
                                contentDescription = "Alerts",
                                tint = colors.warning
                            )
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
                                SoundFx.playClick()
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
                        SubScreen.Reports -> ReportsScreen(consoleRepo = container.consoleRepo)
                        SubScreen.Offers -> OffersScreen(consoleRepo = container.consoleRepo)
                        SubScreen.RedeemCodes -> RedeemCodesScreen(consoleRepo = container.consoleRepo)
                        SubScreen.Transactions -> TransactionsScreen(consoleRepo = container.consoleRepo)
                        SubScreen.PayoutMethods -> PayoutMethodsScreen(consoleRepo = container.consoleRepo)
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
                        SubScreen.Support -> SupportTicketsScreen(consoleRepo = container.consoleRepo)
                        SubScreen.Notifications -> NotificationsScreen(consoleRepo = container.consoleRepo)
                        SubScreen.Promo -> PromoReferralsScreen(consoleRepo = container.consoleRepo)
                        SubScreen.Gamification -> GamificationScreen(consoleRepo = container.consoleRepo)
                        SubScreen.PagesFaq -> PagesFaqScreen(consoleRepo = container.consoleRepo)
                        SubScreen.Alerts -> LiveAlertsScreen(consoleRepo = container.consoleRepo)
                        SubScreen.Backup -> BackupExportScreen(consoleRepo = container.consoleRepo)
                        SubScreen.None -> {
                            Crossfade(targetState = selectedTab, label = "tabTransition") { tab ->
                                when (tab) {
                                    ConsoleTab.Home -> HomeScreen(
                                        consoleRepo = container.consoleRepo,
                                        onNavigateToWithdrawals = {
                                            SoundFx.playClick()
                                            selectedTab = ConsoleTab.Withdrawals
                                        },
                                        onNavigateToReviews = {
                                            SoundFx.playClick()
                                            activeSubScreen = SubScreen.Reviews
                                        },
                                        onNavigateToTickets = {
                                            SoundFx.playClick()
                                            activeSubScreen = SubScreen.Support
                                        }
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
                                        onNavigateToSettings = { activeSubScreen = SubScreen.Settings },
                                        onNavigateToSupport = { activeSubScreen = SubScreen.Support },
                                        onNavigateToNotifications = { activeSubScreen = SubScreen.Notifications },
                                        onNavigateToPromo = { activeSubScreen = SubScreen.Promo },
                                        onNavigateToGamification = { activeSubScreen = SubScreen.Gamification },
                                        onNavigateToPagesFaq = { activeSubScreen = SubScreen.PagesFaq },
                                        onNavigateToAlerts = { activeSubScreen = SubScreen.Alerts },
                                        onNavigateToBackup = { activeSubScreen = SubScreen.Backup },
                                        onNavigateToOffers = { activeSubScreen = SubScreen.Offers },
                                        onNavigateToRedeemCodes = { activeSubScreen = SubScreen.RedeemCodes },
                                        onNavigateToTransactions = { activeSubScreen = SubScreen.Transactions },
                                        onNavigateToPayoutMethods = { activeSubScreen = SubScreen.PayoutMethods },
                                        onNavigateToReports = { activeSubScreen = SubScreen.Reports }
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
