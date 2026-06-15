package com.example.mvt.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.data.firebase.models.Routine
import com.example.mvt.domain.usecases.GetRoutinesByAthleteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth

class RoutineViewModel(
    private val getRoutinesByAthleteUseCase: GetRoutinesByAthleteUseCase
) : ViewModel() {
    private val routinesCache = mutableMapOf<String, List<Routine>>()

    private val _routines = MutableStateFlow<List<Routine>>(emptyList())
    val routines: StateFlow<List<Routine>> = _routines

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadRoutines(athleteId: String) {
        viewModelScope.launch {
            val data = getRoutinesByAthleteUseCase(athleteId)
            _routines.value = data
        }
    }

    suspend fun loadRoutinesForMonth(athleteId: String, month: YearMonth) {
        val cacheKey = "$athleteId-$month"
        routinesCache[cacheKey]?.let { cachedRoutines ->
            _routines.value = cachedRoutines
            return
        }

        _isLoading.value = true
        try {
            val data = getRoutinesByAthleteUseCase(athleteId, month)
            routinesCache[cacheKey] = data
            _routines.value = data
        } finally {
            _isLoading.value = false
        }
    }
}
