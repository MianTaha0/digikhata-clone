package com.digikhata.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.digikhata.ui.book.BookSettingsScreen
import com.digikhata.ui.book.CreateBookScreen
import com.digikhata.ui.cash.CashEntryDetailScreen
import com.digikhata.ui.cash.CashRegisterScreen
import com.digikhata.ui.detail.ClientDetailScreen
import com.digikhata.ui.drawer.DrawerContent
import com.digikhata.ui.home.HomeScreen
import com.digikhata.ui.notifications.NotificationsScreen
import com.digikhata.ui.notifications.NotificationsViewModel
import com.digikhata.ui.placeholder.ComingSoonScreen
import com.digikhata.ui.search.SearchScreen
import com.digikhata.ui.supplier.SupplierListScreen
import com.digikhata.ui.theme.DigiRed
import com.digikhata.ui.components.digiTopBarColors
import kotlinx.coroutines.launch

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigiApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val bootstrapVm: BootstrapViewModel = hiltViewModel()
    val needsBook by bootstrapVm.needsBook.collectAsState()

    LaunchedEffect(needsBook) {
        if (needsBook == true) {
            navController.navigate(Routes.CREATE_BOOK) {
                popUpTo(Routes.HOME) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    val tabs = listOf(
        BottomTab(Routes.HOME, "Home", Icons.Default.Home),
        BottomTab(Routes.CASH, "Cash", Icons.Default.AccountBalanceWallet),
        BottomTab(Routes.comingSoon("Stock"), "Stock", Icons.Default.Inventory2),
        BottomTab(Routes.comingSoon("Bills"), "Bills", Icons.Default.Receipt),
        BottomTab(Routes.comingSoon("Expense"), "Expense", Icons.Default.Description),
    )

    val showChrome = currentRoute == Routes.HOME ||
            currentRoute == Routes.SUPPLIER ||
            currentRoute == Routes.CASH ||
            currentRoute?.startsWith("comingSoon/") == true

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onClose = { scope.launch { drawerState.close() } },
                onNavigateCreateBook = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Routes.CREATE_BOOK)
                },
                onOpenBookSettings = { bookId ->
                    scope.launch { drawerState.close() }
                    navController.navigate(Routes.bookSettings(bookId))
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                if (showChrome) {
                    val notifVm: NotificationsViewModel = hiltViewModel()
                    val unseen by notifVm.unseenCount.collectAsState()
                    TopAppBar(
                        title = { Text("DigiKhata") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = { navController.navigate(Routes.SEARCH) }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = { navController.navigate(Routes.NOTIFICATIONS) }) {
                                BadgedBox(badge = {
                                    if (unseen > 0) Badge { Text(unseen.toString()) }
                                }) {
                                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                                }
                            }
                        },
                        colors = digiTopBarColors()
                    )
                }
            },
            bottomBar = {
                if (showChrome) {
                    NavigationBar(containerColor = Color.White) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route ||
                                (tab.route == Routes.HOME && currentRoute == Routes.HOME)
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != tab.route) {
                                        navController.navigate(tab.route) {
                                            popUpTo(Routes.HOME) { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DigiRed,
                                    selectedTextColor = DigiRed,
                                    indicatorColor = DigiRed.copy(alpha = 0.12f)
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(padding)
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onOpenClient = { navController.navigate(Routes.clientDetail(it)) },
                        onOpenSupplier = { navController.navigate(Routes.SUPPLIER) }
                    )
                }
                composable(Routes.SUPPLIER) {
                    SupplierListScreen(
                        onBack = { navController.popBackStack() },
                        onOpenClient = { navController.navigate(Routes.clientDetail(it)) }
                    )
                }
                composable(Routes.SEARCH) {
                    SearchScreen(
                        onBack = { navController.popBackStack() },
                        onOpenClient = { navController.navigate(Routes.clientDetail(it)) }
                    )
                }
                composable(Routes.NOTIFICATIONS) {
                    NotificationsScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.CREATE_BOOK) {
                    CreateBookScreen(onDone = {
                        navController.popBackStack()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    })
                }
                composable(
                    Routes.BOOK_SETTINGS,
                    arguments = listOf(navArgument("bookId") { type = NavType.LongType })
                ) {
                    BookSettingsScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    Routes.CLIENT_DETAIL,
                    arguments = listOf(navArgument("clientId") { type = NavType.LongType })
                ) {
                    ClientDetailScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    Routes.COMING_SOON,
                    arguments = listOf(navArgument("label") { type = NavType.StringType })
                ) { entry ->
                    val label = entry.arguments?.getString("label") ?: "Coming Soon"
                    ComingSoonScreen(label = label)
                }
                composable(Routes.CASH) {
                    CashRegisterScreen(navController = navController)
                }
                composable(
                    Routes.CASH_ENTRY_DETAIL_PATTERN,
                    arguments = listOf(navArgument("entryId") { type = NavType.LongType })
                ) {
                    CashEntryDetailScreen(navController = navController)
                }
            }
        }
    }
}
