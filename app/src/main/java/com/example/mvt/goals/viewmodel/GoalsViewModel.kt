package com.example.mvt.goals.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.goals.data.GoalsRepository
import com.example.mvt.goals.model.GoalDraft
import com.example.mvt.goals.model.GoalsScreenState
import com.example.mvt.goals.model.SportGoal
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GoalsActionState(
    val isWorking: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

class GoalsViewModel(
    private val repository: GoalsRepository = GoalsRepository()
) : ViewModel() {
    private val _state = MutableStateFlow<GoalsScreenState>(GoalsScreenState.Loading)
    val state: StateFlow<GoalsScreenState> = _state.asStateFlow()
    private val _generalGoals = MutableStateFlow<List<String>>(emptyList())
    val generalGoals: StateFlow<List<String>> = _generalGoals.asStateFlow()
    private val _action = MutableStateFlow(GoalsActionState())
    val action: StateFlow<GoalsActionState> = _action.asStateFlow()
    private var athleteId = ""
    private var generalJob: Job? = null

    fun load(uid: String) {
        athleteId = uid
        viewModelScope.launch {
            _state.value = GoalsScreenState.Loading
            runCatching { repository.load(uid) }
                .onSuccess { _state.value = GoalsScreenState.Ready(it) }
                .onFailure { _state.value = GoalsScreenState.Error(it.userMessage()) }
        }
    }

    fun selectSport(sport: String) {
        generalJob?.cancel()
        _generalGoals.value = emptyList()
        if (sport.isBlank()) return
        generalJob = viewModelScope.launch {
            runCatching { repository.loadGeneralGoals(sport) }
                .onSuccess { _generalGoals.value = it }
                .onFailure { _action.value = GoalsActionState(message = it.userMessage(), isError = true) }
        }
    }

    fun addGoal(draft: GoalDraft) = runAction("Objetivo registrado correctamente.") { ready ->
        repository.addGoal(athleteId, draft, ready.data.trainerId)
    }

    fun deleteGoal(goal: SportGoal) = runAction("Tu objetivo ha sido eliminado.") {
        repository.deleteGoal(athleteId, goal)
    }

    fun updateGoal(goal: SportGoal, draft: GoalDraft) = runAction("Objetivo actualizado correctamente.") { ready ->
        repository.updateGoal(athleteId, goal, draft, ready.data.trainerId)
    }

    fun clearAction() { _action.value = GoalsActionState() }

    private fun runAction(success: String, block: suspend (GoalsScreenState.Ready) -> Unit) {
        val ready = _state.value as? GoalsScreenState.Ready ?: return
        if (_action.value.isWorking) return
        viewModelScope.launch {
            _action.value = GoalsActionState(isWorking = true)
            runCatching { block(ready) }
                .onSuccess {
                    _action.value = GoalsActionState(message = success)
                    runCatching { repository.load(athleteId) }.onSuccess { _state.value = GoalsScreenState.Ready(it) }
                }
                .onFailure { _action.value = GoalsActionState(message = it.userMessage(), isError = true) }
        }
    }
}

private fun Throwable.userMessage() = message?.takeIf(String::isNotBlank) ?: "No fue posible completar la operación."
