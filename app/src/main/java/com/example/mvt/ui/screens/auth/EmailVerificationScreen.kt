package com.example.mvt.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mvt.registration.viewmodel.EmailVerificationViewModel
import com.example.mvt.ui.components.FormErrorNotification
import com.example.mvt.ui.components.FormFieldShape
import com.example.mvt.ui.components.FormSuccessNotification
import com.example.mvt.ui.components.formFieldColors
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppPrimarySoft
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue

@Composable
fun EmailVerificationScreen(
    initialEmail: String,
    navController: NavController,
    viewModel: EmailVerificationViewModel = viewModel()
) {
    val action by viewModel.action.collectAsState()
    var email by remember(initialEmail) { mutableStateOf(initialEmail) }
    var dialog by remember { mutableStateOf<VerificationDialog?>(null) }
    LaunchedEffect(action.updatedEmail) { action.updatedEmail?.let { email = it } }

    Box(Modifier.fillMaxSize().background(AppBackground).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(82.dp).background(AppPrimarySoft, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Email, null, tint = PrimaryBlue, modifier = Modifier.size(40.dp))
            }
            Text("Verifica tu correo", color = AppTextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Enviamos un enlace de verificación a\n$email",
                color = AppTextSecondary,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center
            )
            Text("Si no encuentras el mensaje, revisa la carpeta de spam.", color = AppTextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    navController.navigate("login") {
                        popUpTo("verify_email/{email}") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Ir a iniciar sesión", fontWeight = FontWeight.Bold) }
            TextButton(onClick = { dialog = VerificationDialog.Resend }, enabled = !action.isWorking) {
                Text("Reenviar correo", color = PrimaryBlue)
            }
            TextButton(onClick = { dialog = VerificationDialog.ChangeEmail }, enabled = !action.isWorking) {
                Text("Cambiar correo", color = AppTextSecondary)
            }
            if (action.isWorking) CircularProgressIndicator(Modifier.size(22.dp), color = PrimaryBlue, strokeWidth = 2.dp)
        }
    }

    dialog?.let { type ->
        VerificationActionDialog(
            type = type,
            working = action.isWorking,
            onDismiss = { if (!action.isWorking) dialog = null },
            onConfirm = { password, newEmail ->
                if (type == VerificationDialog.Resend) viewModel.resend(email, password)
                else viewModel.changeEmail(email, newEmail, password)
                dialog = null
            }
        )
    }
    action.message?.let { message ->
        if (action.isError) FormErrorNotification(message, viewModel::clearMessage)
        else FormSuccessNotification(message, viewModel::clearMessage)
    }
}

private enum class VerificationDialog { Resend, ChangeEmail }

@Composable
private fun VerificationActionDialog(
    type: VerificationDialog,
    working: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (password: String, newEmail: String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val valid = password.isNotBlank() && (type == VerificationDialog.Resend || newEmail.contains("@"))
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text(if (type == VerificationDialog.Resend) "Reenviar verificación" else "Cambiar correo", color = AppTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Por seguridad, confirma tu contraseña.", color = AppTextSecondary, fontSize = 12.sp)
                if (type == VerificationDialog.ChangeEmail) {
                    OutlinedTextField(newEmail, { newEmail = it }, label = { Text("Nuevo correo") }, singleLine = true, shape = FormFieldShape, colors = formFieldColors())
                }
                OutlinedTextField(
                    password, { password = it }, label = { Text("Contraseña") }, singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { IconButton({ showPassword = !showPassword }) { Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } },
                    shape = FormFieldShape, colors = formFieldColors()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(password, newEmail) }, enabled = valid && !working) {
                Text(if (type == VerificationDialog.Resend) "Reenviar" else "Confirmar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !working) { Text("Cancelar") } }
    )
}
