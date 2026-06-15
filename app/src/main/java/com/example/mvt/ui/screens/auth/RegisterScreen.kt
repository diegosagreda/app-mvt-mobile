package com.example.mvt.ui.screens.auth

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mvt.registration.model.RegistrationProgress
import com.example.mvt.registration.model.RegistrationUiState
import com.example.mvt.registration.model.passwordChecks
import com.example.mvt.registration.viewmodel.RegistrationViewModel
import com.example.mvt.ui.components.FormFieldShape
import com.example.mvt.ui.components.formFieldColors
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSuccess
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: RegistrationViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    BackHandler(enabled = state.step == 2 && !state.isSubmitting) { viewModel.backToPersonal() }
    LaunchedEffect(state.progress) {
        if (state.progress == RegistrationProgress.SUCCESS) {
            navController.navigate("verify_email/${Uri.encode(state.form.email.trim().lowercase())}") {
                popUpTo("register") { inclusive = true }
            }
        }
    }

    Scaffold(containerColor = AppBackground) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RegistrationHeader(
                step = state.step,
                onBack = { if (state.step == 2) viewModel.backToPersonal() else navController.popBackStack() }
            )
            StepIndicator(state.step)
            if (state.step == 1) PersonalStep(state, viewModel)
            else AccessStep(state, viewModel)
        }
    }
}

@Composable
private fun PersonalStep(state: RegistrationUiState, viewModel: RegistrationViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Información personal", color = AppTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Cuéntanos quién eres para crear tu perfil de deportista.", color = AppTextSecondary, fontSize = 13.sp)
        RegistrationField(
            value = state.form.firstName,
            onChange = { value -> viewModel.updateForm { it.copy(firstName = value) } },
            label = "Primer nombre",
            error = state.errors.firstName,
            capitalization = KeyboardCapitalization.Words
        )
        RegistrationField(
            value = state.form.lastName,
            onChange = { value -> viewModel.updateForm { it.copy(lastName = value) } },
            label = "Primer apellido",
            error = state.errors.lastName,
            capitalization = KeyboardCapitalization.Words
        )
        RegistrationField(
            value = state.form.username,
            onChange = { value -> viewModel.updateForm { it.copy(username = value) } },
            label = "Nombre de usuario",
            error = state.errors.username,
            capitalization = KeyboardCapitalization.None
        )
        LegalCheck(
            checked = state.form.acceptedTerms,
            text = "Acepto los términos y condiciones",
            onChange = { checked -> viewModel.updateForm { it.copy(acceptedTerms = checked) } }
        )
        LegalCheck(
            checked = state.form.acceptedPrivacyPolicy,
            text = "Acepto la política de privacidad",
            onChange = { checked -> viewModel.updateForm { it.copy(acceptedPrivacyPolicy = checked) } }
        )
        state.errors.legal?.let { ErrorText(it) }
        Button(
            onClick = viewModel::continueToAccess,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) { Text("Continuar", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun AccessStep(state: RegistrationUiState, viewModel: RegistrationViewModel) {
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmation by remember { mutableStateOf(false) }
    val checks = passwordChecks(state.form.password)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Crea tu acceso", color = AppTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Usaremos este correo para verificar y proteger tu cuenta.", color = AppTextSecondary, fontSize = 13.sp)
        RegistrationField(
            value = state.form.email,
            onChange = { value -> viewModel.updateForm { it.copy(email = value) } },
            label = "Correo electrónico",
            error = state.errors.email,
            keyboardType = KeyboardType.Email,
            capitalization = KeyboardCapitalization.None
        )
        PasswordField(
            value = state.form.password,
            onChange = { value -> viewModel.updateForm { it.copy(password = value) } },
            label = "Contraseña",
            visible = showPassword,
            onToggle = { showPassword = !showPassword },
            error = state.errors.password
        )
        Column(
            Modifier.fillMaxWidth().background(AppSurface, RoundedCornerShape(14.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            PasswordRequirement("Mínimo 8 caracteres", checks.minimumLength)
            PasswordRequirement("Una letra mayúscula", checks.uppercase)
            PasswordRequirement("Un número", checks.number)
            PasswordRequirement("Un carácter especial", checks.special)
        }
        PasswordField(
            value = state.form.passwordConfirmation,
            onChange = { value -> viewModel.updateForm { it.copy(passwordConfirmation = value) } },
            label = "Confirmar contraseña",
            visible = showConfirmation,
            onToggle = { showConfirmation = !showConfirmation },
            error = state.errors.passwordConfirmation
        )
        state.errorMessage?.let { ErrorText(it) }
        if (state.canRetryInitialization) {
            Button(
                onClick = viewModel::retryInitialization,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) { Text("Reintentar configuración", fontWeight = FontWeight.Bold) }
        } else {
            Button(
                onClick = viewModel::submit,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                    Text(progressLabel(state.progress))
                } else Text("Crear cuenta", fontWeight = FontWeight.Bold)
            }
        }
        Text(
            "Tu cuenta será creada como Deportista.",
            color = AppTextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RegistrationField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { message -> { Text(message) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
        shape = FormFieldShape,
        colors = formFieldColors()
    )
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String, visible: Boolean, onToggle: () -> Unit, error: String?) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { IconButton(onClick = onToggle) { Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } },
        isError = error != null,
        supportingText = error?.let { message -> { Text(message) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(), shape = FormFieldShape, colors = formFieldColors()
    )
}

@Composable
private fun LegalCheck(checked: Boolean, text: String, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onChange, colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue))
        Text(text, color = AppTextPrimary, fontSize = 13.sp)
    }
}

@Composable
private fun PasswordRequirement(label: String, complete: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(18.dp).background(if (complete) AppSuccess.copy(alpha = .18f) else AppBorder, CircleShape), contentAlignment = Alignment.Center) {
            Icon(if (complete) Icons.Default.Check else Icons.Default.Close, null, tint = if (complete) AppSuccess else AppTextSecondary, modifier = Modifier.size(12.dp))
        }
        Text(label, color = if (complete) AppTextPrimary else AppTextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun StepIndicator(step: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(2) { index ->
            Box(Modifier.weight(1f).height(4.dp).background(if (index < step) PrimaryBlue else AppBorder, CircleShape))
        }
    }
}

@Composable
private fun RegistrationHeader(step: Int, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = AppTextPrimary) }
        Column {
            Text("Crear cuenta", color = AppTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Paso $step de 2", color = AppTextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable private fun ErrorText(message: String) = Text(message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
private fun progressLabel(progress: RegistrationProgress) = when (progress) {
    RegistrationProgress.CREATING_AUTH_USER -> "Creando cuenta..."
    RegistrationProgress.CREATING_PROFILE -> "Preparando perfil..."
    RegistrationProgress.SENDING_VERIFICATION -> "Enviando verificación..."
    else -> "Procesando..."
}
