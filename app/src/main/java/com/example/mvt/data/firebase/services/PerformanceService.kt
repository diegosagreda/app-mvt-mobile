package com.example.mvt.data.firebase.services

import android.util.Log
import com.example.mvt.data.firebase.models.PerformanceRecord
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class PerformanceService {

    private val db = FirebaseDatabase.getInstance()
        .getReference("Semicooper")

    // === Guardar nuevo registro en regVAM ===
    suspend fun saveRecord(uid: String, record: PerformanceRecord) {
        try {
            val ref = db.child(uid).child("regVAM").push()
            val data = mapOf(
                "VAM"         to record.VAM,
                "VAM_decimal" to record.VAM_decimal,
                "VO2max"      to record.VO2max,
                "fecha"       to record.fecha,
                "min"         to record.min,
                "seg"         to record.seg,
                "semicooper"  to record.semicooper
            )
            ref.setValue(data).await()
            Log.d("PerformanceService", "Registro guardado: ${ref.key}")
        } catch (e: Exception) {
            Log.e("PerformanceService", "Error guardando registro", e)
            throw e
        }
    }

    // === Obtener todos los registros de regVAM ordenados por fecha ===
    suspend fun getRecords(uid: String): List<PerformanceRecord> {
        return try {
            val snapshot = db.child(uid).child("regVAM").get().await()
            if (!snapshot.exists()) return emptyList()

            val records = mutableListOf<PerformanceRecord>()
            snapshot.children.forEach { child ->
                val id          = child.key ?: return@forEach
                val VAM         = child.child("VAM").getValue(String::class.java) ?: ""
                val VAM_decimal = child.child("VAM_decimal").value.let {
                    when (it) {
                        is Double -> it
                        is Long   -> it.toDouble()
                        is String -> it.toDoubleOrNull() ?: 0.0
                        else      -> 0.0
                    }
                }
                val VO2max     = child.child("VO2max").getValue(String::class.java) ?: ""
                val fecha      = child.child("fecha").value.let {
                    when (it) {
                        is Long   -> it
                        is Double -> it.toLong()
                        else      -> 0L
                    }
                }
                val min        = child.child("min").value.let {
                    when (it) {
                        is Long   -> it.toInt()
                        is Double -> it.toInt()
                        else      -> 0
                    }
                }
                val seg        = child.child("seg").getValue(String::class.java) ?: "00"
                val semicooper = child.child("semicooper").getValue(String::class.java) ?: ""

                records.add(
                    PerformanceRecord(
                        id          = id,
                        VAM         = VAM,
                        VAM_decimal = VAM_decimal,
                        VO2max      = VO2max,
                        fecha       = fecha,
                        min         = min,
                        seg         = seg,
                        semicooper  = semicooper
                    )
                )
            }
            // Ordenar del más reciente al más antiguo
            records.sortedByDescending { it.fecha }
        } catch (e: Exception) {
            Log.e("PerformanceService", "Error obteniendo registros", e)
            emptyList()
        }
    }
}