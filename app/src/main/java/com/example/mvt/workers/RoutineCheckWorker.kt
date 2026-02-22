package com.example.mvt.workers

import android.content.Context
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mvt.data.firebase.repositories.RoutineRepository
import com.example.mvt.data.firebase.services.FirestoreService
import com.example.mvt.utils.NotificationHelper
import com.example.mvt.utils.WorkScheduler
import java.util.*

class RoutineCheckWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    private val TAG = "RoutineCheckWorker"

    @RequiresApi(26)
    override suspend fun doWork(): Result {
        val athleteId = inputData.getString("athleteId") ?: return Result.failure()

        Log.d(TAG, "Iniciando worker de rutina diaria para atleta $athleteId")

        return try {
            val repo = RoutineRepository(FirestoreService())
            val routines = repo.getRoutinesByAthlete(athleteId)
            Log.d(TAG, "Se obtuvieron ${routines.size} rutinas desde Firestore")

            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val pending = routines.filter {
                val date = it.fecha?.toDate()
                isSameDay(date, today) && it.estado.equals("Pendiente", ignoreCase = true)
            }

            Log.d(TAG, "Rutinas pendientes encontradas: ${pending.size}")

            if (pending.isNotEmpty()) {
                NotificationHelper.showPendingRoutineNotification(applicationContext, pending.size)
                Log.d(TAG, "Notificación enviada con éxito.")
            } else {
                Log.d(TAG, "No hay rutinas pendientes para hoy. No se envió notificación.")
            }

            // Reprogramar el siguiente disparo
            WorkScheduler.scheduleDailyRoutineChecks(applicationContext, athleteId)
            Log.d(TAG, "Worker reprogramado para el día siguiente.")

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error al ejecutar worker: ${e.message}", e)
            Result.failure()
        }
    }

    private fun isSameDay(date1: Date?, date2: Date?): Boolean {
        if (date1 == null || date2 == null) return false
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
