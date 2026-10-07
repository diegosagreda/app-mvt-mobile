package com.example.mvt.trainer.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class TrainerAthleteCalendarInfo(
    val name: String,
    val photoUrl: String?,
    val sport: String,
    val trainingDays: Map<String, Boolean>
)

sealed interface TrainerAthleteCalendarUiState {
    data object Loading : TrainerAthleteCalendarUiState
    data class Success(val info: TrainerAthleteCalendarInfo) : TrainerAthleteCalendarUiState
    data class Error(val message: String) : TrainerAthleteCalendarUiState
}

class TrainerAthleteCalendarViewModel(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) : ViewModel() {
    private val _uiState = MutableStateFlow<TrainerAthleteCalendarUiState>(
        TrainerAthleteCalendarUiState.Loading
    )
    val uiState: StateFlow<TrainerAthleteCalendarUiState> = _uiState.asStateFlow()

    fun load(athleteId: String) {
        if (athleteId.isBlank()) {
            _uiState.value = TrainerAthleteCalendarUiState.Error("No se encontró el deportista.")
            return
        }
        viewModelScope.launch {
            _uiState.value = TrainerAthleteCalendarUiState.Loading
            runCatching {
                val user = database.getReference("users").child(athleteId).get().await()
                val objectives = database.getReference("Objetivos").child(athleteId)
                    .child("dias_entrenamiento").get().await()
                val firstName = user.child("nombres").value?.toString().orEmpty()
                val lastName = user.child("apellidos").value?.toString().orEmpty()
                TrainerAthleteCalendarInfo(
                    name = "$firstName $lastName".trim().ifBlank { "Atleta" },
                    photoUrl = user.child("foto_url").value?.toString()
                        ?.takeIf { it.isNotBlank() },
                    sport = user.child("deporte").value?.toString()
                        ?.takeIf { it.isNotBlank() } ?: "Multideporte",
                    trainingDays = objectives.children.associate {
                        it.key.orEmpty() to when (val value = it.value) {
                            is Boolean -> value
                            is Number -> value.toInt() != 0
                            is String -> value.equals("true", ignoreCase = true) || value == "1"
                            else -> false
                        }
                    }
                )
            }.onSuccess { _uiState.value = TrainerAthleteCalendarUiState.Success(it) }
                .onFailure {
                    _uiState.value = TrainerAthleteCalendarUiState.Error(
                        it.localizedMessage ?: "No fue posible cargar el calendario."
                    )
                }
        }
    }
}
