package com.example.mvt.data.firebase.services

import android.util.Log
import com.example.mvt.data.firebase.models.Morphology
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class MorphologyService {

    private val db = FirebaseDatabase.getInstance().getReference("Morfologias")

    // === Leer morfología del deportista ===
    suspend fun getMorphology(uid: String): Morphology? {
        return try {
            val snapshot = db.child(uid).get().await()
            if (!snapshot.exists()) return null

            Morphology(
                estatura          = leerCampo(snapshot, "estatura"),
                peso              = leerCampo(snapshot, "peso"),
                grasa             = leerCampo(snapshot, "grasa"),
                imc               = leerCampo(snapshot, "IMC"),
                somatipo          = leerCampo(snapshot, "somatipo"),
                fecha_hombros     = leerCampo(snapshot, "fecha_hombros"),
                medida_hombros    = leerCampo(snapshot, "medida_hombros"),
                fecha_pecho       = leerCampo(snapshot, "fecha_pecho"),
                medida_pecho      = leerCampo(snapshot, "medida_pecho"),
                fecha_brazo       = leerCampo(snapshot, "fecha_brazo"),
                medida_brazo      = leerCampo(snapshot, "medida_brazo"),
                fecha_cintura     = leerCampo(snapshot, "fecha_cintura"),
                medida_cintura    = leerCampo(snapshot, "medida_cintura"),
                fecha_muslo       = leerCampo(snapshot, "fecha_muslo"),
                medida_muslo      = leerCampo(snapshot, "medida_muslo"),
                fecha_gluteos     = leerCampo(snapshot, "fecha_gluteos"),
                medida_gluteos    = leerCampo(snapshot, "medida_gluteos"),
                fecha_pantorrilla = leerCampo(snapshot, "fecha_pantorrilla"),
                medida_pantorrilla = leerCampo(snapshot, "medida_pantorrilla")
            )
        } catch (e: Exception) {
            Log.e("MorphologyService", "Error al obtener morfología", e)
            null
        }
    }
    // === Guardar morfología ===
    suspend fun saveMorphology(uid: String, morphology: Morphology) {
        try {
            val updates = mapOf(
                "estatura"            to morphology.estatura,
                "peso"                to morphology.peso,
                "grasa"               to morphology.grasa,
                "IMC"                 to morphology.imc,
                "somatipo"            to morphology.somatipo,
                "fecha_hombros"       to morphology.fecha_hombros,
                "medida_hombros"      to morphology.medida_hombros,
                "fecha_pecho"         to morphology.fecha_pecho,
                "medida_pecho"        to morphology.medida_pecho,
                "fecha_brazo"         to morphology.fecha_brazo,
                "medida_brazo"        to morphology.medida_brazo,
                "fecha_cintura"       to morphology.fecha_cintura,
                "medida_cintura"      to morphology.medida_cintura,
                "fecha_muslo"         to morphology.fecha_muslo,
                "medida_muslo"        to morphology.medida_muslo,
                "fecha_gluteos"       to morphology.fecha_gluteos,
                "medida_gluteos"      to morphology.medida_gluteos,
                "fecha_pantorrilla"   to morphology.fecha_pantorrilla,
                "medida_pantorrilla"  to morphology.medida_pantorrilla
            )
            db.child(uid).updateChildren(updates).await()
            Log.d("MorphologyService", "Morfología guardada correctamente")
        } catch (e: Exception) {
            Log.e("MorphologyService", "Error al guardar morfología", e)
            throw e
        }
    }
    // === Helper privado para leer cualquier tipo de Firebase ===
    private fun leerCampo(
        snapshot: com.google.firebase.database.DataSnapshot,
        key: String
    ): String {
        val value = snapshot.child(key).value ?: return ""
        return when (value) {
            is String -> value
            is Double -> if (value == kotlin.math.floor(value))
                value.toInt().toString()
            else value.toString()
            is Long   -> value.toString()
            else      -> value.toString()
        }
    }
}