package com.example.mvt.ui.screens.auth

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mvt.R
import com.example.mvt.data.firebase.AuthService
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import com.example.mvt.ui.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel = viewModel()) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val user = viewModel.user.value
    val error = viewModel.error.value
    val isLoading = viewModel.isLoading.value
    val verificationEmail = viewModel.verificationEmail.value
    val activity = LocalContext.current as? Activity

    // Launcher para manejar el resultado del Sign-In con Google
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.handleGoogleSignInResult(
                activity = activity ?: return@rememberLauncherForActivityResult,
                data = result.data,
                onSuccess = {
                    isGoogleLoading = false
                },
                onError = { errorMsg ->
                    isGoogleLoading = false
                    Log.e("GoogleAuth", errorMsg)
                }
            )
        } else {
            isGoogleLoading = false
            Log.e("GoogleAuth", "Cancelado o fallido")
        }
    }

    LaunchedEffect(user) {
        user?.let { signedInUser ->
            val destination = runCatching {
                if (AuthService().hasCompletedWelcome(signedInUser.uid)) "athleteMain" else "welcome"
            }.getOrElse {
                viewModel.logout()
                return@LaunchedEffect
            }
            navController.navigate(destination) {
                popUpTo("login") { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(verificationEmail) {
        val pendingEmail = verificationEmail ?: return@LaunchedEffect
        navController.navigate("verify_email/${Uri.encode(pendingEmail)}")
        viewModel.clearVerificationNavigation()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(scrollState)
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.mvt),
                        contentDescription = "MVT",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .width(156.dp)
                            .height(68.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    /*Text(
                        text = "My Virtual Trainer",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )*/
                }

                Spacer(modifier = Modifier.height(40.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("Correo electrónico") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontWeight = FontWeight.SemiBold),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            autoCorrect = true,
                            imeAction = ImeAction.Next
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AppSurfaceAlt,
                            unfocusedContainerColor = AppSurfaceAlt,
                            disabledContainerColor = AppSurfaceAlt,
                            focusedIndicatorColor = PrimaryBlue,
                            unfocusedIndicatorColor = AppBorder,
                            cursorColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedPlaceholderColor = AppTextSecondary,
                            unfocusedPlaceholderColor = AppTextSecondary
                        ),
                        leadingIcon = {
                            Text(
                                text = "@",
                                color = AppTextSecondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Contraseña") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showPassword) "Ocultar contraseña" else "Mostrar contraseña",
                                    tint = AppTextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontWeight = FontWeight.SemiBold),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AppSurfaceAlt,
                            unfocusedContainerColor = AppSurfaceAlt,
                            disabledContainerColor = AppSurfaceAlt,
                            focusedIndicatorColor = PrimaryBlue,
                            unfocusedIndicatorColor = AppBorder,
                            cursorColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedPlaceholderColor = AppTextSecondary,
                            unfocusedPlaceholderColor = AppTextSecondary
                        ),
                        leadingIcon = {
                            Text(
                                text = "*",
                                color = AppTextSecondary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))



                    Button(
                        onClick = { viewModel.login(email.trim(), password.trim()) },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.2.dp
                            )
                        } else {
                            Text(
                                "Iniciar Sesión",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }

                    TextButton(
                        onClick = { /* TODO: Recuperar contraseña */ },
                        modifier = Modifier.align(Alignment.End),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "Recuperar contraseña",
                            color = AppTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.14f))
                        )
                        Text(
                            text = "  o continúa con  ",
                            color = Color.White.copy(alpha = 0.66f),
                            fontSize = 13.sp
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.14f))
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = {
                            if (isGoogleLoading || isLoading) return@OutlinedButton
                            activity?.let {
                                try {
                                    isGoogleLoading = true
                                    viewModel.prepareGoogleSignInIntent(
                                        activity = it,
                                        onReady = { signInIntent ->
                                            launcher.launch(signInIntent)
                                        },
                                        onError = { errorMsg ->
                                            isGoogleLoading = false
                                            Log.e("GoogleAuth", errorMsg)
                                        }
                                    )
                                } catch (e: Exception) {
                                    isGoogleLoading = false
                                    Log.e("GoogleAuth", "Error lanzando Sign-In: ${e.message}")
                                }
                            } ?: Log.e("GoogleAuth", "Contexto no es una Activity válida")
                        },
                        enabled = !isGoogleLoading && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = AppSurfaceAlt)
                    ) {
                        if (isGoogleLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.2.dp
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.google),
                                contentDescription = "Google",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Iniciar sesion con Google", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "¿Aún no tiene cuenta? ",
                            color = Color.White.copy(alpha = 0.60f),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Registrarse",
                            color = AppTextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { navController.navigate("register") }
                        )
                    }
                }
            }
        }
    }
}
