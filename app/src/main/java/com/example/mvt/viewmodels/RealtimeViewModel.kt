package com.example.mvt.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.data.firebase.services.RealtimeService
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

class RealtimeViewModel : ViewModel() {
    private val realtimeService = RealtimeService()

    private val _zonas = mutableStateOf<Map<String, Any>?>(null)
    val zonas: State<Map<String, Any>?> = _zonas

    private val _ritmos = mutableStateOf<Map<String, Any>?>(null)
    val ritmos: State<Map<String, Any>?> = _ritmos

    fun cargarDatos(idDeportista: String) {
        viewModelScope.launch {
            _zonas.value = realtimeService.getZonasDeportista(idDeportista)
            _ritmos.value = realtimeService.getRitmosDeportista(idDeportista)
        }
    }
}
