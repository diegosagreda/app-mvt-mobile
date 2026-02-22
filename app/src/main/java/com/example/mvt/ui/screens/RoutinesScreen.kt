package com.example.mvt.ui.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mvt.ui.components.header.RoutinesHeader
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

    LaunchedEffect(Unit) {
        Log.d("RoutinesScreen", "Iniciando carga de rutinas para ID: $currentAthleteId")
        try {
            routineViewModel.loadRoutines(currentAthleteId)
        } catch (e: Exception) {
            Log.e("RoutinesScreen", "Error al cargar rutinas", e)
        }
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FB))
    ) {
        RoutinesHeader() // ← componente movido

        Spacer(modifier = Modifier.height(12.dp))

        val state = rememberCalendarState(
            startMonth = startMonth,
            endMonth = endMonth,
            firstVisibleMonth = currentMonth,
            firstDayOfWeek = firstDayOfWeek
        )

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
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
    val finalColor = if (isSelected) PrimaryBlue.copy(alpha = 0.25f) else backgroundColor

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(finalColor, shape = RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) PrimaryBlue else Color.LightGray.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = day.position == DayPosition.MonthDate) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            color = if (day.position == DayPosition.MonthDate) Color.Black else Color.LightGray,
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
                    color = Color.Gray,
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
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsRun,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = routine.titulo,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                val descripcionCorta = if (routine.descripcion.length > 100) {
                    routine.descripcion.take(100) + "..."
                } else routine.descripcion
                Text(text = descripcionCorta, color = Color.Gray, fontSize = 14.sp)
            }
        }
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
