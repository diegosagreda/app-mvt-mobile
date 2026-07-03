package com.example.mvt.ui.screens.components.ui

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.ui.screens.components.model.PhaseExercise
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary

@Composable
fun PhaseExerciseItem(
    index: Int,
    ejercicio: PhaseExercise,
    ritmos: Map<String, Any>?,
    zonas: Map<String, Any>?,
    tipoRutina: String // "distancia" o "tiempo"
) {
    val neutralAccent = AppTextSecondary.copy(alpha = 0.95f)
    val intensidad = ejercicio.intensidad.ifEmpty { "R1" }

    // === RITMOS ===
    val ritmoMinKey = "${intensidad}min"
    val ritmoMaxKey = "${intensidad}max"
    val ritmoMin = ritmos?.get(ritmoMinKey)?.toString() ?: "-"
    val ritmoMax = ritmos?.get(ritmoMaxKey)?.toString() ?: "-"
    val ritmoConcatenado =
        if (ritmoMin != "-" && ritmoMax != "-") "$ritmoMax ↔ $ritmoMin" else "-"

    // === ZONAS ===
    val zIntensidad = intensidad.replace("R", "Z")
    val zonaMinKey = "${zIntensidad.lowercase()}min"
    val zonaMaxKey = "${zIntensidad.lowercase()}max"
    val zonaMin = zonas?.get(zonaMinKey)?.toString() ?: "-"
    val zonaMax = zonas?.get(zonaMaxKey)?.toString() ?: "-"
    val zonaConcatenada =
        if (zonaMin != "-" && zonaMax != "-") "$zonaMin ↔ $zonaMax" else "-"

    // === Sensaciones ===
    val valoresEntrenador = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10")
    val valoresDeportista = listOf(
        "Nada",
        "Muy muy suave",
        "Muy suave",
        "Suave",
        "No tan suave",
        "Moderado",
        "No tan fuerte",
        "Medianamente fuerte",
        "Fuerte",
        "Muy fuerte",
        "Muy muy fuerte"
    )

    val esSensacion = intensidad in valoresEntrenador || intensidad in valoresDeportista

    val textoSensacion = when {
        intensidad in valoresEntrenador -> {
            val indexNum = intensidad.toIntOrNull()
            if (indexNum != null && indexNum in 0..10) valoresDeportista[indexNum] else "-"
        }
        intensidad in valoresDeportista -> intensidad
        else -> "-"
    }

    Log.d("PhaseExerciseItem", "────────────────────────────────────────")
    Log.d("PhaseExerciseItem", "Ejercicio #$index: ${ejercicio.tipo}")
    Log.d("PhaseExerciseItem", "Intensidad: $intensidad")
    Log.d("PhaseExerciseItem", "Ritmo Mín: $ritmoMin | Máx: $ritmoMax")
    Log.d("PhaseExerciseItem", "Zona Mín: $zonaMin | Máx: $zonaMax")
    Log.d("PhaseExerciseItem", "Sensación: $textoSensacion")

    val tipoExcluido = listOf("Flexibilidad", "Movilidad Articular", "Fortalecimiento")
    val debeMostrar = when (tipoRutina.lowercase()) {
        "distancia" -> !tipoExcluido.contains(ejercicio.tipo)
        "tiempo" -> true
        else -> true
    }

    val infoItems = mutableListOf<Pair<String, String>>()

    if (debeMostrar) {
        if (!ejercicio.distancia.isNullOrEmpty()) {
            val unidad = if (ejercicio.medicion == "metros") "m" else "km"
            infoItems += "Distancia" to "${ejercicio.distancia} $unidad"
            infoItems += "Intensidad" to intensidad
        } else if (ejercicio.duracion_min != null || ejercicio.duracion_seg != null) {
            val tiempo = String.format(
                "%02d:%02d",
                ejercicio.duracion_min ?: 0,
                ejercicio.duracion_seg ?: 0
            )
            infoItems += "Duración" to tiempo
            infoItems += "Intensidad" to intensidad
        } else {
            infoItems += "Dato" to "-"
        }

        when {
            intensidad.startsWith("R", ignoreCase = true) ->
                infoItems += "Zona de ritmo" to ritmoConcatenado
            intensidad.startsWith("Z", ignoreCase = true) ->
                infoItems += "Zona FC" to zonaConcatenada
            esSensacion ->
                infoItems += "Sensación" to textoSensacion
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = AppSurfaceAlt.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.72f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(AppSurface.copy(alpha = 0.72f), RoundedCornerShape(11.dp))
                        .border(1.dp, AppBorder.copy(alpha = 0.7f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = index.toString(),
                        color = neutralAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        text = ejercicio.tipo,
                        color = AppTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            if (infoItems.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    infoItems.forEach { (label, value) ->
                        PhaseInfoTag(
                            label = label,
                            value = value,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhaseInfoTag(
    label: String,
    value: String,
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
                color = AppTextSecondary,
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
