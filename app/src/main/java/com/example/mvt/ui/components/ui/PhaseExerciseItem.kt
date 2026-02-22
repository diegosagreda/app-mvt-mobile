package com.example.mvt.ui.screens.components.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.ui.screens.components.model.PhaseExercise
import com.example.mvt.ui.theme.PrimaryBlue

@Composable
fun PhaseExerciseItem(
    index: Int,
    ejercicio: PhaseExercise,
    ritmos: Map<String, Any>?,
    zonas: Map<String, Any>?,
    tipoRutina: String // "distancia" o "tiempo"
) {
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        // Encabezado con número e información del ejercicio
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(color = PrimaryBlue, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = ejercicio.tipo,
                color = PrimaryBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // === Control de columnas visibles según tipo de rutina ===
        val tipoExcluido = listOf("Flexibilidad", "Movilidad Articular", "Fortalecimiento")

        val debeMostrar = when (tipoRutina.lowercase()) {
            "distancia" -> !tipoExcluido.contains(ejercicio.tipo)
            "tiempo" -> true
            else -> true
        }

        if (debeMostrar) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!ejercicio.distancia.isNullOrEmpty()) {
                    val unidad = if (ejercicio.medicion == "metros") "m" else "km"
                    val distanciaConcatenada = "${ejercicio.distancia} $unidad"

                    PhaseInfoTag("Distancia", distanciaConcatenada)
                    PhaseInfoTag("Intensidad", intensidad)

                    when {
                        intensidad.startsWith("R", ignoreCase = true) ->
                            PhaseInfoTag("Zona Ritmos(min/km)", ritmoConcatenado)
                        intensidad.startsWith("Z", ignoreCase = true) ->
                            PhaseInfoTag("Zonas FC (ppm)", zonaConcatenada)
                        esSensacion ->
                            PhaseInfoTag("Sensaciones", textoSensacion)
                    }
                } else if (ejercicio.duracion_min != null || ejercicio.duracion_seg != null) {
                    val tiempo = String.format(
                        "%02d:%02d",
                        ejercicio.duracion_min ?: 0,
                        ejercicio.duracion_seg ?: 0
                    )

                    PhaseInfoTag("Duración", tiempo)
                    PhaseInfoTag("Intensidad", intensidad)

                    when {
                        intensidad.startsWith("R", ignoreCase = true) ->
                            PhaseInfoTag("Zona Ritmos(min/km)", ritmoConcatenado)
                        intensidad.startsWith("Z", ignoreCase = true) ->
                            PhaseInfoTag("Zonas FC (ppm)", zonaConcatenada)
                        esSensacion ->
                            PhaseInfoTag("Sensaciones", textoSensacion)
                    }
                } else {
                    PhaseInfoTag("Dato", "-")
                }
            }
        }
    }
}

@Composable
fun PhaseInfoTag(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(value.ifEmpty { "-" }, color = Color.Black, fontSize = 12.sp)
    }
}
