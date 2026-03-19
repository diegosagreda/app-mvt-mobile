package com.example.mvt.chat.data.repo

import android.net.Uri
import android.util.Log
import com.example.mvt.chat.data.model.ChatMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository(
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val TAG = "ChatRepository"

    init {
        Log.e(TAG, "CREATED repo=${System.identityHashCode(this)} db=${System.identityHashCode(db)} storage=${System.identityHashCode(storage)}")
    }

    private fun participantsKey(a: String, b: String): String {
        val (x, y) = listOf(a, b).sorted()
        return "${x}_${y}"
    }


    suspend fun findConversationId(uid: String, otherUid: String): String? {
        val key = participantsKey(uid, otherUid)
        Log.e(TAG, "[findConversationId] start uid=$uid otherUid=$otherUid key=$key")

        // --- 1) Intento por participantsKey ---
        try {
            val snap = db.collection("chat")
                .whereEqualTo("participantsKey", key)
                .limit(1)
                .get()
                .await()

            val id = snap.documents.firstOrNull()?.id
            Log.e(TAG, "[findConversationId] byKey docs=${snap.size()} id=${id ?: "null"}")

            if (!id.isNullOrBlank()) return id
        } catch (e: Exception) {
            Log.e(TAG, "[findConversationId] byKey ERROR: ${e.message}", e)
        }

        // --- 2) Fallback estilo web: array-contains ---
        try {
            Log.e(TAG, "[findConversationId] fallback array-contains uid=$uid")
            val snap = db.collection("chat")
                .whereArrayContains("participantes", uid)
                .get()
                .await()

            var conversacionId: String? = null

            snap.documents.forEach { doc ->
                val participantes = doc.get("participantes") as? List<*>
                val hasOther = participantes?.any { it?.toString() == otherUid } == true

                Log.e(
                    TAG,
                    "[findConversationId][fallback] doc=${doc.id} participantes=${participantes?.joinToString()} hasOther=$hasOther"
                )

                if (hasOther) conversacionId = doc.id
            }

            Log.e(TAG, "[findConversationId] fallback result id=${conversacionId ?: "null"} scanned=${snap.size()}")

            // Si la encontró por fallback, opcionalmente “migra” el doc y guarda participantsKey
            if (!conversacionId.isNullOrBlank()) {
                runCatching {
                    Log.e(TAG, "[findConversationId] migrating participantsKey doc=$conversacionId key=$key")
                    db.collection("chat").document(conversacionId!!)
                        .update("participantsKey", key)
                        .await()
                }.onFailure { err ->
                    Log.e(TAG, "[findConversationId] migrate participantsKey failed: ${err.message}")
                }
            }

            return conversacionId
        } catch (e: Exception) {
            Log.e(TAG, "[findConversationId] fallback ERROR: ${e.message}", e)
            return null
        }
    }

    fun listenConversation(
        conversationId: String,
        onChange: (List<ChatMessage>) -> Unit
    ): ListenerRegistration {
        Log.e(TAG, "[listenConversation] attach conversationId=$conversationId")

        return db.collection("chat")
            .document(conversationId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "[listenConversation] ERROR: ${err.message}", err)
                    onChange(emptyList())
                    return@addSnapshotListener
                }

                if (snap == null) {
                    Log.e(TAG, "[listenConversation] snap is NULL")
                    onChange(emptyList())
                    return@addSnapshotListener
                }

                if (!snap.exists()) {
                    Log.e(TAG, "[listenConversation] snap NOT EXISTS conversationId=$conversationId")
                    onChange(emptyList())
                    return@addSnapshotListener
                }

                val data = snap.data
                if (data == null) {
                    Log.e(TAG, "[listenConversation] data is NULL conversationId=$conversationId")
                    onChange(emptyList())
                    return@addSnapshotListener
                }

                val mensajesMap = data["mensajes"] as? Map<*, *>
                val mapSize = mensajesMap?.size ?: 0
                val keysPreview = mensajesMap?.keys?.take(5)?.joinToString()

                Log.e(
                    TAG,
                    "[listenConversation] snapshot OK conversationId=$conversationId mensajesMapSize=$mapSize keysPreview=$keysPreview"
                )

                val list = (mensajesMap ?: emptyMap<Any, Any>()).entries.mapNotNull { (k, v) ->
                    val id = k?.toString() ?: return@mapNotNull null
                    val m = v as? Map<*, *> ?: return@mapNotNull null
                    val iso = m["timestamp"]?.toString().orEmpty()
                    val ts = isoToEpochSeconds(iso)

                    ChatMessage(
                        id = id,
                        texto = m["texto"]?.toString().orEmpty(),
                        audioUrl = m["audioUrl"]?.toString().orEmpty(),
                        imageUrl = m["imageUrl"]?.toString().orEmpty(),
                        remitente = m["remitente"]?.toString().orEmpty(),
                        timestamp = iso,
                        rutina = m["rutina"]?.toString()
                    )
                }.sortedBy { it.timestamp }

                val first = list.firstOrNull()
                val last = list.lastOrNull()
                Log.e(
                    TAG,
                    "[listenConversation] parsed listSize=${list.size} firstId=${first?.id} firstTs=${first?.timestamp} lastId=${last?.id} lastTs=${last?.timestamp}"
                )

                onChange(list)
            }
    }

    suspend fun createConversation(participantes: List<String>, firstMessage: ChatMessage): String {
        val docRef = db.collection("chat").document()
        val messageId = "mensajeID${System.currentTimeMillis()}"

        val (a, b) = participantes
        val key = participantsKey(a, b)

        val payload = hashMapOf(
            "participantes" to listOf(a, b),
            "participantsKey" to key,
            "lecturaDeportista" to true,
            "lecturaEntrenador" to false,
            "mensajes" to mapOf(messageId to firstMessage)
        )

        Log.e(TAG, "[createConversation] creating doc=${docRef.id} key=$key participantes=${participantes.joinToString()} firstMessageId=$messageId")
        docRef.set(payload).await()
        Log.e(TAG, "[createConversation] created doc=${docRef.id}")
        return docRef.id
    }

    suspend fun addMessage(conversationId: String, message: ChatMessage, senderRole: String) {
        val messageId = "mensajeID${System.currentTimeMillis()}"
        Log.e(TAG, "[addMessage] convo=$conversationId messageId=$messageId role=$senderRole textLen=${message.texto.length} hasAudio=${message.audioUrl.isNotBlank()} hasImg=${message.imageUrl.isNotBlank()}")

        val updates = hashMapOf<String, Any>(
            "mensajes.$messageId" to message,
            "lecturaDeportista" to (senderRole == "deportista"),
            "lecturaEntrenador" to (senderRole == "entrenador")
        )
        db.collection("chat").document(conversationId).update(updates).await()
        Log.e(TAG, "[addMessage] done convo=$conversationId messageId=$messageId")
    }

    suspend fun updateMessage(conversationId: String, messageId: String, newText: String, senderRole: String) {
        Log.e(TAG, "[updateMessage] convo=$conversationId messageId=$messageId role=$senderRole newTextLen=${newText.length}")

        val updates = hashMapOf<String, Any>(
            "mensajes.$messageId.texto" to newText,
            "lecturaDeportista" to (senderRole == "deportista"),
            "lecturaEntrenador" to (senderRole == "entrenador")
        )
        db.collection("chat").document(conversationId).update(updates).await()
        Log.e(TAG, "[updateMessage] done convo=$conversationId messageId=$messageId")
    }

    suspend fun deleteMessage(conversationId: String, messageId: String, senderRole: String) {
        Log.e(TAG, "[deleteMessage] convo=$conversationId messageId=$messageId role=$senderRole")

        val updates = hashMapOf<String, Any>(
            "mensajes.$messageId" to com.google.firebase.firestore.FieldValue.delete(),
            "lecturaDeportista" to (senderRole == "deportista"),
            "lecturaEntrenador" to (senderRole == "entrenador")
        )
        db.collection("chat").document(conversationId).update(updates).await()
        Log.e(TAG, "[deleteMessage] done convo=$conversationId messageId=$messageId")
    }

    suspend fun markSeen(conversationId: String, role: String) {
        val field = if (role == "deportista") "lecturaDeportista" else "lecturaEntrenador"
        Log.e(TAG, "[markSeen] convo=$conversationId role=$role field=$field")
        db.collection("chat").document(conversationId).update(field, true).await()
        Log.e(TAG, "[markSeen] done convo=$conversationId")
    }

    suspend fun uploadImage(uid: String, uri: Uri): String {
        Log.e(TAG, "[uploadImage] uid=$uid uri=$uri")
        val ref = storage.reference.child("chat/images/$uid/${UUID.randomUUID()}.jpg")
        ref.putFile(uri).await()
        val url = ref.downloadUrl.await().toString()
        Log.e(TAG, "[uploadImage] done url=$url")
        return url
    }

    suspend fun uploadAudio(uid: String, uri: Uri): String {
        Log.e(TAG, "[uploadAudio] uid=$uid uri=$uri")
        val ref = storage.reference.child("chat/audio/$uid/${UUID.randomUUID()}.webm")
        ref.putFile(uri).await()
        val url = ref.downloadUrl.await().toString()
        Log.e(TAG, "[uploadAudio] done url=$url")
        return url
    }

    fun debugPing(from: String) {
        Log.e(TAG, "[debugPing] from=$from repo=${System.identityHashCode(this)}")
    }

    private fun isoToEpochSeconds(iso: String): Int {
        return try {
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val d = fmt.parse(iso) ?: return 0
            (d.time / 1000L).toInt()
        } catch (_: Exception) {
            0
        }
    }
}

