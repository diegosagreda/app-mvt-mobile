package com.example.mvt.availability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.goals.data.GoalsRepository
import com.example.mvt.goals.model.GoalsPlan
import com.example.mvt.goals.model.TrainingAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AvailabilityScreenState {
    data object Loading : AvailabilityScreenState
    data class Ready(
        val availability: TrainingAvailability,
        val plan: GoalsPlan,
        val trainerId: String
    ) : AvailabilityScreenState
    data class Error(val message: String) : AvailabilityScreenState
}

data class AvailabilityActionState(
    val isWorking: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

class AvailabilityViewModel(
    private val repository: GoalsRepository = GoalsRepository()
) : ViewModel() {
    private val _state = MutableStateFlow<AvailabilityScreenState>(AvailabilityScreenState.Loading)
    val state: StateFlow<AvailabilityScreenState> = _state.asStateFlow()
    private val _action = MutableStateFlow(AvailabilityActionState())
    val action: StateFlow<AvailabilityActionState> = _action.asStateFlow()
    private var athleteId = ""

    fun load(uid: String) {
        athleteId = uid
        viewModelScope.launch {
            _state.value = AvailabilityScreenState.Loading
            runCatching { repository.load(uid) }
                .onSuccess {
                    _state.value = AvailabilityScreenState.Ready(it.availability, it.plan, it.trainerId)
                }
                .onFailure {
                    _state.value = AvailabilityScreenState.Error(it.message ?: "No fue posible cargar tu disponibilidad.")
                }
        }
    }

    fun save(availability: TrainingAvailability) {
        val ready = _state.value as? AvailabilityScreenState.Ready ?: return
        if (_action.value.isWorking) return
        viewModelScope.launch {
            _action.value = AvailabilityActionState(isWorking = true)
            runCatching {
                repository.saveAvailability(
                    athleteId = athleteId,
                    previous = ready.availability,
                    availability = availability,
                    plan = ready.plan,
                    trainerId = ready.trainerId
                )
            }.onSuccess {
                _state.value = ready.copy(availability = availability)
                _action.value = AvailabilityActionState(message = "Disponibilidad guardada correctamente.")
            }.onFailure {
                _action.value = AvailabilityActionState(
                    message = it.message ?: "No fue posible guardar tu disponibilidad.",
                    isError = true
                )
            }
        }
    }

    fun clearAction() { _action.value = AvailabilityActionState() }
}

