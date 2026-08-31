package com.example.mvt.trainer.main.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.trainer.main.data.TrainerDashboardRepository
import com.example.mvt.trainer.main.model.TrainerDashboardModel
import com.example.mvt.trainer.main.model.TrainerPlanDashboard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TrainerDashboardUiState {
    data object Loading : TrainerDashboardUiState
    data class Content(
        val fullData: TrainerDashboardModel,
        val selectedPlan: String = "Plata"
    ) : TrainerDashboardUiState {
        val currentPlanData: TrainerPlanDashboard
            get() = fullData.plans[selectedPlan] ?: TrainerPlanDashboard()
    }
    data class Error(val message: String) : TrainerDashboardUiState
}

class TrainerDashboardViewModel : ViewModel() {
    private val repository = TrainerDashboardRepository()

    private val _uiState = MutableStateFlow<TrainerDashboardUiState>(TrainerDashboardUiState.Loading)
    val uiState: StateFlow<TrainerDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = TrainerDashboardUiState.Loading
            runCatching {
                repository.getDashboardData()
            }.onSuccess { data ->
                _uiState.value = TrainerDashboardUiState.Content(data)
            }.onFailure { error ->
                _uiState.value = TrainerDashboardUiState.Error(error.message ?: "Error desconocido")
            }
        }
    }

    fun selectPlan(plan: String) {
        val currentState = _uiState.value
        if (currentState is TrainerDashboardUiState.Content) {
            _uiState.value = currentState.copy(selectedPlan = plan)
        }
    }
}
