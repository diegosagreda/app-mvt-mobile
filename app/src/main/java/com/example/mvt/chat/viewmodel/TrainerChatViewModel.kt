package com.example.mvt.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.chat.data.model.ConversationPreview
import com.example.mvt.chat.data.repo.ChatRepository
import com.example.mvt.trainer.athletes.data.ApprovedAthletesRepository
import com.example.mvt.trainer.athletes.model.ApprovedAthlete
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Normalizer

sealed interface TrainerChatUiState {
    data object Loading : TrainerChatUiState
    data class Success(
        val athletes: List<ApprovedAthlete>,
        val previews: Map<String, ConversationPreview>,
        val search: String = ""
    ) : TrainerChatUiState
    data class Error(val message: String) : TrainerChatUiState
}

class TrainerChatViewModel(
    private val athletesRepository: ApprovedAthletesRepository = ApprovedAthletesRepository(),
    private val chatRepository: ChatRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<TrainerChatUiState>(TrainerChatUiState.Loading)
    val uiState: StateFlow<TrainerChatUiState> = _uiState.asStateFlow()

    private var previewsListener: ListenerRegistration? = null
    private var trainerId: String = ""
    private var athletes: List<ApprovedAthlete> = emptyList()

    fun load(trainerId: String) {
        if (this.trainerId == trainerId && athletes.isNotEmpty()) return
        this.trainerId = trainerId
        _uiState.value = TrainerChatUiState.Loading

        viewModelScope.launch {
            athletesRepository.fetchApprovedAthletes()
                .onSuccess { loadedAthletes ->
                    athletes = loadedAthletes
                    _uiState.value = TrainerChatUiState.Success(loadedAthletes, emptyMap())
                    observePreviews()
                }
                .onFailure {
                    _uiState.value = TrainerChatUiState.Error(
                        it.localizedMessage ?: "No fue posible cargar los deportistas."
                    )
                }
        }
    }

    fun setSearch(search: String) {
        val current = _uiState.value as? TrainerChatUiState.Success ?: return
        _uiState.value = current.copy(search = search)
    }

    private fun observePreviews() {
        previewsListener?.remove()
        val athleteIds = athletes.map { it.id }.toSet()
        if (trainerId.isBlank() || athleteIds.isEmpty()) return

        previewsListener = chatRepository.listenTrainerConversationPreviews(
            trainerId = trainerId,
            athleteIds = athleteIds,
            onChange = { previews ->
                val current = _uiState.value as? TrainerChatUiState.Success ?: return@listenTrainerConversationPreviews
                _uiState.value = current.copy(previews = previews.associateBy { it.athleteId })
            },
            onError = { error ->
                val current = _uiState.value as? TrainerChatUiState.Success
                if (current != null) {
                    _uiState.value = TrainerChatUiState.Error(
                        error.localizedMessage ?: "No fue posible cargar las conversaciones."
                    )
                }
            }
        )
    }

    fun filteredAthletes(state: TrainerChatUiState.Success): List<ApprovedAthlete> {
        val query = normalize(state.search)
        return state.athletes
            .filter { query.isBlank() || normalize(it.name).contains(query) }
            .sortedWith(
                compareByDescending<ApprovedAthlete> {
                    state.previews[it.id]?.lastMessage?.timestamp.orEmpty()
                }.thenBy { it.name.lowercase() }
            )
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .trim()
            .lowercase()
    }

    override fun onCleared() {
        previewsListener?.remove()
        super.onCleared()
    }
}
