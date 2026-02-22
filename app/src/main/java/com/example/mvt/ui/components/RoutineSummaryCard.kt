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
import com.example.mvt.ui.theme.PrimaryBlue

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RoutineSummaryCard(
    routine: Routine,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // =========================
    // ESTADOS DEL TOTAL
    // =========================
    var totalCalentamiento by remember { mutableStateOf(0.0) }
    var totalCentral by remember { mutableStateOf(0.0) }
    var totalVuelta by remember { mutableStateOf(0.0) }

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
            PrimaryBlue.copy(alpha = 1f),
            PrimaryBlue.copy(alpha = 1f),
            PrimaryBlue.copy(alpha = 1f)
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .width(50.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(50))
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
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formattedTotal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
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
                    else            -> Color.White.copy(0.4f)
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
                        tint = Color.White.copy(alpha = 0.90f),
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
                        color = Color.White.copy(alpha = 0.25f),
                        thickness = 1.dp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CalentamientoSummaryItem(
                            sesiones = routine.sesiones_calentamiento,
                            tipoMedicion = routine.tipo_medicion,
                            modifier = Modifier.weight(1f),
                            onTotalChange = { totalCalentamiento = it }
                        )

                        CentralSummaryItem(
                            sesionesCentral = routine.sesiones_central,
                            tipoMedicion = routine.tipo_medicion,
                            modifier = Modifier.weight(1f),
                            onTotalChange = { totalCentral = it }
                        )

                        VueltaCalmaSummaryItem(
                            sesiones = routine.sesiones_calma,
                            tipoMedicion = routine.tipo_medicion,
                            modifier = Modifier.weight(1f),
                            onTotalChange = { totalVuelta = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Divider(
                        color = Color.White.copy(alpha = 0.25f),
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
                            routine.estado = nuevoEstado   // ← sincroniza UI automáticamente
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .fillMaxWidth(0.25f)
                            .align(Alignment.CenterHorizontally)
                            .background(Color.White.copy(alpha = 0.5f))
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
