package com.example.mvt.ui.screens.components.phases

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.ui.screens.components.model.TrainingPhase
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary

private data class CentralRenderGroup(
    val sesiones: List<Map<String, Any>>,
    val seriesOrdinal: Int?,
    val repetitions: Any?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhaseCardCentral(
    phase: TrainingPhase,
    central: Map<String, Any>,
    ritmos: Map<String, Any>?,
    zonas: Map<String, Any>?
) {
    var expanded by remember { mutableStateOf(true) }
    val groups = buildList {
        var seriesOrdinal = 0
        ((central["series"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList())
            .forEach { serie ->
            val sesiones = (serie["sesiones"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()
            if (sesiones.isEmpty()) return@forEach

            if (hasSeriesMarkers(sesiones)) {
                seriesOrdinal += 1
                add(
                    CentralRenderGroup(
                        sesiones = sesiones,
                        seriesOrdinal = seriesOrdinal,
                        repetitions = serie["repeticiones"] ?: 1
                    )
                )
            } else {
                add(
                    CentralRenderGroup(
                        sesiones = sesiones,
                        seriesOrdinal = null,
                        repetitions = null
                    )
                )
            }
        }
    }
    val totalSeries = groups.count { it.seriesOrdinal != null }
    val totalExercises = groups.sumOf { it.sesiones.size }
    val expandedSeries = remember(totalSeries) {
        mutableStateListOf<Boolean>().apply { repeat(totalSeries) { add(true) } }
    }
    val accent = AppTextSecondary.copy(alpha = 0.95f)
    val repeatAccent = Color(0xFF7FD6FF)
    val repeatSurface = Color(0xFF123B5A)
    val subtitle = buildString {
        append(if (totalSeries == 1) "1 serie" else "$totalSeries series")
        append(" · ")
        append(if (totalExercises == 1) "1 ejercicio" else "$totalExercises ejercicios")
        append(" · ")
        append(if (phase.recursos.size == 1) "1 recurso" else "${phase.recursos.size} recursos")
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder.copy(alpha = 0.8f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = AppSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppSurface)
                .padding(horizontal = 8.dp, vertical = 14.dp)
        ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(AppSurfaceAlt.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                            .border(1.dp, AppBorder.copy(alpha = 0.72f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = phase.icono,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = phase.nombre,
                            color = AppTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = subtitle,
                            color = AppTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = AppSurfaceAlt.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.75f))
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier
                                .padding(9.dp)
                                .size(11.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        var expandedSeriesIndex = 0
                        groups.forEach { group ->
                            if (group.seriesOrdinal != null) {
                                val currentSeriesIndex = expandedSeriesIndex
                                val isExpanded = expandedSeries.getOrElse(currentSeriesIndex) { true }
                                expandedSeriesIndex += 1

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    color = AppSurfaceAlt.copy(alpha = 0.58f),
                                    border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.72f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { expandedSeries[currentSeriesIndex] = !isExpanded },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .background(AppSurface.copy(alpha = 0.74f), RoundedCornerShape(14.dp))
                                                    .border(1.dp, AppBorder.copy(alpha = 0.72f), RoundedCornerShape(14.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Repeat, null, tint = accent, modifier = Modifier.size(11.dp))
                                            }

                                            Spacer(Modifier.width(10.dp))

                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Serie ${group.seriesOrdinal}",
                                                    color = AppTextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                                Spacer(Modifier.weight(1f))
                                                Surface(
                                                    shape = RoundedCornerShape(999.dp),
                                                    color = repeatSurface.copy(alpha = 0.96f),
                                                    border = BorderStroke(1.dp, repeatAccent.copy(alpha = 0.34f))
                                                ) {
                                                    Text(
                                                        text = "Repetir ×${group.repetitions ?: 1} veces",
                                                        color = repeatAccent,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.width(10.dp))

                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = AppSurface.copy(alpha = 0.72f),
                                                border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.72f))
                                            ) {
                                                Icon(
                                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = accent,
                                                    modifier = Modifier
                                                        .padding(8.dp)
                                                        .size(10.dp)
                                                )
                                            }
                                        }

                                        AnimatedVisibility(
                                            visible = isExpanded,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            CentralSessionList(
                                                sesiones = group.sesiones,
                                                ritmos = ritmos,
                                                zonas = zonas,
                                                accent = accent
                                            )
                                        }
                                    }
                                }
                            } else {
                                CentralSessionList(
                                    sesiones = group.sesiones,
                                    ritmos = ritmos,
                                    zonas = zonas,
                                    accent = accent
                                )
                            }
                        }
                    }
                }
            }
    }
}

@Composable
private fun CentralSessionList(
    sesiones: List<Map<String, Any>>,
    ritmos: Map<String, Any>?,
    zonas: Map<String, Any>?,
    accent: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        sesiones.forEachIndexed { index, sesion ->
            val tipo = sesion["tipo"]?.toString() ?: ""
            val intensidad = sesion["intensidad"]?.toString()?.ifEmpty { "R1" } ?: "R1"
            val distancia = sesion["distancia"]?.toString()
            val duracionMin = (sesion["duracion_min"] as? Long)?.toInt()
            val duracionSeg = (sesion["duracion_seg"] as? Long)?.toInt()
            val medicion = sesion["tipo_medicion"]?.toString() ?: ""
            val marca = sesion["marca"]?.toString()?.uppercase() ?: ""

            val ritmoMinKey = "${intensidad}min"
            val ritmoMaxKey = "${intensidad}max"
            val ritmoMin = ritmos?.get(ritmoMinKey)?.toString() ?: "-"
            val ritmoMax = ritmos?.get(ritmoMaxKey)?.toString() ?: "-"
            val ritmoConcatenado =
                if (ritmoMin != "-" && ritmoMax != "-") "$ritmoMax ↔ $ritmoMin" else "-"

            val zIntensidad = intensidad.replace("R", "Z")
            val zonaMinKey = "${zIntensidad.lowercase()}min"
            val zonaMaxKey = "${zIntensidad.lowercase()}max"
            val zonaMin = zonas?.get(zonaMinKey)?.toString() ?: "-"
            val zonaMax = zonas?.get(zonaMaxKey)?.toString() ?: "-"
            val zonaConcatenada =
                if (zonaMin != "-" && zonaMax != "-") "$zonaMin ↔ $zonaMax" else "-"

            val valoresEntrenador = listOf("0","1","2","3","4","5","6","7","8","9","10")
            val valoresDeportista = listOf(
                "Nada","Muy muy suave","Muy suave","Suave","No tan suave",
                "Moderado","No tan fuerte","Medianamente fuerte","Fuerte",
                "Muy fuerte","Muy muy fuerte"
            )
            val esSensacion = intensidad in valoresEntrenador || intensidad in valoresDeportista
            val textoSensacion = when {
                intensidad in valoresEntrenador -> {
                    val idx = intensidad.toIntOrNull()
                    if (idx != null && idx in 0..10) valoresDeportista[idx] else "-"
                }
                intensidad in valoresDeportista -> intensidad
                else -> "-"
            }

            val columnas = mutableListOf<Pair<String, String>>()
            val tipoExcluido = listOf("Flexibilidad", "Movilidad Articular", "Fortalecimiento")

            if (!tipoExcluido.contains(tipo)) {
                if (!distancia.isNullOrEmpty()) {
                    val unidad = if (medicion == "metros") "m" else "km"
                    columnas += "Distancia" to "$distancia $unidad"
                } else if (duracionMin != null || duracionSeg != null) {
                    columnas += "Duración" to String.format("%02d:%02d", duracionMin ?: 0, duracionSeg ?: 0)
                }

                columnas += "Intensidad" to intensidad

                when {
                    intensidad.startsWith("R", true) ->
                        columnas += "Zona de ritmo" to ritmoConcatenado
                    intensidad.startsWith("Z", true) ->
                        columnas += "Zona FC" to zonaConcatenada
                    esSensacion ->
                        columnas += "Sensación" to textoSensacion
                }
            }

            CentralSessionCard(
                index = index + 1,
                title = tipo,
                badge = marca.takeIf { it == "INICIO" || it == "FINAL" },
                badgeColor = AppTextSecondary.copy(alpha = 0.9f),
                metrics = columnas,
                accent = accent
            )

            if (index < sesiones.lastIndex) {
                Divider(
                    thickness = 0.6.dp,
                    color = AppBorder.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun hasSeriesMarkers(
    sesiones: List<Map<String, Any>>
): Boolean {
    val marcas = sesiones.mapNotNull {
        it["marca"]?.toString()?.trim()?.uppercase()
    }
    return "INICIO" in marcas && "FINAL" in marcas
}

@Composable
private fun CentralSessionCard(
    index: Int,
    title: String,
    badge: String?,
    badgeColor: Color,
    metrics: List<Pair<String, String>>,
    accent: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(accent.copy(alpha = 0.14f), RoundedCornerShape(11.dp))
                    .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index.toString(),
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    color = AppTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = badgeColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.24f))
                ) {
                    Text(
                        text = badge,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (metrics.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                metrics.forEach { (label, value) ->
                    CentralMetricTag(
                        label = label,
                        value = value,
                        accent = accent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CentralMetricTag(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = AppSurface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 11.sp
            )
            Text(
                text = value.ifEmpty { "-" },
                color = AppTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}
