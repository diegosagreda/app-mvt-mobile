package com.example.mvt.trainer.dashboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.mvt.R
import com.example.mvt.trainer.dashboard.model.*
import com.example.mvt.trainer.dashboard.viewmodel.TrainerDashboardViewModel
import com.example.mvt.ui.theme.*
import com.example.mvt.ui.components.MvtLoadingOverlay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainerDashboardScreen(
    viewModel: TrainerDashboardViewModel = viewModel(),
    onNavigateToAthletes: () -> Unit = {},
    onNavigateToCalendar: (String) -> Unit = {},
    onNavigateToPersonalInfo: () -> Unit = {},
    onNavigateToPendingRequests: () -> Unit = {},
    onNavigateToRoutineLibrary: () -> Unit = {}
) {

    val uiState by viewModel.uiState.collectAsState()
    val planCounts by viewModel.planCounts.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadDashboardData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        when (val state = uiState) {
            is TrainerDashboardUiState.Loading -> {
                MvtLoadingOverlay()
            }
            is TrainerDashboardUiState.Empty -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No hay información disponible.", color = AppTextSecondary)
                }
            }
            is TrainerDashboardUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AppError,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        color = AppError,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadDashboardData() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Reintentar", color = AppTextPrimary)
                    }
                }
            }
            is TrainerDashboardUiState.Success -> {

                DashboardContent(
                    data = state.data,
                    planCounts = planCounts,
                    onNavigateToAthletes = onNavigateToAthletes,
                    onNavigateToCalendar = onNavigateToCalendar,
                    onNavigateToPersonalInfo = onNavigateToPersonalInfo,
                    onNavigateToPendingRequests = onNavigateToPendingRequests,
                    onNavigateToRoutineLibrary = onNavigateToRoutineLibrary,
                    onPlanSelected = viewModel::selectPlan,
                    onRefresh = { viewModel.loadDashboardData() }
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    data: TrainerDashboardData,
    planCounts: Map<String, Int> = emptyMap(),
    onNavigateToAthletes: () -> Unit,
    onNavigateToCalendar: (String) -> Unit,
    onNavigateToPersonalInfo: () -> Unit = {},
    onNavigateToPendingRequests: () -> Unit = {},
    onNavigateToRoutineLibrary: () -> Unit = {},
    onPlanSelected: (String) -> Unit = {},
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dashboard Entrenador",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = AppTextPrimary
                )
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Recargar",
                        tint = AppTextPrimary
                    )
                }
            }
        }


        item {
            TrainerProfileHeaderCard(
                profile = data.profile,
                planCounts = planCounts,
                onPlanSelected = onPlanSelected,
                onNavigateToAthletes = onNavigateToAthletes
            )
        }

        item {
            Text(text = "Indicadores Principales", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            MetricsGrid(metrics = data.metrics)
        }

        item(key = "active-athletes-section") {
            PriorityAthletesSection(
                athletes = data.priorityAthletes,
                onNavigateToCalendar = onNavigateToCalendar
            )
        }

        item(key = "sports-production-section") {
            CollapsibleDashboardSection(
                title = "PRODUCCIÓN DEPORTIVA",
                subtitle = "Rutinas creadas por deportista",
                icon = Icons.Default.Assignment
            ) {
                if (data.sportsProduction.isEmpty()) {
                    EmptySectionCard(message = "Aún no hay rutinas vinculadas a tus atletas.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        data.sportsProduction.forEach { production ->
                            SportsProductionCard(production = production)
                        }
                    }
                }
            }
        }

        item(key = "general-status-section") {
            CollapsibleDashboardSection(
                title = "ESTADO GENERAL",
                subtitle = "Flujo de rutinas",
                icon = Icons.Default.Assessment
            ) {
                RoutineFlowCard(flow = data.routineFlow)
            }
        }

        item(key = "pending-work-section") {
            CollapsibleDashboardSection(
                title = "TRABAJO PENDIENTE",
                subtitle = "Acciones recomendadas",
                icon = Icons.Default.PendingActions
            ) {
                if (data.recommendedActions.isEmpty()) {
                    EmptySectionCard(message = "No tienes acciones pendientes.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        data.recommendedActions.forEach { action ->
                            RecommendedActionCard(
                                action = action,
                                    onClick = when (action.actionType) {
                                        ActionType.PENDING_REQUEST -> onNavigateToPendingRequests
                                        ActionType.PENDING_ROUTINE -> onNavigateToRoutineLibrary
                                        ActionType.COMPLETE_PROFILE -> onNavigateToPersonalInfo
                                        ActionType.GENERAL -> ({})
                                    }
                                )
                            }
                    }
                }
            }
        }

        item(key = "recent-activity-section") {
            CollapsibleDashboardSection(
                title = "ACTIVIDAD RECIENTE",
                subtitle = "Últimas rutinas programadas",
                icon = Icons.Default.History
            ) {
                if (data.recentActivities.isEmpty()) {
                    EmptySectionCard(message = "Cuando crees rutinas, aparecerán aquí ordenadas por fecha.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        data.recentActivities.forEach { activity ->
                            RecentActivityCard(activity = activity)
                        }
                    }
                }
            }
        }

        item(key = "free-library-section") {
            CollapsibleDashboardSection(
                title = "BIBLIOTECA GRATUITA",
                subtitle = "Planes disponibles",
                icon = Icons.Default.LibraryBooks
            ) {
                if (data.availablePlans.isEmpty()) {
                    EmptySectionCard(message = "Aún no tienes planes gratuitos publicados.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        data.availablePlans.forEach { plan ->
                            PlanCard(plan = plan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleDashboardSection(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    initialExpanded: Boolean = true,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable { androidx.compose.runtime.mutableStateOf(initialExpanded) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AppIconMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppIconMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = subtitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTextPrimary
                    )
                }

                Icon(
                    imageVector = if (expanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = if (expanded) "Contraer sección" else "Expandir sección",
                    tint = AppTextSecondary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun PriorityAthletesSection(
    athletes: List<PriorityAthlete>,
    onNavigateToCalendar: (String) -> Unit
) {
    CollapsibleDashboardSection(
        title = "ATLETAS ACTIVOS",
        subtitle = "Seguimiento prioritario",
        icon = Icons.Default.People,
        initialExpanded = true
    ) {
        if (athletes.isEmpty()) {
            Text(
                text = "No hay deportistas para el filtro seleccionado.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 12.sp,
                color = AppTextSecondary
            )
        } else {
            athletes.forEach { athlete ->
                PriorityAthleteCard(
                    athlete = athlete,
                    onNavigateToCalendar = { onNavigateToCalendar(athlete.id) }
                )
            }
        }
    }
}

@Composable
private fun EmptySectionCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = AppBackground)
    ) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = message, fontSize = 12.sp, color = AppTextSecondary)
        }
    }
}

@Composable
private fun TrainerProfileHeaderCard(
    profile: TrainerProfileInfo,
    planCounts: Map<String, Int>,
    onPlanSelected: (String) -> Unit,
    onNavigateToAthletes: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {

                    if (!profile.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile.photoUrl,
                            contentDescription = "Cuenta",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.Transparent)
                        )
                    } else {

                        Image(
                            painter = painterResource(id = R.drawable.iconografia_02_svg),
                            contentDescription = "Cuenta",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = profile.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = AppTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = profile.discipline,
                        fontSize = 12.sp,
                        color = AppTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = profile.location.orEmpty(),
                        fontSize = 12.sp,
                        color = AppTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "PLAN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTextSecondary
                    )
                    PlanSelector(
                        profile = profile,
                        planCounts = planCounts,
                        onPlanSelected = onPlanSelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            //.widthIn(max = 132.dp)
                            .height(42.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)

                ) {
                    Text(
                        text = "",
                        fontSize = 10.sp
                    )

                    Button(
                        onClick = onNavigateToAthletes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AppTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Deportistas",
                            fontSize = 12.sp,
                            color = AppTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }



                }



            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = AppBorder)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ProfileStatItem(
                    label = "Perfil\ncompleto",
                    value = "${profile.profileCompletedPercentage}%",
                    modifier = Modifier.weight(1f)
                )
                ProfileStatItem(
                    label = "Rutinas\nrealizadas",
                    value = "${profile.routinesCompletedPercentage}%",
                    modifier = Modifier.weight(1f)
                )
                ProfileStatItem(
                    label = "Atletas monitoreados",
                    value = "${profile.monitoredAthletesCount}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PlanSelector(
    profile: TrainerProfileInfo,
    planCounts: Map<String, Int>,
    onPlanSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val selectedLabel = "${profile.selectedPlan} (${planCounts[profile.selectedPlan] ?: 0})"

    Box {
        Surface(
            modifier = modifier.clickable { expanded = true },
            shape = RoundedCornerShape(6.dp),
            color = AppSurfaceAlt,
            border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedLabel,
                    color = AppTextPrimary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Seleccionar plan",
                    tint = AppTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(AppSurface)
        ) {
            profile.availablePlans.forEach { plan ->
                val label = "$plan (${planCounts[plan] ?: 0})"
                DropdownMenuItem(
                    text = { Text(text = label, color = AppTextPrimary, fontSize = 12.sp) },
                    onClick = {
                        expanded = false
                        onPlanSelected(plan)
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                )
            }
        }
    }
}


@Composable
private fun ProfileStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, AppBorder, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryBlue)
            Text(
                text = label,
                fontSize = 10.sp,
                color = AppTextSecondary,
                lineHeight = 12.sp,
                maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun MetricsGrid(metrics: DashboardMetrics) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricMiniCard(title = "Atletas vinculados", count = metrics.linkedAthletesCount, subtitle = "${metrics.activeAthletesCount} activos", icon = Icons.Default.DirectionsRun, modifier = Modifier.weight(1f))
            MetricMiniCard(title = "Atletas activos", count = metrics.activeAthletesCount, subtitle = "Plan vigente o actividad", icon = Icons.Default.Favorite, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricMiniCard(title = "Rutinas creadas", count = metrics.createdRoutinesCount, subtitle = "${metrics.monthlyCreatedRoutinesCount} este mes", icon = Icons.Default.EventNote, modifier = Modifier.weight(1f))
            MetricMiniCard(title = "Rutinas pendientes", count = metrics.pendingRoutinesCount, subtitle = "En borrador o por cerrar", icon = Icons.Default.Timer, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricMiniCard(title = "Planes gratuitos", count = metrics.availableFreePlansCount, subtitle = "Plantillas disponibles", icon = Icons.Default.Layers, modifier = Modifier.weight(1f))
            MetricMiniCard(title = "Solicitudes pendientes", count = metrics.pendingRequestsCount, subtitle = "Por atender", icon = Icons.Default.GroupAdd, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricMiniCard(title: String, count: Int, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.border(1.dp, AppBorder, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = count.toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppTextPrimary)
                Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary, maxLines = 1)
                Text(text = subtitle, fontSize = 9.sp, color = AppTextSecondary, maxLines = 1)
            }
        }
    }
}



@Composable
fun AthleteAvatar(    photoUrl: String?,
                      name: String,
                      modifier: Modifier = Modifier.size(40.dp),
                      backgroundColor: Color = AccentRed
) {
    val initial = name.trim().take(1).uppercase()

    if (!photoUrl.isNullOrBlank()) {
        SubcomposeAsyncImage(
            model = photoUrl,
            contentDescription = "Foto de $name",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(CircleShape),
            loading = {
                AvatarFallback(initial = initial, modifier = modifier, backgroundColor = backgroundColor)
            },
            error = {
                AvatarFallback(initial = initial, modifier = modifier, backgroundColor = backgroundColor)
            }
        )
    } else {
        AvatarFallback(initial = initial, modifier = modifier, backgroundColor = backgroundColor)
    }
}

@Composable
private fun AvatarFallback(
    initial: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            color = AppTextPrimary, // Usando tu token de color
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PriorityAthleteCard(athlete: PriorityAthlete, onNavigateToCalendar: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = AppBackground)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AthleteAvatar(
                photoUrl = athlete.photoUrl,
                name = athlete.name,
                modifier = Modifier.size(40.dp),
                backgroundColor = AccentRed
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = athlete.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppTextPrimary)
                Text(text = "${athlete.discipline} - ${athlete.plan}", fontSize = 12.sp, color = AppTextSecondary)
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AppSuccess.copy(alpha = 0.2f),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(text = athlete.status, color = AppSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
            OutlinedButton(
                onClick = onNavigateToCalendar,
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryBlue)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Calendario", fontSize = 11.sp, color = PrimaryBlue)
            }
        }
    }
}

@Composable
private fun SportsProductionCard(production: AthleteSportsProduction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = AppBackground)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AthleteAvatar(
                    photoUrl = production.athletePhotoUrl,
                    name = production.athleteName,
                    modifier = Modifier.size(36.dp),
                    backgroundColor = AccentRed
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = production.athleteName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppTextPrimary, modifier = Modifier.weight(1f))
                Text(text = "${production.totalRoutines} rutinas", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AppTextPrimary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = production.progressPercentage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = AppSuccess,
                trackColor = AppSurfaceAlt
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${production.completedRoutines} realizadas - ${production.pendingRoutines} pendientes",
                fontSize = 11.sp,
                color = AppTextSecondary
            )
        }
    }
}

@Composable
private fun RoutineFlowCard(flow: RoutineFlowSummary) {

    val totalRoutines = flow.completedCount + flow.pendingCount + flow.notDoneCount + flow.draftCount

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            RoutineFlowItem(
                label = "Realizadas",
                count = flow.completedCount,
                totalRoutines = totalRoutines,
                color = AppSuccess
            )
            RoutineFlowItem(
                label = "Pendientes",
                count = flow.pendingCount,
                totalRoutines = totalRoutines,
                color = AccentRed
            )
            RoutineFlowItem(
                label = "No realizadas",
                count = flow.notDoneCount,
                totalRoutines = totalRoutines,
                color = AppError
            )
            RoutineFlowItem(
                label = "Borradores",
                count = flow.draftCount,
                totalRoutines = totalRoutines,
                color = AppTextSecondary
            )
        }
    }
}

@Composable
private fun RoutineFlowItem(
    label: String,
    count: Int,
    totalRoutines: Int,
    color: Color
) {

    val fraction = if (totalRoutines > 0) {
        (count.toFloat() / totalRoutines.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = count.toString(), style = MaterialTheme.typography.bodyMedium)
        }


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(AppBorder)
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(color)
                )
            }
        }
    }
}

@Composable
private fun RecommendedActionCard(
    action: RecommendedAction,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, AppBorder, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = AppBackground)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.PersonSearch, contentDescription = null, tint = PrimaryBlue)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = action.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppTextPrimary)
                Text(text = action.description, fontSize = 11.sp, color = AppTextSecondary)
            }
        }
    }
}

@Composable
private fun RecentActivityCard(activity: RecentRoutineActivity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = AppBackground)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = activity.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppTextPrimary)
                Text(text = activity.formattedDate, fontSize = 11.sp, color = AppTextSecondary)
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AppSurfaceAlt
            ) {
                Text(text = activity.status, color = AppTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun PlanCard(plan: AvailablePlan) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = AppBackground)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = plan.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppTextPrimary)
            Text(text = plan.description, fontSize = 11.sp, color = AppTextSecondary)
        }
    }
}
