package com.example.mvt.trainer.requests.model

data class PendingTrainerRequest(
    val id: String,
    val athleteId: String,
    val athleteName: String,
    val sport: String,
    val photoUrl: String?,
    val registeredAt: Long?
)
