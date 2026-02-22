package com.example.mvt.ui.components.header

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.R
import com.example.mvt.ui.theme.PrimaryBlue

@Composable
fun RoutineHeader(
    titulo: String,
    fecha: String,
    tipoMedicion: String?,
    onBackClick: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            PrimaryBlue.copy(alpha = 1f),
            PrimaryBlue.copy(alpha = 0.9f),
            PrimaryBlue.copy(alpha = 0.6f)
        ),
        startY = 0f,
        endY = 700f
    )

    val iconoMedicion = when (tipoMedicion?.lowercase()) {
        "distancia" -> Icons.Default.Map
        "tiempo" -> Icons.Default.AccessTime
        else -> Icons.Default.DirectionsRun
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .background(
                brush = gradient,
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // === FILA SUPERIOR ===
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón volver
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color.White.copy(alpha = 0.2f), shape = CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Regresar",
                        tint = Color.White
                    )
                }

                // Línea difuminada izquierda → derecha
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.0f),
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.0f)
                                )
                            )
                        )
                )

                // Imagen central difuminada
                Image(
                    painter = painterResource(id = R.drawable.mvt),
                    contentDescription = "Logo MVT",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(42.dp)
                        .alpha(0.25f)
                )

                // Línea difuminada derecha → izquierda
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.0f),
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.0f)
                                )
                            )
                        )
                )

                // Ícono dinámico según tipo de medición
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconoMedicion,
                        contentDescription = "Tipo de medición",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // === TÍTULO Y FECHA ===
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = titulo,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Fecha",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 4.dp)
                    )
                    Text(
                        text = fecha,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // === LÍNEA DECORATIVA INFERIOR ===
            Box(
                modifier = Modifier
                    .height(2.dp)
                    .fillMaxWidth(0.3f)
                    .align(Alignment.CenterHorizontally)
                    .background(Color.White.copy(alpha = 0.6f))
            )
        }
    }
}
