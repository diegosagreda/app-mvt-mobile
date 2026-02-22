package com.example.mvt.data.firebase.repositories

import com.example.mvt.data.firebase.models.Routine
import com.example.mvt.data.firebase.services.FirestoreService

class RoutineRepository(
    private val firestoreService: FirestoreService
) {
    suspend fun getRoutinesByAthlete(athleteId: String): List<Routine> {
        return firestoreService.getRoutinesByAthlete(athleteId)
    }
}
