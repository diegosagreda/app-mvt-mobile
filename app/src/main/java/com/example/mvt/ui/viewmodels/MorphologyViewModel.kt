package com.example.mvt.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.data.firebase.models.Morphology
import com.example.mvt.domain.repositories.MorphologyRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class MorphologyUiState {
    object Loading : MorphologyUiState()
    object Saved   : MorphologyUiState()
    object Idle    : MorphologyUiState()
    data class Error(val message: String) : MorphologyUiState()
}

class MorphologyViewModel : ViewModel() {

    private val repository = MorphologyRepository()

    private val _morphology = MutableStateFlow<Morphology?>(null)
    val morphology: StateFlow<Morphology?> = _morphology

    private val _uiState = MutableStateFlow<MorphologyUiState>(MorphologyUiState.Idle)
    val uiState: StateFlow<MorphologyUiState> = _uiState

    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    fun loadMorphology() {
        viewModelScope.launch {
            _uiState.value = MorphologyUiState.Loading
            try {
                _morphology.value = repository.getMorphology(uid)
                _uiState.value = MorphologyUiState.Idle
            } catch (e: Exception) {
                Log.e("MorphologyViewModel", "Error cargando", e)
                _uiState.value = MorphologyUiState.Error("Error al cargar los datos")
            }
        }
    }

    fun saveMorphology(morphology: Morphology) {
        viewModelScope.launch {
            try {
                // Carga los datos actuales para preservar FCmin y FCmax
                val current = repository.getMorphology(uid) ?: Morphology()

                val toSave = morphology.copy(
                    FCmin = current.FCmin,
                    FCmax = current.FCmax
                )
                repository.saveMorphology(uid, toSave)
                _morphology.value  = toSave
                _uiState.value     = MorphologyUiState.Saved
            } catch (e: Exception) {
                Log.e("MorphologyViewModel", "Error guardando", e)
                _uiState.value = MorphologyUiState.Error("Error al guardar los datos")
            }
        }
    }

    fun resetState() {
        _uiState.value = MorphologyUiState.Idle
    }
}