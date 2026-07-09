package com.example.mvt.ui.components.header

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.R
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun RoutineHeader(
    titulo: String,
    fecha: String,
    tipoMedicion: String?,
    showStravaToggle: Boolean = false,
    stravaExpanded: Boolean = false,
    stravaConnected: Boolean = false,
    stravaButtonLabel: String = "Resultados Strava",
    onStravaToggle: () -> Unit = {},
    onBackClick: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            AppBackground,
            AppSurface,
            AppSurfaceAlt
        ),
        startY = 0f,
        endY = 700f
    )

    val measurementKey = tipoMedicion.orEmpty().trim().lowercase()
    val (iconoMedicion, iconTint, measurementLabel) = when {
        measurementKey == "tiempo" ->
            Triple(Icons.Default.AccessTime, Color(0xFF7FB3FF), "Rutina por tiempo")
        measurementKey == "distancia" ||
            measurementKey == "km" ||
            measurementKey == "m" ||
            measurementKey.contains("dist") ||
            measurementKey.contains("metro") ->
            Triple(Icons.Default.Straighten, Color(0xFF79D7B7), "Rutina por distancia")
        else ->
            Triple(Icons.Default.DirectionsRun, Color(0xFFFFC76E), "Rutina deportiva")
    }
    val isShortTitle = titulo.trim().length <= 22
    val visualDate = rememberFormattedRoutineDate(fecha)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = gradient,
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 170.dp)
                .padding(top = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // === TÍTULO ===
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = titulo,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    modifier = if (isShortTitle) {
                        Modifier.fillMaxWidth(0.92f)
                    } else {
                        Modifier
                            .fillMaxWidth(0.94f)
                            .basicMarquee()
                    }
                )
            }

            // === FECHA ===
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = AppSurfaceAlt.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.85f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color.White.copy(alpha = 0.08f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Fecha",
                                tint = Color(0xFFFFC76E),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = visualDate,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // === ACCIONES ===
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(46.dp)
                        .background(AppSurfaceAlt, shape = CircleShape)
                        .border(1.dp, AppBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Regresar",
                        tint = Color.White
                    )
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (showStravaToggle) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = AppSurfaceAlt,
                            border = BorderStroke(
                                1.dp,
                                if (stravaConnected) Color(0x55FC4C02) else AppBorder
                            ),
                            modifier = Modifier.clickable { onStravaToggle() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(Color(0xFFFC4C02), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_strava_mark),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                                Text(
                                    text = stravaButtonLabel,
                                    color = AppTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = if (stravaExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.92f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = AppSurfaceAlt,
                    border = BorderStroke(1.dp, AppBorder)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(iconTint.copy(alpha = 0.16f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconoMedicion,
                                contentDescription = measurementLabel,
                                tint = iconTint,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            // === LÍNEA DECORATIVA INFERIOR ===
            Box(
                modifier = Modifier
                    .height(2.dp)
                    .fillMaxWidth(0.3f)
                    .align(Alignment.CenterHorizontally)
                    .background(AppBorder)
            )
        }
    }
}

@Composable
private fun rememberFormattedRoutineDate(fecha: String): String {
    val inputPatterns = listOf(
        "yyyy/MM/dd",
        "yyyy-MM-dd"
    )

    inputPatterns.forEach { pattern ->
        runCatching {
            val parsed = SimpleDateFormat(pattern, Locale.getDefault()).parse(fecha)
            if (parsed != null) {
                return SimpleDateFormat("EEEE, d 'de' MMMM yyyy", Locale("es", "CO"))
                    .format(parsed)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "CO")) else it.toString() }
            }
        }
    }

    return fecha
}
