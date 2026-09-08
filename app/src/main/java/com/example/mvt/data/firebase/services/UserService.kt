package com.example.mvt.data.firebase.services

import android.net.Uri
import android.util.Log
import com.example.mvt.data.firebase.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserService {

    private val db      = FirebaseDatabase.getInstance().getReference("users")
    private val storage = FirebaseStorage.getInstance()
    private val auth    = FirebaseAuth.getInstance()

    // === Mapeo Manual ===
    private fun mapSnapshotToUser(snapshot: DataSnapshot): User? {
        if (!snapshot.exists()) return null
        return try {
            User(
                UserID = snapshot.child("UserID").value?.toString()?.toIntOrNull(),
                nombres = snapshot.child("nombres").value?.toString(),
                apellidos = snapshot.child("apellidos").value?.toString(),
                email = snapshot.child("email").value?.toString(),
                ciudadActual = snapshot.child("ciudadActual").value?.toString(),
                direccion = snapshot.child("direccion").value?.toString(),
                estado = snapshot.child("estado").value?.toString(),
                estrellas = snapshot.child("estrellas").value?.toString()?.toIntOrNull(),
                fecha_nacimiento = snapshot.child("fecha_nacimiento").value?.toString(),
                fecha_registro = snapshot.child("fecha_registro").value?.toString()?.toLongOrNull(),
                descripcion = snapshot.child("descripcion").value?.toString(),
                NotiR = snapshot.child("NotiR").value as? Boolean,
                foto_url = snapshot.child("foto_url").value?.toString(),
                nameUser = snapshot.child("nameUser").value?.toString(),
                telefono = snapshot.child("telefono").value?.toString(),
                genero = snapshot.child("genero").value?.toString(),
                identificacion = snapshot.child("identificacion").value?.toString(),
                pais = snapshot.child("pais").value?.toString()
            )
        } catch (e: Exception) {
            Log.e("UserService", "Error mapeando usuario manualmente", e)
            null
        }
    }

    // === Observar cambios en tiempo real sin crashes ===
    fun observeCurrentUser(): Flow<User?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (auth.currentUser == null) {
                    trySend(null)
                    return
                }
                trySend(mapSnapshotToUser(snapshot))
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        db.child(uid).addValueEventListener(listener)
        awaitClose { db.child(uid).removeEventListener(listener) }
    }

    suspend fun getCurrentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snapshot = db.child(uid).get().await()
            mapSnapshotToUser(snapshot)
        } catch (e: Exception) {
            Log.e("UserService", "Error al obtener usuario", e)
            null
        }
    }

    suspend fun updateUser(
        nombres:      String,
        apellidos:    String,
        telefono:     String,
        genero:       String,
        nacionalidad: String,
        alias:        String,
        documento:    String
    ) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val updates = mapOf(
                "nombres"       to nombres,
                "apellidos"     to apellidos,
                "telefono"      to telefono,
                "genero"        to genero,
                "pais"          to nacionalidad,
                "nameUser"      to alias,
                "identificacion" to documento
            )
            db.child(uid).updateChildren(updates).await()
        } catch (e: Exception) {
            Log.e("UserService", "Error al actualizar usuario", e)
            throw e
        }
    }

    suspend fun uploadProfilePhoto(uri: Uri): String {
        val uid = auth.currentUser?.uid
            ?: throw Exception("Usuario no autenticado")
        try {
            val storageRef = storage.getReference("profile_photos/$uid.jpg")
            storageRef.putFile(uri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            db.child(uid).child("foto_url").setValue(downloadUrl).await()
            return downloadUrl
        } catch (e: Exception) {
            Log.e("UserService", "Error al subir foto", e)
            throw e
        }
    }
}
