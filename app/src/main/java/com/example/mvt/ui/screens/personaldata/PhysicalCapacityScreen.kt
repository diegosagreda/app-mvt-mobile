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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mvt.ui.components.FormErrorNotification
import com.example.mvt.ui.components.FormFieldShape
import com.example.mvt.ui.components.FormSuccessNotification
import com.example.mvt.ui.components.FormTooltip
import com.example.mvt.ui.components.formFieldColors
import com.example.mvt.ui.theme.AccentRed
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppPrimarySoft
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import com.example.mvt.ui.viewmodels.PhysicalCapacityUiState
import com.example.mvt.ui.viewmodels.PhysicalCapacityViewModel

@Composable
fun PhysicalCapacityScreen(
    navController: NavController,
    viewModel: PhysicalCapacityViewModel
) {
    val fcMinData by viewModel.fcMin.collectAsState()
    val fcMaxData by viewModel.fcMax.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var fcMin by remember { mutableStateOf("") }
    var fcMax by remember { mutableStateOf("") }

    var showSuccess by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var heartRateSectionExpanded by remember { mutableStateOf(true) }

    var fcMinError by remember { mutableStateOf(false) }
    var fcMaxError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadPhysicalCapacity()
    }

    LaunchedEffect(fcMinData, fcMaxData) {
        fcMin = fcMinData
        fcMax = fcMaxData
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is PhysicalCapacityUiState.Saved -> {
                isSaving = false
                showSuccess = true
                viewModel.resetState()
            }

            is PhysicalCapacityUiState.Error -> {
                isSaving = false
                showError = true
                viewModel.resetState()
            }

            else -> Unit
        }
    }

    val hasPhysicalCapacityChanges =
        fcMin != fcMinData ||
            fcMax != fcMaxData

    val isLoading = uiState is PhysicalCapacityUiState.Loading
    val scrollState = rememberScrollState()

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

    fun guardar() {
        val fcMinNum = fcMin.toIntOrNull()
        val fcMaxNum = fcMax.toIntOrNull()

        fcMinError = fcMinNum == null || fcMinNum < 35 || fcMinNum > 80
        fcMaxError = fcMaxNum == null || fcMaxNum < 150 || fcMaxNum > 220

        if (fcMinError || fcMaxError) {
            showError = true
            return
        }

        isSaving = true
        viewModel.savePhysicalCapacity(fcMin, fcMax)
    }

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (hasPhysicalCapacityChanges) {
                PhysicalCapacityBottomBar(
                    isSaving = isSaving,
                    onSave = { guardar() }
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
                PhysicalCapacityTopBar(
                    onBack = {
                        navController.navigate("routines") {
                            popUpTo("routines") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                PhysicalCapacityHeroCard(
                    fcMin = fcMin,
                    fcMax = fcMax
                )

                PhysicalCapacitySectionCard(
                    title = "Frecuencia cardiaca",
                    subtitle = "Variables base para trabajo cardiovascular y zonas de esfuerzo.",
                    expanded = heartRateSectionExpanded,
                    onToggle = { heartRateSectionExpanded = !heartRateSectionExpanded }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeartRateField(
                            modifier = Modifier.weight(1f),
                            value = fcMin,
                            onValueChange = { value ->
                                fcMin = value
                                val parsed = value.toIntOrNull()
                                fcMinError = parsed == null || parsed < 35 || parsed > 80
                            },
                            label = "FC minima",
                            placeholder = "FCmin",
                            info = "Pulsaciones del corazon en reposo al despertar.",
                            isError = fcMinError,
                            minValue = 35,
                            maxValue = 80,
                            onStepChange = { value ->
                                fcMin = value
                                fcMinError = false
                            },
                            icon = Icons.Default.FavoriteBorder
                        )
                        HeartRateField(
                            modifier = Modifier.weight(1f),
                            value = fcMax,
                            onValueChange = { value ->
                                fcMax = value
                                val parsed = value.toIntOrNull()
                                fcMaxError = parsed == null || parsed < 150 || parsed > 220
                            },
                            label = "FC maxima",
                            placeholder = "FCmax",
                            info = "Si no la conoces, puedes estimarla con 220 menos tu edad.",
                            isError = fcMaxError,
                            minValue = 150,
                            maxValue = 220,
                            onStepChange = { value ->
                                fcMax = value
                                fcMaxError = false
                            },
                            icon = Icons.Default.Favorite
                        )
                    }

                    PhysicalCapacityFieldError(
                        show = fcMinError,
                        message = "Digita un valor entre 35 y 80."
                    )
                    PhysicalCapacityFieldError(
                        show = fcMaxError,
                        message = "Digita un valor entre 150 y 220."
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
                    message = "Por favor completa los campos obligatorios correctamente.",
                    onDismiss = { showError = false }
                )
            }
        }
    }
}

@Composable
private fun PhysicalCapacityTopBar(onBack: () -> Unit) {
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
                text = "Informacion de capacidad fisica",
                style = MaterialTheme.typography.headlineSmall,
                color = AppTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Actualiza las referencias cardiacas del atleta.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTextSecondary
            )
        }
    }
}

@Composable
private fun PhysicalCapacityHeroCard(
    fcMin: String,
    fcMax: String
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
                text = "Resumen cardiovascular",
                style = MaterialTheme.typography.headlineSmall,
                color = AppTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Consulta rapidamente los pulsos de referencia para seguimiento fisico.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PhysicalCapacityMetricPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.FavoriteBorder,
                    label = "FC minima",
                    value = if (fcMin.isBlank()) "--" else "$fcMin ppm"
                )
                PhysicalCapacityMetricPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Favorite,
                    label = "FC maxima",
                    value = if (fcMax.isBlank()) "--" else "$fcMax ppm"
                )
            }
        }
    }
}

@Composable
private fun PhysicalCapacityMetricPill(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = AppSurfaceAlt
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
private fun PhysicalCapacitySectionCard(
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
private fun PhysicalCapacityHintPill(text: String) {
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
private fun PhysicalCapacityBottomBar(
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
                text = "Actualiza la informacion visible de capacidad fisica.",
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
                        text = "Actualizar capacidad fisica",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun HeartRateField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    info: String,
    isError: Boolean,
    minValue: Int,
    maxValue: Int,
    onStepChange: (String) -> Unit,
    icon: ImageVector
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    OutlinedTextField(
            value = value,
            onValueChange = { input ->
                onValueChange(input.filter { it.isDigit() })
            },
            isError = isError,
            label = {
                PhysicalCapacityFieldLabel(
                    text = label,
                    required = true,
                    info = info
                )
            },
            placeholder = { PhysicalCapacityPlaceholder(placeholder) },
            interactionSource = interactionSource,
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = when {
                        isError -> AccentRed
                        isFocused -> PrimaryBlue
                        else -> AppTextSecondary
                    }
                )
            },
            trailingIcon = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Incrementar",
                        tint = PrimaryBlue,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                val current = value.toIntOrNull() ?: minValue
                                val next = (current + 1).coerceIn(minValue, maxValue)
                                onStepChange(next.toString())
                            }
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Disminuir",
                        tint = PrimaryBlue,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                val current = value.toIntOrNull() ?: minValue
                                val next = (current - 1).coerceIn(minValue, maxValue)
                                onStepChange(next.toString())
                            }
                    )
                }
            },
            singleLine = true,
            modifier = modifier.fillMaxWidth(),
            shape = FormFieldShape,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = formFieldColors()
        )
}

@Composable
private fun PhysicalCapacityFieldLabel(
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
private fun PhysicalCapacityPlaceholder(text: String) {
    Text(text = text, color = AppTextSecondary.copy(alpha = 0.75f))
}

@Composable
private fun PhysicalCapacityFieldError(show: Boolean, message: String) {
    if (!show) return
    Text(
        text = message,
        color = AccentRed,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
    )
}
