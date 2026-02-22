package com.example.mvt.chat.data.repo

import android.net.Uri
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

    fun listenConversation(conversationId: String, onChange: (Map<String, Any>?) -> Unit): ListenerRegistration {
        return db.collection("chat")
            .document(conversationId)
            .addSnapshotListener { snap, _ ->
                onChange(snap?.data)
            }
    }

    suspend fun createConversation(participantes: List<String>, firstMessage: ChatMessage): String {
        val docRef = db.collection("chat").document()
        val messageId = "mensajeID${System.currentTimeMillis()}"
        val payload = hashMapOf(
            "participantes" to participantes,
            "lecturaDeportista" to true,
            "lecturaEntrenador" to false,
            "mensajes" to mapOf(messageId to firstMessage)
        )
        docRef.set(payload).await()
        return docRef.id
    }

    suspend fun addMessage(conversationId: String, message: ChatMessage, senderRole: String) {
        val messageId = "mensajeID${System.currentTimeMillis()}"
        val updates = hashMapOf<String, Any>(
            "mensajes.$messageId" to message,
            "lecturaDeportista" to (senderRole == "deportista"),
            "lecturaEntrenador" to (senderRole == "entrenador")
        )
        db.collection("chat").document(conversationId).update(updates).await()
    }

    suspend fun updateMessage(conversationId: String, messageId: String, newText: String, senderRole: String) {
        val updates = hashMapOf<String, Any>(
            "mensajes.$messageId.texto" to newText,
            "lecturaDeportista" to (senderRole == "deportista"),
            "lecturaEntrenador" to (senderRole == "entrenador")
        )
        db.collection("chat").document(conversationId).update(updates).await()
    }

    suspend fun deleteMessage(conversationId: String, messageId: String, senderRole: String) {
        val updates = hashMapOf<String, Any>(
            "mensajes.$messageId" to com.google.firebase.firestore.FieldValue.delete(),
            "lecturaDeportista" to (senderRole == "deportista"),
            "lecturaEntrenador" to (senderRole == "entrenador")
        )
        db.collection("chat").document(conversationId).update(updates).await()
    }

    suspend fun markSeen(conversationId: String, role: String) {
        val field = if (role == "deportista") "lecturaDeportista" else "lecturaEntrenador"
        db.collection("chat").document(conversationId).update(field, true).await()
    }

    suspend fun uploadImage(uid: String, uri: Uri): String {
        val ref = storage.reference.child("chat/images/$uid/${UUID.randomUUID()}.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun uploadAudio(uid: String, uri: Uri): String {
        val ref = storage.reference.child("chat/audio/$uid/${UUID.randomUUID()}.webm")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
}