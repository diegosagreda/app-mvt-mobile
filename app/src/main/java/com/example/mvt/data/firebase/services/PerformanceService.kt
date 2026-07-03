package com.example.mvt.data.firebase.services

import android.util.Log
import com.example.mvt.data.firebase.models.DistanceOption
import com.example.mvt.data.firebase.models.PerformanceRecord
import com.example.mvt.data.firebase.models.PersonalRecord
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import kotlin.collections.emptyList
import kotlin.collections.sortedByDescending

class PerformanceService {

    private val db = FirebaseDatabase.getInstance()
        .getReference("Semicooper")

    // === Guardar nuevo registro ===
    // === Guardar nuevo registro de test ===
    suspend fun saveRecord(uid: String, record: PerformanceRecord) {
        try {
            val ref = db.child(uid).child("regVAM").push()
            val data = mapOf(
                "VAM"         to record.VAM,
                "VAM_decimal" to record.VAM_decimal,
                "VO2Max"      to record.VO2Max,
                "fecha"       to record.fecha,
                "min"         to record.min,
                "seg"         to record.seg,
                "semicooper"  to record.semicooper
            )
            ref.setValue(data).await()
            Log.d("PerformanceService", "Registro test guardado: ${ref.key}")
        } catch (e: Exception) {
            Log.e("PerformanceService", "Error guardando test", e)
            throw e
        }
    }

    // === Obtener todos los registros ordenados por fecha ===
    // === Obtener registros de regVAM ordenados por fecha ===
    suspend fun getRecords(uid: String): List<PerformanceRecord> {
        return try {
            val snapshot = db.child(uid).child("regVAM").get().await()
            if (!snapshot.exists()) return emptyList()

            val records = mutableListOf<PerformanceRecord>()

            snapshot.children.forEach { child ->
                val id = child.key ?: return@forEach

                val VAM = child.child("VAM")
                    .getValue(String::class.java) ?: ""

                val VAM_decimal = child.child("VAM_decimal").value.let {
                    when (it) {
                        is Double -> it
                        is Long   -> it.toDouble()
                        is String -> it.toDoubleOrNull() ?: 0.0
                        else      -> 0.0
                    }
                }

                val VO2max = child.child("VO2Max")
                    .getValue(String::class.java)
                    ?: child.child("VO2max").getValue(String::class.java)
                    ?: ""

                val fecha = child.child("fecha").value.let {
                    when (it) {
                        is Long   -> it
                        is Double -> it.toLong()
                        else      -> 0L
                    }
                }

                val min = child.child("min").value.let {
                    when (it) {
                        is Long   -> it.toInt()
                        is Double -> it.toInt()
                        else      -> 0
                    }
                }

                val seg = child.child("seg").value.let {
                    when (it) {
                        is String -> it
                        is Long   -> String.format(java.util.Locale.US, "%02d", it)
                        is Double -> String.format(java.util.Locale.US, "%02d", it.toInt())
                        else      -> "00"
                    }
                }

                val semicooper = child.child("semicooper").value.let {
                    when (it) {
                        is String -> it
                        is Long   -> it.toString()
                        is Double -> it.toInt().toString()
                        else      -> ""
                    }
                }

                if (fecha > 0L) {
                    records.add(
                        PerformanceRecord(
                            id          = id,
                            VAM         = VAM,
                            VAM_decimal = VAM_decimal,
                            VO2Max      = VO2max,
                            fecha       = fecha,
                            min         = min,
                            seg         = seg,
                            semicooper  = semicooper
                        )
                    )
                }
            }

            records.sortedByDescending { it.fecha }

        } catch (e: Exception) {
            Log.e("PerformanceService", "Error obteniendo registros", e)
            emptyList()
        }
    }
    // === Obtener marcas desde actVAM/{uid}/marcas ===
// === Obtener marcas de actVAM ===
    suspend fun getPersonalRecords(uid: String): List<PersonalRecord> {
        return try {
            val snapshot = db
                .child(uid)
                .child("actVAM")
                .child(uid)
                .child("marcas")
                .get()
                .await()

            if (!snapshot.exists()) return emptyList()

            val records = mutableListOf<PersonalRecord>()

            snapshot.children.forEachIndexed { index, child ->
                val map = child.value as? Map<String, Any?> ?: return@forEachIndexed

                records.add(
                    PersonalRecord(
                        index     = index,
                        distancia = map["distancia"]?.toString(),
                        fcProm    = map["fcProm"]?.toString(),
                        fecha     = map["fecha"]?.toString(),
                        ritmo     = map["ritmo"]?.toString(),
                        tiempoH   = map["tiempoH"],
                        tiempoM   = map["tiempoM"],
                        tiempoS   = map["tiempoS"]
                    )
                )
            }

            records.sortedByDescending { it.index }

        } catch (e: Exception) {
            Log.e("PerformanceService", "Error obteniendo marcas", e)
            emptyList()
        }
    }

    // === Eliminar marca por índice ===
    suspend fun deletePersonalRecord(uid: String, index: Int) {
        try {
            val marcasRef = db.child(uid)
                .child("actVAM")
                .child(uid)
                .child("marcas")

            // Obtener todas las marcas actuales
            val snapshot = marcasRef.get().await()
            val allMarcas = mutableListOf<Map<String, Any?>>()

            snapshot.children.forEach { child ->
                val map = child.value as? Map<String, Any?> ?: return@forEach
                allMarcas.add(map)
            }

            // Eliminar la marca del índice dado
            if (index < allMarcas.size) {
                allMarcas.removeAt(index)
            }

            // Reescribir todas las marcas reindexadas
            // Esto corrige el bug de índices corruptos
            marcasRef.removeValue().await()

            allMarcas.forEachIndexed { newIndex, marca ->
                marcasRef.child(newIndex.toString()).setValue(marca).await()
            }

            Log.d("PerformanceService", "Marca $index eliminada y reindexada")
        } catch (e: Exception) {
            Log.e("PerformanceService", "Error eliminando marca", e)
            throw e
        }
    }

    // === Obtener opciones de distancia desde Firebase ===
    suspend fun getDistanceOptions(): List<DistanceOption> {
        return try {
            val snapshot = FirebaseDatabase
                .getInstance()
                .getReference("Distancias")
                .get()
                .await()

            if (!snapshot.exists()) return emptyList()

            val distances = mutableListOf<DistanceOption>()
            snapshot.children.forEach { child ->
                distances.add(
                    DistanceOption(
                        name  = child.key ?: "",
                        value = child.value.toString()
                    )
                )
            }
            distances

        } catch (e: Exception) {
            Log.e("PerformanceService", "Error obteniendo distancias", e)
            emptyList()
        }
    }

    // === Guardar marca en actVAM ===
    suspend fun savePersonalRecord(uid: String, index: Int, data: Map<String, String>) {
        try {
            db.child(uid)
                .child("actVAM")
                .child(uid)
                .child("marcas")
                .child(index.toString())
                .setValue(data)
                .await()
            Log.d("PerformanceService", "Marca guardada en índice $index")
        } catch (e: Exception) {
            Log.e("PerformanceService", "Error guardando marca", e)
            throw e
        }
    }


    // === Actualizar campos raíz de actVAM con el test actual ===
    suspend fun updateActVamTestData(uid: String, record: PerformanceRecord) {
        try {
            val data = mapOf(
                "VAM"         to record.VAM,
                "VAM_decimal" to record.VAM_decimal,
                "VO2Max"      to record.VO2Max,
                "fecha"       to record.fecha,
                "min"         to record.min,
                "seg"         to record.seg,
                "semicooper"  to record.semicooper
            )
            db.child(uid)
                .child("actVAM")
                .child(uid)
                .updateChildren(data)
                .await()
            Log.d("PerformanceService", "actVAM actualizado con test actual")
        } catch (e: Exception) {
            Log.e("PerformanceService", "Error actualizando actVAM", e)
            throw e
        }
    }


    // === Obtener marcas actuales de actVAM para copiarlas en regVAM ===
    private suspend fun getMarcasActuales(uid: String): Map<String, Any> {
        return try {
            val snapshot = db
                .child(uid)
                .child("actVAM")
                .child(uid)
                .child("marcas")
                .get()
                .await()

            if (!snapshot.exists()) return emptyMap()

            val marcas = mutableMapOf<String, Any>()
            snapshot.children.forEach { child ->
                val key = child.key ?: return@forEach
                val value = child.value ?: return@forEach
                marcas[key] = value
            }
            marcas
        } catch (e: Exception) {
            Log.e("PerformanceService", "Error obteniendo marcas actuales", e)
            emptyMap()
        }
    }

    // === Guardar marcas dentro de un registro específico de regVAM ===
    suspend fun saveMarcasEnRegVAM(uid: String, regVamKey: String) {
        try {
            val marcasActuales = getMarcasActuales(uid)
            if (marcasActuales.isEmpty()) {
                Log.d("PerformanceService", "No hay marcas para guardar en regVAM")
                return
            }
            db.child(uid)
                .child("regVAM")
                .child(regVamKey)
                .child("marcas")
                .setValue(marcasActuales)
                .await()
            Log.d("PerformanceService", "Marcas guardadas en regVAM/$regVamKey")
        } catch (e: Exception) {
            Log.e("PerformanceService", "Error guardando marcas en regVAM", e)
            throw e
        }
    }

    // === Crear nuevo registro en regVAM al eliminar una marca ===
// Copia el último test con fecha actual y las marcas que quedan
    suspend fun crearRegistroRegVamAlEliminar(uid: String) {
        try {
            // Obtener el último registro de regVAM
            val snapshot = db.child(uid).child("regVAM").get().await()
            if (!snapshot.exists()) return

            // Buscar el registro más reciente por fecha
            var latestRecord: com.google.firebase.database.DataSnapshot? = null
            var latestFecha = 0L

            snapshot.children.forEach { child ->
                val fecha = child.child("fecha").value.let {
                    when (it) {
                        is Long   -> it
                        is Double -> it.toLong()
                        else      -> 0L
                    }
                }
                if (fecha > latestFecha) {
                    latestFecha    = fecha
                    latestRecord   = child
                }
            }

            if (latestRecord == null) return

            // Obtener datos del último test
            val vam        = latestRecord!!.child("VAM").getValue(String::class.java) ?: ""
            val vamDecimal = latestRecord!!.child("VAM_decimal").value.let {
                when (it) {
                    is Double -> it
                    is Long   -> it.toDouble()
                    else      -> 0.0
                }
            }
            val vo2Max     = latestRecord!!.child("VO2Max").getValue(String::class.java) ?: ""
            val min        = latestRecord!!.child("min").value.let {
                when (it) {
                    is Long   -> it.toInt()
                    is Double -> it.toInt()
                    else      -> 0
                }
            }
            val seg = latestRecord!!.child("seg").value.let {
                when (it) {
                    is String -> it
                    is Long   -> String.format(java.util.Locale.US, "%02d", it)
                    is Double -> String.format(java.util.Locale.US, "%02d", it.toInt())
                    else      -> "00"
                }
            }
            val semicooper = latestRecord!!.child("semicooper").value.let {
                when (it) {
                    is String -> it
                    is Long   -> it.toString()
                    is Double -> it.toInt().toString()
                    else      -> ""
                }
            }

            // Crear nuevo registro con fecha actual
            val nuevoRef = db.child(uid).child("regVAM").push()
            val nuevoRegistro = mapOf(
                "VAM"         to vam,
                "VAM_decimal" to vamDecimal,
                "VO2Max"      to vo2Max,
                "fecha"       to System.currentTimeMillis(), // ← fecha actual
                "min"         to min,
                "seg"         to seg,
                "semicooper"  to semicooper
            )
            nuevoRef.setValue(nuevoRegistro).await()

            // Copiar las marcas que quedan después de la eliminación
            val marcasActuales = getMarcasActuales(uid)
            if (marcasActuales.isNotEmpty()) {
                nuevoRef.child("marcas").setValue(marcasActuales).await()
            }

            Log.d("PerformanceService", "Nuevo regVAM creado tras eliminar marca: ${nuevoRef.key}")

        } catch (e: Exception) {
            Log.e("PerformanceService", "Error creando regVAM al eliminar marca", e)
            throw e
        }
    }

    // === Guardar snapshot en regVAM con marcas inmutables ===
    suspend fun saveRecordWithMarcas(
        uid: String,
        record: PerformanceRecord,
        marcas: Map<String, Map<String, String>>
    ) {
        try {
            val ref = db.child(uid).child("regVAM").push()

            val data = mapOf(
                "VAM"         to record.VAM,
                "VAM_decimal" to record.VAM_decimal,
                "VO2Max"      to record.VO2Max,
                "fecha"       to record.fecha,
                "min"         to record.min,
                "seg"         to record.seg,
                "semicooper"  to record.semicooper
            )
            ref.setValue(data).await()

            if (marcas.isNotEmpty()) {
                ref.child("marcas").setValue(marcas).await()
            }

            Log.d("PerformanceService", "Snapshot con ${marcas.size} marcas: ${ref.key}")
        } catch (e: Exception) {
            Log.e("PerformanceService", "Error guardando snapshot", e)
            throw e
        }
    }

}