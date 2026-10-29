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
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone


data class ChatUiState(
    val isLoading: Boolean = true,
    val conversationId: String = "",
    val uid: String = "",
    val otherUid: String = "",
    val role: String = "deportista",
    val isSendingMessage: Boolean = false,
    val isUploadingImage: Boolean = false,
    val messageText: String = "",
    val replyingTo: ChatMessage? = null,
    val replyTo: ChatMessage? = null, // alias opcional
    val editing: ChatMessage? = null,
    val audioUri: Uri? = null,
    val imageUri: Uri? = null,
    val messages: List<UiMessage> = emptyList(),
    val totalMessages: Int = 0,
    val recipientHasSeen: Boolean = false
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

        listener = repo.listenConversation(conversationId) { snapshot ->
            val myUid = _state.value.uid

            // Ordena robusto por ISO (y si llega algún legacy, cae a 0)
            val sorted = snapshot.messages.sortedBy { isoToMillisSafe(it.timestamp) }

            val recipientHasSeen = if (_state.value.role == "deportista") {
                snapshot.trainerHasSeen
            } else {
                snapshot.athleteHasSeen
            }
            val recipientLastSeenAt = if (_state.value.role == "deportista") {
                snapshot.lastTrainerSeenAt
            } else {
                snapshot.lastAthleteSeenAt
            }
            val recipientLastSeenMillis = isoToMillisSafe(recipientLastSeenAt)
            val uiList = groupByDate(
                list = sorted,
                myUid = myUid,
                recipientHasSeen = recipientHasSeen,
                recipientLastSeenMillis = recipientLastSeenMillis
            )

            _state.update {
                it.copy(
                    isLoading = false,
                    messages = uiList,
                    totalMessages = sorted.size,
                    recipientHasSeen = recipientHasSeen
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
        _state.update { it.copy(imageUri = uri, audioUri = null, editing = null) }
    }

    fun attachAudio(uri: Uri?) {
        _state.update { it.copy(audioUri = uri, imageUri = null, editing = null) }
    }

    fun sendTextMessage(text: String, replyToMessageId: String? = null) {
        _state.update { it.copy(messageText = text) }
        sendMessage()
    }

    fun sendMessage() {
        val st = _state.value
        val hasText = st.messageText.trim().isNotEmpty()
        val hasMedia = st.audioUri != null || st.imageUri != null
        if ((!hasText && !hasMedia) || st.isSendingMessage) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSendingMessage = true,
                    isUploadingImage = st.imageUri != null
                )
            }

            // ✅ MISMO FORMATO QUE WEB
            val nowIso = isoNow()

            var audioUrl = ""
            var imageUrl = ""
            var uploadFailed = false

            runCatching {
                st.audioUri?.let { audioUrl = repo.uploadAudio(st.uid, it) }
                st.imageUri?.let { imageUrl = repo.uploadImage(st.uid, it) }
            }.onFailure {
                uploadFailed = true
                Log.e(TAG, "upload media failed: ${it.message}", it)
            }

            if (st.imageUri != null && imageUrl.isBlank()) {
                uploadFailed = true
            }

            if (uploadFailed) {
                _state.update { it.copy(isSendingMessage = false, isUploadingImage = false) }
                return@launch
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
                    repo.createChatNotification(
                        remitente = st.uid,
                        destinatario = st.otherUid,
                        txt = msg.texto.ifBlank {
                            when {
                                msg.imageUrl.isNotBlank() -> "\uD83D\uDDBC\uFE0F Imagen"
                                msg.audioUrl.isNotBlank() -> "\uD83C\uDFA4 Audio"
                                else -> "Nuevo mensaje"
                            }
                        },
                        tipo = if (msg.rutina.isNullOrBlank()) "chat" else "msg",
                        rutina = msg.rutina.orEmpty()
                    )
                }
            }.onFailure {
                Log.e(TAG, "send/update failed: ${it.message}", it)
                _state.update { it.copy(isSendingMessage = false, isUploadingImage = false) }
                return@launch
            }

            _state.update {
                it.copy(
                    isSendingMessage = false,
                    isUploadingImage = false,
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

    fun toggleReaction(messageId: String, emoji: String) {
        val st = _state.value
        if (st.conversationId.isBlank() || st.uid.isBlank()) return

        viewModelScope.launch {
            runCatching {
                repo.toggleReaction(
                    conversationId = st.conversationId,
                    messageId = messageId,
                    userId = st.uid,
                    emoji = emoji,
                    senderRole = st.role
                )
            }.onFailure { Log.e(TAG, "toggleReaction failed: ${it.message}", it) }
        }
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }

    // =========================
    // Helpers UI (headers fecha)
    // =========================
    private fun groupByDate(
        list: List<ChatMessage>,
        myUid: String,
        recipientHasSeen: Boolean = false,
        recipientLastSeenMillis: Long = 0L
    ): List<UiMessage> {

        fun label(tsIso: String): String {
            val ms = isoToMillisSafe(tsIso)
            val d = Date(ms)

            val messageCalendar = Calendar.getInstance().apply { time = d }
            val today = Calendar.getInstance()
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

            return when {
                isSameCalendarDay(messageCalendar, today) -> "HOY"
                isSameCalendarDay(messageCalendar, yesterday) -> "AYER"
                else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(d)
            }
        }

        var lastGroup = ""
        return list.map { m ->
            val g = label(m.timestamp) // ✅ ISO
            val show = g != lastGroup
            lastGroup = g
            val isMine = m.remitente == myUid
            val isReadByRecipient = if (!isMine) {
                false
            } else if (recipientLastSeenMillis > 0L) {
                isoToMillisSafe(m.timestamp) <= recipientLastSeenMillis
            } else {
                recipientHasSeen
            }

            UiMessage(
                m,
                g,
                show,
                isMine,
                isReadByRecipient = isReadByRecipient
            )
        }
    }

    private fun isoNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }

    private fun isoToMillisSafe(iso: String): Long {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        for (pattern in formats) {
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(iso)?.time
            }.getOrNull()?.let { return it }
        }
        return 0L
    }

    private fun isSameCalendarDay(first: Calendar, second: Calendar): Boolean {
        return first.get(Calendar.ERA) == second.get(Calendar.ERA) &&
            first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
            first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
    }
}
