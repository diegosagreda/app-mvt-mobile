package com.example.mvt.ui.screens.components.phases

import android.util.Log
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.ui.screens.components.model.TrainingPhase
import com.example.mvt.ui.screens.components.ui.PhaseExerciseItem
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhaseCardGeneral(
    phase: TrainingPhase,
    ritmos: Map<String, Any>?,
    zonas: Map<String, Any>?,
    tipoMedicion: String
) {
    var expanded by remember { mutableStateOf(true) }
    var showSheet by remember { mutableStateOf(false) }
    val subtitle = buildPhaseSubtitle(phase.ejercicios.size, phase.recursos.size)

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
                            tint = AppTextSecondary,
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
                            tint = AppTextSecondary,
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
                        phase.ejercicios.forEachIndexed { i, e ->
                            PhaseExerciseItem(
                                index = i + 1,
                                ejercicio = e,
                                ritmos = ritmos,
                                zonas = zonas,
                                tipoRutina = tipoMedicion
                            )

                            if (i < phase.ejercicios.lastIndex) {
                                androidx.compose.material3.Divider(
                                    thickness = 0.6.dp,
                                    color = AppBorder.copy(alpha = 0.7f)
                                )
                            }
                        }

                        if (phase.comentario.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = AppSurfaceAlt.copy(alpha = 0.78f),
                                border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.72f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(42.dp)
                                            .background(AppBorder.copy(alpha = 0.95f), RoundedCornerShape(999.dp))
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Comentario de fase",
                                            color = AppTextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = phase.comentario,
                                            color = AppTextPrimary,
                                            fontSize = 13.sp,
                                            lineHeight = 19.sp
                                        )
                                    }
                                }
                            }
                        }

                        if (phase.recursos.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { showSheet = true },
                                modifier = Modifier.align(Alignment.Start),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.78f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = AppSurface.copy(alpha = 0.55f),
                                    contentColor = AppTextPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.PlayCircleOutline, null, modifier = Modifier.size(11.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Ver recursos", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
    }
}

private fun buildPhaseSubtitle(
    exercises: Int,
    resources: Int
): String {
    val exerciseText = if (exercises == 1) "1 ejercicio" else "$exercises ejercicios"
    val resourceText = if (resources == 1) "1 recurso" else "$resources recursos"
    return "$exerciseText · $resourceText"
}
