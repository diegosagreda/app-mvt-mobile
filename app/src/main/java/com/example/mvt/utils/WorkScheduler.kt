package com.example.mvt.utils

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.mvt.workers.RoutineCheckWorker
import java.util.*
import java.util.concurrent.TimeUnit

object WorkScheduler {

    private const val TAG = "WorkScheduler"

    // === PROGRAMAR LOS 3 HORARIOS DIARIOS ===
    fun scheduleDailyRoutineChecks(context: Context, athleteId: String) {
        scheduleRoutineCheckAt(context, athleteId, 6, 0, "morningRoutineCheck")   // 6:00 a. m.
        scheduleRoutineCheckAt(context, athleteId, 12, 0, "noonRoutineCheck")     // 12:00 p. m.
        scheduleRoutineCheckAt(context, athleteId, 18, 0, "eveningRoutineCheck")  // 6:00 p. m.
    }

    // === FUNCIÓN GENERAL PARA CADA HORARIO ===
    private fun scheduleRoutineCheckAt(
        context: Context,
        athleteId: String,
        hour: Int,
        minute: Int,
        workName: String
    ) {
        val now = Calendar.getInstance()
        val nextRun = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }

        val delay = nextRun.timeInMillis - now.timeInMillis
        Log.d(TAG, "Programando $workName para ${nextRun.time} (en ${delay / 1000 / 60} min)")

        val data = workDataOf("athleteId" to athleteId)
        val request = OneTimeWorkRequestBuilder<RoutineCheckWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.KEEP, // evita duplicados
            request
        )
        Log.d(TAG, "$workName encolado correctamente con ID: ${request.id}")
    }
}
