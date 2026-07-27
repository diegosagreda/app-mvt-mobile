package com.example.mvt.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.mvt.R
import com.example.mvt.data.firebase.models.Routine
import com.example.mvt.data.firebase.models.WeeklyRoutineAnalysis
import com.example.mvt.data.firebase.models.WeeklyRoutineMetrics
import com.example.mvt.data.firebase.repositories.NotificationRepository
import com.example.mvt.data.firebase.repositories.RoutineRepository
import com.example.mvt.data.firebase.services.CoachIntelligenceService
import com.example.mvt.data.firebase.services.FirestoreService
import com.example.mvt.domain.repositories.StravaRepository
import com.example.mvt.ui.components.AthleteHeader
import com.example.mvt.ui.screens.personaldata.MorphologyScreen
import com.example.mvt.ui.screens.personaldata.PhysicalCapacityScreen
import com.example.mvt.ui.screens.settings.ConnectionScreen
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppTextSecondary
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
import com.example.mvt.availability.viewmodel.AvailabilityScreenState
import com.example.mvt.availability.viewmodel.AvailabilityViewModel
import com.example.mvt.health.ui.HealthScreen
import com.example.mvt.health.viewmodel.HealthViewModel
import com.example.mvt.sports.ui.SportsScreen
import com.example.mvt.sports.viewmodel.SportsViewModel
import com.example.mvt.plans.ui.PlansScreen
import com.example.mvt.plans.viewmodel.PlansViewModel
import com.example.mvt.subscription.ui.SubscriptionScreen
import com.example.mvt.subscription.viewmodel.SubscriptionViewModel
import com.example.mvt.billing.ui.BillingScreen
import com.example.mvt.billing.viewmodel.BillingViewModel
import com.example.mvt.ui.viewmodels.MorphologyViewModel
import com.example.mvt.ui.viewmodels.NotificationsViewModel
import com.example.mvt.ui.viewmodels.PhysicalCapacityViewModel
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

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
    val pendingStravaRedirect by StravaAuthRedirectBus.redirects.collectAsState()
    val openNotificationsFromAlert by AthleteNotificationBus.requests.collectAsState()
    val innerStartDestination = if (pendingStravaRedirect != null) "connection" else "home"
    val innerNavController = rememberNavController()
    val stravaRepository = remember { StravaRepository() }
    val routineRepository = remember { RoutineRepository(FirestoreService()) }
    val coachIntelligenceService = remember { CoachIntelligenceService() }
    val notificationsViewModel = remember { NotificationsViewModel(NotificationRepository()) }
    val trainerViewModel: TrainerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val trainersCatalogViewModel: TrainersCatalogViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val goalsViewModel: GoalsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val availabilityViewModel: AvailabilityViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val healthViewModel: HealthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val sportsViewModel: SportsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val plansViewModel: PlansViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val subscriptionViewModel: SubscriptionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val billingViewModel: BillingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

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

    val innerBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentInnerRoute = innerBackStackEntry?.destination?.route
    val currentFeatureRoute = innerBackStackEntry
        ?.arguments
        ?.getString(UnderConstructionDestination.featureArg)
        ?.let(UnderConstructionDestination::routeFor)
    val selectedInnerRoute = when {
        currentInnerRoute == UnderConstructionDestination.routePattern -> currentFeatureRoute ?: currentInnerRoute
        currentInnerRoute?.startsWith("routine_detail") == true -> "routines"
        else -> currentInnerRoute
    }
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
    var sectionLoaderTarget by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        if (pendingStravaRedirect != null) return@LaunchedEffect
        delay(2000)
        showLoader = false
    }
    LaunchedEffect(sectionLoaderTarget) {
        if (sectionLoaderTarget != null) {
            delay(650)
            sectionLoaderTarget = null
        }
    }

    Scaffold(
        modifier = Modifier.background(AppBackground),
        topBar = {
            AthleteHeader(
                onMessageClick = onOpenChat,
                onNotificationClick = {
                    showNotificationsPanel = true
                },
                onProfileClick = {
                    innerNavController.navigate("account_settings") {
                        launchSingleTop = true
                    }
                },
                profilePhotoUrl = user?.foto_url,
                isStravaConnected = isStravaConnected,
                unreadMessagesCount = unreadMessagesCount,
                unreadNotificationsCount = notificationsState.unreadCount
            )
        },
        bottomBar = {
            AthleteBottomNavigationBar(
                currentRoute = selectedInnerRoute,
                onNavigate = { route ->
                    if (currentInnerRoute != route) {
                        if (route in mainAthleteRoutesWithLoader) {
                            sectionLoaderTarget = route
                        }
                        innerNavController.navigate(route) {
                            popUpTo(innerNavController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
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
                    composable("home") {
                        AthleteHomeDashboard(
                            athleteName = user?.nombres ?: "Atleta",
                            athleteId = currentAthleteId,
                            routineRepository = routineRepository,
                            coachIntelligenceService = coachIntelligenceService,
                            onOpenRoutine = { routine ->
                                innerNavController.navigateToRoutineDetail(routine.id)
                            }
                        )
                    }
                    composable("routines") {
                        LaunchedEffect(currentAthleteId) {
                            if (currentAthleteId.isNotEmpty()) {
                                Log.d("AthleteMainScreen", "Cargando datos del atleta $currentAthleteId desde Firebase...")
                                realtimeViewModel.cargarDatos(currentAthleteId)
                                availabilityViewModel.load(currentAthleteId)
                            }
                        }

                        val ritmos = realtimeViewModel.ritmos.value
                        val zonas = realtimeViewModel.zonas.value
                        val availabilityState by availabilityViewModel.state.collectAsState()
                        val trainingAvailability = (availabilityState as? AvailabilityScreenState.Ready)?.availability

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
                            zonas = zonas,
                            athleteName = user?.nombres ?: "Atleta",
                            trainingAvailability = trainingAvailability,
                            contentMode = RoutinesContentMode.CALENDAR
                        )
                    }
                    composable("statistics") {
                        LaunchedEffect(currentAthleteId) {
                            if (currentAthleteId.isNotEmpty()) {
                                realtimeViewModel.cargarDatos(currentAthleteId)
                            }
                        }

                        RoutinesScreen(
                            navController = innerNavController,
                            onRoutineClick = { },
                            currentAthleteId = currentAthleteId,
                            ritmos = realtimeViewModel.ritmos.value,
                            zonas = realtimeViewModel.zonas.value,
                            athleteName = user?.nombres ?: "Atleta",
                            contentMode = RoutinesContentMode.STATISTICS
                        )
                    }
                    composable(
                        route = "routine_detail/{routineId}",
                        arguments = listOf(
                            navArgument("routineId") {
                                type = NavType.StringType
                            }
                        )
                    ) { backStackEntry ->
                        val routineId = backStackEntry.arguments?.getString("routineId").orEmpty()
                        var routine by remember(routineId) { mutableStateOf<Routine?>(null) }
                        var isLoadingRoutine by remember(routineId) { mutableStateOf(true) }
                        var routineLoadFailed by remember(routineId) { mutableStateOf(false) }

                        LaunchedEffect(routineId) {
                            isLoadingRoutine = true
                            routineLoadFailed = false
                            routine = runCatching {
                                routineRepository.getRoutineById(routineId)
                            }.onFailure { error ->
                                Log.e("AthleteMainScreen", "Error cargando rutina $routineId", error)
                            }.getOrNull()
                            routineLoadFailed = routine == null
                            isLoadingRoutine = false
                        }

                        when {
                            isLoadingRoutine -> LoaderOverlay()
                            routine != null -> RoutineDetailScreen(
                                routine = routine!!,
                                ritmos = realtimeViewModel.ritmos.value,
                                zonas = realtimeViewModel.zonas.value,
                                onBackClick = { innerNavController.popBackStack() }
                            )
                            else -> MissingRoutineScreen(
                                loadFailed = routineLoadFailed,
                                onBackClick = { innerNavController.popBackStack() }
                            )
                        }
                    }
                    composable("notification_detail") {
                        NotificationDetailScreen()
                    }
                    composable("profile"){
                        PersonalDataHubScreen(
                            onNavigate = { route ->
                                innerNavController.navigate(route) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable("personal_profile"){
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
                    composable("account_settings") {
                        AccountSettingsScreen(
                            onNavigate = { route ->
                                innerNavController.navigate(route) {
                                    launchSingleTop = true
                                }
                            },
                            onLogout = {
                                FirebaseAuth.getInstance().signOut()
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("plan") {
                        PlanHubScreen(
                            onNavigate = { route ->
                                innerNavController.navigate(route) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable("trainer") {
                        TrainerScreen(
                            athleteId = currentAthleteId,
                            viewModel = trainerViewModel,
                            onBack = { innerNavController.navigateToPlanHub() },
                            onUpgradePlan = {
                                innerNavController.navigate("plans")
                            }
                        )
                    }
                    composable("trainers") {
                        TrainersCatalogScreen(
                            athleteId = currentAthleteId,
                            viewModel = trainersCatalogViewModel,
                            onBack = { innerNavController.navigateToPlanHub() },
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
                            onBack = { innerNavController.navigateToPersonalDataHub() }
                        )
                    }
                    composable("availability") {
                        AvailabilityScreen(
                            athleteId = currentAthleteId,
                            viewModel = availabilityViewModel,
                            onBack = { innerNavController.navigateToPersonalDataHub() }
                        )
                    }
                    composable("health") {
                        HealthScreen(
                            athleteId = currentAthleteId,
                            viewModel = healthViewModel,
                            onBack = { innerNavController.navigateToPersonalDataHub() }
                        )
                    }
                    composable("sports") {
                        SportsScreen(
                            athleteId = currentAthleteId,
                            viewModel = sportsViewModel,
                            onBack = { innerNavController.navigateToPersonalDataHub() }
                        )
                    }
                    composable("plans") {
                        PlansScreen(
                            athleteId = currentAthleteId,
                            viewModel = plansViewModel,
                            onBack = { innerNavController.navigateToPlanHub() }
                        )
                    }
                    composable("subscription") {
                        SubscriptionScreen(
                            navController = innerNavController,
                            viewModel = subscriptionViewModel
                        )
                    }
                    composable("billing") {
                        BillingScreen(
                            navController = innerNavController,
                            viewModel = billingViewModel
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
                        val featureId = backStackEntry.arguments?.getString(
                            UnderConstructionDestination.featureArg
                        )
                        UnderConstructionScreen(
                            featureId = featureId,
                            onGoToRoutines = {
                                if (featureId == "performance") {
                                    innerNavController.navigateToPersonalDataHub()
                                } else {
                                    innerNavController.navigate("routines") {
                                        launchSingleTop = true
                                    }
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
                                if (notification.rutina.isNotBlank()) {
                                    innerNavController.navigateToRoutineDetail(notification.rutina)
                                } else {
                                    innerNavController.navigate("routines") {
                                        launchSingleTop = true
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
                if (sectionLoaderTarget != null) LoaderOverlay()
            }
        }
    }

private data class AthleteBottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val athleteBottomNavItems = listOf(
    AthleteBottomNavItem("home", "Inicio", Icons.Default.Home),
    AthleteBottomNavItem("routines", "Rutinas", Icons.Default.CalendarMonth),
    AthleteBottomNavItem("statistics", "Estadisticas", Icons.Default.Analytics),
    AthleteBottomNavItem("plan", "Plan", Icons.Default.Map),
    AthleteBottomNavItem("profile", "Perfil", Icons.Default.Person)
)

private val mainAthleteRoutesWithLoader = setOf("home", "routines", "statistics")

private val planRoutes = setOf(
    "plan",
    "plans",
    "subscription",
    "trainer",
    "trainers",
    UnderConstructionDestination.routeFor("billing")
)

private val personalDataRoutes = setOf(
    "profile",
    "personal_profile",
    "morphology",
    "fitness",
    "sports",
    "health",
    "goals",
    "availability",
    UnderConstructionDestination.routeFor("performance")
)

private fun NavController.navigateToPersonalDataHub() {
    navigate("profile") {
        popUpTo("profile") { inclusive = false }
        launchSingleTop = true
    }
}

private fun NavController.navigateToPlanHub() {
    navigate("plan") {
        popUpTo("plan") { inclusive = false }
        launchSingleTop = true
    }
}

private fun NavController.navigateToRoutineDetail(routineId: String) {
    if (routineId.isBlank()) return
    navigate("routine_detail/${Uri.encode(routineId)}") {
        launchSingleTop = true
    }
}

@Composable
private fun AthleteBottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Surface(
        color = AppSurface,
        tonalElevation = 10.dp,
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Column {
            NavigationBar(
                containerColor = AppSurface,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier.height(76.dp)
            ) {
                athleteBottomNavItems.forEach { item ->
                    val selected = currentRoute == item.route ||
                        (item.route == "plan" && currentRoute in planRoutes) ||
                        (item.route == "profile" && currentRoute in personalDataRoutes)
                    val available = item.route in setOf("home", "routines", "statistics", "plan", "profile")
                    NavigationBarItem(
                        selected = selected,
                        enabled = available,
                        onClick = { if (available) onNavigate(item.route) },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(23.dp)
                            )
                        },
                        label = {
                            if (item.label.isNotBlank()) {
                                Text(
                                    text = item.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                                )
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = AppTextSecondary,
                            unselectedTextColor = AppTextSecondary,
                            indicatorColor = PrimaryBlue.copy(alpha = 0.22f)
                        )
                    )
                }
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

private data class PlanHubOption(
    val title: String,
    val subtitle: String,
    val route: String,
    val icon: ImageVector,
    val accent: Color
)

private val planHubOptions = listOf(
    PlanHubOption(
        title = "Planes",
        subtitle = "Opciones de acompañamiento",
        route = "plans",
        icon = Icons.Default.Map,
        accent = Color(0xFF62D9A8)
    ),
    PlanHubOption(
        title = "Suscripción",
        subtitle = "Estado y beneficios activos",
        route = "subscription",
        icon = Icons.Default.CreditCard,
        accent = Color(0xFF63C7FF)
    ),
    PlanHubOption(
        title = "Facturación",
        subtitle = "Pagos y comprobantes",
        route = "billing",
        icon = Icons.Default.ReceiptLong,
        accent = Color(0xFFFFC857)
    ),
    PlanHubOption(
        title = "Tu entrenador",
        subtitle = "Acompañamiento asignado",
        route = "trainer",
        icon = Icons.Default.PersonPin,
        accent = Color(0xFFFF9A64)
    ),
    PlanHubOption(
        title = "Entrenadores",
        subtitle = "Explora perfiles disponibles",
        route = "trainers",
        icon = Icons.Default.Groups,
        accent = Color(0xFFC29BFF)
    )
)

@Composable
private fun PlanHubScreen(
    onNavigate: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Plan",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Gestiona tu plan, pagos y acompañamiento deportivo.",
                color = AppTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        items(planHubOptions) { option ->
            PlanHubCard(
                option = option,
                onClick = { onNavigate(option.route) }
            )
        }
    }
}

@Composable
private fun PlanHubCard(
    option: PlanHubOption,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable(onClick = onClick),
        color = AppSurface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, AppBorder),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(option.accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        tint = option.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = AppTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = option.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = option.subtitle,
                    color = AppTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private data class AccountSettingsOption(
    val title: String,
    val subtitle: String,
    val route: String?,
    val icon: ImageVector,
    val accent: Color,
    val destructive: Boolean = false
)

private val accountSettingsOptions = listOf(
    AccountSettingsOption(
        title = "Conexión",
        subtitle = "Sincronización y servicios externos",
        route = "connection",
        icon = Icons.Default.Link,
        accent = Color(0xFF63C7FF)
    ),
    AccountSettingsOption(
        title = "Ayuda",
        subtitle = "Soporte y orientación de la app",
        route = UnderConstructionDestination.routeFor("help"),
        icon = Icons.Default.HelpOutline,
        accent = Color(0xFFC29BFF)
    ),
    AccountSettingsOption(
        title = "Acerca de",
        subtitle = "Información de My Virtual Trainer",
        route = UnderConstructionDestination.routeFor("about"),
        icon = Icons.Default.Info,
        accent = Color(0xFF62D9A8)
    ),
    AccountSettingsOption(
        title = "Cerrar sesión",
        subtitle = "Salir de tu cuenta actual",
        route = null,
        icon = Icons.Default.ExitToApp,
        accent = Color(0xFFFF8A8A),
        destructive = true
    )
)

@Composable
private fun AccountSettingsScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Cuenta y configuración",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }

        item {
            Text(
                text = "Gestiona conexiones, soporte e información de tu cuenta.",
                color = AppTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        accountSettingsOptions.forEach { option ->
            item {
                AccountSettingsRow(
                    option = option,
                    onClick = {
                        option.route?.let(onNavigate) ?: onLogout()
                    }
                )
            }
        }
    }
}

@Composable
private fun AccountSettingsRow(
    option: AccountSettingsOption,
    onClick: () -> Unit
) {
    val titleColor = if (option.destructive) option.accent else Color.White

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = AppSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, AppBorder),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(option.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = null,
                    tint = option.accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = option.title,
                    color = titleColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = option.subtitle,
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = AppTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private data class PersonalDataOption(
    val title: String,
    val subtitle: String,
    val route: String,
    val icon: ImageVector,
    val accent: Color
)

private val personalDataOptions = listOf(
    PersonalDataOption(
        title = "Perfil",
        subtitle = "Identidad y contacto",
        route = "personal_profile",
        icon = Icons.Default.Person,
        accent = Color(0xFF63C7FF)
    ),
    PersonalDataOption(
        title = "Morfologia",
        subtitle = "Medidas y composicion",
        route = "morphology",
        icon = Icons.Default.Accessibility,
        accent = Color(0xFF62D9A8)
    ),
    PersonalDataOption(
        title = "Capacidad fisica",
        subtitle = "Zonas y condiciones",
        route = "fitness",
        icon = Icons.Default.FitnessCenter,
        accent = Color(0xFFFFC857)
    ),
    PersonalDataOption(
        title = "Rendimiento",
        subtitle = "Evolucion deportiva",
        route = UnderConstructionDestination.routeFor("performance"),
        icon = Icons.Default.Timer,
        accent = Color(0xFFFF8A65)
    ),
    PersonalDataOption(
        title = "Deportivo",
        subtitle = "Experiencia y equipo",
        route = "sports",
        icon = Icons.Default.DirectionsBike,
        accent = Color(0xFF8EA7FF)
    ),
    PersonalDataOption(
        title = "Salud",
        subtitle = "Historial y bienestar",
        route = "health",
        icon = Icons.Default.FavoriteBorder,
        accent = Color(0xFFFF72A7)
    ),
    PersonalDataOption(
        title = "Objetivos",
        subtitle = "Metas y plan",
        route = "goals",
        icon = Icons.Default.BarChart,
        accent = Color(0xFFC29BFF)
    ),
    PersonalDataOption(
        title = "Disponibilidad",
        subtitle = "Dias para entrenar",
        route = "availability",
        icon = Icons.Default.EventAvailable,
        accent = Color(0xFF4DD0E1)
    )
)

@Composable
private fun PersonalDataHubScreen(
    onNavigate: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Datos personales",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }

        items(personalDataOptions) { option ->
            PersonalDataOptionCard(
                option = option,
                onClick = { onNavigate(option.route) }
            )
        }
    }
}

@Composable
private fun PersonalDataOptionCard(
    option: PersonalDataOption,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable(onClick = onClick),
        color = AppSurface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, AppBorder),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(option.accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        tint = option.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = AppTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = option.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = option.subtitle,
                    color = AppTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("MissingPermission")
@Composable
private fun AthleteHomeDashboard(
    athleteName: String,
    athleteId: String,
    routineRepository: RoutineRepository,
    coachIntelligenceService: CoachIntelligenceService,
    onOpenRoutine: (Routine) -> Unit
) {
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }

    var city by remember { mutableStateOf("Cargando ubicación...") }
    var temperature by remember { mutableStateOf("--") }
    var description by remember { mutableStateOf("--") }
    var weatherIcon by remember { mutableStateOf(Icons.Default.Cloud) }

    var routinesLoading by remember(athleteId) { mutableStateOf(true) }
    var todayRoutine by remember(athleteId) { mutableStateOf<Routine?>(null) }
    var previousActivity by remember(athleteId) { mutableStateOf<Routine?>(null) }
    var nextRoutine by remember(athleteId) { mutableStateOf<Routine?>(null) }
    var routinesError by remember(athleteId) { mutableStateOf<String?>(null) }
    var coachAnalysisLoading by remember(athleteId) { mutableStateOf(true) }
    var coachAnalysis by remember(athleteId) { mutableStateOf<WeeklyRoutineAnalysis?>(null) }
    var coachAnalysisError by remember(athleteId) { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (androidx.core.app.ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            city = "Permiso de ubicación pendiente"
            return@LaunchedEffect
        }

        val location = runCatching { fused.lastLocation.await() }.getOrNull()

        if (location == null) {
            city = "Ubicación no disponible"
            return@LaunchedEffect
        }

        val weather = withContext(Dispatchers.IO) {
            getHomeLocationName(location.latitude, location.longitude) to
                getHomeCurrentWeather(location.latitude, location.longitude)
        }
        city = weather.first
        temperature = weather.second.first
        description = weather.second.second
        weatherIcon = weather.second.third
    }

    LaunchedEffect(athleteId) {
        if (athleteId.isBlank()) {
            routinesLoading = false
            todayRoutine = null
            previousActivity = null
            nextRoutine = null
            routinesError = null
            return@LaunchedEffect
        }

        routinesLoading = true
        routinesError = null
        val result = runCatching {
            withContext(Dispatchers.IO) {
                routineRepository.getRoutinesByAthlete(athleteId)
            }
        }

        result
            .onSuccess { routines ->
                val today = LocalDate.now()
                todayRoutine = routines
                    .filter { it.homeLocalDate() == today }
                    .sortedWith(compareBy<Routine> { it.homeRoutineStatusPriority() }.thenBy { it.titulo })
                    .firstOrNull()
                previousActivity = routines
                    .filter { routine -> routine.homeLocalDate()?.isBefore(today) == true }
                    .maxWithOrNull(compareBy<Routine> { it.homeLocalDate() }.thenBy { it.titulo })
                nextRoutine = routines
                    .filter { routine -> routine.homeLocalDate()?.isAfter(today) == true }
                    .minWithOrNull(compareBy<Routine> { it.homeLocalDate() }.thenBy { it.titulo })
            }
            .onFailure {
                todayRoutine = null
                previousActivity = null
                nextRoutine = null
                routinesError = "No pudimos cargar tus rutinas."
            }
        routinesLoading = false
    }

    LaunchedEffect(athleteId) {
        if (athleteId.isBlank()) {
            coachAnalysisLoading = false
            coachAnalysis = null
            coachAnalysisError = null
            return@LaunchedEffect
        }

        coachAnalysisLoading = true
        coachAnalysisError = null
        val result = runCatching {
            withContext(Dispatchers.IO) {
                coachIntelligenceService.getWeeklyRoutineAnalysis(athleteId)
            }
        }

        result
            .onSuccess { analysis ->
                coachAnalysis = analysis
            }
            .onFailure {
                coachAnalysis = null
                coachAnalysisError = "Coach Intelligent no está disponible ahora."
            }
        coachAnalysisLoading = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 24.dp)
    ) {
        item {
            HomeHeader(
                athleteName = athleteName,
                city = city,
                temperature = temperature,
                description = description,
                weatherIcon = weatherIcon
            )
        }
        item {
            TodayRoutineCard(
                isLoading = routinesLoading,
                routine = todayRoutine,
                errorMessage = routinesError,
                onOpenRoutine = onOpenRoutine
            )
        }
        item {
            CoachInsightCard(
                isLoading = coachAnalysisLoading,
                analysis = coachAnalysis,
                errorMessage = coachAnalysisError
            )
        }
        item {
            NextRoutineCard(
                isLoading = routinesLoading,
                routine = nextRoutine,
                errorMessage = routinesError
            )
        }
        item {
            LastActivityCard(
                isLoading = routinesLoading,
                routine = previousActivity,
                errorMessage = routinesError
            )
        }
    }
}

@Composable
private fun HomeHeader(
    athleteName: String,
    city: String,
    temperature: String,
    description: String,
    weatherIcon: ImageVector
) {
    val greeting = remember { currentHomeGreeting() }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, AppBorder),
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "$greeting, ${athleteName.ifBlank { "Atleta" }}",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                HomeCurrentDateChip()
            }

            HomeWeatherSummary(
                city = city,
                temperature = temperature,
                description = description,
                weatherIcon = weatherIcon
            )
        }
    }
}

@Composable
private fun HomeCurrentDateChip() {
    val currentDate = remember { homeCurrentDateLabel() }

    Surface(
        color = PrimaryBlue.copy(alpha = 0.14f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = Color(0xFF8EC5FF),
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = currentDate,
                color = Color(0xFFBFDFFF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun homeCurrentDateLabel(): String {
    val locale = Locale("es", "CO")
    return LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", locale))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

private fun currentHomeGreeting(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Buenos días"
        in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

@Composable
private fun HomeWeatherSummary(
    city: String,
    temperature: String,
    description: String,
    weatherIcon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryBlue.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = city,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = "Ubicación actual",
                        color = AppTextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            weatherIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = temperature,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = description,
                        color = AppTextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun getHomeCurrentWeather(lat: Double, lon: Double): Triple<String, String, ImageVector> {
    return try {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
        val current = JSONObject(URL(url).readText()).getJSONObject("current_weather")
        val temp = "${current.getDouble("temperature").toInt()}°C"
        val code = current.getInt("weathercode")
        val (status, icon) = when (code) {
            0 -> "Despejado" to Icons.Default.WbSunny
            1, 2, 3 -> "Parcial nublado" to Icons.Default.WbCloudy
            45, 48 -> "Niebla" to Icons.Default.CloudQueue
            in 51..67 -> "Lluvia ligera" to Icons.Default.Umbrella
            in 80..82 -> "Lluvia" to Icons.Default.Grain
            in 95..99 -> "Tormenta" to Icons.Default.FlashOn
            else -> "Clima" to Icons.Default.Cloud
        }
        Triple(temp, status, icon)
    } catch (e: Exception) {
        Triple("--", "Clima no disponible", Icons.Default.Cloud)
    }
}

private fun getHomeLocationName(lat: Double, lon: Double): String {
    return try {
        val conn = URL("https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$lat&lon=$lon")
            .openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "com.example.mvt (Android App)")
        val text = conn.inputStream.bufferedReader().readText()
        conn.disconnect()
        val address = JSONObject(text).getJSONObject("address")
        address.optString("city", address.optString("town", address.optString("village", "Ubicación actual")))
    } catch (e: Exception) {
        "Ubicación actual"
    }
}

@Composable
private fun TodayRoutineCard(
    isLoading: Boolean,
    routine: Routine?,
    errorMessage: String?,
    onOpenRoutine: (Routine) -> Unit
) {
    HomePremiumCard {
        when {
            isLoading -> TodayRoutineMessage(
                icon = Icons.Default.Cloud,
                title = "Cargando rutina",
                message = "Estamos revisando tu entrenamiento para hoy.",
                accent = PrimaryBlue
            )

            errorMessage != null -> TodayRoutineMessage(
                icon = Icons.Default.Info,
                title = "Rutina no disponible",
                message = errorMessage.orEmpty(),
                accent = Color(0xFFFFC857)
            )

            routine == null -> TodayRoutineMessage(
                icon = Icons.Default.CheckCircle,
                title = "Sin rutina para hoy",
                message = "Hoy no tienes un entrenamiento programado.",
                accent = Color(0xFF62D9A8)
            )

            else -> TodayRoutineContent(
                routine = routine!!,
                onOpenRoutine = onOpenRoutine
            )
        }
    }
}

@Composable
private fun TodayRoutineMessage(
    icon: ImageVector,
    title: String,
    message: String,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                color = AppTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun TodayRoutineContent(
    routine: Routine,
    onOpenRoutine: (Routine) -> Unit
) {
    val statusLabel = routine.homeStatusLabel()
    val statusColor = routine.homeStatusColor()
    val objective = routine.objetivos.ifBlank { routine.descripcion }.ifBlank {
        "Tu entrenador no agregó un objetivo específico para esta rutina."
    }
    val plannedMetric = routine.homePlannedMetric()
    var objectiveExpanded by remember(routine.id, objective) { mutableStateOf(false) }
    var objectiveCanExpand by remember(routine.id, objective) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(statusColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DirectionsRun,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(21.dp)
                )
            }
            Text(
                "Rutina de hoy",
                color = AppTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            StatusBadge(statusLabel, statusColor)
            TodayRoutineActionButton(onClick = { onOpenRoutine(routine) })
        }
        Text(
            routine.titulo.ifBlank { "Rutina programada" },
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            modifier = Modifier.basicMarquee()
        )
    }

    Spacer(Modifier.height(14.dp))

    Text(
        text = "Objetivo",
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = objective,
        color = AppTextSecondary,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        maxLines = if (objectiveExpanded) Int.MAX_VALUE else 3,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = {
            if (!objectiveExpanded && it.hasVisualOverflow) {
                objectiveCanExpand = true
            }
        }
    )
    if (objectiveCanExpand) {
        TextButton(
            onClick = { objectiveExpanded = !objectiveExpanded },
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
        ) {
            Text(
                text = if (objectiveExpanded) "Ver menos" else "Ver más",
                color = PrimaryBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    Spacer(Modifier.height(14.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TodayRoutineMetricChip(
                value = plannedMetric.value,
                label = plannedMetric.label,
                icon = if (plannedMetric.label.equals("Distancia", ignoreCase = true)) Icons.Default.Straighten else Icons.Default.Timer,
                modifier = Modifier.weight(1f)
            )
            TodayRoutineMetricChip(
                value = routine.tipo_esfuerzo.ifBlank { "Sin dato" },
                label = "Esfuerzo",
                icon = Icons.Default.Bolt,
                modifier = Modifier.weight(1f)
            )
            TodayRoutineMetricChip(
                value = routine.tipo_terreno.ifBlank { "Sin dato" },
                label = "Terreno",
                icon = Icons.Default.Place,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TodayRoutineActionButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color(0xFF2F8CFF).copy(alpha = 0.18f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color(0xFF2F8CFF).copy(alpha = 0.55f))
    ) {
        Text(
            text = "Ir a rutina",
            color = Color(0xFF8EC5FF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun TodayRoutineMetricChip(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AppSurfaceMuted())
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            modifier = Modifier.basicMarquee()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF8EC5FF),
                modifier = Modifier.size(12.dp)
            )
            Text(
                label,
                color = AppTextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class HomePlannedMetric(
    val value: String,
    val label: String
)

private data class HomeMetric(
    val value: String,
    val label: String
)

private fun Routine.homeLocalDate(): LocalDate? {
    return fecha?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
}

private fun LocalDate.homeRelativeDateLabel(): String {
    val today = LocalDate.now()
    return when (this) {
        today.minusDays(1) -> "Ayer"
        today.plusDays(1) -> "Mañana"
        today -> "Hoy"
        else -> format(DateTimeFormatter.ofPattern("EEE d MMM", Locale("es", "CO")))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "CO")) else it.toString() }
    }
}

private fun Routine.homeHasStravaActivity(): Boolean {
    return isStravaSynced || stravaActivityId.isNotBlank() || strava?.actividad != null
}

private fun Routine.homeRoutineStatusPriority(): Int {
    return when (estado.trim().lowercase()) {
        "pendiente" -> 0
        "" -> 1
        "parcial", "parcialmente_realizada" -> 2
        "no_realizada", "no realizada" -> 3
        "realizada", "completada" -> 4
        else -> 5
    }
}

private fun Routine.homeStatusLabel(): String {
    return when (estado.trim().lowercase()) {
        "realizada", "completada" -> "Realizada"
        "parcial", "parcialmente_realizada" -> "Parcial"
        "no_realizada", "no realizada" -> "No realizada"
        "pendiente" -> "Pendiente"
        else -> estado.ifBlank { "Pendiente" }
    }
}

private fun Routine.homeStatusColor(): Color {
    return when (estado.trim().lowercase()) {
        "realizada", "completada" -> Color(0xFF62D9A8)
        "parcial", "parcialmente_realizada" -> Color(0xFFFFC857)
        "no_realizada", "no realizada" -> Color(0xFFFF6961)
        "pendiente", "" -> Color(0xFFFFC857)
        else -> PrimaryBlue
    }
}

private fun Routine.homePlannedMetric(): HomePlannedMetric {
    val measurement = tipo_medicion.trim().lowercase()
    val compliance = strava?.cumplimiento
    val distanceKm = compliance
        ?.takeIf { it.unidad.lowercase().contains("km") || it.unidad.lowercase().contains("m") }
        ?.let { current ->
            current.planificado?.let { value ->
                val unit = current.unidad.lowercase()
                if (unit.contains("m") && !unit.contains("km")) {
                    value / 1000.0
                } else {
                    value
                }
            }
        }
        ?: homeTotalDistanceKm()

    val minutes = compliance
        ?.takeIf { it.unidad.lowercase().contains("min") }
        ?.planificado
        ?.toInt()
        ?: homeTotalMinutes().toInt()

    return when {
        measurement.contains("dist") || distanceKm > 0.0 -> {
            HomePlannedMetric(
                value = if (distanceKm > 0.0) homeFormatKm(distanceKm) else "Sin distancia",
                label = "Distancia"
            )
        }
        measurement.contains("tiempo") || minutes > 0 -> {
            HomePlannedMetric(
                value = if (minutes > 0) homeFormatMinutes(minutes) else "Sin duración",
                label = "Duración"
            )
        }
        else -> HomePlannedMetric("Sin dato", "Duración")
    }
}

private fun Routine.homeActivityMetrics(): List<HomeMetric> {
    val activity = strava?.actividad
    val analysis = strava?.analisis
    val compliance = strava?.cumplimiento
    val plannedMetric = homePlannedMetric()

    val distanceKm = analysis?.distanciaKm
        ?: activity?.distance?.let { it / 1000.0 }
        ?: if (plannedMetric.label == "Distancia") plannedMetric.value.homeKmNumber() else null

    val durationSeconds = analysis?.tiempoMovimientoSeg
        ?: activity?.movingTime
        ?: activity?.elapsedTime

    val durationText = durationSeconds?.let { homeFormatMinutes((it / 60.0).roundToInt()) }
        ?: if (plannedMetric.label == "Duración") plannedMetric.value else "Sin dato"

    val statusText = compliance?.estado
        ?.replace("_", " ")
        ?.takeIf { it.isNotBlank() }
        ?: homeStatusLabel()

    return listOf(
        HomeMetric(
            value = distanceKm?.takeIf { it > 0.0 }?.let(::homeFormatKm)
                ?: if (plannedMetric.label == "Distancia") plannedMetric.value else "Sin dato",
            label = "Distancia"
        ),
        HomeMetric(durationText, "Tiempo"),
        HomeMetric(statusText, "Estado")
    )
}

private fun String.homeKmNumber(): Double? {
    return replace(",", ".")
        .substringBefore(" km")
        .trim()
        .toDoubleOrNull()
}

private fun Routine.homeTotalDistanceKm(): Double {
    if (!tipo_medicion.lowercase().contains("dist")) return 0.0
    return homePhaseDistance(sesiones_calentamiento) +
        homeCentralDistance(sesiones_central) +
        homePhaseDistance(sesiones_calma)
}

private fun Routine.homeTotalMinutes(): Double {
    if (tipo_medicion.lowercase().contains("dist")) return 0.0
    return homePhaseMinutes(sesiones_calentamiento) +
        homeCentralMinutes(sesiones_central) +
        homePhaseMinutes(sesiones_calma)
}

private fun homePhaseDistance(sessions: List<Map<String, Any>>?): Double {
    return sessions.orEmpty().sumOf { it.homeDistanceKm() }
}

private fun homePhaseMinutes(sessions: List<Map<String, Any>>?): Double {
    return sessions.orEmpty().sumOf { it.homeMinutes() }
}

private fun homeCentralDistance(central: Map<String, Any>?): Double {
    val series = central?.get("series") as? List<*> ?: return 0.0
    return series.filterIsInstance<Map<*, *>>().sumOf { serie ->
        val repetitions = (serie["repeticiones"] as? Number)?.toDouble() ?: 1.0
        val sessions = (serie["sesiones"] as? List<*>)
            ?.filterIsInstance<Map<*, *>>()
            .orEmpty()
        sessions.sumOf { it.homeDistanceKm() } * repetitions
    }
}

private fun homeCentralMinutes(central: Map<String, Any>?): Double {
    val series = central?.get("series") as? List<*> ?: return 0.0
    return series.filterIsInstance<Map<*, *>>().sumOf { serie ->
        val repetitions = (serie["repeticiones"] as? Number)?.toDouble() ?: 1.0
        val sessions = (serie["sesiones"] as? List<*>)
            ?.filterIsInstance<Map<*, *>>()
            .orEmpty()
        sessions.sumOf { it.homeMinutes() } * repetitions
    }
}

private fun Map<*, *>.homeDistanceKm(): Double {
    val excludedTypes = setOf("Flexibilidad", "Movilidad Articular", "Fortalecimiento")
    val type = this["tipo"]?.toString()?.trim().orEmpty()
    if (type in excludedTypes) return 0.0

    val rawDistance = when (val value = this["distancia"]) {
        is Number -> value.toDouble()
        is String -> value.replace(",", ".").toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
    val unit = this["tipo_medicion"]?.toString()?.lowercase().orEmpty()
    return if (unit == "metros" || unit == "m") rawDistance / 1000.0 else rawDistance
}

private fun Map<*, *>.homeMinutes(): Double {
    val minutes = (this["duracion_min"] as? Number)?.toDouble()
        ?: this["duracion_min"]?.toString()?.toDoubleOrNull()
        ?: 0.0
    val seconds = (this["duracion_seg"] as? Number)?.toDouble()
        ?: this["duracion_seg"]?.toString()?.toDoubleOrNull()
        ?: 0.0
    return minutes + seconds / 60.0
}

private fun homeFormatKm(value: Double): String {
    return if (value >= 10.0) {
        "${value.toInt()} km"
    } else {
        "${String.format(java.util.Locale.getDefault(), "%.1f", value)} km"
    }
}

private fun homeFormatMinutes(value: Int): String {
    return if (value >= 60) {
        val hours = value / 60
        val minutes = value % 60
        if (minutes == 0) "${hours} h" else "${hours} h ${minutes} min"
    } else {
        "$value min"
    }
}

@Composable
private fun LastActivityCard(
    isLoading: Boolean,
    routine: Routine?,
    errorMessage: String?
) {
    HomePremiumCard {
        LastActivityHeader(routine = routine)
        Spacer(Modifier.height(12.dp))
        when {
            isLoading -> HomeSectionMessage(
                icon = Icons.Default.Cloud,
                title = "Cargando actividad",
                message = "Estamos buscando tu actividad anterior.",
                accent = PrimaryBlue
            )

            errorMessage != null -> HomeSectionMessage(
                icon = Icons.Default.Info,
                title = "Actividad no disponible",
                message = errorMessage,
                accent = Color(0xFFFFC857)
            )

            routine == null -> HomeSectionMessage(
                icon = Icons.Default.EventAvailable,
                title = "Sin actividad anterior",
                message = "Aun no hay rutinas previas en tu calendario.",
                accent = Color(0xFF62D9A8)
            )

            else -> PreviousActivityContent(routine = routine)
        }
    }
}

@Composable
private fun LastActivityHeader(routine: Routine?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            Text(
                "Ultima actividad",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        routine?.let {
            StatusBadge(it.homeStatusLabel(), it.homeStatusColor())
        }
    }
}

@Composable
private fun PreviousActivityContent(routine: Routine) {
    val date = routine.homeLocalDate()
    val syncText = if (routine.homeHasStravaActivity()) "Sincronizada con Strava" else "Calendario MVT"
    val metrics = routine.homeActivityMetrics()

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            routine.titulo.ifBlank { "Rutina programada" },
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            modifier = Modifier.basicMarquee()
        )
        Text(
            listOfNotNull(date?.homeRelativeDateLabel(), syncText).joinToString(" · "),
            color = AppTextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        metrics.forEach { metric ->
            MetricChip(metric.value, metric.label, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CoachInsightCard(
    isLoading: Boolean,
    analysis: WeeklyRoutineAnalysis?,
    errorMessage: String?
) {
    HomePremiumCard(
        brush = Brush.linearGradient(
            listOf(Color(0xFF101824), Color(0xFF1C3147))
        )
    ) {
        CoachInsightHeader(analysis = analysis)
        Spacer(Modifier.height(12.dp))

        when {
            isLoading -> HomeSectionMessage(
                icon = Icons.Default.Cloud,
                title = "Analizando tu semana",
                message = "Estamos preparando una lectura breve para tu entrenamiento.",
                accent = PrimaryBlue
            )

            errorMessage != null -> HomeSectionMessage(
                icon = Icons.Default.Info,
                title = "Coach no disponible",
                message = errorMessage,
                accent = Color(0xFFFFC857)
            )

            analysis == null || !analysis.hasCoachInsightContent() -> HomeSectionMessage(
                icon = Icons.Default.Analytics,
                title = "Sin análisis todavía",
                message = "Cuando se genere tu análisis semanal, aparecerá aquí.",
                accent = Color(0xFF62D9A8)
            )

            else -> CoachInsightContent(analysis = analysis)
        }
    }
}

private fun WeeklyRoutineAnalysis.hasCoachInsightContent(): Boolean {
    return resumen.isNotBlank() ||
        foco_hoy.isNotBlank() ||
        balance.isNotBlank() ||
        mejora.isNotBlank() ||
        descanso.isNotBlank()
}

private fun WeeklyRoutineAnalysis.weekLabel(): String {
    val start = semana?.inicio?.takeIf { it.isNotBlank() }
    val end = semana?.fin?.takeIf { it.isNotBlank() }
    return if (start != null && end != null) {
        "Semana $start al $end"
    } else {
        fecha.takeIf { it.isNotBlank() }?.let { "Actualizado $it" }.orEmpty()
    }
}

private fun WeeklyRoutineMetrics.completedSessionsLabel(): String {
    val completed = realizadas + parciales
    return if (total > 0) "$completed/$total" else "0"
}

private fun String.coachStatusLabel(): String {
    return when (trim().lowercase()) {
        "rutina_hoy" -> "Hoy"
        "descanso_hoy" -> "Descanso"
        "sin_rutinas" -> "Sin rutinas"
        else -> replace("_", " ").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale("es", "CO")) else it.toString()
        }
    }
}

private fun String.coachStatusColor(): Color {
    return when (trim().lowercase()) {
        "rutina_hoy" -> PrimaryBlue
        "descanso_hoy" -> Color(0xFF62D9A8)
        "sin_rutinas" -> Color(0xFFFFC857)
        else -> PrimaryBlue
    }
}

@Composable
private fun CoachInsightHeader(analysis: WeeklyRoutineAnalysis?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(PrimaryBlue.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Whatshot,
                contentDescription = null,
                tint = Color(0xFFFFC857),
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = "Tu Estado",
            color = Color.White,
            fontSize = 15.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f)
        )
        analysis?.estado?.takeIf { it.isNotBlank() }?.let { status ->
            StatusBadge(status.coachStatusLabel(), status.coachStatusColor())
        }
    }
}

@Composable
private fun CoachInsightContent(analysis: WeeklyRoutineAnalysis) {
    val metrics = analysis.metricas
    val focus = analysis.foco_hoy.ifBlank { analysis.resumen }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = analysis.resumen.ifBlank { "Tu coach ya tiene una lectura de esta semana." },
            color = Color.White,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Bold
        )

        CoachFocusPanel(text = focus)

        if (metrics != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricChip(
                    value = metrics.completedSessionsLabel(),
                    label = "Avance",
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    value = metrics.cumplimiento_promedio?.let { "$it%" } ?: "Sin dato",
                    label = "Cumplimiento",
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    value = metrics.pendientes.toString(),
                    label = "Pendientes",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CoachAdviceChip(
                title = "Mejora",
                text = analysis.mejora.ifBlank { "Mantén técnica y control de intensidad." },
                accent = Color(0xFFFFC857),
                modifier = Modifier.weight(1f)
            )
            CoachAdviceChip(
                title = "Recupera",
                text = analysis.descanso.ifBlank { "Prioriza hidratación, movilidad y sueño." },
                accent = Color(0xFF62D9A8),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CoachFocusPanel(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PrimaryBlue.copy(alpha = 0.12f))
            .border(1.dp, PrimaryBlue.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Default.SportsScore,
            contentDescription = null,
            tint = Color(0xFF8EC5FF),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            color = Color.White,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CoachAdviceChip(
    title: String,
    text: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = text,
            color = AppTextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun NextRoutineCard(
    isLoading: Boolean,
    routine: Routine?,
    errorMessage: String?
) {
    HomePremiumCard {
        SectionTitle(icon = Icons.Default.CalendarMonth, title = "Proxima rutina")
        Spacer(Modifier.height(12.dp))
        when {
            isLoading -> HomeSectionMessage(
                icon = Icons.Default.Cloud,
                title = "Cargando rutina",
                message = "Estamos revisando tus proximos entrenamientos.",
                accent = PrimaryBlue
            )

            errorMessage != null -> HomeSectionMessage(
                icon = Icons.Default.Info,
                title = "Rutina no disponible",
                message = errorMessage,
                accent = Color(0xFFFFC857)
            )

            routine == null -> HomeSectionMessage(
                icon = Icons.Default.CheckCircle,
                title = "Sin proxima rutina",
                message = "No tienes entrenamientos futuros programados.",
                accent = Color(0xFF62D9A8)
            )

            else -> NextRoutineContent(routine = routine)
        }
    }
}

@Composable
private fun NextRoutineContent(routine: Routine) {
    val date = routine.homeLocalDate()
    val statusColor = routine.homeStatusColor()
    val objective = routine.objetivos.ifBlank { routine.descripcion }.ifBlank { "Objetivo por definir" }
    val plannedMetric = routine.homePlannedMetric()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(statusColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SportsScore, contentDescription = null, tint = statusColor, modifier = Modifier.size(24.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                date?.homeRelativeDateLabel() ?: "Fecha por definir",
                color = AppTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                routine.titulo.ifBlank { "Rutina programada" },
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                modifier = Modifier.basicMarquee()
            )
            Text(
                "Objetivo: $objective",
                color = AppTextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        MetricChip(plannedMetric.value, plannedMetric.label, Modifier.weight(1f))
        MetricChip(routine.tipo_esfuerzo.ifBlank { "Sin dato" }, "Esfuerzo", Modifier.weight(1f))
        MetricChip(routine.tipo_terreno.ifBlank { "Sin dato" }, "Terreno", Modifier.weight(1f))
    }
}

@Composable
private fun HomeSectionMessage(
    icon: ImageVector,
    title: String,
    message: String,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(message, color = AppTextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun HomePremiumCard(
    brush: Brush? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .then(
                if (brush != null) Modifier.background(brush) else Modifier.background(AppSurface)
            )
            .border(1.dp, AppBorder, RoundedCornerShape(22.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun SectionTitle(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun MetricChip(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AppSurfaceMuted())
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Text(
            value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            modifier = Modifier.basicMarquee()
        )
        Text(label, color = AppTextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AppSurfaceMuted(): Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)

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
private fun MissingRoutineScreen(
    loadFailed: Boolean,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.padding(20.dp),
            color = AppSurface,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, AppBorder),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    text = "Rutina no disponible",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (loadFailed) {
                        "No pudimos recuperar esta rutina. Vuelve al calendario e intenta abrirla nuevamente."
                    } else {
                        "La rutina no tiene un identificador válido."
                    },
                    color = AppTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Button(
                    onClick = onBackClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Volver al calendario")
                }
            }
        }
    }
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
