package com.example.mvt.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.data.firebase.models.PerformanceRecord
import com.example.mvt.domain.repositories.MorphologyRepository
import com.example.mvt.domain.repositories.PerformanceRepository
import com.example.mvt.domain.repositories.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// === Estados UI ===
sealed class PerformanceUiState {
    object Idle    : PerformanceUiState()
    object Loading : PerformanceUiState()
    object Saved   : PerformanceUiState()
    data class Error(val message: String) : PerformanceUiState()
}

// === Modelo para zonas calculadas ===
data class FrequencyZone(
    val label: String,
    val min: Int,
    val max: Int
)

data class RhythmZone(
    val label: String,
    val minPace: String,
    val maxPace: String = ""
)

class PerformanceViewModel : ViewModel() {

    private val performanceRepo  = PerformanceRepository()
    private val morphologyRepo   = MorphologyRepository()
    private val userRepo         = UserRepository()

    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // === Estado registros ===
    private val _records = MutableStateFlow<List<PerformanceRecord>>(emptyList())
    val records: StateFlow<List<PerformanceRecord>> = _records

    // === Estado UI ===
    private val _uiState = MutableStateFlow<PerformanceUiState>(PerformanceUiState.Idle)
    val uiState: StateFlow<PerformanceUiState> = _uiState

    // === Zonas calculadas ===
    private val _frequencyZones = MutableStateFlow<List<FrequencyZone>>(emptyList())
    val frequencyZones: StateFlow<List<FrequencyZone>> = _frequencyZones

    private val _rhythmZones = MutableStateFlow<List<RhythmZone>>(emptyList())
    val rhythmZones: StateFlow<List<RhythmZone>> = _rhythmZones

    // === VAM actual (del último registro) ===
    private val _currentVam = MutableStateFlow("")
    val currentVam: StateFlow<String> = _currentVam

    // ==========================================
    // CARGA INICIAL
    // ==========================================
    fun loadData() {
        viewModelScope.launch {
            _uiState.value = PerformanceUiState.Loading
            try {
                val records = performanceRepo.getRecords(uid)
                _records.value = records

                // Si hay registros, mostrar el último VAM y calcular zonas
                if (records.isNotEmpty()) {
                    val latest = records.first()
                    _currentVam.value = latest.VAM
                    calcularZonas(latest.VAM_decimal)
                }

                _uiState.value = PerformanceUiState.Idle
            } catch (e: Exception) {
                Log.e("PerformanceVM", "Error cargando datos", e)
                _uiState.value = PerformanceUiState.Error("Error al cargar los datos")
            }
        }
    }

    // ==========================================
    // GUARDAR NUEVO REGISTRO
    // ==========================================
    fun saveRecord(semicooper: String, minutos: Int, segundos: Int) {
        viewModelScope.launch {
            try {
                val distancia = semicooper.toDoubleOrNull() ?: return@launch

                // === Cálculo VAM ===
                // Test = correr 6 minutos → VAM = distancia/100 en km/h
                val vamKmH     = distancia / 100.0
                val vamDecimal = vamKmH
                val vamPace    = calcularPace(vamKmH)

                // === Cálculo VO2max ===
                // Fórmula: (distancia - 504.9) / 44.73
                val vo2max = ((distancia - 504.9) / 44.73)
                    .coerceAtLeast(0.0)
                val vo2maxStr = String.format("%.2f", vo2max)

                val record = PerformanceRecord(
                    VAM         = vamPace,
                    VAM_decimal = vamDecimal,
                    VO2max      = vo2maxStr,
                    fecha       = System.currentTimeMillis(),
                    min         = minutos,
                    seg         = String.format("%02d", segundos),
                    semicooper  = semicooper
                )

                performanceRepo.saveRecord(uid, record)

                // Recargar lista y recalcular zonas
                val updated = performanceRepo.getRecords(uid)
                _records.value  = updated
                _currentVam.value = vamPace
                calcularZonas(vamDecimal)

                _uiState.value = PerformanceUiState.Saved

            } catch (e: Exception) {
                Log.e("PerformanceVM", "Error guardando registro", e)
                _uiState.value = PerformanceUiState.Error("Error al guardar el registro")
            }
        }
    }

    // ==========================================
    // CÁLCULO DE ZONAS
    // ==========================================
    private suspend fun calcularZonas(vamDecimal: Double) {
        try {
            // Obtener FCmin y FCmax de Capacidad Física
            val morphData = morphologyRepo.getMorphology(uid)
            val fcMin = morphData?.FCmin?.toDoubleOrNull() ?: 60.0
            val fcMax = morphData?.FCmax?.toDoubleOrNull() ?: 200.0

            // === Zonas de frecuencia con Karvonen ===
            // FCreserva = FCmax - FCmin
            val fcReserva = fcMax - fcMin

            // Intensidades por zona
            val intensidades = listOf(
                Triple("Z0", 0.50, 0.63),
                Triple("Z1", 0.63, 0.70),
                Triple("Z2", 0.70, 0.77),
                Triple("Z3", 0.77, 0.83),
                Triple("Z4", 0.83, 0.90),
                Triple("Z5", 0.90, 1.00)
            )

            _frequencyZones.value = intensidades.map { (label, minInt, maxInt) ->
                FrequencyZone(
                    label = label,
                    min   = ((fcReserva * minInt) + fcMin).roundToInt(),
                    max   = ((fcReserva * maxInt) + fcMin).roundToInt()
                )
            }

            // === Zonas de ritmo basadas en VAM_decimal ===
            // velocidad_zona = VAM × porcentaje
            // ritmo = 60 / velocidad → formato mm:ss
            val intensidadesRitmo = listOf(
                Triple("R0", 0.50, 0.60),
                Triple("R1", 0.60, 0.70),
                Triple("R2", 0.70, 0.80),
                Triple("R3", 0.80, 0.85),
                Triple("R3+", 0.85, 0.90),
                Triple("R4", 0.90, 0.95),
                Triple("R5", 0.95, 1.00),
                Triple("R6", 1.00, 0.0)   // solo límite superior
            )

            _rhythmZones.value = intensidadesRitmo.map { (label, minPct, maxPct) ->
                val velMin  = vamDecimal * minPct
                val paceMin = calcularPace(velMin)
                val paceMax = if (maxPct > 0.0) {
                    calcularPace(vamDecimal * maxPct)
                } else ""

                RhythmZone(
                    label    = label,
                    minPace  = paceMin,
                    maxPace  = paceMax
                )
            }

        } catch (e: Exception) {
            Log.e("PerformanceVM", "Error calculando zonas", e)
        }
    }

    // ==========================================
    // HELPERS
    // ==========================================

    // Convierte km/h a formato mm:ss min/km
    private fun calcularPace(kmH: Double): String {
        if (kmH <= 0) return "--:--"
        val minKm      = 60.0 / kmH
        val minutos    = minKm.toInt()
        val segundos   = ((minKm - minutos) * 60).roundToInt()
        return if (segundos == 60)
            "${minutos + 1}:00 min/km"
        else
            "$minutos:${String.format("%02d", segundos)} min/km"
    }

    // Formatea timestamp a fecha legible
    fun formatFecha(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun resetState() {
        _uiState.value = PerformanceUiState.Idle
    }

    // === Calcular VAM en tiempo real desde el input ===
    fun calcularVamPreview(semicooper: String): String {
        val distancia = semicooper.toDoubleOrNull() ?: return ""
        if (distancia <= 0) return ""
        val vamKmH = distancia / 100.0
        return calcularPace(vamKmH)
    }
}


