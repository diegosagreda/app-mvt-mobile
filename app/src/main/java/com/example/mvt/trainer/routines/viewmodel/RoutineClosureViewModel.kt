package com.example.mvt.trainer.routines.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.trainer.routines.data.RoutineClosureRepository
import com.example.mvt.trainer.routines.model.RoutineClosureItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RoutineClosureUiState {
    data object Loading : RoutineClosureUiState
    data class Success(val routines: List<RoutineClosureItem>) : RoutineClosureUiState
    data class Error(val message: String) : RoutineClosureUiState
}

class RoutineClosureViewModel(
    private val repository: RoutineClosureRepository = RoutineClosureRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<RoutineClosureUiState>(RoutineClosureUiState.Loading)
    val uiState: StateFlow<RoutineClosureUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = RoutineClosureUiState.Loading
            repository.fetchPending()
                .onSuccess { _uiState.value = RoutineClosureUiState.Success(it) }
                .onFailure {
                    _uiState.value = RoutineClosureUiState.Error(
                        it.localizedMessage ?: "No fue posible cargar las rutinas."
                    )
                }
        }
    }
}
