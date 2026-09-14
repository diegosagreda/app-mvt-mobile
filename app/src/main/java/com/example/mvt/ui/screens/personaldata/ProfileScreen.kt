package com.example.mvt.ui.screens.personaldata

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.mvt.R
import com.example.mvt.ui.components.FormErrorNotification
import com.example.mvt.ui.components.FormSuccessNotification
import com.example.mvt.ui.components.FormTooltip
import com.example.mvt.ui.theme.*
import com.example.mvt.ui.viewmodels.ProfileUiState
import com.example.mvt.ui.viewmodels.UserViewModel
import java.text.SimpleDateFormat
import java.text.ParsePosition
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(userViewModel: UserViewModel, navController: NavController) {
    val user by userViewModel.user.collectAsState()
    val uiState by userViewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { userViewModel.loadUserInfo() }

    val currentUser = user
    if (currentUser == null) {
        Box(Modifier.fillMaxSize().background(AppSurface), contentAlignment = Alignment.Center) {
            if (uiState is ProfileUiState.Loading || uiState is ProfileUiState.Idle) {
                CircularProgressIndicator(color = AthleteNavigationBlue)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No pudimos cargar tu perfil", color = AppTextPrimary)
                    Button(onClick = userViewModel::loadUserInfo) { Text("Reintentar") }
                }
            }
        }
        return
    }

    var nombres by rememberSaveable(currentUser.UserID) { mutableStateOf(currentUser.nombres.orEmpty()) }
    var apellidos by rememberSaveable(currentUser.UserID) { mutableStateOf(currentUser.apellidos.orEmpty()) }
    var documento by rememberSaveable(currentUser.UserID) { mutableStateOf(currentUser.identificacion.orEmpty()) }
    var tipoDocumento by rememberSaveable(currentUser.UserID) { mutableStateOf(currentUser.tipo_documento.orEmpty()) }
    var fechaNacimiento by rememberSaveable(currentUser.UserID) { mutableStateOf(currentUser.fecha_nacimiento.orEmpty()) }
    var genero by rememberSaveable(currentUser.UserID) { mutableStateOf(currentUser.genero.orEmpty()) }
    var nacionalidad by rememberSaveable(currentUser.UserID) { mutableStateOf(currentUser.pais.orEmpty()) }
    var alias by rememberSaveable(currentUser.UserID) { mutableStateOf(currentUser.nameUser.orEmpty()) }
    var telefono by rememberSaveable(currentUser.UserID) { mutableStateOf(currentUser.telefono.orEmpty()) }
    var validate by rememberSaveable { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.Error) {
            errorMessage = (uiState as ProfileUiState.Error).message
        }
    }

    val hasChanges = nombres != currentUser.nombres.orEmpty() ||
        apellidos != currentUser.apellidos.orEmpty() || documento != currentUser.identificacion.orEmpty() ||
        tipoDocumento != currentUser.tipo_documento.orEmpty() ||
        fechaNacimiento != currentUser.fecha_nacimiento.orEmpty() || genero != currentUser.genero.orEmpty() ||
        nacionalidad != currentUser.pais.orEmpty() || alias != currentUser.nameUser.orEmpty() ||
        telefono != currentUser.telefono.orEmpty()
    val requiredError = "Campo obligatorio"

    Scaffold(
        containerColor = AppSurface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (hasChanges) {
                Surface(color = AppSurface) {
                    Button(
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AthleteNavigationBlue),
                        onClick = {
                            validate = true
                            if (listOf(nombres, apellidos, alias, telefono, nacionalidad).any { it.isBlank() } ||
                                nacionalidad == "Seleccione Uno") {
                                errorMessage = "Completa los campos obligatorios."
                            } else {
                                isSaving = true
                                focusManager.clearFocus()
                                userViewModel.updateUser(
                                    nombres = nombres.trim(), apellidos = apellidos.trim(),
                                    telefono = telefono, genero = genero, nacionalidad = nacionalidad,
                                    alias = alias.trim(), documento = documento.trim(),
                                    tipoDocumento = tipoDocumento.takeIf { it.isNotBlank() },
                                    fechaNacimiento = fechaNacimiento.takeIf { it.isNotBlank() },
                                    onSuccess = {
                                        nombres = nombres.trim()
                                        apellidos = apellidos.trim()
                                        alias = alias.trim()
                                        documento = documento.trim()
                                        isSaving = false
                                        showSuccess = true
                                    },
                                    onError = {
                                        isSaving = false
                                        errorMessage = "No pudimos guardar los cambios. Inténtalo de nuevo."
                                    }
                                )
                            }
                        }
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (isSaving) "Guardando…" else "Guardar cambios")
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().imePadding().verticalScroll(scrollState)
                    .padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileIdentity(
                    imageUrl = currentUser.foto_url,
                    name = nombres.ifBlank { "Tu perfil" },
                    alias = alias,
                    onImageSelected = userViewModel::uploadProfilePhoto
                )
                ProfileSection("Datos personales") {
                    ProfileField("Nombres", nombres, { nombres = onlyLettersAndSpaces(it) },
                        enabled = !isSaving, error = if (validate && nombres.isBlank()) requiredError else null)
                    ProfileField("Apellidos", apellidos, { apellidos = onlyLettersAndSpaces(it) },
                        enabled = !isSaving, error = if (validate && apellidos.isBlank()) requiredError else null)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ProfileLabel("Documento de identificación")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ProfileDropdown(
                                label = "Tipo de documento", value = tipoDocumento,
                                options = listOf("C.C", "T.I", "C.E", "Pasaporte", "PPT", "Otro"),
                                onSelect = { tipoDocumento = it }, modifier = Modifier.width(92.dp),
                                enabled = !isSaving, showLabel = false, placeholder = "Tipo"
                            )
                            ProfileField(
                                "Número de documento", documento, { documento = it },
                                modifier = Modifier.weight(1f), enabled = !isSaving, showLabel = false
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileField(
                            label = "Fecha de nacimiento", value = displayBirthDate(fechaNacimiento),
                            modifier = Modifier.weight(1.2f), placeholder = "Día / Mes / Año",
                            enabled = !isSaving, icon = Icons.Outlined.CalendarMonth,
                            onClick = {
                                val date = Calendar.getInstance().apply {
                                    val storedDate = parseBirthDate(fechaNacimiento)
                                    if (storedDate != null) time = storedDate else add(Calendar.YEAR, -18)
                                }
                                DatePickerDialog(context, { _, year, month, day ->
                                    val selectedDate = Calendar.getInstance().apply {
                                        clear()
                                        set(year, month, day)
                                    }.time
                                    fechaNacimiento = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(selectedDate)
                                }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).apply {
                                    datePicker.maxDate = System.currentTimeMillis()
                                }.show()
                            }
                        )
                        ProfileDropdown(
                            "Género", genero, listOf("Masculino", "Femenino", "Otro"),
                            { genero = it }, Modifier.weight(1f), enabled = !isSaving
                        )
                    }
                    ProfileDropdown(
                        "Nacionalidad", nacionalidad, profileNationalities, { nacionalidad = it },
                        enabled = !isSaving,
                        error = if (validate && (nacionalidad.isBlank() || nacionalidad == "Seleccione Uno")) requiredError else null
                    )
                }
                ProfileSection("Cuenta") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileField(
                            "Usuario", alias, { if (it.length <= 15) alias = it }, Modifier.weight(1f),
                            enabled = !isSaving, info = "Tu nombre de usuario, máximo 15 caracteres.",
                            error = if (validate && alias.isBlank()) requiredError else null
                        )
                        ProfileField("ID", currentUser.UserID?.toString().orEmpty(), modifier = Modifier.weight(1.2f),
                            info = "Tu identificador en My Virtual Trainer. No se puede modificar.")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileField("Correo", currentUser.email.orEmpty(), modifier = Modifier.weight(1.5f))
                        ProfileField(
                            "Teléfono", telefono, { if (it.length <= 10) telefono = it }, Modifier.weight(1f),
                            enabled = !isSaving, keyboardType = KeyboardType.Phone,
                            error = if (validate && telefono.isBlank()) requiredError else null
                        )
                    }
                }
            }
            if (showSuccess) {
                FormSuccessNotification(message = "Cambios guardados", onDismiss = { showSuccess = false })
            }
            errorMessage?.let { message ->
                FormErrorNotification(message = message, onDismiss = {
                    errorMessage = null
                    userViewModel.resetState()
                })
            }
        }
    }
}

@Composable
private fun ProfileIdentity(imageUrl: String?, name: String, alias: String, onImageSelected: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(onImageSelected)
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = { launcher.launch("image/*") }, shape = CircleShape,
            color = AppSurfaceMuted, modifier = Modifier.size(88.dp)
        ) {
            AsyncImage(
                model = imageUrl?.takeIf { it.isNotBlank() },
                placeholder = painterResource(R.drawable.iconografia_02_svg),
                fallback = painterResource(R.drawable.iconografia_02_svg),
                error = painterResource(R.drawable.iconografia_02_svg),
                contentDescription = "Cambiar foto de perfil", contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(name, color = AppTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        if (alias.isNotBlank()) Text("@${alias.removePrefix("@")}", color = AppTextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = AthleteNavigationBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        content()
    }
}

@Composable
private fun ProfileLabel(label: String, info: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = AppTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f, fill = false))
        if (info != null) FormTooltip(info)
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onValueChange: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "",
    showLabel: Boolean = true,
    info: String? = null,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (showLabel) ProfileLabel(label, info)
        val shape = RoundedCornerShape(8.dp)
        val border = BorderStroke(1.dp, if (error != null) MaterialTheme.colorScheme.error else AppBorder.copy(alpha = 0.7f))
        val fieldContent: @Composable () -> Unit = {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (onClick != null) {
                    Text(value.ifBlank { placeholder }, color = if (value.isBlank()) AppTextSecondary else AppTextPrimary,
                        fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                } else {
                    BasicTextField(
                        value = value, onValueChange = { onValueChange?.invoke(it) },
                        enabled = enabled, readOnly = onValueChange == null, singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = if (onValueChange == null) AppTextSecondary else AppTextPrimary, fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        cursorBrush = SolidColor(AthleteNavigationBlue),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { contentDescription = label },
                        decorationBox = { innerField ->
                            Box(Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
                                if (value.isBlank()) Text(placeholder, fontSize = 13.sp, color = AppTextSecondary)
                                innerField()
                            }
                        }
                    )
                }
                if (icon != null) Icon(icon, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(18.dp))
            }
        }
        if (onClick != null) {
            Surface(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
                shape = shape, border = border, color = AppSurface, shadowElevation = 1.dp, content = fieldContent)
        } else {
            Surface(modifier = Modifier.fillMaxWidth(), shape = shape, border = border,
                color = AppSurface, shadowElevation = 1.dp, content = fieldContent)
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
    }
}

@Composable
private fun ProfileDropdown(
    label: String, value: String, options: List<String>, onSelect: (String) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, showLabel: Boolean = true,
    placeholder: String = "", error: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        ProfileField(label, value, enabled = enabled, showLabel = showLabel, placeholder = placeholder,
            error = error, icon = Icons.Outlined.KeyboardArrowDown, onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

private val profileNationalities = listOf(
    "Argentina", "Boliviana", "Chilena", "Colombiana", "Costarricense", "Cubana", "Ecuatoriana",
    "Salvadoreña", "Española", "Estadounidense", "Guatemalteca", "Hondureña", "Mexicana",
    "Nicaragüense", "Panameña", "Paraguaya", "Peruana", "Puertorriqueña", "Dominicana"
)

internal fun parseBirthDate(value: String): Date? {
    val date = value.substringBefore("T")
    val pattern = when {
        date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> "yyyy-MM-dd"
        date.matches(Regex("\\d{2}/\\d{2}/\\d{4}")) -> "dd/MM/yyyy"
        else -> return null
    }
    val position = ParsePosition(0)
    return SimpleDateFormat(pattern, Locale.ROOT).apply { isLenient = false }
        .parse(date, position)?.takeIf { position.index == date.length }
}

internal fun displayBirthDate(value: String): String = parseBirthDate(value)
    ?.let { SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(it) } ?: value

private fun onlyLettersAndSpaces(value: String): String = value.filter { it.isLetter() || it.isWhitespace() }
