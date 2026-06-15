package com.example.mvt.data.firebase.services

import android.util.Log
import com.example.mvt.data.firebase.models.Routine
import com.example.mvt.data.firebase.models.RoutineStrava
import com.example.mvt.data.firebase.models.StravaActivity
import com.example.mvt.data.firebase.models.StravaAnalysis
import com.example.mvt.data.firebase.models.StravaCompliance
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

    suspend fun getRoutineById(routineId: String): Routine? {
        if (routineId.isBlank()) return null

        val snapshot = db.collection("rutinas")
            .document(routineId)
            .get()
            .await()

        if (!snapshot.exists()) return null
        return parseRoutineDocument(snapshot)
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
        val strava = stravaMap?.let(::parseRoutineStrava)

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
            strava = strava,
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

    private fun parseRoutineStrava(source: Map<*, *>): RoutineStrava {
        val actividad = parseStravaActivity(source["actividad"] as? Map<*, *>)
        val cumplimiento = parseStravaCompliance(source["cumplimiento"] as? Map<*, *>)
        val analisis = parseStravaAnalysis(source["analisis"] as? Map<*, *>)

        return RoutineStrava(
            estado = extractString(source["estado"]),
            sincronizadoEn = extractDateText(source["sincronizado_en"]),
            desvinculadoManualEn = extractDateText(source["desvinculado_manual_en"]),
            actividadesIgnoradas = extractStringList(source["actividades_ignoradas"]),
            actividad = actividad,
            cumplimiento = cumplimiento,
            analisis = analisis,
            error = extractString(source["error"])
        )
    }

    private fun parseStravaActivity(source: Map<*, *>?): StravaActivity? {
        if (source == null || source.isEmpty()) return null

        return StravaActivity(
            id = extractString(source["id"]),
            name = extractString(source["name"]),
            type = extractString(source["type"]),
            sportType = extractString(source["sport_type"]),
            startDate = extractDateText(source["start_date"]),
            startDateLocal = extractDateText(source["start_date_local"]),
            elapsedTime = extractInt(source["elapsed_time"]),
            movingTime = extractInt(source["moving_time"]),
            distance = extractDouble(source["distance"]),
            averageSpeed = extractDouble(source["average_speed"]),
            maxSpeed = extractDouble(source["max_speed"]),
            averageHeartrate = extractDouble(source["average_heartrate"]),
            maxHeartrate = extractDouble(source["max_heartrate"]),
            totalElevationGain = extractDouble(source["total_elevation_gain"]),
            sufferScore = extractDouble(source["suffer_score"]),
            externalId = extractString(source["external_id"])
        )
    }

    private fun parseStravaCompliance(source: Map<*, *>?): StravaCompliance? {
        if (source == null || source.isEmpty()) return null

        return StravaCompliance(
            porcentaje = extractDouble(source["porcentaje"]),
            estado = extractString(source["estado"]),
            metrica = extractString(source["metrica"]),
            planificado = extractDouble(source["planificado"]),
            realizado = extractDouble(source["realizado"]),
            unidad = extractString(source["unidad"])
        )
    }

    private fun parseStravaAnalysis(source: Map<*, *>?): StravaAnalysis? {
        if (source == null || source.isEmpty()) return null

        return StravaAnalysis(
            estadoTexto = extractString(source["estado_texto"]),
            lecturaEntrenador = extractString(source["lectura_entrenador"]),
            diferencia = extractDouble(source["diferencia"]),
            diferenciaTexto = extractString(source["diferencia_texto"]),
            distanciaKm = extractDouble(source["distancia_km"]),
            tiempoMovimientoSeg = extractInt(source["tiempo_movimiento_seg"]),
            ritmoSegPorKm = extractDouble(source["ritmo_seg_por_km"]),
            velocidadPromedioKmh = extractDouble(source["velocidad_promedio_kmh"]),
            velocidadMaximaKmh = extractDouble(source["velocidad_maxima_kmh"]),
            frecuenciaPromedio = extractDouble(source["frecuencia_promedio"]),
            frecuenciaMaxima = extractDouble(source["frecuencia_maxima"]),
            desnivelM = extractDouble(source["desnivel_m"]),
            sincronizadoEn = extractDateText(source["sincronizado_en"])
        )
    }

    private fun extractString(value: Any?): String {
        return when (value) {
            null -> ""
            is String -> value.trim()
            else -> value.toString().trim()
        }.takeUnless { it.equals("null", ignoreCase = true) }.orEmpty()
    }

    private fun extractDouble(value: Any?): Double? {
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.replace(",", ".").toDoubleOrNull()
            else -> null
        }
    }

    private fun extractInt(value: Any?): Int? {
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }

    private fun extractStringList(value: Any?): List<String> {
        return when (value) {
            is List<*> -> value.mapNotNull { item ->
                item?.toString()?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            }
            else -> emptyList()
        }
    }

    private fun extractDateText(value: Any?): String {
        return when (value) {
            is Timestamp -> value.toDate().time.toString()
            is Date -> value.time.toString()
            is Map<*, *> -> {
                val seconds = (value["_seconds"] as? Number)?.toLong()
                val nanos = (value["_nanoseconds"] as? Number)?.toLong() ?: 0L
                if (seconds != null) {
                    (seconds * 1000L + nanos / 1_000_000L).toString()
                } else {
                    ""
                }
            }
            is Number -> value.toLong().toString()
            is String -> value.trim()
            else -> ""
        }
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
