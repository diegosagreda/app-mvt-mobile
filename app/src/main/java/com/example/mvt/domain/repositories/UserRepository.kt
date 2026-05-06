package com.example.mvt.domain.repositories

import android.net.Uri
import com.example.mvt.data.firebase.models.User
import com.example.mvt.data.firebase.services.UserService

class UserRepository {

    private val service = UserService()

    suspend fun getCurrentUser(): User? {
        return service.getCurrentUser()
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
        service.updateUser(
            nombres      = nombres,
            apellidos    = apellidos,
            telefono     = telefono,
            genero       = genero,
            nacionalidad = nacionalidad,
            alias        = alias,
            documento    = documento
        )
    }

    suspend fun uploadProfilePhoto(uri: Uri): String {
        return service.uploadProfilePhoto(uri)
    }
}