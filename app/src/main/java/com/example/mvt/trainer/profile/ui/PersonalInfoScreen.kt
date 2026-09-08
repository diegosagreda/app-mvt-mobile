package com.example.mvt.trainer.profile.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.mvt.R
import com.example.mvt.trainer.profile.model.PersonalInfoModel
import com.example.mvt.trainer.profile.viewmodel.PersonalInfoUiState
import com.example.mvt.trainer.profile.viewmodel.PersonalInfoViewModel
import com.example.mvt.ui.theme.*

@Composable
fun PersonalInfoScreen(
    navController: NavController,
    viewModel: PersonalInfoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadPhoto(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        when (val state = uiState) {
            PersonalInfoUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            }

            is PersonalInfoUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = state.message, color = AppTextSecondary, textAlign = TextAlign.Center)
                    Button(
                        onClick = viewModel::loadProfile,
                        modifier = Modifier.padding(top = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) { Text("Reintentar") }
                }
            }

            is PersonalInfoUiState.Content -> {
                PersonalInfoContent(
                    navController = navController,
                    profile = state.profile,
                    isUploading = state.isUploading,
                    optimisticPhotoUri = state.optimisticPhotoUri,
                    onPickPhoto = { photoPickerLauncher.launch("image/*") }
                )
            }
        }
    }
}

@Composable
private fun PersonalInfoContent(
    navController: NavController,
    profile: PersonalInfoModel,
    isUploading: Boolean,
    optimisticPhotoUri: Uri?,
    onPickPhoto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 10.dp)
    ) {
        PersonalInfoHeader(onBack = { navController.popBackStack() })

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Tarjeta 1: Foto de perfil
            item {
                PhotoCard(
                    profile = profile,
                    isUploading = isUploading,
                    optimisticPhotoUri = optimisticPhotoUri,
                    onPickPhoto = onPickPhoto
                )
            }

            // Tarjeta 2: Información Personal
            item {
                PersonalDataCard(profile = profile)
            }
        }
    }
}

@Composable
private fun PersonalInfoHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = AppSurfaceAlt,
                border = BorderStroke(1.dp, AppBorder),
                modifier = Modifier.size(40.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = AppTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Informacion de perfil",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTextPrimary
                )
                Text(
                    text = "Actualiza tus datos personales y de cuenta.",
                    fontSize = 13.sp,
                    color = AppTextSecondary
                )
            }
        }
    }
}

@Composable
private fun PhotoCard(
    profile: PersonalInfoModel,
    isUploading: Boolean,
    optimisticPhotoUri: Uri?,
    onPickPhoto: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = AppSurface,
        border = BorderStroke(1.dp, AppBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Foto de perfil",
                color = AppTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "La foto de perfil ayuda a que otras personas te reconozcan",
                color = AppTextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))

            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    shape = CircleShape,
                    color = AppSurfaceMuted,
                    border = BorderStroke(3.dp, PrimaryBlue.copy(alpha = 0.45f)),
                    modifier = Modifier.size(118.dp),
                    shadowElevation = 6.dp
                ) {
                    if (optimisticPhotoUri != null) {
                        AsyncImage(
                            model = optimisticPhotoUri,
                            contentDescription = "Foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (profile.photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = profile.photoUrl,
                            contentDescription = "Foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.iconografia_02_svg),
                                contentDescription = "Foto de perfil",
                                modifier = Modifier.size(90.dp)
                            )
                        }
                    }

                    if (isUploading) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(30.dp))
                        }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(PrimaryBlue, CircleShape)
                        .border(2.dp, AppSurface, CircleShape)
                        .clickable { onPickPhoto() },
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
            
            Text(
                text = profile.fullName.ifBlank { "Entrenador" },
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = AppTextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun PersonalDataCard(profile: PersonalInfoModel) {
    var expanded by remember { mutableStateOf(true) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = AppSurface,
        border = BorderStroke(1.dp, AppBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Información Personal",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTextPrimary
                    )
                    Text(
                        text = "Información principal del atleta dentro de la plataforma.",
                        fontSize = 12.sp,
                        color = AppTextSecondary
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppTextSecondary
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FormReadOnlyField(label = "Nombres", value = profile.firstName, icon = Icons.Default.Person, required = true)
                    FormReadOnlyField(label = "Apellidos", value = profile.lastName, icon = Icons.Default.Person, required = true)
                    FormReadOnlyField(label = "Número de identificación", value = profile.identification, icon = Icons.Default.Badge)
                    FormReadOnlyField(label = "Género", value = profile.gender, icon = Icons.Default.Transgender, required = true)
                    FormReadOnlyField(label = "Fecha de nacimiento", value = profile.birthDate, icon = Icons.Default.CalendarMonth, required = true)
                }
            }
        }
    }
}

@Composable
private fun FormReadOnlyField(
    label: String,
    value: String,
    icon: ImageVector,
    required: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = {
            Text(
                text = buildAnnotatedString {
                    append(label)
                    if (required) {
                        withStyle(SpanStyle(color = AccentRed)) { append(" *") }
                    }
                },
                color = AppTextSecondary,
                fontSize = 13.sp
            )
        },
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null, tint = AppIconMuted, modifier = Modifier.size(20.dp))
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        textStyle = androidx.compose.ui.text.TextStyle(
            color = AppTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        ),
        colors = OutlinedTextFieldDefaults.colors(
            disabledBorderColor = AppBorder,
            disabledContainerColor = Color.Transparent,
            disabledTextColor = AppTextPrimary,
            disabledPlaceholderColor = AppTextSecondary,
            disabledLeadingIconColor = AppIconMuted,
            disabledLabelColor = AppTextSecondary
        )
    )
}
