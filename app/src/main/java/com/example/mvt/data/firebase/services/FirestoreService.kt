package com.example.mvt.data.firebase.services

import android.util.Log
import com.example.mvt.data.firebase.models.Routine
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.time.YearMonth
import java.time.ZoneId

class FirestoreService {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getRoutinesByAthlete(athleteId: String): List<Routine> {
        Log.d("FirestoreService", "Consultando rutinas para id_deportista: $athleteId")

        val snapshot = db.collection("rutinas")
            .whereEqualTo("id_deportista", athleteId)
            .get()
            .await()

        Log.d("FirestoreService", "Documentos encontrados: ${snapshot.size()}")

        return snapshot.documents.mapNotNull(::parseRoutineDocument)
    }

    suspend fun getRoutinesByAthleteForMonth(
        athleteId: String,
        month: YearMonth
    ): List<Routine> {
        val zoneId = ZoneId.systemDefault()
        val monthStart = month.atDay(1).atStartOfDay(zoneId).toInstant()
        val nextMonthStart = month.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant()
        val startTimestamp = Timestamp(Date.from(monthStart))
        val endTimestamp = Timestamp(Date.from(nextMonthStart))

        Log.d(
            "FirestoreService",
            "Consultando rutinas para id_deportista: $athleteId en mes: $month"
        )

        val snapshot = db.collection("rutinas")
            .whereEqualTo("id_deportista", athleteId)
            .whereGreaterThanOrEqualTo("fecha", startTimestamp)
            .whereLessThan("fecha", endTimestamp)
            .orderBy("fecha")
            .get()
            .await()

        Log.d("FirestoreService", "Documentos encontrados en $month: ${snapshot.size()}")

        return snapshot.documents.mapNotNull(::parseRoutineDocument)
    }

    private fun parseRoutineDocument(doc: DocumentSnapshot): Routine? {
        val data = doc.data ?: return null

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

        val stravaMap = (data["strava"] as? Map<*, *>)
        val stravaActivityMap = stravaMap?.get("actividad") as? Map<*, *>

        val stravaActivityId =
            extractStringFromMap(stravaActivityMap, listOf("id", "activity_id", "activityId"))
                .ifBlank {
                    extractFirstString(
                        source = data,
                        keys = listOf(
                            "strava_activity_id",
                            "stravaActivityId",
                            "id_actividad_strava",
                            "idActividadStrava",
                            "activity_id",
                            "strava_id",
                            "actividad_id",
                            "id_actividad",
                            "idActividad",
                            "activityId"
                        )
                    )
                }

        val isStravaSynced = extractFirstBoolean(
            source = data,
            keys = listOf(
                "is_strava_synced",
                "strava_synced",
                "sincronizada_strava",
                "sincronizado_strava",
                "isStravaSynced",
                "stravaSync",
                "sincronizadaConStrava",
                "sincronizada",
                "sincronizado"
            )
        ) ?: (stravaActivityMap != null || stravaActivityId.isNotBlank())

        return Routine(
            id = doc.id,
            titulo = data["titulo"] as? String ?: "",
            descripcion = data["descripcion"] as? String ?: "",
            id_deportista = data["id_deportista"] as? String ?: "",
            fecha = fecha,
            completa = data["completa"] as? Boolean ?: false,
            estado = data["estado"] as? String ?: "",
            objetivos = data["objetivos"] as? String ?: "",
            tipo_esfuerzo = data["tipo_esfuerzo"] as? String ?: "",
            tipo_medicion = tipoMedicion,
            tipo_terreno = data["tipo_terreno"] as? String ?: "",
            isStravaSynced = isStravaSynced,
            stravaActivityId = stravaActivityId,
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
    }

    private fun extractFirstString(
        source: Map<String, Any>,
        keys: List<String>
    ): String {
        val normalizedKeys = keys.map { it.lowercase() }.toSet()

        fun scan(map: Map<*, *>): String? {
            map.entries.forEach { (rawKey, value) ->
                val key = rawKey?.toString()?.trim().orEmpty()
                val normalizedKey = key.lowercase()
                if (normalizedKey in normalizedKeys) {
                    val text = value?.toString()?.trim().orEmpty()
                    if (text.isNotBlank() && text.lowercase() != "null") return text
                }

                if (value is Map<*, *>) {
                    scan(value)?.let { return it }
                }
            }
            return null
        }

        return scan(source).orEmpty()
    }

    private fun extractStringFromMap(
        source: Map<*, *>?,
        keys: List<String>
    ): String {
        if (source == null) return ""
        val normalizedKeys = keys.map { it.lowercase() }.toSet()
        source.entries.forEach { (rawKey, value) ->
            val key = rawKey?.toString()?.trim()?.lowercase().orEmpty()
            if (key in normalizedKeys) {
                val text = value?.toString()?.trim().orEmpty()
                if (text.isNotBlank() && text.lowercase() != "null") return text
            }
        }
        return ""
    }

    private fun extractFirstBoolean(
        source: Map<String, Any>,
        keys: List<String>
    ): Boolean? {
        val normalizedKeys = keys.map { it.lowercase() }.toSet()

        fun parseBoolean(value: Any?): Boolean? = when (value) {
            is Boolean -> value
            is Number -> value.toInt() == 1
            is String -> when (value.trim().lowercase()) {
                "true", "si", "sí", "1", "synced", "sincronizada", "sincronizado" -> true
                "false", "0", "no" -> false
                else -> null
            }
            else -> null
        }

        fun scan(map: Map<*, *>): Boolean? {
            map.entries.forEach { (rawKey, value) ->
                val key = rawKey?.toString()?.trim().orEmpty()
                val normalizedKey = key.lowercase()
                if (normalizedKey in normalizedKeys) {
                    parseBoolean(value)?.let { return it }
                }

                if (normalizedKey.contains("strava")) {
                    parseBoolean(value)?.let { return it }
                }

                if (value is Map<*, *>) {
                    scan(value)?.let { return it }
                }
            }
            return null
        }

        return scan(source)
    }
}
