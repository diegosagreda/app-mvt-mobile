package com.example.mvt.trainer.requests.data

import com.example.mvt.trainer.requests.model.PendingTrainerRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.time.Instant

class PendingTrainerRequestsRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun fetchPending(): Result<List<PendingTrainerRequest>> = runCatching {
        val trainerId = auth.currentUser?.uid ?: error("Usuario no autenticado")
        database.getReference("solicitudes").get().await().children
            .filter {
                it.child("id_entrenador").value?.toString() == trainerId &&
                    it.child("estado").value?.toString().equals("Pendiente", ignoreCase = true)
            }
            .map { request ->
                PendingTrainerRequest(
                    id = request.key.orEmpty(),
                    athleteId = request.child("id_deportista").value?.toString().orEmpty(),
                    athleteName = request.child("nombres").value?.toString()
                        ?.takeIf { it.isNotBlank() } ?: "Atleta",
                    sport = request.child("deporte").value?.toString()
                        ?.takeIf { it.isNotBlank() } ?: "Multideporte",
                    photoUrl = request.child("foto").value?.toString()
                        ?.takeIf { it.isNotBlank() },
                    registeredAt = request.child("fecha_registro").value.toLongOrNull()
                )
            }
            .sortedByDescending { it.registeredAt ?: 0L }
    }

    suspend fun updateStatus(request: PendingTrainerRequest, status: String): Result<Unit> = runCatching {
        val trainerId = auth.currentUser?.uid ?: error("Usuario no autenticado")
        val requestId = request.id
        require(requestId.isNotBlank()) { "Solicitud inválida" }
        database.getReference("solicitudes").child(requestId)
            .updateChildren(mapOf("estado" to status))
            .await()

        if (status.equals("Aprobado", ignoreCase = true)) {
            val routines = firestore.collection("rutinas")
                .whereEqualTo("id_deportista", request.athleteId)
                .get()
                .await()
            routines.documents.forEach { routine ->
                routine.reference.update("id_entrenador", trainerId).await()
            }
        }

        firestore.collection("notificaciones").add(
            mapOf(
                "tipo" to "match",
                "remitente" to trainerId,
                "destinatario" to request.athleteId,
                "txt" to if (status.equals("Aprobado", true)) {
                    "Solicitud aceptada"
                } else {
                    "Solicitud rechazada"
                },
                "fecha" to Timestamp.now(),
                "fechaStr" to Instant.now().toString(),
                "estado" to "no_leido"
            )
        ).await()

        database.getReference("users").child(request.athleteId)
            .updateChildren(mapOf("notificaciones" to true))
            .await()
    }
}

private fun Any?.toLongOrNull(): Long? = when (this) {
    is Number -> toLong()
    is String -> toLongOrNull()
    else -> null
}
