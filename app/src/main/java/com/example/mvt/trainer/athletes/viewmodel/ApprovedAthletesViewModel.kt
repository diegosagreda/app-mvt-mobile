package com.example.mvt.trainer.athletes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.trainer.athletes.data.ApprovedAthletesRepository
import com.example.mvt.trainer.athletes.model.ApprovedAthlete
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ApprovedAthletesUiState {
    data object Loading : ApprovedAthletesUiState
    data class Success(val athletes: List<ApprovedAthlete>) : ApprovedAthletesUiState
    data class Error(val message: String) : ApprovedAthletesUiState
}

class ApprovedAthletesViewModel(
    private val repository: ApprovedAthletesRepository = ApprovedAthletesRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<ApprovedAthletesUiState>(ApprovedAthletesUiState.Loading)
    val uiState: StateFlow<ApprovedAthletesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ApprovedAthletesUiState.Loading
            repository.fetchApprovedAthletes()
                .onSuccess { _uiState.value = ApprovedAthletesUiState.Success(it) }
                .onFailure {
                    _uiState.value = ApprovedAthletesUiState.Error(
                        it.localizedMessage ?: "No fue posible cargar los deportistas aprobados."
                    )
                }
        }
    }
}
