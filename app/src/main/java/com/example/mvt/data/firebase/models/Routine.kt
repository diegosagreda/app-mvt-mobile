package com.example.mvt.data.firebase.models

import com.google.firebase.Timestamp
import java.io.Serializable

data class Routine(
    val id: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val id_deportista: String = "",
    val fecha: Timestamp? = null,
    val completa: Boolean = false,
    var estado: String = "",
    val objetivos: String = "",
    val tipo_esfuerzo: String = "",
    val tipo_medicion: String = "",
    val tipo_terreno: String = "",
    val sesiones_calentamiento: List<Map<String, Any>>? = null,
    val sesiones_central: Map<String, Any>? = null,
    val sesiones_calma: List<Map<String, Any>>? = null,
    val comentarios_fase_calentamiento:  String = "",
    val comentarios_fase_central:  String = "",
    val comentarios_fase_calma:  String = "",
    val videosCalentamiento: List<String>? = null,
    val videosCalma: List<String>? = null,
    val videosCentral: List<String>? = null,
    ) : Serializable
