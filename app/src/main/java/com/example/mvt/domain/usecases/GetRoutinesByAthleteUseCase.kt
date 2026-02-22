package com.example.mvt.domain.usecases

import com.example.mvt.data.firebase.models.Routine
import com.example.mvt.data.firebase.repositories.RoutineRepository

class GetRoutinesByAthleteUseCase(private val repository: RoutineRepository) {
    suspend operator fun invoke(athleteId: String): List<Routine> {
        return repository.getRoutinesByAthlete(athleteId)
    }
}
