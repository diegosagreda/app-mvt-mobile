package com.example.mvt.data.firebase.repositories

import com.example.mvt.data.firebase.models.Routine
import com.example.mvt.data.firebase.services.FirestoreService
import java.time.YearMonth

class RoutineRepository(
    private val firestoreService: FirestoreService
) {
    suspend fun getRoutinesByAthlete(athleteId: String): List<Routine> {
        return firestoreService.getRoutinesByAthlete(athleteId)
    }

    suspend fun getRoutinesByAthleteForMonth(
        athleteId: String,
        month: YearMonth
    ): List<Routine> {
        return firestoreService.getRoutinesByAthleteForMonth(athleteId, month)
    }

    suspend fun getRoutineById(routineId: String): Routine? {
        return firestoreService.getRoutineById(routineId)
    }
}
