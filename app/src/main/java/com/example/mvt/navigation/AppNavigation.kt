// ===============================================
// FILE 2: com.example.mvt.navigation.AppNavigation.kt
// ===============================================
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
import com.example.mvt.ui.screens.LogoScreen
import com.example.mvt.ui.screens.SessionScreen
import com.example.mvt.ui.screens.auth.LoginScreen
import com.example.mvt.ui.viewmodels.UserViewModel

// ===== CHAT =====
import com.example.mvt.chat.ui.components.TrainerBubbleHost
import com.example.mvt.chat.ui.screen.ChatPopup
import com.example.mvt.chat.ui.screen.ChatConversationContent

// ===== TRAINER (RTDB) =====
import com.example.mvt.chat.data.repo.TrainerRealtimeRepository
import com.example.mvt.chat.viewmodel.TrainerBubbleViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

// ===== FIRESTORE/STORAGE =====
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation() {
    val navController: NavHostController = rememberNavController()

    // --- Inyección manual del ViewModel y sus dependencias ---
    val userRepository = remember { UserRepository() }
    val getUserInfoUseCase = remember { GetUserInfoUseCase(userRepository) }
    val userViewModel = remember { UserViewModel(getUserInfoUseCase) }

    // ====== INSTANCIAS ESTABLES (IMPORTANTÍSIMO) ======
    val db = remember { FirebaseFirestore.getInstance() }
    val storage = remember { FirebaseStorage.getInstance() }

    // ====== ESTADO GLOBAL DEL CHAT (overlay) ======
    var showChat by rememberSaveable { mutableStateOf(false) }

    // ====== ID DEPORTISTA (UID) ======
    val athleteId = remember {
        FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    }

    // ====== Trainer bubble (Realtime Database) ======
    val trainerRepo = remember { TrainerRealtimeRepository(FirebaseDatabase.getInstance()) }
    val trainerBubbleVm = remember { TrainerBubbleViewModel(trainerRepo) }

    // ====== RUTA ACTUAL ======
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Mostrar burbuja SOLO en pantallas post-login
    val showBubble = currentRoute in setOf(
        "athleteMain"
    )


    LaunchedEffect(showBubble) {
        if (!showBubble) showChat = false
    }


    val trainerId: String = trainerBubbleVm.trainerId.collectAsState(initial = "").value

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
        if (showBubble && athleteId.isNotBlank()) {

            TrainerBubbleHost(
                athleteId = athleteId,
                vm = trainerBubbleVm,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, bottom = 16.dp),
                //onClick = { showChat = true }
            )

            ChatPopup(
                show = showChat,
                onDismiss = { showChat = false },
                title = "Chat",
                uid = athleteId,
                otherUid = trainerId,
                role = "deportista",
                conversationId = null,
                db = db,
                storage = storage
            ) { vm, state ->
                ChatConversationContent(vm = vm, state = state)
            }
        }
    }
}