package com.example.mvt.data.firebase.services

import android.net.Uri
import android.util.Log
import com.example.mvt.data.firebase.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class UserService {

    private val db      = FirebaseDatabase.getInstance().getReference("users")
    private val storage = FirebaseStorage.getInstance()
    private val auth    = FirebaseAuth.getInstance()

    // === Obtener usuario actual ===
    suspend fun getCurrentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snapshot = db.child(uid).get().await()
            snapshot.getValue(User::class.java)
        } catch (e: Exception) {
            Log.e("UserService", "Error al obtener usuario", e)
            null
        }
    }

    // === Actualizar campos del perfil ===
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
            Log.d("UserService", "Usuario actualizado correctamente")
        } catch (e: Exception) {
            Log.e("UserService", "Error al actualizar usuario", e)
            throw e
        }
    }

    // === Subir foto de perfil ===
    suspend fun uploadProfilePhoto(uri: Uri): String {
        val uid = auth.currentUser?.uid
            ?: throw Exception("Usuario no autenticado")
        try {
            val storageRef = storage
                .getReference("profile_photos/$uid.jpg")
            storageRef.putFile(uri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            db.child(uid).child("foto_url").setValue(downloadUrl).await()
            Log.d("UserService", "Foto actualizada correctamente")
            return downloadUrl
        } catch (e: Exception) {
            Log.e("UserService", "Error al subir foto", e)
            throw e
        }
    }
}