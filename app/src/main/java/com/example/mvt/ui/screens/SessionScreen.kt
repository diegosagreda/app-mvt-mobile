package com.example.mvt.ui.screens

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mvt.R
import com.example.mvt.ui.theme.PrimaryBlue
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun SessionScreen(navController: NavController) {
    var scale by remember { mutableStateOf(0f) }

    val scaleAnim = animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(
            durationMillis = 1500,
            easing = { OvershootInterpolator(2f).getInterpolation(it) }
        ),
        label = "scaleAnimation"
    )

    LaunchedEffect(Unit) {
        scale = 1f
        delay(2500)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            navController.navigate("athleteMain") {
                popUpTo(0) { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Imagen de fondo
        Image(
            painter = painterResource(id = R.drawable.banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.9f) // oscuridad 50 %
        )

        // Capa de degradado azul
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            PrimaryBlue.copy(alpha = 0.8f),
                            PrimaryBlue.copy(alpha = 0.7f),
                            Color(0xFF0D47A1).copy(alpha = 0.7f)
                        )
                    )
                )
        )

        // Contenido central
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo MVT",
                modifier = Modifier
                    .size(180.dp)
                    .scale(scaleAnim.value)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "My Virtual Trainer",
                fontSize = 24.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )


        }
    }
}
