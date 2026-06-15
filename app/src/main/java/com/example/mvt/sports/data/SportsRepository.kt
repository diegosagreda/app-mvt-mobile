package com.example.mvt.sports.data

import android.util.Log
import com.example.mvt.sports.model.ChangedSportsField
import com.example.mvt.sports.model.SportsProfile
import com.example.mvt.sports.model.sportsProfileFromFirebase
import com.google.firebase.Timestamp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SportsRepository(
    database: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val root = database.reference

    suspend fun load(athleteId: String): SportsProfile {
        require(athleteId.isNotBlank()) { "No hay una sesión activa." }
        val snapshot = root.child(SPORTS).child(athleteId).get().await()
        return sportsProfileFromFirebase(snapshot.value)
    }

    suspend fun save(athleteId: String, profile: SportsProfile) {
        require(athleteId.isNotBlank()) { "No hay una sesión activa." }
        root.child(SPORTS).child(athleteId)
            .updateChildren(profile.toFirebaseUpdates())
            .await()
    }

    suspend fun notifyCoach(athleteId: String, changes: List<ChangedSportsField>) {
        if (athleteId.isBlank() || changes.isEmpty()) return
        val requests = root.child(REQUESTS)
            .orderByChild("id_deportista")
            .equalTo(athleteId)
            .get()
            .await()
        val trainerId = requests.children.firstOrNull { request ->
            request.child("estado").value?.toString().orEmpty().equals("Aprobado", ignoreCase = true)
        }?.child("id_entrenador")?.value?.toString().orEmpty()
        if (trainerId.isBlank()) return

        val now = Date()
        firestore.collection(NOTIFICATIONS).add(
            mapOf(
                "tipo" to "deportiva",
                "remitente" to athleteId,
                "destinatario" to trainerId,
                "txt" to "Actualizó su información deportiva",
                "fechaStr" to isoDate(now),
                "fecha" to Timestamp(now),
                "estado" to "no_leido",
                "rutina" to "",
                "fields" to changes.map(ChangedSportsField::toFirebaseMap)
            )
        ).await()
        root.child(USERS).child(athleteId).child("notificaciones").setValue(true).await()
    }

    fun logNotificationError(error: Throwable) {
        Log.w("SportsRepository", "La información se guardó, pero no se pudo notificar al entrenador.", error)
    }

    private fun isoDate(date: Date) = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(date)

    private companion object {
        const val SPORTS = "Deportes"
        const val REQUESTS = "solicitudes"
        const val USERS = "users"
        const val NOTIFICATIONS = "notificaciones"
    }
}
