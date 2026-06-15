package com.example.mvt.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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

    // === GRADIENTE ===
    val gradient = Brush.verticalGradient(
        colors = listOf(
            AppSurface,
            AppSurfaceAlt,
            AppBackground
        ),
        startY = 0f,
        endY = 700f
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable { expanded = !expanded }
            .shadow(20.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(10.dp)
    ) {
        Column(
            modifier = Modifier
                .background(brush = gradient)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .width(50.dp)
                    .height(4.dp)
                    .background(AppTextSecondary.copy(alpha = 0.7f), RoundedCornerShape(50))
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ============================
            // HEADER TOTAL DE LA RUTINA
            // ============================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Rutina",
                        color = AppTextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formattedTotal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = AppTextPrimary
                    )
                }

                // ===============================
                // COLOR OFICIAL DEL ESTADO AQUÍ
                // ===============================
                val colorEstado = when (routine.estado) {
                    "Realizada"     -> Color(0xFF77DD77)
                    "Parcial"       -> Color(0xFFFFCA99)
                    "No_realizada"  -> Color(0xFFFF6961)
                    "Pendiente"     -> Color(0xFFE5DDE6)
                    else            -> AppTextSecondary.copy(alpha = 0.4f)
                }

                // Tonos futuristas (glow)
                val glowColor = colorEstado.copy(alpha = 0.35f)
                val pillBackground = Brush.horizontalGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.10f),
                        glowColor.copy(alpha = 0.25f)
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    // ░ FUTURISTIC STATE PILL ░
                    // ░ FUTURISTIC STATE PILL (FIX: no corta texto) ░
                    Box(
                        modifier = Modifier
                            .height(32.dp) // ← altura mayor y segura para texto
                            .background(
                                brush = pillBackground,
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = routine.estado.replace("_", " "),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorEstado,
                            maxLines = 1
                        )
                    }


                    // ░ ARROW WITH FLOATING EFFECT ░
                    Icon(
                        imageVector = if (expanded)
                            Icons.Default.KeyboardArrowDown
                        else
                            Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = AppTextPrimary,
                        modifier = Modifier
                            .size(26.dp)
                            .padding(start = 6.dp)
                    )
                }

            }

            // ====================================
            // FASES DEL RESUMEN
            // ====================================
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(350)) + fadeIn(tween(350)),
                exit = shrinkVertically(tween(250)) + fadeOut(tween(250))
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Divider(
                        color = AppBorder,
                        thickness = 1.dp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
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

                    Spacer(modifier = Modifier.height(12.dp))

                    Divider(
                        color = AppBorder,
                        thickness = 1.dp
                    )

                    // ====================================
                    // SELECTOR DE ESTADO (ACTUALIZACIÓN)
                    // ====================================
                    RoutineStatusSelector(
                        routine = routine,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        onEstadoActualizado = { nuevoEstado ->
                            onEstadoActualizado(nuevoEstado)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .fillMaxWidth(0.25f)
                            .align(Alignment.CenterHorizontally)
                            .background(AppTextSecondary.copy(alpha = 0.6f))
                    )
                }
            }
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
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.25f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
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
