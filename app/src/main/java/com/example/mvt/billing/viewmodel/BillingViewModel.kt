package com.example.mvt.billing.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.billing.data.BillingRepository
import com.example.mvt.billing.model.BillingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class BillingUiState {
    object Loading : BillingUiState()
    object Success : BillingUiState()
    data class Error(val message: String) : BillingUiState()
}

class BillingViewModel : ViewModel() {

    private val repository = BillingRepository()

    private val _uiState = MutableStateFlow<BillingUiState>(BillingUiState.Loading)
    val uiState: StateFlow<BillingUiState> = _uiState

    private val _status = MutableStateFlow(BillingStatus())
    val status: StateFlow<BillingStatus> = _status

    // ==========================================
    // CARGA Y OBSERVACIÓN EN TIEMPO REAL
    // ==========================================
    fun loadBilling() {
        viewModelScope.launch {
            _uiState.value = BillingUiState.Loading
            repository.getBillingStream()
                .catch { e ->
                    Log.e("BillingVM", "Error en stream facturación", e)
                    _uiState.value = BillingUiState.Error("Error al cargar facturación")
                }
                .collect { billingStatus ->
                    _status.value = billingStatus
                    _uiState.value = BillingUiState.Success
                }
        }
    }
}
