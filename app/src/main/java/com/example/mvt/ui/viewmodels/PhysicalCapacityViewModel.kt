package com.example.mvt.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.domain.repositories.MorphologyRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class PhysicalCapacityUiState {
    object Idle    : PhysicalCapacityUiState()
    object Loading : PhysicalCapacityUiState()
    object Saved   : PhysicalCapacityUiState()
    data class Error(val message: String) : PhysicalCapacityUiState()
}

class PhysicalCapacityViewModel : ViewModel() {

    private val repository = MorphologyRepository()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _fcMin = MutableStateFlow("")
    val fcMin: StateFlow<String> = _fcMin

    private val _fcMax = MutableStateFlow("")
    val fcMax: StateFlow<String> = _fcMax

    private val _uiState = MutableStateFlow<PhysicalCapacityUiState>(
        PhysicalCapacityUiState.Idle
    )
    val uiState: StateFlow<PhysicalCapacityUiState> = _uiState

    // === Cargar solo FCmin y FCmax ===
    fun loadPhysicalCapacity() {
        viewModelScope.launch {
            _uiState.value = PhysicalCapacityUiState.Loading
            try {
                val data = repository.getMorphology(uid)
                _fcMin.value = data?.FCmin ?: ""
                _fcMax.value = data?.FCmax ?: ""
                _uiState.value = PhysicalCapacityUiState.Idle
            } catch (e: Exception) {
                Log.e("PhysicalCapacityVM", "Error cargando", e)
                _uiState.value = PhysicalCapacityUiState.Error("Error al cargar los datos")
            }
        }
    }

    // === Guardar solo FCmin y FCmax ===
    fun savePhysicalCapacity(fcMin: String, fcMax: String) {
        viewModelScope.launch {
            try {
                // Carga el resto de datos para no pisar los otros campos
                val current = repository.getMorphology(uid)
                    ?: com.example.mvt.data.firebase.models.Morphology()

                repository.saveMorphology(
                    uid,
                    current.copy(
                        FCmin = fcMin,
                        FCmax = fcMax
                    )
                )
                _fcMin.value = fcMin
                _fcMax.value = fcMax
                _uiState.value = PhysicalCapacityUiState.Saved
            } catch (e: Exception) {
                Log.e("PhysicalCapacityVM", "Error guardando", e)
                _uiState.value = PhysicalCapacityUiState.Error("Error al guardar los datos")
            }
        }
    }

    fun resetState() {
        _uiState.value = PhysicalCapacityUiState.Idle
    }
}