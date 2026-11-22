package com.example.mvt.ui.components.header

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import coil.compose.AsyncImage
import com.example.mvt.R
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.MVTTheme
import com.example.mvt.ui.theme.PrimaryBlue
import com.example.mvt.ui.theme.AppError
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.net.URL

@Composable
fun RoutineModuleHeader(
    selectedStatistics: Boolean,
    profilePhotoUrl: String?,
    unreadMessagesCount: Int = 0,
    unreadNotificationsCount: Int = 0,
    onBackClick: () -> Unit,
    onMessageClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onCalendarClick: () -> Unit
) {
    Surface(
        modifier = Modifier.statusBarsPadding(),
        color = AppBackground
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = AppTextPrimary
                    )
                }
                Text(
                    text = "Rutinas",
                    color = AppTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onMessageClick) {
                        Icon(Icons.Outlined.Email, "Mensajes", tint = PrimaryBlue)
                    }
                    if (unreadMessagesCount > 0) {
                        RoutineUnreadBadge(
                            unreadCount = unreadMessagesCount,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 1.dp, y = (-1).dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onNotificationClick) {
                        Icon(Icons.Outlined.Notifications, "Notificaciones", tint = PrimaryBlue)
                    }
                    if (unreadNotificationsCount > 0) {
                        RoutineUnreadBadge(
                            unreadCount = unreadNotificationsCount,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 1.dp, y = (-1).dp)
                        )
                    }
                }
                Box {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AppSurface)
                            .clickable(onClick = onProfileClick),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profilePhotoUrl.isNullOrBlank()) {
                            Image(
                                painter = painterResource(id = R.drawable.iconografia_02_svg),
                                contentDescription = "Perfil",
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            AsyncImage(
                                model = profilePhotoUrl,
                                contentDescription = "Perfil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                RoutineHeaderTab(
                    label = "Calendario",
                    icon = Icons.Default.CalendarMonth,
                    selected = !selectedStatistics,
                    onClick = onCalendarClick
                )
                RoutineHeaderTab(
                    label = "Estadísticas",
                    icon = Icons.Default.BarChart,
                    selected = selectedStatistics,
                    onClick = onStatisticsClick
                )
            }
        }
    }
}

@Composable
private fun RowScope.RoutineHeaderTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) PrimaryBlue else AppTextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                color = if (selected) PrimaryBlue else AppTextSecondary,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) PrimaryBlue else Color.Transparent)
        )
    }
}

@Composable
private fun RoutineUnreadBadge(
    unreadCount: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(if (unreadCount > 99) 20.dp else 18.dp)
            .clip(CircleShape)
            .background(AppError),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (unreadCount > 99) "+99" else unreadCount.toString(),
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 9.sp,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RoutineModuleHeaderPreview() {
    MVTTheme {
        RoutineModuleHeader(
            selectedStatistics = false,
            profilePhotoUrl = null,
            onBackClick = {},
            onMessageClick = {},
            onNotificationClick = {},
            onProfileClick = {},
            onStatisticsClick = {},
            onCalendarClick = {}
        )
    }
}

@Composable
fun RoutinesHeader() {
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }

    var city by remember { mutableStateOf("Cargando ubicación...") }
    var temperature by remember { mutableStateOf("--") }
    var description by remember { mutableStateOf("--") }
    var weatherIcon by remember { mutableStateOf(Icons.Default.Cloud) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                @SuppressLint("MissingPermission")
                val location = fused.lastLocation.await()
                if (location != null) {
                    scope.launch(Dispatchers.IO) {
                        val ubicacion = obtenerUbicacion(location.latitude, location.longitude)
                        val clima = obtenerClima(location.latitude, location.longitude)
                        city = ubicacion
                        temperature = clima.first
                        description = clima.second
                        weatherIcon = clima.third
                    }
                } else city = "Ubicación no disponible"
            } catch (e: Exception) {
                city = "Error obteniendo ubicación"
            }
        } else city = "Permisos no concedidos"
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            AppBackground,
            AppSurface,
            AppSurfaceAlt
        ),
        startY = 0f,
        endY = 700f
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .background(
                brush = gradient,
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 10.dp, bottom = 2.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // Fila ubicación + clima
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AppSurfaceAlt.copy(alpha = 0.92f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = city,
                        color = Color.White,
                        fontSize = 16.sp,
                        lineHeight = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .width(160.dp)
                            .basicMarquee()
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AppSurfaceAlt.copy(alpha = 0.92f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            weatherIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = temperature,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = description,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            modifier = Modifier
                                .width(100.dp)
                                .basicMarquee()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Título y subtítulo
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(
                    text = "Rutinas",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Consulta tus sesiones programadas",
                    fontSize = 15.sp,
                    color = AppTextSecondary
                )
            }

            Spacer(
                modifier = Modifier
                    .height(1.5.dp)
                    .fillMaxWidth(0.3f)
                    .background(AppBorder)
            )
        }
    }
}

// === FUNCIÓN CLIMA ===
private fun obtenerClima(lat: Double, lon: Double): Triple<String, String, androidx.compose.ui.graphics.vector.ImageVector> {
    return try {
        val url =
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
        val result = URL(url).readText()

        Log.d("CLIMA_API", "Respuesta completa: $result")

        val current = JSONObject(result).getJSONObject("current_weather")
        val temp = "${current.getDouble("temperature").toInt()}°C"
        val code = current.getInt("weathercode")

        val (estado, icon) = when (code) {
            0 -> "Despejado" to Icons.Default.WbSunny
            1, 2, 3 -> "Parcialmente nublado" to Icons.Default.WbCloudy
            45, 48 -> "Niebla" to Icons.Default.CloudQueue
            in 51..67 -> "Lluvia ligera" to Icons.Default.Umbrella
            in 71..77 -> "Nieve" to Icons.Default.AcUnit
            in 80..82 -> "Lluvia moderada o fuerte" to Icons.Default.Grain
            in 95..99 -> "Tormenta eléctrica" to Icons.Default.FlashOn
            else -> "Desconocido" to Icons.Default.Cloud
        }

        Triple(temp, estado, icon)
    } catch (e: Exception) {
        Log.e("CLIMA_API_ERROR", "Error obteniendo clima: ${e.message}", e)
        Triple("--", "Error clima", Icons.Default.Cloud)
    }
}

// === FUNCIÓN UBICACIÓN ===
private fun obtenerUbicacion(lat: Double, lon: Double): String {
    return try {
        val url =
            URL("https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$lat&lon=$lon")
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "com.example.mvt (Android App)")
        val text = conn.inputStream.bufferedReader().readText()
        conn.disconnect()
        val json = JSONObject(text).getJSONObject("address")
        json.optString("city", json.optString("town", json.optString("village", "Desconocido")))
    } catch (e: Exception) {
        "Desconocido"
    }
}
