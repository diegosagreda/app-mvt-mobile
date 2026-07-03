package com.example.mvt.data.firebase.services

import android.net.Uri
import com.example.mvt.BuildConfig
import com.example.mvt.data.firebase.models.StravaActivitiesResult
import com.example.mvt.data.firebase.models.StravaActivity
import com.example.mvt.data.firebase.models.StravaAnalysis
import com.example.mvt.data.firebase.models.StravaConnection
import com.example.mvt.data.firebase.models.StravaConnectionSnapshot
import com.example.mvt.data.firebase.models.StravaCompliance
import com.example.mvt.data.firebase.models.StravaRoutineActionResult
import com.example.mvt.data.firebase.models.RoutineStrava
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class StravaService {

    private val auth = FirebaseAuth.getInstance()
    private val realtime = FirebaseDatabase.getInstance().getReference("users")
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getConnectionSnapshot(): StravaConnectionSnapshot {
        val uid = requireUid()
        val enabled = getSyncEnabled(uid)
        if (!enabled) return StravaConnectionSnapshot(isEnabled = false, connection = null)

        val connection = postToFunctions(
            endpoint = "stravaConnectionStatus",
            body = JSONObject().put("id_deportista", uid)
        )

        return StravaConnectionSnapshot(
            isEnabled = true,
            connection = connection?.let(::parseConnection)
        )
    }

    suspend fun setSyncEnabled(enabled: Boolean) {
        val uid = requireUid()
        realtime.child(uid).child("showStrava").setValue(enabled).await()
    }

    suspend fun disconnect() {
        val uid = requireUid()
        val connectionSnapshot = runCatching { getConnectionSnapshotOrNull(uid) }.getOrNull()

        deleteFirestoreConnections(uid, connectionSnapshot)
    }

    suspend fun exchangeToken(code: String, scope: String?): StravaConnection {
        val uid = requireUid()
        setSyncEnabled(true)

        val response = postToFunctions(
            endpoint = "stravaExchangeToken",
            body = JSONObject()
                .put("id_deportista", uid)
                .put("code", code)
                .put("scope", scope ?: "")
        ) ?: throw IllegalStateException("Strava no devolvio datos de conexion.")

        return parseConnection(response)
    }

    suspend fun listActivities(
        idDeportista: String,
        fecha: String? = null,
        limit: Int = 20
    ): StravaActivitiesResult {
        val requestLimit = limit.coerceIn(1, 50)
        val body = JSONObject()
            .put("id_deportista", idDeportista)
            .put("limit", requestLimit)

        if (!fecha.isNullOrBlank()) {
            body.put("fecha", fecha)
        }

        val response = postToFunctions(
            endpoint = "stravaListActivities",
            body = body
        ) ?: JSONObject()

        return StravaActivitiesResult(
            estado = response.optString("estado"),
            actividades = parseActivities(response.optJSONArray("actividades")),
            mensaje = response.optString("mensaje"),
            fechaConsulta = response.optString("fecha_consulta"),
            limit = response.optInt("limit", requestLimit)
        )
    }

    suspend fun linkActivityToRoutine(
        idDeportista: String,
        routineId: String,
        activityId: String
    ): StravaRoutineActionResult {
        val response = postToFunctions(
            endpoint = "stravaLinkActivity",
            body = JSONObject()
                .put("id_deportista", idDeportista)
                .put("rutina_id", routineId)
                .put("activity_id", activityId.toLongOrNull() ?: activityId)
        ) ?: JSONObject()

        return StravaRoutineActionResult(
            estado = response.optString("estado"),
            mensaje = response.optString("mensaje"),
            strava = response.optJSONObject("strava")?.let(::parseRoutineStrava),
            actividad = response.optJSONObject("actividad")?.let(::parseActivity),
            cumplimiento = response.optJSONObject("cumplimiento")?.let(::parseCompliance)
        )
    }

    suspend fun unlinkActivityFromRoutine(
        idDeportista: String,
        routineId: String
    ): StravaRoutineActionResult {
        val response = postToFunctions(
            endpoint = "stravaUnlinkActivity",
            body = JSONObject()
                .put("id_deportista", idDeportista)
                .put("rutina_id", routineId)
        ) ?: JSONObject()

        return StravaRoutineActionResult(
            estado = response.optString("estado"),
            mensaje = response.optString("mensaje"),
            strava = response.optJSONObject("strava")?.let(::parseRoutineStrava)
        )
    }

    fun buildAuthorizationUrl(state: String): String {
        val scope = "read,activity:read_all"
        return Uri.parse("https://www.strava.com/oauth/mobile/authorize")
            .buildUpon()
            .appendQueryParameter("client_id", BuildConfig.STRAVA_CLIENT_ID)
            .appendQueryParameter("redirect_uri", BuildConfig.STRAVA_REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("approval_prompt", "auto")
            .appendQueryParameter("scope", scope)
            .appendQueryParameter("state", state)
            .build()
            .toString()
    }

    fun getRedirectUri(): String = BuildConfig.STRAVA_REDIRECT_URI

    private suspend fun getSyncEnabled(uid: String): Boolean {
        val snapshot = realtime.child(uid).child("showStrava").get().await()
        return snapshot.getValue(Boolean::class.java) == true
    }

    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado.")

    private suspend fun postToFunctions(
        endpoint: String,
        body: JSONObject
    ): JSONObject? = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: throw IllegalStateException("Usuario no autenticado.")
        val token = user.getIdToken(false).await().token
            ?: throw IllegalStateException("No fue posible obtener el token del usuario.")

        val url = URL("${BuildConfig.STRAVA_FUNCTIONS_BASE_URL}/$endpoint")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doInput = true
            doOutput = true
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
        }

        try {
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body.toString())
                writer.flush()
            }

            val status = connection.responseCode
            val rawBody = readBody(connection, status in 200..299)
            val payload = rawBody
                ?.takeIf { it.isNotBlank() && it != "null" }
                ?.let { bodyText -> runCatching { JSONObject(bodyText) }.getOrNull() }

            if (status !in 200..299) {
                val backendMessage = payload?.optString("message")
                    ?.takeIf { it.isNotBlank() }
                val backendError = payload?.opt("error")?.let(::extractErrorMessage)
                val fallbackBody = rawBody
                    ?.trim()
                    ?.takeIf { it.isNotBlank() && !it.startsWith("<!DOCTYPE", ignoreCase = true) }
                    ?.take(240)

                val message = listOfNotNull(
                    backendMessage,
                    backendError?.takeUnless { it.equals(backendMessage, ignoreCase = true) },
                    fallbackBody?.takeUnless {
                        !backendMessage.isNullOrBlank() || !backendError.isNullOrBlank()
                    }
                ).joinToString(" · ").ifBlank {
                    "No fue posible completar la operacion con Strava."
                }
                throw IllegalStateException(message)
            }

            payload
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun getConnectionSnapshotOrNull(uid: String): StravaConnection? {
        val connection = postToFunctions(
            endpoint = "stravaConnectionStatus",
            body = JSONObject().put("id_deportista", uid)
        )

        return connection?.let(::parseConnection)
    }

    private suspend fun deleteFirestoreConnections(uid: String, connection: StravaConnection?) {
        val collection = firestore.collection("strava_conexiones")
        val references = linkedMapOf<String, com.google.firebase.firestore.DocumentReference>()

        fun track(reference: com.google.firebase.firestore.DocumentReference) {
            references[reference.path] = reference
        }

        track(collection.document(uid))

        connection?.id
            ?.takeIf { it.isNotBlank() }
            ?.let { connectionId ->
                track(collection.document(connectionId))

                collection.whereEqualTo("id", connectionId)
                    .get()
                    .await()
                    .documents
                    .forEach { track(it.reference) }
            }

        collection.whereEqualTo("id_deportista", uid)
            .get()
            .await()
            .documents
            .forEach { track(it.reference) }

        connection?.athleteId?.let { athleteId ->
            collection.whereEqualTo("athlete_id", athleteId)
                .get()
                .await()
                .documents
                .forEach { track(it.reference) }
        }

        if (references.isEmpty()) return

        val batch = firestore.batch()
        references.values.forEach(batch::delete)
        batch.commit().await()
    }

    private fun readBody(connection: HttpURLConnection, successful: Boolean): String? {
        val stream = if (successful) connection.inputStream else connection.errorStream ?: return null
        return BufferedReader(stream.reader()).use { it.readText() }
    }

    private fun parseConnection(json: JSONObject): StravaConnection {
        val updatedAt = when {
            json.has("actualizado_en") && json.get("actualizado_en") is JSONObject -> {
                val obj = json.getJSONObject("actualizado_en")
                obj.optString("_seconds").ifBlank { obj.toString() }
            }
            else -> json.optString("actualizado_en")
        }

        return StravaConnection(
            id = json.optString("id"),
            idDeportista = json.optString("id_deportista"),
            estado = json.optString("estado"),
            athleteId = json.optLong("athlete_id").takeIf { json.has("athlete_id") },
            athleteName = json.optString("athlete_name"),
            scope = json.optString("scope"),
            expiresAt = json.optLong("expires_at").takeIf { json.has("expires_at") },
            actualizadoEn = updatedAt
        )
    }

    private fun parseActivities(array: JSONArray?): List<StravaActivity> {
        if (array == null) return emptyList()

        return buildList {
            for (index in 0 until array.length()) {
                val activity = array.optJSONObject(index) ?: continue
                add(parseActivity(activity))
            }
        }
    }

    private fun parseActivity(json: JSONObject): StravaActivity {
        return StravaActivity(
            id = json.opt("id")?.toString().orEmpty(),
            name = json.optString("name"),
            type = json.optString("type"),
            sportType = json.optString("sport_type"),
            startDate = json.optString("start_date"),
            startDateLocal = json.optString("start_date_local"),
            elapsedTime = json.optInt("elapsed_time").takeIf { json.has("elapsed_time") },
            movingTime = json.optInt("moving_time").takeIf { json.has("moving_time") },
            distance = json.optDouble("distance").takeIf { json.has("distance") && !it.isNaN() },
            averageSpeed = json.optDouble("average_speed")
                .takeIf { json.has("average_speed") && !it.isNaN() },
            maxSpeed = json.optDouble("max_speed").takeIf { json.has("max_speed") && !it.isNaN() },
            averageHeartrate = json.optDouble("average_heartrate")
                .takeIf { json.has("average_heartrate") && !it.isNaN() },
            maxHeartrate = json.optDouble("max_heartrate")
                .takeIf { json.has("max_heartrate") && !it.isNaN() },
            totalElevationGain = json.optDouble("total_elevation_gain")
                .takeIf { json.has("total_elevation_gain") && !it.isNaN() },
            sufferScore = json.optDouble("suffer_score")
                .takeIf { json.has("suffer_score") && !it.isNaN() },
            externalId = json.optString("external_id")
        )
    }

    private fun parseRoutineStrava(json: JSONObject): RoutineStrava {
        return RoutineStrava(
            estado = json.optString("estado"),
            sincronizadoEn = extractDateText(json.opt("sincronizado_en")),
            desvinculadoManualEn = extractDateText(json.opt("desvinculado_manual_en")),
            actividadesIgnoradas = parseStringList(json.optJSONArray("actividades_ignoradas")),
            actividad = json.optJSONObject("actividad")?.let(::parseActivity),
            cumplimiento = json.optJSONObject("cumplimiento")?.let(::parseCompliance),
            analisis = json.optJSONObject("analisis")?.let(::parseAnalysis),
            error = json.optString("error")
        )
    }

    private fun parseCompliance(json: JSONObject): StravaCompliance {
        return StravaCompliance(
            porcentaje = json.optDouble("porcentaje").takeIf { json.has("porcentaje") && !it.isNaN() },
            estado = json.optString("estado"),
            metrica = json.optString("metrica"),
            planificado = json.optDouble("planificado").takeIf { json.has("planificado") && !it.isNaN() },
            realizado = json.optDouble("realizado").takeIf { json.has("realizado") && !it.isNaN() },
            unidad = json.optString("unidad")
        )
    }

    private fun parseAnalysis(json: JSONObject): StravaAnalysis {
        return StravaAnalysis(
            estadoTexto = json.optString("estado_texto"),
            lecturaEntrenador = json.optString("lectura_entrenador"),
            diferencia = json.optDouble("diferencia").takeIf { json.has("diferencia") && !it.isNaN() },
            diferenciaTexto = json.optString("diferencia_texto"),
            distanciaKm = json.optDouble("distancia_km").takeIf { json.has("distancia_km") && !it.isNaN() },
            tiempoMovimientoSeg = json.optInt("tiempo_movimiento_seg").takeIf { json.has("tiempo_movimiento_seg") },
            ritmoSegPorKm = json.optDouble("ritmo_seg_por_km").takeIf { json.has("ritmo_seg_por_km") && !it.isNaN() },
            velocidadPromedioKmh = json.optDouble("velocidad_promedio_kmh")
                .takeIf { json.has("velocidad_promedio_kmh") && !it.isNaN() },
            velocidadMaximaKmh = json.optDouble("velocidad_maxima_kmh")
                .takeIf { json.has("velocidad_maxima_kmh") && !it.isNaN() },
            frecuenciaPromedio = json.optDouble("frecuencia_promedio")
                .takeIf { json.has("frecuencia_promedio") && !it.isNaN() },
            frecuenciaMaxima = json.optDouble("frecuencia_maxima")
                .takeIf { json.has("frecuencia_maxima") && !it.isNaN() },
            desnivelM = json.optDouble("desnivel_m").takeIf { json.has("desnivel_m") && !it.isNaN() },
            sincronizadoEn = extractDateText(json.opt("sincronizado_en"))
        )
    }

    private fun extractDateText(value: Any?): String {
        return when (value) {
            is JSONObject -> {
                val seconds = value.optLong("_seconds").takeIf { value.has("_seconds") }
                val nanos = value.optLong("_nanoseconds").takeIf { value.has("_nanoseconds") } ?: 0L
                if (seconds != null) {
                    (seconds * 1000L + nanos / 1_000_000L).toString()
                } else {
                    value.toString()
                }
            }
            is Number -> value.toLong().toString()
            null -> ""
            else -> value.toString()
        }
    }

    private fun parseStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.opt(index)?.toString()?.trim().orEmpty()
                if (item.isNotBlank() && !item.equals("null", ignoreCase = true)) {
                    add(item)
                }
            }
        }
    }

    private fun extractErrorMessage(value: Any?): String? {
        return when (value) {
            is JSONObject -> {
                val primary = value.optString("message").takeIf { it.isNotBlank() }
                val firstError = value.optJSONArray("errors")
                    ?.optJSONObject(0)
                    ?.let { error ->
                        listOf(
                            error.optString("resource").takeIf { it.isNotBlank() },
                            error.optString("field").takeIf { it.isNotBlank() },
                            error.optString("code").takeIf { it.isNotBlank() }
                        ).joinToString(" / ").ifBlank { null }
                    }

                listOfNotNull(primary, firstError).joinToString(" · ").ifBlank { value.toString() }
            }
            is JSONArray -> value.toString()
            null -> null
            else -> value.toString().takeIf { it.isNotBlank() }
        }
    }
}
