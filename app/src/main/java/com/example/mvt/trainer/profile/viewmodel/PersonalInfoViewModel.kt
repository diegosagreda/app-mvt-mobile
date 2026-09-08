package com.example.mvt.trainer.profile.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.trainer.profile.data.PersonalInfoRepository
import com.example.mvt.trainer.profile.model.PersonalInfoModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PersonalInfoUiState {
    data object Loading : PersonalInfoUiState

    data class Content(
        val profile: PersonalInfoModel,
        val isUploading: Boolean = false,
        val optimisticPhotoUri: Uri? = null
    ) : PersonalInfoUiState

    data class Error(
        val message: String
    ) : PersonalInfoUiState
}

class PersonalInfoViewModel : ViewModel() {

    private val repository = PersonalInfoRepository()

    private val _uiState = MutableStateFlow<PersonalInfoUiState>(
        PersonalInfoUiState.Loading
    )
    val uiState: StateFlow<PersonalInfoUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            // Si ya hay contenido, no ponemos estado Loading total para evitar parpadeos,
            // a menos que sea la carga inicial.
            val currentContent = uiState.value as? PersonalInfoUiState.Content
            if (currentContent == null) {
                _uiState.value = PersonalInfoUiState.Loading
            }

            runCatching {
                repository.getCurrentProfile()
            }.onSuccess { profile ->
                _uiState.value = PersonalInfoUiState.Content(
                    profile = profile,
                    isUploading = false,
                    optimisticPhotoUri = null
                )
            }.onFailure { error ->
                _uiState.value = PersonalInfoUiState.Error(
                    message = error.message ?: "No fue posible cargar tu información"
                )
            }
        }
    }

    fun uploadPhoto(uri: Uri) {
        val currentContent = uiState.value as? PersonalInfoUiState.Content ?: return
        
        viewModelScope.launch {
            // Actualización Optimista: Mostramos la foto local inmediatamente
            _uiState.value = currentContent.copy(
                isUploading = true,
                optimisticPhotoUri = uri
            )
            
            runCatching {
                repository.uploadProfilePhoto(uri)
            }.onSuccess {
                // Una vez subida, recargamos los datos reales silenciosamente
                loadProfile()
            }.onFailure { error ->
                _uiState.value = PersonalInfoUiState.Error("Error al subir la foto: ${error.message}")
            }
        }
    }
}
