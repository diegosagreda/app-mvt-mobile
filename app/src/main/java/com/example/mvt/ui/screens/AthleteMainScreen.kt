package com.example.mvt.ui.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.mvt.R
import com.example.mvt.data.firebase.models.Routine
import com.example.mvt.data.firebase.repositories.NotificationRepository
import com.example.mvt.data.firebase.repositories.RoutineRepository
import com.example.mvt.data.firebase.services.FirestoreService
import com.example.mvt.domain.repositories.StravaRepository
import com.example.mvt.ui.components.AthleteHeader
import com.example.mvt.ui.components.drawer.DrawerContent
import com.example.mvt.ui.screens.personaldata.MorphologyScreen
import com.example.mvt.ui.screens.personaldata.PhysicalCapacityScreen
import com.example.mvt.ui.screens.settings.ConnectionScreen
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.PrimaryBlue
import com.example.mvt.utils.AppForegroundMonitor
import com.example.mvt.utils.AthleteNotificationBus
import com.example.mvt.utils.NotificationHelper
import com.example.mvt.utils.StravaAuthRedirectBus
import com.example.mvt.viewmodels.RealtimeViewModel
import com.example.mvt.ui.viewmodels.UserViewModel
import com.example.mvt.ui.screens.personaldata.ProfileScreen
import com.example.mvt.trainer.ui.TrainerScreen
import com.example.mvt.trainer.ui.TrainersCatalogScreen
import com.example.mvt.trainer.viewmodel.TrainerViewModel
import com.example.mvt.trainer.viewmodel.TrainersCatalogViewModel
import com.example.mvt.goals.ui.GoalsScreen
import com.example.mvt.goals.viewmodel.GoalsViewModel
import com.example.mvt.availability.ui.AvailabilityScreen
import com.example.mvt.availability.viewmodel.AvailabilityViewModel
import com.example.mvt.health.ui.HealthScreen
import com.example.mvt.health.viewmodel.HealthViewModel
import com.example.mvt.sports.ui.SportsScreen
import com.example.mvt.sports.viewmodel.SportsViewModel
import com.example.mvt.plans.ui.PlansScreen
import com.example.mvt.plans.viewmodel.PlansViewModel
import com.example.mvt.subscription.ui.SubscriptionScreen
import com.example.mvt.subscription.viewmodel.SubscriptionViewModel
import com.example.mvt.ui.viewmodels.MorphologyViewModel
import com.example.mvt.ui.viewmodels.NotificationsViewModel
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
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val pendingStravaRedirect by StravaAuthRedirectBus.redirects.collectAsState()
    val openNotificationsFromAlert by AthleteNotificationBus.requests.collectAsState()
    val innerStartDestination = if (pendingStravaRedirect != null) "connection" else "routines"
    val innerNavController = rememberNavController()
    val stravaRepository = remember { StravaRepository() }
    val routineRepository = remember { RoutineRepository(FirestoreService()) }
    val notificationsViewModel = remember { NotificationsViewModel(NotificationRepository()) }
    val trainerViewModel: TrainerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val trainersCatalogViewModel: TrainersCatalogViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val goalsViewModel: GoalsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val availabilityViewModel: AvailabilityViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val healthViewModel: HealthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val sportsViewModel: SportsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val plansViewModel: PlansViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val subscriptionViewModel: SubscriptionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    val realtimeViewModel: RealtimeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val morphologyViewModel: MorphologyViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val physicalCapacityViewModel: PhysicalCapacityViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val notificationsState by notificationsViewModel.uiState.collectAsState()

    // === Usuario ===
    val user by userViewModel.user.collectAsState()
    val currentAthleteId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    LaunchedEffect(Unit) {
        userViewModel.loadUserInfo()
        Log.d("AthleteMainScreen", "────────────────────────────────────────")
        Log.d("AthleteMainScreen", "Cargando información del usuario Firebase...")
    }

    DisposableEffect(currentAthleteId) {
        notificationsViewModel.start(currentAthleteId)
        onDispose {
            notificationsViewModel.stop()
        }
    }

    val saludo = remember { com.example.mvt.utils.TimeUtils.getGreeting() }
    val innerBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentInnerRoute = innerBackStackEntry?.destination?.route
    var isStravaConnected by remember { mutableStateOf(false) }
    var showNotificationsPanel by remember { mutableStateOf(false) }
    val appInForeground by AppForegroundMonitor.isForeground.collectAsState()
    var hasForegroundNotificationSeed by rememberSaveable { mutableStateOf(false) }
    var lastForegroundNotificationId by rememberSaveable { mutableStateOf("") }

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

    LaunchedEffect(currentAthleteId) {
        hasForegroundNotificationSeed = false
        lastForegroundNotificationId = ""
    }

    LaunchedEffect(showNotificationsPanel, notificationsState.notifications) {
        if (showNotificationsPanel) {
            notificationsViewModel.onNotificationsPanelOpened()
        }
    }

    LaunchedEffect(openNotificationsFromAlert) {
        if (!openNotificationsFromAlert) return@LaunchedEffect
        showNotificationsPanel = true
        AthleteNotificationBus.clear()
    }

    LaunchedEffect(
        notificationsState.isLoading,
        notificationsState.notifications.firstOrNull()?.id,
        showNotificationsPanel,
        appInForeground
    ) {
        if (notificationsState.isLoading) return@LaunchedEffect

        val latestNotification = notificationsState.notifications.firstOrNull()
        if (!hasForegroundNotificationSeed) {
            hasForegroundNotificationSeed = true
            lastForegroundNotificationId = latestNotification?.id.orEmpty()
            return@LaunchedEffect
        }

        if (latestNotification == null) {
            lastForegroundNotificationId = ""
            return@LaunchedEffect
        }

        if (latestNotification.id == lastForegroundNotificationId) return@LaunchedEffect
        lastForegroundNotificationId = latestNotification.id

        if (!appInForeground || showNotificationsPanel || !latestNotification.isUnread) return@LaunchedEffect

        NotificationHelper.showNewAthleteAlertNotification(
            context = context,
            preview = latestNotification.txt
        )
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
            DrawerContent(currentRoute = currentInnerRoute, onItemClick = { route ->
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
                    onNotificationClick = {
                        showNotificationsPanel = true
                    },
                    onLogoutClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    profilePhotoUrl = user?.foto_url,
                    isStravaConnected = isStravaConnected,
                    unreadMessagesCount = unreadMessagesCount,
                    unreadNotificationsCount = notificationsState.unreadCount
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
                    composable("notification_detail") {
                        NotificationDetailScreen()
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
                    composable("trainer") {
                        TrainerScreen(
                            athleteId = currentAthleteId,
                            viewModel = trainerViewModel,
                            onBack = { innerNavController.popBackStack() },
                            onUpgradePlan = {
                                innerNavController.navigate("plans")
                            }
                        )
                    }
                    composable("trainers") {
                        TrainersCatalogScreen(
                            athleteId = currentAthleteId,
                            viewModel = trainersCatalogViewModel,
                            onBack = { innerNavController.popBackStack() },
                            onOpenAssignedTrainer = {
                                innerNavController.navigate("trainer") { launchSingleTop = true }
                            },
                            onUpgradePlan = {
                                innerNavController.navigate("plans")
                            }
                        )
                    }
                    composable("goals") {
                        GoalsScreen(
                            athleteId = currentAthleteId,
                            viewModel = goalsViewModel,
                            onBack = { innerNavController.popBackStack() }
                        )
                    }
                    composable("availability") {
                        AvailabilityScreen(
                            athleteId = currentAthleteId,
                            viewModel = availabilityViewModel,
                            onBack = { innerNavController.popBackStack() }
                        )
                    }
                    composable("health") {
                        HealthScreen(
                            athleteId = currentAthleteId,
                            viewModel = healthViewModel,
                            onBack = { innerNavController.popBackStack() }
                        )
                    }
                    composable("sports") {
                        SportsScreen(
                            athleteId = currentAthleteId,
                            viewModel = sportsViewModel,
                            onBack = { innerNavController.popBackStack() }
                        )
                    }
                    composable("plans") {
                        PlansScreen(
                            athleteId = currentAthleteId,
                            viewModel = plansViewModel,
                            onBack = { innerNavController.popBackStack() }
                        )
                    }
                    composable("subscription") {
                        SubscriptionScreen(
                            athleteId = currentAthleteId,
                            viewModel = subscriptionViewModel,
                            onBack = { innerNavController.popBackStack() }
                        )
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

                NotificationsSidePanel(
                    visible = showNotificationsPanel,
                    uiState = notificationsState,
                    onClose = { showNotificationsPanel = false },
                    onDismissError = notificationsViewModel::clearError,
                    onLoadMore = notificationsViewModel::loadMore,
                    onNotificationClick = { notification ->
                        notificationsViewModel.onNotificationOpened(notification)
                        showNotificationsPanel = false

                        when {
                            notification.tipo.equals("rutina", ignoreCase = true) -> {
                                scope.launch {
                                    val routine = runCatching {
                                        routineRepository.getRoutineById(notification.rutina)
                                    }.getOrNull()

                                    if (routine != null) {
                                        innerNavController.currentBackStackEntry
                                            ?.savedStateHandle
                                            ?.set("routine_selected", routine)
                                        innerNavController.currentBackStackEntry
                                            ?.savedStateHandle
                                            ?.set("ritmos", realtimeViewModel.ritmos.value)
                                        innerNavController.currentBackStackEntry
                                            ?.savedStateHandle
                                            ?.set("zonas", realtimeViewModel.zonas.value)
                                        innerNavController.navigate("routine_detail")
                                    } else {
                                        innerNavController.navigate("routines") {
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            }

                            notification.tipo.equals("match", ignoreCase = true) -> {
                                innerNavController.navigate(
                                    UnderConstructionDestination.routeFor("trainer")
                                ) {
                                    launchSingleTop = true
                                }
                            }

                            notification.tipo.equals("msg", ignoreCase = true) ||
                                notification.tipo.equals("chat", ignoreCase = true) -> {
                                onOpenChat()
                            }

                            else -> {
                                innerNavController.navigate("notification_detail") {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                )

                if (showLoader) LoaderOverlay()
            }
        }
    }
}

@Composable
private fun NotificationsSidePanel(
    visible: Boolean,
    uiState: com.example.mvt.ui.viewmodels.NotificationsUiState,
    onClose: () -> Unit,
    onDismissError: () -> Unit,
    onLoadMore: () -> Unit,
    onNotificationClick: (com.example.mvt.data.firebase.models.AppNotification) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.34f))
                .clickable(onClick = onClose)
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.9f)
                        .clickable { }
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = AppSurface,
                        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder),
                        tonalElevation = 8.dp,
                        shadowElevation = 12.dp
                    ) {
                    NotificationsScreen(
                        uiState = uiState,
                        onClose = onClose,
                        onDismissError = onDismissError,
                        onLoadMore = onLoadMore,
                        onNotificationClick = onNotificationClick
                    )
                }
            }
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
