package com.example.mvt.subscription.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.plans.data.PlansRepository
import com.example.mvt.plans.model.SubscriptionScreenState
import com.example.mvt.plans.model.subscriptionSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SubscriptionViewModel(
    private val repository: PlansRepository = PlansRepository()
) : ViewModel() {
    private val _state = MutableStateFlow<SubscriptionScreenState>(SubscriptionScreenState.Loading)
    val state: StateFlow<SubscriptionScreenState> = _state.asStateFlow()

    fun load(athleteId: String) {
        viewModelScope.launch {
            _state.value = SubscriptionScreenState.Loading
            runCatching { repository.loadCurrentPlan(athleteId) }
                .onSuccess { plan ->
                    val summary = subscriptionSummary(plan)
                    _state.value = if (summary == null) SubscriptionScreenState.Empty
                    else SubscriptionScreenState.Ready(summary)
                }
                .onFailure { error ->
                    _state.value = SubscriptionScreenState.Error(
                        error.message ?: "No fue posible consultar tu suscripción."
                    )
                }
        }
    }
}
