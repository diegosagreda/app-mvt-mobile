package com.example.mvt.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.data.firebase.models.Routine
import com.example.mvt.ui.components.RoutineInfoSection
import com.example.mvt.ui.components.RoutineSummaryCard
import com.example.mvt.ui.components.chart.RoutineChart
import com.example.mvt.ui.components.header.RoutineHeader
import com.example.mvt.ui.screens.components.RoutinePhasesSection
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    routine: Routine,
    ritmos: Map<String, Any>?,   // ← parámetros correctamente definidos
    zonas: Map<String, Any>?,    // ← parámetros correctamente definidos
    onBackClick: () -> Unit
) {
    var routineState by remember(routine.id) { mutableStateOf(routine) }

    val fechaFormatted = remember(routineState.fecha) {
        routineState.fecha?.toDate()?.let {
            SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(it)
        } ?: "Sin fecha"
    }

    Log.d("RoutineDetailScreen", "Rutina ID ${routineState.id} - Titulo ${routineState.titulo}")

    var expandedInfo by remember { mutableStateOf(true) }
    var expandedGraph by remember { mutableStateOf(false) }
    var expandedPhases by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // === CABECERA ===
            RoutineHeader(
                titulo = routineState.titulo,
                fecha = fechaFormatted,
                tipoMedicion = routineState.tipo_medicion,
                onBackClick = onBackClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // === TARJETA PRINCIPAL ===
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .border(1.dp, AppBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    AccordionSection(
                        title = "Información Rutina",
                        expanded = expandedInfo,
                        onExpandChange = { expandedInfo = !expandedInfo }
                    ) {
                        RoutineInfoSection(
                            tipoEsfuerzo = routineState.tipo_esfuerzo,
                            tipoMedicion = routineState.tipo_medicion,
                            tipoTerreno = routineState.tipo_terreno,
                            descripcion = routineState.descripcion,
                            objetivos = routineState.objetivos
                        )
                    }

                    AccordionSection(
                        title = "Gráfica Fase Calentamiento",
                        expanded = expandedGraph,
                        onExpandChange = { expandedGraph = !expandedGraph }
                    ) {
                        if (!routineState.sesiones_calentamiento.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .padding(horizontal = 8.dp, vertical = 12.dp)
                            ) {
                                RoutineChart(
                                    routine = routineState,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Text(
                                text = "No hay ejercicios en la fase de calentamiento.",
                                color = AppTextSecondary
                            )
                        }
                    }




                    AccordionSection(
                        title = "Fases del Entrenamiento",
                        expanded = expandedPhases,
                        onExpandChange = { expandedPhases = !expandedPhases }
                    ) {
                        RoutinePhasesSection(
                            calentamiento = routineState.sesiones_calentamiento ?: emptyList(),
                            central = routineState.sesiones_central ?: emptyMap(),
                            vuelta = routineState.sesiones_calma ?: emptyList(),
                            comentarios_fase_calentamiento = routineState.comentarios_fase_calentamiento,
                            comentarios_fase_central = routineState.comentarios_fase_central,
                            comentarios_fase_calma = routineState.comentarios_fase_calma,
                            recursosCalentamiento = routineState.videosCalentamiento,
                            recursosCentral = routineState.videosCentral,
                            recursosCalma = routineState.videosCalma,
                            ritmos = ritmos,   // ← pasa los datos
                            zonas = zonas,      // ← pasa los datos
                            tipoMedicion = routineState.tipo_medicion
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }


        // === ETIQUETA FLOTANTE (RESUMEN RUTINA) TOTALES ===
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            RoutineSummaryCard(
                routine = routineState,
                onEstadoActualizado = { nuevoEstado ->
                    routineState = routineState.copy(estado = nuevoEstado)
                }
            )
        }

    }
}

@Composable
fun AccordionSection(
    title: String,
    expanded: Boolean,
    onExpandChange: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppSurfaceAlt, RoundedCornerShape(12.dp))
                .clickable { onExpandChange() }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = AppTextPrimary,
                fontSize = 16.sp
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = PrimaryBlue
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = AppBorder, thickness = 1.dp)
            content()
        }
    }
}
