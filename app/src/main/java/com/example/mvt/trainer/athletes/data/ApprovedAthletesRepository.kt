package com.example.mvt.trainer.athletes.data

import com.example.mvt.trainer.athletes.model.ApprovedAthlete
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class ApprovedAthletesRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    suspend fun fetchApprovedAthletes(): Result<List<ApprovedAthlete>> = runCatching {
        val trainerId = auth.currentUser?.uid
            ?: error("Usuario no autenticado")
        val requestsSnapshot = database.getReference("solicitudes").get().await()

        requestsSnapshot.children
            .filter {
                it.child("id_entrenador").value?.toString() == trainerId &&
                    it.child("estado").value?.toString().equals("Aprobado", ignoreCase = true)
            }
            .mapNotNull { request ->
                val athleteId = request.child("id_deportista").value?.toString().orEmpty()
                if (athleteId.isBlank()) return@mapNotNull null

                val athleteSnapshot = database.getReference("users").child(athleteId).get().await()
                val firstName = athleteSnapshot.child("nombres").value?.toString().orEmpty()
                val lastName = athleteSnapshot.child("apellidos").value?.toString().orEmpty()
                val requestName = request.child("nombres").value?.toString().orEmpty()
                val plan = athleteSnapshot.child("plan").value
                val planName = when (plan) {
                    is String -> plan
                    is Map<*, *> -> plan["nombre"]?.toString()
                        ?: plan["name"]?.toString()
                        ?: plan["plan"]?.toString()
                        ?: "Sin plan"
                    else -> athleteSnapshot.child("plan_suscrito").value?.toString()
                        ?: "Sin plan"
                }

                ApprovedAthlete(
                    id = athleteId,
                    name = "$firstName $lastName".trim().ifBlank { requestName.ifBlank { "Atleta" } },
                    photoUrl = athleteSnapshot.child("foto_url").value?.toString()
                        ?.takeIf { it.isNotBlank() }
                        ?: request.child("foto").value?.toString()?.takeIf { it.isNotBlank() },
                    sport = athleteSnapshot.child("deporte").value?.toString()
                        ?.takeIf { it.isNotBlank() }
                        ?: request.child("deporte").value?.toString().orEmpty()
                            .ifBlank { "Multideporte" },
                    planName = planName.ifBlank { "Sin plan" }
                )
            }
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }
    }
}
