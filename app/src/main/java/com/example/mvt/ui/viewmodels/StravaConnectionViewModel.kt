package com.example.mvt.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.data.firebase.models.StravaConnection
import com.example.mvt.domain.repositories.StravaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StravaConnectionUiState(
    val isLoading: Boolean = true,
    val isEnabled: Boolean = false,
    val connection: StravaConnection? = null,
    val isAuthVisible: Boolean = false,
    val authUrl: String = "",
    val redirectUri: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class StravaConnectionViewModel : ViewModel() {

    private val repository = StravaRepository()

    private val _uiState = MutableStateFlow(StravaConnectionUiState())
    val uiState: StateFlow<StravaConnectionUiState> = _uiState

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            runCatching { repository.getConnectionSnapshot() }
                .onSuccess { snapshot ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEnabled = snapshot.isEnabled,
                            connection = snapshot.connection
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No fue posible cargar la conexion."
                        )
                    }
                }
        }
    }

    fun openAuth() {
        _uiState.update {
            it.copy(
                isAuthVisible = true,
                authUrl = repository.buildAuthorizationUrl(),
                redirectUri = repository.getRedirectUri(),
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun closeAuth() {
        _uiState.update { it.copy(isAuthVisible = false) }
    }

    fun onAuthCancelled() {
        _uiState.update {
            it.copy(
                isAuthVisible = false,
                errorMessage = "La autorizacion con Strava fue cancelada."
            )
        }
    }

    fun toggleSync(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            runCatching { repository.setSyncEnabled(enabled) }
                .onSuccess {
                    if (enabled) {
                        load()
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isEnabled = false,
                                connection = null,
                                successMessage = "Sincronizacion con Strava desactivada."
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No fue posible actualizar Strava."
                        )
                    }
                }
        }
    }

    fun exchangeCode(code: String, scope: String?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isAuthVisible = false,
                    errorMessage = null,
                    successMessage = null
                )
            }
            runCatching { repository.exchangeToken(code, scope) }
                .onSuccess { connection ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEnabled = true,
                            connection = connection,
                            successMessage = "Strava conectado correctamente."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No fue posible conectar Strava."
                        )
                    }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
