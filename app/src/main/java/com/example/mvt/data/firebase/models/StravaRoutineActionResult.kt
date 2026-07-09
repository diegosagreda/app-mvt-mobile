package com.example.mvt.data.firebase.models

data class StravaRoutineActionResult(
    val estado: String = "",
    val mensaje: String = "",
    val strava: RoutineStrava? = null,
    val actividad: StravaActivity? = null,
    val cumplimiento: StravaCompliance? = null
)
