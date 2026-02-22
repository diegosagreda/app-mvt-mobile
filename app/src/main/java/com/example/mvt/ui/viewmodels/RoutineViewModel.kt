package com.example.mvt.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.data.firebase.models.Routine
import com.example.mvt.domain.usecases.GetRoutinesByAthleteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RoutineViewModel(
    private val getRoutinesByAthleteUseCase: GetRoutinesByAthleteUseCase
) : ViewModel() {

    private val _routines = MutableStateFlow<List<Routine>>(emptyList())
    val routines: StateFlow<List<Routine>> = _routines

    fun loadRoutines(athleteId: String) {
        viewModelScope.launch {
            val data = getRoutinesByAthleteUseCase(athleteId)
            _routines.value = data
        }
    }
}
