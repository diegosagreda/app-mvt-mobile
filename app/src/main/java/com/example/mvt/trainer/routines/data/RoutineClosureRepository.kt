package com.example.mvt.trainer.routines.data

import com.example.mvt.trainer.routines.model.RoutineClosureItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RoutineClosureRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun fetchPending(): Result<List<RoutineClosureItem>> = runCatching {
        val trainerId = auth.currentUser?.uid ?: error("Usuario no autenticado")
        val routines = firestore.collection("rutinas")
            .whereEqualTo("id_entrenador", trainerId)
            .get().await().documents.mapNotNull { doc ->
                val status = doc.getString("estado").orEmpty()
                if (!isPending(status)) return@mapNotNull null
                RoutineClosureItem(
                    id = doc.id,
                    title = doc.getString("titulo").orEmpty().ifBlank { "Rutina sin título" },
                    status = normalizeStatus(status),
                    athleteName = doc.getString("nombre_deportista").orEmpty()
                        .ifBlank { doc.getString("nombres_deportista").orEmpty() }
                        .ifBlank { "Atleta" },
                    date = doc.getTimestamp("fecha"),
                    isDraft = status.equals("Borrador", true)
                )
            }

        val partials = firestore.collection("rutinas_parciales")
            .whereEqualTo("id_entrenador", trainerId)
            .get().await().documents.map { doc ->
                RoutineClosureItem(
                    id = doc.id,
                    title = doc.getString("titulo").orEmpty().ifBlank { "Rutina en borrador" },
                    status = "Borrador",
                    athleteName = doc.getString("nombre_deportista").orEmpty()
                        .ifBlank { doc.getString("nombres_deportista").orEmpty() }
                        .ifBlank { "Atleta" },
                    date = doc.getTimestamp("fecha"),
                    isDraft = true
                )
            }

        (routines + partials).distinctBy { it.id }
            .sortedByDescending { it.date?.toDate()?.time ?: 0L }
    }

    private fun isPending(status: String): Boolean =
        status.equals("Pendiente", true) || status.equals("Borrador", true)

    private fun normalizeStatus(status: String): String =
        if (status.equals("Borrador", true)) "Borrador" else "Pendiente"
}
