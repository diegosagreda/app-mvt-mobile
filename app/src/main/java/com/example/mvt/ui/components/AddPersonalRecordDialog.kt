package com.example.mvt.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.data.firebase.models.DistanceOption
import com.example.mvt.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ==========================================
// COLORES INTERNOS
// ==========================================
private val FieldBorderColor  = Color(0xFFD6D6D6)
private val FieldTextColor    = Color(0xFF333333)
private val LabelColor        = Color(0xFF1F2A44)
private val PlaceholderColor  = Color(0xFFBDBDBD)

// ==========================================
// DIALOG PRINCIPAL
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonalRecordDialog(
    distanceOptions: List<DistanceOption>,
    onDismiss: () -> Unit,
    // onSave recibe todos los datos para que el llamador los guarde
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

    // Para cuando el usuario elige "Otro"
    var otraDistancia  by rememberSaveable { mutableStateOf("") }
    var unidad         by rememberSaveable { mutableStateOf("km") }
    var expandedUnidad by remember { mutableStateOf(false) }

    // === Errores de validación ===
    var distanciaError by remember { mutableStateOf(false) }
    var fechaError     by remember { mutableStateOf(false) }
    var tiempoError    by remember { mutableStateOf(false) }

// Distancia normalizada a km para el cálculo del ritmo
    val distanciaParaRitmo = when {
        selectedDistance?.name == "Otro" && unidad == "m" ->
            // metros → convertir a km para la fórmula de ritmo
            (otraDistancia.toDoubleOrNull() ?: 0.0) / 1000.0
        selectedDistance?.name == "Otro" ->
            // km directo
            otraDistancia.toDoubleOrNull() ?: 0.0
        else ->
            // Distancias fijas ya están en km en Firebase
            selectedDistance?.value?.toDoubleOrNull() ?: 0.0
    }

    val ritmoCalculado = calcularRitmo(tiempoH, tiempoM, tiempoS, distanciaParaRitmo)

    // === DatePicker — límite hasta día actual ===
    val todayMaxUtcMillis = remember {
        val localNow = Calendar.getInstance()
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(
                localNow.get(Calendar.YEAR),
                localNow.get(Calendar.MONTH),
                localNow.get(Calendar.DAY_OF_MONTH),
                23, 59, 59
            )
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    val selectableDates = remember(todayMaxUtcMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                utcTimeMillis <= todayMaxUtcMillis
            override fun isSelectableYear(year: Int) = true
        }
    }

    // FIX: estado del DatePicker creado aquí arriba para que no se reinicie
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = parseDateToMillis(fechaSeleccionada),
        selectableDates = selectableDates
    )

    // ==========================================
    // DATEPICKER — fuera del AlertDialog
    // para evitar conflictos de z-index
    // ==========================================
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
                    border  = BorderStroke(1.dp, PrimaryBlue),
                    shape   = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancelar", color = PrimaryBlue)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Color.White)
        ) {
            DatePicker(
                state          = datePickerState,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor             = Color.White,
                    titleContentColor          = PrimaryBlue,
                    headlineContentColor       = PrimaryBlue,
                    weekdayContentColor        = Color(0xFF555B61),
                    navigationContentColor     = PrimaryBlue,
                    yearContentColor           = Color(0xFF2B2E34),
                    currentYearContentColor    = PrimaryBlue,
                    selectedYearContainerColor = PrimaryBlue,
                    selectedYearContentColor   = Color.White,
                    dayContentColor            = Color(0xFF2B2E34),
                    selectedDayContainerColor  = PrimaryBlue,
                    selectedDayContentColor    = Color.White,
                    todayContentColor          = PrimaryBlue,
                    todayDateBorderColor       = PrimaryBlue
                )
            )
        }
    }

    // ==========================================
    // DIALOG PRINCIPAL
    // ==========================================
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(20.dp),
        modifier         = Modifier.fillMaxWidth(),
        title = {
            Text(
                text       = "Registro nueva marca",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = LabelColor
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                // ==========================================
                // DISTANCIA
                // ==========================================
                DialogLabel(text = "Distancia", required = true)
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded         = expandedDistance,
                    onExpandedChange = { expandedDistance = !expandedDistance }
                ) {
                    OutlinedTextField(
                        value         = selectedDistance?.name ?: "Seleccione Uno",
                        onValueChange = {},
                        readOnly      = true,
                        isError       = distanciaError,
                        modifier      = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        trailingIcon  = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDistance)
                        },
                        shape  = RoundedCornerShape(14.dp),
                        colors = dialogFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded         = expandedDistance,
                        onDismissRequest = { expandedDistance = false },
                        containerColor   = Color.White,
                        shadowElevation  = 8.dp,
                        shape            = RoundedCornerShape(14.dp)
                    ) {
                        distanceOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text  = option.name,
                                        color = FieldTextColor
                                    )
                                },
                                onClick = {
                                    selectedDistance = option
                                    distanciaError   = false
                                    expandedDistance = false
                                }
                            )
                        }
                    }
                }
                if (distanciaError) {
                    DialogErrorText("Selecciona una distancia")
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ==========================================
                // FECHA — FIX: Box clickeable encima del TextField
                // ==========================================
                DialogLabel(text = "Fecha", required = true)
                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value         = fechaSeleccionada,
                        onValueChange = {},
                        readOnly      = true,
                        enabled       = false,
                        isError       = fechaError,
                        placeholder   = { Text("Seleccione fecha", color = PlaceholderColor) },
                        leadingIcon   = {
                            Icon(
                                imageVector        = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint               = PrimaryBlue
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor      = if (fechaError) Color.Red else FieldBorderColor,
                            disabledContainerColor   = Color.White,
                            disabledTextColor        = FieldTextColor,
                            disabledPlaceholderColor = PlaceholderColor,
                            disabledLeadingIconColor = PrimaryBlue
                        )
                    )
                    // Box transparente encima para capturar el click
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }
                if (fechaError) {
                    DialogErrorText("Selecciona una fecha")
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ==========================================
                // TIEMPO MARCA
                // ==========================================
                DialogLabel(text = "Tiempo Marca", required = true)
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DialogNumberField(
                        value         = tiempoH,
                        onValueChange = { tiempoH = it; tiempoError = false },
                        placeholder   = "HH"
                    )
                    DialogNumberField(
                        value         = tiempoM,
                        onValueChange = { tiempoM = it; tiempoError = false },
                        placeholder   = "MM"
                    )
                    DialogNumberField(
                        value         = tiempoS,
                        onValueChange = { tiempoS = it; tiempoError = false },
                        placeholder   = "SS"
                    )
                }
                if (tiempoError) {
                    DialogErrorText("Ingresa el tiempo completo (HH, MM, SS)")
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ==========================================
                // RITMO — solo lectura, calculado
                // ==========================================
                DialogLabel(text = "Ritmo")
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value         = ritmoCalculado,
                    onValueChange = {},
                    enabled       = false,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(14.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor    = FieldBorderColor,
                        disabledContainerColor = Color.White,
                        disabledTextColor      = FieldTextColor
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // ==========================================
                // FC PROMEDIO
                // ==========================================
                DialogLabel(text = "FC Promedio")
                Spacer(modifier = Modifier.height(6.dp))

                DialogNumberField(
                    value         = fcPromedio,
                    onValueChange = { fcPromedio = it },
                    placeholder   = "FC Promedio"
                )

                // ==========================================
                // OTRO — campo extra si selecciona "Otro"
                // ==========================================
                if (selectedDistance?.name == "Otro") {
                    Spacer(modifier = Modifier.height(18.dp))

                    DialogLabel(text = "Otra Distancia")
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = otraDistancia,
                        onValueChange = {
                            if (it.all { c -> c.isDigit() || c == '.' }) otraDistancia = it
                        },
                        placeholder = { Text("Ej: 3.5", color = PlaceholderColor) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier   = Modifier.fillMaxWidth(),
                        shape      = RoundedCornerShape(14.dp),
                        colors     = dialogFieldColors()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    DialogLabel(text = "Unidad")
                    Spacer(modifier = Modifier.height(6.dp))

                    ExposedDropdownMenuBox(
                        expanded         = expandedUnidad,
                        onExpandedChange = { expandedUnidad = !expandedUnidad }
                    ) {
                        OutlinedTextField(
                            value         = unidad,
                            onValueChange = {},
                            readOnly      = true,
                            modifier      = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            trailingIcon  = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUnidad)
                            },
                            shape  = RoundedCornerShape(14.dp),
                            colors = dialogFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded         = expandedUnidad,
                            onDismissRequest = { expandedUnidad = false },
                            containerColor   = Color.White,
                            shape            = RoundedCornerShape(14.dp)
                        ) {
                            listOf("km", "m").forEach { u ->
                                DropdownMenuItem(
                                    text    = { Text(u, color = FieldTextColor) },
                                    onClick = { unidad = u; expandedUnidad = false }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        },
        confirmButton = {
            // Botón habilitado solo cuando los obligatorios están completos
            val camposCompletos =
                selectedDistance != null &&
                        fechaSeleccionada.isNotBlank() &&
                        tiempoH.isNotBlank() &&
                        tiempoM.isNotBlank() &&
                        tiempoS.isNotBlank() &&
                        (
                                selectedDistance?.name != "Otro" ||
                                        otraDistancia.isNotBlank()
                                )
            Button(
                onClick = {
                    // FIX #5: guardar distancia correcta
                    val distanciaFinal = when {
                        selectedDistance?.name == "Otro" && unidad == "m" ->
                            // Convertir metros a km antes de guardar
                            String.format(
                                "%.3f",
                                (otraDistancia.toDoubleOrNull() ?: 0.0) / 1000.0
                            )
                        selectedDistance?.name == "Otro" ->
                            otraDistancia
                        else ->
                            // Guardar el nombre de la distancia (1500, 10K, etc.)
                            selectedDistance!!.name
                    }
                    onSave(
                        distanciaFinal,
                        fechaSeleccionada,
                        tiempoH,
                        tiempoM,
                        tiempoS,
                        ritmoCalculado,
                        fcPromedio
                    )
                },
                enabled = camposCompletos,  // ← deshabilitado hasta completar
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    disabledContainerColor = PrimaryBlue.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Guardar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar", color = FieldTextColor)
            }
        }
    )
}

// ==========================================
// HELPERS PRIVADOS
// ==========================================

@Composable
private fun DialogLabel(text: String, required: Boolean = false) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, fontSize = 15.sp, color = LabelColor)
        if (required) {
            Text(text = " *", color = Color.Red, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DialogErrorText(text: String) {
    Text(
        text     = "*$text",
        color    = Color.Red,
        fontSize = 12.sp,
        modifier = androidx.compose.ui.Modifier.padding(start = 4.dp, top = 2.dp)
    )
}

@Composable
private fun DialogNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = { if (it.all { c -> c.isDigit() }) onValueChange(it) },
        placeholder     = { Text(placeholder, color = PlaceholderColor) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine      = true,
        modifier        = androidx.compose.ui.Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(14.dp),
        colors          = dialogFieldColors()
    )
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor    = PrimaryBlue,
    unfocusedBorderColor  = FieldBorderColor,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedTextColor      = FieldTextColor,
    unfocusedTextColor    = FieldTextColor,
    cursorColor           = PrimaryBlue
)

private fun parseDateToMillis(dateText: String): Long? {
    if (dateText.isBlank()) return null
    return try {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            .apply { timeZone = TimeZone.getDefault() }
            .parse(dateText)?.time
    } catch (e: Exception) { null }
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

    return String.format("%d:%02d", ritmoMinutos, ritmoSecondsR)
}