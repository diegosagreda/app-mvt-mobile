package com.example.mvt.data.firebase.services

import android.util.Log
import com.example.mvt.data.firebase.models.WeeklyRoutineAnalysis
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class CoachIntelligenceService {

    private val usersRef = FirebaseDatabase.getInstance().getReference("users")

    suspend fun getWeeklyRoutineAnalysis(athleteId: String): WeeklyRoutineAnalysis? {
        if (athleteId.isBlank()) return null

        return try {
            usersRef
                .child(athleteId)
                .child("analisisRutinaSemanal")
                .get()
                .await()
                .getValue(WeeklyRoutineAnalysis::class.java)
        } catch (e: Exception) {
            Log.e("CoachIntelligenceService", "Error cargando analisis semanal", e)
            throw e
        }
    }
}
