package com.example.mvt.chat.data.model

data class ChatMessage(
    val id: String = "",
    val texto: String = "",
    val audioUrl: String = "",
    val imageUrl: String = "",
    val remitente: String = "",
    val timestamp: String = "",
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
    val groupLabel: String,
    val showGroupHeader: Boolean,
    val isMine: Boolean
)

data class TrainerPersonalData(
    val identificacion: String = "",
    val nombres: String = "",
    val apellidos: String = "",
    val genero: String = "",
    val email: String = "",
    val fecha_nacimiento: Any? = null,
    val pais: String = "",
    val ciudad: String = "",
    val telefono: String = "",
    val direccion: String = "",
    val formularioBienvenida: Any? = null,
    val foto_url: String = ""
)