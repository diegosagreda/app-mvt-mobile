package com.example.mvt.ui.screens.personaldata

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.mvt.ui.components.FormConfirmationDialog
import com.example.mvt.ui.components.FormErrorNotification
import com.example.mvt.ui.components.FormLegendField
import com.example.mvt.ui.components.FormLegendLabel
import com.example.mvt.ui.components.FormSuccessNotification
import com.example.mvt.ui.components.FormTooltip
import com.example.mvt.ui.components.formFieldColors
import com.example.mvt.ui.theme.*
import com.example.mvt.ui.viewmodels.FrequencyZone
import com.example.mvt.ui.viewmodels.PerformanceUiState
import com.example.mvt.ui.viewmodels.PerformanceViewModel
import com.example.mvt.ui.viewmodels.RhythmZone

// ==========================================
// PANTALLA PRINCIPAL
// ==========================================
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


    var testValue           by remember { mutableStateOf("") }
    var showSuccess         by remember { mutableStateOf(false) }
    var showError           by remember { mutableStateOf(false) }
    var testError           by remember { mutableStateOf(false) }
    var expandedHistory     by remember { mutableStateOf(false) }
    var showAddDialog       by remember { mutableStateOf(false) }
    var showAddSuccess      by remember { mutableStateOf(false) }
    var showAddError        by remember { mutableStateOf(false) }
    var showDeleteSuccess   by remember { mutableStateOf(false) }
    var isSaving            by remember { mutableStateOf(false) }

    // Valor original cargado desde Firebase para detectar cambios
    var originalTestValue by remember { mutableStateOf("") }

    // Secciones colapsables
    var testSectionExpanded     by remember { mutableStateOf(true) }
    var historialExpanded       by remember { mutableStateOf(true) }
    var freqZonesExpanded       by remember { mutableStateOf(true) }
    var rhythmZonesExpanded     by remember { mutableStateOf(true) }
    var recordsExpanded         by remember { mutableStateOf(true) }

    val vamPreview = remember(testValue) {
        viewModel.calcularVamPreview(testValue)
    }
    val hasUnsavedChanges = testValue.isNotBlank() && testValue != originalTestValue

    LaunchedEffect(Unit) {
        viewModel.loadData()
        viewModel.loadDistanceOptions()
    }

    LaunchedEffect(records) {
        if (records.isNotEmpty()) {
            val semicooper = records.first().semicooper
            if (testValue.isEmpty()) {
                testValue = semicooper
                originalTestValue = semicooper  // ← guardar valor base
            }
            viewModel.updateCurrentTestValue(
                if (testValue.isNotEmpty()) testValue else semicooper
            )
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is PerformanceUiState.Saved -> {
                showSuccess = true
                originalTestValue = testValue  // ← ya no hay cambios pendientes
                isSaving = false
                viewModel.resetState()
            }
            is PerformanceUiState.DeleteSuccess -> {
                // Ya se muestra instantáneamente en el onClick
                viewModel.resetState()
            }
            is PerformanceUiState.Error -> {
                showError = true
                isSaving = false
                viewModel.resetState()
            }
            else -> {}
        }
    }

    val isLoading   = uiState is PerformanceUiState.Loading
    val scrollState = rememberScrollState()

    val freqZonasDisplay = if (frequencyZones.isNotEmpty()) frequencyZones
    else List(6) { i -> FrequencyZone("Z$i", 0, 0) }

    val rhythmZonasDisplay = if (rhythmZones.isNotEmpty()) {
        rhythmZones.map { zone ->
            zone.copy(
                minPace = if (zone.minPace.contains("Infinity") || zone.minPace.contains("NaN") || zone.minPace.contains("--")) "" else zone.minPace,
                maxPace = if (zone.maxPace.contains("Infinity") || zone.maxPace.contains("NaN") || zone.maxPace.contains("--")) "" else zone.maxPace
            )
        }
    } else listOf(
        RhythmZone("R0","",""), RhythmZone("R1","",""), RhythmZone("R2","",""),
        RhythmZone("R3","",""), RhythmZone("R3+","",""), RhythmZone("R4","",""),
        RhythmZone("R5","",""), RhythmZone("R6","","")
    )

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(AppBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue, strokeWidth = 3.dp)
        }
        return
    }

    fun guardar() {
        val dist = testValue.toDoubleOrNull()
        testError = dist == null || dist <= 0
        if (testError) { showError = true; return }
        isSaving = true          // ← activar spinner
        viewModel.saveRecord(testValue)
    }

    val latestRecord = records.firstOrNull()
    val shouldShowExpandButton = records.size > 5
    val visibleRecords = if (expandedHistory) records else records.take(5)

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = if (hasUnsavedChanges) 140.dp else 90.dp)
        ) {

            // ==========================================
            // HERO HEADER
            // ==========================================
            PerformanceHeroHeader(
                navController = navController,
                latestVam     = if (currentVam.isNotBlank()) currentVam else latestRecord?.VAM ?: "--"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 8.dp)) {

                // ==========================================
                // SECCIÓN: TEST SEMICOOPER
                // ==========================================
                PerformanceSectionCard(
                    title       = "Test Semicooper",
                    subtitle    = "Registra tu distancia y calcula tu VAM.",
                    expanded    = testSectionExpanded,
                    onToggle    = { testSectionExpanded = !testSectionExpanded }
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Calienta 10 minutos y estira, luego corre la mayor distancia posible durante 6 minutos, apunta la distancia recorrida en metros.",
                        fontSize  = 13.sp,
                        color     = AppTextSecondary,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // VAM readonly
                    FormLegendField(
                        label = {
                            FormLegendLabel(
                                text = "VAM",
                                info = "Es la velocidad máxima a la que puedes correr cuando tu consumo de oxígeno está al máximo nivel.",
                                required = true
                            )
                        }
                    ) { modifier ->
                        OutlinedTextField(
                            value         = if (vamPreview.isNotBlank()) vamPreview else if (currentVam.isNotBlank()) currentVam else "",
                            onValueChange = {},
                            enabled       = false,
                            placeholder   = { Text("Se calcula automáticamente", color = AppTextSecondary, fontSize = 13.sp) },
                            leadingIcon   = {
                                Icon(
                                    imageVector        = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint               = AppIconMuted
                                )
                            },
                            modifier  = modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(14.dp),
                            textStyle = TextStyle(color = AppTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            colors    = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor      = AppBorder,
                                disabledContainerColor   = AppSurfaceMuted,
                                disabledTextColor        = AppTextPrimary,
                                disabledPlaceholderColor = AppTextSecondary,
                                disabledLeadingIconColor = AppIconMuted
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // TEST input
                    FormLegendField(
                        label = {
                            FormLegendLabel(
                                text = "Test",
                                info = "Pruebas para conocer tu rendimiento deportivo.",
                                required = true
                            )
                        }
                    ) { modifier ->
                        OutlinedTextField(
                            value         = testValue,
                            onValueChange = {
                                testValue = it.filter { c -> c.isDigit() }
                                testError = testValue.isBlank()
                                viewModel.updateCurrentTestValue(testValue)
                            },
                            isError       = testError,
                            placeholder   = { Text("Distancia en metros", color = AppTextSecondary, fontSize = 13.sp) },
                            leadingIcon   = {
                                Icon(imageVector = Icons.Default.DirectionsRun, contentDescription = null, tint = if (testError) AppError else AppIconMuted)
                            },
                            trailingIcon  = {
                                Column(
                                    modifier            = Modifier.padding(end = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector        = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Subir",
                                        tint               = PrimaryBlue,
                                        modifier           = Modifier.size(20.dp).clickable {
                                            val actual = testValue.toIntOrNull() ?: 0
                                            testValue  = (actual + 1).toString()
                                            testError  = false
                                            viewModel.updateCurrentTestValue(testValue)
                                        }
                                    )
                                    Icon(
                                        imageVector        = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Bajar",
                                        tint               = PrimaryBlue,
                                        modifier           = Modifier.size(20.dp).clickable {
                                            val actual = testValue.toIntOrNull() ?: 0
                                            val nuevo  = if (actual > 0) actual - 1 else 0
                                            testValue  = nuevo.toString()
                                            testError  = testValue == "0" || testValue.isBlank()
                                            viewModel.updateCurrentTestValue(testValue)
                                        }
                                    )
                                }
                            },
                            singleLine      = true,
                            modifier        = modifier.fillMaxWidth(),
                            shape           = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle       = TextStyle(color = AppTextPrimary, fontSize = 15.sp),
                            colors          = formFieldColors()
                        )
                    }
                    if (testError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text     = "Escribe una distancia válida",
                            color    = AppError,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // SECCIÓN: HISTORIAL VAM (rediseñado - tarjetas colapsables)
                // ==========================================
                PerformanceSectionCard(
                    title    = "Historial de VAM",
                    subtitle = "Tu progreso en el test Semicooper.",
                    tooltip  = "Cuando realices el test más de una vez, aquí se mostrará tu progreso.",
                    expanded = historialExpanded,
                    onToggle = { historialExpanded = !historialExpanded }
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    if (records.isEmpty()) {
                        EmptyStateMessage(
                            icon    = Icons.Default.Timeline,
                            message = "Aún no tienes registros. ¡Realiza tu primer test!"
                        )
                    } else {
                        visibleRecords.forEachIndexed { index, record ->
                            VamHistoryCard(
                                record        = record,
                                isLatest      = index == 0,
                                formattedDate = viewModel.formatFecha(record.fecha)
                            )
                        }

                        if (shouldShowExpandButton) {
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(
                                onClick = { expandedHistory = !expandedHistory },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(
                                    text  = if (expandedHistory) "Ver menos" else "Ver más",
                                    color = PrimaryBlue,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector        = if (expandedHistory) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint               = PrimaryBlue,
                                    modifier           = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // SECCIÓN: ZONAS DE FRECUENCIA
                // ==========================================
                PerformanceSectionCard(
                    title    = "Zonas de frecuencia",
                    subtitle = "Rangos de pulsaciones por minuto (ppm).",
                    tooltip  = "Pulsaciones por minuto representan rangos para medir la intensidad del ejercicio, basados en tu frecuencia cardíaca.",
                    expanded = freqZonesExpanded,
                    onToggle = { freqZonesExpanded = !freqZonesExpanded }
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    freqZonasDisplay.forEach { zone ->
                        ZonaFrecuenciaItem(zone = zone)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // SECCIÓN: ZONAS DE RITMO
                // ==========================================
                PerformanceSectionCard(
                    title    = "Zonas de ritmo",
                    subtitle = "Rangos de velocidad en min/km.",
                    tooltip  = "Son rangos para medir la intensidad del ejercicio, basados en la velocidad que debes llevar.",
                    expanded = rhythmZonesExpanded,
                    onToggle = { rhythmZonesExpanded = !rhythmZonesExpanded }
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    rhythmZonasDisplay.forEach { zone ->
                        ZonaRitmoItem(zone = zone)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // SECCIÓN: RECORDS PERSONALES
                // ==========================================
                PerformanceSectionCard(
                    title    = "Records Personales",
                    subtitle = "Tus mejores marcas en cada distancia.",
                    tooltip  = "Registra tus tiempos en las distintas distancias.",
                    expanded = recordsExpanded,
                    onToggle = { recordsExpanded = !recordsExpanded },
                    headerExtra = {
                        // Botón Añadir compacto en el header
                        Surface(
                            shape  = RoundedCornerShape(50.dp),
                            color  = AppPrimarySoft,
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable { showAddDialog = true }
                        ) {
                            Row(
                                modifier            = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment   = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.Add,
                                    contentDescription = null,
                                    tint               = PrimaryBlue,
                                    modifier           = Modifier.size(14.dp)
                                )
                                Text("Añadir", color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    if (personalRecords.isEmpty()) {
                        EmptyStateMessage(
                            icon    = Icons.Default.EmojiEvents,
                            message = "Aún no tienes marcas registradas."
                        )
                    } else {
                        PersonalRecordsSection(
                            records    = personalRecords,
                            isDeleting = isDeleting,
                            onDelete   = { index ->
                                showDeleteSuccess = true // Notificación instantánea
                                viewModel.deletePersonalRecord(index)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // ==========================================
// PANEL GUARDAR — solo aparece si hay cambios
// ==========================================
        AnimatedVisibility(
            visible = hasUnsavedChanges,
            enter   = expandVertically(expandFrom = Alignment.Bottom),
            exit    = shrinkVertically(shrinkTowards = Alignment.Bottom),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color    = AppSurface,
                border   = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = AppBorder
                ),
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text       = "Guardar cambios",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = AppTextPrimary
                    )
                    Text(
                        text     = "Actualiza tu registro con el nuevo valor del test.",
                        fontSize = 12.sp,
                        color    = AppTextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick  = { guardar() },
                        enabled  = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = PrimaryBlue,
                            disabledContainerColor = AppSurfaceAlt
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        if (isSaving) {
                            // Spinner mientras guarda
                            CircularProgressIndicator(
                                color       = AppTextPrimary,
                                strokeWidth = 2.dp,
                                modifier    = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text       = "Guardando...",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = AppTextSecondary
                            )
                        } else {
                            Icon(
                                imageVector        = Icons.Default.Save,
                                contentDescription = null,
                                modifier           = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text       = "Actualizar rendimiento",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Notificaciones
        if (showSuccess)      FormSuccessNotification(message = "¡Test guardado con éxito!",           onDismiss = { showSuccess = false })
        if (showError)        FormErrorNotification  (message = "Por favor completa los campos.",       onDismiss = { showError = false })
        if (showAddSuccess)   FormSuccessNotification(message = "¡Marca registrada correctamente!",     onDismiss = { showAddSuccess = false })
        if (showAddError)     FormErrorNotification  (message = "Error al guardar la marca.",           onDismiss = { showAddError = false })
        if (showDeleteSuccess) DeleteSuccessNotification(onDismiss = { showDeleteSuccess = false })

        if (showAddDialog) {
            AddPersonalRecordDialog(
                distanceOptions = distanceOptions,
                onDismiss       = { showAddDialog = false },
                onSave          = { distancia, fecha, tiempoH, tiempoM, tiempoS, ritmo, fcProm ->
                    viewModel.savePersonalRecord(
                        distancia = distancia, fecha = fecha,
                        tiempoH = tiempoH, tiempoM = tiempoM, tiempoS = tiempoS,
                        ritmo = ritmo, fcProm = fcProm,
                        onSuccess = { showAddDialog = false; showAddSuccess = true },
                        onError   = { showAddError = true }
                    )
                }
            )
        }
    }
}

// ==========================================
// HERO HEADER
// ==========================================
@Composable
private fun PerformanceHeroHeader(
    navController: NavController,
    latestVam: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(AppSurface, AppBackground),
                    startY = 0f, endY = Float.POSITIVE_INFINITY
                )
            )
            .padding(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape  = CircleShape,
                color  = AppSurfaceAlt,
                border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(50.dp)
                    ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint               = AppTextPrimary,
                        modifier           = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = "Información Rendimiento",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = AppTextPrimary
                )
                Text(
                    text     = "Controla tu velocidad aeróbica máxima.",
                    fontSize = 13.sp,
                    color    = AppTextSecondary
                )
            }
        }
    }

    // Pill de VAM actual
    if (latestVam != "--") {
        Box(modifier = Modifier.padding(horizontal = 8.dp)) {
            Surface(
                shape    = RoundedCornerShape(16.dp),
                color    = AppSurface,
                border   = androidx.compose.foundation.BorderStroke(1.dp, AppBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier              = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text("VAM Actual", fontSize = 12.sp, color = AppTextSecondary)
                        Text(
                            text       = latestVam,
                            fontSize   = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color      = AppTextPrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(AppPrimarySoft, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint               = PrimaryBlue,
                            modifier           = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// CARD DE SECCIÓN COLAPSABLE
// ==========================================
@Composable
private fun PerformanceSectionCard(
    title       : String,
    subtitle    : String,
    tooltip     : String? = null,
    expanded    : Boolean,
    onToggle    : () -> Unit,
    headerExtra : (@Composable RowScope.() -> Unit)? = null,
    content     : @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape    = RoundedCornerShape(18.dp),
        color    = AppSurface,
        border   = androidx.compose.foundation.BorderStroke(1.dp, AppBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header de la sección
            Row(
                modifier              = Modifier.fillMaxWidth().clickable(onClick = onToggle),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text       = title,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = AppTextPrimary
                        )
                        if (tooltip != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            FormTooltip(tooltip)
                        }
                    }
                    Text(
                        text     = subtitle,
                        fontSize = 12.sp,
                        color    = AppTextSecondary
                    )
                }
                Row(
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (headerExtra != null) headerExtra()
                    Icon(
                        imageVector        = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint               = AppIconMuted,
                        modifier           = Modifier.size(22.dp)
                    )
                }
            }

            // Contenido animado
            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    content()
                }
            }
        }
    }
}

// ==========================================
// LABEL DE CAMPO
// ==========================================
@Composable
private fun PerformanceFieldLabel(
    label    : String,
    tooltip  : String? = null,
    required : Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape  = RoundedCornerShape(8.dp),
            color  = AppSurfaceAlt,
            border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder.copy(alpha = 0.85f))
        ) {
            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = buildAnnotatedString {
                        append(label)
                        if (required) { append(" "); withStyle(SpanStyle(color = AppError)) { append("*") } }
                    },
                    fontSize   = 13.sp,
                    color      = AppTextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                if (tooltip != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    FormTooltip(tooltip)
                }
            }
        }
    }
}

// ==========================================
// ESTADO VACÍO
// ==========================================
@Composable
private fun EmptyStateMessage(icon: ImageVector, message: String) {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier         = Modifier.size(48.dp).background(AppSurfaceAlt, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = AppIconMuted, modifier = Modifier.size(24.dp))
        }
        Text(text = message, fontSize = 13.sp, color = AppTextSecondary, textAlign = TextAlign.Center)
    }
}

// ==========================================
// TARJETA DE HISTORIAL VAM
// ==========================================
@Composable
private fun VamHistoryCard(
    record       : PerformanceRecord,
    isLatest     : Boolean,
    formattedDate: String
) {
    var isExpanded by rememberSaveable(record.fecha, record.VAM) { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { isExpanded = !isExpanded },
        shape  = RoundedCornerShape(16.dp),
        color  = when {
            isLatest   -> Color(0xFF23395E)
            isExpanded -> AppSurfaceMuted
            else       -> AppSurfaceAlt
        },
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = when {
                isLatest   -> AppBorder.copy(alpha = 0.5f)
                isExpanded -> PrimaryBlue.copy(alpha = 0.4f)
                else       -> AppBorder.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // CABECERA (siempre visible)
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isLatest) Color.White.copy(alpha = 0.2f) else PrimaryBlue.copy(alpha = 0.12f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint               = if (isLatest) Color.White else PrimaryBlue,
                            modifier           = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text       = record.VAM,
                                color      = if (isLatest) Color.White else AppTextPrimary,
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isLatest) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(50.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text       = "Actual",
                                        fontSize   = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = Color.White,
                                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector        = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint               = if (isLatest) Color.White.copy(alpha = 0.75f) else AppIconMuted,
                                modifier           = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text     = formattedDate,
                                color    = if (isLatest) Color.White.copy(alpha = 0.75f) else AppTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Icon(
                    imageVector        = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint               = if (isLatest) Color.White else if (isExpanded) PrimaryBlue else AppIconMuted,
                    modifier           = Modifier.size(24.dp)
                )
            }

            // CONTENIDO EXPANDIBLE
            AnimatedVisibility(
                visible = isExpanded,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        color    = if (isLatest) Color.White.copy(alpha = 0.12f) else AppSurface.copy(alpha = 0.5f),
                        border   = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            if (isLatest) Color.White.copy(alpha = 0.2f) else AppBorder.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            VamDetailRow(
                                icon     = Icons.Default.DirectionsRun,
                                label    = "Distancia (Test)",
                                value    = "${record.semicooper} m",
                                isLatest = isLatest
                            )
                            VamDetailRow(
                                icon     = Icons.AutoMirrored.Filled.TrendingUp,
                                label    = "VAM calculada",
                                value    = record.VAM,
                                isLatest = isLatest
                            )
                            VamDetailRow(
                                icon     = Icons.Default.CalendarToday,
                                label    = "Fecha",
                                value    = formattedDate,
                                isLatest = isLatest
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VamDetailRow(icon: ImageVector, label: String, value: String, isLatest: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, null,
                tint     = if (isLatest) Color.White.copy(alpha = 0.75f) else AppIconMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label,
                color    = if (isLatest) Color.White.copy(alpha = 0.75f) else AppTextSecondary,
                fontSize = 13.sp
            )
        }
        Text(
            text       = value,
            color      = if (isLatest) Color.White else AppTextPrimary,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==========================================
// ZONA DE FRECUENCIA
// ==========================================
@Composable
private fun ZonaFrecuenciaItem(zone: FrequencyZone) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.width(62.dp),
            shape = RoundedCornerShape(50.dp),
            color = AppSurfaceAlt,
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.6f))
        ) {
            Text(
                text     = "${zone.label} →",
                color    = PrimaryBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        // Valor min
        Surface(
            modifier = Modifier.weight(1f),
            shape    = RoundedCornerShape(10.dp),
            color    = AppSurfaceAlt,
            border   = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
        ) {
            Text(
                text      = if (zone.min == 0 && zone.max == 0) "—" else zone.min.toString(),
                color     = if (zone.min == 0 && zone.max == 0) AppIconMuted else AppTextPrimary,
                fontSize  = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(vertical = 10.dp).fillMaxWidth()
            )
        }
        Text("—", color = AppIconMuted, fontSize = 13.sp)
        // Valor max
        Surface(
            modifier = Modifier.weight(1f),
            shape    = RoundedCornerShape(10.dp),
            color    = AppSurfaceAlt,
            border   = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
        ) {
            Text(
                text      = if (zone.min == 0 && zone.max == 0) "—" else zone.max.toString(),
                color     = if (zone.min == 0 && zone.max == 0) AppIconMuted else AppTextPrimary,
                fontSize  = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(vertical = 10.dp).fillMaxWidth()
            )
        }
    }
}

// ==========================================
// ZONA DE RITMO
// ==========================================
@Composable
private fun ZonaRitmoItem(zone: RhythmZone) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.width(62.dp),
            shape = RoundedCornerShape(50.dp),
            color = AppSurfaceAlt,
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.6f))
        ) {
            Text(
                text     = "${zone.label} →",
                color    = PrimaryBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        Surface(
            modifier = Modifier.weight(1f),
            shape    = RoundedCornerShape(10.dp),
            color    = AppSurfaceAlt,
            border   = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
        ) {
            Text(
                text      = zone.minPace.replace(" min/km", "").ifBlank { "—" },
                color     = if (zone.minPace.isBlank()) AppIconMuted else AppTextPrimary,
                fontSize  = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(vertical = 10.dp).fillMaxWidth()
            )
        }
        if (zone.maxPace.isNotBlank()) {
            Text("—", color = AppIconMuted, fontSize = 13.sp)
            Surface(
                modifier = Modifier.weight(1f),
                shape    = RoundedCornerShape(10.dp),
                color    = AppSurfaceAlt,
                border   = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
            ) {
                Text(
                    text      = zone.maxPace.replace(" min/km", ""),
                    color     = AppTextPrimary,
                    fontSize  = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(vertical = 10.dp).fillMaxWidth()
                )
            }
        }
    }
}

// ==========================================
// SECCIÓN DE RECORDS PERSONALES
// ==========================================
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

    // Selector Mostrar
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text       = "Marcas Registradas",
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = AppTextPrimary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mostrar:", fontSize = 12.sp, color = AppTextSecondary)
            Spacer(modifier = Modifier.width(6.dp))
            ExposedDropdownMenuBox(
                expanded         = expandedPerPage,
                onExpandedChange = { expandedPerPage = !expandedPerPage }
            ) {
                // Selector personalizado para evitar problemas de padding de OutlinedTextField
                Surface(
                    modifier = Modifier
                        .width(85.dp)
                        .height(42.dp)
                        .menuAnchor(),
                    shape  = RoundedCornerShape(10.dp),
                    color  = AppSurfaceAlt,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (expandedPerPage) PrimaryBlue else AppBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text       = itemsPerPage.toString(),
                            color      = AppTextPrimary,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = if (expandedPerPage) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = AppIconMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                ExposedDropdownMenu(
                    expanded         = expandedPerPage,
                    onDismissRequest = { expandedPerPage = false },
                    containerColor   = AppSurface
                ) {
                    opcionesPerPage.forEach { option ->
                        val isSelected = option == itemsPerPage
                        DropdownMenuItem(
                            text    = { Text(option.toString(), fontSize = 13.sp, color = if (isSelected) Color.White else AppTextPrimary) },
                            onClick = { itemsPerPage = option; currentPage = 1; expandedPerPage = false },
                            modifier = Modifier.background(if (isSelected) PrimaryBlue else Color.Transparent)
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Lista de marcas
    currentItems.forEachIndexed { pageIndex, record ->
        key(record.distancia, record.fecha, record.index) {
            PersonalRecordItem(
                record     = record,
                isDeleting = isDeleting,
                onDelete   = { onDelete(record.index) }
            )
        }
    }

    // Paginación
    if (totalPages > 1) {
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (currentPage > 1) currentPage-- }, enabled = currentPage > 1) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, tint = if (currentPage > 1) PrimaryBlue else AppIconMuted)
            }
            (1..totalPages).forEach { page ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(32.dp)
                        .background(
                            if (page == currentPage) PrimaryBlue else AppSurfaceAlt,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { currentPage = page },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = page.toString(),
                        fontSize   = 12.sp,
                        color      = if (page == currentPage) Color.White else AppTextSecondary,
                        fontWeight = if (page == currentPage) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            IconButton(onClick = { if (currentPage < totalPages) currentPage++ }, enabled = currentPage < totalPages) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = if (currentPage < totalPages) PrimaryBlue else AppIconMuted)
            }
        }
    }
}

// ==========================================
// ITEM DE RECORD PERSONAL (MODERNO Y COLAPSABLE)
// ==========================================
@Composable
private fun PersonalRecordItem(
    record: PersonalRecord,
    isDeleting: Boolean,
    onDelete: () -> Unit
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        FormConfirmationDialog(
            title        = "¿Eliminar esta marca?",
            message      = "Esta acción no se puede revertir.",
            confirmLabel = "Sí, eliminar",
            onConfirm    = {
                isExpanded = false // Cierra la tarjeta inmediatamente
                onDelete()
                showDeleteDialog = false
            },
            onDismiss    = { showDeleteDialog = false },
            destructive  = true
        )
    }

    val fechaFormateada = remember(record.fecha) {
        try {
            val sdf    = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
            val date   = sdf.parse(record.fecha)
            val output = java.text.SimpleDateFormat("d/M/yyyy", java.util.Locale.getDefault())
            output.format(date!!)
        } catch (e: Exception) { record.fecha ?: "" }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { isExpanded = !isExpanded },
        shape    = RoundedCornerShape(16.dp),
        color    = if (isExpanded) AppSurfaceMuted else AppSurfaceAlt,
        border   = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isExpanded) PrimaryBlue.copy(alpha = 0.4f) else AppBorder.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // CABECERA (Siempre visible)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(PrimaryBlue.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Straighten,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = record.distancia ?: "—",
                            color = AppTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = AppIconMuted,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = fechaFormateada,
                                color = AppTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = if (isExpanded) PrimaryBlue else AppIconMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            // CONTENIDO EXPANDIBLE
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Contenedor de detalles
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        color    = AppSurface.copy(alpha = 0.5f),
                        border   = androidx.compose.foundation.BorderStroke(0.5.dp, AppBorder.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            RecordDetailRow(
                                icon  = Icons.Default.Timer,
                                label = "Tiempo",
                                value = "${record.tiempoH}h ${record.tiempoM}m ${record.tiempoS}s"
                            )
                            RecordDetailRow(
                                icon  = Icons.Default.CalendarToday,
                                label = "Fecha",
                                value = fechaFormateada
                            )
                            RecordDetailRow(
                                icon  = Icons.Default.Speed,
                                label = "Ritmo",
                                value = record.ritmo ?: "—"
                            )
                            if (!record.fcProm.isNullOrBlank()) {
                                RecordDetailRow(
                                    icon  = Icons.Default.FavoriteBorder,
                                    label = "FC Prom",
                                    value = "${record.fcProm} ppm"
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Botón Eliminar
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        enabled = !isDeleting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = AppError.copy(alpha = 0.08f),
                            contentColor = AppError
                        )
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Eliminar Marca", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordDetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = AppIconMuted, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = AppTextSecondary, fontSize = 13.sp)
        }
        Text(
            text       = value,
            color      = AppTextPrimary,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==========================================
// NOTIFICACIÓN DE ELIMINACIÓN EXITOSA
// ==========================================
@Composable
private fun DeleteSuccessNotification(onDismiss: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        onDismiss()
    }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape  = RoundedCornerShape(20.dp),
            color  = AppSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, AppSuccess.copy(alpha = 0.5f))
        ) {
            Column(
                modifier            = Modifier.padding(32.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier         = Modifier.size(64.dp).background(AppSuccess.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint               = AppSuccess,
                        modifier           = Modifier.size(36.dp)
                    )
                }
                Text("¡Eliminado!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary)
                Text("Tu marca ha sido eliminada.", fontSize = 13.sp, color = AppTextSecondary, textAlign = TextAlign.Center)
            }
        }
    }
}