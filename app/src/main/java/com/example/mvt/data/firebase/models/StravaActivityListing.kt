package com.example.mvt.data.firebase.models

data class StravaActivitiesResult(
    val estado: String = "",
    val actividades: List<StravaActivity> = emptyList(),
    val mensaje: String = "",
    val fechaConsulta: String = "",
    val limit: Int = 20
)
