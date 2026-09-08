package com.example.mvt.trainer.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.mvt.trainer.main.ui.HomeScreen
import com.example.mvt.trainer.profile.ui.PersonalInfoScreen
import com.example.mvt.trainer.profile.ui.ProfileScreen
import com.example.mvt.ui.screens.AccountSettingsScreen
import com.example.mvt.ui.screens.UnderConstructionDestination
import com.example.mvt.ui.screens.UnderConstructionScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun TrainerNavGraph(
    navController: NavHostController,
    rootNavController: NavHostController, // Para el logout principal
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = TrainerDestination.HOME,
        modifier = modifier
    ) {
        composable(TrainerDestination.HOME) {
            HomeScreen()
        }

        composable(TrainerDestination.PROFILE) {
            ProfileScreen(
                onOpenPersonalInfo = {
                    navController.navigate(
                        TrainerDestination.PERSONAL_INFO
                    ) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(TrainerDestination.PERSONAL_INFO) {
            PersonalInfoScreen(navController = navController)
        }

        composable(TrainerDestination.SETTINGS) {
            AccountSettingsScreen(
                showConnection = false, // Ocultar Conexión para Entrenador
                onNavigate = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    rootNavController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Pantallas de "En construcción"
        composable(
            route = UnderConstructionDestination.routePattern,
            arguments = listOf(navArgument(UnderConstructionDestination.featureArg) { type = NavType.StringType })
        ) { backStackEntry ->
            val featureId = backStackEntry.arguments?.getString(UnderConstructionDestination.featureArg)
            UnderConstructionScreen(
                featureId = featureId,
                onGoToRoutines = {
                    navController.navigate(TrainerDestination.HOME) {
                        popUpTo(TrainerDestination.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}