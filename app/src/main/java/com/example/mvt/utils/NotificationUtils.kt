package com.example.mvt.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationUtils {
    private const val CHAT_MESSAGES_CHANNEL_ID = "chat-messages"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val routinesChannel = NotificationChannel(
                "routines_channel",
                "Rutinas Diarias",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para notificaciones de rutinas pendientes"
            }
            val chatChannel = NotificationChannel(
                CHAT_MESSAGES_CHANNEL_ID,
                "Mensajes del Entrenador",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para notificaciones de nuevos mensajes del entrenador"
                enableVibration(true)
            }

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannels(listOf(routinesChannel, chatChannel))
        }
    }
}
