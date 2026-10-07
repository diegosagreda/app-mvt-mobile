package com.example.mvt.trainer.athletes.model

data class ApprovedAthlete(
    val id: String = "",
    val name: String = "",
    val photoUrl: String? = null,
    val sport: String = "Multideporte",
    val planName: String = "Sin plan",
    val status: String = "Aprobado"
)
