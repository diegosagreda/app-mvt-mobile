package com.example.mvt.data.firebase.models

import com.google.firebase.Timestamp
import java.io.Serializable

data class RoutineStrava(
    val estado: String = "",
    val sincronizadoEn: String = "",
    val desvinculadoManualEn: String = "",
    val actividadesIgnoradas: List<String> = emptyList(),
    val actividad: StravaActivity? = null,
    val cumplimiento: StravaCompliance? = null,
    val analisis: StravaAnalysis? = null,
    val error: String = ""
) : Serializable

data class StravaActivity(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val sportType: String = "",
    val startDate: String = "",
    val startDateLocal: String = "",
    val elapsedTime: Int? = null,
    val movingTime: Int? = null,
    val distance: Double? = null,
    val averageSpeed: Double? = null,
    val maxSpeed: Double? = null,
    val averageHeartrate: Double? = null,
    val maxHeartrate: Double? = null,
    val totalElevationGain: Double? = null,
    val sufferScore: Double? = null,
    val externalId: String = ""
) : Serializable

data class StravaCompliance(
    val porcentaje: Double? = null,
    val estado: String = "",
    val metrica: String = "",
    val planificado: Double? = null,
    val realizado: Double? = null,
    val unidad: String = ""
) : Serializable

data class StravaAnalysis(
    val estadoTexto: String = "",
    val lecturaEntrenador: String = "",
    val diferencia: Double? = null,
    val diferenciaTexto: String = "",
    val distanciaKm: Double? = null,
    val tiempoMovimientoSeg: Int? = null,
    val ritmoSegPorKm: Double? = null,
    val velocidadPromedioKmh: Double? = null,
    val velocidadMaximaKmh: Double? = null,
    val frecuenciaPromedio: Double? = null,
    val frecuenciaMaxima: Double? = null,
    val desnivelM: Double? = null,
    val sincronizadoEn: String = ""
) : Serializable

data class Routine(
    val id: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val id_deportista: String = "",
    val id_entrenador: String = "",
    val fecha: Timestamp? = null,
    val completa: Boolean = false,
    val mostrar: Boolean = true,
    var estado: String = "",
    val objetivos: String = "",
    val tipo_esfuerzo: String = "",
    val tipo_medicion: String = "",
    val tipo_terreno: String = "",
    val isStravaSynced: Boolean = false,
    val stravaActivityId: String = "",
    val strava: RoutineStrava? = null,
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
