package com.example.mvt.health.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.health.data.HealthRepository
import com.example.mvt.health.model.HealthInjury
import com.example.mvt.health.model.HealthProfile
import com.example.mvt.health.model.HealthRepositoryState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface HealthScreenState {
    data object Loading : HealthScreenState
    data class Ready(
        val health: HealthProfile,
        val initialHealth: HealthProfile,
        val nutritionalOptions: List<String>,
        val trainerId: String,
        val catalogError: String? = null
    ) : HealthScreenState {
        val hasUnsavedChanges: Boolean get() = health != initialHealth
    }
    data class Error(val message: String) : HealthScreenState
}

data class HealthActionState(
    val isSaving: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

class HealthViewModel(
    private val repository: HealthRepository = HealthRepository()
) : ViewModel() {
    private val _state = MutableStateFlow<HealthScreenState>(HealthScreenState.Loading)
    val state: StateFlow<HealthScreenState> = _state.asStateFlow()
    private val _action = MutableStateFlow(HealthActionState())
    val action: StateFlow<HealthActionState> = _action.asStateFlow()
    private var athleteId = ""
    private var observeJob: Job? = null

    fun load(uid: String) {
        athleteId = uid
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observe(uid).collect { repositoryState ->
                when (repositoryState) {
                    HealthRepositoryState.Loading -> _state.value = HealthScreenState.Loading
                    is HealthRepositoryState.Error -> _state.value = HealthScreenState.Error(repositoryState.message)
                    is HealthRepositoryState.Ready -> {
                        val previous = _state.value as? HealthScreenState.Ready
                        val data = repositoryState.data
                        _state.value = if (previous != null) {
                            previous.copy(
                                nutritionalOptions = mergeHistoricalOption(data.nutritionalOptions, previous.health.nutritionalStatus),
                                trainerId = data.trainerId,
                                catalogError = data.catalogError
                            )
                        } else {
                            HealthScreenState.Ready(
                                health = data.health,
                                initialHealth = data.health,
                                nutritionalOptions = mergeHistoricalOption(data.nutritionalOptions, data.health.nutritionalStatus),
                                trainerId = data.trainerId,
                                catalogError = data.catalogError
                            )
                        }
                    }
                }
            }
        }
    }

    fun updateHealth(transform: (HealthProfile) -> HealthProfile) {
        val ready = _state.value as? HealthScreenState.Ready ?: return
        _state.value = ready.copy(health = transform(ready.health))
    }

    fun addInjury(location: String, treatment: String, currentlyActive: Boolean) {
        require(location.trim().length >= 2 && treatment.trim().length >= 2) {
            "Completa la localización y el tratamiento."
        }
        val now = System.currentTimeMillis()
        val injury = HealthInjury(
            id = UUID.randomUUID().toString(),
            location = location.trim(), treatment = treatment.trim(),
            currentlyActive = currentlyActive, createdAt = now, updatedAt = now
        )
        val ready = _state.value as? HealthScreenState.Ready ?: return
        persistInjuries(ready, ready.health.injuries + injury, "Lesión registrada correctamente.")
    }

    fun removeInjury(injuryId: String) {
        val ready = _state.value as? HealthScreenState.Ready ?: return
        persistInjuries(
            ready,
            ready.health.injuries.filterNot { injury -> injury.id == injuryId },
            "Lesión eliminada correctamente."
        )
    }

    fun updateInjury(injuryId: String, location: String, treatment: String, currentlyActive: Boolean) {
        require(location.trim().length >= 2 && treatment.trim().length >= 2) {
            "Completa la localización y el tratamiento."
        }
        val ready = _state.value as? HealthScreenState.Ready ?: return
        val updated = ready.health.injuries.map { injury ->
            if (injury.id == injuryId) {
                injury.copy(
                    location = location.trim(), treatment = treatment.trim(),
                    currentlyActive = currentlyActive, updatedAt = System.currentTimeMillis()
                )
            } else injury
        }
        persistInjuries(ready, updated, "Lesión actualizada correctamente.")
    }

    fun discardChanges() {
        val ready = _state.value as? HealthScreenState.Ready ?: return
        _state.value = ready.copy(health = ready.initialHealth)
    }

    fun save() {
        val ready = _state.value as? HealthScreenState.Ready ?: return
        if (!ready.hasUnsavedChanges || _action.value.isSaving) return
        val changedFields = changedFields(ready.initialHealth, ready.health)
        viewModelScope.launch {
            _action.value = HealthActionState(isSaving = true)
            runCatching {
                repository.save(athleteId, ready.health, ready.trainerId, changedFields)
            }.onSuccess { saved ->
                _state.value = ready.copy(health = saved, initialHealth = saved)
                _action.value = HealthActionState(message = "Información de salud actualizada correctamente.")
            }.onFailure {
                _action.value = HealthActionState(
                    message = it.message ?: "No fue posible actualizar la información de salud.",
                    isError = true
                )
            }
        }
    }

    fun clearAction() { _action.value = HealthActionState() }

    private fun persistInjuries(
        ready: HealthScreenState.Ready,
        injuries: List<HealthInjury>,
        successMessage: String
    ) {
        if (_action.value.isSaving) return
        viewModelScope.launch {
            _action.value = HealthActionState(isSaving = true)
            runCatching { repository.saveInjuries(athleteId, injuries, ready.trainerId) }
                .onSuccess {
                    _state.value = ready.copy(
                        health = ready.health.copy(injuries = injuries),
                        initialHealth = ready.initialHealth.copy(injuries = injuries)
                    )
                    _action.value = HealthActionState(message = successMessage)
                }
                .onFailure {
                    _action.value = HealthActionState(
                        message = it.message ?: "No fue posible actualizar las lesiones.",
                        isError = true
                    )
                }
        }
    }

    private fun changedFields(old: HealthProfile, new: HealthProfile): List<String> = buildList {
        if (old.workPhysicalEffort != new.workPhysicalEffort) add("esfuerzo_trabajo")
        if (old.sleepHours != new.sleepHours) add("sueño")
        if (old.nutritionalStatus != new.nutritionalStatus) add("estadoNutricion")
        if (old.healthCondition != new.healthCondition) add("condicion_salud")
        if (old.injuries != new.injuries) add("lesiones")
        if (old.diseases != new.diseases) add("enfermedades")
    }

    private fun mergeHistoricalOption(options: List<String>, selected: String): List<String> {
        return (options + selected.takeIf(String::isNotBlank)).filterNotNull().distinct()
    }
}
