package com.example.mvt.ui.screens.components

import android.R
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mvt.ui.screens.components.model.PhaseExercise
import com.example.mvt.ui.screens.components.model.TrainingPhase
import com.example.mvt.ui.screens.components.phases.PhaseCardCentral
import com.example.mvt.ui.screens.components.phases.PhaseCardGeneral

private const val TAG = "RoutinePhasesSection"

@Composable
fun RoutinePhasesSection(
    calentamiento: List<Map<String, Any>>,
    central: Map<String, Any>,
    vuelta: List<Map<String, Any>>,
    comentarios_fase_calentamiento: Any?,
    comentarios_fase_central: Any?,
    comentarios_fase_calma: Any?,
    recursosCalentamiento: List<String>?,
    recursosCentral: List<String>?,
    recursosCalma: List<String>?,
    ritmos: Map<String, Any>?,   // ← agregado
    zonas: Map<String, Any>?,     // ← agregado
    tipoMedicion: String
) {
    Log.d(TAG, "==> Iniciando render de fases")
    Log.d(TAG, "Datos recibidos:")
    Log.d(TAG, "Calentamiento: ${calentamiento.size} ejercicios")
    Log.d(TAG, "Central: ${central.keys}")
    Log.d(TAG, "Vuelta: ${vuelta.size} ejercicios")
    Log.d(TAG, "Comentarios Calentamiento: $comentarios_fase_calentamiento")
    Log.d(TAG, "Comentarios Central: $comentarios_fase_central")
    Log.d(TAG, "Comentarios Calma: $comentarios_fase_calma")
    Log.d(TAG, "Ritmos: $ritmos")
    Log.d(TAG, "Zonas: $zonas")

    // === MAPEO DE CALENTAMIENTO ===
    val ejerciciosCalentamiento = calentamiento.mapIndexed { index, it ->
        Log.d(TAG, "→ Calentamiento[$index]: $it")
        PhaseExercise(
            tipo = it["tipo"]?.toString() ?: "",
            medicion = it["tipo_medicion"]?.toString() ?: "",
            distancia = it["distancia"]?.toString() ?: "",
            duracion_min = (it["duracion_min"] as? Long)?.toInt(),
            duracion_seg = (it["duracion_seg"] as? Long)?.toInt(),
            intensidad = it["intensidad"]?.toString() ?: "",
            zonaRitmo = it["ritmo"]?.toString() ?: "-"
        ).also { ex ->
            Log.d(TAG, "✔️ Calentamiento[$index] parseado: $ex")
        }
    }

    // === MAPEO DE VUELTA A LA CALMA ===
    val ejerciciosVuelta = vuelta.mapIndexed { index, it ->
        Log.d(TAG, "→ Vuelta[$index]: $it")
        PhaseExercise(
            tipo = it["tipo"]?.toString() ?: "",
            medicion = it["tipo_medicion"]?.toString() ?: "",
            distancia = it["distancia"]?.toString() ?: "",
            duracion_min = (it["duracion_min"] as? Long)?.toInt(),
            duracion_seg = (it["duracion_seg"] as? Long)?.toInt(),
            intensidad = it["intensidad"]?.toString() ?: "",
            zonaRitmo = it["ritmo"]?.toString() ?: "-"
        ).also { ex ->
            Log.d(TAG, "✔️ Vuelta[$index] parseado: $ex")
        }
    }

    // === CONSTRUCCIÓN DE FASES ===
    val fases = listOf(
        TrainingPhase(
            nombre = "Fase Calentamiento",
            icono = Icons.Default.Fireplace,
            ejercicios = ejerciciosCalentamiento,
            recursos = recursosCalentamiento ?: emptyList(),
            comentario = comentarios_fase_calentamiento?.toString() ?: ""
        ),
        TrainingPhase(
            nombre = "Fase Central",
            icono = Icons.Default.FitnessCenter,
            ejercicios = emptyList(),
            recursos = recursosCentral ?: emptyList(),
            comentario = comentarios_fase_central?.toString() ?: ""
        ),
        TrainingPhase(
            nombre = "Vuelta a la Calma",
            icono = Icons.Default.SelfImprovement,
            ejercicios = ejerciciosVuelta,
            recursos = recursosCalma ?: emptyList(),
            comentario = comentarios_fase_calma?.toString() ?: ""
        )
    )

    Log.d(TAG, "✅ Fases construidas: ${fases.size}")
    fases.forEachIndexed { index, phase ->
        Log.d(TAG, "  • Fase[$index]: ${phase.nombre} (${phase.ejercicios.size} ejercicios)")
    }

    // === RENDER ===
    Column(Modifier.fillMaxWidth()) {
        fases.forEach { phase ->
            when (phase.nombre) {
                "Fase Central" -> {
                    Log.d(TAG, "Mostrando Fase Central")
                    PhaseCardCentral(phase, central, ritmos, zonas)
                }
                else -> {
                    Log.d(TAG, "Mostrando Fase General: ${phase.nombre}")
                    PhaseCardGeneral(phase, ritmos, zonas, tipoMedicion)
                }
            }
        }
    }
}
