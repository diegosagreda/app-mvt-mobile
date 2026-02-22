package com.example.mvt.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.mvt.MainActivity
import com.example.mvt.R

object NotificationHelper {
    fun showPendingRoutineNotification(context: Context, count: Int) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
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
}
