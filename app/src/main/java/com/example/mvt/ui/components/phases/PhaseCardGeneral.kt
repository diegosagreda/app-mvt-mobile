package com.example.mvt.ui.screens.components.phases

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.ui.screens.components.model.TrainingPhase
import com.example.mvt.ui.screens.components.ui.PhaseExerciseItem
import com.example.mvt.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhaseCardGeneral(
    phase: TrainingPhase,
    ritmos: Map<String, Any>?,   // ← agregado
    zonas: Map<String, Any>? ,    // ← agregado
    tipoMedicion: String
) {
    var expanded by remember { mutableStateOf(true) }
    var showSheet by remember { mutableStateOf(false) }

    LaunchedEffect(phase.nombre) {
        Log.d("PhaseCardGeneral", "Renderizando fase: ${phase.nombre}")
        Log.d("PhaseCardGeneral", "Ritmos recibidos: $ritmos")
        Log.d("PhaseCardGeneral", "Zonas recibidas: $zonas")
    }

    if (showSheet) {
        MediaResourcesSheet(
            recursos = phase.recursos,
            onDismiss = { showSheet = false }
        )
    }

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
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFFE3F2FD), Color.White)))
                .padding(12.dp)
        ) {
            // === Encabezado de la fase ===
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
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(6.dp))

                    // === Ejercicios ===
                    phase.ejercicios.forEachIndexed { i, e ->
                        // ✅ Pasa ritmos y zonas aquí
                        PhaseExerciseItem(
                            index = i + 1,
                            ejercicio = e,
                            ritmos = ritmos,
                            zonas = zonas,
                            tipoMedicion
                        )

                        if (i < phase.ejercicios.lastIndex) {
                            Divider(
                                thickness = 0.5.dp,
                                color = Color.Gray.copy(alpha = 0.2f),
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }

                    // === Comentario ===
                    if (phase.comentario.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = phase.comentario,
                            color = Color.DarkGray,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }

                    // === Botón de recursos ===
                    if (phase.recursos.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { showSheet = true },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.PlayCircleOutline, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Ver recursos")
                        }
                    }
                }
            }
        }
    }
}
