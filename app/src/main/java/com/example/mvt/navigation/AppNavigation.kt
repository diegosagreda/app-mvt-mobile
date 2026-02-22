package com.example.mvt.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mvt.domain.repositories.UserRepository
import com.example.mvt.domain.usecases.GetUserInfoUseCase
import com.example.mvt.ui.screens.AthleteMainScreen
import com.example.mvt.ui.screens.SessionScreen
import com.example.mvt.ui.screens.auth.LoginScreen
import com.example.mvt.ui.screens.LogoScreen
import com.example.mvt.ui.viewmodels.UserViewModel

// ===== CHAT IMPORTS =====
import com.example.mvt.chat.data.repo.ChatRepository
import com.example.mvt.chat.ui.components.TrainerChatBubble
import com.example.mvt.chat.ui.screen.ChatPopupDialog
import com.example.mvt.chat.ui.screen.ChatScreen
import com.example.mvt.chat.viewmodel.ChatViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation() {
    val navController: NavHostController = rememberNavController()

    // --- Inyección manual del ViewModel y sus dependencias ---
    val userRepository = UserRepository()
    val getUserInfoUseCase = GetUserInfoUseCase(userRepository)
    val userViewModel = UserViewModel(getUserInfoUseCase)

    // ====== ESTADO GLOBAL DEL CHAT (overlay) ======
    var showChat by rememberSaveable { mutableStateOf(false) }

    // TODO: reemplazar por la URL real del entrenador (ya cuando exista sesión)
    val trainerPhotoUrl = remember { "" }

    val chatRepo = remember {
        ChatRepository(
            db = FirebaseFirestore.getInstance(),
            storage = FirebaseStorage.getInstance()
        )
    }
    val chatVm = remember { ChatViewModel(chatRepo) }

    // ====== RUTA ACTUAL ======
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Mostrar burbuja SOLO en pantallas post-login
    // Ajusta esta lista a tus rutas finales.
    val showBubble = currentRoute in setOf(
        "athleteMain"
        // si luego tienes más rutas post-login, agrégalas aquí:
        // "trainerMain", "home", "profile", etc.
    )

    // Si navega a login/logo/session_checker, cierra el popup por si quedó abierto
    LaunchedEffect(showBubble) {
        if (!showBubble) showChat = false
    }

    Box(Modifier.fillMaxSize()) {

        // ====== NAVHOST ======
        NavHost(
            navController = navController,
            startDestination = "session_checker"
        ) {
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

        // ====== BURBUJA + POPUP SOLO POST-LOGIN ======
        if (showBubble) {
            TrainerChatBubble(
                photoUrl = trainerPhotoUrl,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, bottom = 16.dp),
                onClick = { showChat = true }
            )

            if (showChat) {
                ChatPopupDialog(onDismiss = { showChat = false }) {
                    ChatScreen(
                        vm = chatVm,
                        onPickImage = { /* TODO: abrir picker */ },
                        onRecordAudio = { /* TODO: grabar */ }
                    )
                }
            }
        }
    }
}