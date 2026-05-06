package com.example.mvt.ui.screens.personaldata

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mvt.ui.components.FormErrorNotification
import com.example.mvt.ui.components.FormLabel
import com.example.mvt.ui.components.FormSpacer
import com.example.mvt.ui.components.FormSuccessNotification
import com.example.mvt.ui.components.formFieldColors
import com.example.mvt.ui.theme.PrimaryBlue
import com.example.mvt.ui.viewmodels.PhysicalCapacityUiState
import com.example.mvt.ui.viewmodels.PhysicalCapacityViewModel

@Composable
fun PhysicalCapacityScreen(
    navController: NavController,
    viewModel: PhysicalCapacityViewModel
) {
    // === Observar ViewModel ===
    val fcMinData by viewModel.fcMin.collectAsState()
    val fcMaxData by viewModel.fcMax.collectAsState()
    val uiState   by viewModel.uiState.collectAsState()

    // === Estados campos editables ===
    var fcMin by remember { mutableStateOf("") }
    var fcMax by remember { mutableStateOf("") }

    // === Estados UI ===
    var showSuccess by remember { mutableStateOf(false) }
    var showError   by remember { mutableStateOf(false) }

    // === Errores de validación ===
    var fcMinError by remember { mutableStateOf(false) }
    var fcMaxError by remember { mutableStateOf(false) }

    // === Cargar al entrar ===
    LaunchedEffect(Unit) {
        viewModel.loadPhysicalCapacity()
    }

    // === Sincronizar cuando llegan los datos ===
    LaunchedEffect(fcMinData, fcMaxData) {
        fcMin = fcMinData
        fcMax = fcMaxData
    }

    // === Reaccionar a cambios de estado ===
    LaunchedEffect(uiState) {
        when (uiState) {
            is PhysicalCapacityUiState.Saved -> {
                showSuccess = true
                viewModel.resetState()
            }
            is PhysicalCapacityUiState.Error -> {
                showError = true
                viewModel.resetState()
            }
            else -> {}
        }
    }

    val isLoading   = uiState is PhysicalCapacityUiState.Loading
    val scrollState = rememberScrollState()

    // === Loader ===
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
        return
    }

    // === InteractionSources para íconos ===
    val fcMinInteraction = remember { MutableInteractionSource() }
    val fcMaxInteraction = remember { MutableInteractionSource() }
    val fcMinFocused by fcMinInteraction.collectIsFocusedAsState()
    val fcMaxFocused by fcMaxInteraction.collectIsFocusedAsState()

    // === Guardar ===
    fun guardar() {
        val fcMinNum = fcMin.toDoubleOrNull()
        val fcMaxNum = fcMax.toDoubleOrNull()

        fcMinError = fcMinNum == null || fcMinNum < 35 || fcMinNum > 80
        fcMaxError = fcMaxNum == null || fcMaxNum < 150 || fcMaxNum > 220

        if (fcMinError || fcMaxError) {
            showError = true
            return
        }

        viewModel.savePhysicalCapacity(fcMin, fcMax)
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
            // FIX separación: Row en vez de Box para controlar
            // el espacio entre flecha y título
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

                // Espacio entre flecha y título
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Información Capacidad Física",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Indicador obligatorio ---
            Text(
                text = buildAnnotatedString {
                    append("Obligatorio ")
                    withStyle(SpanStyle(color = Color.Red)) { append("*") }
                },
                fontSize = 13.sp,
                color = Color(0xFF555B61)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // FRECUENCIA MÍNIMA
            // ==========================================
            FormLabel(
                text     = "Frecuencia Mínima",
                required = true,
                info     = "Pulsaciones de tu corazón cuando te despiertas"
            )
            OutlinedTextField(
                value = fcMin,
                onValueChange = {
                    val filtered = it.filter { c -> c.isDigit() || c == '.' }
                    fcMin = filtered
                    val valor = filtered.toDoubleOrNull()
                    fcMinError = valor == null || valor < 35 || valor > 80
                },
                isError           = fcMinError,
                placeholder       = { Text("FCmin") },
                interactionSource = fcMinInteraction,
                // FIX ícono: Person en vez de FavoriteBorder
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (fcMinFocused) PrimaryBlue else Color(0xFF888888)
                    )
                },
                // FIX flechitas dentro del input igual que morphology
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
                                    val actual = fcMin.toDoubleOrNull() ?: 35.0
                                    val nuevo  = (actual + 1).coerceIn(35.0, 80.0)
                                    fcMin      = String.format("%.0f", nuevo)
                                    fcMinError = false
                                }
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Bajar",
                            tint = PrimaryBlue,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    val actual = fcMin.toDoubleOrNull() ?: 35.0
                                    val nuevo  = (actual - 1).coerceIn(35.0, 80.0)
                                    fcMin      = String.format("%.0f", nuevo)
                                    fcMinError = false
                                }
                        )
                    }
                },
                singleLine     = true,
                modifier       = Modifier.fillMaxWidth(),
                shape          = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors         = formFieldColors()
            )
            if (fcMinError) {
                Text(
                    text     = "*Digita un valor entre 35 a 80",
                    color    = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }

            FormSpacer()

            // ==========================================
            // FRECUENCIA MÁXIMA
            // ==========================================
            FormLabel(
                text     = "Frecuencia Máxima",
                required = true,
                info     = "Máximas pulsaciones de tu corazón, sino sabes puedes poner 220 - tu edad"
            )
            OutlinedTextField(
                value = fcMax,
                onValueChange = {
                    val filtered = it.filter { c -> c.isDigit() || c == '.' }
                    fcMax = filtered
                    val valor = filtered.toDoubleOrNull()
                    fcMaxError = valor == null || valor < 150 || valor > 220
                },
                isError           = fcMaxError,
                placeholder       = { Text("FCmax") },
                interactionSource = fcMaxInteraction,
                // FIX ícono: Person en vez de FavoriteBorder
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (fcMaxFocused) PrimaryBlue else Color(0xFF888888)
                    )
                },
                // FIX flechitas dentro del input igual que morphology
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
                                    val actual = fcMax.toDoubleOrNull() ?: 150.0
                                    val nuevo  = (actual + 1).coerceIn(150.0, 220.0)
                                    fcMax      = String.format("%.0f", nuevo)
                                    fcMaxError = false
                                }
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Bajar",
                            tint = PrimaryBlue,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    val actual = fcMax.toDoubleOrNull() ?: 150.0
                                    val nuevo  = (actual - 1).coerceIn(150.0, 220.0)
                                    fcMax      = String.format("%.0f", nuevo)
                                    fcMaxError = false
                                }
                        )
                    }
                },
                singleLine      = true,
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors          = formFieldColors()
            )
            if (fcMaxError) {
                Text(
                    text     = "*Digita un valor entre 150 a 220",
                    color    = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
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
                modifier        = Modifier
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

        // === Notificaciones ===
        if (showSuccess) {
            FormSuccessNotification(
                message   = "¡Sus cambios han sido guardados con éxito!",
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