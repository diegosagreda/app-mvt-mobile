package com.example.mvt.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.data.firebase.models.User
import com.example.mvt.domain.usecases.GetUserInfoUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserViewModel(
    private val getUserInfoUseCase: GetUserInfoUseCase
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    fun loadUserInfo() {
        viewModelScope.launch {
            val userInfo = getUserInfoUseCase()
            Log.d("UserViewModel", "Usuario cargado: ${userInfo?.nombres}, Foto: ${userInfo?.foto_url}")
            _user.value = userInfo
        }
    }
    fun updateUser(
        nombres: String,
        apellidos: String,
        telefono: String,
        genero: String,
        nacionalidad: String,
        alias: String,
        documento: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid

                if (uid == null) {
                    onError()
                    return@launch
                }

                val ref = FirebaseDatabase
                    .getInstance()
                    .getReference("users")
                    .child(uid)

                val updates = mapOf(
                    "nombres" to nombres,
                    "apellidos" to apellidos,
                    "telefono" to telefono,
                    "genero" to genero,
                    "pais" to nacionalidad,
                    "nameUser" to alias,
                    "identificacion" to documento
                )

                ref.updateChildren(updates)
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener {
                        onError()
                    }

            } catch (e: Exception) {
                onError()
            }
        }
    }

    // === Subir foto de perfil a Storage y actualizar URL en DB ===
    fun uploadProfilePhoto(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance()
                    .currentUser?.uid ?: return@launch

                //Subir imagen a Firebase Storage
                val storageRef = com.google.firebase.storage.FirebaseStorage
                    .getInstance()
                    .getReference("profile_photos/$uid.jpg")

                storageRef.putFile(uri).await()

                //Obtener la URL pública
                val downloadUrl = storageRef.downloadUrl.await().toString()

                //Guardar URL en Realtime Database
                com.google.firebase.database.FirebaseDatabase
                    .getInstance()
                    .getReference("users")
                    .child(uid)
                    .child("foto_url")
                    .setValue(downloadUrl)
                    .await()

                //Recargar usuario para reflejar cambio
                loadUserInfo()

                Log.d("UPLOAD_PHOTO", "Foto actualizada correctamente")

            } catch (e: Exception) {
                Log.e("UPLOAD_PHOTO", "Error al subir foto", e)
            }
        }
    }
}

