package com.example.mvt.subscription.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.subscription.data.SubscriptionRepository
import com.example.mvt.subscription.model.Cobro
import com.example.mvt.subscription.model.PlanCatalogo
import com.example.mvt.subscription.model.SubscriptionStatus
import com.example.mvt.subscription.model.UserPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class SubscriptionUiState {
    object Loading : SubscriptionUiState()
    object Success : SubscriptionUiState()
    data class Error(val message: String) : SubscriptionUiState()
}

class SubscriptionViewModel : ViewModel() {

    private val repository = SubscriptionRepository()

    private val _uiState = MutableStateFlow<SubscriptionUiState>(SubscriptionUiState.Loading)
    val uiState: StateFlow<SubscriptionUiState> = _uiState

    private val _status = MutableStateFlow(SubscriptionStatus())
    val status: StateFlow<SubscriptionStatus> = _status

    // ==========================================
    // CARGA INICIAL Y OBSERVACIÓN EN TIEMPO REAL
    // ==========================================
    fun loadSubscription() {
        viewModelScope.launch {
            _uiState.value = SubscriptionUiState.Loading
            repository.getSubscriptionStream()
                .catch { e ->
                    Log.e("SubscriptionVM", "Error en stream suscripción", e)
                    _uiState.value = SubscriptionUiState.Error("Error al cargar la suscripción")
                }
                .collect { (userPlan, planCatalogo, cobros) ->
                    _status.value = calcularEstado(userPlan, planCatalogo, cobros)
                    _uiState.value = SubscriptionUiState.Success
                }
        }
    }

    private fun calcularEstado(
        userPlan: UserPlan,
        planCatalogo: PlanCatalogo,
        cobros: List<Cobro>
    ): SubscriptionStatus {

        val esPlanGratuito = userPlan.nombre.equals("Bronce", ignoreCase = true) || planCatalogo.precio == 0
        val periodosDias   = if (esPlanGratuito) 0 else 30

        val cobroMasReciente = cobros.firstOrNull()
        val fechaInicioMs = when {
            !esPlanGratuito && cobroMasReciente != null -> cobroMasReciente.actualizadoEn
            else -> userPlan.fechaRegistro
        }

        val fechaInicio = if (fechaInicioMs > 0) formatTimestamp(fechaInicioMs) else "—"
        val fechaCorte: String
        val diasRestantes: Int
        val diasTotales: Int
        val progreso: Float

        if (esPlanGratuito) {
            fechaCorte    = "No aplica"
            diasRestantes = -1
            diasTotales   = 0
            progreso      = 1f
        } else {
            val msCorte    = fechaInicioMs + (periodosDias * 24L * 60 * 60 * 1000)
            val msAhora    = System.currentTimeMillis()
            val msPasados  = msAhora - fechaInicioMs
            
            val usados     = (msPasados / (1000L * 60 * 60 * 24)).toInt().coerceIn(0, periodosDias)
            val restantes  = periodosDias - usados

            fechaCorte    = formatTimestamp(msCorte)
            diasRestantes = restantes
            diasTotales   = periodosDias
            progreso      = (msPasados.toFloat() / (periodosDias * 24L * 60 * 60 * 1000).toFloat()).coerceIn(0f, 1f)
        }

        return SubscriptionStatus(
            userPlan       = userPlan,
            planCatalogo   = planCatalogo,
            cobros         = cobros,
            esPlanGratuito = esPlanGratuito,
            fechaInicio    = fechaInicio,
            fechaCorte     = fechaCorte,
            diasRestantes  = diasRestantes,
            diasTotales    = diasTotales,
            progresoPlan   = progreso
        )
    }

    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("d 'de' MMM 'de' yyyy", Locale("es", "CO"))
            sdf.format(Date(timestamp))
        } catch (e: Exception) { "—" }
    }
}
