package com.example.mvt.chat.data.repo

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TrainerInfoRepository(
    private val db: FirebaseFirestore
) {
    /**
     * Ajusta estas rutas/colecciones a tu estructura real.
     *
     * Opción típica:
     * - match/{athleteId} -> { trainerId: "..." }
     * - entrenadores/{trainerId} -> { foto_url: "https://..." }
     */
    suspend fun getTrainerIdForAthlete(athleteId: String): String {
        val snap = db.collection("match").document(athleteId).get().await()
        return snap.getString("trainerId").orEmpty()
    }

    suspend fun getTrainerPhotoUrl(trainerId: String): String {
        val snap = db.collection("entrenadores").document(trainerId).get().await()
        return snap.getString("foto_url").orEmpty()
    }
}