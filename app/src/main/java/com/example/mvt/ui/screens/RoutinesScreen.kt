package com.example.mvt.ui.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.R
import androidx.navigation.NavController
import com.example.mvt.ui.components.header.RoutinesHeader
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppPrimarySoft
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import com.example.mvt.ui.viewmodels.RoutineViewModel
import com.example.mvt.domain.usecases.GetRoutinesByAthleteUseCase
import com.example.mvt.data.firebase.repositories.RoutineRepository
import com.example.mvt.data.firebase.services.FirestoreService
import com.example.mvt.data.firebase.models.Routine
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    navController: NavController,
    onRoutineClick: () -> Unit,
    currentAthleteId: String,
    ritmos: Map<String, Any>?,    // ← nuevo
    zonas: Map<String, Any>?      // ← nuevo
) {
    val routineViewModel = remember {
        RoutineViewModel(
            GetRoutinesByAthleteUseCase(
                RoutineRepository(FirestoreService())
            )
        )
    }

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val routines by routineViewModel.routines.collectAsState()

    LaunchedEffect(routines) {
        Log.d("RoutinesScreen", "Se recibieron ${routines.size} rutinas desde Firestore")
        routines.forEach {
            Log.d("RoutinesScreen", "Rutina: ${it.titulo} - Estado: ${it.estado} - Fecha: ${it.fecha} - ID: ${it.id}")
        }
    }

    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(3) }
    val endMonth = remember { currentMonth.plusMonths(3) }
    val firstDayOfWeek = remember { DayOfWeek.MONDAY }
    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    LaunchedEffect(currentAthleteId, state) {
        if (currentAthleteId.isBlank()) return@LaunchedEffect

        snapshotFlow { state.firstVisibleMonth.yearMonth }
            .distinctUntilChanged()
            .collectLatest { visibleMonth ->
                Log.d(
                    "RoutinesScreen",
                    "Cargando rutinas para ID: $currentAthleteId en mes visible: $visibleMonth"
                )

                if (selectedDate.yearMonth != visibleMonth) {
                    val targetDay = selectedDate.dayOfMonth.coerceAtMost(visibleMonth.lengthOfMonth())
                    selectedDate = visibleMonth.atDay(targetDay)
                }

                try {
                    routineViewModel.loadRoutinesForMonth(currentAthleteId, visibleMonth)
                } catch (e: Exception) {
                    Log.e("RoutinesScreen", "Error al cargar rutinas del mes $visibleMonth", e)
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        RoutinesHeader() // ← componente movido

        Spacer(modifier = Modifier.height(12.dp))

        CalendarCard(
            state = state,
            routines = routines,
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        RoutineListSection(
            routines = routines,
            selectedDate = selectedDate,
            onRoutineClick = onRoutineClick,
            navController = navController,
            ritmos = ritmos,
            zonas = zonas
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarCard(
    state: CalendarState,
    routines: List<Routine>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .shadow(3.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            MonthHeader(month = state.firstVisibleMonth)
            HorizontalCalendar(
                state = state,
                dayContent = { day ->
                    val dayRoutines = routines.filter {
                        val date = it.fecha?.toDate()?.toInstant()
                            ?.atZone(ZoneId.systemDefault())?.toLocalDate()
                        date == day.date
                    }

                    val dayColor = when {
                        dayRoutines.any { it.estado.equals("Realizada", ignoreCase = true) } -> Color(0xFF77DD77)
                        dayRoutines.any { it.estado.equals("Parcial", ignoreCase = true) } -> Color(0xFFFFCA99)
                        dayRoutines.any { it.estado.equals("No_realizada", ignoreCase = true) } -> Color(0xFFFF6961)
                        dayRoutines.any { it.estado.equals("Pendiente", ignoreCase = true) } -> Color(0xFFE5DDE6)
                        else -> Color.Transparent
                    }

                    DayCell(
                        day = day,
                        isSelected = day.date == selectedDate,
                        backgroundColor = dayColor,
                        onClick = {
                            onDateSelected(day.date)
                            Log.d("RoutinesScreen", "Fecha seleccionada: ${day.date}")
                        }
                    )
                },
                monthHeader = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DayCell(day: CalendarDay, isSelected: Boolean, backgroundColor: Color, onClick: () -> Unit) {
    val finalColor = when {
        isSelected -> AppPrimarySoft
        backgroundColor != Color.Transparent -> backgroundColor
        else -> AppSurfaceAlt
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(finalColor, shape = RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) PrimaryBlue else AppBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = day.position == DayPosition.MonthDate) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            color = if (day.position == DayPosition.MonthDate) AppTextPrimary else AppTextSecondary.copy(alpha = 0.45f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RoutineListSection(
    routines: List<Routine>,
    selectedDate: LocalDate,
    onRoutineClick: () -> Unit,
    navController: NavController,
    ritmos: Map<String, Any>?,
    zonas: Map<String, Any>?
) {
    val dayRoutines = routines.filter {
        val fecha = it.fecha?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
        fecha == selectedDate
    }

    Log.d("RoutinesScreen", "Rutinas encontradas para $selectedDate: ${dayRoutines.size}")

    Text(
        text = "Rutinas del ${selectedDate.dayOfMonth} ${selectedDate.month.name.lowercase().replaceFirstChar { it.uppercase() }}",
        color = PrimaryBlue,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        if (dayRoutines.isEmpty()) {
            item {
                Text(
                    "No hay rutinas programadas para este día.",
                    color = AppTextSecondary,
                    modifier = Modifier.padding(24.dp),
                    fontSize = 15.sp
                )
            }
        } else {
            items(dayRoutines) { routine ->
                RoutineCard(routine) {
                    navController.currentBackStackEntry?.savedStateHandle?.set("routine_selected", routine)
                    navController.currentBackStackEntry?.savedStateHandle?.set("ritmos", ritmos)
                    navController.currentBackStackEntry?.savedStateHandle?.set("zonas", zonas)
                    navController.navigate("routine_detail")
                }
            }
        }
    }
}

@Composable
fun RoutineCard(routine: Routine, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = AppPrimarySoft,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = routine.titulo,
                            fontWeight = FontWeight.Bold,
                            color = AppTextPrimary,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        val descripcionCorta = if (routine.descripcion.length > 92) {
                            routine.descripcion.take(92) + "..."
                        } else routine.descripcion

                        if (descripcionCorta.isNotBlank()) {
                            Text(
                                text = descripcionCorta,
                                color = AppTextSecondary,
                                fontSize = 14.sp,
                                lineHeight = 19.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoutineMetaChip(
                        icon = measurementIconFor(routine.tipo_medicion),
                        label = measurementLabelFor(routine.tipo_medicion)
                    )

                    if (routine.isStravaSynced) {
                        StravaSyncedChip()
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineMetaChip(
    icon: ImageVector,
    label: String
) {
    Surface(
        color = AppSurfaceAlt,
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppTextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = AppTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StravaSyncedChip() {
    Surface(
        color = Color(0x14FC4C02),
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FC4C02))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(0xFFFC4C02), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_strava_mark),
                    contentDescription = null,
                    modifier = Modifier.size(8.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
            Text(
                text = "Strava",
                color = Color(0xFFFFB28D),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun measurementIconFor(tipoMedicion: String): ImageVector {
    val normalized = tipoMedicion.trim().lowercase()
    return when {
        normalized == "tiempo" -> Icons.Default.Timer
        normalized == "m" || normalized == "km" || normalized.contains("metro") || normalized.contains("dist") ->
            Icons.Default.Straighten
        else -> Icons.Default.Route
    }
}

private fun measurementLabelFor(tipoMedicion: String): String {
    val normalized = tipoMedicion.trim().lowercase()
    return when {
        normalized == "tiempo" -> "Tiempo"
        normalized == "km" -> "Distancia km"
        normalized == "m" -> "Distancia m"
        normalized.isBlank() -> "Medicion"
        else -> tipoMedicion.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthHeader(month: CalendarMonth) {
    val title = "${month.yearMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${month.yearMonth.year}"
    Text(
        text = title,
        color = PrimaryBlue,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}
