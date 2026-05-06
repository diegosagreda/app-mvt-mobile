package com.example.mvt.ui.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.data.firebase.models.User
import com.example.mvt.domain.repositories.UserRepository
import com.example.mvt.domain.usecases.GetUserInfoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// === Estados posibles de la pantalla de perfil ===
sealed class ProfileUiState {
    object Idle    : ProfileUiState()
    object Loading : ProfileUiState()
    object Saved   : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class UserViewModel(
    private val getUserInfoUseCase: GetUserInfoUseCase
) : ViewModel() {

    private val repository = UserRepository()

    // === Estado del usuario ===
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    // === Estado de la UI ===
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState

    // === Cargar usuario ===
    fun loadUserInfo() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val userInfo = getUserInfoUseCase()
                Log.d("UserViewModel", "Usuario cargado: ${userInfo?.nombres}")
                _user.value  = userInfo
                _uiState.value = ProfileUiState.Idle
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error cargando usuario", e)
                _uiState.value = ProfileUiState.Error("Error al cargar los datos")
            }
        }
    }

    // === Actualizar perfil ===
    fun updateUser(
        nombres:      String,
        apellidos:    String,
        telefono:     String,
        genero:       String,
        nacionalidad: String,
        alias:        String,
        documento:    String,
        onSuccess:    () -> Unit,
        onError:      () -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.updateUser(
                    nombres      = nombres,
                    apellidos    = apellidos,
                    telefono     = telefono,
                    genero       = genero,
                    nacionalidad = nacionalidad,
                    alias        = alias,
                    documento    = documento
                )
                // Recarga el usuario para reflejar cambios
                _user.value = getUserInfoUseCase()
                _uiState.value = ProfileUiState.Saved
                onSuccess()
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error actualizando usuario", e)
                _uiState.value = ProfileUiState.Error("Error al guardar los datos")
                onError()
            }
        }
    }

    // === Subir foto de perfil ===
    fun uploadProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            try {
                repository.uploadProfilePhoto(uri)
                // Recarga usuario para mostrar nueva foto
                _user.value = getUserInfoUseCase()
                Log.d("UserViewModel", "Foto subida correctamente")
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error subiendo foto", e)
                _uiState.value = ProfileUiState.Error("Error al subir la foto")
            }
        }
    }

    // === Resetear estado ===
    fun resetState() {
        _uiState.value = ProfileUiState.Idle
    }
}