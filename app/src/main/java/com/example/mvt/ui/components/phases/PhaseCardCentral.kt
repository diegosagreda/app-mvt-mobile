package com.example.mvt.ui.screens.components.phases

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.ui.screens.components.model.TrainingPhase
import com.example.mvt.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhaseCardCentral(
    phase: TrainingPhase,
    central: Map<String, Any>,
    ritmos: Map<String, Any>?,
    zonas: Map<String, Any>?
) {
    var expanded by remember { mutableStateOf(true) }
    val series = (central["series"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()
    val expandedSeries = remember { mutableStateListOf<Boolean>().apply { repeat(series.size) { add(true) } } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(Color(0xFFE3F2FD), Color.White)))
                .padding(12.dp)
        ) {
            // === Encabezado ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(phase.icono, null, tint = PrimaryBlue)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        phase.nombre,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }

                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { expanded = !expanded }
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(10.dp))

                    // === SERIES ===
                    series.forEachIndexed { i, serie ->
                        val sesiones = (serie["sesiones"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()
                        val isExpanded = expandedSeries[i]

                        val startIndex = sesiones.indexOfFirst { it["marca"]?.toString()?.equals("INICIO", true) == true }
                        val endIndex = sesiones.indexOfLast { it["marca"]?.toString()?.equals("FINAL", true) == true }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            // === Encabezado Serie ===
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedSeries[i] = !isExpanded },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Nombre de serie
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(PrimaryBlue, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Repeat, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Serie ${i + 1}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                // Línea punteada azul
                                Canvas(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .padding(horizontal = 6.dp)
                                ) {
                                    drawLine(
                                        color = PrimaryBlue.copy(alpha = 0.6f),
                                        start = Offset(0f, size.height / 2),
                                        end = Offset(size.width, size.height / 2),
                                        strokeWidth = 3f,
                                        cap = StrokeCap.Round,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 12f), 0f)
                                    )
                                }

                                // Repeticiones
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(PrimaryBlue, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "×${serie["repeticiones"] ?: 1}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = PrimaryBlue
                                    )
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            // === Contenido Serie ===
                            AnimatedVisibility(visible = isExpanded) {
                                Box {
                                    Column {
                                        sesiones.forEachIndexed { index, sesion ->
                                            val tipo = sesion["tipo"]?.toString() ?: ""
                                            val intensidad = sesion["intensidad"]?.toString()?.ifEmpty { "R1" } ?: "R1"
                                            val distancia = sesion["distancia"]?.toString()
                                            val duracionMin = (sesion["duracion_min"] as? Long)?.toInt()
                                            val duracionSeg = (sesion["duracion_seg"] as? Long)?.toInt()
                                            val medicion = sesion["tipo_medicion"]?.toString() ?: ""
                                            val marca = sesion["marca"]?.toString()?.uppercase() ?: ""

                                            // ==== Ritmos ====
                                            val ritmoMinKey = "${intensidad}min"
                                            val ritmoMaxKey = "${intensidad}max"
                                            val ritmoMin = ritmos?.get(ritmoMinKey)?.toString() ?: "-"
                                            val ritmoMax = ritmos?.get(ritmoMaxKey)?.toString() ?: "-"
                                            val ritmoConcatenado =
                                                if (ritmoMin != "-" && ritmoMax != "-") "$ritmoMax ↔ $ritmoMin" else "-"

                                            // ==== Zonas ====
                                            val zIntensidad = intensidad.replace("R", "Z")
                                            val zonaMinKey = "${zIntensidad.lowercase()}min"
                                            val zonaMaxKey = "${zIntensidad.lowercase()}max"
                                            val zonaMin = zonas?.get(zonaMinKey)?.toString() ?: "-"
                                            val zonaMax = zonas?.get(zonaMaxKey)?.toString() ?: "-"
                                            val zonaConcatenada =
                                                if (zonaMin != "-" && zonaMax != "-") "$zonaMin ↔ $zonaMax" else "-"

                                            // ==== Sensaciones ====
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

                                            Spacer(Modifier.height(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                            ) {
                                                // === Cabecera de ejercicio con marca arriba derecha ===
                                                Column(
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(30.dp)
                                                                .background(PrimaryBlue, CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = (index + 1).toString(),
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 14.sp
                                                            )
                                                        }

                                                        Spacer(Modifier.width(8.dp))
                                                        Text(tipo, fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 14.sp)
                                                    }

                                                    Spacer(Modifier.height(4.dp))

                                                    // === Datos del ejercicio ===
                                                    val tipoExcluido = listOf("Flexibilidad","Movilidad Articular","Fortalecimiento")

                                                    if (!tipoExcluido.contains(tipo)) {
                                                        Row(
                                                            Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            val columnas = mutableListOf<Pair<String,String>>()

                                                            if (!distancia.isNullOrEmpty()) {
                                                                val unidad = if (medicion == "metros") "m" else "km"
                                                                val distanciaConcatenada = "$distancia $unidad"
                                                                columnas.add("Distancia" to distanciaConcatenada)
                                                            } else if (duracionMin != null || duracionSeg != null) {
                                                                val tiempo = String.format("%02d:%02d", duracionMin ?: 0, duracionSeg ?: 0)
                                                                columnas.add("Duración" to tiempo)
                                                            }

                                                            columnas.add("Intensidad" to intensidad)

                                                            when {
                                                                intensidad.startsWith("R", true) ->
                                                                    columnas.add("Zona Ritmos(min/km)" to ritmoConcatenado)
                                                                intensidad.startsWith("Z", true) ->
                                                                    columnas.add("Zonas FC (ppm)" to zonaConcatenada)
                                                                esSensacion ->
                                                                    columnas.add("Sensaciones" to textoSensacion)
                                                            }

                                                            columnas.forEach { (label, value) ->
                                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                    Text(label, color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                                    Text(value.ifEmpty { "-" }, color = Color.Black, fontSize = 12.sp)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                // === Marca en esquina superior derecha ===
                                                if (marca == "INICIO" || marca == "FINAL") {
                                                    val color = if (marca == "INICIO") Color(0xFFC8E6C9) else Color(0xFFFFCDD2)
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .background(color, RoundedCornerShape(8.dp))
                                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(
                                                            text = marca,
                                                            color = Color.Black,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                            }

                                            if (index < sesiones.lastIndex)
                                                Divider(
                                                    thickness = 0.5.dp,
                                                    color = Color.Gray.copy(alpha = 0.2f),
                                                    modifier = Modifier.padding(vertical = 6.dp)
                                                )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
