package com.example.mvt.ui.screens.personaldata

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Straighten
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
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.mvt.ui.theme.PrimaryBlue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    navController: NavController
) {
    val uid = remember {
        FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }

    // === Estados campos editables ===
    var estatura   by remember { mutableStateOf("") }
    var peso       by remember { mutableStateOf("") }
    var grasa      by remember { mutableStateOf("") }
    var imc        by remember { mutableStateOf("") }
    var somatotipo by remember { mutableStateOf("") }

    // === Estados UI ===
    var expandedSomatotipo by remember { mutableStateOf(false) }
    var showSuccess        by remember { mutableStateOf(false) }
    var showError          by remember { mutableStateOf(false) }
    var showDeleteSuccess by remember { mutableStateOf(false) }
    var isLoading          by remember { mutableStateOf(true) }

    // === Errores de validación ===
    var estaturaError  by remember { mutableStateOf(false) }
    var pesoError      by remember { mutableStateOf(false) }
    var grasaError     by remember { mutableStateOf(false) }
    var perimetroError by remember { mutableStateOf(false) }

    // === Estados perímetros ===
    var hombros     by remember { mutableStateOf(PerimetroMedicion()) }
    var pecho       by remember { mutableStateOf(PerimetroMedicion()) }
    var brazo       by remember { mutableStateOf(PerimetroMedicion()) }
    var cintura     by remember { mutableStateOf(PerimetroMedicion()) }
    var musloMedio  by remember { mutableStateOf(PerimetroMedicion()) }
    var gluteos     by remember { mutableStateOf(PerimetroMedicion()) }
    var pantorrilla by remember { mutableStateOf(PerimetroMedicion()) }

    val somatotipos = listOf(
        "Seleccione Uno",
        "1. Ectomorfo",
        "2. Mesomorfo",
        "3. Endomorfo"
    )

    val scrollState    = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // === Cargar datos desde Firebase ===
    LaunchedEffect(uid) {
        if (uid.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }
        try {
            val snapshot = FirebaseDatabase.getInstance()
                .getReference("Morfologias")
                .child(uid)
                .get()
                .await()

            // FIX 2: Leer también como Double/Long por si Firebase
            // guardó número en vez de String
            estatura   = leerCampoComoString(snapshot, "estatura")
            peso       = leerCampoComoString(snapshot, "peso")
            grasa      = leerCampoComoString(snapshot, "grasa")
            imc        = leerCampoComoString(snapshot, "IMC")
            somatotipo = leerCampoComoString(snapshot, "somatipo")

            hombros     = cargarPerimetro(snapshot, "fecha_hombros",     "medida_hombros")
            pecho       = cargarPerimetro(snapshot, "fecha_pecho",       "medida_pecho")
            brazo       = cargarPerimetro(snapshot, "fecha_brazo",       "medida_brazo")
            cintura     = cargarPerimetro(snapshot, "fecha_cintura",     "medida_cintura")
            musloMedio  = cargarPerimetro(snapshot, "fecha_muslo",       "medida_muslo")
            gluteos     = cargarPerimetro(snapshot, "fecha_gluteos",     "medida_gluteos")
            pantorrilla = cargarPerimetro(snapshot, "fecha_pantorrilla", "medida_pantorrilla")

        } catch (e: Exception) {
            // campos quedan vacíos
        } finally {
            isLoading = false
        }
    }

    // === Calcular IMC automáticamente ===
    LaunchedEffect(estatura, peso) {
        val estaturaNum = estatura.toDoubleOrNull()
        val pesoNum     = peso.toDoubleOrNull()
        imc = if (estaturaNum != null && pesoNum != null && estaturaNum > 0) {
            val metros = estaturaNum / 100.0
            String.format("%.2f", pesoNum / (metros * metros))
        } else ""
    }

    // === Guardar en Firebase ===
    fun guardarMorfologia() {
        val estaturaNum = estatura.toDoubleOrNull()
        val pesoNum     = peso.toDoubleOrNull()
        val grasaNum    = grasa.toDoubleOrNull()

        estaturaError  = estaturaNum == null || estaturaNum < 80 || estaturaNum > 220
        pesoError      = pesoNum == null || pesoNum < 31
        grasaError     = if (grasa.isBlank()) false
        else grasaNum == null || grasaNum < 4 || grasaNum > 50

        // FIX 1: Validar también medida vacía cuando fecha NO está vacía
        perimetroError = listOf(hombros, pecho, brazo, cintura, musloMedio, gluteos, pantorrilla)
            .any { it.fecha.isNotBlank() && it.medida.isBlank() }

        if (estaturaError || pesoError || grasaError || perimetroError) {
            showError = true
            return
        }

        coroutineScope.launch {
            try {
                val updates = mapOf(
                    "estatura"           to estatura,
                    "peso"               to peso,
                    "grasa"              to grasa,
                    "IMC"                to imc,
                    "somatipo"           to somatotipo,
                    "fecha_hombros"      to fechaUIaIso(hombros.fecha),
                    "medida_hombros"     to hombros.medida,
                    "fecha_pecho"        to fechaUIaIso(pecho.fecha),
                    "medida_pecho"       to pecho.medida,
                    "fecha_brazo"        to fechaUIaIso(brazo.fecha),
                    "medida_brazo"       to brazo.medida,
                    "fecha_cintura"      to fechaUIaIso(cintura.fecha),
                    "medida_cintura"     to cintura.medida,
                    "fecha_muslo"        to fechaUIaIso(musloMedio.fecha),
                    "medida_muslo"       to musloMedio.medida,
                    "fecha_gluteos"      to fechaUIaIso(gluteos.fecha),
                    "medida_gluteos"     to gluteos.medida,
                    "fecha_pantorrilla"  to fechaUIaIso(pantorrilla.fecha),
                    "medida_pantorrilla" to pantorrilla.medida
                )

                FirebaseDatabase.getInstance()
                    .getReference("Morfologias")
                    .child(uid)
                    .updateChildren(updates)
                    .await()

                showSuccess = true

            } catch (e: Exception) {
                showError = true
            }
        }
    }

    // === Loader ===
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
        return
    }

    // === UI principal ===
    Box(modifier = Modifier.fillMaxSize()) {
// === InteractionSources para iconos
        val estaturaInteraction  = remember { MutableInteractionSource() }
        val pesoInteraction      = remember { MutableInteractionSource() }
        val grasaInteraction     = remember { MutableInteractionSource() }

        val estaturaFocused by estaturaInteraction.collectIsFocusedAsState()
        val pesoFocused     by pesoInteraction.collectIsFocusedAsState()
        val grasaFocused    by grasaInteraction.collectIsFocusedAsState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = 80.dp)
        ) {

            // --- Header ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                IconButton(
                    // FIX 3: navega directo a routines limpiando el backstack
                    onClick = {
                        navController.navigate("routines") {
                            popUpTo("routines") { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = PrimaryBlue
                    )
                }
                Text(
                    text = "Información Morfología",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    modifier = Modifier.align(Alignment.Center)
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
            // ESTATURA
            // ==========================================
            MorphoLabel(text = "Estatura (cm)", required = true, info = "En centímetros")
            OutlinedTextField(
                value = estatura,
                onValueChange = {
                    val filtered = it.filter { c -> c.isDigit() || c == '.' }
                    estatura = filtered
                    val valor = filtered.toDoubleOrNull()
                    estaturaError = valor == null || valor < 80 || valor > 220
                },
                isError = estaturaError,
                placeholder = { Text("Estatura") },
                interactionSource = estaturaInteraction,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = if (estaturaFocused) PrimaryBlue else Color(0xFF888888)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = morphoFieldColors()
            )
            if (estaturaError) {
                Text(
                    text = "*Digita un valor entre 80 a 220",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }

            MorphoSpacer()

            // ==========================================
            // PESO
            // ==========================================
            MorphoLabel(text = "Peso (Kg)", required = true, info = "En Kilogramos")
            OutlinedTextField(
                value = peso,
                onValueChange = {
                    val filtered = it.filter { c -> c.isDigit() || c == '.' }
                    peso = filtered
                    val valor = filtered.toDoubleOrNull()
                    pesoError = valor == null || valor < 31
                },
                isError = pesoError,
                placeholder = { Text("Peso") },
                interactionSource = pesoInteraction,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.MonitorWeight,
                        contentDescription = null,
                        tint = if (pesoFocused) PrimaryBlue else Color(0xFF888888)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = morphoFieldColors()
            )
            if (pesoError) {
                Text(
                    text = "*Digita un valor mayor a 30",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }

            MorphoSpacer()

            // ==========================================
            // GRASA
            // ==========================================
            MorphoLabel(
                text = "Grasa",
                info = "Medida para distinguir la grasa del músculo, ideal mujer 20–30%, ideal hombre 10–20%"
            )
            OutlinedTextField(
                value = grasa,
                onValueChange = {
                    val filtered = it.filter { c -> c.isDigit() || c == '.' }
                    grasa = filtered
                    val valor = filtered.toDoubleOrNull()
                    grasaError = valor == null || valor < 4 || valor > 50
                },
                isError = grasaError,
                placeholder = { Text("Grasa") },
                interactionSource = grasaInteraction,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Percent,
                        contentDescription = null,
                        tint = if (grasaFocused) PrimaryBlue else Color(0xFF888888)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = morphoFieldColors()
            )
            if (grasaError) {
                Text(
                    text = "*Digita un valor de 4 a 50",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }

            MorphoSpacer()

            // ==========================================
            // IMC
            // ==========================================
            MorphoLabel(text = "IMC", info = "Índice de masa corporal, evalúa la obesidad, ideal 20-25")
            OutlinedTextField(
                value = imc,
                onValueChange = {},
                placeholder = { Text("IMC") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = Color(0xFF888888)
                    )
                },
                enabled = false,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = morphoReadOnlyColors()
            )

            MorphoSpacer()

            // ==========================================
            // SOMATOTIPO
            // ==========================================
            MorphoLabel(
                text = "Somatotipo",
                info = "Hace referencia a tu forma corporal. Ectomorfo: Delgado, metabolismo rápido. " +
                        "Mesomorfo: Robusto, metabolismo normal. Endomorfo: Acumulas grasa, metabolismo lento."
            )
            ExposedDropdownMenuBox(
                expanded = expandedSomatotipo,
                onExpandedChange = { expandedSomatotipo = !expandedSomatotipo }
            ) {
                OutlinedTextField(
                    value = somatotipo,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Somatotipo") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(8.dp),
                    colors = morphoFieldColors(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSomatotipo)
                    }
                )
                ExposedDropdownMenu(
                    expanded = expandedSomatotipo,
                    onDismissRequest = { expandedSomatotipo = false },
                    modifier = Modifier.background(Color(0xFFF7F9FB))
                ) {
                    somatotipos.forEach { item ->
                        val isPlaceholder = item == "Seleccione Uno"
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = item,
                                    color = if (isPlaceholder) Color.Gray else Color(0xFF2B2E34),
                                    fontSize = 14.sp
                                )
                            },
                            enabled = !isPlaceholder,
                            onClick = {
                                somatotipo = item
                                expandedSomatotipo = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (item == somatotipo) PrimaryBlue.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                        )
                    }
                }
            }

            MorphoSpacer()

            // ==========================================
            // SECCIÓN PERÍMETROS
            // ==========================================
            HorizontalDivider(
                color = Color(0xFFEEEEEE),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Perímetros",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
                Spacer(modifier = Modifier.width(6.dp))
                MorphoTooltip("En centímetros para cada contorno")
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Fechas:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF555B61)
            )
            Text(
                text = "Medidas en cm:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF555B61)
            )

            Spacer(modifier = Modifier.height(16.dp))

            PerimetroItem(
                nombre   = "Hombros",
                medicion = hombros,
                isError  = perimetroError && hombros.fecha.isNotBlank() && hombros.medida.isBlank(),
                onDeleteSuccess = { showDeleteSuccess = true },
                onChange = { hombros = it }
            )
            PerimetroItem(
                nombre   = "Pecho",
                medicion = pecho,
                isError  = perimetroError && pecho.fecha.isNotBlank() && pecho.medida.isBlank(),
                onDeleteSuccess = { showDeleteSuccess = true },
                onChange = { pecho = it }
            )
            PerimetroItem(
                nombre   = "Brazo",
                medicion = brazo,
                isError  = perimetroError && brazo.fecha.isNotBlank() && brazo.medida.isBlank(),
                onDeleteSuccess = { showDeleteSuccess = true },
                onChange = { brazo = it }
            )
            PerimetroItem(
                nombre   = "Cintura",
                medicion = cintura,
                isError  = perimetroError && cintura.fecha.isNotBlank() && cintura.medida.isBlank(),
                onDeleteSuccess = { showDeleteSuccess = true },
                onChange = { cintura = it }
            )
            PerimetroItem(
                nombre   = "Muslo Medio",
                medicion = musloMedio,
                isError  = perimetroError && musloMedio.fecha.isNotBlank() && musloMedio.medida.isBlank(),
                onDeleteSuccess = { showDeleteSuccess = true },
                onChange = { musloMedio = it }
            )
            PerimetroItem(
                nombre   = "Glúteos",
                medicion = gluteos,
                isError  = perimetroError && gluteos.fecha.isNotBlank() && gluteos.medida.isBlank(),
                onDeleteSuccess = { showDeleteSuccess = true },
                onChange = { gluteos = it }
            )
            PerimetroItem(
                nombre   = "Pantorrilla",
                medicion = pantorrilla,
                isError  = perimetroError && pantorrilla.fecha.isNotBlank() && pantorrilla.medida.isBlank(),
                onDeleteSuccess = { showDeleteSuccess = true },
                onChange = { pantorrilla = it }
            )
        }

        // === Botón Actualizar fijo ===
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { guardarMorfologia() },
                    modifier = Modifier
                        .height(50.dp)
                        .width(180.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(
                        text = "Actualizar",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // === Notificaciones ===
        if (showSuccess) {
            MorphoSuccessNotification(
                message = "¡Sus cambios han sido guardados con éxito!",
                onDismiss = { showSuccess = false }
            )
        }
        if (showError) {
            MorphoErrorNotification(
                message = "Por favor completa los campos obligatorios",
                onDismiss = { showError = false }
            )
        }
        if (showDeleteSuccess) {
            MorphoSuccessNotification(
                message  = "¡Sus datos han sido eliminados correctamente!",
                onDismiss = { showDeleteSuccess = false }
            )
        }
    }
}

// ==========================================
// HELPERS PRIVADOS
// ==========================================

// FIX 2: Lee un campo de Firebase como String sin importar
// si Firebase lo guardó como String, Double o Long
private fun leerCampoComoString(snapshot: DataSnapshot, key: String): String {
    val value = snapshot.child(key).value ?: return ""
    return when (value) {
        is String -> value
        is Double -> {
            // Si es entero no muestra decimales innecesarios
            if (value == kotlin.math.floor(value)) value.toInt().toString()
            else value.toString()
        }
        is Long   -> value.toString()
        else      -> value.toString()
    }
}

private fun normalizarFechaParaUI(fechaRaw: String): String {
    if (fechaRaw.isBlank()) return ""

    // Si ya está en formato dd/MM/yyyy no hace nada
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

private fun cargarPerimetro(
    snapshot: DataSnapshot,
    fechaKey: String,
    medidaKey: String
): PerimetroMedicion {
    val fechaRaw  = snapshot.child(fechaKey).getValue(String::class.java).orEmpty()
    val medidaRaw = leerCampoComoString(snapshot, medidaKey)
    return PerimetroMedicion(
        fecha  = normalizarFechaParaUI(fechaRaw),
        medida = medidaRaw
    )
}

@Composable
private fun MorphoLabel(
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
                    withStyle(SpanStyle(color = Color.Red)) { append("*") }
                }
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF555B61)
        )
        if (info != null) {
            Spacer(modifier = Modifier.width(6.dp))
            MorphoTooltip(info)
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun MorphoSpacer() {
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun morphoFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = PrimaryBlue,
    unfocusedBorderColor = Color(0xFFCCCCCC),
    cursorColor          = PrimaryBlue,
    focusedTextColor     = Color(0xFF2B2E34),
    unfocusedTextColor   = Color(0xFF2B2E34)
)

@Composable
private fun morphoReadOnlyColors() = OutlinedTextFieldDefaults.colors(
    disabledBorderColor     = Color(0xFFCCCCCC),
    disabledTextColor       = Color(0xFF888888),
    disabledContainerColor  = Color(0xFFF5F5F5),
    disabledLabelColor      = Color(0xFF888888)
)

@Composable
private fun MorphoTooltip(message: String) {
    var show by remember { mutableStateOf(false) }
    Box {
        Icon(
            imageVector = Icons.Default.HelpOutline,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier
                .size(18.dp)
                .clickable { show = true }
        )
        if (show) {
            Dialog(onDismissRequest = { show = false }) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(2.dp, PrimaryBlue),
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .widthIn(min = 200.dp, max = 280.dp)
                    ) {
                        Text(text = message, fontSize = 14.sp, color = Color(0xFF333333))
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { show = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("OK", color = PrimaryBlue)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MorphoSuccessNotification(message: String, onDismiss: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        onDismiss()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            color = Color(0xFF4CAF50),
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = message, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun MorphoErrorNotification(message: String, onDismiss: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        onDismiss()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 20.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            color = Color(0xFFD32F2F),
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
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
                                timeZone = TimeZone.getTimeZone("UTC")  // ← fuerza UTC
                            }
                            onChange(medicion.copy(fecha = sdf.format(Date(millis))))
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Aceptar", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDatePicker = false },
                    border = BorderStroke(1.dp, PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancelar", color = PrimaryBlue)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Color.White)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor          = Color.White,
                    titleContentColor       = PrimaryBlue,
                    headlineContentColor    = PrimaryBlue,
                    weekdayContentColor     = Color(0xFF555B61),
                    navigationContentColor  = PrimaryBlue,
                    yearContentColor        = Color(0xFF2B2E34),
                    currentYearContentColor = PrimaryBlue,
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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .border(
                            width = 2.dp,
                            color = Color(0xFFFF9800),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                }
            },
            title = {
                Text(
                    text = "¿Estás seguro?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2B2E34),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "Esta acción eliminará datos importantes de fecha y medida de morfología. ¿Deseas continuar?",
                    fontSize = 14.sp,
                    color = Color(0xFF555B61),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onChange(PerimetroMedicion())
                        showDeleteDialog = false
                        onDeleteSuccess()   // ← en vez de showDeleteSuccess = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Sí, eliminar", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancelar", color = Color.White)
                }
            }
        )
    }


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isError) Color.Red else Color(0xFFEEEEEE)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // --- Título ---
            Text(
                text = nombre,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = PrimaryBlue,
                        shape = RoundedCornerShape(50.dp)
                    )
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- Campo fecha ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                OutlinedTextField(
                    value = medicion.fecha,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    placeholder = { Text("Seleccionar fecha", color = Color(0xFF888888)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Seleccionar fecha",
                            tint = Color(0xFF888888)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor       = Color(0xFFCCCCCC),
                        disabledContainerColor    = Color.White,
                        disabledTextColor         = Color(0xFF555B61),
                        disabledPlaceholderColor  = Color(0xFF888888),
                        disabledLeadingIconColor  = Color(0xFF888888)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            val medidaInteraction = remember { MutableInteractionSource() }
            val medidaFocused by medidaInteraction.collectIsFocusedAsState()
            // --- Campo medida + flechitas ---
            OutlinedTextField(
                value = medicion.medida,
                onValueChange = { valor ->
                    val filtered = valor.filter { it.isDigit() || it == '-' }
                    onChange(medicion.copy(medida = filtered))
                },
                isError = isError,
                placeholder = { Text("medida", color = Color(0xFF888888)) },
                interactionSource = medidaInteraction,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = when {
                            isError                -> Color.Red
                            medidaFocused          -> PrimaryBlue
                            else                   -> Color(0xFF888888)
                        }
                    )
                },
                trailingIcon = {
                    Column(
                        modifier = Modifier.padding(end = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Flecha subir
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Subir",
                            tint = if (medicion.fecha.isNotBlank()) PrimaryBlue else Color(0xFFCCCCCC),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(enabled = medicion.fecha.isNotBlank()) {
                                    val actual = medicion.medida.toDoubleOrNull() ?: 0.0
                                    onChange(medicion.copy(medida = String.format("%.0f", actual + 1)))
                                }
                        )
                        // Flecha bajar
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Bajar",
                            tint = if (medicion.fecha.isNotBlank()) PrimaryBlue else Color(0xFFCCCCCC),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(enabled = medicion.fecha.isNotBlank()) {
                                    val actual = medicion.medida.toDoubleOrNull() ?: 0.0
                                    onChange(medicion.copy(medida = String.format("%.0f", actual - 1)))
                                }
                        )
                    }
                },
                enabled = medicion.fecha.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),  // ← fillMaxWidth ya que no hay Row
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor    = PrimaryBlue,
                    unfocusedBorderColor  = Color(0xFFEEEEEE),
                    focusedTextColor      = Color(0xFF555B61),
                    unfocusedTextColor    = Color(0xFF555B61),
                    cursorColor           = PrimaryBlue,
                    errorBorderColor      = Color.Red,
                    errorLeadingIconColor = Color.Red
                )
            )
                Spacer(modifier = Modifier.width(6.dp))
            // --- Botón eliminar ---
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = Color(0xFFFFF3E0),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    .clickable { showDeleteDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}