package com.example.mvt.chat.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.chat.data.model.ChatMessage
import com.example.mvt.chat.data.model.UiMessage
import com.example.mvt.chat.data.repo.ChatRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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
    val replyTo: ChatMessage? = null, // alias opcional
    val editing: ChatMessage? = null,
    val audioUri: Uri? = null,
    val imageUri: Uri? = null,
    val messages: List<UiMessage> = emptyList(),
    val totalMessages: Int = 0
)

class ChatViewModel(
    private val repo: ChatRepository
) : ViewModel() {

    private val TAG = "ChatVM"

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state

    private var listener: ListenerRegistration? = null

    fun init(uid: String, otherUid: String, role: String, conversationId: String?) {
        _state.update { it.copy(uid = uid, otherUid = otherUid, role = role, isLoading = true) }

        viewModelScope.launch {
            val cid = when {
                !conversationId.isNullOrBlank() -> conversationId
                else -> repo.findConversationId(uid, otherUid)
            }

            if (cid.isNullOrBlank()) {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }

            _state.update { it.copy(conversationId = cid, isLoading = true) }

            listen(cid)

            runCatching { repo.markSeen(cid, role) }
                .onFailure { Log.e(TAG, "markSeen failed: ${it.message}", it) }
        }
    }

    private fun listen(conversationId: String) {
        listener?.remove()
        _state.update { it.copy(isLoading = true) }

        listener = repo.listenConversation(conversationId) { list ->
            val myUid = _state.value.uid

            // Ordena robusto por ISO (y si llega algún legacy, cae a 0)
            val sorted = list.sortedBy { isoToMillisSafe(it.timestamp) }

            val uiList = groupByDate(sorted, myUid)

            _state.update {
                it.copy(
                    isLoading = false,
                    messages = uiList,
                    totalMessages = sorted.size
                )
            }
        }
    }

    fun onTextChange(s: String) {
        _state.update { it.copy(messageText = s) }
    }

    fun setReply(msg: ChatMessage?) {
        _state.update { it.copy(replyingTo = msg, replyTo = msg, editing = null) }
    }

    fun clearReply() {
        _state.update { it.copy(replyingTo = null, replyTo = null) }
    }

    fun setEdit(msg: ChatMessage?) {
        _state.update {
            it.copy(
                editing = msg,
                replyingTo = null,
                replyTo = null,
                messageText = msg?.texto.orEmpty()
            )
        }
    }

    fun clearAttachments() {
        _state.update { it.copy(audioUri = null, imageUri = null) }
    }

    fun attachImage(uri: Uri?) {
        _state.update { it.copy(imageUri = uri, audioUri = null) }
    }

    fun attachAudio(uri: Uri?) {
        _state.update { it.copy(audioUri = uri, imageUri = null) }
    }

    fun sendTextMessage(text: String, replyToMessageId: String? = null) {
        _state.update { it.copy(messageText = text) }
        sendMessage()
    }

    fun sendMessage() {
        val st = _state.value
        val hasText = st.messageText.trim().isNotEmpty()
        val hasMedia = st.audioUri != null || st.imageUri != null
        if (!hasText && !hasMedia) return

        viewModelScope.launch {
            // ✅ MISMO FORMATO QUE WEB
            val nowIso = isoNow()

            var audioUrl = ""
            var imageUrl = ""

            runCatching {
                st.audioUri?.let { audioUrl = repo.uploadAudio(st.uid, it) }
                st.imageUri?.let { imageUrl = repo.uploadImage(st.uid, it) }
            }.onFailure {
                Log.e(TAG, "upload media failed: ${it.message}", it)
            }

            val msg = ChatMessage(
                texto = st.messageText.trim(),
                audioUrl = audioUrl,
                imageUrl = imageUrl,
                remitente = st.uid,
                timestamp = nowIso,          // ✅ ISO STRING
                rutina = st.replyingTo?.rutina
            )

            val convoId = if (st.conversationId.isBlank()) {
                val existing = repo.findConversationId(st.uid, st.otherUid)
                val newId = if (!existing.isNullOrBlank()) existing
                else repo.createConversation(listOf(st.uid, st.otherUid), msg)

                _state.update { it.copy(conversationId = newId) }
                listen(newId)
                newId
            } else st.conversationId

            val editingId = st.editing?.id
            runCatching {
                if (!editingId.isNullOrBlank()) {
                    repo.updateMessage(convoId, editingId, st.messageText.trim(), st.role)
                } else {
                    repo.addMessage(convoId, msg, st.role)
                }
            }.onFailure {
                Log.e(TAG, "send/update failed: ${it.message}", it)
            }

            _state.update {
                it.copy(
                    messageText = "",
                    replyingTo = null,
                    replyTo = null,
                    editing = null,
                    audioUri = null,
                    imageUri = null
                )
            }
        }
    }

    fun deleteMessage(messageId: String) {
        val st = _state.value
        if (st.conversationId.isBlank()) return

        viewModelScope.launch {
            runCatching { repo.deleteMessage(st.conversationId, messageId, st.role) }
                .onFailure { Log.e(TAG, "delete failed: ${it.message}", it) }
        }
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }

    // =========================
    // Helpers UI (headers fecha)
    // =========================
    private fun groupByDate(list: List<ChatMessage>, myUid: String): List<UiMessage> {

        fun label(tsIso: String): String {
            val ms = isoToMillisSafe(tsIso)
            val d = Date(ms)

            val todayStart = Date().apply { hours = 0; minutes = 0; seconds = 0 }.time
            val yesterdayStart = todayStart - 86_400_000L
            val dayStart = Date(d.time).apply { hours = 0; minutes = 0; seconds = 0 }.time

            return when (dayStart) {
                todayStart -> "Hoy"
                yesterdayStart -> "Ayer"
                else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(d)
            }
        }

        var lastGroup = ""
        return list.map { m ->
            val g = label(m.timestamp) // ✅ ISO
            val show = g != lastGroup
            lastGroup = g

            UiMessage(
                m,
                g,
                show,
                m.remitente == myUid
            )
        }
    }

    private fun isoNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }

    private fun isoToMillisSafe(iso: String): Long {
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            (fmt.parse(iso)?.time) ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
}