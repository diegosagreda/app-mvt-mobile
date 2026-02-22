package com.example.mvt.chat.data.model

data class ChatMessage(
    val id: String = "",
    val texto: String = "",
    val audioUrl: String = "",
    val imageUrl: String = "",
    val remitente: String = "",
    val timestamp: String = "",   // ISO string (igual que web)
    val rutina: String? = null
)

data class Conversation(
    val id: String = "",
    val participantes: List<String> = listOf("", ""),
    val lecturaDeportista: Boolean = false,
    val lecturaEntrenador: Boolean = false,
    val mensajes: Map<String, ChatMessage> = emptyMap()
)

data class UiMessage(
    val msg: ChatMessage,
    val dateGroup: String,
    val showDateHeader: Boolean,
    val isMine: Boolean
)