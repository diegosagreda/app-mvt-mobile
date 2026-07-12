package com.example.mvt.subscription.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.subscription.data.SubscriptionRepository
import com.example.mvt.subscription.model.PlanCatalogo
import com.example.mvt.subscription.model.SubscriptionStatus
import com.example.mvt.subscription.model.UserPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    // CARGA INICIAL
    // ==========================================
    fun loadSubscription() {
        viewModelScope.launch {
            _uiState.value = SubscriptionUiState.Loading
            try {
                // 1. Obtener plan del usuario
                val userPlan = repository.getUserPlan() ?: UserPlan()

                // 2. Obtener catálogo del plan activo
                val planCatalogo = repository.getPlanCatalogo(userPlan.nombre)
                    ?: PlanCatalogo()

                // 3. Obtener historial de cobros
                val cobros = repository.getCobros()

                // 4. Calcular estado
                _status.value = calcularEstado(userPlan, planCatalogo, cobros)
                _uiState.value = SubscriptionUiState.Success

                Log.d("SubscriptionVM", "Plan: ${userPlan.nombre}, Período: ${planCatalogo.periodoActualizacion}")

            } catch (e: Exception) {
                Log.e("SubscriptionVM", "Error cargando suscripción", e)
                _uiState.value = SubscriptionUiState.Error("Error al cargar la suscripción")
            }
        }
    }

    // ==========================================
    // CALCULAR ESTADO DE LA SUSCRIPCIÓN
    // ==========================================
    private fun calcularEstado(
        userPlan: UserPlan,
        planCatalogo: PlanCatalogo,
        cobros: List<com.example.mvt.subscription.model.Cobro>
    ): SubscriptionStatus {

        val esPlanGratuito   = planCatalogo.precio == 0
        val periodosDias     = planCatalogo.periodoActualizacion
        val fechaInicio      = if (userPlan.fechaRegistro > 0)
            formatTimestamp(userPlan.fechaRegistro) else "—"

        val fechaCorte: String
        val diasRestantes: Int
        val diasTotales: Int
        val progreso: Float

        if (periodosDias == 0 || esPlanGratuito) {
            // Plan sin vencimiento
            fechaCorte    = "No aplica"
            diasRestantes = -1
            diasTotales   = 0
            progreso      = 1f
        } else {
            // Plan con fecha de corte
            val msCorte   = userPlan.fechaRegistro + (periodosDias * 24L * 60 * 60 * 1000)
            val msAhora   = System.currentTimeMillis()
            val diasUsados = ((msAhora - userPlan.fechaRegistro) / (1000L * 60 * 60 * 24))
                .toInt().coerceAtLeast(0)

            fechaCorte    = formatTimestamp(msCorte)
            diasRestantes = maxOf(0, periodosDias - diasUsados)
            diasTotales   = periodosDias
            progreso      = (diasUsados.toFloat() / periodosDias.toFloat()).coerceIn(0f, 1f)
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

    // ==========================================
    // HELPERS
    // ==========================================
    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("d 'de' MMM 'de' yyyy", Locale("es", "CO"))
            sdf.format(Date(timestamp))
        } catch (e: Exception) { "—" }
    }
}