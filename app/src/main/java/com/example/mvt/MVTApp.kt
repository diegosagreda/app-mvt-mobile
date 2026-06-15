package com.example.mvt

import android.app.Application
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.example.mvt.utils.AppForegroundMonitor
import com.example.mvt.utils.NotificationUtils
import com.example.mvt.utils.PushTokenManager

class MVTApp : Application(), Configuration.Provider {
    override fun onCreate() {
        super.onCreate()
        NotificationUtils.createNotificationChannel(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppForegroundMonitor)
        PushTokenManager.start(this)
    }

    override val workManagerConfiguration: Configuration
        get() {
            Log.d("MVTApp", "Inicializando configuración de WorkManager")
            return Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .build()
        }
}
