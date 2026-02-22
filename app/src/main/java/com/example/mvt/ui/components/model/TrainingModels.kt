package com.example.mvt.ui.screens.components.model

import androidx.compose.ui.graphics.vector.ImageVector

data class PhaseExercise(
    val tipo: String = "",
    val medicion: String = "",     // "Distancia" o "Tiempo"
    val distancia: String = "",
    val duracion_min: Int? = null,
    val duracion_seg: Int? = null,
    val intensidad: String = "",
    val zonaRitmo: String = "",
    val tipo_medicion: String = ""
)


data class TrainingPhase(
    val nombre: String,
    val icono: ImageVector,
    val ejercicios: List<PhaseExercise>,
    val recursos: List<String>,
    val comentario: String
)
