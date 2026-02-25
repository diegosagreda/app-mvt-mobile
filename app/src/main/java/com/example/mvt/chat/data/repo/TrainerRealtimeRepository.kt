package com.example.mvt.chat.data.repo

import android.annotation.SuppressLint
import android.util.Log
import com.example.mvt.chat.data.model.TrainerPersonalData
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TrainerRealtimeRepository(
    private val rtdb: FirebaseDatabase
) {
    private val TAG = "TrainerRealtimeRepo"
    private val root: DatabaseReference get() = rtdb.reference

    /**
     * Observa solicitudes y emite el id del entrenador si:
     * - id_deportista == athleteId
     * - estado == "Aprobado" (equivalente a match)
     * - id_entrenador no está vacío
     *
     * Soporta dos rutas:
     * - /solicitudes
     * - /signUp/solicitudes
     */
    @SuppressLint("RestrictedApi")
    fun observeTrainerIdForAthlete(athleteId: String): Flow<String> = callbackFlow {
        Log.d(TAG, "observeTrainerIdForAthlete() athleteId=$athleteId")

        val refRootSolicitudes = root.child("solicitudes")
        val refSignUpSolicitudes = root.child("signUp").child("solicitudes")

        val chosenRef = try {
            val snapRoot = refRootSolicitudes.get().await()
            if (snapRoot.exists()) {
                Log.d(TAG, "Usando ruta: /solicitudes")
                refRootSolicitudes
            } else {
                val snapSignUp = refSignUpSolicitudes.get().await()
                if (snapSignUp.exists()) {
                    Log.d(TAG, "Usando ruta: /signUp/solicitudes")
                    refSignUpSolicitudes
                } else {
                    Log.d(TAG, "No existe /solicitudes ni /signUp/solicitudes")
                    // Si no existe, igual escuchamos /signUp/solicitudes por defecto
                    refSignUpSolicitudes
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error determinando ruta solicitudes: ${e.message}", e)
            refSignUpSolicitudes
        }

        val listener = object : ValueEventListener {
            @SuppressLint("RestrictedApi")
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "onDataChange(${chosenRef.path}) exists=${snapshot.exists()} children=${snapshot.childrenCount}")

                if (!snapshot.exists()) {
                    trySend("")
                    return
                }

                val (match, entrenadorId) = verificarMatch(athleteId, snapshot)
                Log.d(TAG, "verificarMatch => match=$match entrenadorId='$entrenadorId'")

                val out = if (match && entrenadorId.isNotBlank()) entrenadorId else ""
                Log.d(TAG, "emit trainerId='$out'")
                trySend(out)
            }

            @SuppressLint("RestrictedApi")
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "onCancelled(${chosenRef.path}): ${error.message}", error.toException())
                trySend("")
            }
        }

        chosenRef.addValueEventListener(listener)
        awaitClose {
            Log.d(TAG, "awaitClose(): removeEventListener(${chosenRef.path})")
            chosenRef.removeEventListener(listener)
        }
    }

    /**
     * Lee users/{idEntrenador} y retorna datos personales (incluye foto_url).
     */
    suspend fun getTrainerPersonalData(trainerId: String): TrainerPersonalData {
        val safeId = trainerId.trim()
        Log.d(TAG, "getTrainerPersonalData() trainerId='$safeId'")

        if (safeId.isBlank()) return TrainerPersonalData()

        return try {
            val snap = root.child("users").child(safeId).get().await()
            Log.d(TAG, "users/$safeId exists=${snap.exists()} children=${snap.childrenCount}")

            if (!snap.exists()) return TrainerPersonalData()

            fun s(key: String) = snap.child(key).getValue(String::class.java).orEmpty()

            val data = TrainerPersonalData(
                identificacion = s("identificacion"),
                nombres = s("nombres"),
                apellidos = s("apellidos"),
                genero = s("genero"),
                email = s("email"),
                fecha_nacimiento = snap.child("fecha_nacimiento").value,
                pais = s("pais"),
                ciudad = s("ciudad"),
                telefono = s("telefono"),
                direccion = s("direccion"),
                formularioBienvenida = snap.child("formularioBienvenida").value,
                foto_url = s("foto_url")
            )

            Log.d(TAG, "TrainerPersonalData.foto_url='${data.foto_url}'")
            data
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo users/{trainerId}: ${e.message}", e)
            TrainerPersonalData()
        }
    }

    /**
     * Tu estructura real (según captura):
     * solicitudes/{pushId}/id_deportista
     * solicitudes/{pushId}/id_entrenador
     * solicitudes/{pushId}/estado  -> "Aprobado" (equivale a match)
     */
    private fun verificarMatch(athleteId: String, solicitudesSnap: DataSnapshot): Pair<Boolean, String> {
        Log.d(TAG, "verificarMatch() buscando athleteId='$athleteId'")

        for (child in solicitudesSnap.children) {
            val key = child.key.orEmpty()

            val idDeportista = child.child("id_deportista").getValue(String::class.java).orEmpty()
            val idEntrenador = child.child("id_entrenador").getValue(String::class.java).orEmpty()
            val estado = child.child("estado").getValue(String::class.java).orEmpty()

            // Log mínimo por item para no spamear (quita si quieres)
            // Log.d(TAG, "item key='$key' id_deportista='$idDeportista' estado='$estado' id_entrenador='$idEntrenador'")

            if (idDeportista == athleteId) {
                val match = estado.equals("Aprobado", ignoreCase = true) && idEntrenador.isNotBlank()
                Log.d(TAG, "FOUND key='$key' estado='$estado' match=$match id_entrenador='$idEntrenador'")
                return match to idEntrenador
            }
        }

        Log.d(TAG, "No se encontró solicitud para athleteId='$athleteId'")
        return false to ""
    }
}