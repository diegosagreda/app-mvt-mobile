package com.example.mvt.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.example.mvt.R
import com.example.mvt.data.firebase.models.Routine
import com.example.mvt.data.firebase.repositories.RoutineRepository
import com.example.mvt.data.firebase.services.FirestoreService
import com.example.mvt.domain.usecases.GetRoutinesByAthleteUseCase
import com.example.mvt.goals.model.TrainingAvailability
import com.example.mvt.ui.components.SharedRoutineCard
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import com.example.mvt.ui.viewmodels.RoutineViewModel
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.yearMonth
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

private val CompletedGreen = Color(0xFF77DD77)
private val PartialOrange = Color(0xFFFFCA99)
private val MissedRed = Color(0xFFFF6961)
private val ScheduledPurple = Color(0xFFE5DDE6)
private val ExceededBlue = CompletedGreen
private val RestGray = Color(0xFF64748B)
private val StravaOrange = Color(0xFFFC4C02)
private const val SHOW_TRAINING_STREAK_HEADER = false

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RoutinesScreen(
    navController: NavController,
    onRoutineClick: () -> Unit,
    currentAthleteId: String,
    ritmos: Map<String, Any>?,
    zonas: Map<String, Any>?,
    athleteName: String = "Atleta",
    trainingAvailability: TrainingAvailability? = null,
    contentMode: RoutinesContentMode = RoutinesContentMode.CALENDAR
) {
    val routineRepository = remember { RoutineRepository(FirestoreService()) }
    val routineViewModel = remember {
        RoutineViewModel(
            GetRoutinesByAthleteUseCase(
                routineRepository
            )
        )
    }

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var scrollToSelectedRoutines by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
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
        if (currentAthleteId.isBlank() || contentMode != RoutinesContentMode.CALENDAR) return@LaunchedEffect

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
                    routineViewModel.loadRoutinesForMonth(currentAthleteId, visibleMonth.minusMonths(1))
                    routineViewModel.loadRoutinesForMonth(currentAthleteId, visibleMonth.plusMonths(1))
                } catch (e: Exception) {
                    Log.e("RoutinesScreen", "Error al cargar rutinas del mes $visibleMonth", e)
                }
            }
    }

    val routinesByDate by remember(routines) {
        derivedStateOf { routines.groupBy { it.localDate() }.filterKeys { it != null }.mapKeys { it.key!! } }
    }
    val visibleMonth = state.firstVisibleMonth.yearMonth
    val selectedRoutines = routinesByDate[selectedDate].orEmpty()
    LaunchedEffect(selectedDate, selectedRoutines.size, contentMode, scrollToSelectedRoutines) {
        if (scrollToSelectedRoutines && contentMode == RoutinesContentMode.CALENDAR && selectedRoutines.isNotEmpty()) {
            listState.animateScrollToItem(index = 3)
            scrollToSelectedRoutines = false
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 6.dp, end = 6.dp, top = 0.dp, bottom = 22.dp)
    ) {
        when (contentMode) {
            RoutinesContentMode.CALENDAR -> {
                item {
                    PerformanceCalendarHeader()
                }

                item {
                    PerformanceCalendarCard(
                        state = state,
                        routinesByDate = routinesByDate,
                        trainingAvailability = trainingAvailability,
                        selectedDate = selectedDate,
                        onDateSelected = {
                            selectedDate = it
                            scrollToSelectedRoutines = true
                            Log.d("RoutinesScreen", "Fecha seleccionada: $it")
                        }
                    )
                }

                if (selectedRoutines.isNotEmpty()) {
                    item {
                        SelectedRoutinesHeader(selectedDate = selectedDate)
                    }

                    items(selectedRoutines) { routine ->
                        SelectedRoutineCard(routine) {
                            onRoutineClick()
                            navController.navigate("routine_detail/${Uri.encode(routine.id)}")
                        }
                    }
                }
            }

            RoutinesContentMode.STATISTICS -> {
                item {
                    StatisticsContent(
                        athleteId = currentAthleteId,
                        routineRepository = routineRepository
                    )
                }
            }
        }
    }
}

enum class RoutinesContentMode {
    CALENDAR,
    STATISTICS
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun SelectedRoutinesHeader(selectedDate: LocalDate) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Rutinas del ${formatSpanishDayMonth(selectedDate)}",
            color = AppTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun PerformanceCalendarHeader() {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PrimaryBlue.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFF8EC5FF),
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Text(
                            text = "Tus rutinas",
                            color = AppTextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Planifica, revisa y ejecuta tus sesiones con una lectura clara de carga, objetivos y recuperación.",
                        color = AppTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakInsightStrip(summary: TrainingStreakSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = summary.color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, summary.color.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = summary.icon,
                    contentDescription = null,
                    tint = summary.color,
                    modifier = Modifier.size(18.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.title,
                        color = AppTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = summary.message,
                        color = AppTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StreakMiniMetric(
                    label = "Mejor",
                    value = "${summary.best} ent.",
                    modifier = Modifier.weight(1f)
                )
                StreakMiniMetric(
                    label = "Semana",
                    value = if (summary.plannedThisWeek == 0) "Sin plan" else "${summary.weeklyConsistency}%",
                    modifier = Modifier.weight(1f)
                )
                StreakMiniMetric(
                    label = "Cumplidas",
                    value = "${summary.completedThisWeek}/${summary.plannedThisWeek}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StreakMiniMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = AppSurfaceAlt.copy(alpha = 0.74f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = value,
                color = AppTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                color = AppTextSecondary,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SmartHeaderCard(
    label: String,
    value: String,
    detail: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = AppSurfaceAlt,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.34f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Surface(
                    color = accent.copy(alpha = 0.16f),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.padding(6.dp).size(15.dp)
                    )
                }
                Text(
                    text = label,
                    color = AppTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = value,
                    color = AppTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = detail,
                    color = AppTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private data class ReadinessSummary(
    val label: String,
    val detail: String,
    val color: Color,
    val icon: ImageVector
)

private data class TodayTrainingSummary(
    val title: String,
    val detail: String,
    val icon: ImageVector
)

private data class ObjectiveSummary(
    val title: String,
    val detail: String
)

private enum class TrainingStreakStatus {
    ACTIVE,
    AT_RISK,
    PROTECTED,
    BROKEN
}

private data class TrainingStreakSummary(
    val current: Int,
    val best: Int,
    val weeklyConsistency: Int,
    val completedThisWeek: Int,
    val plannedThisWeek: Int,
    val status: TrainingStreakStatus,
    val shortLabel: String,
    val title: String,
    val message: String,
    val color: Color,
    val icon: ImageVector
)

private fun calculateReadinessScore(
    todayInsight: DayPerformance,
    weekMetrics: WeeklyMetrics
): Int {
    val routineScore = when (todayInsight.status) {
        PerformanceStatus.COMPLETED, PerformanceStatus.EXCEEDED -> 22
        PerformanceStatus.SCHEDULED -> 18
        PerformanceStatus.PARTIAL -> 12
        PerformanceStatus.REST -> 14
        PerformanceStatus.MISSED -> 4
    }
    val loadScore = (weekMetrics.compliancePercent * 0.28f).toInt().coerceIn(0, 28)
    val streakScore = (weekMetrics.streakDays * 3).coerceAtMost(12)
    val intensityAdjustment = when {
        todayInsight.routines.isEmpty() -> 4
        todayInsight.intensity <= 2 -> 8
        todayInsight.intensity == 3 -> 5
        else -> 1
    }

    return (48 + routineScore + loadScore + streakScore + intensityAdjustment).coerceIn(42, 96)
}

private fun readinessStatus(score: Int): ReadinessSummary {
    return when {
        score >= 82 -> ReadinessSummary(
            label = "Óptimo",
            detail = "Buen momento para rendir",
            color = CompletedGreen,
            icon = Icons.Default.CheckCircle
        )
        score >= 64 -> ReadinessSummary(
            label = "Moderado",
            detail = "Controla la intensidad",
            color = PartialOrange,
            icon = Icons.Default.Speed
        )
        else -> ReadinessSummary(
            label = "Cauto",
            detail = "Prioriza recuperación",
            color = MissedRed,
            icon = Icons.Default.Bedtime
        )
    }
}

private fun buildTodayTrainingSummary(insight: DayPerformance): TodayTrainingSummary {
    val routine = insight.routines.firstOrNull()
    return if (routine == null) {
        TodayTrainingSummary(
            title = "Recuperación",
            detail = "Sin entrenamiento programado",
            icon = Icons.Default.Bedtime
        )
    } else {
        val duration = insight.timeText.takeIf { it != "Sin tiempo" }
        TodayTrainingSummary(
            title = routine.titulo.ifBlank { "Rutina de hoy" },
            detail = listOfNotNull(duration, routineStatusLabel(routine.estado)).joinToString(" · "),
            icon = sportIconFor(routine)
        )
    }
}

private fun buildObjectiveSummary(
    todayInsight: DayPerformance,
    monthInsights: List<DayPerformance>
): ObjectiveSummary {
    val objective = todayInsight.routines.firstOrNull { it.objetivos.isNotBlank() }?.objetivos
        ?: monthInsights.asSequence()
            .flatMap { it.routines.asSequence() }
            .firstOrNull { it.objetivos.isNotBlank() }
            ?.objetivos

    return if (objective.isNullOrBlank()) {
        ObjectiveSummary(
            title = "Objetivo activo",
            detail = "Define tu próxima meta"
        )
    } else {
        ObjectiveSummary(
            title = objective,
            detail = "Propósito del bloque"
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun buildTrainingStreakSummary(
    today: LocalDate,
    weekInsights: List<DayPerformance>,
    monthInsights: List<DayPerformance>
): TrainingStreakSummary {
    val todayInsight = monthInsights.firstOrNull { it.date == today }
        ?: buildDayPerformance(today, emptyList())
    val current = calculateCurrentTrainingStreak(today, monthInsights)
    val best = calculateBestTrainingStreak(today, monthInsights).coerceAtLeast(current)
    val plannedThisWeek = weekInsights.sumOf { it.routines.size }
    val completedThisWeek = weekInsights.sumOf { insight ->
        insight.routines.count { it.countsAsCompletedForStreak() }
    }
    val weeklyConsistency = if (plannedThisWeek == 0) {
        100
    } else {
        ((completedThisWeek.toDouble() / plannedThisWeek) * 100).toInt().coerceIn(0, 100)
    }
    val status = trainingStreakStatus(todayInsight)
    val nextCount = current + todayInsight.routines.count { it.isPendingForStreak() }

    return when (status) {
        TrainingStreakStatus.ACTIVE -> TrainingStreakSummary(
            current = current,
            best = best,
            weeklyConsistency = weeklyConsistency,
            completedThisWeek = completedThisWeek,
            plannedThisWeek = plannedThisWeek,
            status = status,
            shortLabel = "Racha activa",
            title = "Racha de entrenamiento activa",
            message = if (current >= best && current > 0) {
                "Estás igualando tu mejor racha. Mantén la constancia sin forzar la carga."
            } else {
                "Entrenamiento cumplido. Tu cadena de constancia sigue avanzando."
            },
            color = CompletedGreen,
            icon = Icons.Default.Whatshot
        )
        TrainingStreakStatus.AT_RISK -> TrainingStreakSummary(
            current = current,
            best = best,
            weeklyConsistency = weeklyConsistency,
            completedThisWeek = completedThisWeek,
            plannedThisWeek = plannedThisWeek,
            status = status,
            shortLabel = "En riesgo",
            title = "Tu racha está en juego",
            message = "Completa la rutina de hoy para llegar a $nextCount entrenamientos.",
            color = PartialOrange,
            icon = Icons.Default.Speed
        )
        TrainingStreakStatus.PROTECTED -> TrainingStreakSummary(
            current = current,
            best = best,
            weeklyConsistency = weeklyConsistency,
            completedThisWeek = completedThisWeek,
            plannedThisWeek = plannedThisWeek,
            status = status,
            shortLabel = "Protegida",
            title = "Racha protegida",
            message = "Hoy no hay rutina pendiente. Recuperar también sostiene el proceso.",
            color = PrimaryBlue,
            icon = Icons.Default.Bedtime
        )
        TrainingStreakStatus.BROKEN -> TrainingStreakSummary(
            current = current,
            best = best,
            weeklyConsistency = weeklyConsistency,
            completedThisWeek = completedThisWeek,
            plannedThisWeek = plannedThisWeek,
            status = status,
            shortLabel = "Perdida",
            title = "Racha por recuperar",
            message = "Puedes volver a construir momentum desde la próxima sesión planificada.",
            color = MissedRed,
            icon = Icons.Default.Flag
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun calculateCurrentTrainingStreak(
    today: LocalDate,
    monthInsights: List<DayPerformance>
): Int {
    var streak = 0
    monthInsights
        .filter { it.date <= today }
        .sortedByDescending { it.date }
        .forEach { insight ->
            when {
                insight.routines.isEmpty() -> Unit
                insight.routines.any { it.isPendingForStreak() } && insight.date == today -> Unit
                insight.routines.any { it.breaksStreak() || it.isPendingForStreak() } -> return streak
                else -> streak += insight.routines.count { it.countsAsCompletedForStreak() }
            }
        }
    return streak
}

@RequiresApi(Build.VERSION_CODES.O)
private fun calculateBestTrainingStreak(today: LocalDate, monthInsights: List<DayPerformance>): Int {
    var current = 0
    var best = 0
    monthInsights
        .sortedBy { it.date }
        .forEach { insight ->
            when {
                insight.routines.isEmpty() -> Unit
                insight.date > today -> Unit
                insight.routines.any { it.breaksStreak() || it.isPendingForStreak() } -> current = 0
                else -> {
                    current += insight.routines.count { it.countsAsCompletedForStreak() }
                    best = maxOf(best, current)
                }
            }
        }
    return best
}

private fun trainingStreakStatus(todayInsight: DayPerformance): TrainingStreakStatus {
    return when {
        todayInsight.routines.isEmpty() -> TrainingStreakStatus.PROTECTED
        todayInsight.routines.any { it.breaksStreak() } -> TrainingStreakStatus.BROKEN
        todayInsight.routines.any { it.isPendingForStreak() } -> TrainingStreakStatus.AT_RISK
        else -> TrainingStreakStatus.ACTIVE
    }
}

private fun Routine.countsAsCompletedForStreak(): Boolean {
    val normalized = estado.trim().lowercase()
    return completa ||
        normalized == "realizada" ||
        normalized == "completada" ||
        (strava?.cumplimiento?.porcentaje ?: 0.0) >= 100.0 ||
        strava?.actividad != null
}

private fun Routine.maintainsStreakWithoutAdding(): Boolean {
    val normalized = estado.trim().lowercase()
    return normalized == "parcial" || normalized == "parcialmente_realizada"
}

private fun Routine.isPendingForStreak(): Boolean {
    val normalized = estado.trim().lowercase()
    return !countsAsCompletedForStreak() &&
        !maintainsStreakWithoutAdding() &&
        normalized != "no_realizada" &&
        normalized != "no realizada" &&
        normalized != "perdida"
}

private fun Routine.breaksStreak(): Boolean {
    val normalized = estado.trim().lowercase()
    return normalized == "no_realizada" ||
        normalized == "no realizada" ||
        normalized == "perdida"
}

@Composable
private fun MonthRoutineStatusStrip(insights: List<DayPerformance>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurfaceAlt,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Resumen del mes",
                    color = AppTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                insights.take(31).forEach { insight ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(heatmapColor(insight))
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherLocationSummary(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }

    var city by remember { mutableStateOf("Cargando ubicación...") }
    var temperature by remember { mutableStateOf("--") }
    var description by remember { mutableStateOf("--") }
    var weatherIcon by remember { mutableStateOf(Icons.Default.Cloud) }

    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                @SuppressLint("MissingPermission")
                val location = fused.lastLocation.await()
                if (location != null) {
                    val (locationName, weather) = withContext(Dispatchers.IO) {
                        getLocationName(location.latitude, location.longitude) to
                            getCurrentWeather(location.latitude, location.longitude)
                    }
                    city = locationName
                    temperature = weather.first
                    description = weather.second
                    weatherIcon = weather.third
                } else {
                    city = "Ubicación no disponible"
                }
            } catch (e: Exception) {
                city = "Error obteniendo ubicación"
            }
        } else {
            city = "Permisos no concedidos"
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = AppSurfaceAlt,
                shape = CircleShape,
                border = BorderStroke(1.dp, AppBorder)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.padding(7.dp).size(17.dp)
                )
            }
            Text(
                text = city,
                color = AppTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee()
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = weatherIcon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = temperature,
                color = AppTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = description,
                color = AppTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .offset(y = (-1).dp)
                    .weight(1f)
                    .basicMarquee()
            )
        }
    }
}

@Composable
private fun HeaderMetric(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = PrimaryBlue,
    iconSize: Dp = 21.dp
) {
    Surface(
        modifier = modifier,
        color = AppSurfaceAlt,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(iconSize))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = value,
                    color = AppTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = label,
                    color = AppTextSecondary,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun PerformanceCalendarCard(
    state: CalendarState,
    routinesByDate: Map<LocalDate, List<Routine>>,
    trainingAvailability: TrainingAvailability?,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, AppBorder),
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(top = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MonthHeader(month = state.firstVisibleMonth, modifier = Modifier.padding(start = 12.dp))
            CalendarWeekLabels()
            HorizontalCalendar(
                state = state,
                dayContent = {},
                monthBody = { month, _ ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        month.weekDays.forEachIndexed { index, week ->
                            val weekRoutines = week.flatMap { day ->
                                routinesByDate[day.date].orEmpty()
                            }
                            val weekPlan = buildWeeklyPlan(weekRoutines)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                WeekLoadButton(
                                    weekIndex = index + 1,
                                    plan = weekPlan,
                                    modifier = Modifier.width(22.dp)
                                )

                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    week.forEach { day ->
                                        val dayRoutines = routinesByDate[day.date].orEmpty()
                                        Box(modifier = Modifier.weight(1f)) {
                                            PerformanceDayCell(
                                                day = day,
                                                insight = buildDayPerformance(day.date, dayRoutines),
                                                isAvailable = trainingAvailability
                                                    ?.takeIf { it.selectedCount > 0 }
                                                    ?.isAvailable(day.date.dayOfWeek) ?: true,
                                                isSelected = day.date == selectedDate,
                                                onClick = { onDateSelected(day.date) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                monthHeader = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WeekLoadButton(
    weekIndex: Int,
    plan: WeeklyPlanSummary,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clickable { expanded = true },
            color = PrimaryBlue.copy(alpha = 0.14f),
            shape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.42f))
        ) {
            Box(
                modifier = Modifier.padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Bolt,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = AppSurface,
            shape = RoundedCornerShape(14.dp)
        ) {
            WeeklyPlanPopup(plan = plan)
        }
    }
}

@Composable
private fun WeeklyPlanPopup(plan: WeeklyPlanSummary) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.DirectionsRun, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Plan semanal", color = AppTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Rutinas programadas", color = AppTextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WeeklyLoadPopupMetric("Rutinas", plan.routinesText, Modifier.weight(1f))
            WeeklyLoadPopupMetric("Distancia", plan.distanceText, Modifier.weight(1f))
            WeeklyLoadPopupMetric("Tiempo", plan.timeText, Modifier.weight(1f))
        }
    }
}

@Composable
private fun WeeklyLoadPopupMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, color = AppTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, color = AppTextSecondary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CalendarWeekLabels(modifier: Modifier = Modifier) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.width(22.dp))
        daysOfWeek(firstDayOfWeek = DayOfWeek.MONDAY).forEach { day ->
            Text(
                text = day.getDisplayName(java.time.format.TextStyle.SHORT, Locale("es", "CO")).take(1).uppercase(),
                color = AppTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun PerformanceDayCell(
    day: CalendarDay,
    insight: DayPerformance,
    isAvailable: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val enabled = day.position == DayPosition.MonthDate
    val alpha = if (enabled) 1f else 0.34f

    // Lógica unificada para días del mes y rellenos (coherencia)
    val hasContent = insight.routines.isNotEmpty() || !isAvailable
    val hasStatusColor = insight.status != PerformanceStatus.REST && hasContent
    val contentColor = AppTextPrimary.copy(alpha = alpha)
    val borderColor = when {
        isSelected -> PrimaryBlue
        hasStatusColor -> insight.accent.copy(alpha = alpha)
        else -> AppBorder
    }

    Box(
        modifier = Modifier
            .aspectRatio(.72f)
            .padding(2.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(AppSurfaceAlt.copy(alpha = alpha))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(13.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(5.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (hasContent) Arrangement.SpaceBetween else Arrangement.Top
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    color = contentColor,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )
                if (insight.hasStrava && hasContent) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_strava_mark),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(StravaOrange),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            if (hasContent) {
                ProgressRing(
                    progress = insight.progress,
                    color = insight.accent,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (insight.routines.isEmpty() && !isAvailable) Icons.Default.Bedtime else insight.sportIcon,
                        contentDescription = null,
                        tint = insight.accent,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Text(
                    text = insight.compactMetric,
                    color = contentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = insight.compactCompliance,
                    color = insight.accent.copy(alpha = alpha),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            val diameter = size.minDimension - stroke.width
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = Color.White.copy(alpha = 0.11f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
        }
        content()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthHeader(month: CalendarMonth, modifier: Modifier = Modifier) {
    val today = remember { LocalDate.now() }
    val isCurrentMonth = month.yearMonth == today.yearMonth
    val title = if (isCurrentMonth) {
        "${today.dayOfMonth} de ${formatSpanishMonthYear(month.yearMonth)}"
    } else {
        formatSpanishMonthYear(month.yearMonth)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = AppTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        StatusLegendMini()
    }
}

@Composable
private fun StatusLegendMini() {
    var expanded by remember { mutableStateOf(false) }
    val items = listOf(
        "Realizada" to CompletedGreen,
        "Parcial" to PartialOrange,
        "No realizada" to MissedRed,
        "Pendiente" to ScheduledPurple
    )

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable { expanded = true }
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (_, color) ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = AppSurface,
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    text = "Estados",
                    color = AppTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                items.forEach { (label, color) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(color, CircleShape)
                        )
                        Text(
                            text = label,
                            color = AppTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private enum class StatisticsFilter(val label: String) {
    CURRENT_MONTH("Mes"),
    LAST_WEEK("7 dias"),
    LAST_3_MONTHS("3 meses"),
    CUSTOM_RANGE("Rango")
}

private data class StatisticsDateRange(
    val start: LocalDate,
    val end: LocalDate,
    val label: String
)

private data class RoutineStatusStat(
    val label: String,
    val value: Int,
    val percent: Int,
    val color: Color,
    val icon: ImageVector
)

private data class RoutineStatistics(
    val totalRoutines: Int,
    val completed: Int,
    val pending: Int,
    val partial: Int,
    val missed: Int,
    val completionRate: Int,
    val statusItems: List<RoutineStatusStat>,
    val latestRoutines: List<Routine>
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun StatisticsContent(
    athleteId: String,
    routineRepository: RoutineRepository
) {
    var filter by remember { mutableStateOf(StatisticsFilter.CURRENT_MONTH) }
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var customStart by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var customEnd by remember { mutableStateOf(LocalDate.now()) }
    var routines by remember { mutableStateOf<List<Routine>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }
    var requestId by remember { mutableStateOf(0) }

    val dateRange = remember(filter, selectedMonth, customStart, customEnd) {
        buildStatisticsDateRange(filter, selectedMonth, customStart, customEnd)
    }
    val statistics = remember(routines) { buildRoutineStatistics(routines) }

    LaunchedEffect(athleteId, filter, selectedMonth, customStart, customEnd) {
        if (athleteId.isBlank()) {
            routines = emptyList()
            return@LaunchedEffect
        }

        val range = dateRange
        if (range == null) {
            routines = emptyList()
            return@LaunchedEffect
        }

        val currentRequest = requestId + 1
        requestId = currentRequest
        isLoading = true
        loadFailed = false

        runCatching {
            routineRepository.getRoutinesForStatistics(
                athleteId = athleteId,
                start = range.start.toStartTimestamp(),
                end = range.end.plusDays(1).toStartTimestamp(),
                onlyComplete = true
            )
        }.onSuccess { result ->
            if (requestId == currentRequest) {
                routines = result.filter { it.mostrar && it.localDate() != null }
                isLoading = false
            }
        }.onFailure { error ->
            Log.e("RoutinesStatistics", "Error cargando estadisticas", error)
            if (requestId == currentRequest) {
                routines = emptyList()
                loadFailed = true
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StatisticsHeroCard(
            dateLabel = formatHeaderDate(LocalDate.now()),
            statistics = statistics,
            isLoading = isLoading
        )
        StatisticsFilterPanel(
            selectedFilter = filter,
            selectedMonth = selectedMonth,
            customStart = customStart,
            customEnd = customEnd,
            onFilterSelected = { filter = it },
            onMonthSelected = { selectedMonth = it },
            onCustomStartSelected = { customStart = it },
            onCustomEndSelected = { customEnd = it }
        )

        when {
            isLoading -> StatisticsLoadingCard()
            statistics.totalRoutines == 0 -> StatisticsEmptyCard(loadFailed = loadFailed)
            else -> {
                StatisticsDistributionPanel(statistics = statistics)
                LatestRoutinesPanel(routines = statistics.latestRoutines)
            }
        }
    }
}

@Composable
private fun StatisticsHeroCard(
    dateLabel: String,
    statistics: RoutineStatistics,
    isLoading: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, AppBorder),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                StatisticsDonut(
                    items = statistics.statusItems,
                    total = statistics.totalRoutines,
                    modifier = Modifier.size(108.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isLoading) "--" else "${statistics.completionRate}%",
                        color = AppTextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                    Text(
                        text = "cumpl.",
                        color = AppTextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Progreso",
                        color = AppTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = PrimaryBlue.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.36f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                            Text(
                                text = dateLabel,
                                color = AppTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Text(
                    text = "Aqui veras tu avance, constancia y señales clave para ajustar mejor tu entrenamiento.",
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun StatisticsFilterPanel(
    selectedFilter: StatisticsFilter,
    selectedMonth: YearMonth,
    customStart: LocalDate,
    customEnd: LocalDate,
    onFilterSelected: (StatisticsFilter) -> Unit,
    onMonthSelected: (YearMonth) -> Unit,
    onCustomStartSelected: (LocalDate) -> Unit,
    onCustomEndSelected: (LocalDate) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(StatisticsFilter.entries) { item ->
                    FilterChipSurface(
                        label = item.label,
                        selected = selectedFilter == item,
                        onClick = { onFilterSelected(item) }
                    )
                }
            }

            if (selectedFilter == StatisticsFilter.CURRENT_MONTH) {
                val currentMonth = YearMonth.now()
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((1..12).toList()) { month ->
                        val option = YearMonth.of(currentMonth.year, month)
                        val enabled = option <= currentMonth
                        FilterChipSurface(
                            label = option.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale("es", "CO"))
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "CO")) else it.toString() },
                            selected = selectedMonth == option,
                            enabled = enabled,
                            onClick = { if (enabled) onMonthSelected(option) }
                        )
                    }
                }
            }

            if (selectedFilter == StatisticsFilter.CUSTOM_RANGE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DatePickerField(
                        label = "Inicio",
                        value = customStart,
                        onDateSelected = onCustomStartSelected,
                        modifier = Modifier.weight(1f)
                    )
                    DatePickerField(
                        label = "Final",
                        value = customEnd,
                        onDateSelected = onCustomEndSelected,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipSurface(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val color = if (selected) PrimaryBlue else AppSurfaceAlt
    Surface(
        color = if (enabled) color else AppSurfaceAlt.copy(alpha = 0.42f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (selected) PrimaryBlue.copy(alpha = 0.72f) else AppBorder),
        modifier = Modifier.clickable(enabled = enabled) { onClick() }
    ) {
        Text(
            text = label,
            color = if (enabled) AppTextPrimary else AppTextSecondary.copy(alpha = 0.55f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            maxLines = 1
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DatePickerField(
    label: String,
    value: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier.clickable {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
                },
                value.year,
                value.monthValue - 1,
                value.dayOfMonth
            ).show()
        },
        color = AppSurfaceAlt,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, color = AppTextSecondary, fontSize = 10.sp, maxLines = 1)
            Text(formatShortDate(value), color = AppTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun StatisticsLoadingCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                color = PrimaryBlue,
                strokeWidth = 3.dp,
                modifier = Modifier.size(30.dp)
            )
            Column {
                Text("Actualizando estadisticas", color = AppTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Consultando rutinas del periodo seleccionado.", color = AppTextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun StatisticsEmptyCard(loadFailed: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = AppSurfaceAlt, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppBorder)) {
                Icon(
                    imageVector = if (loadFailed) Icons.Default.Warning else Icons.Default.TrackChanges,
                    contentDescription = null,
                    tint = if (loadFailed) MissedRed else PrimaryBlue,
                    modifier = Modifier.padding(10.dp).size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (loadFailed) "No pudimos cargar los datos" else "Sin rutinas en este periodo",
                    color = AppTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (loadFailed) "Revisa tu conexion e intenta cambiar el filtro." else "Cuando tengas rutinas completadas aqui veras tu cumplimiento.",
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun StatisticsDistributionPanel(statistics: RoutineStatistics) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Distribucion por estado", color = AppTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("Lectura rapida del periodo activo", color = AppTextSecondary, fontSize = 12.sp)

            statistics.statusItems.forEach { item ->
                StatusProgressRow(item = item)
            }
        }
    }
}

@Composable
private fun StatisticsDonut(
    items: List<RoutineStatusStat>,
    total: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 13.dp.toPx(), cap = StrokeCap.Butt)
        val diameter = size.minDimension - stroke.width
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val arcSize = Size(diameter, diameter)

        drawArc(
            color = AppSurfaceAlt,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )

        if (total > 0) {
            var startAngle = -90f
            items.forEach { item ->
                val sweep = (item.value.toFloat() / total.toFloat()) * 360f
                if (sweep > 0f) {
                    drawArc(
                        color = item.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )
                    startAngle += sweep
                }
            }
        }
    }
}

@Composable
private fun StatusProgressRow(item: RoutineStatusStat) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(item.icon, null, tint = item.color, modifier = Modifier.size(17.dp))
            Text(item.label, color = AppTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${item.value} · ${item.percent}%", color = AppTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(AppSurfaceAlt)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((item.percent.takeIf { item.value > 0 }?.coerceAtLeast(4) ?: 0) / 100f)
                    .height(9.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(item.color)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun LatestRoutinesPanel(routines: List<Routine>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Actividad reciente", color = AppTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            routines.forEach { routine ->
                LatestRoutineRow(routine = routine)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun LatestRoutineRow(routine: Routine) {
    val status = normalizedRoutineStatus(routine.estado)
    val statusColor = routineStatusColor(status)
    val date = routine.localDate() ?: LocalDate.now()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = statusColor.copy(alpha = 0.14f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.38f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(date.dayOfMonth.toString().padStart(2, '0'), color = AppTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(
                    date.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale("es", "CO")).take(3).uppercase(Locale("es", "CO")),
                    color = AppTextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = routine.titulo.ifBlank { "Rutina personalizada" },
                color = AppTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = routine.tipo_esfuerzo.ifBlank { "Entrenamiento" },
                color = AppTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Surface(
            color = statusColor.copy(alpha = 0.14f),
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.34f))
        ) {
            Text(
                text = routineStatusLabel(status),
                color = statusColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                maxLines = 1
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun buildStatisticsDateRange(
    filter: StatisticsFilter,
    selectedMonth: YearMonth,
    customStart: LocalDate,
    customEnd: LocalDate
): StatisticsDateRange? {
    val today = LocalDate.now()
    return when (filter) {
        StatisticsFilter.CURRENT_MONTH -> {
            val month = if (selectedMonth > YearMonth.now()) YearMonth.now() else selectedMonth
            val end = if (month == YearMonth.now()) today else month.atEndOfMonth()
            StatisticsDateRange(
                start = month.atDay(1),
                end = end,
                label = formatDateRangeLabel(month.atDay(1), end)
            )
        }
        StatisticsFilter.LAST_WEEK -> StatisticsDateRange(
            start = today.minusDays(7),
            end = today,
            label = formatDateRangeLabel(today.minusDays(7), today)
        )
        StatisticsFilter.LAST_3_MONTHS -> {
            val startMonth = YearMonth.now().minusMonths(2)
            StatisticsDateRange(
                start = startMonth.atDay(1),
                end = today,
                label = formatDateRangeLabel(startMonth.atDay(1), today)
            )
        }
        StatisticsFilter.CUSTOM_RANGE -> {
            if (customStart > customEnd) null else StatisticsDateRange(
                start = customStart,
                end = customEnd,
                label = formatDateRangeLabel(customStart, customEnd)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun buildRoutineStatistics(source: List<Routine>): RoutineStatistics {
    val routines = source.filter { it.mostrar && it.localDate() != null }
    val total = routines.size
    val completed = routines.count { normalizedRoutineStatus(it.estado) == "Realizada" }
    val pending = routines.count { normalizedRoutineStatus(it.estado) == "Pendiente" }
    val partial = routines.count { normalizedRoutineStatus(it.estado) == "Parcial" }
    val missed = routines.count { normalizedRoutineStatus(it.estado) == "No_realizada" }
    val completion = if (total == 0) 0 else Math.round((completed.toFloat() / total.toFloat()) * 100f)

    fun percent(value: Int): Int = if (total == 0) 0 else Math.round((value.toFloat() / total.toFloat()) * 100f)

    val items = listOf(
        RoutineStatusStat("Realizadas", completed, percent(completed), CompletedGreen, Icons.Default.CheckCircle),
        RoutineStatusStat("Pendientes", pending, percent(pending), ScheduledPurple, Icons.Default.TrackChanges),
        RoutineStatusStat("Parciales", partial, percent(partial), PartialOrange, Icons.Default.Speed),
        RoutineStatusStat("No realizadas", missed, percent(missed), MissedRed, Icons.Default.Flag)
    )

    return RoutineStatistics(
        totalRoutines = total,
        completed = completed,
        pending = pending,
        partial = partial,
        missed = missed,
        completionRate = completion,
        statusItems = items,
        latestRoutines = routines.sortedByDescending { it.localDate() }.take(6)
    )
}

private fun normalizedRoutineStatus(status: String): String {
    val normalized = status.trim().lowercase()
    return when {
        normalized == "realizada" || normalized == "completada" -> "Realizada"
        normalized == "parcial" || normalized == "parcialmente_realizada" -> "Parcial"
        normalized == "no_realizada" || normalized == "no realizada" || normalized == "perdida" -> "No_realizada"
        else -> "Pendiente"
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun LocalDate.toStartTimestamp(): Timestamp {
    val instant = atStartOfDay(ZoneId.systemDefault()).toInstant()
    return Timestamp(Date.from(instant))
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatHeaderDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("dd MMMM", Locale("es", "CO")))
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatDateRangeLabel(start: LocalDate, end: LocalDate): String {
    return if (start == end) {
        formatFullDate(start)
    } else {
        "${formatFullDate(start)} - ${formatFullDate(end)}"
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatFullDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("es", "CO")))
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatShortDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "CO")))
        .replace(".", "")
}

@Composable
private fun WeeklyLoadCard(metrics: WeeklyMetrics) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AppBorder),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Carga semanal", color = AppTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(metrics.loadLabel, color = AppTextSecondary, fontSize = 12.sp)
                }
                Text("${metrics.compliancePercent}%", color = metrics.loadColor, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
            LinearProgressIndicator(
                progress = { (metrics.compliancePercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = metrics.loadColor,
                trackColor = AppSurfaceAlt
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LoadMetric("Distancia", metrics.distanceText, Icons.Default.Straighten, Modifier.weight(1f))
                LoadMetric("Tiempo", metrics.timeText, Icons.Default.Timer, Modifier.weight(1f))
                LoadMetric("Rutinas", "${metrics.completedRoutines}/${metrics.totalRoutines}", Icons.Default.CheckCircle, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LoadMetric(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = AppSurfaceAlt,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(17.dp))
            Column {
                Text(value, color = AppTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(label, color = AppTextSecondary, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun WeeklyStatusStrip(insights: List<DayPerformance>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Resumen semanal", color = AppTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                insights.forEach { insight ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = insight.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale("es", "CO")).take(1).uppercase(),
                            color = AppTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(insight.accent.copy(alpha = if (insight.status == PerformanceStatus.REST) 0.16f else 0.26f))
                                .border(1.dp, insight.accent.copy(alpha = 0.42f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = insight.statusIcon,
                                contentDescription = null,
                                tint = insight.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            Text(
                text = "Vista analitica de tu calendario de rutinas.",
                color = AppTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun SelectedDayPerformanceCard(
    selectedDate: LocalDate,
    insight: DayPerformance,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, insight.accent.copy(alpha = 0.36f)),
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = insight.accent.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = insight.sportIcon,
                        contentDescription = null,
                        tint = insight.accent,
                        modifier = Modifier.padding(10.dp).size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(formatSpanishDayMonth(selectedDate), color = AppTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(insight.detailTitle, color = AppTextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = AppTextSecondary
                )
            }

            if (expanded) {
                DifficultyBar(insight = insight)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayMetric("Distancia", insight.distanceText, Icons.Default.Straighten, Modifier.weight(1f))
                    DayMetric("Tiempo", insight.timeText, Icons.Default.Timer, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayMetric("Cumplimiento", insight.complianceText, Icons.Default.TrackChanges, Modifier.weight(1f))
                    DayMetric("Intensidad", insight.intensityText, Icons.Default.Bolt, Modifier.weight(1f))
                }
                if (insight.hasStrava) {
                    StravaInsightRow(insight)
                }
            }
        }
    }
}

@Composable
private fun DifficultyBar(insight: DayPerformance) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Dificultad", color = AppTextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(insight.difficultyLabel, color = insight.difficultyColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(7.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (index < insight.intensity) insight.difficultyColor else AppSurfaceAlt)
                )
            }
        }
    }
}

@Composable
private fun DayMetric(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = AppSurfaceAlt,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Row(
            modifier = Modifier.padding(11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
            Column {
                Text(value, color = AppTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(label, color = AppTextSecondary, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun StravaInsightRow(insight: DayPerformance) {
    Surface(
        color = Color(0x18FC4C02),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0x44FC4C02))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(StravaOrange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_strava_mark),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Strava sincronizado", color = AppTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(insight.stravaComparison, color = AppTextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun EmptyDayCard(selectedDate: LocalDate) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurfaceAlt,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = RestGray.copy(alpha = 0.16f), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Bedtime, null, tint = RestGray, modifier = Modifier.padding(10.dp).size(22.dp))
            }
            Column {
                Text("Día de recuperación", color = AppTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("No hay rutinas programadas para el ${formatSpanishDayMonth(selectedDate)}.", color = AppTextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun RoutineCard(routine: Routine, onClick: () -> Unit) {
    SharedRoutineCard(routine = routine, onClick = onClick)
}

@Composable
private fun SelectedRoutineCard(routine: Routine, onClick: () -> Unit) {
    val statusColor = routineStatusColor(routine.estado)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = AppSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.72f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Surface(
                    color = statusColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.44f))
                ) {
                    Icon(
                        imageVector = sportIconFor(routine),
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.padding(10.dp).size(24.dp)
                    )
                }
                if (routine.isStravaSynced || routine.strava != null) {
                    Surface(
                        color = StravaOrange.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, StravaOrange.copy(alpha = 0.38f))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_strava_mark),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(StravaOrange),
                            modifier = Modifier.padding(10.dp).size(24.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = routine.titulo.ifBlank { "Rutina programada" },
                        color = AppTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (routine.descripcion.isNotBlank()) {
                    Text(
                        text = routine.descripcion,
                        color = AppTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoutineInfoChip(
                        icon = routineMeasurementIcon(routine.tipo_medicion),
                        label = routineMeasurementLabel(routine.tipo_medicion),
                        modifier = Modifier.weight(1f)
                    )
                    RoutineInfoChip(
                        icon = Icons.Default.Bolt,
                        label = routine.tipo_esfuerzo.ifBlank { "Esfuerzo" },
                        modifier = Modifier.weight(1f)
                    )
                    RoutineInfoChip(
                        icon = routineStatusIcon(routine.estado),
                        label = routineStatusLabel(routine.estado),
                        modifier = Modifier.weight(1f),
                        tint = statusColor
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineInfoChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = PrimaryBlue
) {
    Surface(
        modifier = modifier,
        color = AppSurfaceAlt,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(13.dp))
            Text(
                text = label,
                color = AppTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee()
            )
        }
    }
}

private fun routineStatusColor(status: String): Color {
    return when {
        status.equals("Realizada", true) -> CompletedGreen
        status.equals("Parcial", true) -> PartialOrange
        status.equals("No_realizada", true) -> MissedRed
        else -> ScheduledPurple
    }
}

private fun routineStatusLabel(status: String): String {
    return when {
        status.equals("Realizada", true) -> "Realizada"
        status.equals("Parcial", true) -> "Parcial"
        status.equals("No_realizada", true) -> "No realizada"
        else -> "Pendiente"
    }
}

private fun routineStatusIcon(status: String): ImageVector {
    return when {
        status.equals("Realizada", true) -> Icons.Default.CheckCircle
        status.equals("Parcial", true) -> Icons.Default.Speed
        status.equals("No_realizada", true) -> Icons.Default.Flag
        else -> Icons.Default.TrackChanges
    }
}

private fun routineMeasurementIcon(value: String): ImageVector {
    val normalized = value.trim().lowercase()
    return when {
        normalized == "tiempo" || normalized.contains("min") || normalized.contains("hora") -> Icons.Default.Timer
        normalized == "m" || normalized == "km" || normalized.contains("metro") || normalized.contains("dist") -> Icons.Default.Straighten
        else -> Icons.Default.TrackChanges
    }
}

private fun routineMeasurementLabel(value: String): String {
    val normalized = value.trim().lowercase()
    return when {
        normalized == "tiempo" -> "Tiempo"
        normalized == "km" -> "Distancia km"
        normalized == "m" -> "Distancia m"
        normalized.contains("dist") -> "Distancia"
        normalized.isBlank() -> "Medición"
        else -> value.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "CO")) else it.toString() }
    }
}

private enum class PerformanceStatus {
    COMPLETED,
    PARTIAL,
    MISSED,
    SCHEDULED,
    EXCEEDED,
    REST
}

private data class DayPerformance(
    val date: LocalDate,
    val routines: List<Routine>,
    val status: PerformanceStatus,
    val progress: Float,
    val accent: Color,
    val sportIcon: ImageVector,
    val statusIcon: ImageVector,
    val compactMetric: String,
    val compactCompliance: String,
    val detailTitle: String,
    val primaryAction: String,
    val distanceText: String,
    val timeText: String,
    val complianceText: String,
    val intensity: Int,
    val intensityText: String,
    val difficultyLabel: String,
    val difficultyColor: Color,
    val hasStrava: Boolean,
    val stravaComparison: String
)

private data class WeeklyMetrics(
    val weekOfYear: Int,
    val totalRoutines: Int,
    val completedRoutines: Int,
    val compliancePercent: Int,
    val streakDays: Int,
    val distanceKm: Double,
    val minutes: Int,
    val distanceText: String,
    val timeText: String,
    val loadLabel: String,
    val loadColor: Color
)

private data class WeeklyPlanSummary(
    val totalRoutines: Int,
    val routinesText: String,
    val distanceText: String,
    val timeText: String
)

@RequiresApi(Build.VERSION_CODES.O)
private fun buildDayPerformance(date: LocalDate, routines: List<Routine>): DayPerformance {
    val mainRoutine = routines.firstOrNull { it.hasStravaResult() }
        ?: routines.firstOrNull { it.estado.equals("Realizada", true) }
        ?: routines.firstOrNull()
    val compliance = routines.mapNotNull { it.strava?.cumplimiento?.porcentaje }.maxOrNull()
    val fallbackProgress = when {
        routines.isEmpty() -> 0f
        routines.any { it.estado.equals("Realizada", true) } -> 1f
        routines.any { it.estado.equals("Parcial", true) } -> .55f
        routines.any { it.estado.equals("No_realizada", true) } -> 0f
        else -> .18f
    }
    val progress = ((compliance ?: (fallbackProgress * 100.0)) / 100.0).toFloat().coerceIn(0f, 1.25f)
    val status = when {
        routines.isEmpty() -> PerformanceStatus.REST
        (compliance ?: 0.0) > 100.0 -> PerformanceStatus.EXCEEDED
        routines.any { it.estado.equals("Realizada", true) } -> PerformanceStatus.COMPLETED
        routines.any { it.estado.equals("Parcial", true) } -> PerformanceStatus.PARTIAL
        routines.any { it.estado.equals("No_realizada", true) } -> PerformanceStatus.MISSED
        else -> PerformanceStatus.SCHEDULED
    }
    val accent = when (status) {
        PerformanceStatus.COMPLETED -> CompletedGreen
        PerformanceStatus.PARTIAL -> PartialOrange
        PerformanceStatus.MISSED -> MissedRed
        PerformanceStatus.SCHEDULED -> ScheduledPurple
        PerformanceStatus.EXCEEDED -> ExceededBlue
        PerformanceStatus.REST -> RestGray
    }
    val distanceKm = routines.sumOf { it.executedDistanceKm() ?: it.plannedDistanceKm() ?: 0.0 }
    val minutes = routines.sumOf { it.executedMinutes() ?: it.plannedMinutes() ?: 0 }
    val intensity = routines.maxOfOrNull { it.intensityLevel() } ?: 1
    val difficulty = difficultyFor(intensity)
    val sportIcon = sportIconFor(mainRoutine)
    val compactMetric = when {
        routines.isEmpty() -> "Descanso"
        distanceKm > 0.0 -> formatKm(distanceKm)
        minutes > 0 -> formatMinutes(minutes)
        else -> "${routines.size} rut."
    }
    val complianceText = if (routines.isEmpty()) "Descanso" else "${(progress * 100).toInt().coerceAtMost(125)}%"
    val detailTitle = when {
        routines.isEmpty() -> "Recuperación y adaptación"
        mainRoutine?.titulo?.isNotBlank() == true -> mainRoutine.titulo
        else -> "Rutina programada"
    }
    val primaryAction = when {
        routines.isEmpty() -> "Hoy no tienes rutinas programadas. Prioriza recuperación y movilidad."
        status == PerformanceStatus.EXCEEDED -> "Hoy superaste el objetivo. Revisa tu carga antes de sumar más esfuerzo."
        status == PerformanceStatus.COMPLETED -> "Hoy ya completaste tu sesión principal."
        status == PerformanceStatus.PARTIAL -> "Tienes una sesión parcial. Puedes revisar el detalle y completar el registro."
        status == PerformanceStatus.MISSED -> "Hay una rutina incumplida. Revisa el motivo y ajusta tu planificación."
        else -> "Tienes ${routines.size} rutina${if (routines.size == 1) "" else "s"} por realizar hoy."
    }

    return DayPerformance(
        date = date,
        routines = routines,
        status = status,
        progress = progress.coerceAtMost(1f),
        accent = accent,
        sportIcon = sportIcon,
        statusIcon = statusIconFor(status),
        compactMetric = compactMetric,
        compactCompliance = if (routines.isEmpty()) " " else "✓$complianceText",
        detailTitle = detailTitle,
        primaryAction = primaryAction,
        distanceText = if (distanceKm > 0.0) formatKm(distanceKm) else "Sin distancia",
        timeText = if (minutes > 0) formatMinutes(minutes) else "Sin tiempo",
        complianceText = complianceText,
        intensity = intensity,
        intensityText = "$intensity/5",
        difficultyLabel = difficulty.first,
        difficultyColor = difficulty.second,
        hasStrava = routines.any { it.isStravaSynced || it.strava != null },
        stravaComparison = buildStravaComparison(mainRoutine)
    )
}

private fun buildWeeklyPlan(routines: List<Routine>): WeeklyPlanSummary {
    val plannedDistance = routines.sumOf { it.plannedDistanceKm() ?: 0.0 }
    val plannedMinutes = routines.sumOf { it.plannedMinutes() ?: 0 }
    return WeeklyPlanSummary(
        totalRoutines = routines.size,
        routinesText = routines.size.toString(),
        distanceText = if (plannedDistance > 0.0) formatKm(plannedDistance) else "0 km",
        timeText = formatMinutes(plannedMinutes)
    )
}

@RequiresApi(Build.VERSION_CODES.O)
private fun buildWeeklyMetrics(insights: List<DayPerformance>): WeeklyMetrics {
    val total = insights.sumOf { it.routines.size }
    val completed = insights.sumOf { insight ->
        insight.routines.count {
            it.estado.equals("Realizada", true) ||
                (it.strava?.cumplimiento?.porcentaje ?: 0.0) >= 100.0
        }
    }
    val compliance = if (total == 0) 0 else ((completed.toDouble() / total) * 100).toInt().coerceIn(0, 125)
    val distance = insights.sumOf { insight ->
        insight.routines.sumOf { it.executedDistanceKm() ?: it.plannedDistanceKm() ?: 0.0 }
    }
    val minutes = insights.sumOf { insight ->
        insight.routines.sumOf { it.executedMinutes() ?: it.plannedMinutes() ?: 0 }
    }
    val streak = insights.takeWhile {
        it.status == PerformanceStatus.COMPLETED || it.status == PerformanceStatus.EXCEEDED
    }.size
    val loadColor = when {
        compliance >= 100 -> ExceededBlue
        compliance >= 75 -> CompletedGreen
        compliance >= 45 -> PartialOrange
        else -> MissedRed
    }
    val loadLabel = when {
        total == 0 -> "Semana sin carga programada"
        compliance >= 100 -> "Sobrecumplimiento controlado"
        compliance >= 75 -> "Carga óptima"
        compliance >= 45 -> "Carga moderada"
        else -> "Carga baja"
    }
    val weekNumber = insights.firstOrNull()?.date
        ?.get(java.time.temporal.WeekFields.of(Locale("es", "CO")).weekOfWeekBasedYear())
        ?: 0

    return WeeklyMetrics(
        weekOfYear = weekNumber,
        totalRoutines = total,
        completedRoutines = completed,
        compliancePercent = compliance,
        streakDays = streak,
        distanceKm = distance,
        minutes = minutes,
        distanceText = if (distance > 0) formatKm(distance) else "0 km",
        timeText = formatMinutes(minutes),
        loadLabel = loadLabel,
        loadColor = loadColor
    )
}

@RequiresApi(Build.VERSION_CODES.O)
private fun Routine.localDate(): LocalDate? {
    return fecha?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
}

private fun Routine.hasStravaResult(): Boolean {
    return strava?.actividad != null || strava?.cumplimiento != null || strava?.analisis != null
}

private fun Routine.executedDistanceKm(): Double? {
    return strava?.analisis?.distanciaKm
        ?: strava?.actividad?.distance?.let { it / 1000.0 }
        ?: strava?.cumplimiento?.takeIf { it.unidad.lowercase().contains("km") }?.realizado
}

private fun Routine.plannedDistanceKm(): Double? {
    return strava?.cumplimiento?.takeIf { it.unidad.lowercase().contains("km") }?.planificado
}

private fun Routine.executedMinutes(): Int? {
    return strava?.analisis?.tiempoMovimientoSeg?.let { it / 60 }
        ?: strava?.actividad?.movingTime?.let { it / 60 }
        ?: strava?.cumplimiento?.takeIf { it.unidad.lowercase().contains("min") }?.realizado?.toInt()
}

private fun Routine.plannedMinutes(): Int? {
    return strava?.cumplimiento?.takeIf { it.unidad.lowercase().contains("min") }?.planificado?.toInt()
}

private fun Routine.intensityLevel(): Int {
    val text = "$tipo_esfuerzo $titulo $descripcion".lowercase()
    return when {
        text.contains("compet") || text.contains("test") -> 5
        text.contains("intenso") || text.contains("interval") || text.contains("series") || text.contains("hiit") -> 4
        text.contains("moder") || text.contains("tempo") || text.contains("fartlek") -> 3
        text.contains("suave") || text.contains("recuper") || text.contains("facil") || text.contains("fácil") -> 1
        else -> 2
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun TrainingAvailability.isAvailable(dayOfWeek: DayOfWeek): Boolean {
    return when (dayOfWeek) {
        DayOfWeek.MONDAY -> monday
        DayOfWeek.TUESDAY -> tuesday
        DayOfWeek.WEDNESDAY -> wednesday
        DayOfWeek.THURSDAY -> thursday
        DayOfWeek.FRIDAY -> friday
        DayOfWeek.SATURDAY -> saturday
        DayOfWeek.SUNDAY -> sunday
    }
}

private fun sportIconFor(routine: Routine?): ImageVector {
    val text = "${routine?.titulo.orEmpty()} ${routine?.tipo_esfuerzo.orEmpty()} ${routine?.strava?.actividad?.sportType.orEmpty()} ${routine?.strava?.actividad?.type.orEmpty()}".lowercase()
    return when {
        text.contains("bici") || text.contains("bike") || text.contains("cycling") || text.contains("cicl") -> Icons.Default.DirectionsBike
        text.contains("swim") || text.contains("natac") -> Icons.Default.Pool
        text.contains("fuerza") || text.contains("gym") || text.contains("strength") -> Icons.Default.FitnessCenter
        else -> Icons.Default.DirectionsRun
    }
}

private fun statusIconFor(status: PerformanceStatus): ImageVector {
    return when (status) {
        PerformanceStatus.COMPLETED -> Icons.Default.CheckCircle
        PerformanceStatus.PARTIAL -> Icons.Default.Speed
        PerformanceStatus.MISSED -> Icons.Default.Flag
        PerformanceStatus.SCHEDULED -> Icons.Default.TrackChanges
        PerformanceStatus.EXCEEDED -> Icons.Default.Star
        PerformanceStatus.REST -> Icons.Default.Bedtime
    }
}

private fun difficultyFor(intensity: Int): Pair<String, Color> {
    return when (intensity) {
        1 -> "Fácil" to CompletedGreen
        2 -> "Base" to Color(0xFF4ADE80)
        3 -> "Moderado" to PartialOrange
        4 -> "Intenso" to MissedRed
        else -> "Competencia" to ScheduledPurple
    }
}

private fun heatmapColor(insight: DayPerformance): Color {
    return when (insight.status) {
        PerformanceStatus.COMPLETED -> CompletedGreen.copy(alpha = 0.78f)
        PerformanceStatus.EXCEEDED -> ExceededBlue.copy(alpha = 0.84f)
        PerformanceStatus.PARTIAL -> PartialOrange.copy(alpha = 0.82f)
        PerformanceStatus.MISSED -> MissedRed.copy(alpha = 0.78f)
        PerformanceStatus.SCHEDULED -> ScheduledPurple.copy(alpha = 0.52f)
        PerformanceStatus.REST -> AppSurfaceAlt
    }
}

private fun buildStravaComparison(routine: Routine?): String {
    val compliance = routine?.strava?.cumplimiento ?: return "Actividad vinculada al entrenamiento."
    val planned = compliance.planificado
    val executed = compliance.realizado
    val unit = compliance.unidad.ifBlank { compliance.metrica }
    val percent = compliance.porcentaje?.toInt()
    return when {
        planned != null && executed != null && percent != null ->
            "Planificado: ${formatNumber(planned)} $unit · Ejecutado: ${formatNumber(executed)} $unit · $percent%"
        percent != null -> "Cumplimiento calculado por Strava: $percent%"
        else -> "Actividad vinculada al entrenamiento."
    }
}

private fun getCurrentWeather(lat: Double, lon: Double): Triple<String, String, ImageVector> {
    return try {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
        val result = URL(url).readText()
        val current = JSONObject(result).getJSONObject("current_weather")
        val temp = "${current.getDouble("temperature").toInt()}°C"
        val code = current.getInt("weathercode")
        val (status, icon) = when (code) {
            0 -> "Despejado" to Icons.Default.WbSunny
            1, 2, 3 -> "Parcialmente nublado" to Icons.Default.WbCloudy
            45, 48 -> "Niebla" to Icons.Default.CloudQueue
            in 51..67 -> "Lluvia ligera" to Icons.Default.Umbrella
            in 71..77 -> "Nieve" to Icons.Default.Cloud
            in 80..82 -> "Lluvia moderada" to Icons.Default.Grain
            in 95..99 -> "Tormenta eléctrica" to Icons.Default.FlashOn
            else -> "Desconocido" to Icons.Default.Cloud
        }

        Triple(temp, status, icon)
    } catch (e: Exception) {
        Log.e("CLIMA_API_ERROR", "Error obteniendo clima: ${e.message}", e)
        Triple("--", "Error clima", Icons.Default.Cloud)
    }
}

private fun getLocationName(lat: Double, lon: Double): String {
    return try {
        val url = URL("https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$lat&lon=$lon")
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "com.example.mvt (Android App)")
        val text = conn.inputStream.bufferedReader().readText()
        conn.disconnect()
        val json = JSONObject(text).getJSONObject("address")
        json.optString("city", json.optString("town", json.optString("village", "Desconocido")))
    } catch (e: Exception) {
        "Desconocido"
    }
}

private fun formatKm(value: Double): String {
    return if (value >= 10) "${value.toInt()} km" else String.format(Locale.US, "%.1f km", value)
}

private fun formatMinutes(value: Int): String {
    if (value <= 0) return "0 min"
    val hours = value / 60
    val minutes = value % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes} min"
}

private fun formatNumber(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
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
