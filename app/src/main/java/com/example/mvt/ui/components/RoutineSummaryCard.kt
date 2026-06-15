package com.example.mvt.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.data.firebase.models.Routine
import com.example.mvt.ui.components.summary.CalentamientoSummaryItem
import com.example.mvt.ui.components.summary.CentralSummaryItem
import com.example.mvt.ui.components.summary.RoutineStatusSelector
import com.example.mvt.ui.components.summary.VueltaCalmaSummaryItem
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RoutineSummaryCard(
    routine: Routine,
    modifier: Modifier = Modifier,
    onEstadoActualizado: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val estadoColor = remember(routine.estado) { routineStatusColor(routine.estado) }

    val totalCalentamiento = remember(routine.sesiones_calentamiento, routine.tipo_medicion) {
        calculatePhaseTotal(
            sesiones = routine.sesiones_calentamiento,
            tipoMedicion = routine.tipo_medicion
        )
    }
    val totalCentral = remember(routine.sesiones_central, routine.tipo_medicion) {
        calculateCentralTotal(
            sesionesCentral = routine.sesiones_central,
            tipoMedicion = routine.tipo_medicion
        )
    }
    val totalVuelta = remember(routine.sesiones_calma, routine.tipo_medicion) {
        calculatePhaseTotal(
            sesiones = routine.sesiones_calma,
            tipoMedicion = routine.tipo_medicion
        )
    }

    val totalGeneral = totalCalentamiento + totalCentral + totalVuelta

    fun formatTiempo(totalMinutos: Double): String {
        val totalSegundos = (totalMinutos * 60).toInt()
        val horas = totalSegundos / 3600
        val minutos = (totalSegundos % 3600) / 60
        val segundos = totalSegundos % 60
        return String.format("%02d:%02d:%02d", horas, minutos, segundos)
    }

    val formattedTotal =
        if (routine.tipo_medicion.lowercase() == "tiempo")
            formatTiempo(totalGeneral)
        else
            "%.2f km".format(totalGeneral)

    val gradient = Brush.verticalGradient(
        colors = listOf(
            AppSurfaceAlt.copy(alpha = 0.98f),
            AppSurface.copy(alpha = 0.98f),
            AppBackground.copy(alpha = 0.98f)
        ),
        startY = 0f,
        endY = 700f
    )
    val stateGlow = estadoColor.copy(alpha = if (expanded) 0.20f else 0.14f)
    val bottomEdgeShade = if (expanded) estadoColor.copy(alpha = 0.12f) else estadoColor.copy(alpha = 0.22f)
    val totalIcon = if (routine.tipo_medicion.lowercase() == "tiempo") {
        Icons.Default.Schedule
    } else {
        Icons.Default.Straighten
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable { expanded = !expanded }
            .shadow(22.dp, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        color = Color.Transparent,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = gradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .height(5.dp)
                            .background(AppTextSecondary.copy(alpha = 0.55f), RoundedCornerShape(50))
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = totalIcon,
                            contentDescription = null,
                            tint = AppTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = formattedTotal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            color = AppTextPrimary,
                            lineHeight = 24.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = stateGlow,
                        border = BorderStroke(1.dp, estadoColor.copy(alpha = 0.42f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = cleanRoutineStateLabel(routine.estado),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = estadoColor,
                                maxLines = 1
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(tween(350)) + fadeIn(tween(350)),
                    exit = shrinkVertically(tween(250)) + fadeOut(tween(250))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CalentamientoSummaryItem(
                                    total = totalCalentamiento,
                                    tipoMedicion = routine.tipo_medicion,
                                    modifier = Modifier.weight(1f)
                                )

                                CentralSummaryItem(
                                    total = totalCentral,
                                    tipoMedicion = routine.tipo_medicion,
                                    modifier = Modifier.weight(1f)
                                )

                                VueltaCalmaSummaryItem(
                                    total = totalVuelta,
                                    tipoMedicion = routine.tipo_medicion,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Divider(color = AppBorder.copy(alpha = 0.78f), thickness = 1.dp)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Actualizacion de la rutina",
                                color = AppTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Define de forma manual como termino esta sesion.",
                                color = AppTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )

                            RoutineStatusSelector(
                                routine = routine,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                onEstadoActualizado = { nuevoEstado ->
                                    onEstadoActualizado(nuevoEstado)
                                }
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(14.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                bottomEdgeShade.copy(alpha = 0.08f),
                                bottomEdgeShade
                            )
                        )
                    )
            )
        }
    }
}

// ==============================
// BASE ITEM
// ==============================
@Composable
fun SummaryBaseItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(color.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = AppTextSecondary,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = AppTextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

private fun routineStatusColor(estado: String): Color {
    return when (estado) {
        "Realizada" -> Color(0xFF77DD77)
        "Parcial" -> Color(0xFFFFCA99)
        "No_realizada" -> Color(0xFFFF6961)
        "Pendiente" -> Color(0xFFE5DDE6)
        else -> PrimaryBlue
    }
}

private fun cleanRoutineStateLabel(label: String): String =
    when (label) {
        "No_realizada" -> "No realizada"
        else -> label
    }

private fun calculatePhaseTotal(
    sesiones: List<Map<String, Any>>?,
    tipoMedicion: String
): Double {
    val tipoExcluido = listOf("Flexibilidad", "Movilidad Articular", "Fortalecimiento")

    if (sesiones.isNullOrEmpty()) return 0.0

    return if (tipoMedicion.lowercase() == "tiempo") {
        sesiones.sumOf {
            val min = (it["duracion_min"] as? Number)?.toDouble() ?: 0.0
            val seg = (it["duracion_seg"] as? Number)?.toDouble() ?: 0.0
            min + seg / 60.0
        }
    } else {
        sesiones.sumOf {
            val tipo = (it["tipo"] as? String)?.trim() ?: ""
            if (tipoExcluido.contains(tipo)) return@sumOf 0.0

            val dist = (it["distancia"] as? Number)?.toDouble() ?: 0.0
            val uni = (it["tipo_medicion"] as? String)?.lowercase() ?: ""
            if (uni == "metros") dist / 1000.0 else dist
        }
    }
}

private fun calculateCentralTotal(
    sesionesCentral: Any?,
    tipoMedicion: String
): Double {
    val tipoExcluido = listOf("Flexibilidad", "Movilidad Articular", "Fortalecimiento")
    val centralMap = sesionesCentral as? Map<String, Any>
    val series = (centralMap?.get("series") as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()

    if (series.isEmpty()) return 0.0

    return if (tipoMedicion.lowercase() == "distancia") {
        var sum = 0.0

        for (serie in series) {
            val sesiones = (serie["sesiones"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: continue
            var subtotal = 0.0

            val ini = (serie["inicio"] as? Number)?.toInt() ?: -1
            val fin = (serie["final"] as? Number)?.toInt() ?: -1
            val rep = (serie["repeticiones"] as? Number)?.toDouble() ?: 1.0

            val lista = if (ini != -1 && fin != -1 && ini < sesiones.size && fin < sesiones.size) {
                sesiones.subList(ini, fin + 1)
            } else {
                sesiones
            }

            for (sesion in lista) {
                val tipo = (sesion["tipo"] as? String)?.trim() ?: ""
                if (tipoExcluido.contains(tipo)) continue

                val dist = (sesion["distancia"] as? Number)?.toDouble() ?: 0.0
                val uni = (sesion["tipo_medicion"] as? String)?.lowercase() ?: ""
                subtotal += if (uni == "metros") dist / 1000.0 else dist
            }

            sum += subtotal * rep
        }

        sum
    } else {
        var sum = 0.0

        for (serie in series) {
            val sesiones = (serie["sesiones"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: continue
            val rep = (serie["repeticiones"] as? Number)?.toDouble() ?: 1.0

            val subtotal = sesiones.sumOf {
                val min = (it["duracion_min"] as? Number)?.toDouble() ?: 0.0
                val sec = (it["duracion_seg"] as? Number)?.toDouble() ?: 0.0
                min + sec / 60.0
            }

            sum += subtotal * rep
        }

        sum
    }
}
