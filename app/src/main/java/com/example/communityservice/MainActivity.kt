package com.example.communityservice

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.communityservice.data.IssueCategory
import com.example.communityservice.ui.screens.*
import com.example.communityservice.ui.viewmodel.IssueViewModel
import kotlinx.coroutines.launch

// Color variables
val Emerald800 = Color(0xFF065F46)
val Emerald950 = Color(0xFF022C22)
val Emerald900 = Color(0xFF064E3B)
val Amber500 = Color(0xFFF59E0B)
val Slate500 = Color(0xFF64748B)
val Slate700 = Color(0xFF334155)
val Slate900 = Color(0xFF0F172A)

// Theme Wrapper
@Composable
fun CommunityServiceTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = Emerald800,
        background = Color(0xFFF8FAFC),
        surface = Color.White
    )
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CommunityServiceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppContent()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent() {
    val viewModel: IssueViewModel = viewModel()
    val navController = rememberNavController()
    val currentUser by viewModel.currentUser.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    if (currentUser == null) {
        // 1. NO NavHost here! Just show Landing directly
        LandingScreen(
            viewModel = viewModel,
            onLoginSuccess = {
                // 2. DO NOTHING HERE. The app will auto-switch to home
                // when currentUser becomes not-null
            }
        )
    } else {
        // Log out confirmation
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Log Out", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to log out?") },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    }) {
                        Text("Log Out", color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel", color = Slate700)
                    }
                }
            )
        }

        // App Layout with Drawer and Bottom Navigation
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Color.White,
                    drawerContentColor = Slate900,
                    modifier = Modifier.width(300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Emerald800, RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Community Service", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                Text("-Issue Reporter", color = Amber500, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(currentUser?.name.orEmpty(), color = Color.White, fontSize = 13.sp)
                                Text(currentUser?.email.orEmpty(), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            }
                        }
                        DrawerLinkItem(icon = Icons.Default.Home, label = "Resident Portal", isSelected = true) {
                            scope.launch { drawerState.close() }
                            navController.navigate("home")
                        }
                        DrawerLinkItem(icon = Icons.Default.History, label = "My Submitted Tickets", isSelected = false) {
                            scope.launch { drawerState.close() }
                            navController.navigate("my_reports")
                        }
                        DrawerLinkItem(icon = Icons.Default.Campaign, label = "Ward Alerts & Notices", isSelected = false) {
                            scope.launch { drawerState.close() }
                            navController.navigate("alerts")
                        }
                        DrawerLinkItem(icon = Icons.Default.Person, label = "My Profile Settings", isSelected = false) {
                            scope.launch { drawerState.close() }
                            navController.navigate("profile")
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Log Out
                        DrawerLinkItem(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            label = "Log Out",
                            isSelected = false
                        ) {
                            scope.launch { drawerState.close() }
                            showLogoutDialog = true
                        }

                        Text(
                            text = "Copyright © 2026 Community Service - Issue Reporter",
                            fontSize = 10.sp,
                            color = Slate500,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }
        ) {
            Scaffold(
                bottomBar = {
                    val currentBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = currentBackStackEntry?.destination?.route
                    if (currentRoute == "home" || currentRoute == "my_reports" || currentRoute == "alerts" || currentRoute == "profile") {
                        ResidentBottomNavigationBar(navController, currentRoute)
                    }
                }
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.padding(paddingValues)
                ) {
                    composable("home") {
                        HomeScreen(
                            viewModel = viewModel,
                            onReportIssueClick = { category ->
                                // "none" means the user has not chosen a category yet
                                navController.navigate("create_report/${category?.name ?: "none"}")
                            },
                            onViewReportsClick = { navController.navigate("my_reports") },
                            onSelectReportClick = { reportId -> navController.navigate("details/$reportId") },
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onOpenAlerts = { navController.navigate("alerts") }
                        )
                    }
                    composable("my_reports") {
                        MyReportsScreen(
                            viewModel = viewModel,
                            onSelectReport = { id -> navController.navigate("details/$id") },
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }
                    composable("alerts") {
                        AlertsScreen(
                            viewModel = viewModel,
                            onReportClick = { id -> navController.navigate("details/$id") },
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }
                    composable("profile") {
                        ProfileScreen(
                            viewModel = viewModel,
                            onLogout = { viewModel.logout() },
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }
                    composable(
                        route = "create_report/{categoryName}",
                        arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
                    ) { backStackEntry ->
                        // "none" (or anything unknown) becomes null, so no category is preselected
                        val categoryName = backStackEntry.arguments?.getString("categoryName")
                        val category = IssueCategory.values().firstOrNull { it.name == categoryName }
                        ReportIssueScreen(
                            initialCategory = category,
                            viewModel = viewModel,
                            onSuccess = { reportId -> navController.navigate("details/$reportId") { popUpTo("home") { saveState = true } } },
                            onCancel = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "details/{reportId}",
                        arguments = listOf(navArgument("reportId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val reportId = backStackEntry.arguments?.getString("reportId") ?: "ISS-1042"
                        ReportDetailScreen(reportId = reportId, viewModel = viewModel, onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerLinkItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) Color(0xFFECFDF5) else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = if (isSelected) Emerald800 else Slate700, modifier = Modifier.size(20.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Emerald900 else Slate900)
    }
}

@Composable
fun ResidentBottomNavigationBar(
    navController: NavHostController,
    currentRoute: String?
) {
    NavigationBar(
        containerColor = Color.White,
        contentColor = Emerald800
    ) {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = { if (currentRoute != "home") navController.navigate("home") },
            icon = { Icon(Icons.Default.Home, "Home") },
            label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Emerald900,
                selectedTextColor = Emerald900,
                indicatorColor = Color(0xFFDCFCE7),
                unselectedIconColor = Slate500,
                unselectedTextColor = Slate500
            )
        )
        NavigationBarItem(
            selected = currentRoute == "my_reports",
            onClick = { if (currentRoute != "my_reports") navController.navigate("my_reports") },
            icon = { Icon(Icons.Default.History, "Reports") },
            label = { Text("My Reports", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Emerald900,
                selectedTextColor = Emerald900,
                indicatorColor = Color(0xFFDCFCE7),
                unselectedIconColor = Slate500,
                unselectedTextColor = Slate500
            )
        )
        NavigationBarItem(
            selected = currentRoute == "alerts",
            onClick = { if (currentRoute != "alerts") navController.navigate("alerts") },
            icon = { Icon(Icons.Default.Campaign, "Alerts") },
            label = { Text("Alerts", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Emerald900,
                selectedTextColor = Emerald900,
                indicatorColor = Color(0xFFDCFCE7),
                unselectedIconColor = Slate500,
                unselectedTextColor = Slate500
            )
        )
        NavigationBarItem(
            selected = currentRoute == "profile",
            onClick = { if (currentRoute != "profile") navController.navigate("profile") },
            icon = { Icon(Icons.Default.Person, "Profile") },
            label = { Text("Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Emerald900,
                selectedTextColor = Emerald900,
                indicatorColor = Color(0xFFDCFCE7),
                unselectedIconColor = Slate500,
                unselectedTextColor = Slate500
            )
        )
    }
}