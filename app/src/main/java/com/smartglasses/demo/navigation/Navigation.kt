package com.smartglasses.demo.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartglasses.demo.data.local.AppDatabase
import com.smartglasses.demo.data.repository.AuthRepository
import com.smartglasses.demo.data.repository.PatientRepository
import com.smartglasses.demo.data.session.SessionManager
import com.smartglasses.demo.ui.screens.DashboardScreen
import com.smartglasses.demo.ui.screens.LoginScreen
import com.smartglasses.demo.ui.screens.ScanPatientScreen
import com.smartglasses.demo.viewmodel.AuthViewModel
import com.smartglasses.demo.viewmodel.PatientViewModel
import com.smartglasses.demo.viewmodel.ScanViewModel

/** Route constants */
object Routes {
    const val LOGIN = "login"
    const val SCAN = "scan/{username}"
    const val DASHBOARD = "dashboard/{username}"
    fun scan(username: String) = "scan/$username"
    fun dashboard(username: String) = "dashboard/$username"
}

/**
 * Single NavHost wiring the routes of the app.
 * Implements proper dependency injection for ViewModels with their repositories.
 */
@Composable
fun SmartGlassesNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as Application

    // Initialize dependencies
    val database = remember { AppDatabase.getDatabase(context) }
    val sessionManager = remember { SessionManager(context) }
    
    // Initialize repositories
    val authRepository = remember { AuthRepository(database.userDao(), sessionManager) }
    val patientRepository = remember { PatientRepository() }

    // Create ViewModel factories
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(authRepository)
    )
    // ScanViewModel has no dependencies - simple factory
    val scanViewModel: ScanViewModel = viewModel(
        factory = ScanViewModel.Factory()
    )
    val patientViewModel: PatientViewModel = viewModel(
        factory = PatientViewModel.Factory(patientRepository)
    )

    // Check session on launch for auto-navigation
    LaunchedEffect(Unit) {
        authViewModel.checkSession { username ->
            navController.navigate(Routes.dashboard(username)) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
    }

    // Observe auth state for navigation
    val authState = authViewModel.uiState.value
    if (authState.isLoggedIn && navController.currentDestination?.route == Routes.LOGIN) {
        LaunchedEffect(authState.isLoggedIn) {
            navController.navigate(Routes.scan(authState.loggedInUsername)) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { username ->
                    navController.navigate(Routes.scan(username)) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.SCAN,
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            ScanPatientScreen(
                username = username,
                viewModel = scanViewModel,
                onScanComplete = {
                    navController.navigate(Routes.dashboard(username)) {
                        popUpTo(Routes.scan(username)) { inclusive = true }
                    }
                },
                onCancel = {
                    scanViewModel.scanAgain()
                    authViewModel.onLogout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.LOGIN) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Routes.DASHBOARD,
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            
            // Load patient data when entering dashboard
            LaunchedEffect(username) {
                patientViewModel.loadPatient(username)
            }
            
            DashboardScreen(
                username = username,
                viewModel = patientViewModel,
                onLogout = {
                    patientViewModel.reset()
                    authViewModel.onLogout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.LOGIN) { inclusive = false }
                    }
                }
            )
        }
    }
}
