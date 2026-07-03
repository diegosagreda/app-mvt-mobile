package com.example.mvt.ui.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mvt.ui.components.CalendarDayCell
import com.example.mvt.ui.components.SharedRoutineCard
import com.example.mvt.ui.components.header.RoutinesHeader
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppSurface
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
import java.time.format.DateTimeFormatter
import java.util.Locale
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

                    DayCell(
                        day = day,
                        isSelected = day.date == selectedDate,
                        routines = dayRoutines,
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
fun DayCell(day: CalendarDay, isSelected: Boolean, routines: List<Routine>, onClick: () -> Unit) {
    val status = routines.firstOrNull { it.estado.equals("Realizada", true) }?.estado
        ?: routines.firstOrNull { it.estado.equals("Parcial", true) }?.estado
        ?: routines.firstOrNull { it.estado.equals("No_realizada", true) }?.estado
        ?: routines.firstOrNull { it.estado.equals("Pendiente", true) }?.estado
        ?: ""
    CalendarDayCell(
        isSelected = isSelected,
        dayNumber = day.date.dayOfMonth.toString(),
        enabled = day.position == DayPosition.MonthDate,
        status = status,
        hasRoutine = routines.isNotEmpty(),
        muted = day.position != DayPosition.MonthDate,
        onClick = onClick
    )
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
        text = "Rutinas del ${formatSpanishDayMonth(selectedDate)}",
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
    SharedRoutineCard(routine = routine, onClick = onClick)
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthHeader(month: CalendarMonth) {
    Text(
        text = formatSpanishMonthYear(month.yearMonth),
        color = PrimaryBlue,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatSpanishDayMonth(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es", "CO"))
    return date.format(formatter)
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatSpanishMonthYear(month: YearMonth): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "CO"))
    return month
        .atDay(1)
        .format(formatter)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "CO")) else it.toString() }
}
