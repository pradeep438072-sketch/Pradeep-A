package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.DosageWarningBanner
import com.example.ui.components.MediVoiceTopBar
import com.example.ui.components.ReminderPopUpDialog
import com.example.ui.screens.*
import com.example.ui.theme.MediVoiceTheme
import com.example.ui.viewmodel.MediVoiceViewModel

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    object Login : Screen("login", "Login")
    object Register : Screen("register", "Register")
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Medicines : Screen("medicines", "Medicines", Icons.Default.Medication)
    object Schedule : Screen("schedule", "Schedule", Icons.Default.Schedule)
    object History : Screen("history", "History", Icons.Default.History)
    object Voice : Screen("voice", "Voice Assistant", Icons.Default.Mic)
    object AddMedicine : Screen("add_medicine?id={id}", "Add Medicine")
    object MedicineDetail : Screen("medicine_detail/{id}", "Medicine Details")
    object Profile : Screen("profile", "Profile & Settings")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MediVoiceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MediVoiceTheme {
                // Request dangerous permissions on launch
                val permissionsToRequest = mutableListOf<String>().apply {
                    add(Manifest.permission.RECORD_AUDIO)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { /* Permissions handled gracefully */ }

                LaunchedEffect(Unit) {
                    val notGranted = permissionsToRequest.filter {
                        ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                    }
                    if (notGranted.isNotEmpty()) {
                        permissionLauncher.launch(notGranted.toTypedArray())
                    }
                }

                MediVoiceAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MediVoiceAppRoot(viewModel: MediVoiceViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val activeAlert by viewModel.activeReminderAlert.collectAsStateWithLifecycle()
    val dosageWarning by viewModel.dosageWarning.collectAsStateWithLifecycle()

    val bottomNavItems = listOf(
        Screen.Dashboard,
        Screen.Medicines,
        Screen.Schedule,
        Screen.History,
        Screen.Voice
    )

    val isAuthScreen = currentRoute == Screen.Login.route || currentRoute == Screen.Register.route

    val currentTitle = when {
        currentRoute == Screen.Dashboard.route -> "MediVoice AI"
        currentRoute == Screen.Medicines.route -> "My Medicines"
        currentRoute == Screen.Schedule.route -> "Today's Schedule"
        currentRoute == Screen.History.route -> "Medicine History"
        currentRoute == Screen.Voice.route -> "Voice Assistant"
        currentRoute == Screen.Profile.route -> "Profile & Settings"
        currentRoute?.startsWith("add_medicine") == true -> "Medicine Manager"
        currentRoute?.startsWith("medicine_detail") == true -> "Medicine Overview"
        else -> "MediVoice AI"
    }

    val showBackNav = currentRoute == Screen.Profile.route ||
            currentRoute?.startsWith("add_medicine") == true ||
            currentRoute?.startsWith("medicine_detail") == true

    Scaffold(
        topBar = {
            if (!isAuthScreen) {
                MediVoiceTopBar(
                    title = currentTitle,
                    userName = currentUser?.name,
                    onVoiceClick = {
                        navController.navigate(Screen.Voice.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onProfileClick = {
                        navController.navigate(Screen.Profile.route)
                    },
                    showBackButton = showBackNav,
                    onBackClick = { navController.popBackStack() }
                )
            }
        },
        bottomBar = {
            if (!isAuthScreen) {
                NavigationBar(
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                screen.icon?.let {
                                    Icon(imageVector = it, contentDescription = screen.title)
                                }
                            },
                            label = { Text(screen.title) },
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = if (currentUser != null) Screen.Dashboard.route else Screen.Login.route
            ) {
                composable(Screen.Login.route) {
                    LoginScreen(
                        viewModel = viewModel,
                        onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                        onLoginSuccess = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Register.route) {
                    RegisterScreen(
                        viewModel = viewModel,
                        onNavigateToLogin = { navController.popBackStack() },
                        onRegisterSuccess = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Register.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToAddMedicine = { navController.navigate("add_medicine?id=0") },
                        onNavigateToSchedule = { navController.navigate(Screen.Schedule.route) },
                        onNavigateToVoice = { navController.navigate(Screen.Voice.route) },
                        onNavigateToMedicines = { navController.navigate(Screen.Medicines.route) }
                    )
                }

                composable(Screen.Medicines.route) {
                    MyMedicinesScreen(
                        viewModel = viewModel,
                        onNavigateToAddMedicine = { navController.navigate("add_medicine?id=0") },
                        onNavigateToEditMedicine = { id -> navController.navigate("add_medicine?id=$id") },
                        onNavigateToMedicineDetail = { id -> navController.navigate("medicine_detail/$id") }
                    )
                }

                composable(
                    route = Screen.AddMedicine.route,
                    arguments = listOf(navArgument("id") {
                        type = NavType.LongType
                        defaultValue = 0L
                    })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getLong("id") ?: 0L
                    AddMedicineScreen(
                        viewModel = viewModel,
                        medicineId = id,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.MedicineDetail.route,
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getLong("id") ?: 0L
                    MedicineDetailScreen(
                        medicineId = id,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToEdit = { editId -> navController.navigate("add_medicine?id=$editId") }
                    )
                }

                composable(Screen.Schedule.route) {
                    TodayScheduleScreen(
                        viewModel = viewModel,
                        onNavigateToAddMedicine = { navController.navigate("add_medicine?id=0") }
                    )
                }

                composable(Screen.History.route) {
                    MedicineHistoryScreen(viewModel = viewModel)
                }

                composable(Screen.Voice.route) {
                    VoiceAssistantScreen(viewModel = viewModel)
                }

                composable(Screen.Profile.route) {
                    ProfileSettingsScreen(
                        viewModel = viewModel,
                        onLogout = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }

            // Global Pop-Up Alert Dialog when a medicine reminder fires
            if (activeAlert != null) {
                ReminderPopUpDialog(
                    schedule = activeAlert!!,
                    onTaken = {
                        viewModel.markDoseTaken(activeAlert!!.id)
                        viewModel.dismissReminderAlert()
                    },
                    onSkip = {
                        viewModel.markDoseSkipped(activeAlert!!.id)
                        viewModel.dismissReminderAlert()
                    },
                    onSnooze = { mins ->
                        viewModel.snoozeDose(activeAlert!!.id, mins)
                        viewModel.dismissReminderAlert()
                    },
                    onDismiss = { viewModel.dismissReminderAlert() }
                )
            }

            // Global Dosage Limit Warning Dialog
            if (dosageWarning != null) {
                DosageWarningBanner(
                    message = dosageWarning!!,
                    onDismiss = { viewModel.clearDosageWarning() }
                )
            }
        }
    }
}
