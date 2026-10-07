package com.example.mvt.trainer.main.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mvt.trainer.dashboard.ui.TrainerDashboardScreen
import com.example.mvt.trainer.dashboard.viewmodel.TrainerDashboardViewModel
import com.example.mvt.trainer.navigation.TrainerDestination

@Composable
fun HomeScreen(
    navController: NavController
) {

    val dashboardViewModel: TrainerDashboardViewModel = viewModel()


    TrainerDashboardScreen(
        viewModel = dashboardViewModel,
        onNavigateToAthletes = {
            navController.navigate(TrainerDestination.ATHLETES)
        },
        onNavigateToCalendar = { athleteId ->
            navController.navigate("${TrainerDestination.CALENDAR}/$athleteId")
        },
        onNavigateToPersonalInfo = {
            navController.navigate(TrainerDestination.PERSONAL_INFO) {
                launchSingleTop = true
            }
        },
        onNavigateToPendingRequests = {
            navController.navigate(TrainerDestination.PENDING_REQUESTS)
        },
        onNavigateToRoutineLibrary = {
            navController.navigate(TrainerDestination.ROUTINE_LIBRARY)
        }
    )
}