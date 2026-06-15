package com.example.mvt.ui.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.mvt.R
import com.example.mvt.data.firebase.models.Routine
import com.example.mvt.domain.repositories.StravaRepository
import com.example.mvt.ui.components.AthleteHeader
import com.example.mvt.ui.components.drawer.DrawerContent
import com.example.mvt.ui.screens.personaldata.MorphologyScreen
import com.example.mvt.ui.screens.personaldata.PhysicalCapacityScreen
import com.example.mvt.ui.screens.settings.ConnectionScreen
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.PrimaryBlue
import com.example.mvt.utils.StravaAuthRedirectBus
import com.example.mvt.viewmodels.RealtimeViewModel
import com.example.mvt.ui.viewmodels.UserViewModel
import com.example.mvt.ui.screens.personaldata.ProfileScreen
import com.example.mvt.ui.viewmodels.MorphologyViewModel
import com.example.mvt.ui.viewmodels.PhysicalCapacityViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthleteMainScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    onOpenChat: () -> Unit = {},
    unreadMessagesCount: Int = 0
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val pendingStravaRedirect by StravaAuthRedirectBus.redirects.collectAsState()
    val innerStartDestination = if (pendingStravaRedirect != null) "connection" else "routines"
    val innerNavController = rememberNavController()
    val stravaRepository = remember { StravaRepository() }

    val realtimeViewModel: RealtimeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val morphologyViewModel: MorphologyViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val physicalCapacityViewModel: PhysicalCapacityViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    // === Usuario ===
    val user by userViewModel.user.collectAsState()
    LaunchedEffect(Unit) {
        userViewModel.loadUserInfo()
        Log.d("AthleteMainScreen", "────────────────────────────────────────")
        Log.d("AthleteMainScreen", "Cargando información del usuario Firebase...")
    }

    val saludo = remember { com.example.mvt.utils.TimeUtils.getGreeting() }
    val innerBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentInnerRoute = innerBackStackEntry?.destination?.route
    var isStravaConnected by remember { mutableStateOf(false) }

    LaunchedEffect(currentInnerRoute) {
        isStravaConnected = runCatching {
            stravaRepository.getConnectionSnapshot().connection != null
        }.getOrDefault(false)
    }

    LaunchedEffect(pendingStravaRedirect) {
        if (pendingStravaRedirect != null && innerNavController.currentDestination?.route != "connection") {
            innerNavController.navigate("connection") {
                launchSingleTop = true
            }
        }
    }

    // Loader inicial
    var showLoader by remember(pendingStravaRedirect) { mutableStateOf(pendingStravaRedirect == null) }
    LaunchedEffect(Unit) {
        if (pendingStravaRedirect != null) return@LaunchedEffect
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
            modifier = Modifier.background(AppBackground),
            topBar = {
                AthleteHeader(
                    saludo = saludo,
                    userName = user?.nombres ?: "Atleta",
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onMessageClick = onOpenChat,
                    onNotificationClick = { },
                    onLogoutClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    profilePhotoUrl = user?.foto_url,
                    isStravaConnected = isStravaConnected,
                    unreadMessagesCount = unreadMessagesCount
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBackground)
                    .padding(padding)
            ) {
                NavHost(
                    navController = innerNavController,
                    startDestination = innerStartDestination,
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
                    composable("fitness") {
                        PhysicalCapacityScreen(
                            navController = innerNavController,
                            viewModel     = physicalCapacityViewModel
                        )
                    }
                    composable("connection") {
                        ConnectionScreen(navController = innerNavController)
                    }
                    composable(
                        route = UnderConstructionDestination.routePattern,
                        arguments = listOf(
                            navArgument(UnderConstructionDestination.featureArg) {
                                type = NavType.StringType
                            }
                        )
                    ) { backStackEntry ->
                        UnderConstructionScreen(
                            featureId = backStackEntry.arguments?.getString(
                                UnderConstructionDestination.featureArg
                            ),
                            onGoToRoutines = {
                                innerNavController.navigate("routines") {
                                    launchSingleTop = true
                                }
                            }
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
        modifier = Modifier.fillMaxSize().background(AppBackground.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                color = PrimaryBlue,
                strokeWidth = 6.dp,
                modifier = Modifier.size(110.dp)
            )
            Image(
                painter = painterResource(id = R.drawable.mvt),
                contentDescription = "MVT",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(63.dp)
                    .height(27.dp)
            )
        }
    }
}
