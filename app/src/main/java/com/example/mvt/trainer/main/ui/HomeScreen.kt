package com.example.mvt.trainer.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.mvt.R
import com.example.mvt.data.firebase.models.User
import com.example.mvt.trainer.main.model.*
import com.example.mvt.trainer.main.viewmodel.TrainerDashboardUiState
import com.example.mvt.trainer.main.viewmodel.TrainerDashboardViewModel
import com.example.mvt.ui.theme.*

@Composable
fun HomeScreen(
    user: User?,
    viewModel: TrainerDashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        when (val state = uiState) {
            is TrainerDashboardUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PrimaryBlue
                )
            }
            is TrainerDashboardUiState.Content -> {
                HomeContent(
                    fullData = state.fullData,
                    selectedPlan = state.selectedPlan,
                    currentPlanData = state.currentPlanData,
                    user = user,
                    onPlanSelected = viewModel::selectPlan
                )
            }
            is TrainerDashboardUiState.Error -> {
                Text(
                    text = state.message,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    fullData: TrainerDashboardModel,
    selectedPlan: String,
    currentPlanData: TrainerPlanDashboard,
    user: User?,
    onPlanSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Dashboard Entrenador",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        item {
            TrainerHeroCard(
                fullData = fullData,
                selectedPlanName = selectedPlan,
                currentPlanData = currentPlanData,
                user = user,
                onPlanSelected = onPlanSelected
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                currentPlanData.stats.chunked(2).forEach { rowMetrics ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowMetrics.forEach { metric ->
                            MetricCard(metric, modifier = Modifier.weight(1f))
                        }
                        if (rowMetrics.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        if (currentPlanData.athletes.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = AppSurface,
                    border = BorderStroke(1.dp, AppBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ATLETAS ACTIVOS",
                            color = PrimaryBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Seguimiento prioritario",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        currentPlanData.athletes.forEachIndexed { index, athlete ->
                            PriorityAthleteRow(athlete)
                            if (index < currentPlanData.athletes.size - 1) {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainerHeroCard(
    fullData: TrainerDashboardModel,
    selectedPlanName: String,
    currentPlanData: TrainerPlanDashboard,
    user: User?,
    onPlanSelected: (String) -> Unit
) {
    val trainerName = listOfNotNull(user?.nombres, user?.apellidos)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { "Entrenador" }

    val discipline = user?.especialidad ?: user?.deporte ?: "Disciplina no definida"
    val location = listOfNotNull(user?.ciudadActual, user?.pais)
        .filter { it.isNotBlank() }
        .joinToString(", ")
        .ifBlank { "Ubicación no definida" }

    var expandedPlan by remember { mutableStateOf(false) }
    val plans = listOf(
        "Plata (${fullData.plataCount})",
        "Bronce (${fullData.bronceCount})"
    )
    val selectedPlanDisplay = if (selectedPlanName == "Plata") {
        "Plata (${fullData.plataCount})"
    } else {
        "Bronce (${fullData.bronceCount})"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = AppSurface,
        border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AppSurfaceAlt.copy(alpha = 0.4f), AppSurface)
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = AppSurfaceAlt,
                    border = BorderStroke(2.dp, PrimaryBlue.copy(alpha = 0.5f)),
                    modifier = Modifier.size(86.dp)
                ) {
                    if (!user?.foto_url.isNullOrBlank()) {
                        AsyncImage(
                            model = user?.foto_url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.iconografia_02_svg),
                            contentDescription = null,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "PANEL PRINCIPAL",
                        color = PrimaryBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = trainerName,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "$discipline · $location",
                        color = AppTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Selector de Plan
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PLAN",
                        color = AppTextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedPlan,
                        onExpandedChange = { expandedPlan = !expandedPlan },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            color = AppSurfaceAlt,
                            border = BorderStroke(1.dp, if(expandedPlan) PrimaryBlue else AppBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(selectedPlanDisplay, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Icon(if (expandedPlan) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = AppTextSecondary)
                            }
                        }

                        ExposedDropdownMenu(
                            expanded = expandedPlan,
                            onDismissRequest = { expandedPlan = false },
                            modifier = Modifier.background(AppSurfaceAlt)
                        ) {
                            plans.forEach { plan ->
                                DropdownMenuItem(
                                    text = { Text(plan, color = Color.White) },
                                    onClick = {
                                        val planName = if (plan.startsWith("Plata")) "Plata" else "Bronce"
                                        onPlanSelected(planName)
                                        expandedPlan = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Botón Deportistas
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable {},
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryBlue.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Deportistas",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroMiniMetric("${fullData.profileCompletion}%", "Perfil completo", Modifier.weight(1f))
                HeroMiniMetric(
                    "${currentPlanData.completedRoutinesRate}%",
                    "Rutinas realizadas",
                    Modifier.weight(1f)
                )
                HeroMiniMetric(currentPlanData.monitoredAthletes.toString(), "Atletas monitoreados", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroMiniMetric(value: String, label: String, modifier: Modifier) {
    Surface(
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(14.dp),
        color = AppSurfaceAlt.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(value, color = PrimaryBlue, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 12.sp)
        }
    }
}

@Composable
private fun MetricCard(metric: DashboardMetric, modifier: Modifier) {
    Surface(
        modifier = modifier.height(115.dp),
        shape = RoundedCornerShape(20.dp),
        color = AppSurface,
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(metric.accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(metric.icon, null, tint = metric.accentColor, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = metric.title,
                    color = AppTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(metric.value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(
                    text = metric.subValue,
                    color = AppTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PriorityAthleteRow(athlete: PriorityAthlete) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = AppSurfaceAlt.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AppSurface,
                modifier = Modifier.size(50.dp)
            ) {
                AsyncImage(
                    model = athlete.photoUrl.ifBlank { R.drawable.iconografia_02_svg },
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(athlete.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(athlete.discipline, color = AppTextSecondary, fontSize = 12.sp)
            }

            val isBronce = athlete.plan.contains("Bronce", ignoreCase = true)
            val badgeText = if (isBronce) "Revisar" else "Activo"
            val badgeColor = if (isBronce) AppTextSecondary else AppSuccess
            val badgeBg = if (isBronce) Color(0xFF35435A).copy(alpha = 0.8f) else AppSuccess.copy(alpha = 0.15f)

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = badgeBg,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = badgeText,
                    color = badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Surface(
                modifier = Modifier
                    .height(36.dp)
                    .clickable { /* TODO */ },
                shape = RoundedCornerShape(10.dp),
                color = PrimaryBlue.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Calendario",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
