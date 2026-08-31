package com.example.mvt.trainer.profile.data

import android.net.Uri
import com.example.mvt.trainer.profile.model.PersonalInfoModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class PersonalInfoRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    suspend fun getCurrentProfile(): PersonalInfoModel {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("No hay una sesión activa.")

        val snapshot = database
            .getReference("users")
            .child(uid)
            .get()
            .await()

        if (!snapshot.exists()) {
            throw IllegalStateException("No fue posible encontrar tu perfil.")
        }

        return PersonalInfoModel(
            uid = uid,
            firstName = snapshot.text("nombres"),
            lastName = snapshot.text("apellidos"),
            identification = snapshot.text("identificacion"),
            gender = snapshot.text("genero"),
            birthDate = snapshot.text("fecha_nacimiento"),
            photoUrl = snapshot.text("foto_url"),
            email = snapshot.text("email"),
            alias = snapshot.text("nameUser"),
            mvtId = snapshot.text("UserID")
        )
    }

    suspend fun uploadProfilePhoto(uri: Uri): String {
        val uid = auth.currentUser?.uid
            ?: throw Exception("Usuario no autenticado")
        
        try {
            // Usamos un nombre de archivo fijo por UID para sobrescribir y ser eficientes
            val storageRef = storage.getReference("profile_photos/trainer_$uid.jpg")
            
            // Subida del archivo
            storageRef.putFile(uri).await()
            
            // Obtención de la URL de descarga
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            // Actualización en la base de datos
            database.getReference("users")
                .child(uid)
                .child("foto_url")
                .setValue(downloadUrl)
                .await()
                
            return downloadUrl
        } catch (e: Exception) {
            throw e
        }
    }

    private fun DataSnapshot.text(field: String): String {
        return child(field)
            .value
            ?.toString()
            ?.takeUnless { it.equals("null", ignoreCase = true) }
            .orEmpty()
    }
}