package com.example.mvt.data.firebase.services

import android.util.Log
import com.example.mvt.data.firebase.models.Routine
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestoreService {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getRoutinesByAthlete(athleteId: String): List<Routine> {
        Log.d("FirestoreService", "Consultando rutinas para id_deportista: $athleteId")

        val snapshot = db.collection("rutinas")
            .whereEqualTo("id_deportista", athleteId)
            .get()
            .await()

        Log.d("FirestoreService", "Documentos encontrados: ${snapshot.size()}")

        return snapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null

            Log.d("FirestoreService", "Documento ${doc.id} => ${data.keys}")

            val fechaValue = data["fecha"]
            val fecha = when (fechaValue) {
                is Timestamp -> fechaValue
                is Map<*, *> -> {
                    val seconds = (fechaValue["_seconds"] as? Number)?.toLong() ?: 0L
                    val nanos = (fechaValue["_nanoseconds"] as? Number)?.toInt() ?: 0
                    Timestamp(Date(seconds * 1000 + nanos / 1000000))
                }
                is Long -> Timestamp(Date(fechaValue))
                else -> null
            }

            // --- Normalizar tipo de medición ---
            val tipoMedicionRaw = (data["tipo_medicion"] as? String)?.lowercase()?.trim()
            val tipoMedicion = when {
                tipoMedicionRaw?.contains("kilometros") == true -> "km"
                tipoMedicionRaw?.contains("metros") == true -> "m"
                else -> tipoMedicionRaw ?: ""
            }

            val rutina = Routine(
                id = doc.id,
                titulo = data["titulo"] as? String ?: "",
                descripcion = data["descripcion"] as? String ?: "",
                id_deportista = data["id_deportista"] as? String ?: "",
                fecha = fecha,
                completa = data["completa"] as? Boolean ?: false,
                estado = data["estado"] as? String ?: "",
                objetivos = data["objetivos"] as? String ?: "",
                tipo_esfuerzo = data["tipo_esfuerzo"] as? String ?: "",
                tipo_medicion = tipoMedicion, // ← abreviado
                tipo_terreno = data["tipo_terreno"] as? String ?: "",
                sesiones_calentamiento = data["sesiones_calentamiento"] as? List<Map<String, Any>> ?: emptyList(),
                sesiones_central = data["sesiones_central"] as? Map<String, Any> ?: emptyMap(),
                sesiones_calma = data["sesiones_calma"] as? List<Map<String, Any>> ?: emptyList(),
                comentarios_fase_calentamiento = data["comentarios_fase_calentamiento"] as? String ?: "",
                comentarios_fase_central = data["comentarios_fase_central"] as? String ?: "",
                comentarios_fase_calma = data["comentarios_fase_calma"] as? String ?: "",
                videosCalentamiento = data["videosCalentamiento"] as? List<String> ?: emptyList(),
                videosCalma = data["videosCalma"] as? List<String> ?: emptyList(),
                videosCentral = data["videosCentral"] as? List<String> ?: emptyList(),
            )

            rutina
        }
    }

}
