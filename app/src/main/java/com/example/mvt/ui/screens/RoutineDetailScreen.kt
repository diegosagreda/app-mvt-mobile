package com.example.mvt.ui.screens

import android.util.Log
import androidx.compose.foundation.background
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
    val fechaFormatted = remember(routine.fecha) {
        routine.fecha?.toDate()?.let {
            SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(it)
        } ?: "Sin fecha"
    }

    Log.d("RoutineDetailScreen", "Rutina ID ${routine.id} - Titulo ${routine.titulo}")

    var expandedInfo by remember { mutableStateOf(true) }
    var expandedGraph by remember { mutableStateOf(false) }
    var expandedPhases by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 0.dp)
        ) {
            // === CABECERA ===
            RoutineHeader(
                titulo = routine.titulo,
                fecha = fechaFormatted,
                tipoMedicion = routine.tipo_medicion,
                onBackClick = onBackClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // === TARJETA PRINCIPAL ===
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    AccordionSection(
                        title = "Información Rutina",
                        expanded = expandedInfo,
                        onExpandChange = { expandedInfo = !expandedInfo }
                    ) {
                        RoutineInfoSection(
                            tipoEsfuerzo = routine.tipo_esfuerzo,
                            tipoMedicion = routine.tipo_medicion,
                            tipoTerreno = routine.tipo_terreno,
                            descripcion = routine.descripcion,
                            objetivos = routine.objetivos
                        )
                    }

                    AccordionSection(
                        title = "Gráfica Fase Calentamiento",
                        expanded = expandedGraph,
                        onExpandChange = { expandedGraph = !expandedGraph }
                    ) {
                        if (!routine.sesiones_calentamiento.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .padding(horizontal = 8.dp, vertical = 12.dp)
                            ) {
                                RoutineChart(
                                    routine = routine,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Text(
                                text = "No hay ejercicios en la fase de calentamiento.",
                                color = Color.Gray
                            )
                        }
                    }




                    AccordionSection(
                        title = "Fases del Entrenamiento",
                        expanded = expandedPhases,
                        onExpandChange = { expandedPhases = !expandedPhases }
                    ) {
                        RoutinePhasesSection(
                            calentamiento = routine.sesiones_calentamiento ?: emptyList(),
                            central = routine.sesiones_central ?: emptyMap(),
                            vuelta = routine.sesiones_calma ?: emptyList(),
                            comentarios_fase_calentamiento = routine.comentarios_fase_calentamiento,
                            comentarios_fase_central = routine.comentarios_fase_central,
                            comentarios_fase_calma = routine.comentarios_fase_calma,
                            recursosCalentamiento = routine.videosCalentamiento,
                            recursosCentral = routine.videosCentral,
                            recursosCalma = routine.videosCalma,
                            ritmos = ritmos,   // ← pasa los datos
                            zonas = zonas,      // ← pasa los datos
                            tipoMedicion = routine.tipo_medicion
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
            RoutineSummaryCard(routine = routine)
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
                .clickable { onExpandChange() }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue,
                fontSize = 16.sp
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = PrimaryBlue
            )
        }

        if (expanded) {
            Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
            content()
        }
    }
}
