package com.example.mvt.ui.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.mvt.data.firebase.models.Routine
import com.example.mvt.ui.components.AthleteHeader
import com.example.mvt.ui.components.drawer.DrawerContent
import com.example.mvt.ui.screens.personaldata.MorphologyScreen
import com.example.mvt.ui.theme.PrimaryBlue
import com.example.mvt.viewmodels.RealtimeViewModel
import com.example.mvt.ui.viewmodels.UserViewModel
import com.example.mvt.ui.screens.personaldata.ProfileScreen
import com.example.mvt.ui.viewmodels.MorphologyViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthleteMainScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val innerNavController = rememberNavController()

    val realtimeViewModel: RealtimeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val morphologyViewModel: MorphologyViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    // === Usuario ===
    val user by userViewModel.user.collectAsState()
    LaunchedEffect(Unit) {
        userViewModel.loadUserInfo()
        Log.d("AthleteMainScreen", "────────────────────────────────────────")
        Log.d("AthleteMainScreen", "Cargando información del usuario Firebase...")
    }

    val saludo = remember { com.example.mvt.utils.TimeUtils.getGreeting() }

    // Loader inicial
    var showLoader by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(2000)
        showLoader = false
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(onItemClick = { route ->
                scope.launch { drawerState.close() }
                innerNavController.navigate(route)
            })
        }
    ) {
        Scaffold(
            modifier = Modifier.background(Color.White),
            topBar = {
                AthleteHeader(
                    saludo = saludo,
                    userName = user?.nombres ?: "Atleta",
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onMessageClick = { },
                    onNotificationClick = { },
                    onLogoutClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    profilePhotoUrl = user?.foto_url
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(padding)
            ) {
                NavHost(
                    navController = innerNavController,
                    startDestination = "routines",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("routines") {
                        val firebaseUser = FirebaseAuth.getInstance().currentUser
                        val currentAthleteId = firebaseUser?.uid ?: ""

                        LaunchedEffect(currentAthleteId) {
                            if (currentAthleteId.isNotEmpty()) {
                                Log.d("AthleteMainScreen", "Cargando datos del atleta $currentAthleteId desde Firebase...")
                                realtimeViewModel.cargarDatos(currentAthleteId)
                            }
                        }

                        val ritmos = realtimeViewModel.ritmos.value
                        val zonas = realtimeViewModel.zonas.value

                        LaunchedEffect(ritmos, zonas) {
                            Log.d("AthleteMainScreen", "────────────────────────────────────────")
                            Log.d("AthleteMainScreen", "Ritmos cargados: $ritmos")
                            Log.d("AthleteMainScreen", "Zonas cargadas: $zonas")
                        }

                        RoutinesScreen(
                            navController = innerNavController,
                            onRoutineClick = { },
                            currentAthleteId = currentAthleteId,
                            ritmos = ritmos,
                            zonas = zonas
                        )
                    }
                    composable("routine_detail") {

                        Log.e("NavGraph", "──────────── Entrando a routine_detail ────────────")
                        Log.e("NavGraph", "current = ${innerNavController.currentBackStackEntry}")
                        Log.e("NavGraph", "previous = ${innerNavController.previousBackStackEntry}")

                        val previous = innerNavController.previousBackStackEntry

                        val routine = previous
                            ?.savedStateHandle
                            ?.get<Routine>("routine_selected")

                        val ritmos = previous
                            ?.savedStateHandle
                            ?.get<Map<String, Any>>("ritmos")

                        val zonas = previous
                            ?.savedStateHandle
                            ?.get<Map<String, Any>>("zonas")

                        Log.e("NavGraph", "Routine = $routine")
                        Log.e("NavGraph", "Routine.id = ${routine?.id}")
                        Log.e("NavGraph", "Ritmos = $ritmos")
                        Log.e("NavGraph", "Zonas = $zonas")

                        if (routine != null) {
                            RoutineDetailScreen(
                                routine = routine,
                                ritmos = ritmos,
                                zonas = zonas,
                                onBackClick = { innerNavController.popBackStack() }
                            )
                        } else {
                            Log.e("NavGraph", "❌ routine es NULL – mostrando fallback")
                            MissingRoutineScreen()
                        }
                    }
                    composable("profile"){
                        ProfileScreen(
                            userViewModel = userViewModel,
                            navController = innerNavController
                        )
                    }
                    composable("morphology") {
                        MorphologyScreen(
                            navController      = innerNavController,
                            morphologyViewModel = morphologyViewModel
                        )
                    }

                }

                if (showLoader) LoaderOverlay()
            }
        }
    }
}

@Composable
private fun MissingRoutineScreen() {
    LoaderOverlay()
}

@Composable
private fun LoaderOverlay() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = PrimaryBlue, strokeWidth = 6.dp, modifier = Modifier.size(70.dp))
    }
}
