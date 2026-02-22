package com.example.mvt

import android.app.Application
import android.util.Log
import androidx.work.Configuration

class MVTApp : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() {
            Log.d("MVTApp", "Inicializando configuración de WorkManager")
            return Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .build()
        }
}
