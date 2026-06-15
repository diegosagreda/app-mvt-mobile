package com.example.mvt.data.firebase.models

import com.google.firebase.Timestamp
import java.io.Serializable

data class AppNotification(
    val id: String = "",
    val destinatario: String = "",
    val remitente: String = "",
    val tipo: String = "",
    val txt: String = "",
    val estado: String = "",
    val fecha: Timestamp? = null,
    val fechaStr: String = "",
    val rutina: String = "",
    val stravaActivityId: String = "",
    val fields: String = "",
    val senderName: String = "",
    val senderAvatarUrl: String = ""
) : Serializable {
    val isUnread: Boolean
        get() = estado.equals("no_leido", ignoreCase = true)

    val isMessageLike: Boolean
        get() = tipo.equals("msg", ignoreCase = true) || tipo.equals("chat", ignoreCase = true)

    val isStravaRoutine: Boolean
        get() = stravaActivityId.isNotBlank()

    val timestampMillis: Long?
        get() = fecha?.toDate()?.time
}

data class NotificationSenderPreview(
    val uid: String = "",
    val name: String = "",
    val avatarUrl: String = ""
)
