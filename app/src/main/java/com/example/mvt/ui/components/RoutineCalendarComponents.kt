package com.example.mvt.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.R
import com.example.mvt.data.firebase.models.Routine
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppPrimarySoft
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue

data class RoutineWeekDay(
    val label: String,
    val dayNumber: String = "",
    val routine: Routine? = null,
    val locked: Boolean = false,
    val enabled: Boolean = true
)

@Composable
fun SharedRoutineCard(
    routine: Routine,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    locked: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (enabled && onClick != null) Modifier.clickable { onClick() } else Modifier
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .then(clickModifier),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface)
    ) {
        Row(
            modifier = Modifier
                .alpha(if (locked) .58f else 1f)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = AppPrimarySoft,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AppBorder)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (locked) Icons.Default.Lock else Icons.Default.DirectionsRun,
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
                Text(
                    text = routine.titulo.ifBlank { "Rutina programada" },
                    fontWeight = FontWeight.Bold,
                    color = AppTextPrimary,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val shortDescription = routine.descripcion.takeIf(String::isNotBlank)
                    ?.let { if (it.length > 92) it.take(92) + "..." else it }
                if (shortDescription != null) {
                    Text(
                        text = shortDescription,
                        color = AppTextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
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
fun RoutineWeekCalendar(
    title: String,
    days: List<RoutineWeekDay>,
    selectedIndex: Int,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    locked: Boolean = false
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AppSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AppBorder),
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = AppTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (locked) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Lock, null, tint = AppTextSecondary, modifier = Modifier.size(15.dp))
                        Text("Vista parcial", color = AppTextSecondary, fontSize = 11.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                days.forEachIndexed { index, day ->
                    RoutineWeekDayCell(
                        day = day,
                        selected = selectedIndex == index,
                        onClick = { onDaySelected(index) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun RoutineWeekDayCell(
    day: RoutineWeekDay,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasRoutine = day.routine != null
    val finalColor = when {
        hasRoutine -> Color(0xFF77DD77)
        else -> AppSurfaceAlt
    }

    Column(
        modifier = modifier
            .aspectRatio(.78f)
            .alpha(if (day.locked) .58f else 1f)
            .background(finalColor, shape = RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) PrimaryBlue else AppBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = day.enabled) { onClick() }
            .padding(horizontal = 4.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(day.label.take(3), color = AppTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        if (day.dayNumber.isNotBlank()) {
            Text(day.dayNumber, color = AppTextSecondary, fontSize = 10.sp, maxLines = 1)
        }
        Spacer(Modifier.size(4.dp))
        Box(
            Modifier
                .size(6.dp)
                .background(if (hasRoutine) PrimaryBlue else Color.Transparent, CircleShape)
        )
    }
}

@Composable
fun EmptyRoutineDayCard(
    title: String = "Día disponible",
    subtitle: String = "Sin sesión asignada",
    locked: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth().alpha(if (locked) .58f else 1f),
        color = AppSurfaceAlt,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = AppTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = AppTextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun CalendarDayCell(
    isSelected: Boolean,
    dayNumber: String,
    enabled: Boolean,
    status: String,
    hasRoutine: Boolean,
    muted: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor = statusColorFor(status, hasRoutine)
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
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayNumber,
            color = if (!muted) AppTextPrimary else AppTextSecondary.copy(alpha = 0.45f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

fun statusColorFor(status: String, hasRoutine: Boolean): Color = when {
    status.equals("Realizada", ignoreCase = true) -> Color(0xFF77DD77)
    status.equals("Parcial", ignoreCase = true) -> Color(0xFFFFCA99)
    status.equals("No_realizada", ignoreCase = true) -> Color(0xFFFF6961)
    status.equals("Pendiente", ignoreCase = true) || hasRoutine -> Color(0xFFE5DDE6)
    else -> Color.Transparent
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

@Composable
private fun RoutineMetaChip(
    icon: ImageVector,
    label: String
) {
    Surface(
        color = AppSurfaceAlt,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, AppBorder)
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
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StravaSyncedChip() {
    Surface(
        color = Color(0x14FC4C02),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color(0x33FC4C02))
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
