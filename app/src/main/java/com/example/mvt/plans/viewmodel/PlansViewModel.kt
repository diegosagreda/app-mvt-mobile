package com.example.mvt.plans.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.plans.data.PlansRepository
import com.example.mvt.plans.model.PlanCatalogState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class PlansViewModel(
    private val repository: PlansRepository = PlansRepository()
) : ViewModel() {
    private val _state = MutableStateFlow<PlanCatalogState>(PlanCatalogState.Loading)
    val state: StateFlow<PlanCatalogState> = _state.asStateFlow()

    fun load(athleteId: String) {
        viewModelScope.launch {
            _state.value = PlanCatalogState.Loading
            runCatching {
                coroutineScope {
                    val catalog = async { repository.loadCatalog() }
                    val currentPlan = async { runCatching { repository.loadCurrentPlan(athleteId) }.getOrNull() }
                    catalog.await() to currentPlan.await()
                }
            }
                .onSuccess { (plans, currentPlan) ->
                    _state.value = if (plans.isEmpty()) PlanCatalogState.Empty
                    else PlanCatalogState.Ready(plans, currentPlan?.name)
                }
                .onFailure { error ->
                    _state.value = PlanCatalogState.Error(
                        error.message ?: "No fue posible consultar los planes."
                    )
                }
        }
    }
}
