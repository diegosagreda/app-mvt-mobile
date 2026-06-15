package com.example.mvt.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.data.firebase.models.StravaConnection
import com.example.mvt.domain.repositories.StravaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class StravaConnectionUiState(
    val isLoading: Boolean = true,
    val isEnabled: Boolean = false,
    val connection: StravaConnection? = null,
    val isAuthVisible: Boolean = false,
    val authUrl: String = "",
    val redirectUri: String = "",
    val authState: String = "",
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
        val authState = UUID.randomUUID().toString()
        _uiState.update {
            it.copy(
                isAuthVisible = true,
                authUrl = repository.buildAuthorizationUrl(authState),
                redirectUri = repository.getRedirectUri(),
                authState = authState,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun closeAuth() {
        _uiState.update { it.copy(isAuthVisible = false, authState = "") }
    }

    fun onAuthCancelled() {
        _uiState.update {
            it.copy(
                isAuthVisible = false,
                authState = "",
                errorMessage = "La autorizacion con Strava fue cancelada."
            )
        }
    }

    fun onAuthLaunchFailed() {
        _uiState.update {
            it.copy(
                isAuthVisible = false,
                authState = "",
                errorMessage = "No fue posible abrir Strava o el navegador seguro."
            )
        }
    }

    fun toggleSync(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            runCatching {
                if (enabled) {
                    repository.setSyncEnabled(true)
                } else {
                    repository.disconnect()
                }
            }
                .onSuccess {
                    if (enabled) {
                        load()
                    } else {
                        val wasEnabled = _uiState.value.isEnabled
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isEnabled = wasEnabled,
                                connection = null,
                                successMessage = "Conexion con Strava eliminada."
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

    fun exchangeCode(code: String, scope: String?, state: String?) {
        val expectedState = _uiState.value.authState
        if (expectedState.isBlank() || state != expectedState) {
            _uiState.update {
                it.copy(
                    isAuthVisible = false,
                    authState = "",
                    errorMessage = "La respuesta de Strava no es valida. Intenta de nuevo."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isAuthVisible = false,
                    authState = "",
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
