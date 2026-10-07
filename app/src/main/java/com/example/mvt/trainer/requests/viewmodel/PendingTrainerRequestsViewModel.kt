package com.example.mvt.trainer.requests.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.trainer.requests.data.PendingTrainerRequestsRepository
import com.example.mvt.trainer.requests.model.PendingTrainerRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PendingTrainerRequestsUiState {
    data object Loading : PendingTrainerRequestsUiState
    data class Success(val requests: List<PendingTrainerRequest>) : PendingTrainerRequestsUiState
    data class Error(val message: String) : PendingTrainerRequestsUiState
}

class PendingTrainerRequestsViewModel(
    private val repository: PendingTrainerRequestsRepository = PendingTrainerRequestsRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<PendingTrainerRequestsUiState>(
        PendingTrainerRequestsUiState.Loading
    )
    val uiState: StateFlow<PendingTrainerRequestsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = PendingTrainerRequestsUiState.Loading
            repository.fetchPending()
                .onSuccess { _uiState.value = PendingTrainerRequestsUiState.Success(it) }
                .onFailure {
                    _uiState.value = PendingTrainerRequestsUiState.Error(
                        it.localizedMessage ?: "No fue posible cargar las solicitudes."
                    )
                }
        }
    }

    fun updateStatus(request: PendingTrainerRequest, status: String) {
        viewModelScope.launch {
            repository.updateStatus(request, status)
                .onSuccess { load() }
                .onFailure {
                    _uiState.value = PendingTrainerRequestsUiState.Error(
                        it.localizedMessage ?: "No fue posible actualizar la solicitud."
                    )
                }
        }
    }
}
