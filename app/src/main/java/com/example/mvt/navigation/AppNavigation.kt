package com.example.mvt.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mvt.domain.repositories.UserRepository
import com.example.mvt.domain.usecases.GetUserInfoUseCase
import com.example.mvt.ui.screens.AthleteMainScreen
import com.example.mvt.ui.screens.SessionScreen      // 👈 importar la nueva pantalla
import com.example.mvt.ui.screens.auth.LoginScreen
import com.example.mvt.ui.screens.LogoScreen
import com.example.mvt.ui.viewmodels.UserViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation() {
    val navController: NavHostController = rememberNavController()

    // --- Inyección manual del ViewModel y sus dependencias ---
    val userRepository = UserRepository()
    val getUserInfoUseCase = GetUserInfoUseCase(userRepository)
    val userViewModel = UserViewModel(getUserInfoUseCase)

    NavHost(
        navController = navController,
        startDestination = "session_checker"   // 👈 nueva pantalla de arranque
    ) {
        // === NUEVA PANTALLA DE CONTROL DE SESIÓN ===
        composable("session_checker") {
            SessionScreen(navController = navController)
        }

        composable("logo") {
            LogoScreen(
                onClick = {
                    navController.navigate("login") {
                        popUpTo("logo") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(navController)
        }

        composable("athleteMain") {
            AthleteMainScreen(
                navController = navController,
                userViewModel = userViewModel
            )
        }
    }
}
