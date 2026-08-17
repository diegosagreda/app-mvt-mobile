package com.example.mvt.trainer.catalog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.trainer.catalog.data.TrainersCatalogRepository
import com.example.mvt.trainer.catalog.model.AthleteRatingProfile
import com.example.mvt.trainer.catalog.model.TrainerProfile
import com.example.mvt.trainer.catalog.model.TrainerRequest
import com.example.mvt.trainer.catalog.model.TrainersCatalogState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CatalogActionState(
    val isWorking: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

class TrainersCatalogViewModel(
    private val repository: TrainersCatalogRepository = TrainersCatalogRepository()
) : ViewModel() {
    private val _state = MutableStateFlow<TrainersCatalogState>(TrainersCatalogState.Loading)
    val state: StateFlow<TrainersCatalogState> = _state.asStateFlow()

    private val _action = MutableStateFlow(CatalogActionState())
    val action: StateFlow<CatalogActionState> = _action.asStateFlow()
    private var observeJob: Job? = null

    fun start(athleteId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observeCatalog(athleteId).collect { _state.value = it }
        }
    }

    fun sendRequest(athlete: AthleteRatingProfile, trainer: TrainerProfile) = runAction {
        repository.sendRequest(athlete, trainer)
        "Solicitud enviada correctamente."
    }

    fun cancelRequest(athleteId: String, request: TrainerRequest) = runAction {
        repository.cancelRequest(athleteId, request)
        "Solicitud cancelada."
    }

    fun clearAction() { _action.value = CatalogActionState() }

    private fun runAction(block: suspend () -> String) {
        if (_action.value.isWorking) return
        viewModelScope.launch {
            _action.value = CatalogActionState(isWorking = true)
            runCatching { block() }
                .onSuccess { _action.value = CatalogActionState(message = it) }
                .onFailure {
                    _action.value = CatalogActionState(
                        message = it.message ?: "No fue posible completar la operación.",
                        isError = true
                    )
                }
        }
    }
}
