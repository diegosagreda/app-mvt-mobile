package com.example.mvt.ui.components

import android.app.DatePickerDialog
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.R
import com.example.mvt.data.firebase.models.StravaActivity
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppPrimarySoft
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StravaActivityPickerSheet(
    activities: List<StravaActivity>,
    isLoading: Boolean,
    queryDate: LocalDate,
    searchQuery: String,
    selectedActivityId: String?,
    isSyncing: Boolean,
    message: String?,
    onDismiss: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onActivitySelected: (StravaActivity) -> Unit,
    onSyncSelected: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val dateLabel = remember(queryDate) {
        queryDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()))
    }
    val filteredActivities = remember(activities, searchQuery, queryDate) {
        activities
            .sortedByDescending(::activitySortKey)
            .filter { activity ->
                val activityDate = extractActivityDate(activity)
                val query = searchQuery.trim()
                val matchesQuery = if (query.isBlank()) {
                    true
                } else {
                    activity.name.contains(query, ignoreCase = true) ||
                        activity.sportType.contains(query, ignoreCase = true) ||
                        activity.type.contains(query, ignoreCase = true)
                }
                val matchesSelectedDate = activityDate == queryDate
                matchesSelectedDate && matchesQuery
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = AppSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Actividades Strava",
                        color = AppTextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Aqui puedes ver las actividades de la fecha seleccionada, elegir una y sincronizarla con esta rutina.",
                        color = AppTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(Color(0xFFFC4C02), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_strava_mark),
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = AppTextSecondary
                        )
                    },
                    placeholder = {
                        Text("Filtrar por nombre o tipo", color = AppTextSecondary)
                    },
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                onDateChange(LocalDate.of(year, month + 1, dayOfMonth))
                            },
                            queryDate.year,
                            queryDate.monthValue - 1,
                            queryDate.dayOfMonth
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AppBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTextPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color(0xFFFC4C02),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fecha base: $dateLabel")
                }
            }

            if (!message.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AppSurfaceAlt,
                    border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.7f))
                ) {
                    Text(
                        text = message,
                        color = AppTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFC4C02),
                                strokeWidth = 2.6.dp
                            )
                            Text(
                                text = "Cargando actividades...",
                                color = AppTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                filteredActivities.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (activities.isEmpty()) {
                                "No encontramos actividades para la fecha seleccionada."
                            } else {
                                "No hay actividades que coincidan con el filtro actual."
                            },
                            color = AppTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredActivities, key = { it.id }) { activity ->
                                StravaActivityOptionCard(
                                    activity = activity,
                                    selected = activity.id == selectedActivityId,
                                    onClick = { onActivitySelected(activity) }
                                )
                            }
                        }

                        Button(
                            onClick = onSyncSelected,
                            enabled = selectedActivityId != null && !isSyncing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFC4C02),
                                disabledContainerColor = Color(0x66FC4C02),
                                disabledContentColor = Color.White.copy(alpha = 0.82f)
                            )
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(15.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_strava_mark),
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    colorFilter = ColorFilter.tint(Color.White)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sincronizar actividad",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun StravaActivityOptionCard(
    activity: StravaActivity,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = AppBorder.copy(alpha = 0.7f)
    val selectionPulse = rememberInfiniteTransition(label = "strava_selection_pulse")
    val scale = if (selected) {
        selectionPulse.animateFloat(
            initialValue = 1f,
            targetValue = 1.018f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "strava_selection_scale"
        ).value
    } else {
        1f
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) AppPrimarySoft.copy(alpha = 0.22f) else AppSurfaceAlt
        ),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0x18FC4C02), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_strava_mark),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    colorFilter = ColorFilter.tint(Color(0xFFFC4C02))
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = activity.name.ifBlank { "Actividad Strava" },
                    color = AppTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee()
                )
                Text(
                    text = "${activityTypeLabel(activity)} · ${formatActivityDate(activity)}",
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee()
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = formatActivityDistance(activity.distance),
                    color = AppTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatActivityDuration(activity.movingTime ?: activity.elapsedTime),
                    color = AppTextSecondary,
                    fontSize = 12.sp
                )
                if (selected) {
                    Text(
                        text = "Seleccionada",
                        color = Color(0xFFFC4C02),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun activitySortKey(activity: StravaActivity): Long {
    val source = activity.startDateLocal.ifBlank { activity.startDate }
    if (source.isBlank()) return 0L

    return runCatching {
        OffsetDateTime.parse(source).toInstant().toEpochMilli()
    }.getOrElse {
        runCatching {
            LocalDateTime.parse(source.take(19))
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrDefault(0L)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatActivityDate(activity: StravaActivity): String {
    val source = activity.startDateLocal.ifBlank { activity.startDate }
    if (source.isBlank()) return "--"

    return runCatching {
        OffsetDateTime.parse(source)
            .toLocalDateTime()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy · hh:mm a", Locale.getDefault()))
    }.getOrElse {
        runCatching {
            LocalDateTime.parse(source.take(19))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy · hh:mm a", Locale.getDefault()))
        }.getOrDefault("--")
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun extractActivityDate(activity: StravaActivity): LocalDate? {
    val source = activity.startDateLocal.ifBlank { activity.startDate }
    if (source.isBlank()) return null

    return runCatching {
        OffsetDateTime.parse(source).toLocalDate()
    }.getOrElse {
        runCatching {
            LocalDateTime.parse(source.take(19)).toLocalDate()
        }.getOrNull()
    }
}

private fun activityTypeLabel(activity: StravaActivity): String {
    return activity.sportType.ifBlank {
        activity.type.ifBlank { "Actividad" }
    }
}

private fun formatActivityDistance(distanceMeters: Double?): String {
    val distance = distanceMeters ?: return "--"
    return String.format(Locale.US, "%.2f km", distance / 1000.0)
}

private fun formatActivityDuration(seconds: Int?): String {
    if (seconds == null || seconds <= 0) return "--"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes} min"
    }
}
