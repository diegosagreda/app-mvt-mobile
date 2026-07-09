package com.example.mvt.sports.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.sports.data.SportsRepository
import com.example.mvt.sports.model.SportsProfile
import com.example.mvt.sports.model.SportsValidationErrors
import com.example.mvt.sports.model.changedSportsFields
import com.example.mvt.sports.model.validateSportsProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SportsScreenState {
    data object Loading : SportsScreenState
    data class Ready(
        val profile: SportsProfile,
        val initialProfile: SportsProfile,
        val errors: SportsValidationErrors = SportsValidationErrors()
    ) : SportsScreenState {
        val hasUnsavedChanges: Boolean get() = profile != initialProfile
    }
    data class LoadError(val message: String) : SportsScreenState
}

data class SportsActionState(
    val isSaving: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

class SportsViewModel(
    private val repository: SportsRepository = SportsRepository()
) : ViewModel() {
    private val _state = MutableStateFlow<SportsScreenState>(SportsScreenState.Loading)
    val state: StateFlow<SportsScreenState> = _state.asStateFlow()
    private val _action = MutableStateFlow(SportsActionState())
    val action: StateFlow<SportsActionState> = _action.asStateFlow()
    private var athleteId = ""

    fun load(uid: String) {
        athleteId = uid
        viewModelScope.launch {
            _state.value = SportsScreenState.Loading
            runCatching { repository.load(uid) }
                .onSuccess { profile ->
                    _state.value = SportsScreenState.Ready(profile, profile)
                }
                .onFailure { error ->
                    _state.value = SportsScreenState.LoadError(
                        error.message ?: "No fue posible cargar tu información deportiva."
                    )
                }
        }
    }

    fun update(transform: (SportsProfile) -> SportsProfile) {
        val ready = _state.value as? SportsScreenState.Ready ?: return
        _state.value = ready.copy(profile = transform(ready.profile), errors = SportsValidationErrors())
    }

    fun discardChanges() {
        val ready = _state.value as? SportsScreenState.Ready ?: return
        _state.value = ready.copy(profile = ready.initialProfile, errors = SportsValidationErrors())
    }

    fun save() {
        val ready = _state.value as? SportsScreenState.Ready ?: return
        if (_action.value.isSaving || !ready.hasUnsavedChanges) return
        val payload = ready.profile.normalizedForSave()
        val errors = validateSportsProfile(payload)
        if (errors.hasErrors) {
            _state.value = ready.copy(profile = payload, errors = errors)
            _action.value = SportsActionState(
                message = "Corrige los campos marcados antes de guardar.",
                isError = true
            )
            return
        }
        val changes = changedSportsFields(ready.initialProfile.normalizedForSave(), payload)
        if (changes.isEmpty()) {
            _state.value = ready.copy(profile = payload, initialProfile = payload)
            return
        }

        viewModelScope.launch {
            _action.value = SportsActionState(isSaving = true)
            runCatching { repository.save(athleteId, payload) }
                .onSuccess {
                    _state.value = SportsScreenState.Ready(payload, payload)
                    _action.value = SportsActionState(message = "Información deportiva actualizada correctamente.")
                    runCatching { repository.notifyCoach(athleteId, changes) }
                        .onFailure(repository::logNotificationError)
                }
                .onFailure { error ->
                    _action.value = SportsActionState(
                        message = error.message ?: "No fue posible actualizar tu información deportiva.",
                        isError = true
                    )
                }
        }
    }

    fun clearAction() { _action.value = SportsActionState() }
}
