package com.example.mvt.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.data.firebase.models.DistanceOption
import com.example.mvt.data.firebase.models.PerformanceRecord
import com.example.mvt.data.firebase.models.PersonalRecord
import com.example.mvt.domain.repositories.MorphologyRepository
import com.example.mvt.domain.repositories.PerformanceRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// ==========================================
// ESTADOS UI
// ==========================================
sealed class PerformanceUiState {
    object Idle          : PerformanceUiState()
    object Loading       : PerformanceUiState()
    object Saved         : PerformanceUiState()
    object DeleteSuccess : PerformanceUiState()
    data class Error(val message: String) : PerformanceUiState()
}

data class FrequencyZone(val label: String, val min: Int, val max: Int)
data class RhythmZone(val label: String, val minPace: String, val maxPace: String = "")

// ==========================================
// VIEWMODEL
// ==========================================
class PerformanceViewModel : ViewModel() {

    private val performanceRepo = PerformanceRepository()
    private val morphologyRepo  = MorphologyRepository()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // === Estados ===
    private val _records         = MutableStateFlow<List<PerformanceRecord>>(emptyList())
    val records: StateFlow<List<PerformanceRecord>> = _records

    private val _uiState         = MutableStateFlow<PerformanceUiState>(PerformanceUiState.Idle)
    val uiState: StateFlow<PerformanceUiState> = _uiState

    private val _frequencyZones  = MutableStateFlow<List<FrequencyZone>>(emptyList())
    val frequencyZones: StateFlow<List<FrequencyZone>> = _frequencyZones

    private val _rhythmZones     = MutableStateFlow<List<RhythmZone>>(emptyList())
    val rhythmZones: StateFlow<List<RhythmZone>> = _rhythmZones

    private val _currentVam      = MutableStateFlow("")
    val currentVam: StateFlow<String> = _currentVam

    private val _personalRecords = MutableStateFlow<List<PersonalRecord>>(emptyList())
    val personalRecords: StateFlow<List<PersonalRecord>> = _personalRecords

    private val _isDeleting      = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting

    private val _distanceOptions = MutableStateFlow<List<DistanceOption>>(emptyList())
    val distanceOptions: StateFlow<List<DistanceOption>> = _distanceOptions

    // Test en tiempo real desde el input
    private val _currentTestValue = MutableStateFlow("")
    val currentTestValue: StateFlow<String> = _currentTestValue

    private val _currentMin = MutableStateFlow(6)
    val currentMin: StateFlow<Int> = _currentMin

    private val _currentSeg = MutableStateFlow("00")
    val currentSeg: StateFlow<String> = _currentSeg

    // ==========================================
    // CARGA INICIAL
    // ==========================================
    fun loadData() {
        viewModelScope.launch {
            _uiState.value = PerformanceUiState.Loading
            try {
                val records = performanceRepo.getRecords(uid)
                _records.value = records

                val marcas = performanceRepo.getPersonalRecords(uid)
                _personalRecords.value = marcas

                if (records.isNotEmpty()) {
                    val latest = records.first()
                    _currentVam.value = latest.VAM

                    // Usar semicooper del último registro para calcular zonas correctamente
                    val semicooperActual = if (_currentTestValue.value.isNotBlank())
                        _currentTestValue.value
                    else
                        latest.semicooper

                    _currentTestValue.value = semicooperActual

                    val distancia = semicooperActual.toDoubleOrNull() ?: latest.VAM_decimal
                    val vamKmH = if (distancia > 10) distancia / 100.0 else 60.0 / distancia

                    calcularZonas(vamKmH)
                }

                _uiState.value = PerformanceUiState.Idle
            } catch (e: Exception) {
                Log.e("PerformanceVM", "Error cargando datos", e)
                _uiState.value = PerformanceUiState.Error("Error al cargar los datos")
            }
        }
    }

    fun loadDistanceOptions() {
        viewModelScope.launch {
            try {
                _distanceOptions.value = performanceRepo.getDistanceOptions()
            } catch (e: Exception) {
                Log.e("PerformanceVM", "Error cargando distancias", e)
            }
        }
    }

    // ==========================================
    // ACTUALIZAR TEST EN TIEMPO REAL
    // FIX #3: recalcular zonas inmediatamente
    // ==========================================
    fun updateCurrentTestValue(value: String) {
        _currentTestValue.value = value
        val distancia = value.toDoubleOrNull()
        if (distancia != null && distancia > 0) {
            val vamKmH = distancia / 100.0
            val (minutos, segStr) = getPaceComponents(vamKmH)
            _currentVam.value = "$minutos:$segStr min/km"
            _currentMin.value = minutos
            _currentSeg.value = segStr
            viewModelScope.launch {
                calcularZonas(vamKmH)
            }
        }
    }

    // ==========================================
    // GUARDAR TEST
    // ==========================================
    fun saveRecord(semicooper: String) {
        viewModelScope.launch {
            try {
                val distancia  = semicooper.toDoubleOrNull() ?: return@launch
                val vamKmH     = distancia / 100.0
                val (minutos, segStr) = getPaceComponents(vamKmH)
                val vamPace    = "$minutos:$segStr min/km"
                val vo2max = ((distancia - 504.9) / 44.73).coerceAtLeast(0.0)
                val vo2maxStr = String.format(Locale.US, "%.2f", vo2max)

                val record = PerformanceRecord(
                    VAM         = vamPace,
                    VAM_decimal = 60.0 / vamKmH,
                    VO2Max      = vo2maxStr,
                    fecha       = System.currentTimeMillis(),
                    min         = minutos,
                    seg         = segStr,
                    semicooper  = semicooper
                )

                performanceRepo.saveRecord(uid, record)

                // Actualizar actVAM con el nuevo test
                performanceRepo.updateActVamTestData(uid, record)

                _currentMin.value = minutos
                _currentSeg.value = segStr

                val updated = performanceRepo.getRecords(uid)
                _records.value    = updated
                _currentVam.value = vamPace
                calcularZonas(vamKmH)

                _uiState.value = PerformanceUiState.Saved

            } catch (e: Exception) {
                Log.e("PerformanceVM", "Error guardando test", e)
                _uiState.value = PerformanceUiState.Error("Error al guardar el registro")
            }
        }
    }

    // ==========================================
    // GUARDAR MARCA PERSONAL
    // FIX #1 y #2: actualiza actVAM y crea snapshot en regVAM
    // ==========================================
    fun savePersonalRecord(
        distancia: String,
        fecha: String,
        tiempoH: String,
        tiempoM: String,
        tiempoS: String,
        ritmo: String,
        fcProm: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val currentRecords = performanceRepo.getPersonalRecords(uid)
                val nextIndex      = currentRecords.size

                // Convertir fecha a ISO
                val fechaIso = try {
                    val sdfIn  = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val sdfOut = SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US
                    ).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                    sdfOut.format(sdfIn.parse(fecha)!!)
                } catch (e: Exception) { fecha }

                val marcaData = mapOf(
                    "distancia" to distancia,
                    "fecha"     to fechaIso,
                    "tiempoH"   to tiempoH,
                    "tiempoM"   to tiempoM,
                    "tiempoS"   to tiempoS,
                    "ritmo"     to ritmo,
                    "fcProm"    to fcProm
                )

                // Guardar en actVAM
                performanceRepo.savePersonalRecord(uid, nextIndex, marcaData)

                // Recargar marcas con la nueva incluida
                val updatedRecords = performanceRepo.getPersonalRecords(uid)
                _personalRecords.value = updatedRecords

                // FIX #1: actualizar campos raíz de actVAM con test actual
                val testActual = buildRecordFromCurrentTest()
                if (testActual != null) {
                    performanceRepo.updateActVamTestData(uid, testActual)
                }

                // FIX #2: crear snapshot en regVAM y recargar historial
                createRegVamSnapshot(updatedRecords)
                val updatedHistory = performanceRepo.getRecords(uid)
                _records.value = updatedHistory

                onSuccess()

            } catch (e: Exception) {
                Log.e("PerformanceVM", "Error guardando marca", e)
                onError()
            }
        }
    }

    // ==========================================
    // ELIMINAR MARCA
    // ==========================================
    fun deletePersonalRecord(index: Int) {
        viewModelScope.launch {
            _isDeleting.value = true
            try {
                // FIX #5: el service ahora reindexa correctamente
                performanceRepo.deletePersonalRecord(uid, index)

                val updatedRecords = performanceRepo.getPersonalRecords(uid)
                _personalRecords.value = updatedRecords

                // FIX #1: actualizar actVAM con test actual
                val testActual = buildRecordFromCurrentTest()
                if (testActual != null) {
                    performanceRepo.updateActVamTestData(uid, testActual)
                }

                // Crear snapshot inmutable en regVAM
                createRegVamSnapshot(updatedRecords)

                // Recargar historial VAM
                val updatedHistory = performanceRepo.getRecords(uid)
                _records.value = updatedHistory

                _uiState.value = PerformanceUiState.DeleteSuccess

            } catch (e: Exception) {
                Log.e("PerformanceVM", "Error eliminando marca", e)
                _uiState.value = PerformanceUiState.Error("Error al eliminar la marca")
            } finally {
                _isDeleting.value = false
            }
        }
    }

    // ==========================================
    // HELPERS INTERNOS
    // ==========================================

    // Construye un PerformanceRecord con el test en tiempo real
    private fun buildRecordFromCurrentTest(): PerformanceRecord? {
        val testValue = _currentTestValue.value
        val distancia = testValue.toDoubleOrNull() ?: return null
        if (distancia <= 0) return null

        val vamKmH            = distancia / 100.0
        val (minutos, segStr) = getPaceComponents(vamKmH)
        val vamPace           = "$minutos:$segStr min/km"
        val vo2max = ((distancia - 504.9) / 44.73).coerceAtLeast(0.0)


        return PerformanceRecord(
            VAM         = vamPace,
            VAM_decimal = 60.0 / vamKmH,
            VO2Max      = String.format(Locale.US, "%.2f", vo2max),
            fecha       = System.currentTimeMillis(),
            min         = minutos,
            seg         = segStr,
            semicooper  = testValue
        )
    }

    // Crea snapshot inmutable en regVAM
    private suspend fun createRegVamSnapshot(marcas: List<PersonalRecord>) {
        try {
            val record = buildRecordFromCurrentTest() ?: return

            val marcasMap = marcas.mapIndexed { i, marca ->
                i.toString() to mapOf(
                    "distancia" to (marca.distancia ?: ""),
                    "fecha"     to (marca.fecha ?: ""),
                    "tiempoH"   to (marca.tiempoH?.toString() ?: ""),
                    "tiempoM"   to (marca.tiempoM?.toString() ?: ""),
                    "tiempoS"   to (marca.tiempoS?.toString() ?: ""),
                    "ritmo"     to (marca.ritmo ?: ""),
                    "fcProm"    to (marca.fcProm ?: "")
                )
            }.toMap()

            performanceRepo.saveRecordWithMarcas(uid, record, marcasMap)
            Log.d("PerformanceVM", "Snapshot creado con ${marcas.size} marcas")

        } catch (e: Exception) {
            Log.e("PerformanceVM", "Error creando snapshot", e)
        }
    }

    // ==========================================
    // CÁLCULO DE ZONAS
    // ==========================================
    private suspend fun calcularZonas(vamDecimal: Double) {
        try {
            val morphData = morphologyRepo.getMorphology(uid)
            val fcMin     = morphData?.FCmin?.toDoubleOrNull() ?: 60.0
            val fcMax     = morphData?.FCmax?.toDoubleOrNull() ?: 200.0
            val fcReserva = fcMax - fcMin

            val intensidadesFreq = listOf(
                Triple("Z0", 0.50, 0.63),
                Triple("Z1", 0.63, 0.70),
                Triple("Z2", 0.70, 0.77),
                Triple("Z3", 0.77, 0.83),
                Triple("Z4", 0.83, 0.90),
                Triple("Z5", 0.90, 1.00)
            )

            _frequencyZones.value = intensidadesFreq.map { (label, minInt, maxInt) ->
                FrequencyZone(
                    label = label,
                    min   = ((fcReserva * minInt) + fcMin).roundToInt(),
                    max   = ((fcReserva * maxInt) + fcMin).roundToInt()
                )
            }

            val intensidadesRitmo = listOf(
                Triple("R0",  0.486,  0.648),
                Triple("R1",  0.652,  0.748),
                Triple("R2",  0.753,  0.860),
                Triple("R3",  0.853,  0.951),
                Triple("R3+", 0.951,  1.050),
                Triple("R4",  1.056,  1.197),
                Triple("R5",  1.203,  1.399),
                Triple("R6",  1.408,  0.0)
            )

            _rhythmZones.value = if (vamDecimal <= 0) {
                intensidadesRitmo.map { (label, _, _) ->
                    RhythmZone(label = label, minPace = "", maxPace = "")
                }
            } else {
                intensidadesRitmo.map { (label, lento, rapido) ->
                    if (label == "R6") {
                        RhythmZone(
                            label   = label,
                            minPace = calcularPace(vamDecimal * lento),
                            maxPace = ""
                        )
                    } else {
                        RhythmZone(
                            label   = label,
                            minPace = calcularPace(vamDecimal * rapido),
                            maxPace = calcularPace(vamDecimal * lento)
                        )
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("PerformanceVM", "Error calculando zonas", e)
        }
    }

    // ==========================================
    // HELPERS PÚBLICOS
    // ==========================================
    fun calcularVamPreview(semicooper: String): String {
        val distancia = semicooper.toDoubleOrNull() ?: return ""
        if (distancia <= 0) return ""
        return calcularPace(distancia / 100.0)
    }

    fun updateRealtimeZones(semicooper: String) {
        val distancia = semicooper.toDoubleOrNull()
        if (distancia == null || distancia <= 0) {
            _rhythmZones.value = emptyList()
            _currentVam.value  = ""
            return
        }
        val vam = distancia / 100.0
        val (minutos, segStr) = getPaceComponents(vam)
        _currentVam.value = "$minutos:$segStr min/km"
        viewModelScope.launch { calcularZonas(vam) }
    }

    private fun getPaceComponents(kmH: Double): Pair<Int, String> {
        if (kmH <= 0) return Pair(0, "00")
        val minKm = 60.0 / kmH
        var minutos = minKm.toInt()
        var segundos = ((minKm - minutos) * 60).roundToInt()
        if (segundos == 60) {
            minutos += 1
            segundos = 0
        }
        return Pair(minutos, String.format(Locale.US, "%02d", segundos))
    }

    private fun calcularPace(kmH: Double): String {
        val (minutos, segStr) = getPaceComponents(kmH)
        if (minutos == 0 && segStr == "00") return "--:--"
        return "$minutos:$segStr min/km"
    }

    fun formatFecha(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    }

    fun resetState() {
        _uiState.value = PerformanceUiState.Idle
    }
}