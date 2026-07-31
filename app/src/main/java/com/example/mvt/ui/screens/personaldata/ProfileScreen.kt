package com.example.mvt.ui.screens.personaldata

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
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
import com.example.mvt.ui.viewmodels.UserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userViewModel: UserViewModel,
    navController: NavController
) {
    val user by userViewModel.user.collectAsState()
    LaunchedEffect(Unit) {
        userViewModel.loadUserInfo()
    }

    var nombres by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var documento by remember { mutableStateOf("") }
    var fechaNacimiento by remember { mutableStateOf("") }
    var genero by remember { mutableStateOf("") }
    var nacionalidad by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }

    var expandedPais by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var personalSectionExpanded by remember { mutableStateOf(true) }
    var accountSectionExpanded by remember { mutableStateOf(true) }

    var nombresError by remember { mutableStateOf(false) }
    var apellidosError by remember { mutableStateOf(false) }
    var telefonoError by remember { mutableStateOf(false) }
    var nacionalidadError by remember { mutableStateOf(false) }
    var aliasError by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val nacionalidades = listOf(
        "Seleccione Uno", "Argentina", "Boliviana", "Chilena", "Colombiana",
        "Costarricense", "Cubana", "Ecuatoriana", "Salvadoreña", "Española",
        "Estadounidense", "Guatemalteca", "Hondureña", "Mexicana",
        "Nicaragüense", "Panameña", "Paraguaya", "Peruana",
        "Puertorriqueña", "Dominicana"
    )

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

    val initialNombres = user?.nombres.orEmpty()
    val initialApellidos = user?.apellidos.orEmpty()
    val initialDocumento = user?.identificacion.orEmpty()
    val initialFechaNacimiento = formatFecha(user?.fecha_nacimiento)
    val initialGenero = user?.genero.orEmpty()
    val initialNacionalidad = user?.pais.orEmpty()
    val initialAlias = user?.nameUser.orEmpty()
    val initialTelefono = user?.telefono.orEmpty()

    val hasProfileChanges =
        nombres != initialNombres ||
            apellidos != initialApellidos ||
            documento != initialDocumento ||
            fechaNacimiento != initialFechaNacimiento ||
            genero != initialGenero ||
            nacionalidad != initialNacionalidad ||
            alias != initialAlias ||
            telefono != initialTelefono

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (hasProfileChanges) {
                ProfileBottomBar(
                    isSaving = isSaving,
                    onSave = {
                        nombresError = nombres.isBlank()
                        apellidosError = apellidos.isBlank()
                        telefonoError = telefono.isBlank()
                        aliasError = alias.isBlank()
                        nacionalidadError = nacionalidad.isBlank() || nacionalidad == "Seleccione Uno"

                        val hasError =
                            nombresError || apellidosError || telefonoError || nacionalidadError || aliasError

                        if (hasError) {
                            showError = true
                            return@ProfileBottomBar
                        }

                        isSaving = true
                        userViewModel.updateUser(
                            nombres = nombres,
                            apellidos = apellidos,
                            telefono = telefono,
                            genero = genero,
                            nacionalidad = nacionalidad,
                            alias = alias,
                            documento = documento,
                            onSuccess = {
                                isSaving = false
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                scope.launch { scrollState.animateScrollTo(0) }
                                showSuccess = true
                            },
                            onError = {
                                isSaving = false
                                showError = true
                            }
                        )
                    }
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
                ProfileTopBar(
                    onBack = {
                        navController.navigate("profile") {
                            popUpTo("profile") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                ProfileHeroCard(
                    imageUrl = user?.foto_url,
                    displayName = listOf(nombres, apellidos).filter { it.isNotBlank() }.joinToString(" "),
                    alias = alias,
                    email = user?.email.orEmpty(),
                    userId = user?.UserID?.toString().orEmpty(),
                    onImageSelected = { uri ->
                        userViewModel.uploadProfilePhoto(uri)
                    }
                )

                ProfileSectionCard(
                    title = "Datos personales",
                    subtitle = "Informacion principal del atleta dentro de la plataforma.",
                    expanded = personalSectionExpanded,
                    onToggle = { personalSectionExpanded = !personalSectionExpanded }
                ) {
                    OutlinedTextField(
                        value = nombres,
                        onValueChange = {
                            nombres = onlyLettersAndSpaces(it)
                            nombresError = nombres.isBlank()
                        },
                        isError = nombresError,
                        label = { ProfileFieldLabel(text = "Nombres", required = true) },
                        placeholder = { ProfilePlaceholder("Ingresa tus nombres") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = FormFieldShape,
                        colors = formFieldColors()
                    )
                    ProfileFieldError(nombresError, "Este campo es obligatorio.")
                    ProfileFieldSpacer()

                    OutlinedTextField(
                        value = apellidos,
                        onValueChange = {
                            apellidos = onlyLettersAndSpaces(it)
                            apellidosError = apellidos.isBlank()
                        },
                        isError = apellidosError,
                        label = { ProfileFieldLabel(text = "Apellidos", required = true) },
                        placeholder = { ProfilePlaceholder("Ingresa tus apellidos") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = FormFieldShape,
                        colors = formFieldColors()
                    )
                    ProfileFieldError(apellidosError, "Este campo es obligatorio.")
                    ProfileFieldSpacer()

                    OutlinedTextField(
                        value = documento,
                        onValueChange = { documento = it },
                        label = {
                            ProfileFieldLabel(
                                text = "Documento de identificacion",
                                info = "Para posible facturacion"
                            )
                        },
                        placeholder = { ProfilePlaceholder("Numero de identificacion") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = FormFieldShape,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = formFieldColors()
                    )
                    ProfileFieldSpacer()

                    OutlinedTextField(
                        value = fechaNacimiento,
                        onValueChange = {},
                        label = { ProfileFieldLabel(text = "Fecha de nacimiento", required = true) },
                        placeholder = { ProfilePlaceholder("DD/MM/AAAA") },
                        singleLine = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = FormFieldShape,
                        colors = formReadOnlyColors()
                    )
                    ProfileFieldSpacer()

                    OutlinedTextField(
                        value = genero,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { ProfileFieldLabel(text = "Genero", required = true) },
                        placeholder = { ProfilePlaceholder("Selecciona tu genero") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = FormFieldShape,
                        colors = formReadOnlyColors(),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = AppTextSecondary
                            )
                        }
                    )
                    ProfileFieldSpacer()

                    ExposedDropdownMenuBox(
                        expanded = expandedPais,
                        onExpandedChange = { expandedPais = !expandedPais }
                    ) {
                        OutlinedTextField(
                            value = nacionalidad,
                            onValueChange = {},
                            readOnly = true,
                            isError = nacionalidadError,
                            label = { ProfileFieldLabel(text = "Nacionalidad", required = true) },
                            placeholder = { ProfilePlaceholder("Seleccione Uno") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = FormFieldShape,
                            colors = formFieldColors(),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPais)
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedPais,
                            onDismissRequest = { expandedPais = false },
                            modifier = Modifier.background(AppSurfaceAlt)
                        ) {
                            nacionalidades.forEach { item ->
                                val isPlaceholder = item == "Seleccione Uno"
                                DropdownItemText(
                                    text = item,
                                    selected = item == nacionalidad,
                                    enabled = !isPlaceholder,
                                    onClick = {
                                        nacionalidad = item
                                        expandedPais = false
                                    }
                                )
                            }
                        }
                    }
                    ProfileFieldError(
                        nacionalidadError,
                        "Selecciona una nacionalidad valida."
                    )
                }

                ProfileSectionCard(
                    title = "Cuenta y contacto",
                    subtitle = "Informacion visible para acceso y seguimiento dentro de MVT.",
                    expanded = accountSectionExpanded,
                    onToggle = { accountSectionExpanded = !accountSectionExpanded }
                ) {
                    OutlinedTextField(
                        value = alias,
                        onValueChange = {
                            if (it.length <= 15) alias = it
                            aliasError = it.isBlank()
                        },
                        isError = aliasError,
                        label = {
                            ProfileFieldLabel(
                                text = "Alias usuario",
                                info = "Tu nombre de usuario, maximo 15 caracteres"
                            )
                        },
                        placeholder = { ProfilePlaceholder("Ingresa tu alias") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = FormFieldShape,
                        colors = formFieldColors()
                    )
                    ProfileFieldError(aliasError, "Este campo es obligatorio.")
                    ProfileFieldSpacer()

                    OutlinedTextField(
                        value = user?.UserID?.toString() ?: "",
                        onValueChange = {},
                        label = {
                            ProfileFieldLabel(
                                text = "ID Usuario",
                                required = true,
                                info = "Esta es tu identificacion en MVT"
                            )
                        },
                        singleLine = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = FormFieldShape,
                        colors = formReadOnlyColors()
                    )
                    ProfileFieldSpacer()

                    OutlinedTextField(
                        value = user?.email ?: "",
                        onValueChange = {},
                        label = { ProfileFieldLabel(text = "Correo", required = true) },
                        singleLine = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = FormFieldShape,
                        colors = formReadOnlyColors()
                    )
                    ProfileFieldSpacer()

                    OutlinedTextField(
                        value = telefono,
                        onValueChange = {
                            if (it.length <= 10) telefono = it
                            telefonoError = telefono.isBlank()
                        },
                        isError = telefonoError,
                        label = { ProfileFieldLabel(text = "Telefono", required = true) },
                        placeholder = { ProfilePlaceholder("Numero de telefono") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = FormFieldShape,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = formFieldColors()
                    )
                    ProfileFieldError(telefonoError, "Este campo es obligatorio.")
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
                    message = "Por favor complete todos los campos obligatorios.",
                    onDismiss = { showError = false }
                )
            }
        }
    }
}

@Composable
private fun ProfileTopBar(onBack: () -> Unit) {
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
                text = "Informacion de perfil",
                style = MaterialTheme.typography.headlineSmall,
                color = AppTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Actualiza tus datos personales y de cuenta.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTextSecondary
            )
        }
    }
}

@Composable
private fun ProfileHeroCard(
    imageUrl: String?,
    displayName: String,
    alias: String,
    email: String,
    userId: String,
    onImageSelected: (Uri) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            AppSurfaceAlt,
                            AppSurface
                        )
                    )
                )
                .border(1.dp, AppBorder.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileImageSection(
                    imageUrl = imageUrl,
                    onImageSelected = onImageSelected
                )

                Text(
                    text = if (displayName.isBlank()) "Perfil del atleta" else displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = AppTextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (alias.isNotBlank()) {
                        ProfileInfoRow(
                            label = "Alias",
                            value = "@$alias",
                            highlighted = true
                        )
                    }

                    if (email.isNotBlank()) {
                        ProfileInfoRow(
                            label = "Correo",
                            value = email
                        )
                    }

                    if (userId.isNotBlank()) {
                        ProfileInfoRow(
                            label = "ID MVT",
                            value = userId
                        )
                    }
                }
            }
        }
    }
}

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
            onImageSelected(uri)
        }
    }

    Box(contentAlignment = Alignment.BottomEnd) {
        Surface(
            shape = CircleShape,
            color = AppSurfaceMuted,
            border = BorderStroke(3.dp, PrimaryBlue.copy(alpha = 0.45f)),
            shadowElevation = 6.dp,
            modifier = Modifier.size(118.dp)
        ) {
            when {
                selectedImageUri != null -> AsyncImage(
                    model = selectedImageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                !imageUrl.isNullOrBlank() -> AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                else -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = AppTextSecondary
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(38.dp)
                .background(PrimaryBlue, CircleShape)
                .border(2.dp, AppSurfaceAlt, CircleShape)
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Cambiar foto",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String,
    highlighted: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (highlighted) AppPrimarySoft else AppSurfaceMuted
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (highlighted) AppTextPrimary else AppTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(60.dp)
            )
            Text(
                text = value,
                color = AppTextPrimary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProfileSectionCard(
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
private fun ProfileBottomBar(
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
                text = "Actualiza la informacion visible del perfil.",
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
                        text = "Actualizar perfil",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DropdownItemText(
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
private fun ProfilePlaceholder(text: String) {
    Text(text = text, color = AppTextSecondary.copy(alpha = 0.75f))
}

@Composable
private fun ProfileFieldLabel(
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
private fun ProfileFieldSpacer() {
    Spacer(modifier = Modifier.height(2.dp))
}

@Composable
private fun ProfileFieldError(show: Boolean, message: String) {
    if (!show) return
    Text(
        text = message,
        color = AccentRed,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 6.dp, start = 4.dp)
    )
}

private fun formatFecha(fecha: String?): String {
    return try {
        val datePart = fecha?.split("T")?.get(0)
        val parts = datePart?.split("-")
        "${parts?.get(2)}/${parts?.get(1)}/${parts?.get(0)}"
    } catch (e: Exception) {
        fecha ?: ""
    }
}

private fun onlyLettersAndSpaces(value: String): String {
    return value.filter { it.isLetter() || it.isWhitespace() }
}
