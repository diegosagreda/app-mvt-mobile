package com.example.mvt.data.firebase.services

import com.example.mvt.data.firebase.models.AppNotification
import com.example.mvt.data.firebase.models.NotificationSenderPreview
import com.google.firebase.Timestamp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class NotificationService {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersRef = FirebaseDatabase.getInstance().getReference("users")

    fun listenRecentNotificationsByRecipient(
        uid: String,
        limit: Int,
        onUpdate: (List<AppNotification>) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration {
        return firestore.collection("notificaciones")
            .whereEqualTo("destinatario", uid)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents
                    ?.mapNotNull(::parseNotificationDocument)
                    .orEmpty()

                onUpdate(notifications)
            }
    }

    fun listenUnreadNotificationsByRecipient(
        uid: String,
        onUpdate: (List<AppNotification>) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration {
        return firestore.collection("notificaciones")
            .whereEqualTo("destinatario", uid)
            .whereEqualTo("estado", "no_leido")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents
                    ?.mapNotNull(::parseNotificationDocument)
                    .orEmpty()

                onUpdate(notifications)
            }
    }

    suspend fun getNotificationsPageByRecipient(
        uid: String,
        pageSize: Int,
        startAfter: DocumentSnapshot? = null
    ): NotificationPageResult {
        var query = firestore.collection("notificaciones")
            .whereEqualTo("destinatario", uid)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit((pageSize + 1).toLong())

        if (startAfter != null) {
            query = query.startAfter(startAfter)
        }

        val documents = query.get().await().documents
        val hasMore = documents.size > pageSize
        val pageDocuments = if (hasMore) documents.dropLast(1) else documents

        return NotificationPageResult(
            notifications = pageDocuments.mapNotNull(::parseNotificationDocument),
            lastDocument = pageDocuments.lastOrNull(),
            hasMore = hasMore
        )
    }

    suspend fun getUnreadNotificationsByRecipient(uid: String): List<AppNotification> {
        return firestore.collection("notificaciones")
            .whereEqualTo("destinatario", uid)
            .whereEqualTo("estado", "no_leido")
            .get()
            .await()
            .documents
            .mapNotNull(::parseNotificationDocument)
    }

    suspend fun markAsRead(notificationId: String) {
        if (notificationId.isBlank()) return

        firestore.collection("notificaciones")
            .document(notificationId)
            .update("estado", "leido")
            .await()
    }

    suspend fun markAsRead(notificationIds: List<String>) = coroutineScope {
        notificationIds
            .filter { it.isNotBlank() }
            .distinct()
            .map { notificationId ->
                async {
                    firestore.collection("notificaciones")
                        .document(notificationId)
                        .update("estado", "leido")
                        .await()
                }
            }
            .awaitAll()
    }

    suspend fun getSenderPreview(uid: String): NotificationSenderPreview {
        if (uid.isBlank()) return NotificationSenderPreview()

        val snapshot = usersRef.child(uid).get().await()
        val nombres = snapshot.child("nombres").getValue(String::class.java).orEmpty().trim()
        val apellidos = snapshot.child("apellidos").getValue(String::class.java).orEmpty().trim()
        val fotoUrl = snapshot.child("foto_url").getValue(String::class.java).orEmpty().trim()
        val fullName = listOf(nombres, apellidos)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        return NotificationSenderPreview(
            uid = uid,
            name = fullName,
            avatarUrl = fotoUrl
        )
    }

    private fun parseNotificationDocument(doc: DocumentSnapshot): AppNotification? {
        val data = doc.data ?: return null
        val fechaStr = data["fechaStr"] as? String ?: ""

        return AppNotification(
            id = doc.id,
            destinatario = data["destinatario"] as? String ?: "",
            remitente = data["remitente"] as? String ?: "",
            tipo = data["tipo"] as? String ?: "",
            txt = data["txt"] as? String ?: "",
            estado = data["estado"] as? String ?: "",
            fecha = parseTimestamp(data["fecha"], fechaStr),
            fechaStr = fechaStr,
            rutina = data["rutina"] as? String ?: "",
            stravaActivityId = data["strava_activity_id"]?.toString().orEmpty(),
            fields = data["fields"] as? String ?: ""
        )
    }

    private fun parseTimestamp(value: Any?, fallbackIso: String): Timestamp? {
        return when (value) {
            is Timestamp -> value
            is Date -> Timestamp(value)
            is Number -> Timestamp(Date(value.toLong()))
            is String -> parseIsoTimestamp(value)
            is Map<*, *> -> {
                val seconds = (value["_seconds"] as? Number)?.toLong() ?: 0L
                val nanos = (value["_nanoseconds"] as? Number)?.toInt() ?: 0
                if (seconds == 0L && nanos == 0) {
                    parseIsoTimestamp(fallbackIso)
                } else {
                    Timestamp(Date(seconds * 1000 + nanos / 1_000_000))
                }
            }
            else -> parseIsoTimestamp(fallbackIso)
        }
    }

    private fun parseIsoTimestamp(raw: String): Timestamp? {
        val source = raw.trim()
        if (source.isBlank()) return null

        source.toLongOrNull()?.let { return Timestamp(Date(it)) }

        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd HH:mm:ss"
        )

        for (pattern in patterns) {
            runCatching {
                val formatter = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                formatter.parse(source)
            }.getOrNull()?.let { parsed ->
                return Timestamp(parsed)
            }
        }

        return null
    }
}

data class NotificationPageResult(
    val notifications: List<AppNotification>,
    val lastDocument: DocumentSnapshot?,
    val hasMore: Boolean
)
