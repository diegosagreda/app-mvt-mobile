package com.example.mvt.chat.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.chat.data.model.ChatMessage
import com.example.mvt.chat.data.model.UiMessage
import com.example.mvt.chat.data.repo.ChatRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ChatUiState(
    val isLoading: Boolean = true,
    val conversationId: String = "",
    val uid: String = "",
    val otherUid: String = "",
    val role: String = "deportista",
    val messageText: String = "",
    val replyingTo: ChatMessage? = null,
    val editing: ChatMessage? = null,
    val audioUri: Uri? = null,
    val imageUri: Uri? = null,
    val messages: List<UiMessage> = emptyList()
)

class ChatViewModel(
    private val repo: ChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state

    private var listener: ListenerRegistration? = null

    fun init(uid: String, otherUid: String, role: String, conversationId: String?) {
        _state.value = _state.value.copy(uid = uid, otherUid = otherUid, role = role)

        if (!conversationId.isNullOrBlank()) {
            _state.value = _state.value.copy(conversationId = conversationId)
            listen(conversationId)
            viewModelScope.launch { repo.markSeen(conversationId, role) }
        } else {
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    private fun listen(conversationId: String) {
        listener?.remove()
        _state.value = _state.value.copy(isLoading = true)

        listener = repo.listenConversation(conversationId) { data ->
            val rawMensajes = (data?.get("mensajes") as? Map<*, *>) ?: emptyMap<Any, Any>()
            val list = rawMensajes.entries.mapNotNull { (k, v) ->
                val m = v as? Map<*, *> ?: return@mapNotNull null
                ChatMessage(
                    id = k.toString(),
                    texto = (m["texto"] ?: "").toString(),
                    audioUrl = (m["audioUrl"] ?: "").toString(),
                    imageUrl = (m["imageUrl"] ?: "").toString(),
                    remitente = (m["remitente"] ?: "").toString(),
                    timestamp = (m["timestamp"] ?: "").toString(),
                    rutina = m["rutina"]?.toString()
                )
            }.sortedBy { it.timestamp } // ISO lexicographic funciona si siempre es ISO completo

            _state.value = _state.value.copy(
                isLoading = false,
                messages = groupByDate(list, _state.value.uid)
            )
        }
    }

    fun onTextChange(s: String) {
        _state.value = _state.value.copy(messageText = s)
    }

    fun setReply(msg: ChatMessage?) {
        _state.value = _state.value.copy(replyingTo = msg, editing = null)
    }

    fun setEdit(msg: ChatMessage?) {
        _state.value = _state.value.copy(editing = msg, replyingTo = null, messageText = msg?.texto ?: "")
    }

    fun clearAttachments() {
        _state.value = _state.value.copy(audioUri = null, imageUri = null)
    }

    fun attachImage(uri: Uri?) {
        _state.value = _state.value.copy(imageUri = uri, audioUri = null)
    }

    fun attachAudio(uri: Uri?) {
        _state.value = _state.value.copy(audioUri = uri, imageUri = null)
    }

    fun sendMessage() {
        val st = _state.value
        val hasText = st.messageText.trim().isNotEmpty()
        val hasMedia = st.audioUri != null || st.imageUri != null
        if (!hasText && !hasMedia) return

        viewModelScope.launch {
            val nowIso = isoNow()
            var audioUrl = ""
            var imageUrl = ""

            st.audioUri?.let { audioUrl = repo.uploadAudio(st.uid, it) }
            st.imageUri?.let { imageUrl = repo.uploadImage(st.uid, it) }

            val msg = ChatMessage(
                texto = st.messageText.trim(),
                audioUrl = audioUrl,
                imageUrl = imageUrl,
                remitente = st.uid,
                timestamp = nowIso,
                rutina = st.replyingTo?.rutina
            )

            val convoId = if (st.conversationId.isBlank()) {
                val newId = repo.createConversation(listOf(st.uid, st.otherUid), msg)
                _state.value = _state.value.copy(conversationId = newId)
                listen(newId)
                newId
            } else st.conversationId

            // editar vs nuevo
            val editingId = st.editing?.id
            if (!editingId.isNullOrBlank()) {
                repo.updateMessage(convoId, editingId, st.messageText.trim(), st.role)
            } else {
                repo.addMessage(convoId, msg, st.role)
            }

            _state.value = _state.value.copy(
                messageText = "",
                replyingTo = null,
                editing = null,
                audioUri = null,
                imageUri = null
            )
        }
    }

    fun deleteMessage(messageId: String) {
        val st = _state.value
        if (st.conversationId.isBlank()) return
        viewModelScope.launch {
            repo.deleteMessage(st.conversationId, messageId, st.role)
        }
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }

    private fun groupByDate(list: List<ChatMessage>, myUid: String): List<UiMessage> {
        val today0 = Date().apply { hours = 0; minutes = 0; seconds = 0 }
        val todayMs = today0.time
        val yesterdayMs = todayMs - 86_400_000L

        fun dayStartMs(d: Date): Long {
            val dd = Date(d.time)
            dd.hours = 0; dd.minutes = 0; dd.seconds = 0
            return dd.time
        }

        fun label(tsIso: String): String {
            val d = parseIso(tsIso) ?: return "—"
            val ds = dayStartMs(d)
            return when (ds) {
                todayMs -> "Hoy"
                yesterdayMs -> "Ayer"
                else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(d)
            }
        }

        var lastGroup = ""
        return list.map { m ->
            val g = label(m.timestamp)
            val show = g != lastGroup
            lastGroup = g
            UiMessage(m, g, show, m.remitente == myUid)
        }
    }

    private fun isoNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }

    private fun parseIso(s: String): Date? {
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            fmt.parse(s)
        } catch (_: Exception) {
            null
        }
    }
}