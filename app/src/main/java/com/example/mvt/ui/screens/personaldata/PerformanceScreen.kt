package com.example.mvt.ui.screens.personaldata

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mvt.data.firebase.models.PerformanceRecord
import com.example.mvt.ui.components.FormErrorNotification
import com.example.mvt.ui.components.FormSuccessNotification
import com.example.mvt.ui.components.FormTooltip
import com.example.mvt.ui.components.formFieldColors
import com.example.mvt.ui.theme.PrimaryBlue
import com.example.mvt.ui.viewmodels.FrequencyZone
import com.example.mvt.ui.viewmodels.PerformanceUiState
import com.example.mvt.ui.viewmodels.PerformanceViewModel
import com.example.mvt.ui.viewmodels.RhythmZone

// Color más oscuro para los labels de zonas igual que en la web
private val ZoneLabelColor = Color(0xFF1A3A5C)

@Composable
fun PerformanceScreen(
    navController: NavController,
    viewModel: PerformanceViewModel
) {
    val records        by viewModel.records.collectAsState()
    val uiState        by viewModel.uiState.collectAsState()
    val currentVam     by viewModel.currentVam.collectAsState()
    val frequencyZones by viewModel.frequencyZones.collectAsState()
    val rhythmZones    by viewModel.rhythmZones.collectAsState()

    var testValue   by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    var showError   by remember { mutableStateOf(false) }
    var testError   by remember { mutableStateOf(false) }

    // VAM calculado en tiempo real
    val vamPreview = remember(testValue) {
        viewModel.calcularVamPreview(testValue)
    }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    LaunchedEffect(records) {
        if (records.isNotEmpty() && testValue.isEmpty()) {
            testValue = records.first().semicooper
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is PerformanceUiState.Saved -> { showSuccess = true; viewModel.resetState() }
            is PerformanceUiState.Error -> { showError = true;   viewModel.resetState() }
            else -> {}
        }
    }

    val isLoading   = uiState is PerformanceUiState.Loading
    val scrollState = rememberScrollState()

    // Siempre 6 zonas de frecuencia y 8 de ritmo aunque estén vacías
    val freqZonasDisplay = if (frequencyZones.isNotEmpty()) frequencyZones
    else List(6) { i -> FrequencyZone("Z$i", 0, 0) }

    val rhythmZonasDisplay = if (rhythmZones.isNotEmpty()) rhythmZones
    else listOf(
        RhythmZone("R0", "", ""),
        RhythmZone("R1", "", ""),
        RhythmZone("R2", "", ""),
        RhythmZone("R3", "", ""),
        RhythmZone("R3+", "", ""),
        RhythmZone("R4", "", ""),
        RhythmZone("R5", "", ""),
        RhythmZone("R6", "", "")
    )

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
        return
    }

    fun guardar() {
        val dist = testValue.toDoubleOrNull()
        testError = dist == null || dist <= 0
        if (testError) {
            showError = true
            return
        }
        viewModel.saveRecord(testValue, 0, 0)
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = 80.dp)
        ) {

            // --- Header ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        navController.navigate("routines") {
                            popUpTo("routines") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = PrimaryBlue
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Información Rendimiento",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Test Semicooper:",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF555B61)
            )
            Text(
                text = "Calienta 10 minutos y estira, luego corre la mayor " +
                        "distancia posible durante 6 minutos, apunta la distancia " +
                        "recorrida en metros.",
                fontSize = 13.sp,
                color = Color(0xFF888888)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = buildAnnotatedString {
                    append("Obligatorio ")
                    withStyle(SpanStyle(color = Color.Red)) { append("*") }
                },
                fontSize = 13.sp,
                color = Color(0xFF555B61)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // VAM — solo lectura, calculado en tiempo real
            // ==========================================
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = buildAnnotatedString {
                        append("VAM ")
                        withStyle(SpanStyle(color = Color.Red)) { append("*") }
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF555B61)
                )
                Spacer(modifier = Modifier.width(6.dp))
                FormTooltip(
                    "Es la velocidad máxima a la que puedes correr cuando tu " +
                            "consumo de oxígeno está al máximo nivel. Este valor refleja " +
                            "qué tan bueno eres corriendo, si aún no has realizado el test, " +
                            "esta es una estimación basada en tu perfil."
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = if (vamPreview.isNotBlank()) vamPreview
                else if (currentVam.isNotBlank()) currentVam
                else "",
                onValueChange = {},
                enabled = false,
                placeholder = { Text("Se calcula automáticamente") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF888888)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor      = Color(0xFFCCCCCC),
                    disabledContainerColor   = Color(0xFFF5F5F5),
                    disabledTextColor        = Color(0xFF555B61),
                    disabledPlaceholderColor = Color(0xFF888888),
                    disabledLeadingIconColor = Color(0xFF888888)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // TEST — distancia con flechitas y tooltip
            // ==========================================
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = buildAnnotatedString {
                        append("Test ")
                        withStyle(SpanStyle(color = Color.Red)) { append("*") }
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF555B61)
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Tooltip del campo Test
                FormTooltip("Pruebas para conocer tu rendimiento deportivo.")
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = testValue,
                onValueChange = {
                    testValue = it.filter { c -> c.isDigit() }
                    // Validar en tiempo real cuando queda vacío
                    testError = testValue.isBlank()
                },
                isError = testError,
                placeholder = { Text("Distancia en metros") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF888888)
                    )
                },
                // Flechitas dentro del input igual que morphology
                trailingIcon = {
                    Column(
                        modifier = Modifier.padding(end = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Subir",
                            tint = PrimaryBlue,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    val actual = testValue.toIntOrNull() ?: 0
                                    testValue  = (actual + 1).toString()
                                    testError  = false
                                }
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Bajar",
                            tint = PrimaryBlue,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    val actual = testValue.toIntOrNull() ?: 0
                                    val nuevo  = if (actual > 0) actual - 1 else 0
                                    testValue  = nuevo.toString()
                                    testError  = testValue == "0" || testValue.isBlank()
                                }
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = formFieldColors()
            )
            // Mensaje de error cuando queda vacío
            if (testError) {
                Text(
                    text = "*Escribe una distancia valida.",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // HISTORIAL DE VAM
            // ==========================================
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Historial de VAM",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2B2E34)
                )
                Spacer(modifier = Modifier.width(6.dp))
                FormTooltip(
                    "Cuando realices el test de Semicooper más de una vez, " +
                            "aquí se mostrará tu progreso para que puedas mejorar."
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (records.isEmpty()) {
                Text(
                    text = "Aún no tienes registros. ¡Realiza tu primer test!",
                    fontSize = 13.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                records.forEachIndexed { index, record ->
                    HistorialItem(
                        record      = record,
                        isLatest    = index == 0,
                        isAlternate = index % 2 == 0,
                        formatFecha = { viewModel.formatFecha(it) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // ZONAS DE FRECUENCIA — siempre visibles
            // ==========================================
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Zonas de frecuencia (ppm)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2B2E34)
                )
                Spacer(modifier = Modifier.width(6.dp))
                FormTooltip(
                    "Pulsaciones por minuto representan rangos utilizados para " +
                            "medir la intensidad del ejercicio, basados en tu frecuencia cardíaca."
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            freqZonasDisplay.forEach { zone ->
                ZonaFrecuenciaItem(zone = zone)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // ZONAS DE RITMO — siempre visibles
            // ==========================================
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Zona de ritmos (min/km)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2B2E34)
                )
                Spacer(modifier = Modifier.width(6.dp))
                FormTooltip(
                    "Son rangos para medir la intensidad del ejercicio, " +
                            "basados en la velocidad que debes llevar."
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            rhythmZonasDisplay.forEach { zone ->
                ZonaRitmoItem(zone = zone)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // === Botón Actualizar fijo ===
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color           = Color.White,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { guardar() },
                    modifier = Modifier
                        .height(50.dp)
                        .width(180.dp),
                    shape  = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(
                        text       = "Actualizar",
                        color      = Color.White,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (showSuccess) {
            FormSuccessNotification(
                message   = "¡Test registrado con éxito!",
                onDismiss = { showSuccess = false }
            )
        }
        if (showError) {
            FormErrorNotification(
                message   = "Por favor completa los campos correctamente",
                onDismiss = { showError = false }
            )
        }
    }
}

// ==========================================
// COMPONENTES PRIVADOS
// ==========================================

@Composable
private fun HistorialItem(
    record: PerformanceRecord,
    isLatest: Boolean,
    isAlternate: Boolean,
    formatFecha: (Long) -> String
) {
    val bgColor    = when {
        isLatest    -> PrimaryBlue
        isAlternate -> Color(0xFFF5F5F5)
        else        -> Color.White
    }
    val textColor  = if (isLatest) Color.White else Color(0xFF2B2E34)
    val labelColor = if (isLatest) Color.White.copy(alpha = 0.8f) else Color(0xFF888888)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(8.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("VAM",  fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = labelColor)
                Text(record.VAM, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
            }
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Test", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = labelColor)
                Text(record.semicooper, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
            }
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Fecha", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = labelColor)
                Text(formatFecha(record.fecha), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
            }
        }
    }
}

@Composable
private fun ZonaFrecuenciaItem(zone: FrequencyZone) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(10.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Label con color más oscuro igual que la web
            Box(
                modifier         = Modifier
                    .background(ZoneLabelColor, RoundedCornerShape(50.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "${zone.label} →",
                    color      = Color.White,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Valor mínimo — vacío si no hay datos
            OutlinedTextField(
                value         = if (zone.min == 0 && zone.max == 0) "" else zone.min.toString(),
                onValueChange = {},
                enabled       = false,
                singleLine    = true,
                modifier      = Modifier.weight(1f),
                shape         = RoundedCornerShape(8.dp),
                textStyle     = TextStyle(textAlign = TextAlign.Center, fontSize = 13.sp),
                colors        = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor    = Color(0xFFCCCCCC),
                    disabledContainerColor = Color(0xFFF5F5F5),
                    disabledTextColor      = Color(0xFF555B61)
                )
            )

            Text("-", color = Color(0xFF888888), fontSize = 14.sp)

            // Valor máximo — vacío si no hay datos
            OutlinedTextField(
                value         = if (zone.min == 0 && zone.max == 0) "" else zone.max.toString(),
                onValueChange = {},
                enabled       = false,
                singleLine    = true,
                modifier      = Modifier.weight(1f),
                shape         = RoundedCornerShape(8.dp),
                textStyle     = TextStyle(textAlign = TextAlign.Center, fontSize = 13.sp),
                colors        = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor    = Color(0xFFCCCCCC),
                    disabledContainerColor = Color(0xFFF5F5F5),
                    disabledTextColor      = Color(0xFF555B61)
                )
            )
        }
    }
}

@Composable
private fun ZonaRitmoItem(zone: RhythmZone) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(10.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Label con color más oscuro igual que la web
            Box(
                modifier         = Modifier
                    .background(ZoneLabelColor, RoundedCornerShape(50.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "${zone.label} →",
                    color      = Color.White,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Pace mínimo — vacío si no hay datos
            OutlinedTextField(
                value         = zone.minPace.replace(" min/km", ""),
                onValueChange = {},
                enabled       = false,
                singleLine    = true,
                modifier      = Modifier.weight(1f),
                shape         = RoundedCornerShape(8.dp),
                textStyle     = TextStyle(textAlign = TextAlign.Center, fontSize = 13.sp),
                colors        = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor    = Color(0xFFCCCCCC),
                    disabledContainerColor = Color(0xFFF5F5F5),
                    disabledTextColor      = Color(0xFF555B61)
                )
            )

            if (zone.maxPace.isNotBlank()) {
                Text("-", color = Color(0xFF888888), fontSize = 14.sp)

                // Pace máximo — vacío si no hay datos
                OutlinedTextField(
                    value         = zone.maxPace.replace(" min/km", ""),
                    onValueChange = {},
                    enabled       = false,
                    singleLine    = true,
                    modifier      = Modifier.weight(1f),
                    shape         = RoundedCornerShape(8.dp),
                    textStyle     = TextStyle(textAlign = TextAlign.Center, fontSize = 13.sp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor    = Color(0xFFCCCCCC),
                        disabledContainerColor = Color(0xFFF5F5F5),
                        disabledTextColor      = Color(0xFF555B61)
                    )
                )
            }
        }
    }
}