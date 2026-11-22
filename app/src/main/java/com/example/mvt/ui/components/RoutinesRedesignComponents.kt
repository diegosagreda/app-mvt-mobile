package com.example.mvt.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.data.firebase.models.Routine
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppPrimarySoft
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.OutDateStyle
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class RoutineCalendarView {
    MONTH,
    AGENDA
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RoutinesCalendarContent(
    modifier: Modifier = Modifier,
    state: CalendarState,
    routinesByDate: Map<LocalDate, List<Routine>>,
    selectedDate: LocalDate,
    view: RoutineCalendarView,
    onDateSelected: (LocalDate) -> Unit,
    onViewChanged: (RoutineCalendarView) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onRoutineClick: (Routine) -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(top = 12.dp),
        color = AppSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Column(
            modifier = Modifier.padding(
                start = 12.dp,
                top = 12.dp,
                end = 12.dp,
                bottom = 18.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RoutineMonthNavigation(
                month = state.firstVisibleMonth.yearMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth
            )
            RoutineViewToggle(view = view, onViewChanged = onViewChanged)

            if (view == RoutineCalendarView.MONTH) {
                RoutineMonthGrid(
                    state = state,
                    routinesByDate = routinesByDate,
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected
                )
            } else {
                RoutineAgenda(
                    month = state.firstVisibleMonth.yearMonth,
                    routinesByDate = routinesByDate,
                    onRoutineClick = onRoutineClick
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun RoutineMonthNavigation(
    month: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MonthNavigationButton(
            icon = Icons.Default.ChevronLeft,
            contentDescription = "Mes anterior",
            onClick = onPreviousMonth
        )
        MonthNavigationButton(
            icon = Icons.Default.ChevronRight,
            contentDescription = "Mes siguiente",
            onClick = onNextMonth
        )
        Text(
            text = month.format(
                DateTimeFormatter.ofPattern("LLLL yyyy", Locale("es", "CO"))
            ).replaceFirstChar { it.titlecase(Locale("es", "CO")) },
            color = AppTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MonthNavigationButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(38.dp)
            .clickable(onClick = onClick),
        color = PrimaryBlue,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription, tint = AppBackground, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun RoutineViewToggle(
    view: RoutineCalendarView,
    onViewChanged: (RoutineCalendarView) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurfaceAlt,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            RoutineViewToggleItem(
                label = "Mes",
                icon = Icons.Default.Event,
                selected = view == RoutineCalendarView.MONTH,
                onClick = { onViewChanged(RoutineCalendarView.MONTH) }
            )
            RoutineViewToggleItem(
                label = "Agenda",
                icon = Icons.Default.ViewAgenda,
                selected = view == RoutineCalendarView.AGENDA,
                onClick = { onViewChanged(RoutineCalendarView.AGENDA) }
            )
        }
    }
}

@Composable
private fun RowScope.RoutineViewToggleItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .weight(1f)
            .background(
                color = if (selected) PrimaryBlue else Color.Transparent,
                shape = RoundedCornerShape(9.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color.White else AppTextSecondary,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = label,
            color = if (selected) Color.White else AppTextSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ColumnScope.RoutineMonthGrid(
    state: CalendarState,
    routinesByDate: Map<LocalDate, List<Routine>>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, AppBorder, RoundedCornerShape(12.dp))
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("L", "M", "M", "J", "V", "S", "D").forEach { day ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .border(0.5.dp, AppBorder)
                        .background(AppSurfaceAlt),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        color = AppTextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        HorizontalCalendar(
            state = state,
            dayContent = {},
            monthHeader = {},
            monthBody = { month, _ ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    month.weekDays.forEach { week ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            week.forEach { day ->
                                RoutineCalendarDay(
                                    day = day,
                                    selected = day.date == selectedDate,
                                    hasRoutine = routinesByDate[day.date].orEmpty().isNotEmpty(),
                                    onClick = { onDateSelected(day.date) }
                                )
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

@Composable
private fun RowScope.RoutineCalendarDay(
    day: CalendarDay,
    selected: Boolean,
    hasRoutine: Boolean,
    onClick: () -> Unit
) {
    val inCurrentMonth = day.position == DayPosition.MonthDate
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .border(0.5.dp, AppBorder)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(
                    color = if (selected) PrimaryBlue else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = when {
                    selected -> Color.White
                    !inCurrentMonth -> AppTextSecondary.copy(alpha = 0.35f)
                    else -> AppTextPrimary
                },
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
        Spacer(Modifier.size(2.dp))
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(
                    if (hasRoutine && inCurrentMonth) PrimaryBlue else Color.Transparent,
                    CircleShape
                )
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ColumnScope.RoutineAgenda(
    month: YearMonth,
    routinesByDate: Map<LocalDate, List<Routine>>,
    onRoutineClick: (Routine) -> Unit
) {
    val agenda = routinesByDate
        .filterKeys { it.yearMonth == month }
        .toSortedMap()

    if (agenda.isEmpty()) {
        EmptyRoutineDayCard(
            title = "Sin rutinas programadas",
            subtitle = "No hay sesiones para ${month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale("es", "CO")))}."
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        agenda.forEach { (date, routines) ->
            item(key = "agenda-date-$date") {
                Text(
                    text = date.format(
                        DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "CO"))
                    ).replaceFirstChar { it.titlecase(Locale("es", "CO")) },
                    color = AppTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(
                items = routines.sortedBy { it.fecha?.toDate()?.time ?: Long.MAX_VALUE },
                key = { routine -> routine.id.ifBlank { "agenda-$date-${routine.hashCode()}" } }
            ) { routine ->
                SharedRoutineCard(
                    routine = routine,
                    onClick = { onRoutineClick(routine) }
                )
            }
        }
    }
}

private val LocalDate.yearMonth: YearMonth
    get() = YearMonth.from(this)

private fun Routine.localDate(): LocalDate? =
    fecha?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
