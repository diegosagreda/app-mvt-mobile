package com.example.mvt.ui.screens.personaldata

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
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
import com.example.mvt.data.firebase.models.PersonalRecord
import com.example.mvt.ui.components.AddPersonalRecordDialog
import com.example.mvt.ui.components.FormErrorNotification
import com.example.mvt.ui.components.FormSuccessNotification
import com.example.mvt.ui.components.FormTooltip
import com.example.mvt.ui.components.formFieldColors
import com.example.mvt.ui.theme.PrimaryBlue
import com.example.mvt.ui.viewmodels.FrequencyZone
import com.example.mvt.ui.viewmodels.PerformanceUiState
import com.example.mvt.ui.viewmodels.PerformanceViewModel
import com.example.mvt.ui.viewmodels.RhythmZone

private val ZoneLabelColor = Color(0xFF1A3A5C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceScreen(
    navController: NavController,
    viewModel: PerformanceViewModel
) {
    val records         by viewModel.records.collectAsState()
    val uiState         by viewModel.uiState.collectAsState()
    val currentVam      by viewModel.currentVam.collectAsState()
    val frequencyZones  by viewModel.frequencyZones.collectAsState()
    val rhythmZones     by viewModel.rhythmZones.collectAsState()
    val personalRecords by viewModel.personalRecords.collectAsState()
    val isDeleting      by viewModel.isDeleting.collectAsState()
    val distanceOptions by viewModel.distanceOptions.collectAsState()

    var testValue       by remember { mutableStateOf("") }
    var showSuccess     by remember { mutableStateOf(false) }
    var showError       by remember { mutableStateOf(false) }
    var testError       by remember { mutableStateOf(false) }
    var expandedHistory by remember { mutableStateOf(false) }
    var showAddDialog by remember   { mutableStateOf(false) }
    var showAddSuccess  by remember { mutableStateOf(false) }
    var showAddError    by remember { mutableStateOf(false) }
    var showDeleteSuccess by remember { mutableStateOf(false) }

    val vamPreview = remember(testValue) {
        viewModel.calcularVamPreview(testValue)
    }

    LaunchedEffect(Unit) {
        viewModel.loadData()
        viewModel.loadDistanceOptions()
    }

    LaunchedEffect(records) {
        if (records.isNotEmpty()) {
            val semicooper = records.first().semicooper
            if (testValue.isEmpty()) {
                testValue = semicooper
            }
            // Por precaución de no carga de datos se forza recálculo de zonas con el valor actual
            viewModel.updateCurrentTestValue(
                if (testValue.isNotEmpty()) testValue else semicooper
            )
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is PerformanceUiState.Saved -> { showSuccess = true; viewModel.resetState() }
            is PerformanceUiState.DeleteSuccess -> { showDeleteSuccess = true; viewModel.resetState() }
            is PerformanceUiState.Error -> { showError = true;   viewModel.resetState() }
            else -> {}
        }
    }

    val isLoading   = uiState is PerformanceUiState.Loading
    val scrollState = rememberScrollState()

    // Zonas vacías cuando no hay datos — "" en vez de "Infinity"
    val freqZonasDisplay = if (frequencyZones.isNotEmpty()) frequencyZones
    else List(6) { i -> FrequencyZone("Z$i", 0, 0) }

    val rhythmZonasDisplay = if (rhythmZones.isNotEmpty()) {
        rhythmZones.map { zone ->
            zone.copy(
                minPace = if (zone.minPace.contains("Infinity") ||
                    zone.minPace.contains("NaN") ||
                    zone.minPace.contains("--")) "" else zone.minPace,
                maxPace = if (zone.maxPace.contains("Infinity") ||
                    zone.maxPace.contains("NaN") ||
                    zone.maxPace.contains("--")) "" else zone.maxPace
            )
        }
    } else listOf(
        RhythmZone("R0",  "", ""),
        RhythmZone("R1",  "", ""),
        RhythmZone("R2",  "", ""),
        RhythmZone("R3",  "", ""),
        RhythmZone("R3+", "", ""),
        RhythmZone("R4",  "", ""),
        RhythmZone("R5",  "", ""),
        RhythmZone("R6",  "", "")
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
        viewModel.saveRecord(testValue)
    }

    val shouldShowExpandButton = records.size > 5
    val visibleRecords = if (expandedHistory) records else records.take(5)

    Box(modifier = Modifier.fillMaxSize()) {

        // ← TODO el contenido va dentro de este Column con scroll
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
            // VAM
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
            // TEST
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
                FormTooltip("Pruebas para conocer tu rendimiento deportivo.")
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = testValue,
                onValueChange = {
                    testValue = it.filter { c -> c.isDigit() }
                    testError = testValue.isBlank()
                    viewModel.updateCurrentTestValue(testValue)
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
                                    viewModel.updateCurrentTestValue(testValue)
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
                                    viewModel.updateCurrentTestValue(testValue)
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
            HorizontalDivider(
                modifier  = Modifier.padding(vertical = 16.dp),
                thickness = 1.dp,
                color     = Color(0xFFD9D9D9)
            )

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

            Spacer(modifier = Modifier.height(10.dp))

            if (records.isEmpty()) {
                Text(
                    text = "Aún no tienes registros. ¡Realiza tu primer test!",
                    fontSize = 13.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                visibleRecords.forEachIndexed { index, record ->
                    val isLatest         = index == 0
                    val isLastVisibleItem = index == visibleRecords.lastIndex

                    val backgroundColor = when {
                        isLatest      -> PrimaryBlue
                        index % 2 == 1 -> Color(0xFFF5F7FF)
                        else           -> Color.White
                    }
                    val titleColor = if (isLatest) Color.White else Color(0xFF9B9B9B)
                    val valueColor = if (isLatest) Color.White else Color(0xFF9B9B9B)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(backgroundColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("VAM", color = titleColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(record.VAM, color = valueColor,
                                fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp)
                        }
                        if (!isLastVisibleItem) {
                            HorizontalDivider(thickness = 1.dp,
                                color = if (isLatest) Color.White.copy(alpha = 0.7f) else Color(0xFFE0E0E0))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Test", color = titleColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(record.semicooper, color = valueColor,
                                fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp)
                        }
                        if (!isLastVisibleItem) {
                            HorizontalDivider(thickness = 1.dp,
                                color = if (isLatest) Color.White.copy(alpha = 0.7f) else Color(0xFFE0E0E0))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Fecha", color = titleColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(viewModel.formatFecha(record.fecha), color = valueColor,
                                fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp)
                        }
                        if (!isLastVisibleItem) {
                            HorizontalDivider(thickness = 1.dp,
                                color = if (isLatest) Color.White.copy(alpha = 0.7f) else Color(0xFFE0E0E0))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (shouldShowExpandButton) {
                    Button(
                        onClick = { expandedHistory = !expandedHistory },
                        colors  = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape   = RoundedCornerShape(50.dp),
                        modifier = Modifier.padding(top = 4.dp).height(46.dp).width(130.dp)
                    ) {
                        Text(
                            text = if (expandedHistory) "Ver menos" else "Ver más",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier  = Modifier.padding(vertical = 20.dp),
                thickness = 1.dp,
                color     = Color(0xFFD9D9D9)
            )

            // ==========================================
            // ZONAS DE FRECUENCIA
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

            HorizontalDivider(
                modifier  = Modifier.padding(vertical = 20.dp),
                thickness = 1.dp,
                color     = Color(0xFFD9D9D9)
            )

            // ==========================================
            // ZONAS DE RITMO
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

            // ==========================================
            // RECORDS PERSONALES ← dentro del Column
            // ==========================================
            HorizontalDivider(
                modifier  = Modifier.padding(vertical = 20.dp),
                thickness = 1.dp,
                color     = Color(0xFFEEEEEE)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Records Personales",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2B2E34)
                )
                Spacer(modifier = Modifier.width(6.dp))
                FormTooltip("Tus mejores registros en cada distancia")
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { showAddDialog = true },
                shape  = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(
                    text       = "Añadir Marca",
                    color      = Color.White,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (personalRecords.isNotEmpty()) {
                PersonalRecordsSection(
                    records    = personalRecords,
                    isDeleting = isDeleting,
                    onDelete   = { index -> viewModel.deletePersonalRecord(index) }
                )
            } else {
                Text(
                    text = "Aún no tienes marcas registradas.",
                    fontSize = 13.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

        } // ← cierre del Column principal


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

        if (showAddSuccess) {
            FormSuccessNotification(
                message   = "¡Marca registrada correctamente!",
                onDismiss = { showAddSuccess = false }
            )
        }
        if (showAddError) {
            FormErrorNotification(
                message   = "Error al guardar la marca",
                onDismiss = { showAddError = false }
            )
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
        if (showAddDialog) {
            AddPersonalRecordDialog(
                distanceOptions = distanceOptions,
                onDismiss       = { showAddDialog = false },
                onSave          = { distancia, fecha, tiempoH, tiempoM, tiempoS, ritmo, fcProm ->
                    viewModel.savePersonalRecord(
                        distancia = distancia,
                        fecha     = fecha,
                        tiempoH   = tiempoH,
                        tiempoM   = tiempoM,
                        tiempoS   = tiempoS,
                        ritmo     = ritmo,
                        fcProm    = fcProm,
                        onSuccess = {
                            showAddDialog   = false
                            showAddSuccess  = true
                        },
                        onError = {
                            showAddError = true
                        }
                    )
                }
            )
        }
        if (showDeleteSuccess) {
            DeleteSuccessNotification(
                onDismiss = { showDeleteSuccess = false }
            )
        }

    }
}

// ==========================================
// COMPONENTES PRIVADOS
// ==========================================

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
            OutlinedTextField(
                value         = if (zone.min == 0 && zone.max == 0) "" else zone.min.toString(),
                onValueChange = {},
                enabled       = false,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = Color(0xFFD6D6D6),
                        modifier = Modifier.size(18.dp)
                    )
                },
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
            OutlinedTextField(
                value         = if (zone.min == 0 && zone.max == 0) "" else zone.max.toString(),
                onValueChange = {},
                enabled       = false,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = Color(0xFFD6D6D6),
                        modifier = Modifier.size(18.dp)
                    )
                },
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
            OutlinedTextField(
                value         = zone.minPace.replace(" min/km", ""),
                onValueChange = {},
                enabled       = false,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = Color(0xFFD6D6D6),
                        modifier = Modifier.size(18.dp)
                    )
                },
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
                OutlinedTextField(
                    value         = zone.maxPace.replace(" min/km", ""),
                    onValueChange = {},
                    enabled       = false,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = Color(0xFFD6D6D6),
                            modifier = Modifier.size(18.dp)
                        )
                    },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalRecordsSection(
    records: List<PersonalRecord>,
    isDeleting: Boolean,
    onDelete: (Int) -> Unit
) {
    var itemsPerPage    by remember { mutableStateOf(5) }
    var currentPage     by remember { mutableStateOf(1) }
    var expandedPerPage by remember { mutableStateOf(false) }

    val opcionesPerPage = listOf(5, 10, 15, 20, 25)
    val totalPages   = maxOf(1, (records.size + itemsPerPage - 1) / itemsPerPage)
    val startIndex   = (currentPage - 1) * itemsPerPage
    val endIndex     = minOf(startIndex + itemsPerPage, records.size)
    val currentItems = records.subList(startIndex, endIndex)


    LaunchedEffect(itemsPerPage) { currentPage = 1 }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = "Marcas Registradas",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2B2E34)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Mostrar:",
                fontSize = 13.sp,
                color = Color(0xFF888888)
            )

            Spacer(modifier = Modifier.width(6.dp))

            ExposedDropdownMenuBox(

                expanded = expandedPerPage,

                onExpandedChange = {
                    expandedPerPage = !expandedPerPage
                }

            ) {

                OutlinedTextField(

                    value = itemsPerPage.toString(),

                    onValueChange = {},

                    readOnly = true,

                    modifier = Modifier
                        .width(92.dp)
                        .height(48.dp)
                        .menuAnchor(),

                    shape = RoundedCornerShape(8.dp),

                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                        textAlign = TextAlign.Center
                    ),

                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = expandedPerPage
                        )
                    },

                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Color(0xFFCCCCCC),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                ExposedDropdownMenu(

                    expanded = expandedPerPage,

                    onDismissRequest = {
                        expandedPerPage = false
                    },

                    modifier = Modifier
                        .background(Color.White)
                ) {

                    opcionesPerPage.forEach { option ->

                        val isSelected = option == itemsPerPage

                        DropdownMenuItem(

                            text = {

                                Text(
                                    text = option.toString(),

                                    color =
                                        if (isSelected)
                                            Color.White
                                        else
                                            Color(0xFF555555),

                                    fontSize = 14.sp,

                                    fontWeight =
                                        if (isSelected)
                                            FontWeight.SemiBold
                                        else
                                            FontWeight.Normal
                                )
                            },

                            onClick = {

                                itemsPerPage = option
                                currentPage = 1
                                expandedPerPage = false
                            },

                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected)
                                        PrimaryBlue
                                    else
                                        Color.White
                                )
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    currentItems.forEachIndexed { pageIndex, record ->

        val isLast = pageIndex == currentItems.lastIndex

        val backgroundColor =
            if (pageIndex % 2 == 0)
                Color.White
            else
                Color(0xFFF5F7FF)

        Column(
            modifier = Modifier.padding(bottom = 14.dp)
        ) {

            PersonalRecordItem(
                record = record,
                isLast = isLast,
                backgroundColor = backgroundColor,
                isDeleting = isDeleting,
                onDelete = { onDelete(record.index) }
            )
        }
    }

    if (records.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            (1..totalPages).forEach { page ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(36.dp)
                        .background(
                            color = if (page == currentPage) PrimaryBlue else Color.Transparent,
                            shape = RoundedCornerShape(50.dp)
                        )
                        .clickable { currentPage = page },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = page.toString(),
                        fontSize   = 13.sp,
                        color      = if (page == currentPage) Color.White else Color(0xFF2B2E34),
                        fontWeight = if (page == currentPage) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            IconButton(
                onClick = { if (currentPage > 1) currentPage-- },
                enabled = currentPage > 1
            ) {
                Icon(
                    imageVector        = Icons.Default.ArrowBack,
                    contentDescription = "Anterior",
                    tint               = if (currentPage > 1) PrimaryBlue else Color(0xFFCCCCCC)
                )
            }
            IconButton(
                onClick = { if (currentPage < totalPages) currentPage++ },
                enabled = currentPage < totalPages
            ) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Siguiente",
                    tint               = if (currentPage < totalPages) PrimaryBlue else Color(0xFFCCCCCC)
                )
            }
        }
    }
}

@Composable
private fun PersonalRecordItem(
    record: PersonalRecord,
    isLast: Boolean,
    backgroundColor: Color,
    isDeleting: Boolean,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor   = Color.White,
            shape            = RoundedCornerShape(16.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .border(width = 2.dp, color = Color(0xFFFF9800), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = "!",
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFFFF9800)
                    )
                }
            },
            title = {
                Text(
                    text       = "¿Estás seguro?",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF2B2E34),
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text      = "No podrás revertir esto!",
                    fontSize  = 14.sp,
                    color     = Color(0xFF555B61),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape   = RoundedCornerShape(8.dp)
                ) { Text("Sí, eliminar", color = Color.White) }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteDialog = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape   = RoundedCornerShape(8.dp)
                ) { Text("Cancelar", color = Color.White) }
            }
        )
    }

    val fechaFormateada = remember(record.fecha) {
        try {
            val sdf    = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
            val date   = sdf.parse(record.fecha)
            val output = java.text.SimpleDateFormat("d/M/yyyy", java.util.Locale.getDefault())
            output.format(date!!)
        } catch (e: Exception) { record.fecha }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
    ) {

        RecordRow(
            label = "Distancia",
            value = record.distancia ?: ""
        )

        if (!isLast) {
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
        }

        RecordRow(
            label = "Tiempo (HH:MM:SS)",
            value = "${record.tiempoH?.toString() ?: "0"}:${
                record.tiempoM?.toString() ?: "0"
            }:${
                record.tiempoS?.toString() ?: "00"
            }"
        )

        if (!isLast) {
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
        }

        RecordRow(
            label = "Fecha",
            value = fechaFormateada ?: ""
        )

        if (!isLast) {
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
        }

        RecordRow(
            label = "Ritmo",
            value = record.ritmo ?: ""
        )

        if (!isLast) {
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
        }

        RecordRow(
            label = "FC Promedio",
            value = record.fcProm ?: ""
        )

        if (!isLast) {
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 14.dp),

            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Eliminar",
                fontSize = 14.sp,
                color = Color(0xFF888888),
                fontWeight = FontWeight.SemiBold
            )

            Box(
                modifier = Modifier
                    .padding(end = 10.dp)
                    .size(40.dp)
                    .border(
                        width = 1.dp,
                        color = Color(0xFFD32F2F),
                        shape = CircleShape
                    )
                    .clickable(enabled = !isDeleting) {
                        showDeleteDialog = true
                    },

                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar marca",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)

        if (!isLast) {
            HorizontalDivider(
                color = Color(0xFFEEEEEE),
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
private fun RecordRow(label: String, value: String) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 14.dp),

        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF8F8F8F),
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = value,
            fontSize = 14.sp,
            color = Color(0xFF9B9B9B),
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(end = 10.dp)
        )
    }
}

@Composable
private fun DeleteSuccessNotification(onDismiss: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        onDismiss()
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.spacedBy(16.dp)
            ) {
                // Círculo verde con checkmark
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .border(
                            width = 2.dp,
                            color = Color(0xFF4CAF50),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint               = Color(0xFF4CAF50),
                        modifier           = Modifier.size(40.dp)
                    )
                }

                Text(
                    text       = "¡Eliminado!",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF2B2E34)
                )

                Text(
                    text      = "Tu marca ha sido eliminada.",
                    fontSize  = 14.sp,
                    color     = Color(0xFF888888),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}