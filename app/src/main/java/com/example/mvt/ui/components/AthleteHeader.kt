package com.example.mvt.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.mvt.R
import com.example.mvt.ui.theme.PrimaryBlue
import java.util.Calendar

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AthleteHeader(
    saludo: String,
    userName: String,
    onMenuClick: () -> Unit,
    onMessageClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onLogoutClick: () -> Unit,
    profilePhotoUrl: String? = null
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Determinar hora actual y seleccionar ícono
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val isDayTime = currentHour in 6..17 // 6am a 5:59pm
    val greetingIcon = if (isDayTime) Icons.Default.WbSunny else Icons.Default.DarkMode

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue),
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menú", tint = Color.White)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                /*Icon(
                    imageVector = greetingIcon,
                    contentDescription = if (isDayTime) "Día" else "Noche",
                    tint = Color.White,
                    modifier = Modifier
                        .size(22.dp)
                        .padding(end = 6.dp)
                )*/
                Text(
                    text = "$saludo, $userName",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        },
        actions = {
            IconButton(onClick = onMessageClick) {
                Icon(Icons.Default.Email, contentDescription = "Mensajes", tint = Color.White)
            }
            IconButton(onClick = onNotificationClick) {
                Icon(Icons.Default.Notifications, contentDescription = "Notificaciones", tint = Color.White)
            }

            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { showLogoutDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (profilePhotoUrl == null) {
                    Image(
                        painter = painterResource(id = R.drawable.iconografia_02_svg),
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    AsyncImage(
                        model = profilePhotoUrl,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }
    )

    // === DIÁLOGO DE CIERRE DE SESIÓN ===
    if (showLogoutDialog) {
        Dialog(onDismissRequest = { showLogoutDialog = false }) {
            AnimatedVisibility(
                visible = showLogoutDialog,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1E1E),
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .wrapContentHeight()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(24.dp)
                            .background(Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "¿Deseas cerrar sesión?",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                showLogoutDialog = false
                                onLogoutClick()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cerrar sesión", color = Color.White)
                        }

                        TextButton(
                            onClick = { showLogoutDialog = false },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("Cancelar", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
