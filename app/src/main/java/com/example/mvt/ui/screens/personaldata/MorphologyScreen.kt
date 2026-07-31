package com.example.mvt.ui.screens.personaldata

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mvt.data.firebase.models.Morphology
import com.example.mvt.ui.components.FormErrorNotification
import com.example.mvt.ui.components.FormFieldShape
import com.example.mvt.ui.components.FormSuccessNotification
import com.example.mvt.ui.components.FormTooltip
import com.example.mvt.ui.components.formFieldColors
import com.example.mvt.ui.components.formReadOnlyColors
import com.example.mvt.ui.theme.AccentRed
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppPrimarySoft
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppSurfaceMuted
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import com.example.mvt.ui.viewmodels.MorphologyUiState
import com.example.mvt.ui.viewmodels.MorphologyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class PerimetroMedicion(
    val fecha: String = "",
    val medida: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorphologyScreen(
    navController: NavController,
    morphologyViewModel: MorphologyViewModel
) {
    var estatura by remember { mutableStateOf("") }
    var peso by remember { mutableStateOf("") }
    var grasa by remember { mutableStateOf("") }
    var imc by remember { mutableStateOf("") }
    var somatotipo by remember { mutableStateOf("") }

    var expandedSomatotipo by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var showDeleteSuccess by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var bodySectionExpanded by remember { mutableStateOf(true) }
    var perimetersSectionExpanded by remember { mutableStateOf(true) }

    var estaturaError by remember { mutableStateOf(false) }
    var pesoError by remember { mutableStateOf(false) }
    var grasaError by remember { mutableStateOf(false) }
    var perimetroError by remember { mutableStateOf(false) }

    var hombros by remember { mutableStateOf(PerimetroMedicion()) }
    var pecho by remember { mutableStateOf(PerimetroMedicion()) }
    var brazo by remember { mutableStateOf(PerimetroMedicion()) }
    var cintura by remember { mutableStateOf(PerimetroMedicion()) }
    var musloMedio by remember { mutableStateOf(PerimetroMedicion()) }
    var gluteos by remember { mutableStateOf(PerimetroMedicion()) }
    var pantorrilla by remember { mutableStateOf(PerimetroMedicion()) }

    val somatotipos = listOf(
        "Seleccione Uno",
        "1. Ectomorfo",
        "2. Mesomorfo",
        "3. Endomorfo"
    )

    val scrollState = rememberScrollState()
    val morphologyData by morphologyViewModel.morphology.collectAsState()
    val uiState by morphologyViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        morphologyViewModel.loadMorphology()
    }

    LaunchedEffect(estatura, peso) {
        val estaturaNum = estatura.toDoubleOrNull()
        val pesoNum = peso.toDoubleOrNull()
        imc = if (estaturaNum != null && pesoNum != null && estaturaNum > 0) {
            val metros = estaturaNum / 100.0
            String.format("%.2f", pesoNum / (metros * metros))
        } else ""
    }

    LaunchedEffect(morphologyData) {
        morphologyData?.let { data ->
            estatura = data.estatura ?: ""
            peso = data.peso ?: ""
            grasa = data.grasa ?: ""
            imc = data.imc ?: ""
            somatotipo = data.somatipo ?: ""
            hombros = cargarPerimetroDesdeModelo(data.fecha_hombros, data.medida_hombros)
            pecho = cargarPerimetroDesdeModelo(data.fecha_pecho, data.medida_pecho)
            brazo = cargarPerimetroDesdeModelo(data.fecha_brazo, data.medida_brazo)
            cintura = cargarPerimetroDesdeModelo(data.fecha_cintura, data.medida_cintura)
            musloMedio = cargarPerimetroDesdeModelo(data.fecha_muslo, data.medida_muslo)
            gluteos = cargarPerimetroDesdeModelo(data.fecha_gluteos, data.medida_gluteos)
            pantorrilla = cargarPerimetroDesdeModelo(data.fecha_pantorrilla, data.medida_pantorrilla)
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is MorphologyUiState.Saved -> {
                isSaving = false
                showSuccess = true
                morphologyViewModel.resetState()
            }

            is MorphologyUiState.Error -> {
                isSaving = false
                showError = true
                morphologyViewModel.resetState()
            }

            else -> {}
        }
    }

    val initialEstatura = morphologyData?.estatura.orEmpty()
    val initialPeso = morphologyData?.peso.orEmpty()
    val initialGrasa = morphologyData?.grasa.orEmpty()
    val initialSomatotipo = morphologyData?.somatipo.orEmpty()
    val initialHombros = morphologyData?.let {
        cargarPerimetroDesdeModelo(it.fecha_hombros, it.medida_hombros)
    } ?: PerimetroMedicion()
    val initialPecho = morphologyData?.let {
        cargarPerimetroDesdeModelo(it.fecha_pecho, it.medida_pecho)
    } ?: PerimetroMedicion()
    val initialBrazo = morphologyData?.let {
        cargarPerimetroDesdeModelo(it.fecha_brazo, it.medida_brazo)
    } ?: PerimetroMedicion()
    val initialCintura = morphologyData?.let {
        cargarPerimetroDesdeModelo(it.fecha_cintura, it.medida_cintura)
    } ?: PerimetroMedicion()
    val initialMusloMedio = morphologyData?.let {
        cargarPerimetroDesdeModelo(it.fecha_muslo, it.medida_muslo)
    } ?: PerimetroMedicion()
    val initialGluteos = morphologyData?.let {
        cargarPerimetroDesdeModelo(it.fecha_gluteos, it.medida_gluteos)
    } ?: PerimetroMedicion()
    val initialPantorrilla = morphologyData?.let {
        cargarPerimetroDesdeModelo(it.fecha_pantorrilla, it.medida_pantorrilla)
    } ?: PerimetroMedicion()

    val hasMorphologyChanges =
        estatura != initialEstatura ||
            peso != initialPeso ||
            grasa != initialGrasa ||
            somatotipo != initialSomatotipo ||
            hombros != initialHombros ||
            pecho != initialPecho ||
            brazo != initialBrazo ||
            cintura != initialCintura ||
            musloMedio != initialMusloMedio ||
            gluteos != initialGluteos ||
            pantorrilla != initialPantorrilla

    val isLoading = uiState is MorphologyUiState.Loading

    fun guardarMorfologia() {
        val estaturaNum = estatura.toDoubleOrNull()
        val pesoNum = peso.toDoubleOrNull()
        val grasaNum = grasa.toDoubleOrNull()

        estaturaError = estaturaNum == null || estaturaNum < 80 || estaturaNum > 220
        pesoError = pesoNum == null || pesoNum < 31
        grasaError = if (grasa.isBlank()) false else grasaNum == null || grasaNum < 4 || grasaNum > 50
        perimetroError = listOf(hombros, pecho, brazo, cintura, musloMedio, gluteos, pantorrilla)
            .any { it.fecha.isNotBlank() && it.medida.isBlank() }

        if (estaturaError || pesoError || grasaError || perimetroError) {
            showError = true
            return
        }

        isSaving = true
        morphologyViewModel.saveMorphology(
            Morphology(
                estatura = estatura,
                peso = peso,
                grasa = grasa,
                imc = imc,
                somatipo = somatotipo.replace(Regex("^\\d+\\.\\s*"), ""),
                fecha_hombros = fechaUIaIso(hombros.fecha),
                medida_hombros = hombros.medida,
                fecha_pecho = fechaUIaIso(pecho.fecha),
                medida_pecho = pecho.medida,
                fecha_brazo = fechaUIaIso(brazo.fecha),
                medida_brazo = brazo.medida,
                fecha_cintura = fechaUIaIso(cintura.fecha),
                medida_cintura = cintura.medida,
                fecha_muslo = fechaUIaIso(musloMedio.fecha),
                medida_muslo = musloMedio.medida,
                fecha_gluteos = fechaUIaIso(gluteos.fecha),
                medida_gluteos = gluteos.medida,
                fecha_pantorrilla = fechaUIaIso(pantorrilla.fecha),
                medida_pantorrilla = pantorrilla.medida
            )
        )
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
        return
    }

    val estaturaInteraction = remember { MutableInteractionSource() }
    val pesoInteraction = remember { MutableInteractionSource() }
    val grasaInteraction = remember { MutableInteractionSource() }

    val estaturaFocused by estaturaInteraction.collectIsFocusedAsState()
    val pesoFocused by pesoInteraction.collectIsFocusedAsState()
    val grasaFocused by grasaInteraction.collectIsFocusedAsState()

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (hasMorphologyChanges) {
                MorphologyBottomBar(
                    isSaving = isSaving,
                    onSave = { guardarMorfologia() }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(start = 8.dp, end = 8.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MorphologyTopBar(
                    onBack = {
                        navController.navigate("profile") {
                            popUpTo("profile") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                MorphologyHeroCard(
                    estatura = estatura,
                    peso = peso,
                    imc = imc
                )

                MorphologySectionCard(
                    title = "Composicion corporal",
                    subtitle = "Variables base para seguimiento antropometrico del atleta.",
                    expanded = bodySectionExpanded,
                    onToggle = { bodySectionExpanded = !bodySectionExpanded }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                                value = estatura,
                                onValueChange = {
                                    val filtered = it.filter { c -> c.isDigit() || c == '.' }
                                    estatura = filtered
                                    val valor = filtered.toDoubleOrNull()
                                    estaturaError = valor == null || valor < 80 || valor > 220
                                },
                                isError = estaturaError,
                                label = {
                                    MorphologyFieldLabel(
                                        text = "Estatura (cm)",
                                        required = true,
                                        info = "En centimetros"
                                    )
                                },
                                placeholder = { MorphologyPlaceholder("Estatura") },
                                interactionSource = estaturaInteraction,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Straighten,
                                        contentDescription = null,
                                        tint = if (estaturaFocused) PrimaryBlue else AppTextSecondary
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = FormFieldShape,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = formFieldColors()
                            )
                        OutlinedTextField(
                                value = peso,
                                onValueChange = {
                                    val filtered = it.filter { c -> c.isDigit() || c == '.' }
                                    peso = filtered
                                    val valor = filtered.toDoubleOrNull()
                                    pesoError = valor == null || valor < 31
                                },
                                isError = pesoError,
                                label = {
                                    MorphologyFieldLabel(
                                        text = "Peso (kg)",
                                        required = true,
                                        info = "En kilogramos"
                                    )
                                },
                                placeholder = { MorphologyPlaceholder("Peso") },
                                interactionSource = pesoInteraction,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.MonitorWeight,
                                        contentDescription = null,
                                        tint = if (pesoFocused) PrimaryBlue else AppTextSecondary
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = FormFieldShape,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = formFieldColors()
                            )
                    }
                    MorphologyFieldError(estaturaError, "Digita un valor entre 80 y 220.")
                    MorphologyFieldError(pesoError, "Digita un valor mayor a 30.")
                    MorphologyFieldSpacer()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                                value = grasa,
                                onValueChange = {
                                    val filtered = it.filter { c -> c.isDigit() || c == '.' }
                                    grasa = filtered
                                    val valor = filtered.toDoubleOrNull()
                                    grasaError = if (filtered.isBlank()) false else valor == null || valor < 4 || valor > 50
                                },
                                isError = grasaError,
                                label = {
                                    MorphologyFieldLabel(
                                        text = "Grasa",
                                        info = "Ideal mujer 20-30%. Ideal hombre 10-20%."
                                    )
                                },
                                placeholder = { MorphologyPlaceholder("Porcentaje") },
                                interactionSource = grasaInteraction,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Percent,
                                        contentDescription = null,
                                        tint = if (grasaFocused) PrimaryBlue else AppTextSecondary
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = FormFieldShape,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = formFieldColors()
                            )
                        OutlinedTextField(
                                value = imc,
                                onValueChange = {},
                                enabled = false,
                                label = {
                                    MorphologyFieldLabel(
                                        text = "IMC",
                                        info = "Indice de masa corporal. Ideal entre 20 y 25."
                                    )
                                },
                                placeholder = { MorphologyPlaceholder("IMC") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = null,
                                        tint = AppTextSecondary
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = FormFieldShape,
                                colors = formReadOnlyColors()
                            )
                    }
                    MorphologyFieldError(grasaError, "Digita un valor entre 4 y 50.")
                    MorphologyFieldSpacer()

                    ExposedDropdownMenuBox(
                            expanded = expandedSomatotipo,
                            onExpandedChange = { expandedSomatotipo = !expandedSomatotipo }
                        ) {
                            OutlinedTextField(
                                value = somatotipo,
                                onValueChange = {},
                                readOnly = true,
                                label = {
                                    MorphologyFieldLabel(
                                        text = "Somatotipo",
                                        info = "Ectomorfo: delgado. Mesomorfo: robusto. Endomorfo: acumulacion de grasa."
                                    )
                                },
                                placeholder = { MorphologyPlaceholder("Seleccione una opcion") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = FormFieldShape,
                                colors = formFieldColors(),
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSomatotipo)
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = expandedSomatotipo,
                                onDismissRequest = { expandedSomatotipo = false },
                                modifier = Modifier.background(AppSurfaceAlt)
                            ) {
                                somatotipos.forEach { item ->
                                    val isPlaceholder = item == "Seleccione Uno"
                                    MorphologyDropdownItem(
                                        text = item,
                                        selected = item == somatotipo,
                                        enabled = !isPlaceholder,
                                        onClick = {
                                            somatotipo = item
                                            expandedSomatotipo = false
                                        }
                                    )
                                }
                            }
                        }
                }

                MorphologySectionCard(
                    title = "Perimetros",
                    subtitle = "Registra fecha y medida para cada contorno corporal.",
                    expanded = perimetersSectionExpanded,
                    onToggle = { perimetersSectionExpanded = !perimetersSectionExpanded }
                ) {
                    MorphologyHintPill("Las medidas se registran en centimetros.")

                    PerimetroItem(
                        nombre = "Hombros",
                        medicion = hombros,
                        isError = perimetroError && hombros.fecha.isNotBlank() && hombros.medida.isBlank(),
                        onDeleteSuccess = { showDeleteSuccess = true },
                        onChange = { hombros = it }
                    )
                    PerimetroItem(
                        nombre = "Pecho",
                        medicion = pecho,
                        isError = perimetroError && pecho.fecha.isNotBlank() && pecho.medida.isBlank(),
                        onDeleteSuccess = { showDeleteSuccess = true },
                        onChange = { pecho = it }
                    )
                    PerimetroItem(
                        nombre = "Brazo",
                        medicion = brazo,
                        isError = perimetroError && brazo.fecha.isNotBlank() && brazo.medida.isBlank(),
                        onDeleteSuccess = { showDeleteSuccess = true },
                        onChange = { brazo = it }
                    )
                    PerimetroItem(
                        nombre = "Cintura",
                        medicion = cintura,
                        isError = perimetroError && cintura.fecha.isNotBlank() && cintura.medida.isBlank(),
                        onDeleteSuccess = { showDeleteSuccess = true },
                        onChange = { cintura = it }
                    )
                    PerimetroItem(
                        nombre = "Muslo medio",
                        medicion = musloMedio,
                        isError = perimetroError && musloMedio.fecha.isNotBlank() && musloMedio.medida.isBlank(),
                        onDeleteSuccess = { showDeleteSuccess = true },
                        onChange = { musloMedio = it }
                    )
                    PerimetroItem(
                        nombre = "Gluteos",
                        medicion = gluteos,
                        isError = perimetroError && gluteos.fecha.isNotBlank() && gluteos.medida.isBlank(),
                        onDeleteSuccess = { showDeleteSuccess = true },
                        onChange = { gluteos = it }
                    )
                    PerimetroItem(
                        nombre = "Pantorrilla",
                        medicion = pantorrilla,
                        isError = perimetroError && pantorrilla.fecha.isNotBlank() && pantorrilla.medida.isBlank(),
                        onDeleteSuccess = { showDeleteSuccess = true },
                        onChange = { pantorrilla = it }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            if (showSuccess) {
                FormSuccessNotification(
                    message = "Sus cambios han sido guardados con exito.",
                    onDismiss = { showSuccess = false }
                )
            }
            if (showError) {
                FormErrorNotification(
                    message = "Por favor completa los campos obligatorios.",
                    onDismiss = { showError = false }
                )
            }
            if (showDeleteSuccess) {
                FormSuccessNotification(
                    message = "Sus datos han sido eliminados correctamente.",
                    onDismiss = { showDeleteSuccess = false }
                )
            }
        }
    }
}

@Composable
private fun MorphologyTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = AppSurfaceAlt,
            border = BorderStroke(1.dp, AppBorder)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = AppTextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = "Informacion de morfologia",
                style = MaterialTheme.typography.headlineSmall,
                color = AppTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Actualiza tus medidas y registros corporales.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTextSecondary
            )
        }
    }
}

@Composable
private fun MorphologyHeroCard(
    estatura: String,
    peso: String,
    imc: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(AppSurfaceAlt, AppSurface)
                    )
                )
                .border(1.dp, AppBorder.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                Text(
                    text = "Resumen morfologico",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AppTextPrimary,
                    fontWeight = FontWeight.Bold
            )
            Text(
                text = "Consulta rapidamente las variables base del seguimiento corporal.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MorphologyMetricPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Straighten,
                    label = "Estatura",
                    value = if (estatura.isBlank()) "--" else "$estatura cm"
                )
                MorphologyMetricPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.MonitorWeight,
                    label = "Peso",
                    value = if (peso.isBlank()) "--" else "$peso kg"
                )
                MorphologyMetricPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CreditCard,
                    label = "IMC",
                    value = if (imc.isBlank()) "--" else imc
                )
            }
        }
    }
}

@Composable
private fun MorphologyMetricPill(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = AppSurfaceMuted
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(AppPrimarySoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                color = AppTextSecondary,
                fontSize = 11.sp
            )
            Text(
                text = value,
                color = AppTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun MorphologySectionCard(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = AppSurface,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = AppTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTextSecondary
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppTextSecondary
                )
            }
            if (expanded) {
                content()
            }
        }
    }
}

@Composable
private fun MorphologyHintPill(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = AppPrimarySoft
    ) {
        Text(
            text = text,
            color = AppTextPrimary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun MorphologyBottomBar(
    isSaving: Boolean,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurface,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Guardar cambios",
                color = AppTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Actualiza la informacion visible de morfologia.",
                color = AppTextSecondary,
                fontSize = 12.sp
            )
            Button(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Guardando...",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "Actualizar morfologia",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun cargarPerimetroDesdeModelo(fecha: String, medida: String): PerimetroMedicion {
    return PerimetroMedicion(
        fecha = normalizarFechaParaUI(fecha),
        medida = medida
    )
}

private fun normalizarFechaParaUI(fechaRaw: String): String {
    if (fechaRaw.isBlank()) return ""
    if (fechaRaw.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) return fechaRaw

    val formatosEntrada = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    )

    for (formato in formatosEntrada) {
        val parsed = runCatching {
            SimpleDateFormat(formato, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }.parse(fechaRaw)
        }.getOrNull()

        if (parsed != null) {
            return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(parsed)
        }
    }

    return fechaRaw
}

private fun fechaUIaIso(fecha: String): String {
    if (fecha.isBlank()) return ""
    if (fecha.contains("T")) return fecha

    val parsed = runCatching {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            isLenient = false
        }.parse(fecha)
    }.getOrNull() ?: return fecha

    return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(parsed)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PerimetroItem(
    nombre: String,
    medicion: PerimetroMedicion,
    isError: Boolean = false,
    onDeleteSuccess: () -> Unit,
    onChange: (PerimetroMedicion) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(medicion.fecha.isNotBlank() || medicion.medida.isNotBlank()) }
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            onChange(medicion.copy(fecha = sdf.format(Date(millis))))
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Aceptar", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDatePicker = false },
                    border = BorderStroke(1.dp, PrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancelar", color = PrimaryBlue)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = AppSurface)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = AppSurface,
                    titleContentColor = AppTextPrimary,
                    headlineContentColor = PrimaryBlue,
                    weekdayContentColor = AppTextSecondary,
                    navigationContentColor = PrimaryBlue,
                    yearContentColor = AppTextPrimary,
                    currentYearContentColor = PrimaryBlue,
                    selectedYearContainerColor = PrimaryBlue,
                    selectedYearContentColor = Color.White,
                    dayContentColor = AppTextPrimary,
                    selectedDayContainerColor = PrimaryBlue,
                    selectedDayContentColor = Color.White,
                    todayContentColor = PrimaryBlue,
                    todayDateBorderColor = PrimaryBlue
                )
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = AppSurface,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Eliminar registro",
                    color = AppTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Se eliminaran la fecha y la medida registradas para $nombre.",
                    color = AppTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onChange(PerimetroMedicion())
                        showDeleteDialog = false
                        onDeleteSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    border = BorderStroke(1.dp, AppBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancelar", color = AppTextPrimary)
                }
            }
        )
    }

    val medidaInteraction = remember { MutableInteractionSource() }
    val medidaFocused by medidaInteraction.collectIsFocusedAsState()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = AppSurfaceAlt,
        border = BorderStroke(
            width = 1.dp,
            color = if (isError) AccentRed else AppBorder.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(AppPrimarySoft, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Straighten,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = nombre,
                            color = AppTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = when {
                                medicion.fecha.isBlank() && medicion.medida.isBlank() ->
                                    "Sin registro"
                                medicion.fecha.isBlank() ->
                                    "Pendiente de fecha"
                                medicion.medida.isBlank() ->
                                    "Fecha registrada, falta medida"
                                else ->
                                    medicion.fecha
                            },
                            color = if (isError) AccentRed else AppTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                if (medicion.medida.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = AppSurfaceMuted
                    ) {
                        Text(
                            text = "${medicion.medida} cm",
                            color = AppTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0x26FF8A00), CircleShape)
                        .clickable { showDeleteDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color(0xFFFFA726),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppTextSecondary
                )
            }

            if (expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.25f)
                            .clickable { showDatePicker = true }
                    ) {
                        OutlinedTextField(
                            value = medicion.fecha,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { MorphologyFieldLabel(text = "Fecha") },
                            placeholder = { MorphologyPlaceholder("Seleccionar") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = AppTextSecondary
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = FormFieldShape,
                            colors = formReadOnlyColors()
                        )
                    }

                    OutlinedTextField(
                            value = medicion.medida,
                            onValueChange = { valor ->
                                val filtered = valor.filter { it.isDigit() || it == '.' }
                                onChange(medicion.copy(medida = filtered))
                            },
                            isError = isError,
                            enabled = medicion.fecha.isNotBlank(),
                            label = { MorphologyFieldLabel(text = "Cm") },
                            placeholder = { MorphologyPlaceholder("--") },
                            interactionSource = medidaInteraction,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Straighten,
                                    contentDescription = null,
                                    tint = when {
                                        isError -> AccentRed
                                        medidaFocused -> PrimaryBlue
                                        else -> AppTextSecondary
                                    }
                                )
                            },
                            trailingIcon = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Incrementar",
                                        tint = if (medicion.fecha.isNotBlank()) PrimaryBlue else AppTextSecondary.copy(alpha = 0.4f),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable(enabled = medicion.fecha.isNotBlank()) {
                                                val actual = medicion.medida.toDoubleOrNull() ?: 0.0
                                                onChange(medicion.copy(medida = String.format(Locale.US, "%.0f", actual + 1)))
                                            }
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Disminuir",
                                        tint = if (medicion.fecha.isNotBlank()) PrimaryBlue else AppTextSecondary.copy(alpha = 0.4f),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable(enabled = medicion.fecha.isNotBlank()) {
                                                val actual = medicion.medida.toDoubleOrNull() ?: 0.0
                                                onChange(medicion.copy(medida = String.format(Locale.US, "%.0f", actual - 1)))
                                            }
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(0.75f)
                                .height(64.dp),
                            shape = FormFieldShape,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = formFieldColors()
                        )
                }

                MorphologyFieldError(
                    show = isError,
                    message = "Registra una medida cuando exista fecha."
                )
            }
        }
    }
}

@Composable
private fun MorphologyDropdownItem(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    androidx.compose.material3.DropdownMenuItem(
        text = {
            Text(
                text = text,
                color = when {
                    !enabled -> AppTextSecondary.copy(alpha = 0.6f)
                    selected -> AppTextPrimary
                    else -> AppTextSecondary
                },
                fontSize = 14.sp
            )
        },
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) PrimaryBlue.copy(alpha = 0.14f) else Color.Transparent
            )
    )
}

@Composable
private fun MorphologyFieldLabel(
    text: String,
    required: Boolean = false,
    info: String? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = buildAnnotatedString {
                append(text)
                if (required) {
                    append(" ")
                    withStyle(SpanStyle(color = AccentRed)) { append("*") }
                }
            },
            fontSize = 13.sp,
            color = AppTextSecondary
        )
        if (info != null) {
            Spacer(modifier = Modifier.width(3.dp))
            FormTooltip(info)
        }
    }
}

@Composable
private fun MorphologyPlaceholder(text: String) {
    Text(text = text, color = AppTextSecondary.copy(alpha = 0.75f))
}

@Composable
private fun MorphologyFieldError(show: Boolean, message: String) {
    if (!show) return
    Text(
        text = message,
        color = AccentRed,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
    )
}

@Composable
private fun MorphologyFieldSpacer() {
    Spacer(modifier = Modifier.height(2.dp))
}
