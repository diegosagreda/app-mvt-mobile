package com.example.mvt.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.chat.data.model.TrainerPersonalData
import com.example.mvt.chat.data.repo.TrainerRealtimeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TrainerBubbleViewModel(
    private val repo: TrainerRealtimeRepository
) : ViewModel() {

    private val _trainer = MutableStateFlow(TrainerPersonalData())
    val trainer: StateFlow<TrainerPersonalData> = _trainer.asStateFlow()

    private val _trainerId = MutableStateFlow("")
    val trainerId: StateFlow<String> = _trainerId.asStateFlow()

    private var startJob: Job? = null

    fun start(athleteId: String) {
        // Evita dejar listeners/flows duplicados si se llama start() varias veces
        startJob?.cancel()

        startJob = repo.observeTrainerIdForAthlete(athleteId)
            .map { it.trim() }
            .distinctUntilChanged()
            .onEach { id ->
                _trainerId.value = id  // <-- ESTO ES LO QUE FALTABA

                if (id.isBlank()) {
                    _trainer.value = TrainerPersonalData()
                } else {
                    // Si getTrainerPersonalData es suspend, esto está bien dentro de onEach
                    _trainer.value = repo.getTrainerPersonalData(id)
                }
            }
            .catch { e ->
                _trainerId.value = ""
                _trainer.value = TrainerPersonalData()
            }
            .launchIn(viewModelScope)
    }
}