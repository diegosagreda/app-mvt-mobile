package com.example.mvt.data.firebase.models

data class StravaConnection(
    val id: String = "",
    val idDeportista: String = "",
    val estado: String = "",
    val athleteId: Long? = null,
    val athleteName: String = "",
    val scope: String = "",
    val expiresAt: Long? = null,
    val actualizadoEn: String = ""
)

data class StravaConnectionSnapshot(
    val isEnabled: Boolean = false,
    val connection: StravaConnection? = null
)
