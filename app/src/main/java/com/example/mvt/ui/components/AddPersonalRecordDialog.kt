package com.example.mvt.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.data.firebase.models.DistanceOption
import com.example.mvt.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ==========================================
// DIALOG PRINCIPAL (DISEÑO OSCURO PREMIUM)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonalRecordDialog(
    distanceOptions: List<DistanceOption>,
    onDismiss: () -> Unit,
    onSave: (
        distancia: String,
        fecha: String,
        tiempoH: String,
        tiempoM: String,
        tiempoS: String,
        ritmo: String,
        fcProm: String
    ) -> Unit
) {
    // === Estados del formulario ===
    var expandedDistance   by remember { mutableStateOf(false) }
    var selectedDistance   by remember { mutableStateOf<DistanceOption?>(null) }

    var fechaSeleccionada  by rememberSaveable { mutableStateOf("") }
    var showDatePicker     by remember { mutableStateOf(false) }

    var tiempoH    by rememberSaveable { mutableStateOf("") }
    var tiempoM    by rememberSaveable { mutableStateOf("") }
    var tiempoS    by rememberSaveable { mutableStateOf("") }
    var fcPromedio by rememberSaveable { mutableStateOf("") }

    var otraDistancia  by rememberSaveable { mutableStateOf("") }
    var unidad         by rememberSaveable { mutableStateOf("km") }
    var expandedUnidad by remember { mutableStateOf(false) }

    // === Errores ===
    var distanciaError by remember { mutableStateOf(false) }
    var fechaError     by remember { mutableStateOf(false) }
    var tiempoError    by remember { mutableStateOf(false) }

    val distanciaParaRitmo = when {
        selectedDistance?.name == "Otro" && unidad == "m" ->
            (otraDistancia.toDoubleOrNull() ?: 0.0) / 1000.0
        selectedDistance?.name == "Otro" ->
            otraDistancia.toDoubleOrNull() ?: 0.0
        else ->
            selectedDistance?.value?.toDoubleOrNull() ?: 0.0
    }

    val ritmoCalculado = calcularRitmo(tiempoH, tiempoM, tiempoS, distanciaParaRitmo)

    val todayMaxUtcMillis = remember {
        val localNow = Calendar.getInstance()
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(localNow.get(Calendar.YEAR), localNow.get(Calendar.MONTH), localNow.get(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    val selectableDates = remember(todayMaxUtcMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMaxUtcMillis
            override fun isSelectableYear(year: Int) = true
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = parseDateToMillis(fechaSeleccionada),
        selectableDates = selectableDates
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            fechaSeleccionada = formatMillisToDate(millis)
                            fechaError = false
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape  = RoundedCornerShape(8.dp)
                ) {
                    Text("Aceptar", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDatePicker = false },
                    border  = BorderStroke(1.dp, AppBorder),
                    shape   = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancelar", color = AppTextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = AppSurface)
        ) {
            DatePicker(
                state          = datePickerState,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor             = AppSurface,
                    titleContentColor          = AppTextPrimary,
                    headlineContentColor       = AppTextPrimary,
                    weekdayContentColor        = AppTextSecondary,
                    navigationContentColor     = PrimaryBlue,
                    yearContentColor           = AppTextPrimary,
                    currentYearContentColor    = PrimaryBlue,
                    selectedYearContainerColor = PrimaryBlue,
                    selectedYearContentColor   = Color.White,
                    dayContentColor            = AppTextPrimary,
                    selectedDayContainerColor  = PrimaryBlue,
                    selectedDayContentColor    = Color.White,
                    todayContentColor          = PrimaryBlue,
                    todayDateBorderColor       = PrimaryBlue
                )
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        content = {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AppSurface,
                border = BorderStroke(1.dp, AppBorder),
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header con degradado
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(AppSurfaceAlt, AppSurface),
                                    startY = 0f, endY = Float.POSITIVE_INFINITY
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(AppPrimarySoft, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Registro Marca",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTextPrimary
                                )
                                Text(
                                    text = "Nueva marca personal",
                                    fontSize = 12.sp,
                                    color = AppTextSecondary
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        
                        // DISTANCIA
                        FormLabel(text = "Distancia", required = true)
                        ExposedDropdownMenuBox(
                            expanded = expandedDistance,
                            onExpandedChange = { expandedDistance = !expandedDistance }
                        ) {
                            OutlinedTextField(
                                value = selectedDistance?.name ?: "Seleccione marca",
                                onValueChange = {},
                                readOnly = true,
                                isError = distanciaError,
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDistance) },
                                leadingIcon = { Icon(Icons.Default.Straighten, null, tint = if(distanciaError) AppError else AppIconMuted, modifier = Modifier.size(20.dp)) },
                                shape = FormFieldShape,
                                colors = formFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedDistance,
                                onDismissRequest = { expandedDistance = false },
                                containerColor = AppSurface,
                                modifier = Modifier.background(AppSurface)
                            ) {
                                distanceOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(text = option.name, color = AppTextPrimary) },
                                        onClick = {
                                            selectedDistance = option
                                            distanciaError = false
                                            expandedDistance = false
                                        }
                                    )
                                }
                            }
                        }
                        if (distanciaError) DialogErrorText("Selecciona una distancia")

                        FormSpacer()

                        // FECHA
                        FormLabel(text = "Fecha", required = true)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = fechaSeleccionada,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                isError = fechaError,
                                placeholder = { Text("dd/mm/aaaa", color = AppTextSecondary) },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, null, tint = if(fechaError) AppError else PrimaryBlue, modifier = Modifier.size(20.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = FormFieldShape,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = if (fechaError) AppError else AppBorder,
                                    disabledContainerColor = Color.Transparent,
                                    disabledTextColor = AppTextPrimary,
                                    disabledPlaceholderColor = AppTextSecondary,
                                    disabledLeadingIconColor = PrimaryBlue
                                )
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                        }
                        if (fechaError) DialogErrorText("Selecciona una fecha")

                        FormSpacer()

                        // TIEMPO MARCA
                        FormLabel(text = "Tiempo Marca", required = true)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                DialogNumberField(
                                    value = tiempoH,
                                    onValueChange = { tiempoH = it; tiempoError = false },
                                    placeholder = "HH",
                                    leadingIcon = Icons.Default.Timer
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                DialogNumberField(
                                    value = tiempoM,
                                    onValueChange = { tiempoM = it; tiempoError = false },
                                    placeholder = "MM"
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                DialogNumberField(
                                    value = tiempoS,
                                    onValueChange = { tiempoS = it; tiempoError = false },
                                    placeholder = "SS"
                                )
                            }
                        }
                        if (tiempoError) DialogErrorText("Ingresa el tiempo completo")

                        FormSpacer()

                        // RITMO Y FC PROMEDIO en fila
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                FormLabel(text = "Ritmo")
                                OutlinedTextField(
                                    value = ritmoCalculado,
                                    onValueChange = {},
                                    enabled = false,
                                    leadingIcon = { Icon(Icons.Default.Speed, null, tint = AppIconMuted, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = FormFieldShape,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledBorderColor = AppBorder,
                                        disabledContainerColor = AppSurfaceMuted,
                                        disabledTextColor = AppTextPrimary,
                                        disabledLeadingIconColor = AppIconMuted
                                    )
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                FormLabel(text = "FC Prom")
                                DialogNumberField(
                                    value = fcPromedio,
                                    onValueChange = { fcPromedio = it },
                                    placeholder = "ppm",
                                    leadingIcon = Icons.Default.Favorite
                                )
                            }
                        }

                        // OTRO (si aplica)
                        if (selectedDistance?.name == "Otro") {
                            FormSpacer()
                            FormLabel(text = "Otra Distancia", required = true)
                            OutlinedTextField(
                                value = otraDistancia,
                                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) otraDistancia = it },
                                placeholder = { Text("Ej: 3.5", color = AppTextSecondary) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = FormFieldShape,
                                colors = formFieldColors()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FormLabel(text = "Unidad")
                            ExposedDropdownMenuBox(
                                expanded = expandedUnidad,
                                onExpandedChange = { expandedUnidad = !expandedUnidad }
                            ) {
                                OutlinedTextField(
                                    value = unidad,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUnidad) },
                                    shape = FormFieldShape,
                                    colors = formFieldColors()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedUnidad,
                                    onDismissRequest = { expandedUnidad = false },
                                    containerColor = AppSurface
                                ) {
                                    listOf("km", "m").forEach { u ->
                                        DropdownMenuItem(
                                            text = { Text(u, color = AppTextPrimary) },
                                            onClick = { unidad = u; expandedUnidad = false }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // BOTONES
                        val camposCompletos = selectedDistance != null &&
                                fechaSeleccionada.isNotBlank() &&
                                tiempoH.isNotBlank() &&
                                tiempoM.isNotBlank() &&
                                tiempoS.isNotBlank() &&
                                (selectedDistance?.name != "Otro" || otraDistancia.isNotBlank())

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, AppBorder)
                            ) {
                                Text("Cancelar", color = AppTextSecondary)
                            }
                            Button(
                                onClick = {
                                    val distanciaFinal = when {
                                        selectedDistance?.name == "Otro" && unidad == "m" ->
                                            String.format(Locale.getDefault(), "%.3f", (otraDistancia.toDoubleOrNull() ?: 0.0) / 1000.0)
                                        selectedDistance?.name == "Otro" -> otraDistancia
                                        else -> selectedDistance!!.name
                                    }
                                    onSave(distanciaFinal, fechaSeleccionada, tiempoH, tiempoM, tiempoS, ritmoCalculado, fcPromedio)
                                },
                                enabled = camposCompletos,
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryBlue,
                                    disabledContainerColor = PrimaryBlue.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    )
}

// ==========================================
// HELPERS PRIVADOS
// ==========================================

@Composable
private fun DialogErrorText(text: String) {
    Text(
        text     = "*$text",
        color    = AppError,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
    )
}

@Composable
private fun DialogNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = { if (it.all { c -> c.isDigit() }) onValueChange(it) },
        placeholder     = { Text(placeholder, color = AppTextSecondary, fontSize = 14.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        leadingIcon     = leadingIcon?.let { { Icon(it, null, tint = AppIconMuted, modifier = Modifier.size(18.dp)) } },
        singleLine      = true,
        modifier        = Modifier.fillMaxWidth(),
        shape           = FormFieldShape,
        colors          = formFieldColors()
    )
}

private fun parseDateToMillis(dateText: String): Long? {
    if (dateText.isBlank()) return null
    return try {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            .apply { timeZone = TimeZone.getDefault() }
            .parse(dateText)?.time
    } catch (_: Exception) { null }
}

private fun formatMillisToDate(millis: Long): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(millis))
}

private fun calcularRitmo(
    horas: String,
    minutos: String,
    segundos: String,
    distancia: Double
): String {
    if (horas.isBlank() && minutos.isBlank() && segundos.isBlank()) return "0:00"
    if (distancia <= 0.0) return "0:00"

    val h = horas.toIntOrNull()    ?: 0
    val m = minutos.toIntOrNull()  ?: 0
    val s = segundos.toIntOrNull() ?: 0

    val totalSegundos  = (h * 3600) + (m * 60) + s
    val ritmoSegundos  = totalSegundos / distancia
    val ritmoMinutos   = (ritmoSegundos / 60).toInt()
    val ritmoSecondsR  = (ritmoSegundos % 60).toInt()

    return String.format(Locale.getDefault(), "%d:%02d", ritmoMinutos, ritmoSecondsR)
}
