package com.example.mvt.ui.components.header

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.mvt.ui.theme.PrimaryBlue
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.net.URL

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
            PrimaryBlue.copy(alpha = 1f),
            PrimaryBlue.copy(alpha = 0.9f),
            PrimaryBlue.copy(alpha = 0.6f)
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
                            .background(Color.White.copy(alpha = 0.2f)),
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
                            .background(Color.White.copy(alpha = 0.2f)),
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
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Spacer(
                modifier = Modifier
                    .height(1.5.dp)
                    .fillMaxWidth(0.3f)
                    .background(Color.White.copy(alpha = 0.6f))
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
