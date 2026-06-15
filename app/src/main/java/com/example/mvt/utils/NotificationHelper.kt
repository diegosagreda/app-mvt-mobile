package com.example.mvt.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.mvt.MainActivity
import com.example.mvt.R

object NotificationHelper {
    private const val CHAT_MESSAGES_CHANNEL_ID = "chat-messages"

    fun showPendingRoutineNotification(context: Context, count: Int) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, "routines_channel")
            .setSmallIcon(R.drawable.mvt)
            .setContentTitle("Rutinas pendientes")
            .setContentText("Tienes $count rutinas pendientes para hoy")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1001, notification)
    }

    fun showNewChatMessageNotification(
        context: Context,
        trainerName: String,
        preview: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ChatNotificationBus.EXTRA_OPEN_CHAT, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2001,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val safeTrainerName = trainerName.ifBlank { "Tu entrenador" }
        val safePreview = preview.ifBlank { "Tienes un mensaje nuevo de tu entrenador." }
        val notification = NotificationCompat.Builder(context, CHAT_MESSAGES_CHANNEL_ID)
            .setSmallIcon(R.drawable.mvt)
            .setContentTitle("Nuevo mensaje de $safeTrainerName")
            .setContentText(safePreview)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(safePreview)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(2001, notification)
    }
}
