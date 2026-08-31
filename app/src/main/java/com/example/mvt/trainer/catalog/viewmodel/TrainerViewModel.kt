package com.example.mvt.trainer.catalog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.trainer.catalog.data.TrainerRepository
import com.example.mvt.trainer.catalog.model.TrainerRatingsState
import com.example.mvt.trainer.catalog.model.TrainerScreenState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class RatingSubmissionState(
    val isSubmitting: Boolean = false,
    val completed: Boolean = false,
    val error: String? = null
)

class TrainerViewModel(
    private val repository: TrainerRepository = TrainerRepository()
) : ViewModel() {
    private val _screenState = MutableStateFlow<TrainerScreenState>(TrainerScreenState.Loading)
    val screenState: StateFlow<TrainerScreenState> = _screenState.asStateFlow()

    private val _ratingsState = MutableStateFlow(TrainerRatingsState())
    val ratingsState: StateFlow<TrainerRatingsState> = _ratingsState.asStateFlow()

    private val _submissionState = MutableStateFlow(RatingSubmissionState())
    val submissionState: StateFlow<RatingSubmissionState> = _submissionState.asStateFlow()

    private var trainerJob: Job? = null
    private var ratingsJob: Job? = null
    private var observedTrainerId = ""

    fun start(athleteId: String) {
        trainerJob?.cancel()
        ratingsJob?.cancel()
        observedTrainerId = ""
        _screenState.value = TrainerScreenState.Loading
        trainerJob = viewModelScope.launch {
            repository.observeTrainer(athleteId)
                .catch { _screenState.value = TrainerScreenState.Error(it.userMessage()) }
                .collect { state ->
                    _screenState.value = state
                    val assigned = state as? TrainerScreenState.Assigned
                    val nextTrainerId = assigned?.trainer?.id.orEmpty()
                    if (nextTrainerId != observedTrainerId) {
                        observedTrainerId = nextTrainerId
                        ratingsJob?.cancel()
                        _ratingsState.value = TrainerRatingsState()
                        if (assigned != null) observeRatings(assigned.trainer.id, assigned.athlete.id)
                    }
                }
        }
    }

    fun submitRating(score: Int, comment: String) {
        val state = _screenState.value as? TrainerScreenState.Assigned ?: return
        if (_submissionState.value.isSubmitting) return
        viewModelScope.launch {
            _submissionState.value = RatingSubmissionState(isSubmitting = true)
            runCatching {
                repository.submitRating(state.assignment, state.trainer, state.athlete, score, comment)
            }.onSuccess {
                _submissionState.value = RatingSubmissionState(completed = true)
            }.onFailure {
                _submissionState.value = RatingSubmissionState(error = it.userMessage())
            }
        }
    }

    fun clearSubmissionResult() {
        _submissionState.value = RatingSubmissionState()
    }

    private fun observeRatings(trainerId: String, athleteId: String) {
        ratingsJob = viewModelScope.launch {
            repository.observeRatings(trainerId, athleteId)
                .catch { _ratingsState.value = TrainerRatingsState() }
                .collect { _ratingsState.value = it }
        }
    }
}

private fun Throwable.userMessage(): String = message
    ?.takeIf { it.isNotBlank() }
    ?: "Ocurrió un error inesperado. Inténtalo nuevamente."

