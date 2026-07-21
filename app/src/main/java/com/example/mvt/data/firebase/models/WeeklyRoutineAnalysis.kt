package com.example.mvt.data.firebase.models

data class WeeklyRoutineAnalysis(
    val version: String = "",
    val fecha: String = "",
    val semana: WeeklyRoutineRange? = null,
    val estado: String = "",
    val generado_por: String = "",
    val resumen: String = "",
    val balance: String = "",
    val foco_hoy: String = "",
    val mejora: String = "",
    val descanso: String = "",
    val metricas: WeeklyRoutineMetrics? = null,
    val rutina_hoy: WeeklyRoutineToday? = null,
    val actualizado_en: Long? = null
)

data class WeeklyRoutineRange(
    val inicio: String = "",
    val fin: String = ""
)

data class WeeklyRoutineMetrics(
    val total: Int = 0,
    val realizadas: Int = 0,
    val parciales: Int = 0,
    val pendientes: Int = 0,
    val no_realizadas: Int = 0,
    val con_resultados: Int = 0,
    val cumplimiento_promedio: Int? = null
)

data class WeeklyRoutineToday(
    val id: String = "",
    val fecha: String = "",
    val es_hoy: Boolean = false,
    val titulo: String = "",
    val objetivos: String = "",
    val descripcion: String = "",
    val tipo_esfuerzo: String = "",
    val tipo_medicion: String = "",
    val estado: String = "",
    val cumplimiento_pct: Int? = null,
    val coach_resumen: String = "",
    val coach_mejoras: List<String> = emptyList()
)
