package com.example.mvt.ui.screens.personaldata

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.mvt.ui.theme.PrimaryBlue
import com.example.mvt.ui.viewmodels.UserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userViewModel: UserViewModel,
    navController: NavController
) {
    // === Usuario desde el ViewModel ===
    val user by userViewModel.user.collectAsState()
    LaunchedEffect(Unit) {
        userViewModel.loadUserInfo()
    }

    // === Estados campos editables ===
    var nombres by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var documento by remember { mutableStateOf("") }
    var fechaNacimiento by remember { mutableStateOf("") }
    var genero by remember { mutableStateOf("") }
    var nacionalidad by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }

    // === Estados UI ===
    var expandedGenero by remember { mutableStateOf(false) }
    var expandedPais by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    // === variables de error ===
    var nombresError by remember { mutableStateOf(false) }
    var apellidosError by remember { mutableStateOf(false) }
    var telefonoError by remember { mutableStateOf(false) }
    var nacionalidadError by remember { mutableStateOf(false) }
    var aliasError by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    var showError by remember { mutableStateOf(false) }

    // === Listas de opciones ===
    val generos = listOf("Masculino", "Femenino", "Otro")
    val nacionalidades = listOf(
        "Seleccione Uno", "Argentina", "Boliviana", "Chilena", "Colombiana",
        "Costarricense", "Cubana", "Ecuatoriana", "Salvadoreña", "Española",
        "Estadounidense", "Guatemalteca", "Hondureña", "Mexicana",
        "Nicaragüense", "Panameña", "Paraguaya", "Peruana",
        "Puertorriqueña", "Dominicana"
    )

    // === Sincronizar cuando Firebase responda ===
    LaunchedEffect(user) {
        nombres = user?.nombres ?: ""
        apellidos = user?.apellidos ?: ""
        fechaNacimiento = formatFecha(user?.fecha_nacimiento)
        alias = user?.nameUser ?: ""
        documento = user?.identificacion ?: ""
        telefono = user?.telefono ?: ""
        genero = user?.genero ?: ""
        nacionalidad = user?.pais ?: ""
    }

    val scrollState = rememberScrollState()

    // === Box externo
    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = PrimaryBlue
                    )
                }
                Text(
                    text = "Información de Perfil",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            // --- Foto de perfil ---
            ProfileImageSection(
                imageUrl = user?.foto_url,
                onImageSelected = { uri ->
                    userViewModel.uploadProfilePhoto(uri)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // NOMBRES
            // ==========================================
            FormLabel(text = "Nombres", required = true)
            OutlinedTextField(
                value = nombres,
                onValueChange = {
                    nombres = onlyLettersAndSpaces(it)
                    nombresError = nombres.isBlank()
                },
                isError = nombresError,
                placeholder = { Text("Ingresa tus nombres") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = fieldColors()
            )

            FormSpacer()

            // APELLIDOS
            // ==========================================
            FormLabel(text = "Apellidos", required = true)
            OutlinedTextField(
                value = apellidos,
                onValueChange = {
                    apellidos = onlyLettersAndSpaces(it)
                    apellidosError = apellidos.isBlank()
                },
                isError = apellidosError,
                placeholder = { Text("Ingresa tus apellidos") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = fieldColors()
            )

            FormSpacer()

            // DOCUMENTO
            // ==========================================
            FormLabel(
                text = "Documento de identificación",
                info = "Para posible facturación"
            )
            OutlinedTextField(
                value = documento,
                onValueChange = { documento = it },
                placeholder = { Text("Número de identificación") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = fieldColors()
            )

            FormSpacer()

            // FECHA DE NACIMIENTO
            // ==========================================
            FormLabel(text = "Fecha de nacimiento", required = true)
            OutlinedTextField(
                value = fechaNacimiento,
                onValueChange = {},
                placeholder = { Text("DD/MM/AAAA") },
                singleLine = true,
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = readOnlyFieldColors()
            )

            FormSpacer()

            // GÉNERO
            // ==========================================
            FormLabel(
                text = "Género",
                required = true
            )

            OutlinedTextField(
                value = genero,
                onValueChange = {},
                readOnly = true,
                enabled = false, // 🔥 ESTO LO BLOQUEA TOTAL
                placeholder = { Text("Selecciona tu género") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = readOnlyFieldColors(),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            )

            FormSpacer()

            // NACIONALIDAD - dropdown
            // ==========================================
            FormLabel(text = "Nacionalidad", required = true)
            ExposedDropdownMenuBox(
                expanded = expandedPais,
                onExpandedChange = { expandedPais = !expandedPais }
            ) {
                OutlinedTextField(
                    value = nacionalidad,
                    onValueChange = {},
                    readOnly = true,
                    isError = nacionalidadError,
                    placeholder = { Text("Seleccione Uno") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPais)
                    }
                )
                ExposedDropdownMenu(
                    expanded = expandedPais,
                    onDismissRequest = { expandedPais = false },
                    modifier = Modifier.background(Color(0xFFF7F9FB))
                ) {
                    nacionalidades.forEach { item ->
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
                                nacionalidad = item
                                expandedPais = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (item == nacionalidad) PrimaryBlue.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                        )
                    }
                }
            }

            FormSpacer()

            // ALIAS USUARIO
            // ==========================================
            FormLabel(
                text = "Alias usuario",
                info = "Tu nombre de usuario, máximo 15 caracteres"
            )
            OutlinedTextField(
                value = alias,
                onValueChange = {
                    if (it.length <= 15) alias = it
                    aliasError = it.isBlank()
                },
                isError = aliasError,
                placeholder = { Text("Ingresa tu alias") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = fieldColors()
            )

            FormSpacer()

            // ID USUARIO - solo lectura
            // ==========================================
            FormLabel(
                text = "ID Usuario",
                required = true,
                info = "Esta es tu identificación en MVT"
            )
            OutlinedTextField(
                value = user?.UserID?.toString() ?: "",
                onValueChange = {},
                singleLine = true,
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = readOnlyFieldColors()
            )

            FormSpacer()

            // CORREO - solo lectura
            // ==========================================
            FormLabel(text = "Correo", required = true)
            OutlinedTextField(
                value = user?.email ?: "",
                onValueChange = {},
                singleLine = true,
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = readOnlyFieldColors()
            )

            FormSpacer()

            // TELÉFONO
            // ==========================================
            FormLabel(text = "Teléfono", required = true)
            OutlinedTextField(
                value = telefono,
                onValueChange = {
                    if (it.length <= 10) telefono = it
                    telefonoError = it.isBlank()
                },
                isError = telefonoError,
                placeholder = { Text("Número de teléfono") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = fieldColors()
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
        // BOTONES
        // ==========================================
        Spacer(modifier = Modifier.height(24.dp))
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
                    onClick = {
                        nombresError = nombres.isBlank()
                        apellidosError = apellidos.isBlank()
                        telefonoError = telefono.isBlank()
                        aliasError = alias.isBlank()
                        nacionalidadError = nacionalidad.isBlank() || nacionalidad == "Seleccione Uno"

                        val hasError = nombresError || apellidosError || telefonoError || nacionalidadError || aliasError

                        if (hasError) {
                            showError = true
                            return@Button
                        }

                        userViewModel.updateUser(
                            nombres = nombres,
                            apellidos = apellidos,
                            telefono = telefono,
                            genero = genero,
                            nacionalidad = nacionalidad,
                            alias = alias,
                            documento = documento,
                            onSuccess = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                scope.launch {
                                    scrollState.animateScrollTo(0)
                                }
                                showSuccess = true
                            },
                            onError = {
                                showError = true
                            }
                        )
                    },
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
    }


    // === Notificación flotante de éxito ===
    if (showSuccess) {
        SuccessNotification(
            message = "¡Sus cambios han sido guardados con éxito!",
            onDismiss = { showSuccess = false }
        )
    }
    if (showError) {
        ErrorNotification(
            message = "Por favor complete todos los campos obligatorios",
            onDismiss = { showError = false }
        )
    }
}


// HELPERS PRIVADOS
// ==========================================

private fun formatFecha(fecha: String?): String {
    return try {
        val datePart = fecha?.split("T")?.get(0)
        val parts = datePart?.split("-")
        "${parts?.get(2)}/${parts?.get(1)}/${parts?.get(0)}"
    } catch (e: Exception) {
        fecha ?: ""
    }
}

@Composable
private fun FormLabel(
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
            InfoTooltip(info)
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun FormSpacer() {
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryBlue,
    unfocusedBorderColor = Color(0xFFCCCCCC),
    cursorColor = PrimaryBlue,
    focusedLabelColor = PrimaryBlue
)

@Composable
private fun readOnlyFieldColors() = OutlinedTextFieldDefaults.colors(
    disabledBorderColor = Color(0xFFCCCCCC),
    disabledTextColor = Color(0xFF888888),
    disabledContainerColor = Color(0xFFF5F5F5)
)

@Composable
private fun ProfileImageSection(
    imageUrl: String?,
    onImageSelected: (Uri) -> Unit
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            onImageSelected(uri)   // sube a Firebase inmediatamente
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {

            Surface(
                shape = CircleShape,
                border = BorderStroke(3.dp, Color(0xFFE0E0E0)),
                shadowElevation = 6.dp,
                modifier = Modifier.size(150.dp)
            ) {
                when {
                    selectedImageUri != null -> AsyncImage(
                        model = selectedImageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )

                    imageUrl != null -> AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )

                    else -> Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(30.dp),
                        tint = Color.Gray
                    )
                }
            }

            // Botón "+"
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(PrimaryBlue, CircleShape)
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Cambiar foto",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Foto de perfil",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1D1D)
            )

            Spacer(modifier = Modifier.width(6.dp))

            InfoTooltip("Máximo 2MB, formato JPG, PNG")
        }
    }
}

@Composable
private fun InfoTooltip(message: String) {
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
private fun SuccessNotification(
    message: String,
    onDismiss: () -> Unit
) {
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
private fun ErrorNotification(
    message: String,
    onDismiss: () -> Unit
) {
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

private fun onlyLettersAndSpaces(value: String): String {
    return value.filter { it.isLetter() || it.isWhitespace() }
}
