// ===============================================
// FILE 2: com.example.mvt.navigation.AppNavigation.kt
// ===============================================
package com.example.mvt.navigation

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
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
import com.example.mvt.ui.screens.auth.RegisterScreen
import com.example.mvt.ui.screens.auth.EmailVerificationScreen
import com.example.mvt.welcome.ui.WelcomeScreen
import com.example.mvt.welcome.viewmodel.WelcomeViewModel
import com.example.mvt.utils.AppForegroundMonitor
import com.example.mvt.ui.viewmodels.UserViewModel
import com.example.mvt.utils.ChatNotificationBus
import com.example.mvt.utils.NotificationHelper
import com.example.mvt.utils.StravaAuthRedirectBus

// ===== CHAT =====
import com.example.mvt.chat.data.model.ChatMessage
import com.example.mvt.chat.data.repo.ChatRepository
import com.example.mvt.chat.ui.screen.ChatConversationContent
import com.example.mvt.chat.ui.screen.ChatPopup

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
    val tag = "MVT_AppNavigation"
    val navController: NavHostController = rememberNavController()
    val context = LocalContext.current
    val pendingStravaRedirect by StravaAuthRedirectBus.redirects.collectAsState()
    val openChatFromNotification by ChatNotificationBus.requests.collectAsState()
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    val startDestination = if (pendingStravaRedirect != null && currentUser != null) {
        "athleteMain"
    } else {
        "session_checker"
    }

    // --- Inyección manual del ViewModel y sus dependencias ---
    val userRepository = remember { UserRepository() }
    val getUserInfoUseCase = remember { GetUserInfoUseCase(userRepository) }
    val userViewModel = remember { UserViewModel(getUserInfoUseCase) }

    // ====== INSTANCIAS ESTABLES (IMPORTANTÍSIMO) ======
    val db = remember { FirebaseFirestore.getInstance() }
    val storage = remember { FirebaseStorage.getInstance() }
    val chatRepo = remember(db, storage) { ChatRepository(db, storage) }

    // ====== ESTADO GLOBAL DEL CHAT (overlay) ======
    var showChat by rememberSaveable { mutableStateOf(false) }
    var unreadMessagesCount by rememberSaveable { mutableStateOf(0) }
    var lastNotifiedIncomingMessageId by rememberSaveable { mutableStateOf("") }

    // ====== ID DEPORTISTA (UID) ======
    val athleteId = currentUser?.uid.orEmpty()

    // ====== Trainer bubble (Realtime Database) ======
    val trainerRepo = remember { TrainerRealtimeRepository(FirebaseDatabase.getInstance()) }
    val trainerBubbleVm = remember { TrainerBubbleViewModel(trainerRepo) }
    val trainer by trainerBubbleVm.trainer.collectAsState()
    val trainerId by trainerBubbleVm.trainerId.collectAsState()
    val appInForeground by AppForegroundMonitor.isForeground.collectAsState()

    // ====== RUTA ACTUAL ======
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentShowChat by rememberUpdatedState(showChat)
    val currentAppInForeground by rememberUpdatedState(appInForeground)
    val currentTrainerName by rememberUpdatedState(
        listOf(trainer.nombres, trainer.apellidos)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "Tu entrenador" }
    )

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
            Log.d(tag, "authState currentUser=${firebaseAuth.currentUser?.uid.orEmpty()}")
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(athleteId) {
        if (athleteId.isNotBlank()) {
            trainerBubbleVm.start(athleteId)
        }
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute != "athleteMain") showChat = false
    }

    LaunchedEffect(pendingStravaRedirect, currentRoute) {
        if (pendingStravaRedirect != null && currentUser != null && currentRoute != "athleteMain") {
            navController.navigate("athleteMain") {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(openChatFromNotification, currentRoute) {
        Log.d(tag, "openChatFromNotification=$openChatFromNotification currentRoute=$currentRoute currentUser=${currentUser?.uid.orEmpty()}")
        if (!openChatFromNotification) return@LaunchedEffect
        if (currentUser == null) {
            ChatNotificationBus.clear()
            return@LaunchedEffect
        }

        if (currentRoute != "athleteMain") {
            navController.navigate("athleteMain") {
                launchSingleTop = true
            }
        }
        showChat = true
        Log.d(tag, "showChat set true from notification")
        ChatNotificationBus.clear()
    }

    DisposableEffect(athleteId, trainerId, chatRepo) {
        if (athleteId.isBlank() || trainerId.isBlank()) {
            unreadMessagesCount = 0
            onDispose { }
        } else {
            val registration = chatRepo.listenAthleteChatAlerts(
                uid = athleteId,
                otherUid = trainerId
            ) { alert ->
                unreadMessagesCount = alert.unreadCountForAthlete

                val latestIncoming = alert.latestIncomingMessage ?: return@listenAthleteChatAlerts
                if (alert.isInitial) {
                    lastNotifiedIncomingMessageId = latestIncoming.id
                    return@listenAthleteChatAlerts
                }

                if (latestIncoming.id == lastNotifiedIncomingMessageId) return@listenAthleteChatAlerts
                lastNotifiedIncomingMessageId = latestIncoming.id

                if (currentAppInForeground && !currentShowChat) {
                    NotificationHelper.showNewChatMessageNotification(
                        context = context,
                        trainerName = currentTrainerName,
                        preview = latestIncoming.notificationPreview()
                    )
                }
            }

            onDispose {
                registration.remove()
            }
        }
    }

    Box(Modifier.fillMaxSize()) {

        // ====== NAVHOST ======
        NavHost(
            navController = navController,
            startDestination = startDestination
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

            composable("register") {
                RegisterScreen(navController)
            }

            composable("welcome") {
                val welcomeViewModel: WelcomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                WelcomeScreen(
                    uid = currentUser?.uid.orEmpty(),
                    viewModel = welcomeViewModel,
                    onCompleted = {
                        navController.navigate("athleteMain") {
                            popUpTo("welcome") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = "verify_email/{email}",
                arguments = listOf(navArgument("email") { type = NavType.StringType })
            ) { backStackEntry ->
                EmailVerificationScreen(
                    initialEmail = backStackEntry.arguments?.getString("email").orEmpty(),
                    navController = navController
                )
            }

            composable("athleteMain") {
                AthleteMainScreen(
                    navController = navController,
                    userViewModel = userViewModel,
                    onOpenChat = {
                        Log.d(tag, "onOpenChat clicked athleteId=$athleteId trainerId=$trainerId currentRoute=$currentRoute")
                        showChat = true
                    },
                    unreadMessagesCount = if (showChat) 0 else unreadMessagesCount
                )
            }
        }

        // ====== CHAT SOLO POST-LOGIN ======
        if (currentRoute == "athleteMain" && athleteId.isNotBlank()) {
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

private fun ChatMessage.notificationPreview(): String {
    return when {
        texto.isNotBlank() -> texto
        imageUrl.isNotBlank() -> "Te envio una imagen."
        audioUrl.isNotBlank() -> "Te envio un audio."
        else -> "Tienes un mensaje nuevo."
    }
}
