package com.example.mvt.data.firebase.services

import android.net.Uri
import com.example.mvt.BuildConfig
import com.example.mvt.data.firebase.models.StravaConnection
import com.example.mvt.data.firebase.models.StravaConnectionSnapshot
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class StravaService {

    private val auth = FirebaseAuth.getInstance()
    private val realtime = FirebaseDatabase.getInstance().getReference("users")

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

    fun buildAuthorizationUrl(): String {
        val scope = "read,activity:read_all"
        return Uri.parse("https://www.strava.com/oauth/authorize")
            .buildUpon()
            .appendQueryParameter("client_id", BuildConfig.STRAVA_CLIENT_ID)
            .appendQueryParameter("redirect_uri", BuildConfig.STRAVA_REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("approval_prompt", "auto")
            .appendQueryParameter("scope", scope)
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
            val payload = rawBody?.takeIf { it.isNotBlank() && it != "null" }?.let(::JSONObject)

            if (status !in 200..299) {
                val message = payload?.optString("message")
                    ?.takeIf { it.isNotBlank() }
                    ?: "No fue posible completar la operacion con Strava."
                throw IllegalStateException(message)
            }

            payload
        } finally {
            connection.disconnect()
        }
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
}
