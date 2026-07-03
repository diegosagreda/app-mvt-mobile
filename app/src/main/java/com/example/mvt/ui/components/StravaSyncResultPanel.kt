package com.example.mvt.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.R
import com.example.mvt.data.firebase.models.RoutineStrava
import com.example.mvt.data.firebase.models.StravaActivity
import com.example.mvt.data.firebase.models.StravaCompliance
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppPrimarySoft
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppSurfaceMuted
import com.example.mvt.ui.theme.AppSuccess
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StravaSyncResultPanel(
    strava: RoutineStrava?,
    onSyncClick: () -> Unit = {},
    isSyncLoading: Boolean = false,
    onUnlinkClick: () -> Unit = {},
    isUnlinkLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hasLinkedActivity = strava?.actividad != null || strava?.cumplimiento != null || strava?.analisis != null
    if (strava == null || !hasLinkedActivity) {
        val emptyCopy = buildEmptyStateCopy(strava)

        Surface(
            modifier = modifier.fillMaxWidth(),
            color = AppSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.85f)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = AppSurfaceAlt,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.65f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Sincronizar actividad",
                            color = AppTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = emptyCopy,
                            color = AppTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Surface(
                    color = AppSurfaceAlt,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.65f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StravaActionButton(
                            text = "Sincronizar actividad",
                            icon = Icons.Default.Sync,
                            drawableRes = R.drawable.ic_strava_mark,
                            filled = true,
                            onClick = onSyncClick,
                            enabled = true,
                            isLoading = isSyncLoading,
                            containerColor = Color(0xFFFC4C02),
                            disabledContainerColor = Color(0xCCFC4C02),
                            disabledContentColor = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        )
                    }
                }
            }
        }
        return
    }

    val cumplimiento = strava.cumplimiento
    val actividad = strava.actividad
    val analisis = strava.analisis ?: buildFallbackAnalysis(
        activity = actividad,
        compliance = cumplimiento,
        fallbackSyncDate = strava.sincronizadoEn
    )
    val accentColor = stravaAccentColor(strava.estado)
    val porcentaje = (cumplimiento?.porcentaje ?: 0.0).coerceIn(0.0, 100.0)
    val objetivo = buildObjectiveText(cumplimiento)
    val cumplimientoTexto = buildComplianceText(cumplimiento)
    val distanciaTexto = formatDistanceKm(analisis?.distanciaKm, actividad?.distance)
    val tiempoTexto = formatDuration(analisis?.tiempoMovimientoSeg ?: actividad?.movingTime)
    val ritmoTexto = formatPace(analisis?.ritmoSegPorKm)
    val velocidadPromedioTexto = formatSpeedKmh(analisis?.velocidadPromedioKmh, actividad?.averageSpeed)
    val velocidadMaximaTexto = formatSpeedKmh(analisis?.velocidadMaximaKmh, actividad?.maxSpeed)
    val frecuenciaPromedioTexto = formatBpm(analisis?.frecuenciaPromedio, actividad?.averageHeartrate)
    val frecuenciaMaximaTexto = formatBpm(analisis?.frecuenciaMaxima, actividad?.maxHeartrate)
    val desnivelTexto = formatMeters(analisis?.desnivelM, actividad?.totalElevationGain)
    val fechaActividad = formatDateTime(
        actividad?.startDateLocal?.ifBlank { actividad.startDate } ?: actividad?.startDate.orEmpty()
    )
    val sincronizadoEn = formatDateTime(
        analisis?.sincronizadoEn
            ?.ifBlank { strava.sincronizadoEn }
            .orEmpty()
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AppSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.85f)),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(19.dp)
                            .background(Color(0xFFFC4C02), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_strava_mark),
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }
                    Text(
                        text = "Strava",
                        color = Color(0xFFFC4C02),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                StravaStatusChip(
                    label = stravaStatusLabel(strava),
                    accentColor = accentColor
                )
            }

            if (objetivo.isNotBlank()) {
                Text(
                    text = objetivo,
                    color = AppTextSecondary,
                    fontSize = 13.sp
                )
            }

            if (sincronizadoEn.isNotBlank()) {
                Text(
                    text = "Sincronizado en $sincronizadoEn",
                    color = AppTextSecondary,
                    fontSize = 12.sp
                )
            }

            Surface(
                color = AppSurfaceAlt,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.65f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Analisis MVT",
                                    color = AppTextSecondary,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = formatPercentage(porcentaje),
                                    color = accentColor,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = analisis?.estadoTexto
                                    ?.ifBlank {
                                        cumplimiento?.estado.orEmpty().replaceFirstChar { it.uppercase() }
                                    }
                                    .orEmpty()
                                    .ifBlank { "Sin analisis" },
                                color = AppTextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = analisis?.diferenciaTexto
                                    ?.ifBlank {
                                        analisis.lecturaEntrenador
                                    }
                                    .orEmpty()
                                    .ifBlank { "Aun no hay una lectura detallada para esta actividad." },
                                color = AppTextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (porcentaje / 100.0).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = accentColor,
                        trackColor = AppPrimarySoft
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StravaInsightCard(
                            title = "Actividad vinculada",
                            value = actividad?.name?.ifBlank { "Sin actividad asociada" } ?: "Sin actividad asociada",
                            detail = fechaActividad.ifBlank {
                                actividad?.sportType?.ifBlank { actividad.type } ?: actividad?.type.orEmpty()
                            },
                            icon = Icons.Default.Flag,
                            accentColor = Color(0xFF8B5D4B),
                            modifier = Modifier.fillMaxWidth()
                        )
                        StravaInsightCard(
                            title = "Cumplimiento",
                            value = cumplimientoTexto.ifBlank { "Sin resultado calculado" },
                            detail = analisis?.lecturaEntrenador.orEmpty(),
                            icon = Icons.Default.Timeline,
                            accentColor = Color(0xFF3D5A9B),
                            modifier = Modifier.fillMaxWidth()
                        )
                        StravaMetricRow(
                            left = { StravaMetricCard("Distancia", distanciaTexto, Icons.Default.Map, Color(0xFF8B5D4B)) },
                            right = { StravaMetricCard("Tiempo movimiento", tiempoTexto, Icons.Default.Timer, Color(0xFF3D5A9B)) }
                        )
                        StravaMetricRow(
                            left = { StravaMetricCard("Ritmo promedio", ritmoTexto, Icons.Default.FavoriteBorder, Color(0xFF8B5D4B)) },
                            right = { StravaMetricCard("Velocidad prom.", velocidadPromedioTexto, Icons.Default.TrendingUp, Color(0xFF2F6B59)) }
                        )
                        StravaMetricRow(
                            left = { StravaMetricCard("Velocidad max.", velocidadMaximaTexto, Icons.Default.Bolt, Color(0xFF8B5D4B)) },
                            right = { StravaMetricCard("FC promedio", frecuenciaPromedioTexto, Icons.Default.FavoriteBorder, Color(0xFF6D4258)) }
                        )
                        StravaMetricRow(
                            left = { StravaMetricCard("FC maxima", frecuenciaMaximaTexto, Icons.Default.FavoriteBorder, Color(0xFF6D4258)) },
                            right = { StravaMetricCard("Desnivel", desnivelTexto, Icons.Default.Landscape, Color(0xFF324B8B)) }
                        )
                    }
                }
            }

            if (strava.error.isNotBlank()) {
                Surface(
                    color = Color(0x20FC4C02),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0x33FC4C02))
                ) {
                    Text(
                        text = strava.error,
                        color = Color(0xFFFFB28D),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            Surface(
                color = AppSurfaceAlt,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.65f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Acciones de actividad",
                                color = AppTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Sincroniza nuevamente la actividad para actualizar el resultado de esta rutina.",
                                color = AppTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StravaActionButton(
                            text = "Sincronizar",
                            icon = Icons.Default.Sync,
                            drawableRes = R.drawable.ic_strava_mark,
                            filled = true,
                            onClick = onSyncClick,
                            enabled = true,
                            isLoading = isSyncLoading,
                            containerColor = Color(0xFFFC4C02),
                            disabledContainerColor = Color(0xCCFC4C02),
                            disabledContentColor = Color.White,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        )
                        StravaActionButton(
                            text = "Desvincular",
                            icon = Icons.Outlined.LinkOff,
                            filled = false,
                            onClick = onUnlinkClick,
                            enabled = true,
                            isLoading = isUnlinkLoading,
                            outlinedBorderColor = Color(0x66FC4C02),
                            outlinedContentColor = Color(0xFFFFB28D),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StravaMetricRow(
    left: @Composable () -> Unit,
    right: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) { left() }
        Box(modifier = Modifier.weight(1f)) { right() }
    }
}

@Composable
private fun StravaActionButton(
    text: String,
    icon: ImageVector,
    filled: Boolean,
    onClick: () -> Unit = {},
    enabled: Boolean = false,
    isLoading: Boolean = false,
    drawableRes: Int? = null,
    containerColor: Color = PrimaryBlue,
    disabledContainerColor: Color = PrimaryBlue.copy(alpha = 0.42f),
    disabledContentColor: Color = Color.White.copy(alpha = 0.92f),
    outlinedBorderColor: Color = Color(0x66FF4C7D),
    outlinedContentColor: Color = Color(0xFFFF9CB7),
    modifier: Modifier = Modifier
) {
    if (filled) {
        Button(
            onClick = onClick,
            enabled = enabled && !isLoading,
            modifier = modifier,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = Color.White,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else if (drawableRes != null) {
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled && !isLoading,
            modifier = modifier,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            border = BorderStroke(1.dp, outlinedBorderColor),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = outlinedContentColor,
                disabledContentColor = outlinedContentColor
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = outlinedContentColor,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

private fun buildEmptyStateCopy(strava: RoutineStrava?): String {
    if (strava?.error?.isNotBlank() == true) {
        return strava.error
    }

    return when (strava?.estado?.lowercase()) {
        "sin_actividad" ->
            "Aun no hay una actividad sincronizada para esta rutina. Lista tus actividades recientes de Strava y elige cual corresponde."
        "sin_registro", "registro_iniciado", "" ->
            "Vincula una actividad de Strava con esta rutina para visualizar cumplimiento, analisis y metricas del entrenamiento."
        else ->
            "Consulta tus actividades de Strava y selecciona la que quieras asociar a esta rutina."
    }
}

@Composable
private fun StravaStatusChip(
    label: String,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accentColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Text(
            text = label,
            color = accentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun StravaInsightCard(
    title: String,
    value: String,
    detail: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = AppSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(accentColor.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = title,
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee()
                )
                Text(
                    text = value.ifBlank { "--" },
                    color = AppTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee()
                )
                if (detail.isNotBlank()) {
                    Text(
                        text = detail,
                        color = AppTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee()
                    )
                }
            }
        }
    }
}

@Composable
private fun StravaMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.85f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(accentColor.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = title,
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee()
                )
                Text(
                    text = value,
                    color = AppTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee()
                )
            }
        }
    }
}

private fun stravaAccentColor(estado: String): Color {
    return when (estado.lowercase()) {
        "actividad_detectada" -> Color(0xFFFC7A3D)
        "registro_iniciado" -> PrimaryBlue
        "sin_actividad" -> Color(0xFFE0A23B)
        "error" -> Color(0xFFFF5A7A)
        else -> AppSuccess
    }
}

private fun stravaStatusLabel(strava: RoutineStrava): String {
    return when (strava.estado.lowercase()) {
        "actividad_detectada" -> "Conectado"
        "registro_iniciado" -> "Pendiente"
        "sin_actividad" -> "Sin actividad"
        "error" -> "Error"
        "sin_registro" -> "Sin registro"
        else -> "Strava"
    }
}

private fun buildFallbackAnalysis(
    activity: StravaActivity?,
    compliance: StravaCompliance?,
    fallbackSyncDate: String
): com.example.mvt.data.firebase.models.StravaAnalysis? {
    if (activity == null || compliance == null) return null

    val planificado = compliance.planificado ?: return null
    val realizado = compliance.realizado ?: return null
    val porcentaje = compliance.porcentaje ?: 0.0
    val unidad = compliance.unidad.ifBlank { compliance.metrica }
    val diferencia = realizado - planificado
    val tiempoMovimiento = activity.movingTime ?: activity.elapsedTime ?: 0
    val distanciaKm = (activity.distance ?: 0.0) / 1000.0
    val ritmoSegPorKm =
        if (distanciaKm > 0.0 && tiempoMovimiento > 0) tiempoMovimiento / distanciaKm else null

    return com.example.mvt.data.firebase.models.StravaAnalysis(
        estadoTexto = when {
            porcentaje >= 100.0 -> "Objetivo cumplido por completo"
            porcentaje >= 80.0 -> "Cumplimiento suficiente"
            else -> "Cumplimiento parcial"
        },
        lecturaEntrenador = when {
            porcentaje >= 100.0 -> "El deportista alcanzo o supero el objetivo previsto."
            porcentaje >= 80.0 -> "El deportista quedo dentro de un rango aceptable para la sesion."
            else -> "El resultado requiere revision del entrenador frente a carga, fatiga o adherencia."
        },
        diferencia = diferencia,
        diferenciaTexto = if (diferencia == 0.0) {
            "Exacto al objetivo planificado"
        } else {
            "${formatTrimmedNumber(kotlin.math.abs(diferencia))} $unidad ${
                if (diferencia > 0) "por encima" else "por debajo"
            } del plan"
        },
        distanciaKm = distanciaKm,
        tiempoMovimientoSeg = tiempoMovimiento,
        ritmoSegPorKm = ritmoSegPorKm,
        velocidadPromedioKmh = activity.averageSpeed?.times(3.6),
        velocidadMaximaKmh = activity.maxSpeed?.times(3.6),
        frecuenciaPromedio = activity.averageHeartrate?.let { kotlin.math.round(it) },
        frecuenciaMaxima = activity.maxHeartrate?.let { kotlin.math.round(it) },
        desnivelM = activity.totalElevationGain?.let { kotlin.math.round(it) },
        sincronizadoEn = fallbackSyncDate
    )
}

private fun buildObjectiveText(compliance: com.example.mvt.data.firebase.models.StravaCompliance?): String {
    if (compliance == null || compliance.planificado == null) return ""
    val unidad = if (compliance.unidad.isBlank()) compliance.metrica else compliance.unidad
    return "Objetivo MVT: ${formatPlannedValue(compliance.planificado, unidad)} $unidad planificados"
}

private fun buildComplianceText(compliance: com.example.mvt.data.firebase.models.StravaCompliance?): String {
    if (compliance == null) return ""
    val porcentaje = compliance.porcentaje?.let { formatPercentage(it) } ?: "--"
    val realizado = compliance.realizado?.let { formatPlannedValue(it, compliance.unidad) } ?: "--"
    val planificado = compliance.planificado?.let { formatPlannedValue(it, compliance.unidad) } ?: "--"
    val unidad = compliance.unidad.ifBlank { compliance.metrica }
    return "$porcentaje · $realizado de $planificado $unidad"
}

private fun formatPercentage(value: Double): String = "${value.toInt()}%"

private fun formatTrimmedNumber(value: Double): String {
    val rounded = String.format(Locale.US, "%.2f", value)
    return rounded
        .trimEnd('0')
        .trimEnd('.')
}

private fun formatPlannedValue(value: Double, unit: String): String {
    return if (unit == "min") {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}

private fun formatDistanceKm(analysisKm: Double?, activityMeters: Double?): String {
    val km = analysisKm ?: activityMeters?.div(1000.0) ?: return "--"
    return String.format(Locale.US, "%.2f km", km)
}

private fun formatDuration(seconds: Int?): String {
    if (seconds == null || seconds <= 0) return "--"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes} min"
    }
}

private fun formatPace(secondsPerKm: Double?): String {
    if (secondsPerKm == null || secondsPerKm <= 0) return "--"
    val totalSeconds = secondsPerKm.toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d min/km", minutes, seconds)
}

private fun formatSpeedKmh(speedKmh: Double?, speedMetersPerSec: Double?): String {
    val value = speedKmh ?: speedMetersPerSec?.times(3.6) ?: return "--"
    return String.format(Locale.US, "%.1f km/h", value)
}

private fun formatBpm(analysisBpm: Double?, activityBpm: Double?): String {
    val bpm = analysisBpm ?: activityBpm ?: return "--"
    return "${bpm.toInt()} bpm"
}

private fun formatMeters(analysisMeters: Double?, activityMeters: Double?): String {
    val meters = analysisMeters ?: activityMeters ?: return "--"
    return "${meters.toInt()} m"
}

private fun formatDateTime(raw: String): String {
    if (raw.isBlank()) return ""

    raw.toLongOrNull()?.let { millis ->
        return SimpleDateFormat("d/M/yyyy, h:mm a", Locale.getDefault())
            .format(Date(millis))
            .replace("AM", "a. m.")
            .replace("PM", "p. m.")
    }

    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    )

    patterns.forEach { pattern ->
        runCatching {
            SimpleDateFormat(pattern, Locale.US).parse(raw)
        }.getOrNull()?.let { date ->
            return SimpleDateFormat("d/M/yyyy, h:mm a", Locale.getDefault())
                .format(date)
                .replace("AM", "a. m.")
                .replace("PM", "p. m.")
        }
    }

    return raw
}
