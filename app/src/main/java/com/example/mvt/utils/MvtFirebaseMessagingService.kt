package com.example.mvt.utils

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MvtFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PushTokenManager.onNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        if (data.isEmpty()) return

        val isChatMessage =
            data["event"] == "chat_message" ||
                data["type"] == "chat" ||
                data["routeName"] == "deportista-chat"

        if (!isChatMessage || AppForegroundMonitor.isForeground.value) return

        val trainerName = data["remitenteNombre"]
            ?: data["senderName"]
            ?: remoteMessage.notification?.title
                ?.removePrefix("Nuevo mensaje de ")
                ?.trim()
                .orEmpty()

        val preview = data["txt"]
            ?: data["preview"]
            ?: remoteMessage.notification?.body
                .orEmpty()

        NotificationHelper.showNewChatMessageNotification(
            context = applicationContext,
            trainerName = trainerName.ifBlank { "Tu entrenador" },
            preview = preview.ifBlank { "Tienes un mensaje nuevo de tu entrenador." }
        )
    }
}
